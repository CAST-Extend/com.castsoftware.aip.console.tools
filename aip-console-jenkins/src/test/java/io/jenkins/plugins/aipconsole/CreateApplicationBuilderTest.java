package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.DatabaseConnectionSettingsDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.utils.SemVerUtils;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.function.Consumer;
import java.util.function.Function;

import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_info_startJob;
import static io.jenkins.plugins.aipconsole.Messages.CreateApplicationBuilder_CreateApplication_info_success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateApplicationBuilderTest extends BaseBuilderTest {
    private static final String TARGET_CSS_SERVER_NAME = "host.docker.internal:2285";
    private static final String TARGET_CSS_SERVER_GUID = "b6059ea8-cec9-4e62-86a6-065def8ebb69";

    @InjectMocks
    private CreateApplicationBuilder createApplicationBuilder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        createApplicationBuilder.setApplicationName(TEST_APP_NAME);
        createApplicationBuilder.setCssServerName(TARGET_CSS_SERVER_NAME);
        ApiInfoDto apiInfo = ApiInfoDto.builder().apiVersion(SemVerUtils.getMinCompatibleVersion().toString()).build();
        doReturn(apiInfo).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfo).when(applicationService).getAipConsoleApiInfo();
        doReturn(TEST_APP).when(applicationService).getApplicationFromGuid(TEST_APP_NAME);
    }

    @Test
    public void testCreateApplicationToJob() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(createApplicationBuilder);
        project = jenkins.configRoundtrip(project);
        CreateApplicationBuilder job = new CreateApplicationBuilder(TEST_APP_NAME);
        job.setCssServerName(TARGET_CSS_SERVER_NAME);
        job.setNodeName("");
        job.setDomainName("");
        jenkins.assertEqualDataBoundBeans(job, project.getBuildersList().get(0));
    }

    @Test
    public void testCreateApplicationBuilder_WithCssServer() throws Exception {
        doNothing().when(restApiService).validateUrlAndKey(TEST_URL, TEST_KEY);
        DatabaseConnectionSettingsDto oneDb = new DatabaseConnectionSettingsDto();
        oneDb.setDatabaseName("postgres");
        oneDb.setGuid(TARGET_CSS_SERVER_GUID);
        oneDb.setHost("host.docker.internal");
        oneDb.setPort(2285);
        oneDb.setSslEnabled(false);
        oneDb.setUserName("operator");
        when(restApiService.getForEntity("api/settings/css-settings", DatabaseConnectionSettingsDto[].class))
                .thenReturn(new DatabaseConnectionSettingsDto[]{oneDb});
        when(jobsService.startCreateApplication(any(String.class), eq(null), eq(null), anyBoolean(), eq(null), eq(TARGET_CSS_SERVER_NAME)))
                .thenReturn(TEST_JOB_GUID);
        when(jobsService.pollAndWaitForJobFinished(any(String.class), any(Consumer.class), any(Consumer.class), any(Function.class), eq(null))).thenReturn(JobState.COMPLETED);

        FreeStyleProject project = getProjectWithBuilder(createApplicationBuilder);
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        jenkins.assertLogContains(CreateApplicationBuilder_CreateApplication_info_startJob(), build);
        jenkins.assertLogContains(CreateApplicationBuilder_CreateApplication_info_success(TEST_APP_NAME, null), build);
    }
}
