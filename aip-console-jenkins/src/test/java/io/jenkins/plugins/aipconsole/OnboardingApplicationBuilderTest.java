package io.jenkins.plugins.aipconsole;

import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OnboardingApplicationBuilderTest extends BaseBuilderTest {
    @InjectMocks
    private OnboardingApplicationBuilder onboardingApplicationBuilder;

    @Before
    public void setUp() throws Exception {
        super.startUp();
        onboardingApplicationBuilder = new OnboardingApplicationBuilder(BaseBuilderTest.TEST_APP_NAME, BaseBuilderTest.TEST_ARCHIVE_NAME);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnboardingApplicationJob() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(onboardingApplicationBuilder);
        project = jenkins.configRoundtrip(project);
        Object builtProject = project.getBuildersList().get(0);
        OnboardingApplicationBuilder expectedResults = new OnboardingApplicationBuilder("", "archive.zip");
        expectedResults.setDomainName("");
        expectedResults.setOnboardStrategy("FAST_SCAN");
        expectedResults.setApplicationName(TEST_APP_NAME);
        jenkins.assertEqualDataBoundBeans(expectedResults, builtProject);
    }
}
