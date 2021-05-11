package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.NodeDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.UploadException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Future;

import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_appNotFound;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobFailure;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_jobServiceException;
import static io.jenkins.plugins.aipconsole.Messages.AddVersionBuilder_AddVersion_error_nodeNotFound;
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
public class AddVersionBuilderTest extends JenkinsBasicTest {

    @InjectMocks
    private AddVersionBuilder addVersionBuilder;

    @Before
    public void setUp() throws Exception {
        AipConsoleGlobalConfiguration config = AipConsoleGlobalConfiguration.get();
        config.setAipConsoleUrl(JenkinsBasicTest.TEST_URL);
        config.setApiKey(Secret.fromString(JenkinsBasicTest.TEST_KEY));
        addVersionBuilder = new AddVersionBuilder(JenkinsBasicTest.TEST_APP_NAME, JenkinsBasicTest.TEST_ARCHIVE_NAME);
        MockitoAnnotations.initMocks(this);
        doReturn(ApiInfoDto.builder().apiVersion("1.12.0-DEV").build())
                .when(restApiService).getAipConsoleApiInfo();
    }

    @Test
    public void testAddVersionStepToJob() throws Exception {
        FreeStyleProject project = getProjectWithDefaultAddVersion();
        project = jenkins.configRoundtrip(project);
        AddVersionBuilder job = new AddVersionBuilder(JenkinsBasicTest.TEST_APP_NAME, JenkinsBasicTest.TEST_ARCHIVE_NAME);
        job.setDomainName("");
        jenkins.assertEqualDataBoundBeans(job, project.getBuildersList().get(0));
    }
    
    @Test
    public void testBuildFreestyleDefaultOk() throws Exception {
        FreeStyleProject project = getProjectWithDefaultAddVersionAndFile(JenkinsBasicTest.TEST_ARCHIVE_NAME);

        doNothing().
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);
        doReturn(JenkinsBasicTest.TEST_APP_NAME)
                .when(applicationService).getApplicationGuidFromName(JenkinsBasicTest.TEST_APP_NAME);
        doReturn(JenkinsBasicTest.TEST_APP)
                .when(applicationService).getApplicationFromName(JenkinsBasicTest.TEST_APP_NAME);
        doReturn(true)
                .when(uploadService).uploadInputStream(eq(JenkinsBasicTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));
        doReturn(JenkinsBasicTest.TEST_JOB_GUID)
                .when(jobsService).startAddVersionJob(any(JobRequestBuilder.class));
        doReturn(JobState.COMPLETED)
                .when(jobsService).pollAndWaitForJobFinished(eq(JenkinsBasicTest.TEST_JOB_GUID), any(), any(), any());

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_success_analysisComplete(), build);
    }

    @Test
    @Ignore
    public void testBuildPipelineScriptOk() throws Exception {
        // Replace path separators with / for the groovy script
        String filePath = StringUtils.replaceChars(createTempFileAndGetPath(JenkinsBasicTest.TEST_ARCHIVE_NAME), File.separatorChar, '/');
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("node {" +
                "  aipAddVersion applicationName: '" + JenkinsBasicTest.TEST_APP_NAME + "', filePath: '" + filePath + "'" +
                "}", true)
        );

        doNothing().
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);
        doReturn(JenkinsBasicTest.TEST_APP_NAME)
                .when(applicationService).getApplicationGuidFromName(JenkinsBasicTest.TEST_APP_NAME);
        doReturn(true)
                .when(uploadService).uploadInputStream(eq(JenkinsBasicTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));
        doReturn(JenkinsBasicTest.TEST_JOB_GUID)
                .when(jobsService).startAddVersionJob(any(JobRequestBuilder.class));
        doReturn(JobState.COMPLETED)
                .when(jobsService).pollAndWaitForJobFinished(eq(JenkinsBasicTest.TEST_JOB_GUID), any(), any(), any());

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
        FreeStyleProject project = getProjectWithDefaultAddVersionAndFile(JenkinsBasicTest.TEST_ARCHIVE_NAME);
        doThrow(new ApiCallException(500)).
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(GenericError_error_accessDenied(JenkinsBasicTest.TEST_URL), build);
    }

    @Test
    public void testBuildApplicationNotFoundAndAutoCreateFalse() throws Exception {
        Assert.assertFalse("Auto create should be false. Some changes were persisted to the builder ?", addVersionBuilder.isAutoCreate());
        addVersionBuilder.setFilePath(createTempFileAndGetPath(JenkinsBasicTest.TEST_ARCHIVE_NAME));
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);
        doReturn(null)
                .when(applicationService).getApplicationFromGuid(JenkinsBasicTest.TEST_APP_NAME);

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_error_appNotFound(JenkinsBasicTest.TEST_APP_NAME), build);
    }

    @Test
    public void testBuildApplicationUploadFailure() throws Exception {
        addVersionBuilder.setFilePath(createTempFileAndGetPath(JenkinsBasicTest.TEST_ARCHIVE_NAME));
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);
        doReturn(JenkinsBasicTest.TEST_APP)
                .when(applicationService).getApplicationFromName(JenkinsBasicTest.TEST_APP_NAME);
        doThrow(new UploadException("Fake error"))
                .when(uploadService).uploadInputStream(eq(JenkinsBasicTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));
        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_error_uploadFailed(), build);
    }

    @Test
    public void testBuildErrorCreatingAnalyzeJob() throws Exception {
        addVersionBuilder.setFilePath(createTempFileAndGetPath(JenkinsBasicTest.TEST_ARCHIVE_NAME));
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);
        doReturn(JenkinsBasicTest.TEST_APP_NAME)
                .when(applicationService).getApplicationGuidFromName(JenkinsBasicTest.TEST_APP_NAME);
        doReturn(JenkinsBasicTest.TEST_APP)
                .when(applicationService).getApplicationFromName(JenkinsBasicTest.TEST_APP_NAME);
        doReturn(true)
                .when(uploadService).uploadInputStream(eq(JenkinsBasicTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));
        doThrow(new JobServiceException("fake exception"))
                .when(jobsService).startAddVersionJob(any(JobRequestBuilder.class));

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_error_jobServiceException(), build);
    }

    @Test
    public void testBuildAnalyseResultCancelled() throws Exception {
        addVersionBuilder.setFilePath(createTempFileAndGetPath(JenkinsBasicTest.TEST_ARCHIVE_NAME));
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);
        doReturn(JenkinsBasicTest.TEST_APP)
                .when(applicationService).getApplicationFromName(JenkinsBasicTest.TEST_APP_NAME);
        doReturn(true)
                .when(uploadService).uploadInputStream(eq(JenkinsBasicTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));
        doReturn(JenkinsBasicTest.TEST_JOB_GUID)
                .when(jobsService).startAddVersionJob(any(JobRequestBuilder.class));
        doReturn(JobState.CANCELED)
                .when(jobsService).pollAndWaitForJobFinished(eq(JenkinsBasicTest.TEST_JOB_GUID), any(), any(), any());

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_error_jobFailure(JobState.CANCELED), build);
    }

    @Test
    public void testBuildApplicationNotFoundCreateApplicationFails() throws Exception {
        Assert.assertTrue("Node name should be null/empty. Other test set it and it wasn't reset ?",
                StringUtils.isBlank(addVersionBuilder.getNodeName()));
        addVersionBuilder.setFilePath(createTempFileAndGetPath(JenkinsBasicTest.TEST_ARCHIVE_NAME));
        addVersionBuilder.setAutoCreate(true);
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);
        doReturn(null)
                .when(applicationService).getApplicationGuidFromName(JenkinsBasicTest.TEST_APP_NAME);
        doReturn(null)
                .when(applicationService).getApplicationFromName(JenkinsBasicTest.TEST_APP_NAME);
        doThrow(new JobServiceException("cannot create application"))
                .when(jobsService).startCreateApplication(eq(JenkinsBasicTest.TEST_APP_NAME), anyString(), anyBoolean());

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_info_appNotFoundAutoCreate(JenkinsBasicTest.TEST_APP_NAME), build);
        jenkins.assertLogContains(CreateApplicationBuilder_CreateApplication_error_jobServiceException(JenkinsBasicTest.TEST_APP_NAME, JenkinsBasicTest.TEST_URL), build);
    }

    @Test
    public void testBuildNodeNotFoundWhenCreatingApplication() throws Exception {
        addVersionBuilder.setFilePath(createTempFileAndGetPath(JenkinsBasicTest.TEST_ARCHIVE_NAME));
        addVersionBuilder.setAutoCreate(true);
        addVersionBuilder.setNodeName(JenkinsBasicTest.TEST_NODE_NAME);
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);
        doReturn(null)
                .when(applicationService).getApplicationGuidFromName(JenkinsBasicTest.TEST_APP_NAME);
        doReturn(new ArrayList<NodeDto>())
                .when(restApiService).getForEntity(eq("/api/nodes"), isA(TypeReference.class));

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_error_nodeNotFound(JenkinsBasicTest.TEST_NODE_NAME), build);
    }

    @Test
    public void testBuildCreateAppOnNodeWithNameSuccess() throws Exception {
        addVersionBuilder.setFilePath(createTempFileAndGetPath(JenkinsBasicTest.TEST_ARCHIVE_NAME));
        addVersionBuilder.setAutoCreate(true);
        addVersionBuilder.setNodeName(JenkinsBasicTest.TEST_NODE_NAME);
        addVersionBuilder.setDomainName(null);
        FreeStyleProject project = getProjectWithBuilder(addVersionBuilder);

        doNothing().
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);
        doReturn(null)
                .when(applicationService).getApplicationGuidFromName(JenkinsBasicTest.TEST_APP_NAME);
        doReturn(Collections.singletonList(new NodeDto(JenkinsBasicTest.TEST_NODE_NAME, JenkinsBasicTest.TEST_NODE_NAME, "http", "localhost", 8082)))
                .when(restApiService).getForEntity(eq("/api/nodes"), isA(TypeReference.class));
        doReturn("createAppGuid")
                .when(jobsService).startCreateApplication(JenkinsBasicTest.TEST_APP_NAME, JenkinsBasicTest.TEST_NODE_NAME, null, false);
        doReturn(JenkinsBasicTest.TEST_APP_NAME)
                .when(jobsService).pollAndWaitForJobFinished(eq("createAppGuid"), any(), any(), any());
        doReturn(true)
                .when(uploadService).uploadInputStream(eq(JenkinsBasicTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));
        doReturn(JenkinsBasicTest.TEST_JOB_GUID)
                .when(jobsService).startAddVersionJob(any(JobRequestBuilder.class));
        doReturn(JobState.COMPLETED)
                .when(jobsService).pollAndWaitForJobFinished(eq(JenkinsBasicTest.TEST_JOB_GUID), any(), any(), any());

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_info_appNotFoundAutoCreate(JenkinsBasicTest.TEST_APP_NAME), build);
        jenkins.assertLogContains(AddVersionBuilder_AddVersion_success_analysisComplete(), build);
    }


    private FreeStyleProject getProjectWithDefaultAddVersion() throws IOException {
        return getProjectWithBuilder(addVersionBuilder);
    }

    private FreeStyleProject getProjectWithDefaultAddVersionAndFile(String fileName) throws IOException {
        addVersionBuilder.setFilePath(createTempFileAndGetPath(fileName));
        return getProjectWithBuilder(addVersionBuilder);
    }
}