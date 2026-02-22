package com.isaac.approvalworkflowengine.workflowruntime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.security.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskClaimApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void approverCanClaimTaskAndRetrySameIdempotencyKey() throws Exception {
        String requestorToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "requestor");
        String approverToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "approver");

        JsonNode created = RuntimeTestSupport.createRequest(
            mockMvc,
            objectMapper,
            requestorToken,
            "EXPENSE",
            "Claim runtime",
            1200
        );

        UUID requestId = UUID.fromString(created.get("id").asText());
        RuntimeTestSupport.submitRequest(mockMvc, objectMapper, requestorToken, requestId, "claim-submit-12345");

        UUID taskId = jdbcTemplate.queryForObject(
            "select id from tasks where request_id = ? and status = 'PENDING'",
            UUID.class,
            requestId
        );

        MvcResult firstClaim = mockMvc.perform(post("/api/tasks/" + taskId + "/claim")
                .header("Authorization", "Bearer " + approverToken)
                .header("Idempotency-Key", "claim-key-12345"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLAIMED"))
            .andReturn();

        String firstClaimedBy = objectMapper.readTree(firstClaim.getResponse().getContentAsString())
            .get("claimedByUserId").asText();

        mockMvc.perform(post("/api/tasks/" + taskId + "/claim")
                .header("Authorization", "Bearer " + approverToken)
                .header("Idempotency-Key", "claim-key-12345"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLAIMED"))
            .andExpect(jsonPath("$.claimedByUserId").value(firstClaimedBy));

        Integer claimedCount = jdbcTemplate.queryForObject(
            "select count(*) from tasks where id = ? and status = 'CLAIMED'",
            Integer.class,
            taskId
        );

        assertThat(claimedCount).isEqualTo(1);
    }

    @Test
    void nonAssigneeCannotClaimTask() throws Exception {
        String requestorToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "requestor");
        JsonNode created = RuntimeTestSupport.createRequest(
            mockMvc,
            objectMapper,
            requestorToken,
            "EXPENSE",
            "Forbidden claim",
            1400
        );

        UUID requestId = UUID.fromString(created.get("id").asText());
        RuntimeTestSupport.submitRequest(mockMvc, objectMapper, requestorToken, requestId, "claim-submit-54321");

        UUID taskId = jdbcTemplate.queryForObject(
            "select id from tasks where request_id = ? and status = 'PENDING'",
            UUID.class,
            requestId
        );

        mockMvc.perform(post("/api/tasks/" + taskId + "/claim")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "requestor-claim-9999"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listTasksDefaultCombinesDirectAndRoleAssignments() throws Exception {
        String requestorToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "requestor");
        String approverToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "approver");

        JsonNode created = RuntimeTestSupport.createRequest(
            mockMvc,
            objectMapper,
            requestorToken,
            "EXPENSE",
            "List task defaults",
            1100
        );

        UUID requestId = UUID.fromString(created.get("id").asText());
        RuntimeTestSupport.submitRequest(mockMvc, objectMapper, requestorToken, requestId, "list-submit-12345");

        mockMvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + approverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].requestId", hasItem(requestId.toString())));

        mockMvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0));
    }
}
