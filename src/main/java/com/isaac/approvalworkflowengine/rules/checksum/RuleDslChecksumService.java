package com.isaac.approvalworkflowengine.rules.checksum;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleDslChecksumService {

    private final ObjectMapper canonicalMapper;

    public RuleDslChecksumService(ObjectMapper objectMapper) {
        this.canonicalMapper = objectMapper.copy()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String canonicalize(JsonNode dsl) {
        JsonNode canonicalDsl = canonicalizeNode(dsl);
        try {
            return canonicalMapper.writeValueAsString(canonicalDsl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize rule DSL", exception);
        }
    }

    public String checksumSha256(String canonicalDslJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = canonicalDslJson.getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private JsonNode canonicalizeNode(JsonNode node) {
        if (node == null || node.isNull() || node.isValueNode()) {
            return node;
        }

        if (node.isArray()) {
            ArrayNode canonicalArray = canonicalMapper.createArrayNode();
            for (JsonNode child : node) {
                canonicalArray.add(canonicalizeNode(child));
            }
            return canonicalArray;
        }

        ObjectNode objectNode = (ObjectNode) node;
        ObjectNode canonicalObject = canonicalMapper.createObjectNode();

        Iterator<String> fieldNames = objectNode.fieldNames();
        List<String> keys = new ArrayList<>();
        while (fieldNames.hasNext()) {
            keys.add(fieldNames.next());
        }

        Collections.sort(keys);
        for (String key : keys) {
            canonicalObject.set(key, canonicalizeNode(objectNode.get(key)));
        }

        return canonicalObject;
    }
}
