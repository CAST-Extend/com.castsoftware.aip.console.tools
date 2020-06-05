package com.castsoftware.aip.console.tools.providers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
public class VersionProvider implements CommandLine.IVersionProvider {

    private String appVersion;

    public VersionProvider(@Value("${application.version}") String appVersion) {
        this.appVersion = appVersion;
    }

    @Override
    public String[] getVersion() throws Exception {
        return new String[]{appVersion};
    }
}
