package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.exceptions.AipConsoleException;

public interface AipConsoleService {

    ApiInfoDto getAipConsoleApiInfo() throws AipConsoleException;

    void updateModuleGenerationType(String applicationGuid, JobRequestBuilder builder, ModuleGenerationType generationType);
}
