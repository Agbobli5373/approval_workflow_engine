package com.isaac.approvalworkflowengine.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorHealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void livenessEndpointReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void readinessEndpointIncludesDatabaseHealth() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void readinessEndpointIncludesDatabaseHealthForAuthenticatedRequest() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness").with(user("requestor").roles("REQUESTOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.db.status").value("UP"));
    }
}
