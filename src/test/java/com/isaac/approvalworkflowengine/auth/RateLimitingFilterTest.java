package com.isaac.approvalworkflowengine.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "app.security.rate-limit.authenticated-limit=100",
    "app.security.rate-limit.anonymous-limit=2",
    "app.security.rate-limit.window-seconds=60",
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestsAboveConfiguredWindowLimitReceive429() throws Exception {
        String body = "{\"usernameOrEmail\":\"admin\",\"password\":\"wrong\"}";

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
            .andExpect(header().exists("Retry-After"));
    }
}
