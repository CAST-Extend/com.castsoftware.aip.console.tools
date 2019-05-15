package com.castsoftware.uc.aip.console.tools.core.dto.upload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Objects;

// Custom version of CHunkedUploadDto in common
// prevents issues with enums for different AIP versions
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkedUploadDto {
    private String guid;
    private String fileName;
    private String destinationFolder;
    private String applicationGuid;
    private long fileSize;
    private long currentOffset;
    private boolean resumable;
    private Date expirationDate;
    private String status;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChunkedUploadDto)) {
            return false;
        }
        ChunkedUploadDto baseDto = (ChunkedUploadDto) o;
        return Objects.equals(guid, baseDto.guid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(guid);
    }
}
