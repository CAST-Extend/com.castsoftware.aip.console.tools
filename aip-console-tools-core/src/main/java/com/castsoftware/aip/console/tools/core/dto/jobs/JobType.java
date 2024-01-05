package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JobType {

    ANALYZE(Constants.ANALYZE, "Analyze"),

    UPDATE_ANALYSIS_DATA(Constants.UPDATE_ANALYSIS_DATA, "Update analysis data"),
    //V1 type
    PREPARE_ANALYSIS_DATA(Constants.UPDATE_ANALYSIS_DATA, "Update analysis data"),

    REJECT_VERSION(Constants.REJECT_VERSION, "Reject version"),

    DELETE_VERSION(Constants.DELETE_VERSION, "Delete version"),

    PURGE_VERSION(Constants.PURGE_VERSION, "Purge version"),

    DELETE_SNAPSHOT(Constants.DELETE_SNAPSHOT, "Delete snapshot"),

    DELETE_SELECTED_SNAPSHOTS(Constants.DELETE_SELECTED_SNAPSHOTS, "Delete selected snapshots"),

    RENAME_SNAPSHOT(Constants.RENAME_SNAPSHOT, "Rename snapshot"),

    PUBLISH_ALL(Constants.PUBLISH_ALL, "Publish all application snapshots"),

    PUBLISH(Constants.PUBLISH, "Publish application snapshot"),
    // V1 types for PUBLISH
    UPLOAD_APPLICATION(Constants.UPLOAD_APPLICATION, "Publish application snapshot"),
    PUBLISH_APPLICATION(Constants.PUBLISH_APPLICATION, "Publish application snapshot"),

    CONSOLIDATE_SNAPSHOT(Constants.CONSOLIDATE_SNAPSHOT, "Consolidate snapshot"),

    CREATE_APPLICATION(Constants.CREATE_APPLICATION, "Create application"),
    DECLARE_APPLICATION(Constants.DECLARE_APPLICATION, "Create application"),
    ONBOARD_APPLICATION(Constants.ONBOARD_APPLICATON, "Onboard Application"),
    // Keep those discover and rediscover job types for history
    DISCOVER_APPLICATION(Constants.DISCOVER_APPLICATON, "Discover Application"),
    REDISCOVER_APPLICATION(Constants.REDISCOVER_APPLICATON, "Re-Discover Application"),
    FAST_SCAN(Constants.FAST_SCAN, "Fast Scan"),
    FIRST_SCAN_APPLICATION(Constants.FIRST_SCAN_APPLICATION, "First time Scan Application"),
    RESCAN_APPLICATION(Constants.RESCAN_APPLICATON, "ReScan Application"),
    DEEP_ANALYSIS(Constants.DEEP_ANALYSIS, "Deep Analysis"),
    PROCESS_IMAGING(Constants.PROCESS_IMAGING, "Upload to CAST-Imaging"),

    REFRESH_ONBOARDING_DELIVERY_CONFIGURATION(Constants.REFRESH_ONBOARDING_DELIVERY_CONFIGURATION, "Refresh Onboarding Delivery Configuration"),

    DELETE_APPLICATION(Constants.DELETE_APPLICATION, "Delete application"),

    FUNCTION_POINTS(Constants.FUNCTION_POINTS, "Compute function points"),

    BACKUP(Constants.BACKUP, "Backup"),

    BACKUP_MEASUREMENT(Constants.BACKUP_MEASUREMENT, "Backup measurement database"),

    RESTORE(Constants.RESTORE, "Restore"),

    UPGRADE_APPLICATION(Constants.UPGRADE_APPLICATION, "Upgrade application"),

    OPTIMIZE(Constants.OPTIMIZE, "Optimize"),

    OPTIMIZE_MEASUREMENT(Constants.OPTIMIZE_MEASUREMENT, "Optimize Measurement"),

    SHERLOCK_BACKUP(Constants.SHERLOCK_BACKUP, "Create a Sherlock backup"),

    ADD_VERSION(Constants.ADD_VERSION, "Add version"),

    CLONE_VERSION(Constants.CLONE_VERSION, "Clone version"),

    EDIT_VERSION(Constants.EDIT_VERSION, "Edit version"),

    RESYNC_APPLICATION(Constants.RESYNC_APPLICATION, "Resync application"),
    //V1 type
    SYNC_APPLICATION(Constants.SYNC_APPLICATION, "Resync application"),

    RENAME_APPLICATION(Constants.RENAME_APPLICATION, "Rename application"),

    ANALYZE_SECURITY_DATAFLOW(Constants.ANALYZE_SECURITY_DATAFLOW, "Analyze security dataflow"),
    //V1 type
    DATAFLOW_SECURITY_ANALYZE(Constants.ANALYZE_SECURITY_DATAFLOW, "Analyze security dataflow"),

    ANALYZE_EXEC_GROUP(Constants.ANALYZE_EXEC_GROUP, "Analyze execution group"),
    //V1 type
    RUN_ANALYZE_UNITS(Constants.RUN_ANALYZE_UNITS, "Analyze execution group"),

    INSTALL_EXTENSIONS(Constants.INSTALL_EXTENSIONS, "Install extensions"),
    //V1 type
    UPDATE_EXTENSIONS(Constants.UPDATE_EXTENSIONS, "Install extensions"),

    UPDATE_ASSESSMENT_MODEL(Constants.UPDATE_ASSESSMENT_MODEL, "Update Assessment Model"),

    CLEANUP_SNAPSHOTS(Constants.CLEANUP_SNAPSHOTS, "Cleanup snapshots"),

    CLEANUP_VERSIONS(Constants.CLEANUP_VERSIONS, "Cleanup versions"),

    RECOMPUTE_CHECKSUMS(Constants.RECOMPUTE_CHECKSUMS, "Recompute checksums"),

    RECOMPUTE_SNAPSHOT_INDICATORS(Constants.RECOMPUTE_SNAPSHOT_INDICATORS, "Recompute snapshot indicators"),

    RECOMPUTE_APPLICATION_INDICATORS(Constants.RECOMPUTE_APPLICATION_INDICATORS, "Recompute application indicators"),

    REFERENCE_FINDER(Constants.REFERENCE_FINDER, "Run Reference Finder");

    private final String label;
    private final String displayName;

    JobType(String label, String displayName) {
        this.label = label;
        this.displayName = displayName;
    }

    @JsonCreator
    public static JobType fromString(String value) {
        return value == null ? null : JobType.valueOf(value.toUpperCase());
    }

    public String getLabel() {
        return label;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    @JsonValue
    public String toString() {
        return name().toLowerCase();
    }

    public String getSerializableValue() {
        return toString();
    }
}
