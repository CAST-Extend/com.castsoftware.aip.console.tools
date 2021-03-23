package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.util.Secret;
import io.jenkins.plugins.aipconsole.config.AipConsoleGlobalConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

import static org.mockito.Mockito.doNothing;

public class DeliverVersionTest extends JenkinsBasicTest {

    @Mock
    private RestApiService restApiService;

    @Mock
    private UploadService uploadService;

    @Mock
    private JobsService jobsService;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private DeliverBuilder deliverBuilder;

    @Before
    public void setUp() throws Exception {
        AipConsoleGlobalConfiguration config = AipConsoleGlobalConfiguration.get();
        config.setAipConsoleUrl(JenkinsBasicTest.TEST_URL);
        config.setApiKey(Secret.fromString(JenkinsBasicTest.TEST_KEY));
        deliverBuilder = new DeliverBuilder(JenkinsBasicTest.TEST_APP_NAME, JenkinsBasicTest.TEST_ARCHIVE_NAME);
        MockitoAnnotations.initMocks(this);

        Mockito.doAnswer((Answer<ApiInfoDto>) invocation -> ApiInfoDto.builder().apiVersion("1.23.0-DEV").build()).when(restApiService).getAipConsoleApiInfo();

        Path mfPath = temporaryFolder.getRoot().toPath().resolve("MF_J2EE\\CICSSQLDemo\\Copylib");
        Files.createDirectories(mfPath);
        createTempFileAndGetPath(mfPath, "AUTHORS.CPY");
        deliverBuilder.setFilePath(JenkinsBasicTest.TEST_FOLDER_NAME);
    }

    @Test
    public void testDeliverDefault() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(deliverBuilder);
        project = jenkins.configRoundtrip(project);

        DeliverBuilder job = new DeliverBuilder(JenkinsBasicTest.TEST_APP_NAME, JenkinsBasicTest.TEST_FOLDER_NAME);
        job.setDomainName("");
        jenkins.assertEqualDataBoundBeans(job, project.getBuildersList().get(0));
    }

    @Test
    public void testScheduleDeliver() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(deliverBuilder);
        doNothing().
                when(restApiService).validateUrlAndKey(JenkinsBasicTest.TEST_URL, null, JenkinsBasicTest.TEST_KEY);

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, futureBuild.get());
        String log = JenkinsRule.getLog(build);


    }
}
