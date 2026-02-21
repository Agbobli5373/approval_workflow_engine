package com.isaac.approvalworkflowengine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.isaac.approvalworkflowengine.shared.context.CorrelationIdContext;
import com.isaac.approvalworkflowengine.shared.error.DomainConflictException;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ErrorEnvelopeAndCorrelationTest.TestController.class)
class ErrorEnvelopeAndCorrelationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationErrorsUseStandardEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/platform-test/validation").param("value", ""))
            .andExpect(status().isBadRequest())
            .andExpect(header().exists(CorrelationIdContext.HEADER_NAME))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.correlationId").isNotEmpty())
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.details[0].field").isNotEmpty())
            .andExpect(jsonPath("$.details[0].reason").isNotEmpty());
    }

    @Test
    void unexpectedErrorsUseInternalErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/platform-test/runtime"))
            .andExpect(status().isInternalServerError())
            .andExpect(header().exists(CorrelationIdContext.HEADER_NAME))
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void conflictPlaceholderMapsToConflictError() throws Exception {
        mockMvc.perform(get("/api/v1/platform-test/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void generatesCorrelationIdWhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/platform-test/ok"))
            .andExpect(status().isOk())
            .andExpect(header().exists(CorrelationIdContext.HEADER_NAME))
            .andExpect(header().string(CorrelationIdContext.HEADER_NAME, org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())));
    }

    @Test
    void propagatesIncomingCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/platform-test/ok").header(CorrelationIdContext.HEADER_NAME, "corr-e0-123"))
            .andExpect(status().isOk())
            .andExpect(header().string(CorrelationIdContext.HEADER_NAME, "corr-e0-123"));
    }

    @RestController
    @Validated
    @RequestMapping("/api/v1/platform-test")
    static class TestController {

        @GetMapping("/ok")
        String ok() {
            return "ok";
        }

        @GetMapping("/validation")
        String validation(@RequestParam @NotBlank String value) {
            return value;
        }

        @GetMapping("/runtime")
        String runtime() {
            throw new RuntimeException("simulated runtime failure");
        }

        @GetMapping("/conflict")
        String conflict() {
            throw new DomainConflictException("simulated conflict");
        }
    }
}
