package io.quarkus.registry.app.util;

import java.util.Iterator;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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

    public JsonNode merge(ObjectNode extObject, JsonNode extOverride) {
        if (extObject == null) {
            return null;
        } else if (extOverride == null) {
            return extObject;
        }
        ObjectNode mergedObject = extObject.deepCopy();
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = extOverride.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> e = fieldsIterator.next();
            JsonNode jsonNode = extObject.get(e.getKey());
            if (e.getValue().isObject()
                    && extObject.has(e.getKey())
                    && jsonNode.isObject()) {
                mergedObject.set(e.getKey(), merge((ObjectNode) jsonNode, e.getValue()));
            } else {
                mergedObject.set(e.getKey(), e.getValue());
            }
        }
        return mergedObject;
    }
}
