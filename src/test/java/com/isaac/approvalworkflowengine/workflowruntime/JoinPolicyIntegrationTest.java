package com.isaac.approvalworkflowengine.workflowruntime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.security.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JoinPolicyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void anyJoinSkipsSiblingTasksAfterFirstApproval() throws Exception {
        RuntimeJoinContext context = setupParallelWorkflow("ANY", null);

        UUID firstTask = claimAnyPendingTask(context, "any-claim-1");
        approveTask(context, firstTask, "any-approve-1");

        Integer skippedCount = jdbcTemplate.queryForObject(
            "select count(*) from tasks where request_id = ? and status = 'SKIPPED'",
            Integer.class,
            context.requestId()
        );

        assertThat(skippedCount).isEqualTo(1);

        mockMvc.perform(get("/api/requests/" + context.requestId())
                .header("Authorization", "Bearer " + context.requestorToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void allJoinRequiresAllApprovals() throws Exception {
        RuntimeJoinContext context = setupParallelWorkflow("ALL", null);

        UUID firstTask = claimAnyPendingTask(context, "all-claim-1");
        approveTask(context, firstTask, "all-approve-1");

        mockMvc.perform(get("/api/requests/" + context.requestId())
                .header("Authorization", "Bearer " + context.requestorToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        UUID secondTask = claimAnyPendingTask(context, "all-claim-2");
        approveTask(context, secondTask, "all-approve-2");

        mockMvc.perform(get("/api/requests/" + context.requestId())
                .header("Authorization", "Bearer " + context.requestorToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void quorumJoinSkipsSiblingWhenThresholdReached() throws Exception {
        RuntimeJoinContext context = setupParallelWorkflow("QUORUM", 1);

        UUID firstTask = claimAnyPendingTask(context, "quorum-claim-1");
        approveTask(context, firstTask, "quorum-approve-1");

        Integer skippedCount = jdbcTemplate.queryForObject(
            "select count(*) from tasks where request_id = ? and status = 'SKIPPED'",
            Integer.class,
            context.requestId()
        );

        assertThat(skippedCount).isEqualTo(1);

        mockMvc.perform(get("/api/requests/" + context.requestId())
                .header("Authorization", "Bearer " + context.requestorToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    private RuntimeJoinContext setupParallelWorkflow(String policy, Integer quorum) throws Exception {
        String adminToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "admin");
        String requestorToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "requestor");
        String approverToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "approver");

        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String definitionKey = "WF_JOIN_" + policy + "_" + suffix;
        String requestType = "JOIN_" + policy + "_" + suffix;

        RuntimeTestSupport.createAndActivateWorkflow(
            mockMvc,
            objectMapper,
            adminToken,
            definitionKey,
            requestType,
            parallelJoinGraph(policy, quorum)
        );

        JsonNode created = RuntimeTestSupport.createRequest(
            mockMvc,
            objectMapper,
            requestorToken,
            requestType,
            "Join policy " + policy,
            2200
        );

        UUID requestId = UUID.fromString(created.get("id").asText());
        RuntimeTestSupport.submitRequest(mockMvc, objectMapper, requestorToken, requestId, "join-submit-" + suffix);

        Integer pendingCount = jdbcTemplate.queryForObject(
            "select count(*) from tasks where request_id = ? and status = 'PENDING'",
            Integer.class,
            requestId
        );

        assertThat(pendingCount).isEqualTo(2);

        return new RuntimeJoinContext(requestId, requestorToken, approverToken);
    }

    private UUID claimAnyPendingTask(RuntimeJoinContext context, String claimKey) throws Exception {
        UUID taskId = nextPendingTaskId(context.requestId());

        mockMvc.perform(post("/api/tasks/" + taskId + "/claim")
                .header("Authorization", "Bearer " + context.approverToken())
                .header("Idempotency-Key", claimKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLAIMED"));

        return taskId;
    }

    private void approveTask(RuntimeJoinContext context, UUID taskId, String decisionKey) throws Exception {
        mockMvc.perform(post("/api/tasks/" + taskId + "/decisions")
                .header("Authorization", "Bearer " + context.approverToken())
                .header("Idempotency-Key", decisionKey)
                .contentType("application/json")
                .content("{\"action\":\"APPROVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.action").value("APPROVE"));
    }

    private UUID nextPendingTaskId(UUID requestId) {
        List<UUID> taskIds = jdbcTemplate.queryForList(
            "select id from tasks where request_id = ? and status = 'PENDING' order by step_key",
            UUID.class,
            requestId
        );

        assertThat(taskIds).isNotEmpty();
        return taskIds.get(0);
    }

    private String parallelJoinGraph(String policy, Integer quorum) {
        String join = quorum == null
            ? "{\"policy\":\"" + policy + "\"}"
            : "{\"policy\":\"" + policy + "\",\"quorum\":" + quorum + "}";

        return """
            {
              "nodes":[
                {"id":"start","type":"START"},
                {"id":"approve_a","type":"APPROVAL","assignment":{"strategy":"ROLE","role":"APPROVER"}},
                {"id":"approve_b","type":"APPROVAL","assignment":{"strategy":"ROLE","role":"APPROVER"}},
                {"id":"join","type":"JOIN","join":%s},
                {"id":"end","type":"END"}
              ],
              "edges":[
                {"from":"start","to":"approve_a"},
                {"from":"start","to":"approve_b"},
                {"from":"approve_a","to":"join"},
                {"from":"approve_b","to":"join"},
                {"from":"join","to":"end"}
              ]
            }
            """.formatted(join);
    }

    private record RuntimeJoinContext(UUID requestId, String requestorToken, String approverToken) {
    }
}
