package com.castsoftware.aip.console.tools.core.dto.export.jackson;

import com.castsoftware.aip.console.tools.core.utils.DateUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;

public class LocalDateTimeJsonDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String string = parser.getText().trim();
        if (string.length() == 0) {
            return null;
        }
        return DateUtils.parseJsonLocalDateTime(string);
    }
}
