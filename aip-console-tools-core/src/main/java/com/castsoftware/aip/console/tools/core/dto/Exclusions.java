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
        return Arrays.stream(new String[]{"tmp/", "temp/", "*test", "tests", "target/", ".svn/", ".git/", "_Macosx/"}).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        String CRLF = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();
        sb.append("Exclusion patterns: ");
        sb.append(excludePatterns);
        sb.append(CRLF);
        sb.append("Project exclusion rules: ");
        sb.append(getExclusionRules().stream().map(ExclusionRuleType::name).collect(Collectors.joining(", ")));
        return sb.toString();
    }
}

