package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;

import java.util.Set;

public interface ApplicationService {
    /**
     * Retrieve an application's GUID from the given application name
     *
     * @param applicationName The name of the application
     * @return The application GUID if it found the application, null otherwise
     * @throws ApplicationServiceException If any error occurs while retrieving the application list from AIP Console
     */
    String getApplicationGuidFromName(String applicationName) throws ApplicationServiceException;

    String getApplicationNameFromGuid(String applicationGuid) throws ApplicationServiceException;

    /**
     * Checks whether the application has any versions
     *
     * @param applicationGuid The application GUID
     * @return True if no version exists for the given application, false otherwise
     * @throws ApplicationServiceException If any error occurs while retrieving the list of version from AIP Console
     */
    boolean applicationHasVersion(String applicationGuid) throws ApplicationServiceException;

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

    /**
     * Retrieve an application's GUID from the given application name.
     * <p/>
     * If the "autoCreate" parameter is true and the application doesn't exist on AIP Console, it'll automatically create it
     * before returning the GUID.
     *
     * @param applicationName The name of the application to look up
     * @param autoCreate      Whether the application should be created if it couldn't be found
     * @param nodeName        The name of the node on which the application
     * @return An application GUID or null if non was found
     * @throws ApplicationServiceException
     */
    String getOrCreateApplicationFromName(String applicationName, boolean autoCreate, String nodeName) throws ApplicationServiceException;

    /**
     * Retrieve an application's version
     *
     * @param appGuid The applicatino GUID
     * @return A Set of VersionDtos
     * @throws ApplicationServiceException
     */
    Set<VersionDto> getApplicationVersion(String appGuid) throws ApplicationServiceException;

    /**
     * Create delivery configuration add exclusion patterns
     *
     * @param exclusionPatterns
     * @return
     */
    String createDeliveryConfiguration(String appGuid, String exclusionPatterns) throws ApiCallException;
}
