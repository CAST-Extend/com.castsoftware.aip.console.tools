package com.castsoftware.uc.aip.console.tools.commands;

import org.apache.commons.lang3.StringUtils;
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

    @CommandLine.Option(names = {"--apikey"}, description = "The API Key to access AIP Console. Will prompt entry if no value is passed.", interactive = true, arity = "0..1")
    private String apiKey;

    @CommandLine.Option(names = {"--apikey:env"}, paramLabel = "ENV_VAR_NAME", description = "The name of the environment variable containing the AIP Key to access AIP Console")
    private String apiKeyEnvVariable;

    @CommandLine.Option(names = {"--user"}, description = "User name. Use this if no API Key generation is available on AIP Console. Provide the user's password in the apikey parameter.")
    private String username;

    @CommandLine.Option(names = {"--timeout"}, description = "The timeout in seconds for calls to AIP Console. Defaults to a 30 timeout", defaultValue = "30")
    private long timeout;

    @CommandLine.Unmatched
    private List<String> unmatchedOptions;

    public String getServerRootUrl() {
        return serverRootUrl;
    }

    public void setServerRootUrl(String serverRootUrl) {
        this.serverRootUrl = serverRootUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeyEnvVariable() {
        return apiKeyEnvVariable;
    }

    public void setApiKeyEnvVariable(String apiKeyEnvVariable) {
        this.apiKeyEnvVariable = apiKeyEnvVariable;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getUnmatchedOptions() {
        return unmatchedOptions;
    }

    public void setUnmatchedOptions(List<String> unmatchedOptions) {
        this.unmatchedOptions = unmatchedOptions;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getApiKeyValue() {
        if (apiKeyEnvVariable != null) {
            return System.getenv(apiKeyEnvVariable);
        }
        return apiKey;
    }

    public String getFullServerRootUrl() {
        if (StringUtils.isNotBlank(serverRootUrl) && !serverRootUrl.startsWith("http")) {
            serverRootUrl = "http://" + serverRootUrl;
        }
        return serverRootUrl;
    }

    @Override
    public String toString() {
        return "SharedOptions{" +
                "serverRootUrl='" + serverRootUrl + '\'' +
                ", apiKey='omitted'" +
                ", apiKeyEnvVariable='" + apiKeyEnvVariable + '\'' +
                ", username='" + username + '\'' +
                ", unmatchedOptions=" + unmatchedOptions +
                '}';
    }
}
