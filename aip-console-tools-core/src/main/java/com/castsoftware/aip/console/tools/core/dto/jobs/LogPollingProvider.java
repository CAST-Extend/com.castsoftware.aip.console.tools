package com.castsoftware.aip.console.tools.core.dto.jobs;

import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;

public interface LogPollingProvider {
    String pollJobLog(String jobGuid) throws JobServiceException;
}
