package com.isaac.approvalworkflowengine.users;

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
class DelegationApiTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("a0d11f04-2e54-4b0e-bf14-7d9e05cbef4a");
    private static final UUID REQUESTOR_USER_ID = UUID.fromString("5ad15712-2c98-4b9d-8f1d-6baf6a4f6d78");
    private static final UUID APPROVER_USER_ID = UUID.fromString("6f6ea9ed-4c1a-4302-a7ab-2c9f4bf4384f");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void approverCanCreateListAndRevokeOwnDelegation() throws Exception {
        String approverToken = loginAndExtractToken("approver");
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant validFrom = now.minus(10, ChronoUnit.MINUTES);
        Instant validUntil = now.plus(2, ChronoUnit.HOURS);

        String createPayload = """
            {
              "delegateUserId":"%s",
              "requestType":"EXPENSE",
              "department":"Finance",
              "role":"APPROVER",
              "allScope":false,
              "validFrom":"%s",
              "validUntil":"%s"
            }
            """.formatted(REQUESTOR_USER_ID, validFrom, validUntil);

        MvcResult createResult = mockMvc.perform(post("/api/delegations")
                .header("Authorization", "Bearer " + approverToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.delegatorUserId").value(APPROVER_USER_ID.toString()))
            .andExpect(jsonPath("$.delegateUserId").value(REQUESTOR_USER_ID.toString()))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();

        UUID delegationId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/delegations")
                .param("asDelegator", "true")
                .header("Authorization", "Bearer " + approverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNotEmpty());

        mockMvc.perform(post("/api/delegations/" + delegationId + "/revoke")
                .header("Authorization", "Bearer " + approverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(delegationId.toString()))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.revokedAt").isNotEmpty());

        Integer activeCount = jdbcTemplate.queryForObject(
            "select count(*) from delegations where id = ? and active = true",
            Integer.class,
            delegationId
        );
        assertThat(activeCount).isEqualTo(0);
    }

    @Test
    void requestorCannotCreateDelegationForAnotherDelegator() throws Exception {
        String requestorToken = loginAndExtractToken("requestor");
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        String createPayload = """
            {
              "delegatorUserId":"%s",
              "delegateUserId":"%s",
              "requestType":"EXPENSE",
              "role":"APPROVER",
              "allScope":false,
              "validFrom":"%s",
              "validUntil":"%s"
            }
            """.formatted(APPROVER_USER_ID, ADMIN_USER_ID, now.minus(5, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/delegations")
                .header("Authorization", "Bearer " + requestorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanCreateDelegationForAnotherDelegator() throws Exception {
        String adminToken = loginAndExtractToken("admin");
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        String createPayload = """
            {
              "delegatorUserId":"%s",
              "delegateUserId":"%s",
              "requestType":"PROCUREMENT",
              "role":"APPROVER",
              "allScope":false,
              "validFrom":"%s",
              "validUntil":"%s"
            }
            """.formatted(APPROVER_USER_ID, REQUESTOR_USER_ID, now.minus(5, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/delegations")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.delegatorUserId").value(APPROVER_USER_ID.toString()))
            .andExpect(jsonPath("$.delegateUserId").value(REQUESTOR_USER_ID.toString()));
    }

    private String loginAndExtractToken(String usernameOrEmail) throws Exception {
        String payload = "{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"password\"}";

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("accessToken").asText();
    }
}
