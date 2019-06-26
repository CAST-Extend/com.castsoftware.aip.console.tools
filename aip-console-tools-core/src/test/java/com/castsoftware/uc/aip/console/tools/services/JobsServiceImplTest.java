package com.castsoftware.uc.aip.console.tools.services;

import com.castsoftware.uc.aip.console.tools.core.dto.jobs.ChangeJobStateRequest;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.CreateJobsRequest;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobStatus;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobStatusWithSteps;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.JobType;
import com.castsoftware.uc.aip.console.tools.core.dto.jobs.SuccessfulJobStartDto;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.uc.aip.console.tools.core.services.JobsService;
import com.castsoftware.uc.aip.console.tools.core.services.JobsServiceImpl;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiService;
import com.castsoftware.uc.aip.console.tools.core.utils.Constants;
import lombok.extern.java.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
@Log
public class JobsServiceImplTest {
    private static final String TEST_APP_NAME = "appName";
    private static final String TEST_APP_GUID = "appGuid";
    private static final String TEST_ZIP_NAME = "file.zip";
    private static final String TEST_VERSION_NAME = "versionName";
    private static final String TEST_JOB_GUID = "jobGuid";

    @Mock
    private RestApiService restApiService;

    private JobsService service;

    @Before
    public void setUp() {
        service = new JobsServiceImpl(restApiService);
    }

    @Test(expected = JobServiceException.class)
    public void testCreateApplicationMissingAppName() throws Exception {
        service.startCreateApplication(null);
        fail("Creating application null name should throw an exception");
    }

    @Test(expected = JobServiceException.class)
    public void testCreateApplicationCreateJobFailed() throws Exception {
        Map<String, String> jobParams = new HashMap<>();
        jobParams.put(Constants.PARAM_APP_NAME, TEST_APP_NAME);
        CreateJobsRequest expectedRequest = new CreateJobsRequest();
        expectedRequest.setJobType(JobType.DECLARE_APPLICATION);
        expectedRequest.setJobParameters(jobParams);

        when(restApiService
                .postForEntity(eq("/api/jobs"), eq(expectedRequest), eq(SuccessfulJobStartDto.class))
        ).thenThrow(new ApiCallException(500));

        service.startCreateApplication(TEST_APP_NAME);
        fail("Create application should have failed due to api call exception");
    }

    @Test
    public void testCreateApplicationOk() throws Exception {
        Map<String, String> jobParams = new HashMap<>();
        jobParams.put(Constants.PARAM_APP_NAME, TEST_APP_NAME);
        CreateJobsRequest expectedRequest = new CreateJobsRequest();
        expectedRequest.setJobType(JobType.DECLARE_APPLICATION);
        expectedRequest.setJobParameters(jobParams);
        SuccessfulJobStartDto expectedResult = new SuccessfulJobStartDto();
        expectedResult.setJobGuid(TEST_JOB_GUID);
        expectedResult.setAppGuid(TEST_APP_GUID);

        when(restApiService
                .postForEntity(eq("/api/jobs"), eq(expectedRequest), eq(SuccessfulJobStartDto.class))
        ).thenReturn(expectedResult);

        String jobGuid = service.startCreateApplication(TEST_APP_NAME);
        assertEquals("Job guid should be the same as mocked guid returned", TEST_JOB_GUID, jobGuid);
    }

    @Test(expected = JobServiceException.class)
    public void testAddVersionMissingAppGuid() throws Exception {
        service.startAddVersionJob(null, null, null, null, false);
        fail("Method call should have thrown an exception");
    }

    @Test(expected = JobServiceException.class)
    public void testAddVersionMissingZipFileName() throws Exception {
        service.startAddVersionJob(TEST_APP_GUID, null, null, null, false);
    }

    @Test(expected = JobServiceException.class)
    public void testAddVersionMissingVersionReleaseDate() throws Exception {
        service.startAddVersionJob(TEST_APP_GUID, TEST_ZIP_NAME, null, null, false);
        fail("Method call should have thrown an exception");
    }

    @Test(expected = JobServiceException.class)
    public void testAddVersionCreateJobFailed() throws Exception {
        when(restApiService
                .postForEntity(anyString(), argThat(getCreateJobsRequestMatcher()), ArgumentMatchers.eq(SuccessfulJobStartDto.class))
        ).thenThrow(new ApiCallException(500));

        service.startAddVersionJob(TEST_APP_GUID, TEST_ZIP_NAME, TEST_VERSION_NAME, new Date(), false);
        fail("Method call should have thrown an exception");
    }

    @Test(expected = JobServiceException.class)
    public void testAddVersionResumeJobFailed() throws Exception {
        SuccessfulJobStartDto dto = new SuccessfulJobStartDto();
        dto.setAppGuid(TEST_APP_GUID);
        dto.setJobGuid(TEST_JOB_GUID);
        dto.setJobUrl("/api/jobs/" + TEST_JOB_GUID);
        JobStatusWithSteps status = new JobStatusWithSteps();
        status.setState(JobState.STARTING);

        when(restApiService
                .postForEntity(anyString(), argThat(getCreateJobsRequestMatcher()), ArgumentMatchers.eq(SuccessfulJobStartDto.class))
        ).thenReturn(dto);
        when(restApiService
                .getForEntity(anyString(), ArgumentMatchers.eq(JobStatusWithSteps.class))
        ).thenReturn(status);
        when(restApiService
                .putForEntity(anyString(), ArgumentMatchers.any(ChangeJobStateRequest.class), ArgumentMatchers.eq(String.class))
        ).thenThrow(new ApiCallException(500));

        service.startAddVersionJob(TEST_APP_GUID, TEST_ZIP_NAME, TEST_VERSION_NAME, new Date(), false);
        fail("Method call should have thrown an exception");
    }

    @Test
    public void testAddVersionCreateJobOk() throws Exception {
        SuccessfulJobStartDto dto = new SuccessfulJobStartDto();
        dto.setAppGuid(TEST_APP_GUID);
        dto.setJobGuid(TEST_JOB_GUID);
        dto.setJobUrl("/api/jobs/" + TEST_JOB_GUID);
        JobStatusWithSteps status = new JobStatusWithSteps();
        status.setState(JobState.STARTED);

        when(restApiService
                .postForEntity(anyString(), argThat(getCreateJobsRequestMatcher()), ArgumentMatchers.eq(SuccessfulJobStartDto.class))
        ).thenReturn(dto);
        when(restApiService
                .getForEntity(anyString(), ArgumentMatchers.eq(JobStatusWithSteps.class))
        ).thenReturn(status);

        try {
            String jobGuid = service.startAddVersionJob(TEST_APP_GUID, TEST_ZIP_NAME, TEST_VERSION_NAME, new Date(), false);
            assertEquals(dto.getJobGuid(), jobGuid);
        } catch (JobServiceException e) {
            log.log(Level.SEVERE, "JobServiceException : ", e);
            fail("Method call should not throw an exception");
        }
    }

    @Test(expected = AssertionError.class)
    public void testPollAndWaitForJobNoGuid() throws Exception {
        service.pollAndWaitForJobFinished(null);
    }

    @Test(expected = JobServiceException.class)
    public void testPollAndWaitForJobApiCallException() throws Exception {
        when(restApiService
                .getForEntity(ArgumentMatchers.eq("/api/jobs/" + TEST_JOB_GUID), ArgumentMatchers.eq(JobStatusWithSteps.class))
        ).thenThrow(new ApiCallException(500));

        service.pollAndWaitForJobFinished(TEST_JOB_GUID);
    }

    @Test
    public void testPollAndWaitForJobStatusCancelled() throws Exception {
        // Lower sleep duration to 1s instead of default 10
        lowerSleepDuration();
        JobStatus baseStatus = new JobStatus(TEST_APP_GUID, "/api/jobs/" + TEST_JOB_GUID, JobType.ADD_VERSION, JobState.STARTED, new HashMap<>(), new Date(), new Date(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
        JobStatusWithSteps initialStatus = new JobStatusWithSteps(baseStatus, "unzip_source", null);

        JobStatus errorBaseStatus = new JobStatus(TEST_APP_GUID, "/api/jobs/" + TEST_JOB_GUID, JobType.ADD_VERSION, JobState.CANCELED, new HashMap<>(), new Date(), new Date(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
        JobStatusWithSteps errorStatus = new JobStatusWithSteps(errorBaseStatus, Constants.CODE_SCANNER_STEP_NAME, Constants.CODE_SCANNER_STEP_NAME);

        when(restApiService
                .getForEntity("/api/jobs/" + TEST_JOB_GUID, JobStatusWithSteps.class)
        ).thenReturn(initialStatus, errorStatus);

        JobState resultState = service.pollAndWaitForJobFinished(TEST_JOB_GUID);
        assertEquals("Expected state should be 'CANCELED'", JobState.CANCELED, resultState);
    }

    private ArgumentMatcher<CreateJobsRequest> getCreateJobsRequestMatcher() {
        return argument -> {
            Map<String, String> jobParams = argument.getJobParameters();
            return argument.getJobType() == JobType.ADD_VERSION &&
                    jobParams.size() > 0 &&
                    TEST_APP_GUID.equalsIgnoreCase(jobParams.get(Constants.PARAM_APP_GUID)) &&
                    TEST_ZIP_NAME.equalsIgnoreCase(jobParams.get(Constants.PARAM_SOURCE_ARCHIVE));
        };
    }

    private void lowerSleepDuration() throws Exception {
        Field staticSleepDuration = JobsServiceImpl.class.getDeclaredField("POLL_SLEEP_DURATION");
        assert staticSleepDuration != null;
        staticSleepDuration.setAccessible(true);

        // Remove final modifier from static final field
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(staticSleepDuration, staticSleepDuration.getModifiers() & ~Modifier.FINAL);

        // update static value with new value
        staticSleepDuration.setLong(null, TimeUnit.SECONDS.toMillis(1));
    }
}