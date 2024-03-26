package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.utils.SemVerUtils;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.util.concurrent.Future;

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
        deepAnalysisBuilder.setApplicationName(BaseBuilderTest.TEST_APP_NAME);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnboardApplicationDeepAnalysisJob() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(deepAnalysisBuilder);
        doReturn(false).when(applicationService).isImagingAvailable();

        project = jenkins.configRoundtrip(project);
        Object builtProject = project.getBuildersList().get(0);
        OnboardApplicationDeepAnalysisBuilder expectedResults = new OnboardApplicationDeepAnalysisBuilder(TEST_APP_NAME);
        expectedResults.setSnapshotName("");
        jenkins.assertEqualDataBoundBeans(expectedResults, builtProject);
    }

    @Test
    public void testDeepAnalysis_WhenFastScanRequired() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(deepAnalysisBuilder);

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion(SemVerUtils.getMinCompatibleVersion().toString()).build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();

        when(applicationService.getApplicationFromName(BaseBuilderTest.TEST_APP_NAME)).thenReturn(null);
        when(applicationService.getApplicationDetails(BaseBuilderTest.TEST_APP_GUID)).thenReturn(null);

        doNothing().when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(BaseBuilderTest.TEST_APP_GUID).when(applicationService).getApplicationGuidFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(true).when(uploadService).uploadInputStream(eq(BaseBuilderTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
    }
}
