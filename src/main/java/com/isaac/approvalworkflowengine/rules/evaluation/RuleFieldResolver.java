package com.isaac.approvalworkflowengine.rules.evaluation;

import com.isaac.approvalworkflowengine.rules.model.RuleEvaluationContext;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RuleFieldResolver {

    Object resolve(String field, RuleEvaluationContext context) {
        if (!StringUtils.hasText(field)) {
            return null;
        }

        return switch (field) {
            case "amount" -> context.amount();
            case "department" -> context.department();
            case "requestType" -> context.requestType();
            case "currency" -> context.currency();
            case "payload" -> context.payload();
            default -> resolvePayloadPath(field, context.payload());
        };
    }

    private Object resolvePayloadPath(String field, Map<String, Object> payload) {
        if (!field.startsWith("payload.")) {
            return null;
        }

        String[] segments = field.substring("payload.".length()).split("\\.");
        Object current = payload;

        for (String segment : segments) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }

        return current;
    }
}
