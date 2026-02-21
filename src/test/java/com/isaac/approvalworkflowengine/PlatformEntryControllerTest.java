package com.isaac.approvalworkflowengine;

import static org.assertj.core.api.Assertions.assertThat;
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
    void rootRedirectsToSwaggerUi() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    void iconRequestsReturnNoContent() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/apple-touch-icon.png"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/apple-touch-icon-precomposed.png"))
            .andExpect(status().isNoContent());
    }

    @Test
    void swaggerUiEndpointIsAvailable() throws Exception {
        MvcResult result = mockMvc.perform(get("/swagger-ui/index.html"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(200, 302);
    }
}
