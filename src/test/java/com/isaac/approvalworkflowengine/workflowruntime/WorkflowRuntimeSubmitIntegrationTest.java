package com.isaac.approvalworkflowengine.workflowruntime;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.security.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkflowRuntimeSubmitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void submitCreatesRuntimeInstanceAndPendingTask() throws Exception {
        String requestorToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "requestor");
        JsonNode created = RuntimeTestSupport.createRequest(
            mockMvc,
            objectMapper,
            requestorToken,
            "EXPENSE",
            "Runtime submit",
            1500
        );

        UUID requestId = UUID.fromString(created.get("id").asText());
        JsonNode submitted = RuntimeTestSupport.submitRequest(
            mockMvc,
            objectMapper,
            requestorToken,
            requestId,
            "runtime-submit-12345"
        );

        assertThat(submitted.get("status").asText()).isEqualTo("IN_REVIEW");

        Integer instanceCount = jdbcTemplate.queryForObject(
            "select count(*) from workflow_instances where request_id = ?",
            Integer.class,
            requestId
        );
        Integer pendingTaskCount = jdbcTemplate.queryForObject(
            "select count(*) from tasks where request_id = ? and status = 'PENDING' and assignee_role = 'APPROVER'",
            Integer.class,
            requestId
        );

        assertThat(instanceCount).isEqualTo(1);
        assertThat(pendingTaskCount).isEqualTo(1);
    }

    @Test
    void submitRejectsRuleAssignmentStrategyAtRuntime() throws Exception {
        String adminToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "admin");
        String requestorToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "requestor");
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        RuntimeTestSupport.createAndActivateWorkflow(
            mockMvc,
            objectMapper,
            adminToken,
            "WF_RULE_ASSIGN_" + suffix,
            "RULE_REQ_" + suffix,
            """
            {
              "nodes":[
                {"id":"start","type":"START"},
                {"id":"approve","type":"APPROVAL","assignment":{"strategy":"RULE","expression":"payload.manager"}},
                {"id":"end","type":"END"}
              ],
              "edges":[
                {"from":"start","to":"approve"},
                {"from":"approve","to":"end"}
              ]
            }
            """
        );

        JsonNode created = RuntimeTestSupport.createRequest(
            mockMvc,
            objectMapper,
            requestorToken,
            "RULE_REQ_" + suffix,
            "Rule assignment",
            900
        );

        mockMvc.perform(post("/api/requests/" + created.get("id").asText() + "/submit")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "rule-submit-12345"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value("RULE assignment strategy is not supported in E5 runtime"));
    }
}
