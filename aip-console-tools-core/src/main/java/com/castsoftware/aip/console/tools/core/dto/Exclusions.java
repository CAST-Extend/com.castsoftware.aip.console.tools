package com.castsoftware.aip.console.tools.core.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
public class Exclusions {
    @Builder.Default
    private String excludePatterns = null;
    @Builder.Default
    private Set<ExclusionRuleType> exclusionRules = ExclusionRuleType.getDefaultExclusionRules();

    public static Set<String> getDefaultIgnorePatterns() {
        return Arrays.asList("tmp/", "temp/", "*test", "tests", "target/", ".svn/", ".git/", "_Macosx/").stream().collect(Collectors.toSet());
    }
}
