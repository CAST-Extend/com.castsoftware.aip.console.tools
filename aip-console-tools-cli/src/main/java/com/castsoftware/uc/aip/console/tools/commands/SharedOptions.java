package com.castsoftware.uc.aip.console.tools.commands;

import lombok.Getter;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.List;

@Component
public class SharedOptions {
    /**
     * Connection to AIP Console parameters
     **/
    @CommandLine.Option(names = {"-s", "--server-url"}, paramLabel = "AIP_CONSOLE_URL", description = "The base URL for AIP Console (defaults to ${DEFAULT-VALUE})", defaultValue = "http://localhost:8081")
    private String serverRootUrl;

    @CommandLine.Option(names = {"--apikey"}, description = "Enable prompt to enter password after start of CLI", interactive = true, hideParamSyntax = true)
    private String apiKey;
    @CommandLine.Option(names = {"--apikey:env"}, paramLabel = "ENV_VAR_NAME", description = "The name of the environment variable containing the user's access token to AIP Console")
    private String apiKeyEnvVariable;

    @Getter
    @CommandLine.Option(names = {"--user"}, description = "User name to use on AIP Console", hidden = true, hideParamSyntax = true)
    private String username;


    @CommandLine.Unmatched
    private List<String> unmatchedOptions;

    public String getApiKeyValue() {
        if (apiKeyEnvVariable != null) {
            return System.getenv(apiKeyEnvVariable);
        }
        return apiKey;
    }

    public String getFullServerRootUrl() {
        if (serverRootUrl != null && !serverRootUrl.startsWith("http")) {
            serverRootUrl = "http://" + serverRootUrl;
        }
        return serverRootUrl;
    }

    @Override
    public String toString() {
        return "SharedOptions{" +
                "serverRootUrl='" + serverRootUrl + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", apiKeyEnvVariable='" + apiKeyEnvVariable + '\'' +
                ", username='" + username + '\'' +
                ", unmatchedOptions=" + unmatchedOptions +
                '}';
    }
}
