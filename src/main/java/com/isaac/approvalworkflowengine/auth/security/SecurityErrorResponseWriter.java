package com.isaac.approvalworkflowengine.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.approvalworkflowengine.shared.api.ApiError;
import com.isaac.approvalworkflowengine.shared.context.CorrelationIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
        HttpServletRequest request,
        HttpServletResponse response,
        HttpStatus status,
        String code,
        String message
    ) throws IOException {
        String correlationId = resolveCorrelationId(request);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(CorrelationIdContext.HEADER_NAME, correlationId);

        ApiError payload = new ApiError(code, message, correlationId, List.of());
        objectMapper.writeValue(response.getWriter(), payload);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdContext.REQUEST_ATTRIBUTE);
        if (value instanceof String correlationId && !correlationId.isBlank()) {
            return correlationId;
        }
        return UUID.randomUUID().toString();
    }
}
