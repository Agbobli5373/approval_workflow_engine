package com.isaac.approvalworkflowengine.requests.api;

import java.util.List;

public record PagedRequestResource(
    List<RequestResource> items,
    PageMetadata page
) {
}
