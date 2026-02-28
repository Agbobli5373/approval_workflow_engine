package com.isaac.approvalworkflowengine.workflowruntime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.security.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskDelegationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void delegatedUserCanClaimAndApproveOnBehalfOfDelegator() throws Exception {
        String requestorToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "requestor");
        String approverToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "approver");

        createDelegation(approverToken, RuntimeTestSupport.REQUESTOR_USER_ID, "EXPENSE", "Finance", "APPROVER");

        JsonNode created = RuntimeTestSupport.createRequest(
            mockMvc,
            objectMapper,
            requestorToken,
            "EXPENSE",
            "Delegated approval",
            1600
        );

        UUID requestId = UUID.fromString(created.get("id").asText());
        RuntimeTestSupport.submitRequest(mockMvc, objectMapper, requestorToken, requestId, "delegation-submit-12345");

        UUID taskId = jdbcTemplate.queryForObject(
            "select id from tasks where request_id = ? and status = 'PENDING'",
            UUID.class,
            requestId
        );

        mockMvc.perform(post("/api/tasks/" + taskId + "/claim")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "delegate-claim-12345"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.claimedByUserId").value(RuntimeTestSupport.REQUESTOR_USER_ID.toString()));

        mockMvc.perform(post("/api/tasks/" + taskId + "/decisions")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "delegate-approve-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"APPROVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actedByUserId").value(RuntimeTestSupport.REQUESTOR_USER_ID.toString()))
            .andExpect(jsonPath("$.actedOnBehalfOfUserId").value(RuntimeTestSupport.APPROVER_USER_ID.toString()));

        UUID actedOnBehalf = jdbcTemplate.queryForObject(
            "select acted_on_behalf_of_user_id from task_decisions where task_id = ?",
            UUID.class,
            taskId
        );
        assertThat(actedOnBehalf).isEqualTo(RuntimeTestSupport.APPROVER_USER_ID);

        mockMvc.perform(get("/api/requests/" + requestId)
                .header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void expiredDelegationCannotAuthorizeClaim() throws Exception {
        String requestorToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "requestor");
        String approverToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "approver");

        UUID delegationId = createDelegation(approverToken, RuntimeTestSupport.REQUESTOR_USER_ID, "EXPENSE", null, "APPROVER");
        jdbcTemplate.update(
            "update delegations set valid_until = ? where id = ?",
            Instant.now().minus(1, ChronoUnit.MINUTES),
            delegationId
        );

        JsonNode created = RuntimeTestSupport.createRequest(
            mockMvc,
            objectMapper,
            requestorToken,
            "EXPENSE",
            "Expired delegation claim",
            1700
        );

        UUID requestId = UUID.fromString(created.get("id").asText());
        RuntimeTestSupport.submitRequest(mockMvc, objectMapper, requestorToken, requestId, "delegation-submit-67890");

        UUID taskId = jdbcTemplate.queryForObject(
            "select id from tasks where request_id = ? and status = 'PENDING'",
            UUID.class,
            requestId
        );

        mockMvc.perform(post("/api/tasks/" + taskId + "/claim")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "expired-claim-12345"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void outOfScopeDelegationCannotAuthorizeClaim() throws Exception {
        String requestorToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "requestor");
        String approverToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "approver");

        createDelegation(approverToken, RuntimeTestSupport.REQUESTOR_USER_ID, "TRAVEL", null, "APPROVER");

        JsonNode created = RuntimeTestSupport.createRequest(
            mockMvc,
            objectMapper,
            requestorToken,
            "EXPENSE",
            "Out of scope delegation",
            1500
        );

        UUID requestId = UUID.fromString(created.get("id").asText());
        RuntimeTestSupport.submitRequest(mockMvc, objectMapper, requestorToken, requestId, "delegation-submit-54321");

        UUID taskId = jdbcTemplate.queryForObject(
            "select id from tasks where request_id = ? and status = 'PENDING'",
            UUID.class,
            requestId
        );

        mockMvc.perform(post("/api/tasks/" + taskId + "/claim")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "outscope-claim-12345"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private UUID createDelegation(
        String token,
        UUID delegateUserId,
        String requestType,
        String department,
        String role
    ) throws Exception {
        jdbcTemplate.update(
            "update delegations set active = false, revoked_at = current_timestamp where delegator_user_id = ? and delegate_user_id = ? and active = true",
            RuntimeTestSupport.APPROVER_USER_ID,
            delegateUserId
        );

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant validFrom = now.minus(10, ChronoUnit.MINUTES);
        Instant validUntil = now.plus(2, ChronoUnit.HOURS);

        String requestTypeField = requestType == null ? "null" : "\"" + requestType + "\"";
        String departmentField = department == null ? "null" : "\"" + department + "\"";
        String roleField = role == null ? "null" : "\"" + role + "\"";

        String payload = """
            {
              "delegateUserId":"%s",
              "requestType":%s,
              "department":%s,
              "role":%s,
              "allScope":false,
              "validFrom":"%s",
              "validUntil":"%s"
            }
            """.formatted(delegateUserId, requestTypeField, departmentField, roleField, validFrom, validUntil);

        MvcResult result = mockMvc.perform(post("/api/delegations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
