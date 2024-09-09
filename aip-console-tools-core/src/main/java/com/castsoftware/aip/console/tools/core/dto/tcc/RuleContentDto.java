package com.castsoftware.aip.console.tools.core.dto.tcc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleContentDto {
    private int objectId;
    private String objectName;
    private String objectFullName;
    private String objectType;
    private boolean excluded;

    public String toString() {
        return "{ " +
                String.format("\"objectId\": %s, ", this.objectId) +
                String.format("\"objectName\": \"%s\", ", this.objectName) +
                String.format("\"objectFullName\": \"%s\", ", this.objectFullName) +
                String.format("\"objectType\": \"%s\", ", this.objectType) +
                String.format("\"excluded\": %s", this.excluded ? "true" : "false") +
                " }";
    }
}
