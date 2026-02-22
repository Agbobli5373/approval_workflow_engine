package com.isaac.approvalworkflowengine.rules;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.security.rate-limit.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RuleSetApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanCreateGetAndListRuleSetVersions() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String ruleSetKey = "EXPENSE_RULES_" + uniqueSuffix();

        JsonNode versionOne = createRuleSetVersion(adminToken, ruleSetKey, amountGreaterDsl(1000));
        JsonNode versionTwo = createRuleSetVersion(adminToken, ruleSetKey, amountGreaterDsl(2000));

        assertThat(versionOne.get("versionNo").asInt()).isEqualTo(1);
        assertThat(versionTwo.get("versionNo").asInt()).isEqualTo(2);

        mockMvc.perform(get("/api/rule-sets/" + ruleSetKey + "/versions/1")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ruleSetKey").value(ruleSetKey))
            .andExpect(jsonPath("$.versionNo").value(1));

        mockMvc.perform(get("/api/rule-sets/" + ruleSetKey + "/versions")
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].versionNo").value(2));
    }

    @Test
    void nonAdminIsForbiddenForRuleSetEndpoints() throws Exception {
        String requestorToken = loginAndExtractToken("requestor", "password");

        mockMvc.perform(post("/api/rule-sets/EXPENSE_ROUTING/versions")
                .header("Authorization", "Bearer " + requestorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dsl":{"field":"amount","op":">","value":1000}}
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/rule-sets/EXPENSE_ROUTING/versions")
                .header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/rule-sets/simulations")
                .header("Authorization", "Bearer " + requestorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(simulationPayload("EXPENSE_ROUTING", 1, 1500)))
            .andExpect(status().isForbidden());
    }

    @Test
    void simulationReturnsDecisionAndTrace() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String ruleSetKey = "SIM_RULES_" + uniqueSuffix();

        createRuleSetVersion(adminToken, ruleSetKey, amountGreaterDsl(1000));

        mockMvc.perform(post("/api/rule-sets/simulations")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(simulationPayload(ruleSetKey, 1, 1500)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ruleSetKey").value(ruleSetKey))
            .andExpect(jsonPath("$.versionNo").value(1))
            .andExpect(jsonPath("$.matched").value(true))
            .andExpect(jsonPath("$.traces").isArray())
            .andExpect(jsonPath("$.traces[0].expressionType").isNotEmpty());
    }

    @Test
    void unknownRuleSetVersionReturnsNotFound() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");

        mockMvc.perform(get("/api/rule-sets/MISSING_RULES/versions/1")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void invalidDslReturnsBadRequestWithDetails() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");

        mockMvc.perform(post("/api/rule-sets/INVALID_RULES/versions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dsl":{"field":"amount","op":"between","value":1000}}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.details[0].field").value("dsl.op"));
    }

    private JsonNode createRuleSetVersion(String adminToken, String ruleSetKey, String dslJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rule-sets/" + ruleSetKey + "/versions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"dsl\":" + dslJson + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.checksumSha256").isNotEmpty())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String loginAndExtractToken(String usernameOrEmail, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload(usernameOrEmail, password)))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String loginPayload(String usernameOrEmail, String password) {
        return "{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"" + password + "\"}";
    }

    private String simulationPayload(String ruleSetKey, int versionNo, int amount) {
        return """
            {
              "ruleSetKey":"%s",
              "versionNo":%d,
              "context":{
                "amount":%d,
                "department":"Finance",
                "requestType":"EXPENSE",
                "currency":"USD",
                "payload":{"code":"EXP-2026"}
              }
            }
            """.formatted(ruleSetKey, versionNo, amount);
    }

    private String amountGreaterDsl(int amount) {
        return """
            {"field":"amount","op":">","value":%d}
            """.formatted(amount);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
