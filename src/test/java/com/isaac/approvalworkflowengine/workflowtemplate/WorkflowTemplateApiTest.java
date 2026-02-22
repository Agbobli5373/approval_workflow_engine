package com.isaac.approvalworkflowengine.workflowtemplate;

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
class WorkflowTemplateApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanCreateWorkflowDefinition() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String suffix = uniqueSuffix();

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload("WF_" + suffix, "Workflow " + suffix, "REQ_" + suffix, false)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.definitionKey").value("WF_" + suffix))
            .andExpect(jsonPath("$.requestType").value("REQ_" + suffix));
    }

    @Test
    void duplicateDefinitionKeyReturnsConflict() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String suffix = uniqueSuffix();
        String definitionKey = "DUP_KEY_" + suffix;

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload(definitionKey, "Workflow A", "REQ_A_" + suffix, false)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload(definitionKey, "Workflow B", "REQ_B_" + suffix, false)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void duplicateRequestTypeReturnsConflict() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String suffix = uniqueSuffix();
        String requestType = "REQ_DUP_" + suffix;

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload("WF_A_" + suffix, "Workflow A", requestType, false)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload("WF_B_" + suffix, "Workflow B", requestType, false)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void createVersionIncrementsVersionNumber() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String suffix = uniqueSuffix();
        String definitionKey = "WF_VER_" + suffix;

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload(definitionKey, "Versioned workflow", "REQ_VER_" + suffix, false)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/workflow-definitions/" + definitionKey + "/versions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(versionPayload(validGraphJson())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.versionNo").value(1))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(post("/api/workflow-definitions/" + definitionKey + "/versions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(versionPayload(validGraphJson())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.versionNo").value(2))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void activationOfValidGraphSetsActiveAndChecksum() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String suffix = uniqueSuffix();
        String definitionKey = "WF_ACT_" + suffix;

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload(definitionKey, "Activation workflow", "REQ_ACT_" + suffix, false)))
            .andExpect(status().isCreated());

        JsonNode createdVersion = createVersion(adminToken, definitionKey, validGraphJson());
        String versionId = createdVersion.get("id").asText();

        mockMvc.perform(post("/api/workflow-versions/" + versionId + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.checksumSha256").isNotEmpty());
    }

    @Test
    void activationRejectsDanglingEdges() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        JsonNode version = createDefinitionAndVersion(adminToken, "WF_DANGLE", "REQ_DANGLE", danglingEdgeGraphJson());

        mockMvc.perform(post("/api/workflow-versions/" + version.get("id").asText() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void activationRejectsMissingStartOrEnd() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        JsonNode version = createDefinitionAndVersion(adminToken, "WF_NO_START", "REQ_NO_START", missingStartGraphJson());

        mockMvc.perform(post("/api/workflow-versions/" + version.get("id").asText() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void activationRejectsCycleWhenLoopbackDisabled() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        JsonNode version = createDefinitionAndVersion(adminToken, "WF_CYCLE", "REQ_CYCLE", cycleGraphJson());

        mockMvc.perform(post("/api/workflow-versions/" + version.get("id").asText() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void activationRejectsInvalidJoinQuorum() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        JsonNode version = createDefinitionAndVersion(adminToken, "WF_JOIN", "REQ_JOIN", invalidJoinGraphJson());

        mockMvc.perform(post("/api/workflow-versions/" + version.get("id").asText() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void activationRejectsMalformedGatewayRuleRef() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        JsonNode version = createDefinitionAndVersion(adminToken, "WF_RULE", "REQ_RULE", malformedRuleRefGraphJson());

        mockMvc.perform(post("/api/workflow-versions/" + version.get("id").asText() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void activationRejectsGatewayRuleRefWhenRuleSetVersionMissing() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String suffix = uniqueSuffix();
        String missingRuleSetKey = "MISSING_RULESET_" + suffix;
        JsonNode version = createDefinitionAndVersion(
            adminToken,
            "WF_RULE_MISSING",
            "REQ_RULE_MISSING",
            gatewayRuleRefGraphJson(missingRuleSetKey, 1)
        );

        mockMvc.perform(post("/api/workflow-versions/" + version.get("id").asText() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Referenced rule set version not found")));
    }

    @Test
    void activationSucceedsGatewayRuleRefWhenRuleSetVersionExists() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String suffix = uniqueSuffix();
        String ruleSetKey = "EXPENSE_ROUTE_" + suffix;

        mockMvc.perform(post("/api/rule-sets/" + ruleSetKey + "/versions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dsl":{"field":"amount","op":">","value":1000}}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.versionNo").value(1));

        JsonNode version = createDefinitionAndVersion(
            adminToken,
            "WF_RULE_PRESENT",
            "REQ_RULE_PRESENT",
            gatewayRuleRefGraphJson(ruleSetKey, 1)
        );

        mockMvc.perform(post("/api/workflow-versions/" + version.get("id").asText() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void activatingNewVersionAutoRetiresExistingActiveVersion() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String suffix = uniqueSuffix();
        String definitionKey = "WF_RETIRE_" + suffix;

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload(definitionKey, "Retire workflow", "REQ_RETIRE_" + suffix, false)))
            .andExpect(status().isCreated());

        JsonNode versionOne = createVersion(adminToken, definitionKey, validGraphJson());
        JsonNode versionTwo = createVersion(adminToken, definitionKey, validGraphJson());

        mockMvc.perform(post("/api/workflow-versions/" + versionOne.get("id").asText() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/workflow-versions/" + versionTwo.get("id").asText() + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/workflow-versions/" + versionOne.get("id").asText())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RETIRED"));
    }

    @Test
    void nonAdminIsForbiddenForWorkflowTemplateEndpoints() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String requestorToken = loginAndExtractToken("requestor", "password");
        String suffix = uniqueSuffix();
        String definitionKey = "WF_SEC_" + suffix;

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + requestorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload(definitionKey, "Denied", "REQ_SEC_" + suffix, false)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload(definitionKey, "Allowed", "REQ_SEC_" + suffix, false)))
            .andExpect(status().isCreated());

        JsonNode createdVersion = createVersion(adminToken, definitionKey, validGraphJson());
        String versionId = createdVersion.get("id").asText();

        mockMvc.perform(post("/api/workflow-definitions/" + definitionKey + "/versions")
                .header("Authorization", "Bearer " + requestorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(versionPayload(validGraphJson())))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/workflow-versions/" + versionId + "/activate")
                .header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/workflow-versions/" + versionId)
                .header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void getUnknownVersionReturnsNotFound() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");

        mockMvc.perform(get("/api/workflow-versions/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private JsonNode createDefinitionAndVersion(
        String adminToken,
        String keyPrefix,
        String requestTypePrefix,
        String graphJson
    ) throws Exception {
        String suffix = uniqueSuffix();
        String definitionKey = keyPrefix + "_" + suffix;
        String requestType = requestTypePrefix + "_" + suffix;

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(definitionPayload(definitionKey, "Workflow " + suffix, requestType, false)))
            .andExpect(status().isCreated());

        return createVersion(adminToken, definitionKey, graphJson);
    }

    private JsonNode createVersion(String adminToken, String definitionKey, String graphJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workflow-definitions/" + definitionKey + "/versions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(versionPayload(graphJson)))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("id").asText()).isNotBlank();
        return json;
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

    private String definitionPayload(String key, String name, String requestType, boolean allowLoopback) {
        return """
            {
              "definitionKey":"%s",
              "name":"%s",
              "requestType":"%s",
              "allowLoopback":%s
            }
            """.formatted(key, name, requestType, allowLoopback);
    }

    private String versionPayload(String graphJson) {
        return "{\"graph\":" + graphJson + "}";
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String validGraphJson() {
        return """
            {
              "nodes":[
                {"id":"start","type":"START"},
                {"id":"manager","type":"APPROVAL","assignment":{"strategy":"ROLE","role":"MANAGER"}},
                {"id":"end","type":"END"}
              ],
              "edges":[
                {"from":"start","to":"manager"},
                {"from":"manager","to":"end"}
              ]
            }
            """;
    }

    private String danglingEdgeGraphJson() {
        return """
            {
              "nodes":[
                {"id":"start","type":"START"},
                {"id":"manager","type":"APPROVAL","assignment":{"strategy":"ROLE","role":"MANAGER"}},
                {"id":"end","type":"END"}
              ],
              "edges":[
                {"from":"start","to":"manager"},
                {"from":"manager","to":"missing"}
              ]
            }
            """;
    }

    private String missingStartGraphJson() {
        return """
            {
              "nodes":[
                {"id":"manager","type":"APPROVAL","assignment":{"strategy":"ROLE","role":"MANAGER"}},
                {"id":"end","type":"END"}
              ],
              "edges":[
                {"from":"manager","to":"end"}
              ]
            }
            """;
    }

    private String cycleGraphJson() {
        return """
            {
              "nodes":[
                {"id":"start","type":"START"},
                {"id":"a","type":"APPROVAL","assignment":{"strategy":"ROLE","role":"MANAGER"}},
                {"id":"b","type":"APPROVAL","assignment":{"strategy":"ROLE","role":"FINANCE"}},
                {"id":"end","type":"END"}
              ],
              "edges":[
                {"from":"start","to":"a"},
                {"from":"a","to":"b"},
                {"from":"b","to":"a"},
                {"from":"b","to":"end"}
              ]
            }
            """;
    }

    private String invalidJoinGraphJson() {
        return """
            {
              "nodes":[
                {"id":"start","type":"START"},
                {"id":"join","type":"JOIN","join":{"policy":"QUORUM","quorum":2}},
                {"id":"end","type":"END"}
              ],
              "edges":[
                {"from":"start","to":"join"},
                {"from":"join","to":"end"}
              ]
            }
            """;
    }

    private String malformedRuleRefGraphJson() {
        return """
            {
              "nodes":[
                {"id":"start","type":"START"},
                {"id":"gate","type":"GATEWAY","ruleRef":{"ruleSetKey":"expense-routing","version":0}},
                {"id":"end","type":"END"}
              ],
              "edges":[
                {"from":"start","to":"gate"},
                {"from":"gate","to":"end"}
              ]
            }
            """;
    }

    private String gatewayRuleRefGraphJson(String ruleSetKey, int version) {
        return """
            {
              "nodes":[
                {"id":"start","type":"START"},
                {"id":"gate","type":"GATEWAY","ruleRef":{"ruleSetKey":"%s","version":%d}},
                {"id":"end","type":"END"}
              ],
              "edges":[
                {"from":"start","to":"gate"},
                {"from":"gate","to":"end"}
              ]
            }
            """.formatted(ruleSetKey, version);
    }
}
