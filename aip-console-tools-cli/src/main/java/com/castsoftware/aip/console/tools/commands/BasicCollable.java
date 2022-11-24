package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Callable;

@Getter
@Setter
public abstract class BasicCollable implements Callable<Integer> {
    protected final RestApiService restApiService;
    protected final JobsService jobsService;
    protected final UploadService uploadService;
    protected final ApplicationService applicationService;

    protected BasicCollable(RestApiService restApiService, JobsService jobsService
            , UploadService uploadService, ApplicationService applicationService) {
        this.applicationService = applicationService;
        this.jobsService = jobsService;
        this.uploadService = uploadService;
        this.restApiService = restApiService;
    }

    protected BasicCollable(RestApiService restApiService, JobsService jobsService, ApplicationService applicationService) {
        this(restApiService, jobsService, null, applicationService);
    }
}
