package com.castsoftware.uc.aip.console.tools.core.services;

import com.castsoftware.uc.aip.console.tools.core.exceptions.ApplicationServiceException;

public interface ApplicationService {
    /**
     * Retrieve an application's GUID from the given application name
     *
     * @param applicationName The name of the application
     * @return The application GUID if it found the application, null otherwise
     * @throws ApplicationServiceException If any error occurs while retrieving the application list from AIP Console
     */
    String getApplicationGuidFromName(String applicationName) throws ApplicationServiceException;

    /**
     * Retrieve an application's GUID from the given application name.
     * <p/>
     * If the "autoCreate" parameter is true and the application doesn't exist on AIP Console, it'll automatically create it
     * before returning the GUID.
     *
     * @param applicationName The name of the application to look up
     * @param autoCreate      Whether the application should be created if it couldn't be found.
     * @return The application GUID or null if none was found.
     * @throws ApplicationServiceException If any error occurs during the retrieval or creation of the application
     */
    String getOrCreateApplicationFromName(String applicationName, boolean autoCreate) throws ApplicationServiceException;
}
