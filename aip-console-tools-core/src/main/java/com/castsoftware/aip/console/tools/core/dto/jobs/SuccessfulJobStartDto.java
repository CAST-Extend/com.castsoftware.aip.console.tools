package com.castsoftware.aip.console.tools.core.dto.jobs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The structure of JSON message to be returned to the HTTP client
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuccessfulJobStartDto {
    private String guid;

    public String getJobGuid() {
        return guid;
    }
}
