package com.castsoftware.aip.console.tools.core.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionProfileExportDto {

    private String host;
    private int port;
    private String userName;
    private String password;
    private String schemaPrefix;
    private String databaseName;
}

