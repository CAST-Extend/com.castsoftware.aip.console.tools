package com.castsoftware.uc.aip.console.tools.core.dto.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

/**
 * Serializes the {@code Map<String, String>} into a JSON object
 */
public class StatusStringMapSerializer extends JsonSerializer<Map<String, String>> {

    @Override
    public void serialize(Map<String, String> map, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (map == null) {
            return;
        }
        jsonGenerator.writeStartObject();
        if (!map.isEmpty()) {
            for (Map.Entry<String, String> e : map.entrySet()) {
                jsonGenerator.writeObjectField(e.getKey(), e.getValue());
            }
        }
        jsonGenerator.writeEndObject();
    }
}
