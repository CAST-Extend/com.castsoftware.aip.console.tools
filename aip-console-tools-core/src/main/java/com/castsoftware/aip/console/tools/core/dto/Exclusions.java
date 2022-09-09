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
    private Set<ExclusionRuleDto> exclusionRules = ExclusionRuleType.getDefaultExclusionRules();

    public void setInitialExclusionRules(ExclusionRuleType[] rules) {
        exclusionRules = Arrays.stream(rules).map(ExclusionRuleDto::new).collect(Collectors.toSet());
    }

    public static Set<String> getDefaultIgnorePatterns() {
        return Arrays.stream(new String[]{"tmp/", "temp/", "*test", "tests", "target/", ".svn/", ".git/", "_Macosx/"}).collect(Collectors.toSet());
    }
}
