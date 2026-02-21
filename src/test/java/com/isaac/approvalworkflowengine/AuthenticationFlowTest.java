package com.isaac.approvalworkflowengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void protectedEndpointRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void validLoginReturnsBearerToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload("admin", "password")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn();

        String token = extractToken(result);
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.roles[0]").value("WORKFLOW_ADMIN"));
    }

    @Test
    void invalidPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload("admin", "wrong-password")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void inactiveUserCannotLogin() throws Exception {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
            """
            insert into users (id, external_subject, email, display_name, department, employee_id, password_hash, active, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
            """,
            userId,
            "inactive-user",
            "inactive@local.approval",
            "Inactive User",
            "Finance",
            "EMP-INACTIVE",
            "$2y$10$UwrJ5Qc/3Uz/X9hmFRuDKOczRzFLcmPcob9eNq4R1qdFpPt7VrIQW",
            false
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload("inactive-user", "password")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void swaggerDocsArePublicInLocalAuthMode() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk());
    }

    @Test
    void logoutRevokesAccessToken() throws Exception {
        String requestorToken = loginAndExtractToken("requestor", "password");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + requestorToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unauthorizedResponsesCarryCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().exists("X-Correlation-Id"));
    }

    private String loginAndExtractToken(String usernameOrEmail, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload(usernameOrEmail, password)))
            .andExpect(status().isOk())
            .andReturn();

        return extractToken(result);
    }

    private String extractToken(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("accessToken").asText();
    }

    private String loginPayload(String usernameOrEmail, String password) {
        return "{\"usernameOrEmail\":\"" + usernameOrEmail + "\",\"password\":\"" + password + "\"}";
    }
}
