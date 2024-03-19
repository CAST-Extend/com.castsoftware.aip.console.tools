package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.utils.SemVerUtils;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.Future;

import static io.jenkins.plugins.aipconsole.Messages.GenericError_DescriptorImpl_bad_server_version;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OnboardApplicationFastScanBuilderTest extends BaseBuilderTest {
    @InjectMocks
    private OnboardApplicationFastScanBuilder onboardApplicationFastScanBuilder;

    private Path testSourcesPath;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    private void createFastScanBuilderFilePath(String sourcesPath) throws Exception {
        testSourcesPath = createTempFileAndGetPath(sourcesPath);
        onboardApplicationFastScanBuilder = new OnboardApplicationFastScanBuilder(BaseBuilderTest.TEST_APP_NAME, testSourcesPath.toString());
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFastScanJob() throws Exception {
        createFastScanBuilderFilePath(BaseBuilderTest.TEST_ARCHIVE_NAME);
        FreeStyleProject project = getProjectWithBuilder(onboardApplicationFastScanBuilder);
        project = jenkins.configRoundtrip(project);
        Object builtProject = project.getBuildersList().get(0);
        OnboardApplicationFastScanBuilder expectedResults = new OnboardApplicationFastScanBuilder(BaseBuilderTest.TEST_APP_NAME, testSourcesPath.toString());
        expectedResults.setDomainName("");
        expectedResults.setNodeName("");
        jenkins.assertEqualDataBoundBeans(expectedResults, builtProject);
    }

    @Test
    public void testFastScanJobWithRelativePath() throws Exception {
        createFastScanBuilderFilePath(BaseBuilderTest.TEST_FOLDER_NAME);
        FreeStyleProject project = getProjectWithBuilder(onboardApplicationFastScanBuilder);
        project = jenkins.configRoundtrip(project);
        Object builtProject = project.getBuildersList().get(0);
        OnboardApplicationFastScanBuilder expectedResults = new OnboardApplicationFastScanBuilder(BaseBuilderTest.TEST_APP_NAME, testSourcesPath.toString());
        expectedResults.setDomainName("");
        expectedResults.setNodeName("");
        jenkins.assertEqualDataBoundBeans(expectedResults, builtProject);
    }

    @Test
    public void testFastScan_OnBadServerVersion() throws Exception {
        createFastScanBuilderFilePath(BaseBuilderTest.TEST_ARCHIVE_NAME);
        FreeStyleProject project = getProjectWithBuilder(onboardApplicationFastScanBuilder);

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion("2.8.0-SNAPSHOT-133").build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
        doReturn(true).when(applicationService).isOnboardingSettingsEnabled();

        ApplicationDto applicationDto = ApplicationDto.builder()
                .guid(BaseBuilderTest.TEST_APP_GUID)
                .name(BaseBuilderTest.TEST_APP_NAME).build();

        applicationDto.setOnboarded(false);
        when(applicationService.getApplicationFromName(BaseBuilderTest.TEST_APP_NAME)).thenReturn(applicationDto);

        doNothing().when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        doReturn(BaseBuilderTest.TEST_APP_GUID).when(applicationService).getApplicationGuidFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(true).when(uploadService).uploadInputStream(eq(BaseBuilderTest.TEST_APP_NAME), anyString(), anyLong(), isA(InputStream.class));
        //Should work without imaging
        when(applicationService.isImagingAvailable()).thenReturn(false);

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());

        String apiVersion = applicationService.getAipConsoleApiInfo().getApiVersion();
        VersionInformation compatibilityVersion = SemVerUtils.getMinCompatibleVersion();

        jenkins.assertLogContains(GenericError_DescriptorImpl_bad_server_version(apiVersion, compatibilityVersion.toString()), build);
    }
}
