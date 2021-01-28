package io.quarkus.registry.app.util;

import java.util.Iterator;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApplicationScoped
public final class JsonNodes {

    private final ObjectMapper objectMapper;

    @Inject
    public JsonNodes(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode toJsonNode(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        if (mainNode == null) {
            return updateNode;
        } else if (updateNode == null) {
            return mainNode;
        }
        JsonNode resultNode = mainNode.deepCopy();
        for (Iterator<String> fieldNames = updateNode.fieldNames(); fieldNames.hasNext(); ) {
            String fieldName = fieldNames.next();
            JsonNode jsonNode = mainNode.get(fieldName);
            // if field exists and is an embedded object
            if (jsonNode != null && jsonNode.isObject()) {
                merge(jsonNode, updateNode.get(fieldName));
            } else {
                if (mainNode instanceof ObjectNode) {
                    // Overwrite field
                    JsonNode value = updateNode.get(fieldName);
                    ((ObjectNode) resultNode).set(fieldName, value);
                }
            }
        }
        return resultNode;
    }

}
