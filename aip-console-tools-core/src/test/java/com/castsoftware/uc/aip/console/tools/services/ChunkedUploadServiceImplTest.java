package com.castsoftware.uc.aip.console.tools.services;

import com.castsoftware.uc.aip.console.tools.core.dto.upload.ChunkedUploadDto;
import com.castsoftware.uc.aip.console.tools.core.dto.upload.ChunkedUploadStatus;
import com.castsoftware.uc.aip.console.tools.core.dto.upload.CreateUploadRequest;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.uc.aip.console.tools.core.services.ChunkedUploadService;
import com.castsoftware.uc.aip.console.tools.core.services.ChunkedUploadServiceImpl;
import com.castsoftware.uc.aip.console.tools.core.services.RestApiService;
import com.castsoftware.uc.aip.console.tools.core.utils.ApiEndpointHelper;
import lombok.extern.java.Log;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Log
public class ChunkedUploadServiceImplTest {
    private static final String TEST_UPLOAD_GUID = "uploadGuid";
    private static final String TEST_APP_GUID = "appGuid";
    private static final String TEST_ZIP_FILENAME = "fake.zip";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private RestApiService restApiService;

    private ChunkedUploadService uploadService;

    private File fakeZip;

    @Before
    public void setUp() throws Exception {
        uploadService = new ChunkedUploadServiceImpl(restApiService);
        fakeZip = temporaryFolder.newFile(TEST_ZIP_FILENAME);
        Files.write(fakeZip.toPath(), "Some random content".getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = UploadException.class)
    public void testAssertionErrorNoAppGuid() throws Exception {
        uploadService.uploadFile(null, null);
    }

    @Test(expected = UploadException.class)
    public void testAssertionErrorNoUploadFile() throws Exception {
        uploadService.uploadFile(TEST_APP_GUID, null);
    }

    @Test(expected = UploadException.class)
    public void testAssertionErrorFileNotAZip() throws Exception {
        uploadService.uploadFile(TEST_APP_GUID, new File("somefile.notzip"));
    }

    @Test(expected = UploadException.class)
    public void testAssertionErrorFileNotExists() throws Exception {
        uploadService.uploadFile(TEST_APP_GUID, new File("C:/somefile.ZIP"));
    }

    @Test(expected = UploadException.class)
    public void testCreateUploadThrowsException() throws Exception {
        CreateUploadRequest expectedRequest = new CreateUploadRequest();
        expectedRequest.setFileName(TEST_ZIP_FILENAME);
        expectedRequest.setFileSize(fakeZip.length());

        doThrow(new ApiCallException(500))
                .when(restApiService)
                .postForEntity(
                        eq(ApiEndpointHelper.getApplicationCreateUploadPath(TEST_APP_GUID)),
                        eq(expectedRequest),
                        eq(ChunkedUploadDto.class)
                );

        uploadService.uploadFile(TEST_APP_GUID, fakeZip);
    }

    @Test(expected = UploadException.class)
    public void testFirstContentUploadThrowsException() throws Exception {
        log.info("Setting up");
        long fileSize = fakeZip.length();
        CreateUploadRequest expectedRequest = new CreateUploadRequest();
        expectedRequest.setFileName(TEST_ZIP_FILENAME);
        expectedRequest.setFileSize(fileSize);

        ChunkedUploadDto expectedDto = ChunkedUploadDto.builder()
                .guid(TEST_UPLOAD_GUID)
                .fileName(TEST_ZIP_FILENAME)
                .fileSize(fileSize)
                .applicationGuid(TEST_APP_GUID)
                .build();

        String uploadEndpoint = ApiEndpointHelper.getApplicationUploadPath(TEST_APP_GUID, TEST_UPLOAD_GUID);
        when(restApiService.postForEntity(eq("/api/applications/appGuid/upload"), eq(expectedRequest), eq(ChunkedUploadDto.class)))
                .thenReturn(expectedDto);
        when(restApiService.exchangeMultipartForEntity(eq("PATCH"), eq(uploadEndpoint), argThat(getChunkUploadMatcher()), argThat(getChunkUploadMatcher()), eq(ChunkedUploadDto.class)))
                .thenThrow(new ApiCallException(500));
        when(restApiService.deleteForEntity(anyString(), eq(null), eq(String.class)))
                .thenReturn("");

        uploadService.uploadFile(TEST_APP_GUID, fakeZip);
    }

    @Test
    public void testUploadComplete() throws Exception {
        long fileSize = fakeZip.length();
        CreateUploadRequest expectedRequest = new CreateUploadRequest();
        expectedRequest.setFileName(TEST_ZIP_FILENAME);
        expectedRequest.setFileSize(fileSize);
        ChunkedUploadDto.ChunkedUploadDtoBuilder expectedDtoBuilder = ChunkedUploadDto.builder()
                .fileName(TEST_ZIP_FILENAME)
                .fileSize(fileSize)
                .applicationGuid(TEST_APP_GUID);
        ChunkedUploadDto expectedDto = expectedDtoBuilder.build();
        expectedDto.setGuid(TEST_UPLOAD_GUID);

        ChunkedUploadDto afterUploadExpectedDto = expectedDtoBuilder
                .status(ChunkedUploadStatus.UPLOADED.name())
                .currentOffset(fileSize)
                .build();
        afterUploadExpectedDto.setGuid(TEST_UPLOAD_GUID);

        String uploadEndpoint = ApiEndpointHelper.getApplicationUploadPath(TEST_APP_GUID, TEST_UPLOAD_GUID);

        doReturn(expectedDto).
                when(restApiService).postForEntity(anyString(), eq(expectedRequest), eq(ChunkedUploadDto.class));

        doReturn(afterUploadExpectedDto).
                when(restApiService).exchangeMultipartForEntity(eq("PATCH"), eq(uploadEndpoint), argThat(getChunkUploadMatcher()), argThat(getChunkUploadMatcher()), eq(ChunkedUploadDto.class));

        assertTrue(uploadService.uploadFile(TEST_APP_GUID, fakeZip));

        verify(restApiService, Mockito.never()).deleteForEntity(anyString(), eq(null), eq(String.class));
    }

    private ArgumentMatcher<Map> getChunkUploadMatcher() {
        return argument -> argument.size() == 2
                && argument.get("metadata") != null
                && argument.get("content") != null;
    }
}