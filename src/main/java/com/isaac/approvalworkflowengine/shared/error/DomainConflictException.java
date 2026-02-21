package com.isaac.approvalworkflowengine.shared.error;

/**
 * Placeholder exception for domain state conflicts.
 */
public class DomainConflictException extends RuntimeException {

    public DomainConflictException(String message) {
        super(message);
    }
}
