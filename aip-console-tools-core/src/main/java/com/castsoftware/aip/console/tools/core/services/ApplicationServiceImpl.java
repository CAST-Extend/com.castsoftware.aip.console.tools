package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.Applications;
import com.castsoftware.aip.console.tools.core.dto.BaseDto;
import com.castsoftware.aip.console.tools.core.dto.DebugOptionsDto;
import com.castsoftware.aip.console.tools.core.dto.DeliveryConfigurationDto;
import com.castsoftware.aip.console.tools.core.dto.JsonDto;
import com.castsoftware.aip.console.tools.core.dto.ModuleGenerationType;
import com.castsoftware.aip.console.tools.core.dto.NodeDto;
import com.castsoftware.aip.console.tools.core.dto.PendingResultDto;
import com.castsoftware.aip.console.tools.core.dto.VersionDto;
import com.castsoftware.aip.console.tools.core.dto.VersionStatus;
import com.castsoftware.aip.console.tools.core.dto.jobs.DeliveryPackageDto;
import com.castsoftware.aip.console.tools.core.dto.jobs.DiscoverPackageRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobState;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.JobServiceException;
import com.castsoftware.aip.console.tools.core.exceptions.PackagePathInvalidException;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.java.Log;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    public Set<ApplicationDto> findApplicationsByNames(Set<String> applicationNames) throws ApplicationServiceException {
        Set<String> appNames = applicationNames.stream().map(String::toLowerCase).collect(Collectors.toSet());
        return getApplications().getApplications().stream()
                .filter(applicationDto -> appNames.contains(applicationDto.getName().toLowerCase(Locale.ROOT))).collect(Collectors.toSet());
    }

    @Override
    public ApplicationDto getApplicationFromName(String applicationName) throws ApplicationServiceException {
        return getApplications()
                .getApplications()
                .stream()
                .filter(Objects::nonNull)
                .filter(a -> StringUtils.equalsAnyIgnoreCase(applicationName, a.getName()))
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
    public String getOrCreateApplicationFromName(String applicationName, boolean autoCreate) throws ApplicationServiceException {
        return getOrCreateApplicationFromName(applicationName, autoCreate, null);
    }

    @Override
    public String getOrCreateApplicationFromName(String applicationName, boolean autoCreate, String nodeName) throws ApplicationServiceException {
        return getOrCreateApplicationFromName(applicationName, autoCreate, nodeName, null, true);
    }

    @Override
    public String getOrCreateApplicationFromName(String applicationName, boolean autoCreate, String nodeName, String domainName, boolean logOutput) throws ApplicationServiceException {
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
                String nodeGuid = null;
                if (StringUtils.isNotBlank(nodeName)) {
                    nodeGuid = restApiService.getForEntity("/api/nodes", new TypeReference<List<NodeDto>>() {
                    }).stream()
                            .filter(n -> StringUtils.equalsIgnoreCase(nodeName, n.getName()))
                            .map(NodeDto::getGuid)
                            .findFirst()
                            .orElse(null);
                    if (nodeGuid == null) {
                        throw new ApplicationServiceException("Node with name " + nodeName + " could not be found on AIP Console to create the new application");
                    }
                }
                String infoMessage = String.format("Application '%s' not found and 'auto create' enabled. Starting application creation", applicationName);
                if (nodeGuid != null) {
                    infoMessage += " on node " + nodeName;
                }
                log.info(infoMessage);

                String jobGuid = jobService.startCreateApplication(applicationName, nodeGuid, domainName, false);
                return jobService.pollAndWaitForJobFinished(jobGuid, (s) -> s.getState() == JobState.COMPLETED ? s.getAppGuid() : null, logOutput);
            } catch (JobServiceException | ApiCallException e) {
                log.log(Level.SEVERE, "Could not create the application due to the following error", e);
                throw new ApplicationServiceException("Unable to create application automatically.", e);
            }
        }
        return appDto.get().getGuid();
    }

    private Applications getApplications() throws ApplicationServiceException {
        try {
            Applications result = restApiService.getForEntity(ApiEndpointHelper.getApplicationsPath(), Applications.class);
            return result == null ? new Applications() : result;
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to get applications from AIP Console", e);
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
    public DebugOptionsDto getDebugOptions(String appGuid) throws ApplicationServiceException {
        try {
            return restApiService.getForEntity(ApiEndpointHelper.getDebugOptionsPath(appGuid), new TypeReference<DebugOptionsDto>() {
            });
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to retrieve the applications' debug options settings", e);
        }
    }

    @Override
    public void updateShowSqlDebugOption(String appGuid, boolean showSql) throws ApplicationServiceException {
        try {
            restApiService.putForEntity(ApiEndpointHelper.getDebugOptionShowSqlPath(appGuid), JsonDto.of(showSql), String.class);
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to update the application' Show Sql debug option", e);
        }
    }

    @Override
    public void updateAmtProfileDebugOption(String appGuid, boolean amtProfile) throws ApplicationServiceException {
        try {
            //--------------------------------------------------------------
            //The PUT shouldn't returned anything than void.class, but doing so clashed as object mapper is trying to map
            //Some response body. The response interpreter here does behave as expected.
            //Using String.class prevents from type clash (!#?)
            //--------------------------------------------------------------
            restApiService.putForEntity(ApiEndpointHelper.getDebugOptionAmtProfilePath(appGuid), JsonDto.of(amtProfile), String.class);
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to update the application' AMT Profiling debug option", e);
        }
    }

    @Override
    public void resetDebugOptions(String appGuid, DebugOptionsDto debugOptionsDto) throws ApplicationServiceException {
        updateShowSqlDebugOption(appGuid, debugOptionsDto.isShowSql());
        updateAmtProfileDebugOption(appGuid, debugOptionsDto.isActivateAmtMemoryProfile());
    }

    @Override
    public String createDeliveryConfiguration(String appGuid, String sourcePath, String exclusionPatterns, boolean rescan) throws JobServiceException, PackagePathInvalidException {
        ApiInfoDto apiInfoDto = restApiService.getAipConsoleApiInfo();
        try {
            Set<DeliveryPackageDto> packages = new HashSet<>();
            VersionDto previousVersion = getApplicationVersion(appGuid)
                    .stream()
                    .filter(v -> v.getStatus().ordinal() >= VersionStatus.DELIVERED.ordinal())
                    .max(Comparator.comparing(VersionDto::getVersionDate)).orElse(null);
            Set<String> ignorePatterns = StringUtils.isEmpty(exclusionPatterns) ? Collections.emptySet() : Arrays.stream(exclusionPatterns.split(",")).collect(Collectors.toSet());
            if (apiInfoDto.isEnablePackagePathCheck() && previousVersion != null && rescan) {
                packages = discoverPackages(appGuid, sourcePath, previousVersion.getGuid());
                if (StringUtils.isEmpty(exclusionPatterns) && previousVersion.getDeliveryConfiguration() != null) {
                    ignorePatterns = previousVersion.getDeliveryConfiguration().getIgnorePatterns();
                }
            }
            DeliveryConfigurationDto deliveryConfigurationDto = DeliveryConfigurationDto.builder()
                    .ignorePatterns(ignorePatterns)
                    .packages(packages)
                    .build();

            BaseDto response = restApiService.postForEntity("/api/applications/" + appGuid + "/delivery-configuration", deliveryConfigurationDto, BaseDto.class);
            log.fine("Delivery configuration response " + response);
            return response != null ? response.getGuid() : null;
        } catch (ApplicationServiceException | ApiCallException e) {
            throw new JobServiceException("Error creating delivery config");
        }
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
}
