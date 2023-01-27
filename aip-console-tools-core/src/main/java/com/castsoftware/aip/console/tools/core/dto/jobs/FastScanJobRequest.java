package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.castsoftware.aip.console.tools.core.dto.ExclusionRuleDto;

import java.util.Set;

// Wrapper to old request
public class FastScanJobRequest extends DiscoverApplicationJobRequest {
    public FastScanJobRequest(String appGuid, String sourcePath, String deliveryConfigGuid, String onboardGuid, Set<String> ignorePatterns, Set<ExclusionRuleDto> exclusionRules, String versionName, boolean deleteVersion, String caipVersion, String targetNode, String fromVersionGuid, boolean backup, boolean saveSourceFolderZip) {
        super(appGuid, sourcePath, deliveryConfigGuid, onboardGuid, ignorePatterns, exclusionRules, versionName, deleteVersion, caipVersion, targetNode, fromVersionGuid, backup, saveSourceFolderZip);
    }
}
