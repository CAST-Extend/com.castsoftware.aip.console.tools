package com.castsoftware.uc.aip.console.tools.core.services;

import com.castsoftware.uc.aip.console.tools.core.exceptions.ApplicationServiceException;

public interface ApplicationService {
    String getOrCreateApplicationByName(String applicationName, boolean autoCreate) throws ApplicationServiceException;
}
