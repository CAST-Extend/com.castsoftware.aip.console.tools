package com.castsoftware.aip.console.tools.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class VersionDto {

    private static final int DMT_DATE_PATTERN_LENGTH = 10;
    private static final SimpleDateFormat DMT_ENTITY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DMT_ENTITY_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private String guid;

    private String name;

    private VersionStatus status;

    private boolean isCurrent;

    private Date releaseDate;

    private VersionDto previousVersion;

    private boolean lastExecutionSuccess;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    public Date getReleaseDate() {
        return releaseDate;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getPreviousVersion() {
        return null != previousVersion ? previousVersion.getGuid() : null;
    }

    public void setReleaseDate(String releaseDate) {
        try {
            this.releaseDate = DMT_ENTITY_DATETIME_FORMAT.parse(releaseDate);
        } catch (ParseException e) {
            // NOP
        }
    }

}
