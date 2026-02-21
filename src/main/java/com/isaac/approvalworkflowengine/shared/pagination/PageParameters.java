package com.isaac.approvalworkflowengine.shared.pagination;

/**
 * Common pagination defaults and normalization helpers.
 */
public record PageParameters(int page, int size, String sort) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    public static PageParameters of(Integer page, Integer size, String sort) {
        int normalizedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new PageParameters(normalizedPage, normalizedSize, sort);
    }
}
