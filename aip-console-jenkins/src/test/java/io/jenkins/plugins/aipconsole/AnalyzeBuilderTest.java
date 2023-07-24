package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.google.common.collect.Sets;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Set;
import java.util.concurrent.Future;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzeBuilderTest extends BaseBuilderTest {
    @InjectMocks
    private AnalyzeBuilder analyzeBuilder;

    @Before
    public void setUp() throws Exception {
        super.startUp();
        analyzeBuilder = new AnalyzeBuilder(BaseBuilderTest.TEST_APP_NAME);
        MockitoAnnotations.initMocks(this);
        doReturn(BaseBuilderTest.TEST_APP).when(applicationService).getApplicationFromGuid(BaseBuilderTest.TEST_APP_NAME);
        when(applicationService.getApplicationFromGuid(TEST_APP_GUID)).thenReturn(BaseBuilderTest.TEST_APP);
    }

    @Test
    public void testAnalyze_WithModulePreserveConfigured() throws Exception {
        analyzeBuilder.setVersionName(TEST_VERSION_NAME);
        analyzeBuilder.setModuleGenerationType(ModuleGenerationType.PRESERVE_CONFIGURED.toString());
        FreeStyleProject project = getProjectWithBuilder(analyzeBuilder);

        VersionDto versionDto = Mockito.mock(VersionDto.class);
        when(versionDto.getName()).thenReturn(TEST_VERSION_NAME);
        when(versionDto.getStatus()).thenReturn(VersionStatus.DELIVERED);
        Set<VersionDto> versions = Sets.newHashSet(versionDto);
        when(applicationService.getApplicationVersion(TEST_APP_GUID)).thenReturn(versions);

        doReturn(BaseBuilderTest.TEST_APP_NAME).when(applicationService).getApplicationGuidFromName(BaseBuilderTest.TEST_APP_NAME);
        doReturn(BaseBuilderTest.TEST_APP).when(applicationService).getApplicationFromName(BaseBuilderTest.TEST_APP_NAME);
        when(jobsService.startJob(any(JobRequestBuilder.class))).thenReturn(BaseBuilderTest.TEST_JOB_GUID);

        doReturn(JobState.COMPLETED).when(jobsService).pollAndWaitForJobFinished(eq(BaseBuilderTest.TEST_JOB_GUID), any(), any(), any(), eq(null));

        Future<FreeStyleBuild> futureBuild = project.scheduleBuild2(0);
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.SUCCESS, futureBuild.get());

        //jenkins.assertLogContains(OnbordingApplicationBuilder_DescriptorImpl_FastScanForbidden(), build);
    }
}
