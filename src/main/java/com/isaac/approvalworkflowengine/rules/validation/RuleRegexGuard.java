package com.isaac.approvalworkflowengine.rules.validation;

import com.isaac.approvalworkflowengine.shared.api.ApiErrorDetail;
import com.isaac.approvalworkflowengine.shared.error.BadRequestException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Component;

@Component
public class RuleRegexGuard {

    private static final int MAX_PATTERN_LENGTH = 256;
    private static final int MAX_INPUT_LENGTH = 4_000;

    public Pattern compile(String pattern, String fieldPath) {
        validatePattern(pattern, fieldPath);
        try {
            return Pattern.compile(pattern);
        } catch (PatternSyntaxException exception) {
            throw badRequest(fieldPath, "Invalid regex syntax");
        }
    }

    public void validateInput(String value, String fieldPath) {
        if (value != null && value.length() > MAX_INPUT_LENGTH) {
            throw badRequest(fieldPath, "Regex input exceeds max length of " + MAX_INPUT_LENGTH);
        }
    }

    public void validatePattern(String pattern, String fieldPath) {
        if (pattern == null || pattern.isBlank()) {
            throw badRequest(fieldPath, "Regex pattern must be non-empty");
        }

        if (pattern.length() > MAX_PATTERN_LENGTH) {
            throw badRequest(fieldPath, "Regex pattern exceeds max length of " + MAX_PATTERN_LENGTH);
        }

        if (containsLookBehind(pattern)) {
            throw badRequest(fieldPath, "Lookbehind constructs are not allowed");
        }

        if (containsBackReference(pattern)) {
            throw badRequest(fieldPath, "Backreference constructs are not allowed");
        }
    }

    private boolean containsLookBehind(String pattern) {
        return pattern.contains("(?<=") || pattern.contains("(?<!");
    }

    private boolean containsBackReference(String pattern) {
        for (int index = 0; index < pattern.length() - 1; index++) {
            if (pattern.charAt(index) == '\\' && Character.isDigit(pattern.charAt(index + 1))) {
                return true;
            }
        }
        return false;
    }

    private BadRequestException badRequest(String fieldPath, String reason) {
        return new BadRequestException(
            "Rule DSL is invalid",
            List.of(new ApiErrorDetail(fieldPath, reason))
        );
    }
}
