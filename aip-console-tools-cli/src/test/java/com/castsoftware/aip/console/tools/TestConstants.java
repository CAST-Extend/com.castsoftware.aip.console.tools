package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;

import java.io.File;

public final class TestConstants {
    public static final String TEST_CREATRE_APP = "To_Create_App-name";
    public static final String TEST_API_KEY = "API-Key";
    public static final String TEST_APP_GUID = "APP-G-U-I-D";
    public static final String TEST_JOB_GUID = "JOB-G-U-I-D";
    public static final String TEST_DELIVERY_CONFIG_GUID = "DELIVERY-CONFIG-G-U-I-D";
    public static final String TEST_DOMAIN = "TEST-DOM";
    public static final String TEST_NODE = "TEST-NODE";
    public static final String TEST_BACKUP_NAME = "Bak-Name";
    public static final String TEST_VERSION_NAME = "V-1";
    public static final String TEST_SNAPSHOT_NAME = "Snapshot-1";
    public static final String TEST_SRC_FOLDER = "temp" + File.separator + "SubFolder";
    public static final String PROFILE_INTEGRATION_TEST = "INTEGRATION_PROFILE_TEST";
    public static final ApplicationDto TEST_APP = ApplicationDto.builder().guid(TEST_APP_GUID).name(TEST_CREATRE_APP).caipVersion("8.3.99").build();
}

