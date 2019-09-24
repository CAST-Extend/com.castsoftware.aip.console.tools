package com.castsoftware.uc.aip.console.tools.core.services;

import com.castsoftware.uc.aip.console.tools.core.dto.upload.ChunkedUploadDto;
import com.castsoftware.uc.aip.console.tools.core.dto.upload.ChunkedUploadMetadataRequest;
import com.castsoftware.uc.aip.console.tools.core.dto.upload.CreateUploadRequest;
import com.castsoftware.uc.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.uc.aip.console.tools.core.exceptions.UploadException;
import com.castsoftware.uc.aip.console.tools.core.utils.ApiEndpointHelper;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Log
public class ChunkedUploadServiceImpl implements ChunkedUploadService {

    private static final int MAX_CHUNK_SIZE = 10 * 1024 * 1024;

    private RestApiService restApiService;

    public ChunkedUploadServiceImpl(RestApiService restApiService) {
        this.restApiService = restApiService;
    }

    @Override
    public boolean uploadFile(String appGuid, File archiveFile) throws UploadException {
        if (StringUtils.isBlank(appGuid)) {
            throw new UploadException("No Application GUID provided.");
        }

        Path archivePath = archiveFile != null ? archiveFile.toPath() : null;
        if (archivePath == null || !Files.exists(archiveFile.toPath())) {
            throw new UploadException("No file provided for upload");
        }

        long fileSize;
        try {
            fileSize = Files.size(archivePath);
        } catch (IOException e) {
            throw new UploadException("Unable to get archive size for given file " + archivePath, e);
        }
        try (InputStream is = FileUtils.openInputStream(archivePath.toFile())) {
            return uploadInputStream(appGuid, FilenameUtils.getName(archivePath.toString()), fileSize, is);
        } catch (IOException e) {
            throw new UploadException("Unable to read file", e);
        }
    }

    @Override
    public boolean uploadInputStream(String appGuid, String fileName, long fileSize, InputStream content)
            throws UploadException {

        String createUploadEndpoint = ApiEndpointHelper.getApplicationCreateUploadPath(appGuid);
        CreateUploadRequest request = new CreateUploadRequest();
        request.setFileName(fileName);
        request.setFileSize(fileSize);

        ChunkedUploadDto dto;
        try {
            log.info("Creating a new upload for application");
            log.fine("Params : " + createUploadEndpoint + "\n" + request.toString());
            dto = restApiService.postForEntity(createUploadEndpoint, request, ChunkedUploadDto.class);
        } catch (ApiCallException e) {
            log.log(Level.SEVERE, "Error while tryign to create upload", e);
            throw new UploadException("Unable to create upload", e);
        }

        if (dto == null || StringUtils.isBlank(dto.getGuid())) {
            throw new UploadException("Upload was not created on AIP Console");
        }
        String uploadChunkEndpoint = ApiEndpointHelper.getApplicationUploadPath(appGuid, dto.getGuid());
        int currentChunk = 1;
        try {
            log.info("Starting chunks uploads");
            long currentOffset = 0;
            int totalChunks = (int) Math.ceil((double) fileSize / (double) MAX_CHUNK_SIZE);
            for (; currentOffset < fileSize; currentChunk++) {
                byte[] buffer = new byte[MAX_CHUNK_SIZE];
                int nbBytesRead = content.read(buffer);
                log.finer("Read " + nbBytesRead + " from file");
                if (nbBytesRead < 0) {
                    throw new UploadException("No more content to read, but file not complete (the file might be modified by another program?).");
                }

                ChunkedUploadMetadataRequest metadata = new ChunkedUploadMetadataRequest();
                metadata.setChunkSize(nbBytesRead);

                Map<String, String> metadataHeaderMap = new HashMap<>();
                metadataHeaderMap.put("Content-Type", "application/json");

                Map<String, String> contentHeaderMap = new HashMap<>();
                contentHeaderMap.put("Content-Disposition", "form-data; name=content; filename=filechunk");
                contentHeaderMap.put("Content-Type", "application/octet-stream");

                Map<String, Map<String, String>> headers = new HashMap<>();
                headers.put("metadata", metadataHeaderMap);
                headers.put("content", contentHeaderMap);

                Map<String, Object> body = new HashMap<>();
                body.put("metadata", metadata);
                if (nbBytesRead < MAX_CHUNK_SIZE) {
                    body.put("content", ArrayUtils.subarray(buffer, 0, nbBytesRead));
                } else {
                    body.put("content", buffer);
                }

                log.info(String.format("Uploading chunk %s of %s", currentChunk, totalChunks));
                log.fine("Uploading a chunk of " + fileSize + " bytes");

                dto = restApiService.exchangeMultipartForEntity("PATCH", uploadChunkEndpoint, headers, body, ChunkedUploadDto.class);
                currentOffset += nbBytesRead;

                assert dto != null;
                assert dto.getCurrentOffset() == currentOffset;
            }

        } catch (ApiCallException | IOException e) {
            log.info("Error occurred during upload. Trying to delete before failing.");
            try {
                restApiService.deleteForEntity(uploadChunkEndpoint, null, String.class);
            } catch (ApiCallException inner) {
                if (dto != null) {
                    log.warning("Unable to remove failed upload with GUID '" + dto.getGuid() + "'");
                }
            }
            throw new UploadException("Error occurred while uploading chunk number " + currentChunk, e);
        }

        return StringUtils.equalsAnyIgnoreCase(dto.getStatus(), "completed", "uploaded");
    }
}
