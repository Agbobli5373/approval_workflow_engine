package com.isaac.approvalworkflowengine.shared.context;

/**
 * Correlation id constants shared across web and error handling layers.
 */
public final class CorrelationIdContext {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String REQUEST_ATTRIBUTE = CorrelationIdContext.class.getName() + ".value";

    private CorrelationIdContext() {
    }
}
