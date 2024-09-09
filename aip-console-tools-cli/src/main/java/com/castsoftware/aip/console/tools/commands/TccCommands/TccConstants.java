package com.castsoftware.aip.console.tools.commands.TccCommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TccConstants {
    public static final Map<String, List<String>> validSettingValues = new HashMap<String, List<String>>() {{
        put("AUTO_CONFIG_REFRESH", new ArrayList<>(Arrays.asList("true", "false")));
        put("DF_DEFAULT_TYPE", new ArrayList<>(Arrays.asList("EIF", "ILF")));
        put("DF_FILTER_LOOKUP_TABLES", new ArrayList<>(Arrays.asList("true", "false")));
        put("DF_FKPK_MERGE", new ArrayList<>(Arrays.asList("true", "false")));
        put("SAVE_EMPTY_TR_OBJECTS", new ArrayList<>(Arrays.asList("ALWAYS", "NEVER", "ONLY AEP")));
        put("TF_ESTIMATED_FP_VALUE", new ArrayList<>(Arrays.asList("0", "3", "4", "5", "6", "7", "ASSESS")));
    }};
}
