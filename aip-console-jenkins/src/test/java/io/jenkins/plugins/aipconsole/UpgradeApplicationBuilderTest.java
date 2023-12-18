package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.utils.SemVerUtils;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doReturn;

public class UpgradeApplicationBuilderTest extends BaseBuilderTest{

    @InjectMocks
    private UpgradeApplicationBuilder upgradeApplicationBuilder;

    @Before
    public void setUp() throws Exception {
        upgradeApplicationBuilder = new UpgradeApplicationBuilder(BaseBuilderTest.TEST_APP_NAME);
        MockitoAnnotations.initMocks(this);
        ApiInfoDto apiInfo = ApiInfoDto.builder().apiVersion(SemVerUtils.getMinCompatibleVersion().toString()).build();
        doReturn(ApiInfoDto.builder().apiVersion(apiInfo.getApiVersion()).build())
                .when(restApiService).getAipConsoleApiInfo();
        doReturn(TEST_APP).when(applicationService).getApplicationFromName(TEST_APP_NAME);
    }

    @Test
    public void testUpgradeApplicationToJob() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(upgradeApplicationBuilder);
        project = jenkins.configRoundtrip(project);
        UpgradeApplicationBuilder job = new UpgradeApplicationBuilder(TEST_APP_NAME);
        jenkins.assertEqualDataBoundBeans(job, project.getBuildersList().get(0));
    }

}
