package io.jenkins.plugins.aipconsole;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelLinkDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.Category;
import com.castsoftware.aip.console.tools.core.utils.SemVerUtils;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static io.jenkins.plugins.aipconsole.Messages.ArchitectureStudioBuilder_DescriptorImpl_success;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class ArchitectureStudioBuilderTest extends BaseBuilderTest{

    @InjectMocks
    private ArchitectureStudioBuilder architectureStudioBuilder;

    @Before
    public void setUp() throws Exception {
        architectureStudioBuilder = new ArchitectureStudioBuilder(BaseBuilderTest.TEST_APP_NAME);
        MockitoAnnotations.initMocks(this);

        ApiInfoDto apiInfoDto = ApiInfoDto.builder().apiVersion(SemVerUtils.getMinCompatibleVersion().toString()).build();
        doReturn(apiInfoDto).when(restApiService).getAipConsoleApiInfo();
        doReturn(apiInfoDto).when(applicationService).getAipConsoleApiInfo();
    }

    @Test
    public void testPerform_SuccessfulRun() throws Exception {
        FreeStyleProject project = getProjectWithBuilder(architectureStudioBuilder);

        ApplicationDto applicationDto = ApplicationDto.builder()
                .guid(BaseBuilderTest.TEST_APP_GUID)
                .name(BaseBuilderTest.TEST_APP_NAME)
                .build();

        Set<ArchitectureModelDto> expectedModels = new HashSet<>();

        expectedModels.add(new ArchitectureModelDto(
                "guid",
                "modelName",
                "path",
                0,
                0,
                false,
                "description",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "modelName",
                "fileName",
                Category.STANDARD_LAYER,
                false));

        Set<ArchitectureModelLinkDto> modelChecker = new HashSet<>();
        modelChecker.add(new ArchitectureModelLinkDto());

        architectureStudioBuilder.setModelName(BaseBuilderTest.TEST_MODEL_NAME);
        architectureStudioBuilder.setUploadFilePath(BaseBuilderTest.TEST_UPLOAD_FILE_PATH);
        architectureStudioBuilder.setReportPath(BaseBuilderTest.TEST_REPORT_PATH);

        when(architectureStudioService.getArchitectureModels()).thenReturn(expectedModels);
        when(applicationService.getApplicationFromName(BaseBuilderTest.TEST_APP_NAME)).thenReturn(applicationDto);

        when(applicationService.getApplicationFromName(BaseBuilderTest.TEST_APP_NAME)).thenReturn(applicationDto);
        doNothing().when(restApiService).validateUrlAndKey(BaseBuilderTest.TEST_URL, null, BaseBuilderTest.TEST_KEY);
        when(architectureStudioService.modelChecker(BaseBuilderTest.TEST_APP_GUID, "path", applicationService.getAipConsoleApiInfo().getApiVersion())).thenReturn(modelChecker);

        doNothing().when(architectureStudioService).downloadCheckedModelReport(
                BaseBuilderTest.TEST_APP_GUID,
                "modelName",
                0,
                "description",
                0,
                modelChecker,
                BaseBuilderTest.TEST_REPORT_PATH);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains(ArchitectureStudioBuilder_DescriptorImpl_success(), build);
    }


}


