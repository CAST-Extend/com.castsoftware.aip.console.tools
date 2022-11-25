package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationOnboardingDto;
import com.castsoftware.aip.console.tools.core.dto.DebugOptionsDto;
import com.castsoftware.aip.console.tools.core.dto.DeliveryConfigurationDto;
import com.castsoftware.aip.console.tools.core.dto.DomainDto;
import com.castsoftware.aip.console.tools.core.dto.Exclusions;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogPollingProvider;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.PackagePathInvalidException;

import java.util.Date;
import java.util.Set;
import java.util.function.Consumer;

public interface ApplicationService {
    ApiInfoDto getAipConsoleApiInfo();

    String getApplicationGuidFromName(String applicationName) throws ApplicationServiceException;

    String getApplicationNameFromGuid(String applicationGuid) throws ApplicationServiceException;

    ApplicationDto getApplicationFromGuid(String applicationGuid) throws ApplicationServiceException;

    ApplicationDto getApplicationFromName(String applicationName) throws ApplicationServiceException;

    ApplicationDto getApplicationDetails(String applicationGuid) throws ApplicationServiceException;

    DomainDto getDomainFromName(String domainName) throws ApplicationServiceException;

    Date getVersionDate(String versionDateString) throws ApplicationServiceException;

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

    String onboardApplication(String applicationName, String domainName, boolean verbose, String sourcePath) throws ApplicationServiceException;

    String discoverApplication(String applicationGuid, String sourcePath, String versionName,
                               String caipVersion, String targetNode, boolean verbose, LogPollingProvider logPollingProvider) throws ApplicationServiceException;

    ApplicationOnboardingDto getApplicationOnboarding(String applicationGuid) throws ApplicationServiceException;

    boolean isOnboardingSettingsEnabled() throws ApplicationServiceException;

    void setEnableOnboarding(boolean enabled) throws ApplicationServiceException;

    boolean isImagingAvailable() throws ApplicationServiceException;

    String runFirstScanApplication(String applicationGuid, String targetNode, String caipVersion, boolean verbose, LogPollingProvider logPollingProvider) throws ApplicationServiceException;

    String runReScanApplication(String applicationGuid, String targetNode, String caipVersion, boolean verbose, LogPollingProvider logPollingProvider) throws ApplicationServiceException;

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
     * Retrieve an application's GUID from the given application name.
     * <p/>
     * If the "autoCreate" parameter is true and the application doesn't exist on AIP Console, it'll automatically create it
     * before returning the GUID.
     *
     * @param applicationName The name of the application to look up
     * @param autoCreate      Whether the application should be created if it couldn't be found
     * @param nodeName        The name of the node on which the application
     * @param domainName      The name of the domain to assign to the application
     * @param cssServerName   CSS database server hosting the application
     * @param verbose         whether the log is displa
     * @return An application GUID or null if non was found
     * @throws ApplicationServiceException
     */
    String getOrCreateApplicationFromName(String applicationName, boolean autoCreate, String nodeName, String domainName
            , String cssServerName, boolean verbose) throws ApplicationServiceException;

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
     * @param exclusions
     * @return
     */
    String createDeliveryConfiguration(String appGuid, String sourcePath, Exclusions exclusions, boolean rescan) throws JobServiceException, PackagePathInvalidException;

    String discoverPackagesAndCreateDeliveryConfiguration(String appGuid, String sourcePath, Exclusions exclusions, VersionStatus status, boolean rescan, Consumer<DeliveryConfigurationDto> deliveryConfigConsumer) throws JobServiceException, PackagePathInvalidException;

    String reDiscoverApplication(String appGuid, String sourcePath, String versionName, DeliveryConfigurationDto deliveryConfig,
                                 String caipVersion, String targetNode, boolean verbose, LogPollingProvider logPollingProvider) throws ApplicationServiceException;

    /**
     * Get the existing {@code }debug options} settings
     *
     * @param appGuid host application
     * @return debug options
     * @throws ApplicationServiceException
     */
    DebugOptionsDto getDebugOptions(String appGuid);

    void updateShowSqlDebugOption(String appGuid, boolean showSql);

    void updateAmtProfileDebugOption(String appGuid, boolean amtProfile);

    void resetDebugOptions(String appGuid, DebugOptionsDto debugOptionsDto);

    void setModuleOptionsGenerationType(String appGuid, ModuleGenerationType generationType);

    void updateModuleGenerationType(String applicationGuid, JobRequestBuilder builder, ModuleGenerationType generationType, boolean firstVersion);

}
