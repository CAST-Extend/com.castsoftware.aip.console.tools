package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.services.AipConsoleService;
import com.castsoftware.aip.console.tools.core.services.AipConsoleServiceImpl;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.ApplicationServiceImpl;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.JobsServiceImpl;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.services.UploadServiceImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import hudson.Extension;
import io.jenkins.plugins.aipconsole.extensions.RestApiServiceExtension;

@Extension
public class AipConsoleModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RestApiService.class).toInstance(new RestApiServiceExtension());
    }

    @Provides
    @Singleton
    public JobsService getJobsService(RestApiService restApiService) {
        return new JobsServiceImpl(restApiService);
    }

    @Provides
    @Singleton
    public UploadService getChunkedUploadService(RestApiService restApiService) {
        return new UploadServiceImpl(restApiService);
    }

    @Provides
    @Singleton
    public ApplicationService getApplicationService(RestApiService restApiService, JobsService jobsService) {
        return new ApplicationServiceImpl(restApiService, jobsService);
    }

    @Provides
    @Singleton
    public AipConsoleService getAipConsoleService(RestApiService restApiService, ApplicationService applicationService) {
        return new AipConsoleServiceImpl(restApiService, applicationService);
    }

}
