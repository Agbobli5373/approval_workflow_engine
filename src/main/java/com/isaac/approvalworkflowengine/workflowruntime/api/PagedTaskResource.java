package com.isaac.approvalworkflowengine.workflowruntime.api;

import java.util.List;

public record PagedTaskResource(
    List<TaskResource> items,
    TaskPageMetadata page
) {
}
