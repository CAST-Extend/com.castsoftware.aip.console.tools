package com.castsoftware.aip.console.tools.commands.TccCommands;

import com.castsoftware.aip.console.tools.commands.BasicCallable;
import com.castsoftware.aip.console.tools.commands.SharedOptions;
import com.castsoftware.aip.console.tools.core.services.ApplicationService;
import com.castsoftware.aip.console.tools.core.services.JobsService;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import com.castsoftware.aip.console.tools.core.utils.VersionInformation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@CommandLine.Command(name = "update-settings", description = "Updates the computation settings.")
@Slf4j
@Getter
@Setter
public class UpdateSettings extends BasicCallable {
    @CommandLine.ParentCommand
    private TccCommand parentCommand;

    boolean isValidKeyValueString(String input) {
        String regex = "^([a-zA-Z_][a-zA-Z0-9_]*=[a-zA-Z0-9]+)(,[a-zA-Z_][a-zA-Z0-9_]*=[a-zA-Z0-9]+)*$";
;
        return !input.isEmpty() && input.matches(regex);
    }

    @CommandLine.Option(names = "--new-settings", required = true, description = "A list of comma(,) separated values of setting=newValue pairs that have to be updated. Eg. \"FILTER_LOOKUP_TABLES=true,DF_DEFAULT_TYPE=EIF\"")
    String settings = "";

    Map<String, List<String>> validValues = new HashMap<String, List<String>>() {{
        put("AUTO_CONFIG_REFRESH", new ArrayList<>(Arrays.asList("true", "false")));
        put("DF_DEFAULT_TYPE", new ArrayList<>(Arrays.asList("EIF", "ILF")));
        put("DF_FILTER_LOOKUP_TABLES", new ArrayList<>(Arrays.asList("true", "false")));
        put("DF_FKPK_MERGE", new ArrayList<>(Arrays.asList("true", "false")));
        put("SAVE_EMPTY_TR_OBJECTS", new ArrayList<>(Arrays.asList("ALWAYS", "NEVER", "ONLY AEP")));
        put("TF_ESTIMATED_FP_VALUE", new ArrayList<>(Arrays.asList("0", "3", "4", "5", "6", "7", "ASSESS")));
    }};

    public UpdateSettings(RestApiService restApiService, JobsService jobsService, ApplicationService applicationService) {
        super(restApiService, jobsService, applicationService);
    }

    @Override
    public Integer processCallCommand() throws Exception {
        if (!isValidKeyValueString(settings)) {
            log.error("Invalid value given for --new-settings option.");
            log.info("Use the format Eg. update-settings --new-settings \"FILTER_LOOKUP_TABLES=true,DEFAULT_DATA_FUNCTION_TYPE=EIF\"");
            return Constants.RETURN_INVALID_PARAMETERS_ERROR;
        }
        String[] keyValues = settings.split(",");
        Map<String, String> settingValueMap = new HashMap<>();

        for (String keyVal : keyValues) {
            String[] parts = keyVal.split("=", 2);
            if (parts.length != 2) {
                log.error("Invalid value given for --new-settings option.");
                log.info("Use the format Eg. update-settings --new-settings \"FILTER_LOOKUP_TABLES=true,DEFAULT_DATA_FUNCTION_TYPE=EIF\"");
                return Constants.RETURN_INVALID_PARAMETERS_ERROR;
            }
            String key = parts[0], val = parts[1];
            if (!validValues.containsKey(key)) {
                log.error("Invalid setting key provided, no setting available with name {}.", key);
                log.info("Following keys are valid:");
                log.info("[ {} ]", StringUtils.join(validValues.keySet(), ", "));
                return Constants.RETURN_INVALID_PARAMETERS_ERROR;
            }
            if (!validValues.get(key).contains(val)) {
                log.error("Invalid setting value provided for key {}.", key);
                log.error("Valid values are: [ {} ]", StringUtils.join(validValues.get(key), ", "));
                return Constants.RETURN_INVALID_PARAMETERS_ERROR;
            }
            settingValueMap.put(key, val);
        }
        return applicationService.updateFunctionPointSettings(parentCommand.getApplicationName(), settingValueMap);
    }

    public SharedOptions getSharedOptions() {
        return parentCommand.getSharedOptions();
    }

    protected VersionInformation getMinVersion() {
        return parentCommand.getMinVersion();
    }
}
