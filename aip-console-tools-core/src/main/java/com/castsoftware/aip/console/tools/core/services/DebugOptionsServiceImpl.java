package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.dto.DebugOptionsDto;
import com.castsoftware.aip.console.tools.core.dto.JsonDto;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import com.castsoftware.aip.console.tools.core.utils.CompatibityFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.java.Log;

@Log
public class DebugOptionsServiceImpl implements DebugOptionsService {
    public final boolean isCompatible;
    private RestApiService restApiService;

    public DebugOptionsServiceImpl(RestApiService restApiService, ApiInfoDto apiInfoDto) {
        isCompatible = CompatibityFeature.toVersion(apiInfoDto.getApiVersion())
                .isHigherThanOrEqual(CompatibityFeature.DEBUG_OPTIONS.getVersion());
        this.restApiService = restApiService;
        log.info("The target AIP Console version is " + apiInfoDto.getApiVersion() +
                " and " + CompatibityFeature.DEBUG_OPTIONS.name() + " is " +
                ((isCompatible) ? "available" : "not available"));
    }

    @Override
    public DebugOptionsDto getDebugOptions(String appGuid) throws ApplicationServiceException {
        if (!isCompatible) {
            return DebugOptionsDto.builder().build();
        }
        try {
            return restApiService.getForEntity(ApiEndpointHelper.getDebugOptionsPath(appGuid), new TypeReference<DebugOptionsDto>() {
            });
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to retrieve the applications' debug options settings", e);
        }
    }

    @Override
    public DebugOptionsDto updateDebugOptions(String appGuid, DebugOptionsDto newDebugOptions) throws ApplicationServiceException {
        //==============
        // Ony set debug option when ON. Run Analysis always consider these options as OFF(disabled)
        //==============
        DebugOptionsDto oldDebugOptions = getDebugOptions(appGuid);
        if (isCompatible) {
            if (newDebugOptions.isShowSql()) {
                updateShowSqlDebugOption(appGuid, newDebugOptions.isShowSql());
            }
            if (newDebugOptions.isActivateAmtMemoryProfile()) {
                updateAmtProfileDebugOption(appGuid, newDebugOptions.isActivateAmtMemoryProfile());
            }
        }
        return oldDebugOptions;
    }

    @Override
    public void resetDebugOptions(String appGuid, DebugOptionsDto debugOptionsDto) throws ApplicationServiceException {
        if (isCompatible) {
            updateShowSqlDebugOption(appGuid, debugOptionsDto.isShowSql());
            updateAmtProfileDebugOption(appGuid, debugOptionsDto.isActivateAmtMemoryProfile());
        }
    }

    private void updateShowSqlDebugOption(String appGuid, boolean showSql) throws ApplicationServiceException {
        try {
            restApiService.putForEntity(ApiEndpointHelper.getDebugOptionShowSqlPath(appGuid), JsonDto.of(showSql), String.class);
        } catch (ApiCallException e) {
            throw new ApplicationServiceException("Unable to update the application' Show Sql debug option", e);
        }
    }

    private void updateAmtProfileDebugOption(String appGuid, boolean amtProfile) throws ApplicationServiceException {
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
}
