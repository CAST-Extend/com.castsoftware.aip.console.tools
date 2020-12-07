package com.castsoftware.aip.console.tools.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class VersionDto {

    private static final DateTimeFormatter LOCAL_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.systemDefault());
    private String guid;
    private String name;
    private String title;
    private VersionStatus status;
    private LocalDateTime versionDate;
    private boolean currentVersion;
    private boolean configurationChanged;
    private String previousVersion;
    private DeliveryConfigurationDto deliveryConfiguration;

    @JsonInclude(JsonInclude.Include.NON_NULL)

    public String getPreviousVersion() {
        return previousVersion;
    }

}
