package com.isaac.approvalworkflowengine.workflowruntime;

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
class TaskDecisionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void decisionWithoutClaimReturnsConflict() throws Exception {
        TaskContext taskContext = createSubmittedExpenseTask("No claim conflict");

        mockMvc.perform(post("/api/tasks/" + taskContext.taskId() + "/decisions")
                .header("Authorization", "Bearer " + taskContext.approverToken())
                .header("Idempotency-Key", "decide-without-claim")
                .contentType("application/json")
                .content("{\"action\":\"APPROVE\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void sendBackRequiresComment() throws Exception {
        TaskContext taskContext = claimSubmittedExpenseTask("Send back comment required");

        mockMvc.perform(post("/api/tasks/" + taskContext.taskId() + "/decisions")
                .header("Authorization", "Bearer " + taskContext.approverToken())
                .header("Idempotency-Key", "sendback-missing-comment")
                .contentType("application/json")
                .content("{\"action\":\"SEND_BACK\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void sendBackSetsRequestToChangesRequested() throws Exception {
        TaskContext taskContext = claimSubmittedExpenseTask("Send back status");

        mockMvc.perform(post("/api/tasks/" + taskContext.taskId() + "/decisions")
                .header("Authorization", "Bearer " + taskContext.approverToken())
                .header("Idempotency-Key", "sendback-with-comment")
                .contentType("application/json")
                .content("{\"action\":\"SEND_BACK\",\"comment\":\"Please add receipt\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.action").value("SEND_BACK"));

        mockMvc.perform(get("/api/requests/" + taskContext.requestId())
                .header("Authorization", "Bearer " + taskContext.requestorToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"));
    }

    @Test
    void rejectSetsRequestToRejected() throws Exception {
        TaskContext taskContext = claimSubmittedExpenseTask("Reject status");

        mockMvc.perform(post("/api/tasks/" + taskContext.taskId() + "/decisions")
                .header("Authorization", "Bearer " + taskContext.approverToken())
                .header("Idempotency-Key", "reject-with-comment")
                .contentType("application/json")
                .content("{\"action\":\"REJECT\",\"comment\":\"Out of policy\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.action").value("REJECT"));

        mockMvc.perform(get("/api/requests/" + taskContext.requestId())
                .header("Authorization", "Bearer " + taskContext.requestorToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void approveCompletesRequest() throws Exception {
        TaskContext taskContext = claimSubmittedExpenseTask("Approve final");

        mockMvc.perform(post("/api/tasks/" + taskContext.taskId() + "/decisions")
                .header("Authorization", "Bearer " + taskContext.approverToken())
                .header("Idempotency-Key", "approve-terminal")
                .contentType("application/json")
                .content("{\"action\":\"APPROVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.action").value("APPROVE"));

        mockMvc.perform(get("/api/requests/" + taskContext.requestId())
                .header("Authorization", "Bearer " + taskContext.requestorToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void delegateDecisionReturnsConflict() throws Exception {
        TaskContext taskContext = claimSubmittedExpenseTask("Delegate conflict");

        mockMvc.perform(post("/api/tasks/" + taskContext.taskId() + "/decisions")
                .header("Authorization", "Bearer " + taskContext.approverToken())
                .header("Idempotency-Key", "delegate-not-supported")
                .contentType("application/json")
                .content("{\"action\":\"DELEGATE\",\"delegateUserId\":\"5ad15712-2c98-4b9d-8f1d-6baf6a4f6d78\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void decisionIdempotencyReturnsSameDecision() throws Exception {
        TaskContext taskContext = claimSubmittedExpenseTask("Decision idempotency");

        MvcResult first = mockMvc.perform(post("/api/tasks/" + taskContext.taskId() + "/decisions")
                .header("Authorization", "Bearer " + taskContext.approverToken())
                .header("Idempotency-Key", "approve-idem-12345")
                .contentType("application/json")
                .content("{\"action\":\"APPROVE\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String decisionId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/tasks/" + taskContext.taskId() + "/decisions")
                .header("Authorization", "Bearer " + taskContext.approverToken())
                .header("Idempotency-Key", "approve-idem-12345")
                .contentType("application/json")
                .content("{\"action\":\"APPROVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(decisionId));
    }

    private TaskContext createSubmittedExpenseTask(String title) throws Exception {
        String requestorToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "requestor");
        String approverToken = RuntimeTestSupport.loginAndExtractToken(mockMvc, objectMapper, "approver");

        JsonNode created = RuntimeTestSupport.createRequest(mockMvc, objectMapper, requestorToken, "EXPENSE", title, 1800);
        UUID requestId = UUID.fromString(created.get("id").asText());
        RuntimeTestSupport.submitRequest(mockMvc, objectMapper, requestorToken, requestId, "submit-" + title.hashCode());

        UUID taskId = jdbcTemplate.queryForObject(
            "select id from tasks where request_id = ? and status = 'PENDING'",
            UUID.class,
            requestId
        );

        return new TaskContext(requestId, taskId, requestorToken, approverToken);
    }

    private TaskContext claimSubmittedExpenseTask(String title) throws Exception {
        TaskContext context = createSubmittedExpenseTask(title);

        mockMvc.perform(post("/api/tasks/" + context.taskId() + "/claim")
                .header("Authorization", "Bearer " + context.approverToken())
                .header("Idempotency-Key", "claim-" + title.hashCode()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLAIMED"));

        return context;
    }

    private record TaskContext(UUID requestId, UUID taskId, String requestorToken, String approverToken) {
    }
}
