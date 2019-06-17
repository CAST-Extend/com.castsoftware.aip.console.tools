package com.castsoftware.uc.aip.console.tools.core.services;

import com.castsoftware.uc.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.uc.aip.console.tools.core.dto.Applications;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.JobServiceException;
import lombok.extern.java.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
@Log
public class ApplicationServiceImplTest {

    private static final String API_APP_ENDPOINT = "/api/applications";
    private static final String TEST_APP_NAME = "appName";
    private static final String TEST_APP_GUID = "appGuid";
    private static final String TEST_JOB_GUID = "jobGuid";

    @Mock
    private RestApiService restApiService;

    @Mock
    private JobsService jobsService;

    private ApplicationService applicationService;

    @Before
    public void setUp() {
        this.applicationService = new ApplicationServiceImpl(restApiService, jobsService);
    }

    @Test(expected = ApplicationServiceException.class)
    public void testGetOrCreateApplicationNoApplicationName() throws Exception {
        applicationService.getOrCreateApplicationByName(null, false);
        fail("Application service should throw an exception when application name is null");
    }

    @Test
    public void testGetOrCreateApplicationNoApplicationsNoAutoCreate() throws Exception {
        when(restApiService.getForEntity(API_APP_ENDPOINT, Applications.class))
                .thenReturn(new Applications());

        String appGuid = applicationService.getOrCreateApplicationByName(TEST_APP_NAME, false);
        assertNull("getOrCreateApplication should return null", appGuid);
    }

    @Test(expected = ApplicationServiceException.class)
    public void testGetOrCreateApplicationAipConsoleUnavailable() throws Exception {
        when(restApiService.getForEntity(API_APP_ENDPOINT, Applications.class))
                .thenThrow(new ApiCallException("fake exception"));

        applicationService.getOrCreateApplicationByName(TEST_APP_NAME, false);
        fail("Method should throw an exception due to error in restapiservice");
    }

    @Test
    public void testGetOrCreateApplicationApplicationFound() throws Exception {
        Set<ApplicationDto> appDtoSet = new HashSet<>();
        appDtoSet.add(ApplicationDto.builder()
                .name(TEST_APP_NAME)
                .guid(TEST_APP_GUID)
                .build());
        appDtoSet.add(ApplicationDto.builder()
                .name("OTHER APP")
                .guid("otherAppGuid")
                .build());

        Applications apps = new Applications(appDtoSet);

        when(restApiService.getForEntity(API_APP_ENDPOINT, Applications.class))
                .thenReturn(apps);

        String appGuid = applicationService.getOrCreateApplicationByName(TEST_APP_NAME, false);
        assertEquals("Result from getOrCreateApplication should be the test app guid 'appGuid'", TEST_APP_GUID, appGuid);
    }

    @Test(expected = ApplicationServiceException.class)
    public void testGetOrCreateApplicationCreateJobFailure() throws Exception {
        when(restApiService.getForEntity(API_APP_ENDPOINT, Applications.class))
                .thenReturn(new Applications());
        when(jobsService.startCreateApplication(TEST_APP_NAME))
                .thenThrow(new JobServiceException());

        applicationService.getOrCreateApplicationByName(TEST_APP_NAME, true);
        fail("Method should throw an exception when trying to create the application");
    }

    @Test
    public void testGetOrCreateApplicationJobFailed() throws Exception {
        when(restApiService.getForEntity(API_APP_ENDPOINT, Applications.class))
                .thenReturn(new Applications());
        when(jobsService.startCreateApplication(TEST_APP_NAME))
                .thenReturn(TEST_JOB_GUID);
        when(jobsService.pollAndWaitForJobFinished(eq(TEST_JOB_GUID), any()))
                .thenReturn(null);

        String appGuid = applicationService.getOrCreateApplicationByName(TEST_APP_NAME, true);
        assertNull("Application GUID should be null, as the create app job didn't finish properly", appGuid);
    }

    @Test
    public void testGetOrCreateApplicationOk() throws Exception {
        when(restApiService.getForEntity(API_APP_ENDPOINT, Applications.class))
                .thenReturn(new Applications());
        when(jobsService.startCreateApplication(TEST_APP_NAME))
                .thenReturn(TEST_JOB_GUID);
        when(jobsService.pollAndWaitForJobFinished(eq(TEST_JOB_GUID), any()))
                .thenReturn(TEST_APP_GUID);

        String appGuid = applicationService.getOrCreateApplicationByName(TEST_APP_NAME, true);
        assertEquals("Appguid should be the expected value", TEST_APP_GUID, appGuid);
    }
}