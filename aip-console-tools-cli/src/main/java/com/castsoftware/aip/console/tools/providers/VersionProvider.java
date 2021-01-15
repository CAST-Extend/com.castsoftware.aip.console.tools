package com.castsoftware.aip.console.tools.providers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
public class VersionProvider implements CommandLine.IVersionProvider {

    @Value("${application.version}")
    private String appVersion;

    public VersionProvider(){}

    @Override
    public String[] getVersion() throws Exception {
        return new String[]{appVersion};
    }
}
