package com.castsoftware.aip.console.tools.core.dto.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log
public class FullStepsDeserializer extends JsonDeserializer<List<String>> {
    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        List<String> fullSteps = new ArrayList<>();
        JsonNode node = p.readValueAsTree();
        // Ignore non arrays... ?
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode child : arrayNode) {
                // before 1.20 AIP Console, fullSteps node is just a list of strings
                // After 1.20, fullSteps contains a list of object
                // Here we dynamically map from object or string to a string
                if (child.isObject()) {
                    // Make sure we have a text node and not an object node
                    JsonNode stepNode = child.get("step");
                    if (stepNode.isTextual()) {
                        fullSteps.add(stepNode.textValue());
                    }
                } else if (child.isTextual()) {
                    fullSteps.add(child.textValue());
                }
            }
        }
        //
        return fullSteps;
    }
}
