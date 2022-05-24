package com.castsoftware.aip.console.tools.core.utils;

public class ApiEndpointHelper {

    private static final String ROOT_PATH = "/api";
    private static final String APPLICATIONS_ENDPOINT = "/applications";
    private static final String JOBS_ENDPOINT = "/jobs";
    private static final String UPLOAD_ENDPOINT = "/upload";
    private static final String VERSIONS_ENDPOINT = "/versions";
    private static final String EXTRACT_ENDPOINT = "/extract";
    private static final String DEBUG_OPTIONS_ENDPOINT = "/debug-options";
    private static final String SHOW_SQL_ENDPOINT = "/show-sql";
    private static final String AMT_PROFILE_ENDPOINT = "/activate-amt-memory-profile";
    public static final String MODULE_OPTIONS_ENDPOINT = "/module-options";

    public static final String MODULE_OPTIONS_GENERATION_TYPE_ENDPOINT = MODULE_OPTIONS_ENDPOINT + "/generation-type";

    public static String getRootPath() {
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
        return getApplicationPath(appGuid) + VERSIONS_ENDPOINT;
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

    public static String getApplicationExtractUploadPath(String appGuid, String uploadGuid) {
        return getApplicationUploadPath(appGuid, uploadGuid) + EXTRACT_ENDPOINT;
    }

    public static String getDebugOptionsPath(String appGuid) {
        return getApplicationPath(appGuid) + DEBUG_OPTIONS_ENDPOINT;
    }

    public static String getDebugOptionShowSqlPath(String appGuid) {
        return getDebugOptionsPath(appGuid) + SHOW_SQL_ENDPOINT;
    }

    public static String getDebugOptionAmtProfilePath(String appGuid) {
        return getDebugOptionsPath(appGuid) + AMT_PROFILE_ENDPOINT;
    }

    public static String getAmtProfilingDownloadUrl(String appGuid) {
        return ApiEndpointHelper.getDebugOptionsPath(appGuid) + "/logs" + AMT_PROFILE_ENDPOINT + "/download";
    }

    public static String getJobsEndpoint() {
        return ROOT_PATH + JOBS_ENDPOINT;
    }

    public static String getJobDetailsEndpoint(String jobGuid) {
        assert jobGuid != null && !jobGuid.isEmpty();

        return getJobsEndpoint() + "/" + jobGuid;
    }

    public static String getModuleOptionsGenerationTypePath(String appGuid) {
        return getApplicationPath(appGuid) + MODULE_OPTIONS_GENERATION_TYPE_ENDPOINT;
    }
}
