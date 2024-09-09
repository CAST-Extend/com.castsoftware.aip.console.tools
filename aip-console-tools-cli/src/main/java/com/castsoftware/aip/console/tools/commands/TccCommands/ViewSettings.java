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

import java.util.List;
import java.util.Map;

@Component
@CommandLine.Command(name = "view-settings", description = "Shows the computation settings, their current values and their possible values.")
@Slf4j
@Getter
@Setter
public class ViewSettings extends BasicCallable {
    @CommandLine.ParentCommand
    private TccCommand parentCommand;

    public ViewSettings(RestApiService restApiService, JobsService jobsService, ApplicationService applicationService) {
        super(restApiService, jobsService, applicationService);
    }

//    String[][] settings = {
//        {"AUTO_CONFIG_REFRESH", "[true, false]"},
//        {"TF_ESTIMATED_FP_VALUE", "[0, 3, 4, 5, 6, 7, ASSESS]"},
//        {"DF_DEFAULT_TYPE", "[EIF, ILF]"},
//        {"DF_FILTER_LOOKUP_TABLES", "[true, false]"},
//        {"DF_FKPK_MERGE", "[true, false]"},
//        {"SAVE_EMPTY_TR_OBJECTS", "[ALWAYS, NEVER, ONLY AEP]"}
//    };


    @Override
    public Integer processCallCommand() throws Exception {
        System.out.printf("%-30s | %s%n", "Setting", "Possible Values");
        System.out.println(new String(new char[70]).replace("\0", "-"));
        Map<String, List<String>> settings = TccConstants.validSettingValues;
        for (Map.Entry<String, List<String>> setting: settings.entrySet()) {
            System.out.printf("%-30s | %s%n", setting.getKey(), StringUtils.join(setting.getValue()));
        }
        return Constants.RETURN_OK;
    }

    @Override
    public SharedOptions getSharedOptions() {
        return parentCommand.getSharedOptions();
    }

    @Override
    protected VersionInformation getMinVersion() {
        return parentCommand.getMinVersion();
    }
}
