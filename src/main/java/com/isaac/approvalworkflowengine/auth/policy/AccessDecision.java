package com.isaac.approvalworkflowengine.auth.policy;

public record AccessDecision(boolean allowed, String reasonCode) {

    public static AccessDecision allow(String reasonCode) {
        return new AccessDecision(true, reasonCode);
    }

    public static AccessDecision deny(String reasonCode) {
        return new AccessDecision(false, reasonCode);
    }
}
