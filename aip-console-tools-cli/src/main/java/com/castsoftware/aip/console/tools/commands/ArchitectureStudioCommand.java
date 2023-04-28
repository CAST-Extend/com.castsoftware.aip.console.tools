package com.castsoftware.aip.console.tools.commands;

import com.castsoftware.aip.console.tools.core.dto.ApplicationDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelDto;
import com.castsoftware.aip.console.tools.core.dto.architecturestudio.ArchitectureModelLinkDto;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.ArchitectureStudioService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@CommandLine.Command(
        name = "ArchitectureStudio",
        mixinStandardHelpOptions = true,
        aliases = {"Arch-Studio"},
        description = "Arch studio"
)
@Slf4j
@Getter
@Setter
public class ArchitectureStudioCommand extends BasicCollable{

    @Autowired
    private ArchitectureStudioService archService;
    @CommandLine.Mixin
    private SharedOptions sharedOptions;

    @CommandLine.Option(
            names = {"-n", "--app-name"},
            paramLabel = "APPLICATION_NAME",
            description = "The name of the application",
            required = true)
    private String applicationName;

    @CommandLine.Option(
            names = "--model-name",
            description = "The name of the model",
            required = true)
    private String modelName;

    private static final VersionInformation MIN_VERSION = VersionInformation.fromVersionString("2.8.0");

    public ArchitectureStudioCommand(RestApiService restApiService, JobsService jobsService, UploadService uploadService, ApplicationService applicationService) {
        super(restApiService, jobsService, uploadService, applicationService);
    }

    @Override
    public Integer processCallCommand() throws Exception {
        if (StringUtils.isBlank(modelName)) {
            log.error("Architecture model name should not be empty.");
            return Constants.RETURN_APPLICATION_INFO_MISSING;
        }
        log.info("Getting all architecture models");
        Set<ArchitectureModelDto> modelDtoSet = archService.getArchitectureModels();
        log.info("Available Architecture Models:");
        log.info(String.valueOf(modelDtoSet));


        if(modelDtoSet.isEmpty()){
            log.info("No archutecture models available");
            log.info(String.format("%s not found in available architecture models list", modelName));
            return Constants.RETURN_ARCHITECTURE_MODEL_NOT_FOUND;
        }

        /* Search name of the model in the list of available models and get the model details. */
        ArchitectureModelDto modelInUse = modelDtoSet
                .stream()
                .peek(m -> log.info("Name property value: " + m.getName()))
                .filter(m -> m.getName().equalsIgnoreCase(modelName))
                .findFirst()
                .orElse(null);


        //Check if model list is empty
        if (modelInUse == null){
            log.error(String.format("Architecture model %s could not be found.", modelName));
            return Constants.RETURN_ARCHITECTURE_MODEL_NOT_FOUND;
        }

        String path = modelInUse.getPath();

        ApplicationDto app = applicationService.getApplicationFromName(applicationName);
        if (app == null){
            log.error(String.format("Application %s could not be found.", applicationName));
            return Constants.RETURN_APPLICATION_NOT_FOUND;
        }
        log.info(String.format("Checking architecture model with app: %s", applicationName));

        log.info("App '{}' successful", applicationName);

        Set<ArchitectureModelLinkDto> checkModel = archService.modelChecker(app.getGuid(), path, app.getCaipVersion());
        log.info("Violations for architecture model: '{}'", modelName);

        //Print the result
        log.info(String.format("| %10s | %15s | %10s | %10s | %15s | %10s |",
                "Caller Name", "Caller Type", "Callee Name",
                "Callee Type", "Link Type", "Dynamic Info"));
        log.info("+------------+-----------------+------------+-----------------+---------------+--------------------------------+");
        for (ArchitectureModelLinkDto dto : checkModel) {
            log.info(String.format("| %10s | %15s | %10s | %10s | %15s | %10s |",
                    dto.getCallerName(), dto.getCallerType(), dto.getCalleeName(),
                    dto.getCalleeType(), dto.getLinkType(), dto.getDynamicInfo()));
        }
        log.info("+------------+-----------------+------------+-----------------+---------------+--------------------------------+");

        return Constants.RETURN_OK;
    }

    @Override
    protected VersionInformation getMinVersion() {
        return MIN_VERSION;
    }

    @Override
    public SharedOptions getSharedOptions() {
        return sharedOptions;
    }
}
