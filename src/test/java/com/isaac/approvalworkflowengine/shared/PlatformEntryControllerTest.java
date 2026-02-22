package com.isaac.approvalworkflowengine.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rootRedirectsToSwaggerUiForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/").with(user("admin").roles("WORKFLOW_ADMIN")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    void iconRequestsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/apple-touch-icon.png"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/apple-touch-icon-precomposed.png"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerUiEndpointIsPublicInLocalAuthMode() throws Exception {
        MvcResult result = mockMvc.perform(get("/swagger-ui/index.html"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(200, 302);
    }

    @Test
    void swaggerUiEndpointIsAvailableForAdmin() throws Exception {
        MvcResult result = mockMvc.perform(get("/swagger-ui/index.html").with(user("admin").roles("WORKFLOW_ADMIN")))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(200, 302);
    }
}
