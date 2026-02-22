package com.isaac.approvalworkflowengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestLifecycleApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndReadOwnRequest() throws Exception {
        String requestorToken = loginAndExtractToken("requestor", "password");
        String approverToken = loginAndExtractToken("approver", "password");

        JsonNode created = createRequest(requestorToken, "EXPENSE", "Team lunch");
        String requestId = created.get("id").asText();

        mockMvc.perform(get("/api/requests/" + requestId).header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(requestId))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/requests/" + requestId).header("Authorization", "Bearer " + approverToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void requestCanOnlyBeEditedInEditableStates() throws Exception {
        String requestorToken = loginAndExtractToken("requestor", "password");
        JsonNode created = createRequest(requestorToken, "EXPENSE", "Hotel booking");
        String requestId = created.get("id").asText();

        mockMvc.perform(patch("/api/requests/" + requestId)
                .header("Authorization", "Bearer " + requestorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Hotel booking updated\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Hotel booking updated"));

        mockMvc.perform(post("/api/requests/" + requestId + "/submit")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "submit-12345678"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.workflowVersionId").isNotEmpty());

        mockMvc.perform(patch("/api/requests/" + requestId)
                .header("Authorization", "Bearer " + requestorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"should fail\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void submitIsIdempotentPerKey() throws Exception {
        String requestorToken = loginAndExtractToken("requestor", "password");
        JsonNode created = createRequest(requestorToken, "EXPENSE", "Conference ticket");
        String requestId = created.get("id").asText();

        mockMvc.perform(post("/api/requests/" + requestId + "/submit")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "same-key-12345"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(post("/api/requests/" + requestId + "/submit")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "same-key-12345"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(post("/api/requests/" + requestId + "/submit")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "new-key-123456"))
            .andExpect(status().isConflict());
    }

    @Test
    void cancelIsIdempotentPerKey() throws Exception {
        String requestorToken = loginAndExtractToken("requestor", "password");
        JsonNode created = createRequest(requestorToken, "EXPENSE", "Taxi reimbursement");
        String requestId = created.get("id").asText();

        mockMvc.perform(post("/api/requests/" + requestId + "/cancel")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "cancel-key-1234"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/requests/" + requestId + "/cancel")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "cancel-key-1234"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/requests/" + requestId + "/cancel")
                .header("Authorization", "Bearer " + requestorToken)
                .header("Idempotency-Key", "cancel-key-9999"))
            .andExpect(status().isConflict());
    }

    @Test
    void listSupportsPaginationFilteringAndOwnership() throws Exception {
        String requestorToken = loginAndExtractToken("requestor", "password");
        String approverToken = loginAndExtractToken("approver", "password");
        String adminToken = loginAndExtractToken("admin", "password");

        createRequest(requestorToken, "EXPENSE", "Req-1");
        createRequest(requestorToken, "EXPENSE", "Req-2");
        createRequest(requestorToken, "TRAVEL", "Req-3");
        createRequest(approverToken, "EXPENSE", "Req-4");

        mockMvc.perform(get("/api/requests?page=0&size=2")
                .header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.page.totalElements").value(3));

        mockMvc.perform(get("/api/requests?requestType=TRAVEL")
                .header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].requestType").value("TRAVEL"));

        mockMvc.perform(get("/api/requests")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(4));
    }

    private JsonNode createRequest(String token, String requestType, String title) throws Exception {
        String payload = """
            {
              "requestType":"%s",
              "title":"%s",
              "description":"Request description",
              "payload":{"amount":123.45,"category":"MEAL"},
              "amount":123.45,
              "currency":"USD",
              "department":"Finance",
              "costCenter":"CC-100"
            }
            """.formatted(requestType, title);

        MvcResult result = mockMvc.perform(post("/api/requests")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(created.get("id").asText()).isNotBlank();
        return created;
    }

    private String loginAndExtractToken(String usernameOrEmail, String password) throws Exception {
        String payload = "{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"" + password + "\"}";

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("accessToken").asText();
    }
}
