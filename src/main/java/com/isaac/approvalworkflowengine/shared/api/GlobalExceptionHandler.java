package com.isaac.approvalworkflowengine.shared.api;

import com.isaac.approvalworkflowengine.shared.context.CorrelationIdContext;
import com.isaac.approvalworkflowengine.shared.error.DomainConflictException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps framework and domain exceptions into a deterministic API error envelope.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<ApiErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> new ApiErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
            .collect(Collectors.toList());

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Validation failed",
            details,
            request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        List<ApiErrorDetail> details = exception.getConstraintViolations().stream()
            .map(violation -> new ApiErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
            .collect(Collectors.toList());

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Validation failed",
            details,
            request
        );
    }

    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MissingRequestHeaderException.class,
        HttpMessageNotReadableException.class,
        BindException.class,
        MethodArgumentTypeMismatchException.class,
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception exception, HttpServletRequest request) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "BAD_REQUEST",
            "Request could not be processed",
            List.of(),
            request
        );
    }

    @ExceptionHandler({DomainConflictException.class, IllegalStateException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException exception, HttpServletRequest request) {
        return buildResponse(
            HttpStatus.CONFLICT,
            "CONFLICT",
            exception.getMessage(),
            List.of(),
            request
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLockConflict(
        ObjectOptimisticLockingFailureException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.CONFLICT,
            "CONFLICT",
            "Concurrent update conflict",
            List.of(),
            request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
        AuthenticationException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            "Authentication required",
            List.of(),
            request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(
        AccessDeniedException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.FORBIDDEN,
            "FORBIDDEN",
            "Access denied",
            List.of(),
            request
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(
        NoSuchElementException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.NOT_FOUND,
            "NOT_FOUND",
            exception.getMessage(),
            List.of(),
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception while processing request", exception);
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "Unexpected server error",
            List.of(),
            request
        );
    }

    private ResponseEntity<ApiError> buildResponse(
        HttpStatus status,
        String code,
        String message,
        List<ApiErrorDetail> details,
        HttpServletRequest request
    ) {
        ApiError error = new ApiError(code, message, resolveCorrelationId(request), details);
        return ResponseEntity.status(status).body(error);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        Object attribute = request.getAttribute(CorrelationIdContext.REQUEST_ATTRIBUTE);
        if (attribute instanceof String correlationId && !correlationId.isBlank()) {
            return correlationId;
        }

        String mdcCorrelationId = MDC.get(CorrelationIdContext.MDC_KEY);
        if (mdcCorrelationId != null && !mdcCorrelationId.isBlank()) {
            return mdcCorrelationId;
        }

        return UUID.randomUUID().toString();
    }
}
