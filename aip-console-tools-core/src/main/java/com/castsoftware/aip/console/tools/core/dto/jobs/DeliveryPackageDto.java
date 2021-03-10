package com.castsoftware.aip.console.tools.core.dto.jobs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.builder.CompareToBuilder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DeliveryPackageDto implements Comparable<DeliveryPackageDto>{
    private String guid;
    private String name;
    private String path;
    private String oldPath;

    @Override
    public int compareTo(DeliveryPackageDto o) {
        return new CompareToBuilder()
                .append(this.name, o.name)
                .append(this.path, o.path)
                .toComparison();
    }
}
