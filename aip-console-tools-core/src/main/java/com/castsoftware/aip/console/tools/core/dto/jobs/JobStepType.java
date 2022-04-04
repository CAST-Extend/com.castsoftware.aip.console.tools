package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Set;

import static java.util.stream.Collectors.joining;

@Getter
public enum JobStepType {
    ACCEPT("Accept version"),
    SETCURRENT("Set version as current"),
    UPDATE_EXTENSIONS("Update extensions"),
    ANALYZE("Analyze"),
    RUN_ANALYZE_UNITS("Run analysis on the current analysis unit's group"),
    PREPARE_ANALYSIS_DATA("Prepare analysis data"),
    PROCESS_IMAGING("Process Imaging"),
    SNAPSHOT("Create snapshot"),
    VALIDATE_SNAPSHOT("Validate snapshot"),
    CONSOLIDATE_SNAPSHOT("Consolidate snapshot"),
    ANALYSIS_REPORT("Analysis Report"),
    // app management
    BACKUP("Backup"),
    BACKUP_MEASUREMENT("Backup Measurement"),
    RESTORE("Restore"),
    UPGRADE_APPLICATION("Upgrade application"),
    CSS_OPTIMIZE("Css Optimize"),
    CSS_OPTIMIZE_MEASUREMENT("Css Optimize Measure"),
    SHERLOCK_BACKUP("Sherlock backup"),
    DATAFLOW_SECURITY_ANALYZE("Dataflow security analyze"),
    RENAME_APPLICATION("Rename application"),
    // declare application
    CREATE_DELIVERY_FOLDER("Create delivery folder"),
    CREATE_TRIPLET_SCHEMA("Create triplet schema"),
    CREATE_LOCAL_DB("Create local schema"),
    CREATE_CENTRAL_DB("Create central schema"),
    CREATE_MNGT_DB("Create management schema"),
    CREATE_COMBINED_DB("Create combined schema"),
    IMPORT_PREFERENCES("Import preferences"),
    MANAGE_APPLICATION("Manage application"),
    IMPORT_ASSESSMENT_MODEL("Import assessment model"),
    UPDATE_ASSESSMENT_MODEL("Update assessment model"),
    RESTORE_TRIPLET("Restore triplet"),
    // delete application
    APPLICATION_CLEANUP_MEASURE("Application cleanup measure"),
    UNMANAGE_APPLICATION("Unmanage application"),
    DELETE_TRIPLET_SCHEMA("Delete triplet schema"),
    CLEANUP_DATABASE("Cleanup database"),
    REMOVE_APPLICATION_DATA("Remove application data"),
    DEREGISTER_APPLICATION("Deregister application"),
    // dmt
    UNZIP_SOURCE("Unzip source"),
    CODE_SCANNER("Code scanner"),
    INSTALL_EXTENSIONS("Install extensions"),
    CREATE_PACKAGE("Create package"),
    ADD_VERSION("Add version"),
    ATTACH_PACKAGE_TO_VERSION("Attach package to version"),
    COPY_FROM_VERSION("Copy from version"),
    PREPARE_VERSION("Prepare version"),
    COPY_AND_ATTACH_PACKAGES("Copy and attach packages"),
    UPDATE_PACKAGE("Update package"),
    DELIVER_VERSION("Deliver version"),
    ADD_PACKAGE("Update packages"),
    // snapshot
    DELETE_SNAPSHOT("Delete snapshot"),
    DELETE_SNAPSHOTS("Delete snapshots"),
    CLEANUP_SNAPSHOTS("Cleanup snapshots"),
    UPLOAD_APPLICATION("Upload application"),
    SNAPSHOT_INDICATOR("Snapshot indicator"),
    RENAME_SNAPSHOT("Rename snapshot"),
    // version
    DELETE_VERSION("Delete version"),
    PURGE_VERSION("Purge version"),
    REJECT_VERSION("Reject version"),
    CLEANUP_VERSIONS("Cleanup versions"),
    FUNCTION_POINTS("Calculate function points"),
    RECOMPUTE_CHECKSUMS("Recompute checksums"),
    RECOMPUTE_SNAPSHOT_INDICATORS("Recompute snapshot indicators"),
    // sync application
    RELOAD_APPLICATION("Reload application"),
    // ref-finder
    REFERENCE_FINDER("Reference Finder");

    public static final String JOBSTEPTYPE_DELIMITER = ";";

    private String label;

    JobStepType(String label) {
        this.label = label;
    }

    @JsonCreator
    public static JobStepType fromString(String value) {
        return value == null ? null : JobStepType.valueOf(value.toUpperCase());
    }

    @Override
    @JsonValue
    public String toString() {
        return name().toLowerCase();
    }

    public String getSerializableValue() {
        return toString();
    }

    public static String toJoinedString(Set<JobStepType> skipSteps) {
        return skipSteps.stream().map(JobStepType::toString).collect(joining(JobStepType.JOBSTEPTYPE_DELIMITER));
    }
}

