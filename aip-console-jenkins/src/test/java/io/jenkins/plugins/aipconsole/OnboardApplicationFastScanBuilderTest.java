package io.jenkins.plugins.aipconsole;

import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OnboardApplicationFastScanBuilderTest extends BaseBuilderTest {
    @InjectMocks
    private OnboardApplicationFastScanBuilder fastScanBuilder;

    @Before
    public void setUp() throws Exception {
        super.startUp();
        fastScanBuilder = new OnboardApplicationFastScanBuilder(BaseBuilderTest.TEST_APP_NAME, BaseBuilderTest.TEST_ARCHIVE_NAME);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnboardingApplicationFastScanJob() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(fastScanBuilder);
        project = jenkins.configRoundtrip(project);
        Object builtProject = project.getBuildersList().get(0);
        OnboardApplicationFastScanBuilder expectedResults = new OnboardApplicationFastScanBuilder(BaseBuilderTest.TEST_APP_NAME, "archive.zip");
        expectedResults.setDomainName("");
        jenkins.assertEqualDataBoundBeans(expectedResults, builtProject);
    }
}
