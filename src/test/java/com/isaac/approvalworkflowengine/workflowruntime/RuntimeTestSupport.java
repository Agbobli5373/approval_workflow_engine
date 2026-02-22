package com.isaac.approvalworkflowengine.workflowruntime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

final class RuntimeTestSupport {

    private RuntimeTestSupport() {
    }

    static String loginAndExtractToken(MockMvc mockMvc, ObjectMapper objectMapper, String usernameOrEmail) throws Exception {
        String payload = "{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"password\"}";

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("accessToken").asText();
    }

    static JsonNode createRequest(
        MockMvc mockMvc,
        ObjectMapper objectMapper,
        String token,
        String requestType,
        String title,
        double amount
    ) throws Exception {
        String payload = """
            {
              "requestType":"%s",
              "title":"%s",
              "description":"Runtime test request",
              "payload":{"amount":%s,"costCenter":"CC-100"},
              "amount":%s,
              "currency":"USD",
              "department":"Finance",
              "costCenter":"CC-100"
            }
            """.formatted(requestType, title, amount, amount);

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

    static JsonNode submitRequest(MockMvc mockMvc, ObjectMapper objectMapper, String token, UUID requestId, String key)
        throws Exception {
        MvcResult result = mockMvc.perform(post("/api/requests/" + requestId + "/submit")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key))
            .andExpect(status().isAccepted())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    static UUID createAndActivateWorkflow(
        MockMvc mockMvc,
        ObjectMapper objectMapper,
        String adminToken,
        String definitionKey,
        String requestType,
        String graphJson
    ) throws Exception {
        String definitionPayload = """
            {
              "definitionKey":"%s",
              "name":"Runtime %s",
              "requestType":"%s",
              "allowLoopback":false
            }
            """.formatted(definitionKey, definitionKey, requestType);

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload))
            .andExpect(status().isCreated());

        MvcResult createVersion = mockMvc.perform(post("/api/workflow-definitions/" + definitionKey + "/versions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"graph\":" + graphJson + "}"))
            .andExpect(status().isCreated())
            .andReturn();

        UUID versionId = UUID.fromString(objectMapper.readTree(createVersion.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/workflow-versions/" + versionId + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        return versionId;
    }
}
