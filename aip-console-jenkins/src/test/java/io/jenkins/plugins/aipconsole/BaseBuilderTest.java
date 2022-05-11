package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.services.AipConsoleService;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import hudson.util.Secret;
import io.jenkins.plugins.aipconsole.config.AipConsoleGlobalConfiguration;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;

import java.io.IOException;

public class BaseBuilderTest {
    protected static final String TEST_URL = "http://localhost:8081";
    protected static final String TEST_KEY = "key";
    protected static final String TEST_CONTENT = "test";
    protected static final String TEST_APP_NAME = "appName";
    protected static final String TEST_ARCHIVE_NAME = "archive.zip";
    protected static final String TEST_JOB_GUID = "jobGuid";
    protected static final String TEST_NODE_NAME = "nodeName";
    protected static final ApplicationDto TEST_APP = ApplicationDto.builder().name(TEST_APP_NAME).guid(TEST_APP_NAME).inPlaceMode(false).build();
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    protected RestApiService restApiService;

    @Mock
    protected UploadService uploadService;

    @Mock
    protected JobsService jobsService;

    @Mock
    protected ApplicationService applicationService;
    @Mock
    protected AipConsoleService aipConsoleService;

    public void startUp() throws Exception {
        AipConsoleGlobalConfiguration config = AipConsoleGlobalConfiguration.get();
        config.setAipConsoleUrl(TEST_URL);
        config.setApiKey(Secret.fromString(TEST_KEY));
    }

    protected FreeStyleProject getProjectWithBuilder(Builder builder) throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(builder);
        return project;
    }
}
