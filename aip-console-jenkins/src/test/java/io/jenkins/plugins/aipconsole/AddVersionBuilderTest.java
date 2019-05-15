package io.jenkins.plugins.aipconsole;

import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.uc.aip.console.tools.core.services.ChunkedUploadService;
import com.castsoftware.uc.aip.console.tools.core.services.JobsService;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiService;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

@Ignore
public class AddVersionBuilderTest {
    private static final String TEST_APP_GUID = "appGuid";
    private static final String TEST_ARCHIVE_NAME = "archive.zip";
    private static final String TEST_JOB_GUID = "jobGuid";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private RestApiService restApiService;

    @Mock
    private ChunkedUploadService chunkedUploadService;

    @Mock
    private JobsService jobsService;

    @InjectMocks
    private AddVersionBuilder addVersionBuilder;

    @Before
    public void setUp() {
        addVersionBuilder = new AddVersionBuilder(TEST_APP_GUID, TEST_ARCHIVE_NAME);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAddVersionStepToJob() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(addVersionBuilder);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new AddVersionBuilder(TEST_APP_GUID, TEST_ARCHIVE_NAME), project.getBuildersList().get(0));
    }

    @Test
    public void testBuildOk() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(addVersionBuilder);

        doNothing().
            when(restApiService).validateUrlAndKey("http://localhost:8081", "cast", "cast");
        doReturn(true)
                .when(chunkedUploadService).uploadFile(TEST_APP_GUID, TEST_ARCHIVE_NAME);
        doReturn(TEST_JOB_GUID)
                .when(jobsService).startAddVersionJob(eq(TEST_APP_GUID), eq(TEST_ARCHIVE_NAME), anyString(), any(Date.class), eq(false));
        doReturn(JobState.COMPLETED)
                .when(jobsService).pollAndWaitForJobFinished(TEST_JOB_GUID);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Analysis of application is completed.", build);
    }

}