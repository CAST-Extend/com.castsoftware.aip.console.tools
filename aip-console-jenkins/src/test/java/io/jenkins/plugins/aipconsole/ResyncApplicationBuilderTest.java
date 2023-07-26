package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doReturn;

public class ResyncApplicationBuilderTest extends BaseBuilderTest{

    @InjectMocks
    private ResyncApplicationBuilder resyncApplicationBuilder;

    @Before
    public void setUp() throws Exception {
        super.startUp();
        resyncApplicationBuilder = new ResyncApplicationBuilder(BaseBuilderTest.TEST_APP_GUID);
        MockitoAnnotations.initMocks(this);
        doReturn(ApiInfoDto.builder().apiVersion("2.0.0-SNAPSHOT-256").build())
                .when(restApiService).getAipConsoleApiInfo();
        doReturn(TEST_APP).when(applicationService).getApplicationFromGuid(TEST_APP_GUID);
    }

    @Test
    public void testResyncApplicationToJob() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(resyncApplicationBuilder);
        project = jenkins.configRoundtrip(project);
        ResyncApplicationBuilder job = new ResyncApplicationBuilder(TEST_APP_GUID);
        jenkins.assertEqualDataBoundBeans(job, project.getBuildersList().get(0));
    }

}
