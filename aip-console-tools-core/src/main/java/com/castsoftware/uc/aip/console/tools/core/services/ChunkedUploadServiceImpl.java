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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Log
public class ChunkedUploadServiceImpl implements ChunkedUploadService {

    private static final int MAX_CHUNK_SIZE = 10 * 1024 * 1024;

    private RestApiService restApiService;

    public ChunkedUploadServiceImpl(RestApiService restApiService) {
        this.restApiService = restApiService;
    }

    @Override
    public boolean uploadFile(String appGuid, String archiveFile) throws UploadException {
        assert StringUtils.isNotEmpty(appGuid);
        assert StringUtils.isNotEmpty(archiveFile);

        Path archivePath = Paths.get(archiveFile);
        assert Files.exists(archivePath);

        long fileSize;
        try {
            fileSize = Files.size(archivePath);
        } catch (IOException e) {
            throw new UploadException("Unable to get archive size for given file " + archivePath, e);
        }
        String createUploadEndpoint = ApiEndpointHelper.getApplicationCreateUploadPath(appGuid);
        CreateUploadRequest request = new CreateUploadRequest();
        request.setFileName(FilenameUtils.getName(archivePath.toString()));
        request.setFileSize(fileSize);

        ChunkedUploadDto dto;
        try {
            log.fine("Creating a new upload for application");
            log.fine("Params : " + createUploadEndpoint + " " + request.getFileName() + " " + request.getFileSize());
            dto = restApiService.postForEntity(createUploadEndpoint, request, ChunkedUploadDto.class);
        } catch (ApiCallException e) {
            throw new UploadException("Unable to create upload", e);
        }

        assert StringUtils.isNotEmpty(dto.getGuid());
        String uploadChunkEndpoint = ApiEndpointHelper.getApplicationUploadPath(appGuid, dto.getGuid());

        try (InputStream is = FileUtils.openInputStream(archivePath.toFile())) {
            int currentOffset = 0;
            int totalChunks = (int) Math.ceil((double) fileSize / (double) MAX_CHUNK_SIZE);
            int currentChunk = 1;
            for (; currentOffset < fileSize; currentChunk++) {
                byte[] buffer = new byte[MAX_CHUNK_SIZE];
                int nbBytesRead = is.read(buffer);
                if (nbBytesRead < 0) {
                    throw new UploadException("Could not read more content from file, but did not reach EOF. Exiting.");
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

                Map<String, Object> content = new HashMap<>();
                content.put("metadata", metadata);
                if (nbBytesRead < MAX_CHUNK_SIZE) {
                    content.put("content", ArrayUtils.subarray(buffer, 0, nbBytesRead));
                } else {
                    content.put("content", buffer);
                }

                log.info(String.format("Uploading chunk %s of %s", currentChunk, totalChunks));
                log.fine("Uploading a chunk of " + fileSize + " bytes");

                dto = restApiService.exchangeMultipartForEntity("PATCH", uploadChunkEndpoint, headers, content, ChunkedUploadDto.class);
                currentOffset += nbBytesRead;

                assert dto != null;
                assert dto.getCurrentOffset() == currentOffset;
            }

        } catch (Exception e) {
            try {
                restApiService.deleteForEntity(uploadChunkEndpoint, null, String.class);
            } catch (ApiCallException inner) {
                if (dto != null) {
                    log.warning("Unable to remove failed upload with GUID '" + dto.getGuid() + "'");
                }
            }
            throw new UploadException("Error occurred during file upload", e);
        }

        return StringUtils.equalsAnyIgnoreCase(dto.getStatus(), "completed", "uploaded");
    }
}
