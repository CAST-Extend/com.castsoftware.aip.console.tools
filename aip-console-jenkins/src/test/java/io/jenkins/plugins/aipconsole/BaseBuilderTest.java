package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.services.AipConsoleService;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.ArchitectureStudioService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.google.common.collect.Lists;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import hudson.util.Secret;
import io.jenkins.plugins.aipconsole.config.AipConsoleGlobalConfiguration;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BaseBuilderTest {
    protected static final String TEST_URL = "http://localhost:8081";
    protected static final String TEST_KEY = "key";
    protected static final String TEST_CONTENT = "test";
    protected static final String TEST_APP_NAME = "appName";
    protected static final String TEST_VERSION_NAME = "versionName";
    protected static final String TEST_APP_GUID = "app-GUID";
    protected static final String TEST_ARCHIVE_NAME = "archive.zip";
    protected static final String TEST_FOLDER_NAME = "testFolder";
    protected static final String TEST_JOB_GUID = "jobGuid";
    protected static final String TEST_NODE_NAME = "nodeName";
    protected static final String TEST_MODEL_NAME = "modelName";

    protected static final String TEST_UPLOAD_FILE_PATH = "C:\\Report\\model.CASTArchitect";
    protected static final String TEST_REPORT_PATH = "C:\\Report";
    protected static final ApplicationDto TEST_APP = ApplicationDto.builder().name(TEST_APP_NAME).guid(TEST_APP_GUID).inPlaceMode(false).build();
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
    protected ArchitectureStudioService architectureStudioService;
    @Mock
    protected AipConsoleService aipConsoleService;

    public void startUp() throws Exception {
        AipConsoleGlobalConfiguration config = AipConsoleGlobalConfiguration.get();
        config.setAipConsoleUrl(TEST_URL);
        config.setApiKey(Secret.fromString(TEST_KEY));
    }

    protected Path createTempFileAndGetPath(String fileName) throws IOException {
        File tempFile = temporaryFolder.newFile(fileName);
        Files.write(tempFile.toPath(), Lists.newArrayList(BaseBuilderTest.TEST_CONTENT));
        return tempFile.toPath();
    }

    protected Path createTempFolderAndGetPath(String folderName) throws IOException {
        File tempFolder = temporaryFolder.newFolder(folderName);
        return tempFolder.toPath();
    }

    protected FreeStyleProject getProjectWithBuilder(Builder builder) throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(builder);
        return project;
    }
}
