package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.google.common.collect.Lists;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class JenkinsBasicTest {
    protected static final String TEST_URL = "http://localhost:8081";
    protected static final String TEST_KEY = "key";
    protected static final String TEST_CONTENT = "test";
    protected static final String TEST_APP_NAME = "appName";
    protected static final String TEST_ARCHIVE_NAME = "archive.zip";
    protected static final String TEST_FOLDER_NAME = "MF_J2EE";
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

    protected FreeStyleProject getProjectWithBuilder(Builder builder) throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(builder);
        return project;
    }

    protected String createTempFileAndGetPath(String fileName) throws IOException {
        File tempFile = temporaryFolder.newFile(fileName);
        Files.write(tempFile.toPath(), Lists.newArrayList(JenkinsBasicTest.TEST_CONTENT));
        return tempFile.getAbsolutePath();
    }

    protected String createTempFileAndGetPath(Path parent, String fileName) throws IOException {
        File tempFile = new File(parent.toFile(), fileName);
        tempFile.createNewFile();
        Files.write(tempFile.toPath(), Lists.newArrayList(JenkinsBasicTest.TEST_CONTENT));
        return tempFile.getAbsolutePath();
    }

}
