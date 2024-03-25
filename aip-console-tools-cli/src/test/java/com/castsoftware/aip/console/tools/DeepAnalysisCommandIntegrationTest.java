package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.BasicCallable;
import com.castsoftware.aip.console.tools.commands.DeepAnalysisCommand;
import com.castsoftware.aip.console.tools.core.dto.ApplicationCommonDetails;
import com.castsoftware.aip.console.tools.core.dto.ApplicationCommonDetailsDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationOnboardingDto;
import com.castsoftware.aip.console.tools.core.dto.ImagingSettingsDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobExecutionDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.ScanAndReScanApplicationJobRequest;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.services.ApplicationServiceImpl;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {AipConsoleToolsCliIntegrationTest.class})
@ActiveProfiles(TestConstants.PROFILE_INTEGRATION_TEST)
public class DeepAnalysisCommandIntegrationTest extends AipConsoleToolsCliBaseTest {

    @InjectMocks
    private DeepAnalysisCommand deepAnalysisCommand;
    @InjectMocks
    private ApplicationServiceImpl applicationServiceImpl;
    private ApplicationCommonDetails appDetails;
    private ApplicationCommonDetailsDto appDetailsDto;

    @Override
    protected void initializePrivateMocks() {
        assignMockedBeans(deepAnalysisCommand);
    }

    @Override
    protected void cleanupTestCommand() {
        // Reset shared options and command parameters between tests.
        resetSharedOptions(deepAnalysisCommand.getSharedOptions());
        deepAnalysisCommand.setApplicationName(null);
    }

    @Override
    protected void assignMockedBeans(BasicCallable command) {
        super.assignMockedBeans(command);

        //We need upload for this test
        Class<? extends BasicCallable> commandClass = command.getClass();
        Field uploadServiceField = ReflectionUtils.findField(commandClass, "uploadService");
        ReflectionUtils.makeAccessible(uploadServiceField);
        ReflectionUtils.setField(uploadServiceField, command, uploadService);
    }

    @Before
    public void setupMocks() throws IOException, ApplicationServiceException {
        ReflectionTestUtils.setField(applicationServiceImpl, "restApiService", restApiService);
        ReflectionTestUtils.setField(applicationServiceImpl, "jobService", jobsService);
        ReflectionTestUtils.setField(applicationServiceImpl, "uploadService", uploadService);

        // Mock application details
        appDetailsDto = new ApplicationCommonDetailsDto();
        appDetailsDto.setName(TestConstants.TEST_CREATE_APP);
        appDetailsDto.setGuid(TestConstants.TEST_APP_GUID);

        appDetails = new ApplicationCommonDetails();
        appDetails.setApplicationCommonDetailsDtoSet(Arrays.asList(appDetailsDto));

    }

    @Test
    public void testDeepAnalysis_SuccessfulDeepAnalysisWithImaging() throws Exception {
        when(applicationService.getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP)).thenReturn(appDetailsDto);
        mockDeepAnalyzeWithJobState(JobState.COMPLETED);
        runDeepAnalysisTest(TestConstants.TEST_CREATE_APP, ModuleGenerationType.ONE_PER_AU, true);
    }

    @Test
    public void testDeepAnalysis_SuccessfulDeepAnalysisWithoutImaging() throws Exception {
        when(applicationService.getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP)).thenReturn(appDetailsDto);
        mockDeepAnalyzeWithJobState(JobState.COMPLETED);
        when(applicationService.isImagingAvailable()).thenReturn(false);
        runDeepAnalysisTest(TestConstants.TEST_CREATE_APP, ModuleGenerationType.ONE_PER_AU, false);
    }

    @Test
    public void testDeepAnalysis_JobFailed() throws Exception {
        mockDeepAnalyzeWithJobState(JobState.FAILED);
        runDeepAnalysisTest(TestConstants.TEST_CREATE_APP, ModuleGenerationType.ONE_PER_AU, true);
    }

    @Test
    public void testDeepAnalysis_AppDoesNotExist() throws Exception {
        when(applicationService.getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP)).thenReturn(null);
        runDeepAnalysisTest(TestConstants.TEST_CREATE_APP, ModuleGenerationType.ONE_PER_AU, true);
    }

    // ** Helper Methods **

    private void runDeepAnalysisTest(String appName, ModuleGenerationType moduleType, boolean imagingAvailable) throws Exception {
        String[] args = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--app-name", appName,
                "--module-option", moduleType.toString()};

        deepAnalysisCommand.setModuleGenerationType(moduleType); // Optional for some assertions
        runStringArgs(deepAnalysisCommand, args);
        assertThat(exitCode, is(Constants.RETURN_OK));
    }

    private void mockDeepAnalyzeWithJobState(JobState jobState) throws JobServiceException, ApiCallException, ApplicationServiceException {
        doNothing().when(restApiService).validateUrlAndKey(anyString(), anyString(), anyString());

        when(applicationService.getApplicationDetailsFromName(TestConstants.TEST_CREATE_APP)).thenReturn(appDetailsDto);

        // Mock application onboarding details
        ApplicationOnboardingDto onboardedAppDto = Mockito.mock(ApplicationOnboardingDto.class);
        when(onboardedAppDto.getCaipVersion()).thenReturn("8.3.45");

        when(applicationService.getApplicationOnboarding(TestConstants.TEST_APP_GUID)).thenReturn(onboardedAppDto);

        // Mocks for imaging availability and settings (adjust as needed)
        when(applicationService.isImagingAvailable()).thenReturn(true);
        ImagingSettingsDto imagingDto = Mockito.mock(ImagingSettingsDto.class);
        when(imagingDto.isValid()).thenReturn(true);
        when(restApiService.getForEntity(ApiEndpointHelper.getImagingSettingsEndPoint(), ImagingSettingsDto.class)).thenReturn(imagingDto);

        // Mock job service behavior based on job state
        AtomicReference<ScanAndReScanApplicationJobRequest> jobRequest = null; // Capture request for assertions
        when(jobsService.startDeepAnalysis(any(ScanAndReScanApplicationJobRequest.class))).then(invocation -> {
            jobRequest.set(invocation.getArgument(0));
            assertThat(jobRequest.get().getAppGuid(), is(TestConstants.TEST_APP_GUID)); // Basic assertion
            // ... (add further assertions on request details as needed)
            return TestConstants.TEST_JOB_GUID;
        });

        JobExecutionDto jobStatus = new JobExecutionDto();
        jobStatus.setAppGuid(TestConstants.TEST_APP_GUID);
        jobStatus.setState(jobState);
        jobStatus.setCreatedDate(new Date());
        jobStatus.setAppName(TestConstants.TEST_CREATE_APP);
        if (jobState == JobState.COMPLETED) {
            when(jobsService.pollAndWaitForJobFinished(TestConstants.TEST_JOB_GUID, Function.identity(), true)).thenReturn(jobStatus);
        } else {
            when(jobsService.pollAndWaitForJobFinished(anyString(), any(Consumer.class), any(Consumer.class), any(Function.class), any(Supplier.class))).thenReturn(jobState.toString());
        }
    }

}
