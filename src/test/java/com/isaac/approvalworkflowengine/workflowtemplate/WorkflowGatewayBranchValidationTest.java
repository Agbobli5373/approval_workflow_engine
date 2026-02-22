package com.isaac.approvalworkflowengine.workflowtemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class WorkflowGatewayBranchValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void activationFailsWhenGatewayBranchLabelsAreMissing() throws Exception {
        String adminToken = loginAndExtractToken("admin", "password");
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String definitionKey = "WF_GATE_BRANCH_" + suffix;

        mockMvc.perform(post("/api/workflow-definitions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "definitionKey":"%s",
                      "name":"Gateway Branch Validation",
                      "requestType":"REQ_GATE_%s",
                      "allowLoopback":false
                    }
                    """.formatted(definitionKey, suffix)))
            .andExpect(status().isCreated());

        MvcResult versionResult = mockMvc.perform(post("/api/workflow-definitions/" + definitionKey + "/versions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "graph":{
                        "nodes":[
                          {"id":"start","type":"START"},
                          {"id":"gate","type":"GATEWAY","ruleRef":{"ruleSetKey":"EXPENSE_ROUTING","version":1}},
                          {"id":"end","type":"END"}
                        ],
                        "edges":[
                          {"from":"start","to":"gate"},
                          {"from":"gate","to":"end"},
                          {"from":"gate","to":"end","condition":{"branch":true}}
                        ]
                      }
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn();

        String versionId = objectMapper.readTree(versionResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/workflow-versions/" + versionId + "/activate")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("condition.branch")));
    }

    private String loginAndExtractToken(String usernameOrEmail, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
