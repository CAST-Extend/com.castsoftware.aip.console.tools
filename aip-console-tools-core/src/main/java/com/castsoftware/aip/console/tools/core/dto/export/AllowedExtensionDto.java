package com.castsoftware.aip.console.tools.core.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Getter
@Setter
public class AllowedExtensionDto {
    private String id;
    private String title;
    private String version;
    private LocalDateTime lockDate;
    private boolean forceInstall;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AllowedExtensionDto)) {
            return false;
        }
        AllowedExtensionDto that = (AllowedExtensionDto) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title);
    }
}
