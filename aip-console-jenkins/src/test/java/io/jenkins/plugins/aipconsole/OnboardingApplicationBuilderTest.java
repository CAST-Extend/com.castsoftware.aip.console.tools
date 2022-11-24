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
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnboardingApplicationJob() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(onboardingApplicationBuilder);
        project = jenkins.configRoundtrip(project);
        Object builtProject = project.getBuildersList().get(0);
        OnboardingApplicationBuilder expectedResults = new OnboardingApplicationBuilder("", "");
        expectedResults.setDomainName("");
        jenkins.assertEqualDataBoundBeans(expectedResults, builtProject);
    }
}
