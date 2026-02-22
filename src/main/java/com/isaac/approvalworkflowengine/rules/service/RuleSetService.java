package com.isaac.approvalworkflowengine.rules.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.approvalworkflowengine.rules.RuleSetLookup;
import com.isaac.approvalworkflowengine.rules.RuleSetRuntimeEvaluator;
import com.isaac.approvalworkflowengine.rules.api.PagedRuleSetVersionResource;
import com.isaac.approvalworkflowengine.rules.api.RuleEvaluationContextInput;
import com.isaac.approvalworkflowengine.rules.api.RuleEvaluationTraceResource;
import com.isaac.approvalworkflowengine.rules.api.RulePageMetadata;
import com.isaac.approvalworkflowengine.rules.api.RuleSetVersionInput;
import com.isaac.approvalworkflowengine.rules.api.RuleSetVersionResource;
import com.isaac.approvalworkflowengine.rules.api.RuleSimulationRequest;
import com.isaac.approvalworkflowengine.rules.api.RuleSimulationResponse;
import com.isaac.approvalworkflowengine.rules.checksum.RuleDslChecksumService;
import com.isaac.approvalworkflowengine.rules.dsl.RuleDslParser;
import com.isaac.approvalworkflowengine.rules.evaluation.RuleEvaluationResult;
import com.isaac.approvalworkflowengine.rules.evaluation.RuleEvaluator;
import com.isaac.approvalworkflowengine.rules.model.RuleEvaluationContext;
import com.isaac.approvalworkflowengine.rules.repository.RuleSetJpaRepository;
import com.isaac.approvalworkflowengine.rules.repository.entity.RuleSetEntity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RuleSetService implements RuleSetLookup, RuleSetRuntimeEvaluator {

    private static final Logger log = LoggerFactory.getLogger(RuleSetService.class);

    private final RuleSetJpaRepository ruleSetJpaRepository;
    private final RuleDslParser ruleDslParser;
    private final RuleEvaluator ruleEvaluator;
    private final RuleDslChecksumService ruleDslChecksumService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Counter evaluationsTotalCounter;
    private final Counter evaluationsFailedCounter;
    private final Timer evaluationsLatencyTimer;

    public RuleSetService(
        RuleSetJpaRepository ruleSetJpaRepository,
        RuleDslParser ruleDslParser,
        RuleEvaluator ruleEvaluator,
        RuleDslChecksumService ruleDslChecksumService,
        ObjectMapper objectMapper,
        MeterRegistry meterRegistry
    ) {
        this.ruleSetJpaRepository = ruleSetJpaRepository;
        this.ruleDslParser = ruleDslParser;
        this.ruleEvaluator = ruleEvaluator;
        this.ruleDslChecksumService = ruleDslChecksumService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.evaluationsTotalCounter = meterRegistry.counter("rules_evaluations_total");
        this.evaluationsFailedCounter = meterRegistry.counter("rules_evaluations_failed_total");
        this.evaluationsLatencyTimer = meterRegistry.timer("rules_evaluation_latency");
    }

    @Transactional
    public RuleSetVersionResource createVersion(String ruleSetKey, RuleSetVersionInput input, RuleSetActor actor) {
        String normalizedRuleSetKey = normalizeUpper(ruleSetKey);
        JsonNode dslNode = objectMapper.valueToTree(input.dsl());
        ruleDslParser.parse(dslNode);

        int nextVersionNo = ruleSetJpaRepository.findTopByRuleSetKeyOrderByVersionNoDesc(normalizedRuleSetKey)
            .map(existing -> existing.getVersionNo() + 1)
            .orElse(1);

        String canonicalDslJson = ruleDslChecksumService.canonicalize(dslNode);
        String checksum = ruleDslChecksumService.checksumSha256(canonicalDslJson);

        RuleSetEntity entity = new RuleSetEntity();
        entity.setId(UUID.randomUUID());
        entity.setRuleSetKey(normalizedRuleSetKey);
        entity.setVersionNo(nextVersionNo);
        entity.setDslJson(canonicalDslJson);
        entity.setChecksumSha256(checksum);
        entity.setCreatedByUserId(actor.userId());

        try {
            RuleSetEntity saved = ruleSetJpaRepository.save(entity);
            return toVersionResource(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Rule set version creation conflicted with existing data", exception);
        }
    }

    @Transactional(readOnly = true)
    public RuleSetVersionResource getVersion(String ruleSetKey, int versionNo) {
        RuleSetEntity entity = findByRuleSetKeyAndVersionNo(ruleSetKey, versionNo);
        return toVersionResource(entity);
    }

    @Transactional(readOnly = true)
    public PagedRuleSetVersionResource listVersions(String ruleSetKey, int page, int size) {
        String normalizedRuleSetKey = normalizeUpper(ruleSetKey);
        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 200),
            Sort.by(Sort.Direction.DESC, "versionNo")
        );

        Page<RuleSetEntity> versionPage = ruleSetJpaRepository
            .findByRuleSetKeyOrderByVersionNoDesc(normalizedRuleSetKey, pageable);

        List<RuleSetVersionResource> items = versionPage.getContent().stream()
            .map(this::toVersionResource)
            .toList();

        return new PagedRuleSetVersionResource(
            items,
            new RulePageMetadata(
                versionPage.getNumber(),
                versionPage.getSize(),
                versionPage.getTotalElements(),
                versionPage.getTotalPages()
            )
        );
    }

    @Transactional(readOnly = true)
    public RuleSimulationResponse simulate(RuleSimulationRequest input) {
        String normalizedRuleSetKey = normalizeUpper(input.ruleSetKey());
        RuleSetEntity entity = findByRuleSetKeyAndVersionNo(normalizedRuleSetKey, input.versionNo());

        JsonNode dsl = readJsonNode(entity.getDslJson());
        var expression = ruleDslParser.parse(dsl);

        RuleEvaluationContext context = toEvaluationContext(input.context());

        evaluationsTotalCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            RuleEvaluationResult result = ruleEvaluator.evaluate(expression, context);
            log.info(
                "Rule simulation evaluated ruleSetKey={} versionNo={} matched={}",
                normalizedRuleSetKey,
                input.versionNo(),
                result.matched()
            );

            List<RuleEvaluationTraceResource> traces = result.traces().stream()
                .map(trace -> new RuleEvaluationTraceResource(
                    trace.path(),
                    trace.expressionType(),
                    trace.result(),
                    trace.field(),
                    trace.operator(),
                    trace.fieldValue(),
                    trace.expectedValue(),
                    trace.reason()
                ))
                .toList();

            return new RuleSimulationResponse(normalizedRuleSetKey, input.versionNo(), result.matched(), traces);
        } catch (RuntimeException exception) {
            evaluationsFailedCounter.increment();
            log.warn(
                "Rule simulation failed ruleSetKey={} versionNo={} message={}",
                normalizedRuleSetKey,
                input.versionNo(),
                exception.getMessage()
            );
            throw exception;
        } finally {
            sample.stop(evaluationsLatencyTimer);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public boolean exists(String ruleSetKey, int versionNo) {
        if (!StringUtils.hasText(ruleSetKey) || versionNo < 1) {
            return false;
        }

        return ruleSetJpaRepository.existsByRuleSetKeyAndVersionNo(normalizeUpper(ruleSetKey), versionNo);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean matches(String ruleSetKey, int versionNo, RuleEvaluationContext context) {
        RuleSetEntity entity = findByRuleSetKeyAndVersionNo(ruleSetKey, versionNo);
        JsonNode dsl = readJsonNode(entity.getDslJson());
        var expression = ruleDslParser.parse(dsl);
        return ruleEvaluator.evaluate(expression, context).matched();
    }

    private RuleSetEntity findByRuleSetKeyAndVersionNo(String ruleSetKey, int versionNo) {
        return ruleSetJpaRepository.findByRuleSetKeyAndVersionNo(normalizeUpper(ruleSetKey), versionNo)
            .orElseThrow(() -> new NoSuchElementException("Rule set version not found"));
    }

    private RuleSetVersionResource toVersionResource(RuleSetEntity entity) {
        return new RuleSetVersionResource(
            entity.getId(),
            entity.getRuleSetKey(),
            entity.getVersionNo(),
            readJsonMap(entity.getDslJson()),
            entity.getChecksumSha256(),
            entity.getCreatedAt()
        );
    }

    private RuleEvaluationContext toEvaluationContext(RuleEvaluationContextInput input) {
        return new RuleEvaluationContext(
            input.amount(),
            trimToNull(input.department()),
            trimToNull(input.requestType()),
            normalizeCurrency(input.currency()),
            input.payload() == null ? java.util.Map.of() : input.payload()
        );
    }

    private JsonNode readJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize rule DSL JSON", exception);
        }
    }

    private java.util.Map<String, Object> readJsonMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize rule DSL JSON", exception);
        }
    }

    private String normalizeUpper(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Required value is missing");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            return null;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
