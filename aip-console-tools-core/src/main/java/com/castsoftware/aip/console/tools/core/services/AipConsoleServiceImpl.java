package com.castsoftware.aip.console.tools.core.services;

import com.castsoftware.aip.console.tools.core.dto.ApiInfoDto;
import com.castsoftware.aip.console.tools.core.exceptions.AipConsoleException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.utils.ApiEndpointHelper;
import lombok.extern.java.Log;

@Log
public class AipConsoleServiceImpl implements AipConsoleService {

    RestApiService apiService;

    public AipConsoleServiceImpl(RestApiService restApiService) {
        this.apiService = restApiService;
    }

    @Override
    public ApiInfoDto getAipConsoleApiInfo() throws AipConsoleException {
        try {
            return apiService.getForEntity(ApiEndpointHelper.getRootPath(), ApiInfoDto.class);
        } catch (ApiCallException e) {
            throw new AipConsoleException("Error while retrieving AIP Console capabilities", e);
        }
    }
}
