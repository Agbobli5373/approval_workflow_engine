package com.isaac.approvalworkflowengine.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiVersioningTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginEndpointAcceptsApiVersionHeader() throws Exception {
        String payload = "{\"usernameOrEmail\":\"admin\",\"password\":\"password\"}";

        mockMvc.perform(
                post("/api/auth/login")
                    .header("API-Version", "1.0")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
            .andExpect(status().isOk());
    }
}
