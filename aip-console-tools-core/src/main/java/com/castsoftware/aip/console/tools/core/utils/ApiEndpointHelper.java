package com.castsoftware.aip.console.tools.core.utils;

public class ApiEndpointHelper {

    private static final String ROOT_PATH = "/api";
    private static final String APPLICATIONS_ENDPOINT = "/applications";
    private static final String JOBS_ENDPOINT = "/jobs";
    private static final String UPLOAD_ENDPOINT = "/upload";
    private static final String VERSIONS_ENDPOINT = "/versions";

    public static String getLoginPath() {
        return ROOT_PATH + "/";
    }

    public static String getApplicationsPath() {
        return ROOT_PATH + APPLICATIONS_ENDPOINT;
    }

    public static String getApplicationPath(String appGuid) {
        assert appGuid != null && !appGuid.isEmpty();

        return getApplicationsPath() + "/" + appGuid;
    }

    public static String getApplicationVersionsPath(String appGuid) {
        assert appGuid != null && !appGuid.isEmpty();
        return getApplicationPath(appGuid) + "/" + VERSIONS_ENDPOINT;
    }

    public static String getApplicationCreateUploadPath(String appGuid) {
        assert appGuid != null && !appGuid.isEmpty();

        return getApplicationPath(appGuid) + UPLOAD_ENDPOINT;
    }

    public static String getApplicationUploadPath(String appGuid, String uploadGuid) {
        assert appGuid != null && !appGuid.isEmpty();
        assert uploadGuid != null && !uploadGuid.isEmpty();

        return getApplicationCreateUploadPath(appGuid) + "/" + uploadGuid;
    }

    public static String getJobsEndpoint() {
        return ROOT_PATH + JOBS_ENDPOINT;
    }

    public static String getJobDetailsEndpoint(String jobGuid) {
        assert jobGuid != null && !jobGuid.isEmpty();

        return getJobsEndpoint() + "/" + jobGuid;
    }
}
