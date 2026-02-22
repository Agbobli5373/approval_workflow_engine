package com.isaac.approvalworkflowengine.workflowtemplate.api;

import com.isaac.approvalworkflowengine.workflowtemplate.service.WorkflowTemplateActor;
import com.isaac.approvalworkflowengine.workflowtemplate.service.WorkflowTemplateService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(path = "/api", version = "1.0")
@PreAuthorize("hasRole('WORKFLOW_ADMIN')")
public class WorkflowTemplateController {

    private final WorkflowTemplateService workflowTemplateService;

    public WorkflowTemplateController(WorkflowTemplateService workflowTemplateService) {
        this.workflowTemplateService = workflowTemplateService;
    }

    @PostMapping("/workflow-definitions")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDefinitionResource createDefinition(
        @Valid @RequestBody WorkflowDefinitionInput input,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workflowTemplateService.createDefinition(input, WorkflowTemplateActor.fromJwt(jwt));
    }

    @PostMapping("/workflow-definitions/{definitionKey}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowVersionResource createVersion(
        @PathVariable String definitionKey,
        @Valid @RequestBody WorkflowVersionInput input
    ) {
        return workflowTemplateService.createVersion(definitionKey, input);
    }

    @PostMapping("/workflow-versions/{workflowVersionId}/activate")
    public WorkflowVersionResource activateVersion(
        @PathVariable UUID workflowVersionId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return workflowTemplateService.activateVersion(workflowVersionId, WorkflowTemplateActor.fromJwt(jwt));
    }

    @GetMapping("/workflow-versions/{workflowVersionId}")
    public WorkflowVersionResource getVersion(@PathVariable UUID workflowVersionId) {
        return workflowTemplateService.getVersion(workflowVersionId);
    }
}
