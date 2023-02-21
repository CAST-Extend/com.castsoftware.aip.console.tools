package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationOnboardingDto;
import com.castsoftware.aip.console.tools.core.dto.Applications;
import com.castsoftware.aip.console.tools.core.dto.BaseDto;
import com.castsoftware.aip.console.tools.core.dto.DebugOptionsDto;
import com.castsoftware.aip.console.tools.core.dto.DeliveryConfigurationDto;
import com.castsoftware.aip.console.tools.core.dto.DomainDto;
import com.castsoftware.aip.console.tools.core.dto.ExclusionRuleDto;
import com.castsoftware.aip.console.tools.core.dto.Exclusions;
import com.castsoftware.aip.console.tools.core.dto.ImagingSettingsDto;
import com.castsoftware.aip.console.tools.core.dto.JsonDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.PendingResultDto;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.DeliveryPackageDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.DiscoverPackageRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.dto.jobs.LogPollingProvider;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.PackagePathInvalidException;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.java.Log;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
public class ApplicationServiceImpl implements ApplicationService {

    private RestApiService restApiService;
    private JobsService jobService;

    public ApplicationServiceImpl(RestApiService restApiService, JobsService jobsService) {
        this.restApiService = restApiService;
        this.jobService = jobsService;
    }

    @Override
    public String getApplicationGuidFromName(String applicationName) throws ApplicationServiceException {
        ApplicationDto app = getApplicationFromName(applicationName);
        return app == null ? null : app.getGuid();
    }

    @Override
    public ApiInfoDto getAipConsoleApiInfo() {
        return restApiService.getAipConsoleApiInfo();
    }

    @Override
    public String getApplicationNameFromGuid(String applicationGuid) throws ApplicationServiceException {
        ApplicationDto app = getApplicationFromGuid(applicationGuid);
        return app == null ? null : app.getName();
    }

    @Override
    public ApplicationDto getApplicationFromGuid(String applicationGuid) throws ApplicationServiceException {
        return getApplications()
                .getApplications()
                .stream()
                .filter(Objects::nonNull)
                .filter(a -> StringUtils.equalsAnyIgnoreCase(applicationGuid, a.getGuid()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public ApplicationDto getApplicationDetails(String applicationGuid) throws ApplicationServiceException {
        try {
            return restApiService.getForEntity(ApiEndpointHelper.getApplicationPath(applicationGuid), ApplicationDto.class);
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to get an application with GUID: " + applicationGuid, e);
        }
    }

    @Override
    public ApplicationDto getApplicationFromName(String applicationName) throws ApplicationServiceException {
        Applications applications = getApplications();
        return getApplications()
                .getApplications()
                .stream()
                .filter(Objects::nonNull)
                .filter(a -> StringUtils.equalsAnyIgnoreCase(applicationName, a.getName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public DomainDto getDomainFromName(String domainName) throws ApplicationServiceException {
        return getDomains()
                .stream()
                .filter(Objects::nonNull)
                .filter(a -> StringUtils.equalsAnyIgnoreCase(domainName, a.getName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean applicationHasVersion(String applicationGuid) throws ApplicationServiceException {
        Set<VersionDto> appVersions = getApplicationVersion(applicationGuid);
        return appVersions != null &&
                !appVersions.isEmpty();
    }

    @Override
    public Date getVersionDate(String versionDateString) throws ApplicationServiceException {
        if (StringUtils.isEmpty(versionDateString)) {
            return new Date();
        } else {
            try {
                return JobRequestBuilder.RELEASE_DATE_FORMATTER.parse(versionDateString + ".000Z");
            } catch (ParseException e) {
                log.log(Level.SEVERE, "Version release date doesn't match the expected date format");
                throw new ApplicationServiceException("Version release date doesn't match the expected date format", e);
            }
        }
    }

    @Override
    public String getOrCreateApplicationFromName(String applicationName, boolean autoCreate) throws ApplicationServiceException {
        return getOrCreateApplicationFromName(applicationName, autoCreate, null);
    }

    @Override
    public String getOrCreateApplicationFromName(String applicationName, boolean autoCreate, String nodeName) throws ApplicationServiceException {
        return getOrCreateApplicationFromName(applicationName, autoCreate, nodeName, null, null, true);
    }

    @Override
    public String getOrCreateApplicationFromName(String applicationName, boolean autoCreate, String nodeName, String domainName, String cssServerName, boolean verbose) throws ApplicationServiceException {
        if (StringUtils.isBlank(applicationName)) {
            throw new ApplicationServiceException("No application name provided.");
        }

        Optional<ApplicationDto> appDto = getApplications()
                .getApplications()
                .stream()
                .filter(Objects::nonNull)
                .filter(a -> StringUtils.equalsAnyIgnoreCase(applicationName, a.getName()))
                .findFirst();

        if (!appDto.isPresent()) {
            if (!autoCreate) {
                return null;
            }
            try {
                String infoMessage = String.format("Application '%s' not found and 'auto create' enabled. Starting application creation", applicationName);
                if (nodeName != null) {
                    infoMessage += " on node " + nodeName;
                }
                log.info(infoMessage);

                String cssServerGuid = jobService.getCssGuid(cssServerName);
                if(cssServerGuid != null) {
                    log.log(Level.INFO,
                        "Application " + applicationName + " data repository will stored in CSS Server " + cssServerName + "(guid: " + cssServerGuid + ")");
                } else {
                    log.log(Level.INFO,
                        "Application " + applicationName + " data repository will stored on default CSS server");
                }

                String jobGuid = jobService.startCreateApplication(applicationName, nodeName, domainName, false, null, cssServerName);
                return jobService.pollAndWaitForJobFinished(jobGuid, (s) -> s.getState() == JobState.COMPLETED ? s.getJobParameters().get("appGuid") : null, verbose);
            } catch (JobServiceException e) {
                log.log(Level.SEVERE, "Could not create the application due to the following error", e);
                throw new ApplicationServiceException("Unable to create application automatically.", e);
            }
        }
        return appDto.get().getGuid();
    }

    @Override
    public String onboardApplication(String applicationName, String domainName, boolean verbose, String sourcePath) throws ApplicationServiceException {
        if (StringUtils.isBlank(applicationName)) {
            throw new ApplicationServiceException("No application name provided.");
        }
        log.log(Level.INFO, "Starting job to onboard Application: " + applicationName);
        try {
            return jobService.startOnboardApplication(applicationName, null, domainName, null);
        } catch (JobServiceException e) {
            log.log(Level.SEVERE, "Could not create the application due to the following error", e);
            throw new ApplicationServiceException("Unable to create application automatically.", e);
        }
    }

    @Override
    public String fastScan(String applicationGuid, String sourcePath, String versionName, DeliveryConfigurationDto deliveryConfig, String caipVersion,
                           String targetNode, boolean verbose, LogPollingProvider logPollingProvider) throws ApplicationServiceException {
        try {
            String discoverAction = StringUtils.isNotEmpty(applicationGuid) ? "Refresh" : "Onboard";
            log.log(Level.INFO, "Starting Fast-Scan job" + (StringUtils.isNotEmpty(applicationGuid) ? " for application GUID= " + applicationGuid : ""));
            String jobGuid = jobService.startFastScan(applicationGuid, sourcePath, versionName, deliveryConfig, caipVersion, targetNode);
            log.log(Level.INFO, discoverAction + " Fast-Scan job is ongoing: GUID= " + jobGuid);
            return logPollingProvider != null ? logPollingProvider.pollJobLog(jobGuid) : null;
        } catch (JobServiceException e) {
            log.log(Level.SEVERE, "Could not perform Fast-Scan action due to following error", e);
            throw new ApplicationServiceException("Unable to perform Fast-Scan action.", e);
        }
    }

    @Override
    public String discoverApplication(String applicationGuid, String sourcePath, String versionName, String caipVersion,
                                      String targetNode, boolean verbose, LogPollingProvider logPollingProvider) throws ApplicationServiceException {
        try {
            String discoverAction = StringUtils.isNotEmpty(applicationGuid) ? "Refresh" : "Onboard";
            log.log(Level.INFO, "Starting Discover Application job" + (StringUtils.isNotEmpty(applicationGuid) ? " for application GUID= " + applicationGuid : ""));
            String jobGuid = jobService.startDiscoverApplication(applicationGuid, sourcePath, versionName, caipVersion, targetNode);
            log.log(Level.INFO, discoverAction + " Application running job GUID= " + jobGuid);
            return logPollingProvider != null ? logPollingProvider.pollJobLog(jobGuid) : null;
        } catch (JobServiceException e) {
            log.log(Level.SEVERE, "Could not discover application contents due to following error", e);
            throw new ApplicationServiceException("Unable to discover application contents automatically.", e);
        }
    }
    
    @Override
    public String runDeepAnalysis(String applicationGuid, String targetNode, String caipVersion, String snapshotName, ModuleGenerationType moduleGenerationType, boolean verbose, LogPollingProvider logPollingProvider) throws ApplicationServiceException {
        log.log(Level.INFO, "Starting job to perform Deep Analysis action (Run Analysis) ");
        try {
            String jobGuid = jobService.startDeepAnalysis(applicationGuid, targetNode, caipVersion, snapshotName, moduleGenerationType);
            log.log(Level.INFO, "Deep Analysis running job GUID= " + jobGuid);
            return logPollingProvider != null ? logPollingProvider.pollJobLog(jobGuid) : null;
        } catch (JobServiceException e) {
            log.log(Level.SEVERE, "Could not perform the Deep Analysis due to the following error", e);
            throw new ApplicationServiceException("Unable to Run Deep Analysis automatically.", e);
        }
    }

    @Override
    public boolean isOnboardingSettingsEnabled() throws ApplicationServiceException {
        try {
            return restApiService.getForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), Boolean.class);
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to retrieve the onboarding mode settings", e);
        }
    }

    @Override
    public boolean isImagingAvailable() throws ApplicationServiceException {
        try {
            ImagingSettingsDto imagingDto = restApiService.getForEntity(ApiEndpointHelper.getImagingSettingsEndPoint(), ImagingSettingsDto.class);
            return imagingDto.isValid();
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to retrieve the onboarding mode settings", e);
        }
    }

    @Override
    public void setEnableOnboarding(boolean enabled) throws ApplicationServiceException {
        try {
            restApiService.putForEntity(ApiEndpointHelper.getEnableOnboardingSettingsEndPoint(), JsonDto.of(enabled), String.class);
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to update the 'On-boarding mode' settings", e);
        }
    }

    @Override
    public ApplicationOnboardingDto getApplicationOnboarding(String applicationGuid) throws ApplicationServiceException {
        try {
            return restApiService.getForEntity(ApiEndpointHelper.getApplicationOnboardingPath(applicationGuid), ApplicationOnboardingDto.class);
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to get onboarded application with GUID: " + applicationGuid, e);
        }
    }

    private Applications getApplications() throws ApplicationServiceException {
        try {
            Applications result = restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class);
            return result == null ? new Applications() : result;
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to get applications from AIP Console", e);
        }
    }

    private Set<DomainDto> getDomains() throws ApplicationServiceException {
        try {
            return restApiService.getForEntity(ApiEndpointHelper.getDomainsPath(), new TypeReference<Set<DomainDto>>() {
            });
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to get domains from CAST Imaging Console", e);
        }

    }

    @Override
    public Set<VersionDto> getApplicationVersion(String appGuid) throws ApplicationServiceException {
        try {
            return restApiService.getForEntity(ApiEndpointHelper.getApplicationVersionsPath(appGuid), new TypeReference<Set<VersionDto>>() {
            });
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to retrieve the applications' versions", e);
        }
    }

    @Override
    public DebugOptionsDto getDebugOptions(String appGuid) {
        try {
            return restApiService.getForEntity(ApiEndpointHelper.getDebugOptionsPath(appGuid), new TypeReference<DebugOptionsDto>() {
            });
        } catch (ApiCallException e) {
            return DebugOptionsDto.builder().build();
        }
    }

    @Override
    public void updateSecurityDataflow(String appGuid, boolean securityDataflowFlag, String technologyPath) {
        try {
            restApiService.putForEntity(ApiEndpointHelper.getApplicationSecurityDataflowPath(appGuid) + technologyPath, JsonDto.of(securityDataflowFlag), String.class);
        } catch (ApiCallException e) {
            log.log(Level.WARNING, e.getMessage());
        }
    }

    @Override
    public void updateShowSqlDebugOption(String appGuid, boolean showSql) {
        try {
            restApiService.putForEntity(ApiEndpointHelper.getDebugOptionShowSqlPath(appGuid), JsonDto.of(showSql), String.class);
        } catch (ApiCallException e) {
            log.log(Level.WARNING, e.getMessage());
        }
    }

    @Override
    public void updateAmtProfileDebugOption(String appGuid, boolean amtProfile) {
        try {
            //--------------------------------------------------------------
            //The PUT shouldn't returned anything than void.class, but doing so clashed as object mapper is trying to map
            //Some response body. The response interpreter here does behave as expected.
            //Using String.class prevents from type clash (!#?)
            //--------------------------------------------------------------
            restApiService.putForEntity(ApiEndpointHelper.getDebugOptionAmtProfilePath(appGuid), JsonDto.of(amtProfile), String.class);
        } catch (ApiCallException e) {
            //log.log(Level.SEVERE, e.getMessage());
        }
    }

    @Override
    public void resetDebugOptions(String appGuid, DebugOptionsDto debugOptionsDto) {
        updateShowSqlDebugOption(appGuid, debugOptionsDto.isShowSql());
        updateAmtProfileDebugOption(appGuid, debugOptionsDto.isActivateAmtMemoryProfile());
    }

    @Override
    public void setModuleOptionsGenerationType(String appGuid, ModuleGenerationType generationType) {
        //This endpoint operates only with either "one_per_au" or "full_content"
        if (generationType != null && generationType != ModuleGenerationType.ONE_PER_TECHNO) {
            try {
                restApiService.putForEntity(ApiEndpointHelper.getModuleOptionsGenerationTypePath(appGuid), JsonDto.of(generationType.toString()), String.class);
            } catch (ApiCallException e) {
                log.log(Level.WARNING, e.getMessage());
            }
        }
    }

    @Override
    public void updateModuleGenerationType(String applicationGuid, JobRequestBuilder builder, ModuleGenerationType moduleGenerationType, boolean firstVersion) {
        if (moduleGenerationType != null) {
            if (moduleGenerationType == ModuleGenerationType.FULL_CONTENT) {
                setModuleOptionsGenerationType(applicationGuid, moduleGenerationType);
                log.info("Module option has been set to " + moduleGenerationType);
            } else if (firstVersion) {
                //Job will handle it
                builder.moduleGenerationType(moduleGenerationType);
            } else { //clone
                if (moduleGenerationType == ModuleGenerationType.ONE_PER_AU) {
                    setModuleOptionsGenerationType(applicationGuid, moduleGenerationType);
                    log.info("Module option has been set to " + moduleGenerationType);
                } else {
                    //delegated to the job that will issue the appropriate message in case of;
                    builder.moduleGenerationType(moduleGenerationType);
                }
            }
        }
    }

    @Override
    public String discoverPackagesAndCreateDeliveryConfiguration(String appGuid, String sourcePath, Exclusions exclusions,
                                                                 VersionStatus status, boolean rescan, Consumer<DeliveryConfigurationDto> deliveryConfigConsumer) throws JobServiceException, PackagePathInvalidException {
        ApiInfoDto apiInfoDto = restApiService.getAipConsoleApiInfo();
        String flag = apiInfoDto.isEnablePackagePathCheck() ? "enabled" : "disabled";
        log.info("enable.package.path.check option is " + flag);

        try {
            Set<DeliveryPackageDto> packages = new HashSet<>();
            VersionDto previousVersion = getApplicationVersion(appGuid)
                    .stream()
                    .filter(v -> v.getStatus().ordinal() >= status.ordinal())
                    .max(Comparator.comparing(VersionDto::getVersionDate)).orElse(null);
            Set<String> ignorePatterns = StringUtils.isEmpty(exclusions.getExcludePatterns()) ?
                    Exclusions.getDefaultIgnorePatterns() : Arrays.stream(exclusions.getExcludePatterns().split(",")).collect(Collectors.toSet());
            if (apiInfoDto.isEnablePackagePathCheck() && previousVersion != null && rescan) {
                log.info("Copy configuration from the previous version: " + previousVersion.getName());
                packages = discoverPackages(appGuid, sourcePath, previousVersion.getGuid());
                if (StringUtils.isEmpty(exclusions.getExcludePatterns()) && previousVersion.getDeliveryConfiguration() != null) {
                    ignorePatterns = previousVersion.getDeliveryConfiguration().getIgnorePatterns();
                    exclusions.setExclusionRules(previousVersion.getDeliveryConfiguration().getExclusionRules());
                }
            }
            DeliveryConfigurationDto deliveryConfigurationDto = DeliveryConfigurationDto.builder()
                    .ignorePatterns(ignorePatterns)
                    .exclusionRules(exclusions.getExclusionRules())
                    .packages(packages)
                    .build();
            if (deliveryConfigConsumer != null) {
                deliveryConfigConsumer.accept(deliveryConfigurationDto);
            }
            log.info("Exclusion patterns: " + deliveryConfigurationDto.getIgnorePatterns().stream().collect(Collectors.joining(", ")));
            log.info("Project exclusion rules: " + deliveryConfigurationDto.getExclusionRules().stream().map(ExclusionRuleDto::getRule).collect(Collectors.joining(", ")));
            BaseDto response = restApiService.postForEntity("/api/applications/" + appGuid + "/delivery-configuration", deliveryConfigurationDto, BaseDto.class);
            log.fine("Delivery configuration response " + response);
            return response != null ? response.getGuid() : null;
        } catch (ApplicationServiceException | ApiCallException e) {
            log.severe("Failed to create the Delivery configuration ");
            throw new JobServiceException("Error creating delivery config", e);
        }
    }

    @Override
    public String reDiscoverApplication(String appGuid, String sourcePath, String versionName, DeliveryConfigurationDto deliveryConfig,
                                        String caipVersion, String targetNode, boolean verbose, LogPollingProvider logPollingProvider) throws ApplicationServiceException {
        try {
            log.log(Level.INFO, "Starting ReDiscover Application job for application GUID= " + appGuid);
            String jobGuid = jobService.startReDiscoverApplication(appGuid, sourcePath, versionName, deliveryConfig, caipVersion, targetNode);
            log.log(Level.INFO, "ReDiscover Application running job GUID= " + jobGuid);
            return logPollingProvider.pollJobLog(jobGuid);
        } catch (JobServiceException e) {
            log.log(Level.SEVERE, "Could not re-discover application contents due to following error", e);
            throw new ApplicationServiceException("Unable to re-discover application contents automatically.", e);
        }
    }

    @Override
    public String publishToImaging(String appGuid, LogPollingProvider logPollingProvider) throws ApplicationServiceException {
        try {
            log.log(Level.INFO, "Starting Publish to Imaging job for application GUID= " + appGuid);
            String jobGuid = jobService.startPublishToImaging(appGuid, null, null);
            log.log(Level.INFO, "Publish to Imaging running job GUID= " + jobGuid);
            return logPollingProvider != null ? logPollingProvider.pollJobLog(jobGuid) : null;
        } catch (JobServiceException e) {
            log.log(Level.SEVERE, "Application data could not be Published to Imaging due to following error", e);
            throw new ApplicationServiceException("Unable to Publish application contents to Imaging.", e);
        }
    }

    @Override
    public String createDeliveryConfiguration(String appGuid, String sourcePath, Exclusions exclusions, boolean rescan) throws JobServiceException, PackagePathInvalidException {
        return discoverPackagesAndCreateDeliveryConfiguration(appGuid, sourcePath, exclusions, VersionStatus.DELIVERED, rescan, null);
    }

    private Set<DeliveryPackageDto> discoverPackages(String appGuid, String sourcePath, String previousVersionGuid) throws PackagePathInvalidException, JobServiceException {
        try {
            Response resp = restApiService.exchangeForResponse("POST", "/api/applications/" + appGuid + "/delivery-configuration/discover-packages",
                    DiscoverPackageRequest.builder().previousVersionGuid(previousVersionGuid).sourcePath(sourcePath).build());
            int status = resp.code();
            Response packageReponse = null;
            if (status == 200) {
                packageReponse = resp;
            } else if (status == 202) {
                PendingResultDto resultDto = restApiService.mapResponse(resp, PendingResultDto.class);
                while (status != 200) {
                    log.fine("Polling server to get discovered packages...");
                    Response response = restApiService.exchangeForResponse("GET", "/api/applications/" + appGuid + "/pending-results/" + resultDto.getGuid(), null);
                    status = response.code();

                    if (status == 200) {
                        packageReponse = response;
                        break;
                    }
                    Thread.sleep(5000);
                }
            }
            if (packageReponse != null) {
                Set<DeliveryPackageDto> packages = restApiService.mapResponse(packageReponse, new TypeReference<Set<DeliveryPackageDto>>() {
                });

                ApplicationDto app = getApplicationFromGuid(appGuid);
                if (!app.isInPlaceMode() && packages.stream().anyMatch(p -> p.getPath() == null)) {
                    throw new PackagePathInvalidException(packages.stream().filter(p -> p.getPath() == null).collect(Collectors.toSet()));
                }
                return packages;
            }
            return Collections.emptySet();
        } catch (ApiCallException | InterruptedException | ApplicationServiceException e) {
            throw new JobServiceException("Error discovering packages", e);
        }
    }
}
