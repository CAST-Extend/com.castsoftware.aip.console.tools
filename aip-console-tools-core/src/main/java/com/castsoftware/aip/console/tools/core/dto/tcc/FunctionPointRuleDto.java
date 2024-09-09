package com.castsoftware.aip.console.tools.core.dto.tcc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FunctionPointRuleDto {
    private Attributes attributes;
    private String definition;
    private int id;
    private String name;
    private int numberOfObjects;
    private String type;

    @Data
    @NoArgsConstructor
    public static class Attributes {
        private int activation;
        private String packageName;
        private int updated;

        // You may need to map the "package" field manually since "package" is a reserved keyword in Java.
        public void setPackage(String packageName) {
            this.packageName = packageName;
        }

        public String getPackage() {
            return packageName;
        }

    }

    public String toString() {
        return "{ " +
                String.format("\"id\": %s, ", this.id) +
                String.format("\"name\": \"%s\", ", this.name) +
                String.format("\"noOfObjects\": %s", this.numberOfObjects) +
                " }";
    }
}
