package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelLinkDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.CheckModelReportRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.CheckModelUploadRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.PathRequest;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.FileUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.java.Log;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.fileupload.FileUploadBase;
import org.springframework.http.MediaType;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Log
public class ArchitectureStudioServiceImpl implements ArchitectureStudioService {

    private final RestApiService restApiService;

    public ArchitectureStudioServiceImpl(RestApiService restApiService) {
        this.restApiService = restApiService;
    }

    public Set<ArchitectureModelDto> getArchitectureModels() throws ApplicationServiceException {
        try {
            String modelUrl = ApiEndpointHelper.getArchitectureModelUrl();
            return restApiService.getForEntity(modelUrl, new TypeReference<Set<ArchitectureModelDto>>() {
            });
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to get architecture models", e);
        }
    }

    public Response uploadArchitectureModel(String filePath, Boolean isTemplate) throws ApplicationServiceException {
        try {
            File file = new File(filePath);
            String uploadModelUrl = ApiEndpointHelper.getArchitectureUploadModelEndpoint();
            Map<String, String> contentHeaderMap = new HashMap<>();
            contentHeaderMap.put(FileUploadBase.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            contentHeaderMap.put(FileUploadBase.CONTENT_DISPOSITION, "form-data; name=\"file\"; filename=\"" + file.getName() + "\"");

            Map<String, Map<String, String>> headers = new HashMap<>();
            headers.put("content", contentHeaderMap);
            Response resp = restApiService.exchangeMultipartForResponse("POST", uploadModelUrl, headers, file);
            return resp;
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to upload architecture model", e);
        }
    }

    public Set<ArchitectureModelLinkDto> modelChecker(String appGuid, String path, String caipVersion) throws ApiCallException {

        PathRequest pathRequest = PathRequest
                .builder()
                .path(path)
                .build();
        return restApiService.postForEntity(
                ApiEndpointHelper.getModelCheckUrl(appGuid),
                pathRequest,
                new TypeReference<Set<ArchitectureModelLinkDto>>(){}
        );
    }

    public void downloadCheckedModelReport(String appGuid, String modelName, Integer metricId, String description, Integer transactionId, Set<ArchitectureModelLinkDto> modelChecker, String reportPath) throws Exception {

        CheckModelReportRequest checkModelReportRequest = CheckModelReportRequest
                .builder()
                .description(description)
                .links(modelChecker)
                .metricId(metricId)
                .name(modelName)
                .transactionId(transactionId)
                .build();

        String modelUrl = ApiEndpointHelper.getDownlaodModelCheckUrl(appGuid);

        Response response = restApiService.exchangeForResponse(
                "POST",
                modelUrl,
                checkModelReportRequest
        );
        ResponseBody responseBody = response.body();

        String filename = getDownloadedFileName(modelName);
        //downloads the report
        if(responseBody != null) {
            FileUtils.fileDownload(filename, responseBody, reportPath);
        }
    }

    private String getDownloadedFileName(String modelName) {
        String cleanedModelName = modelName.replaceAll("[\\\\/:*?\"<>|]+", "");

        return String.format("%s_%s.xlsx", cleanedModelName, DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()));
    }

}
