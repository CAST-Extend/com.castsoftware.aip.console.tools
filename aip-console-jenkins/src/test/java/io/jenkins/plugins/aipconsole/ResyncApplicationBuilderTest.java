package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.utils.SemVerUtils;
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
        resyncApplicationBuilder = new ResyncApplicationBuilder(BaseBuilderTest.TEST_APP_NAME);
        MockitoAnnotations.initMocks(this);
        ApiInfoDto apiInfo = ApiInfoDto.builder().apiVersion(SemVerUtils.getMinCompatibleVersion().toString()).build();
        doReturn(ApiInfoDto.builder().apiVersion(apiInfo.getApiVersion()).build())
                .when(restApiService).getAipConsoleApiInfo();
        doReturn(TEST_APP).when(applicationService).getApplicationFromGuid(TEST_APP_NAME);
    }

    @Test
    public void testResyncApplicationToJob() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(resyncApplicationBuilder);
        project = jenkins.configRoundtrip(project);
        ResyncApplicationBuilder job = new ResyncApplicationBuilder(TEST_APP_NAME);
        jenkins.assertEqualDataBoundBeans(job, project.getBuildersList().get(0));
    }

}
