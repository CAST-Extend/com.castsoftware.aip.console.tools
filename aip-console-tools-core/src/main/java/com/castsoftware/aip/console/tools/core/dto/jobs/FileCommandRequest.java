package com.castsoftware.aip.console.tools.core.dto.jobs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FileCommandRequest {
    private String command;
    private String path;
}
