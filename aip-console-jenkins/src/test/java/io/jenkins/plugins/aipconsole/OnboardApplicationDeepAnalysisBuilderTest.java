package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.util.concurrent.Future;

import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_DeepAnalysisForbidden;
import static io.jenkins.plugins.aipconsole.Messages.OnbordingApplicationBuilder_DescriptorImpl_FastScanRequired;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class OnboardApplicationDeepAnalysisBuilderTest extends BaseBuilderTest {
    @InjectMocks
    private OnboardApplicationDeepAnalysisBuilder deepAnalysisBuilder;

    @Before
    public void setUp() throws Exception {
        deepAnalysisBuilder = new OnboardApplicationDeepAnalysisBuilder(BaseBuilderTest.TEST_APP_NAME);
        MockitoAnnotations.initMocks(this);
        super.startUp();
    }

    @Test
    public void testOnboardApplicationDeepAnalysisJob() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(deepAnalysisBuilder);
        project = jenkins.configRoundtrip(project);
        Object builtProject = project.getBuildersList().get(0);
        OnboardApplicationDeepAnalysisBuilder expectedResults = new OnboardApplicationDeepAnalysisBuilder(TEST_APP_NAME);
        expectedResults.setSnapshotName("");
        jenkins.assertEqualDataBoundBeans(expectedResults, builtProject);
    }

    @Test
    public void testDeepAnalysis_OnExistingApplication_NotFastScanWorkflow() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(deepAnalysisBuilder);

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.8.0-SNAPSHOT-133").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        ApplicationDto applicationDto = ApplicationDto.builder()
                .guid(BaseBuilderTest.TEST_APP_GUID)
                .name(BaseBuilderTest.TEST_APP_NAME).build();

        applicationDto.setOnboarded(false);
        when(applicationService.getApplicationFromName(BaseBuilderTest.TEST_APP_NAME)).thenReturn(applicationDto);
        when(applicationService.getApplicationDetails(BaseBuilderTest.TEST_APP_GUID)).thenReturn(applicationDto);

        doNothing().when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(BaseBuilderTest.TEST_APP_GUID).when(applicationService).getApplicationGuidFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(true).when(uploadService).uploadInputStream(eq(BaseBuilderTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());

        jenkins.assertLogContains(OnbordingApplicationBuilder_DescriptorImpl_DeepAnalysisForbidden(), build);
    }

    @Test
    public void testDeepAnalysis_WhenFastScanRequired() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(deepAnalysisBuilder);

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.8.0-SNAPSHOT-133").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        ApplicationDto applicationDto = ApplicationDto.builder()
                .guid(BaseBuilderTest.TEST_APP_GUID)
                .name(BaseBuilderTest.TEST_APP_NAME).build();

        when(applicationService.getApplicationFromName(BaseBuilderTest.TEST_APP_NAME)).thenReturn(null);
        when(applicationService.getApplicationDetails(BaseBuilderTest.TEST_APP_GUID)).thenReturn(null);

        doNothing().when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(BaseBuilderTest.TEST_APP_GUID).when(applicationService).getApplicationGuidFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(true).when(uploadService).uploadInputStream(eq(BaseBuilderTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());

        jenkins.assertLogContains(OnbordingApplicationBuilder_DescriptorImpl_FastScanRequired(), build);
    }
}
