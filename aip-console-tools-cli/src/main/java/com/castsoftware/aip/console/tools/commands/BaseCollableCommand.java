package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.services.RestApiService;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
public abstract class BaseCollableCommand implements Callable<Integer> {
    protected final RestApiService restApiService;

    @CommandLine.Mixin
    protected SharedOptions sharedOptions;

    protected BaseCollableCommand(RestApiService restApiService) {
        this.restApiService = restApiService;
    }

    protected abstract Integer doCall() throws Exception;

    public SharedOptions getSharedOptions() {
        return sharedOptions;
    }

    @Override
    public Integer call() throws Exception {
        log.info("AddVersion version command has triggered with log verbose mode = '{}'", sharedOptions.isVerbose());
        restApiService.setVerbose(sharedOptions.isVerbose());
        return doCall();
    }
}
