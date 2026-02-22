package com.isaac.approvalworkflowengine.workflowtemplate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowAssignmentInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowDefinitionInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowDefinitionResource;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowEdgeInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowGraphInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowJoinInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowNodeInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowRuleRefInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowSlaInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowVersionInput;
import com.isaac.approvalworkflowengine.workflowtemplate.api.WorkflowVersionResource;
import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowNodeType;
import com.isaac.approvalworkflowengine.workflowtemplate.checksum.WorkflowGraphChecksumService;
import com.isaac.approvalworkflowengine.workflowtemplate.model.WorkflowVersionStatus;
import com.isaac.approvalworkflowengine.rules.RuleSetLookup;
import com.isaac.approvalworkflowengine.workflowtemplate.WorkflowTemplateLookup;
import com.isaac.approvalworkflowengine.workflowtemplate.WorkflowTemplateRuntimeLookup;
import com.isaac.approvalworkflowengine.workflowtemplate.repository.WorkflowDefinitionJpaRepository;
import com.isaac.approvalworkflowengine.workflowtemplate.repository.WorkflowEdgeJpaRepository;
import com.isaac.approvalworkflowengine.workflowtemplate.repository.WorkflowNodeJpaRepository;
import com.isaac.approvalworkflowengine.workflowtemplate.repository.WorkflowVersionJpaRepository;
import com.isaac.approvalworkflowengine.workflowtemplate.repository.entity.WorkflowDefinitionEntity;
import com.isaac.approvalworkflowengine.workflowtemplate.repository.entity.WorkflowEdgeEntity;
import com.isaac.approvalworkflowengine.workflowtemplate.repository.entity.WorkflowNodeEntity;
import com.isaac.approvalworkflowengine.workflowtemplate.repository.entity.WorkflowVersionEntity;
import com.isaac.approvalworkflowengine.workflowtemplate.validation.WorkflowGraphValidator;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WorkflowTemplateService implements WorkflowTemplateLookup, WorkflowTemplateRuntimeLookup {

    private final WorkflowDefinitionJpaRepository workflowDefinitionJpaRepository;
    private final WorkflowVersionJpaRepository workflowVersionJpaRepository;
    private final WorkflowNodeJpaRepository workflowNodeJpaRepository;
    private final WorkflowEdgeJpaRepository workflowEdgeJpaRepository;
    private final WorkflowGraphValidator workflowGraphValidator;
    private final WorkflowGraphChecksumService workflowGraphChecksumService;
    private final RuleSetLookup ruleSetLookup;
    private final ObjectMapper objectMapper;

    public WorkflowTemplateService(
        WorkflowDefinitionJpaRepository workflowDefinitionJpaRepository,
        WorkflowVersionJpaRepository workflowVersionJpaRepository,
        WorkflowNodeJpaRepository workflowNodeJpaRepository,
        WorkflowEdgeJpaRepository workflowEdgeJpaRepository,
        WorkflowGraphValidator workflowGraphValidator,
        WorkflowGraphChecksumService workflowGraphChecksumService,
        RuleSetLookup ruleSetLookup,
        ObjectMapper objectMapper
    ) {
        this.workflowDefinitionJpaRepository = workflowDefinitionJpaRepository;
        this.workflowVersionJpaRepository = workflowVersionJpaRepository;
        this.workflowNodeJpaRepository = workflowNodeJpaRepository;
        this.workflowEdgeJpaRepository = workflowEdgeJpaRepository;
        this.workflowGraphValidator = workflowGraphValidator;
        this.workflowGraphChecksumService = workflowGraphChecksumService;
        this.ruleSetLookup = ruleSetLookup;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkflowDefinitionResource createDefinition(WorkflowDefinitionInput input, WorkflowTemplateActor actor) {
        String definitionKey = normalizeUpper(input.definitionKey());
        String requestType = normalizeUpper(input.requestType());

        if (workflowDefinitionJpaRepository.existsByDefinitionKey(definitionKey)) {
            throw new IllegalStateException("Workflow definition key already exists");
        }

        if (workflowDefinitionJpaRepository.existsByRequestType(requestType)) {
            throw new IllegalStateException("Workflow definition for request type already exists");
        }

        WorkflowDefinitionEntity definition = new WorkflowDefinitionEntity();
        definition.setId(UUID.randomUUID());
        definition.setDefinitionKey(definitionKey);
        definition.setName(input.name().trim());
        definition.setRequestType(requestType);
        definition.setOwnerUserId(actor.userId());
        definition.setAllowLoopback(Boolean.TRUE.equals(input.allowLoopback()));

        try {
            WorkflowDefinitionEntity saved = workflowDefinitionJpaRepository.save(definition);
            return toDefinitionResource(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Workflow definition conflicts with existing data", exception);
        }
    }

    @Transactional
    public WorkflowVersionResource createVersion(String definitionKey, WorkflowVersionInput input) {
        WorkflowDefinitionEntity definition = workflowDefinitionJpaRepository
            .findByDefinitionKey(normalizeUpper(definitionKey))
            .orElseThrow(() -> new NoSuchElementException("Workflow definition not found"));

        int nextVersion = workflowVersionJpaRepository
            .findTopByWorkflowDefinitionIdOrderByVersionNoDesc(definition.getId())
            .map(existing -> existing.getVersionNo() + 1)
            .orElse(1);

        WorkflowVersionEntity version = new WorkflowVersionEntity();
        version.setId(UUID.randomUUID());
        version.setWorkflowDefinitionId(definition.getId());
        version.setVersionNo(nextVersion);
        version.setStatus(WorkflowVersionStatus.DRAFT);
        version.setGraphJson(writeJson(input.graph()));

        try {
            WorkflowVersionEntity saved = workflowVersionJpaRepository.save(version);
            persistGraphStructure(saved.getId(), input.graph());
            return toVersionResource(saved, definition, input.graph());
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Workflow version creation conflicted with existing data", exception);
        }
    }

    @Transactional
    public WorkflowVersionResource activateVersion(UUID workflowVersionId, WorkflowTemplateActor actor) {
        WorkflowVersionEntity targetVersion = workflowVersionJpaRepository.findForUpdate(workflowVersionId)
            .orElseThrow(() -> new NoSuchElementException("Workflow version not found"));

        if (!targetVersion.getStatus().isMutable()) {
            throw new IllegalStateException("Only DRAFT workflow versions can be activated");
        }

        WorkflowDefinitionEntity definition = workflowDefinitionJpaRepository.findById(targetVersion.getWorkflowDefinitionId())
            .orElseThrow(() -> new NoSuchElementException("Workflow definition not found"));

        WorkflowGraphInput graph = readGraph(targetVersion.getGraphJson());
        workflowGraphValidator.validateForActivation(graph, definition.isAllowLoopback());
        validateGatewayRuleReferences(graph);

        String canonicalGraphJson = workflowGraphChecksumService.canonicalize(graph);
        String checksum = workflowGraphChecksumService.checksumSha256(canonicalGraphJson);

        Optional<WorkflowVersionEntity> activeVersion = workflowVersionJpaRepository
            .findByWorkflowDefinitionIdAndStatus(definition.getId(), WorkflowVersionStatus.ACTIVE);

        activeVersion
            .filter(existing -> !existing.getId().equals(targetVersion.getId()))
            .ifPresent(existing -> {
                existing.setStatus(WorkflowVersionStatus.RETIRED);
                workflowVersionJpaRepository.save(existing);
            });

        targetVersion.setStatus(WorkflowVersionStatus.ACTIVE);
        targetVersion.setGraphJson(canonicalGraphJson);
        targetVersion.setChecksumSha256(checksum);
        targetVersion.setActivatedAt(Instant.now());
        targetVersion.setActivatedByUserId(actor.userId());

        WorkflowVersionEntity saved = workflowVersionJpaRepository.saveAndFlush(targetVersion);
        return toVersionResource(saved, definition, readGraph(saved.getGraphJson()));
    }

    @Transactional(readOnly = true)
    public WorkflowVersionResource getVersion(UUID workflowVersionId) {
        WorkflowVersionEntity version = workflowVersionJpaRepository.findById(workflowVersionId)
            .orElseThrow(() -> new NoSuchElementException("Workflow version not found"));

        WorkflowDefinitionEntity definition = workflowDefinitionJpaRepository.findById(version.getWorkflowDefinitionId())
            .orElseThrow(() -> new NoSuchElementException("Workflow definition not found"));

        return toVersionResource(version, definition, readGraph(version.getGraphJson()));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<UUID> findActiveWorkflowVersionIdByRequestType(String requestType) {
        if (!StringUtils.hasText(requestType)) {
            return Optional.empty();
        }

        return workflowVersionJpaRepository.findVersionIdByRequestTypeAndStatus(
            requestType.trim(),
            WorkflowVersionStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<WorkflowTemplateRuntimeVersion> findRuntimeWorkflowVersion(UUID workflowVersionId) {
        if (workflowVersionId == null) {
            return Optional.empty();
        }

        WorkflowVersionEntity version = workflowVersionJpaRepository.findById(workflowVersionId).orElse(null);
        if (version == null) {
            return Optional.empty();
        }

        WorkflowDefinitionEntity definition = workflowDefinitionJpaRepository
            .findById(version.getWorkflowDefinitionId())
            .orElse(null);
        if (definition == null) {
            return Optional.empty();
        }

        return Optional.of(new WorkflowTemplateRuntimeVersion(
            version.getId(),
            definition.getDefinitionKey(),
            readGraph(version.getGraphJson())
        ));
    }

    private void persistGraphStructure(UUID workflowVersionId, WorkflowGraphInput graph) {
        Instant now = Instant.now();

        workflowNodeJpaRepository.deleteByWorkflowVersionId(workflowVersionId);
        workflowEdgeJpaRepository.deleteByWorkflowVersionId(workflowVersionId);

        List<WorkflowNodeEntity> nodes = graph.nodes().stream()
            .map(node -> new WorkflowNodeEntity(
                UUID.randomUUID(),
                workflowVersionId,
                node.id().trim(),
                node.type().name(),
                buildNodeConfigJson(node),
                now
            ))
            .toList();

        List<WorkflowEdgeEntity> edges = graph.edges().stream()
            .map(edge -> new WorkflowEdgeEntity(
                UUID.randomUUID(),
                workflowVersionId,
                edge.from().trim(),
                edge.to().trim(),
                edge.condition() == null || edge.condition().isEmpty() ? null : writeJson(edge.condition()),
                now
            ))
            .toList();

        workflowNodeJpaRepository.saveAll(nodes);
        workflowEdgeJpaRepository.saveAll(edges);
    }

    private String buildNodeConfigJson(WorkflowNodeInput node) {
        Map<String, Object> config = new LinkedHashMap<>();

        WorkflowAssignmentInput assignment = node.assignment();
        if (assignment != null) {
            Map<String, Object> assignmentConfig = new LinkedHashMap<>();
            assignmentConfig.put("strategy", assignment.strategy().name());
            if (StringUtils.hasText(assignment.role())) {
                assignmentConfig.put("role", assignment.role().trim());
            }
            if (StringUtils.hasText(assignment.userId())) {
                assignmentConfig.put("userId", assignment.userId().trim());
            }
            if (StringUtils.hasText(assignment.expression())) {
                assignmentConfig.put("expression", assignment.expression().trim());
            }
            config.put("assignment", assignmentConfig);
        }

        WorkflowRuleRefInput ruleRef = node.ruleRef();
        if (ruleRef != null) {
            config.put("ruleRef", Map.of(
                "ruleSetKey", ruleRef.ruleSetKey().trim(),
                "version", ruleRef.version()
            ));
        }

        WorkflowJoinInput join = node.join();
        if (join != null) {
            Map<String, Object> joinConfig = new LinkedHashMap<>();
            joinConfig.put("policy", join.policy().name());
            if (join.quorum() != null) {
                joinConfig.put("quorum", join.quorum());
            }
            config.put("join", joinConfig);
        }

        WorkflowSlaInput sla = node.sla();
        if (sla != null && sla.dueInHours() != null) {
            config.put("sla", Map.of("dueInHours", sla.dueInHours()));
        }

        if (config.isEmpty()) {
            return writeJson(Map.of());
        }

        return writeJson(config);
    }

    private WorkflowDefinitionResource toDefinitionResource(WorkflowDefinitionEntity definition) {
        return new WorkflowDefinitionResource(
            definition.getId(),
            definition.getDefinitionKey(),
            definition.getName(),
            definition.getRequestType(),
            definition.isAllowLoopback()
        );
    }

    private WorkflowVersionResource toVersionResource(
        WorkflowVersionEntity version,
        WorkflowDefinitionEntity definition,
        WorkflowGraphInput graph
    ) {
        return new WorkflowVersionResource(
            version.getId(),
            definition.getDefinitionKey(),
            version.getVersionNo(),
            version.getStatus(),
            graph,
            version.getChecksumSha256(),
            version.getActivatedAt()
        );
    }

    private WorkflowGraphInput readGraph(String graphJson) {
        try {
            return objectMapper.readValue(graphJson, WorkflowGraphInput.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize workflow graph", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize workflow graph", exception);
        }
    }

    private String normalizeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Required value is missing");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void validateGatewayRuleReferences(WorkflowGraphInput graph) {
        for (WorkflowNodeInput node : graph.nodes()) {
            if (node.type() != WorkflowNodeType.GATEWAY) {
                continue;
            }

            WorkflowRuleRefInput ruleRef = node.ruleRef();
            if (ruleRef == null) {
                continue;
            }

            String ruleSetKey = normalizeUpper(ruleRef.ruleSetKey());
            int version = ruleRef.version();
            boolean exists = ruleSetLookup.exists(ruleSetKey, version);
            if (!exists) {
                throw new IllegalStateException(
                    "Referenced rule set version not found for gateway node "
                        + node.id() + ": " + ruleSetKey + " v" + version
                );
            }
        }
    }
}
