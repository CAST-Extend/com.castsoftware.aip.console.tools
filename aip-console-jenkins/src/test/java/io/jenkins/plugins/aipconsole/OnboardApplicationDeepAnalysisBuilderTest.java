package io.jenkins.plugins.aipconsole;

import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class OnboardApplicationDeepAnalysisBuilderTest extends BaseBuilderTest {
    @InjectMocks
    private OnboardApplicationDeepAnalysisBuilder onboardApplicationBuilder;

    @Before
    public void setUp() throws Exception {
        super.startUp();
        onboardApplicationBuilder = new OnboardApplicationDeepAnalysisBuilder(BaseBuilderTest.TEST_APP_NAME);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnboardApplicationDeepAnalysisJob() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(onboardApplicationBuilder);
        project = jenkins.configRoundtrip(project);
        Object builtProject = project.getBuildersList().get(0);
        OnboardApplicationDeepAnalysisBuilder expectedResults = new OnboardApplicationDeepAnalysisBuilder(TEST_APP_NAME);
        expectedResults.setSnapshotName("");
        jenkins.assertEqualDataBoundBeans(expectedResults, builtProject);
    }
}
