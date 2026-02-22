package com.isaac.approvalworkflowengine.rules.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RuleOperator {
    EQ("=="),
    NE("!="),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    IN("in"),
    CONTAINS("contains"),
    MATCHES("matches");

    private static final Map<String, RuleOperator> BY_TOKEN = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(RuleOperator::token, Function.identity()));

    private final String token;

    RuleOperator(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    public static RuleOperator fromToken(String token) {
        return BY_TOKEN.get(token);
    }
}
