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
public class DatabaseConnectionSettingsDto {
    private String host;
    private Integer port;
    private String userName;
    private String password;
    private String databaseName;

    public String getConnectionUrl() {
        return String.format("jdbc:postgresql://%s:%s/%s", host, port, databaseName);
    }
}

