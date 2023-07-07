package com.castsoftware.aip.console.tools.core.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
public class Exclusions {
    @Builder.Default
    private String excludePatterns = null;
    @Builder.Default
    private Set<ExclusionRuleDto> exclusionRules = new HashSet<>(ExclusionRuleType.getDefaultExclusionRules());

    public static Set<String> getDefaultIgnorePatterns() {
        return Arrays.stream(new String[]{"tmp/", "temp/", "*test", "tests", "target/", ".svn/", ".git/", "_Macosx/"}).collect(Collectors.toSet());
    }

    public void setInitialExclusionRules(ExclusionRuleType[] rules) {
        exclusionRules = ExclusionRuleType.toExclusionRuleDtos(rules);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("Exclusion patterns: %s%n", getExcludePatterns()));
        sb.append(String.format("Project exclusion rules: %s%n", exclusionRules.stream().map(ExclusionRuleDto::getRule).collect(Collectors.joining(", "))));
        return sb.toString();
    }
}

