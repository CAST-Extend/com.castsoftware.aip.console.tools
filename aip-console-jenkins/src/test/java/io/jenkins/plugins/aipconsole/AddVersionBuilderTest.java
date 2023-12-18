package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.aip.console.tools.core.utils.SemVerUtils;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.Secret;
import io.jenkins.plugins.aipconsole.config.AipConsoleGlobalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_appNotFound;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobFailure;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_uploadFailed;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_info_appNotFoundAutoCreate;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_success_analysisComplete;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.GenericError_error_accessDenied;
import static io.jenkins.plugins.aipconsole.Messages.GenericError_error_missingRequiredParameters;
import static io.jenkins.plugins.aipconsole.Messages.GenericError_error_noApiKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class AddVersionBuilderTest extends BaseBuilderTest{

    @InjectMocks
    private AddVersionBuilder addVersionBuilder;

    @Before
    public void setUp() throws Exception {
        addVersionBuilder = new AddVersionBuilder(BaseBuilderTest.TEST_APP_NAME, BaseBuilderTest.TEST_ARCHIVE_NAME);
        MockitoAnnotations.initMocks(this);
        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion(SemVerUtils.getMinCompatibleVersion().toString()).build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        doReturn(BaseBuilderTest.TEST_APP).when(applicationService).getApplicationFromGuid(BaseBuilderTest.TEST_APP_NAME);
        doReturn(BaseBuilderTest.TEST_APP).when(applicationService).getApplicationFromGuid(BaseBuilderTest.TEST_APP_GUID);
    }

    @Test
    public void testAddVersionStepToJob() throws Exception {
        FreeStyleProject project = getProjectWithDefaultAddVersion();
        project = jenkins.configRoundtrip(project);
        AddVersionBuilder job = new AddVersionBuilder(BaseBuilderTest.TEST_APP_NAME, BaseBuilderTest.TEST_ARCHIVE_NAME);
        job.setDomainName("");
        job.setCssServerName("");
        job.setModuleGenerationType(ModuleGenerationType.PRESERVE_CONFIGURED.toString());
        jenkins.assertEqualDataBoundBeans(job, project.getBuildersList().get(0));
    }

    @Test
    public void testAddVersionStepToJob_TargetingPrivateConsole() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(addVersionBuilder);
        addVersionBuilder.setApiKey(Secret.fromString("Z-Y-X"));
        //addVersionBuilder.setAipConsoleUrl("http://localhost:8083");
        AddVersionBuilder job = new AddVersionBuilder(BaseBuilderTest.TEST_APP_NAME, BaseBuilderTest.TEST_ARCHIVE_NAME);
        job.setApiKey(Secret.fromString("Z-Y-X"));
        job.setAipConsoleUrl(addVersionBuilder.getDescriptor().getAipConsoleUrl());
        jenkins.assertEqualDataBoundBeans(job, project.getBuildersList().get(0));
    }

    @Test
    public void testBuildFreestyleDefaultOk() throws Exception {
        FreeStyleProject project = getProjectWithDefaultAddVersionAndFile(BaseBuilderTest.TEST_ARCHIVE_NAME);

        doNothing().
                when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(BaseBuilderTest.TEST_APP_NAME)
                .when(applicationService).getApplicationGuidFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(BaseBuilderTest.TEST_APP)
                .when(applicationService).getApplicationFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(true)
                .when(uploadService).uploadInputStream(eq(BaseBuilderTest.TEST_APP_GUID), anyString(), anyLong(), isA(InputStream.class));
        doReturn(BaseBuilderTest.TEST_JOB_GUID)
                .when(jobsService).startAddVersionJob(any(JobRequestBuilder.class));
        doReturn(JobState.COMPLETED)
                .when(jobsService).pollAndWaitForJobFinished(eq(BaseBuilderTest.TEST_JOB_GUID), any(), any(), any(), eq(null));

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_success_analysisComplete(), build);
    }

    @Test
    @Ignore
    public void testBuildPipelineScriptOk() throws Exception {
        // Replace path separators with / for the groovy script
        String filePath = StringUtils.replaceChars(createTempFileAndGetPath(BaseBuilderTest.TEST_ARCHIVE_NAME).toString(), File.separatorChar, '/');
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("node {" +
                "  aipAddVersion applicationName: '" + BaseBuilderTest.TEST_APP_NAME + "', filePath: '" + filePath + "'" +
                "}", true)
        );

        doNothing().
                when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(BaseBuilderTest.TEST_APP_NAME)
                .when(applicationService).getApplicationGuidFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(true)
                .when(uploadService).uploadInputStream(eq(BaseBuilderTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));
        doReturn(BaseBuilderTest.TEST_JOB_GUID)
                .when(jobsService).startAddVersionJob(any(JobRequestBuilder.class));
        doReturn(JobState.COMPLETED)
                .when(jobsService).pollAndWaitForJobFinished(eq(BaseBuilderTest.TEST_JOB_GUID), any(), any(), any(), eq(null));

        WorkflowRun workflowRun = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_success_analysisComplete(), workflowRun);
    }

    @Test
    public void testBuildMissingRequiredStepParameters() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new AddVersionBuilder("", ""));

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.NOT_BUILT, futureBuild.get());
        jenkins.assertLogContains(GenericError_error_missingRequiredParameters(), build);
    }

    @Test
    public void testBuildMissingApiKeyParameter() throws Exception {
        FreeStyleProject project = getProjectWithDefaultAddVersion();
        AipConsoleGlobalConfiguration.get().setApiKey(null);

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.NOT_BUILT, futureBuild.get());
        jenkins.assertLogContains(GenericError_error_noApiKey(), build);
    }

    @Test
    public void testBuildAipConsoleAccessError() throws Exception {
        FreeStyleProject project = getProjectWithDefaultAddVersionAndFile(BaseBuilderTest.TEST_ARCHIVE_NAME);
        doThrow(new ApiCallException(500)).
                when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(GenericError_error_accessDenied(BaseBuilderTest.TEST_URL), build);
    }

    @Test
    public void testBuildApplicationNotFoundAndAutoCreateFalse() throws Exception {
        Assert.assertFalse("Auto create should be false. Some changes were persisted to the builder ?", addVersionBuilder.isAutoCreate());
        addVersionBuilder.setFilePath(createTempFileAndGetPath(BaseBuilderTest.TEST_ARCHIVE_NAME).toString());
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(null)
                .when(applicationService).getApplicationFromGuid(BaseBuilderTest.TEST_APP_NAME);

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_error_appNotFound(BaseBuilderTest.TEST_APP_NAME), build);
    }

    @Test
    public void testBuildApplicationUploadFailure() throws Exception {
        addVersionBuilder.setFilePath(createTempFileAndGetPath(BaseBuilderTest.TEST_ARCHIVE_NAME).toString());
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(BaseBuilderTest.TEST_APP)
                .when(applicationService).getApplicationFromName(BaseBuilderTest.TEST_APP_NAME);
        doThrow(new UploadException("Fake error"))
                .when(uploadService).uploadInputStream(eq(BaseBuilderTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));
        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_error_uploadFailed(), build);
    }

    @Test
    public void testBuildErrorCreatingAnalyzeJob() throws Exception {
        addVersionBuilder.setFilePath(createTempFileAndGetPath(BaseBuilderTest.TEST_ARCHIVE_NAME).toString());
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(BaseBuilderTest.TEST_APP_NAME)
                .when(applicationService).getApplicationGuidFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(BaseBuilderTest.TEST_APP)
                .when(applicationService).getApplicationFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(true)
                .when(uploadService).uploadInputStream(eq(BaseBuilderTest.TEST_APP_GUID), anyString(), anyLong(), isA(InputStream.class));
        doThrow(new JobServiceException("fake exception"))
                .when(jobsService).startAddVersionJob(any(JobRequestBuilder.class));

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_error_jobServiceException(), build);
    }

    @Test
    public void testBuildAnalyseResultCancelled() throws Exception {
        addVersionBuilder.setFilePath(createTempFileAndGetPath(BaseBuilderTest.TEST_ARCHIVE_NAME).toString());
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(BaseBuilderTest.TEST_APP)
                .when(applicationService).getApplicationFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(true)
                .when(uploadService).uploadInputStream(eq(BaseBuilderTest.TEST_APP_GUID), anyString(), anyLong(), isA(InputStream.class));
        doReturn(BaseBuilderTest.TEST_JOB_GUID)
                .when(jobsService).startAddVersionJob(any(JobRequestBuilder.class));
        doReturn(JobState.CANCELED)
                .when(jobsService).pollAndWaitForJobFinished(eq(BaseBuilderTest.TEST_JOB_GUID), any(), any(), any(), eq(null));

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_error_jobFailure(JobState.CANCELED), build);
    }

    @Test
    public void testBuildApplicationNotFoundCreateApplicationFails() throws Exception {
        Assert.assertTrue("Node name should be null/empty. Other test set it and it wasn't reset ?",
                StringUtils.isBlank(addVersionBuilder.getNodeName()));
        addVersionBuilder.setFilePath(createTempFileAndGetPath(BaseBuilderTest.TEST_ARCHIVE_NAME).toString());
        addVersionBuilder.setAutoCreate(true);
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(null)
                .when(applicationService).getApplicationGuidFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(null)
                .when(applicationService).getApplicationFromName(BaseBuilderTest.TEST_APP_NAME);
        doThrow(new JobServiceException("cannot create application"))
                .when(jobsService).startCreateApplication(eq(BaseBuilderTest.TEST_APP_NAME), anyString(), anyBoolean(), anyString());

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_info_appNotFoundAutoCreate(BaseBuilderTest.TEST_APP_NAME), build);
        jenkins.assertLogContains(CreateApplicationBuilder_CreateApplication_error_jobServiceException(BaseBuilderTest.TEST_APP_NAME, BaseBuilderTest.TEST_URL), build);
    }

    private FreeStyleProject getProjectWithDefaultAddVersion() throws IOException {
        return getProjectWithBuilder(addVersionBuilder);
    }

    private FreeStyleProject getProjectWithDefaultAddVersionAndFile(String fileName) throws IOException {
        addVersionBuilder.setFilePath(createTempFileAndGetPath(fileName).toString());
        return getProjectWithBuilder(addVersionBuilder);
    }
}