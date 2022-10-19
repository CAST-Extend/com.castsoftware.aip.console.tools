package com.castsoftware.aip.console.tools.core.dto;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum ExclusionRuleType {
    EXCLUDE_EMPTY_PROJECTS,
    PREFER_FULL_DOT_NET_TO_BASIC_DOT_NET_WEB,
    PREFER_DOT_NET_WEB_TO_ASP,
    PREFER_FULL_JAVA_PROJECTS_TO_BASIC_JSP,
    PREFER_MAVEN_TO_ECLIPSE,
    PREFER_ECLIPSE_TO_MAVEN,
    EXCLUDE_EMBEDDED_ECLIPSE_PROJECTS,
    EXCLUDE_ECLIPSE_PROJECT_WITH_DUPLICATED_NAME,
    EXCLUDE_DUPLICATE_DOT_NET_PROJECT_IN_SAME_FOLDER,
    EXCLUDE_TEST_CODE,
    EXCLUDE_JAVA_FILES_WHEN_A_FULL_JEE_PROJECT_EXISTS,
    EXCLUDE_JAVA_FILES_WITH_AN_INCOMPLETE_PACKAGE,
    EXCLUDE_JAVA_FILES_WITH_AN_UNNAMED_PACKAGE,
    EXCLUDE_WEB_JSP_PROJECT_WHEN_JAVA_FILES_EXISTS_FOR_THE_SAME_WEB_XML_FILE,
    EXCLUDE_JAVA_FILES_PROJECT_LOCATED_INSIDE_OTHER_JAVA_FILES_PROJECT;

    public static Set<ExclusionRuleDto> toExclusionRuleDtos(ExclusionRuleType[] rules) {
        if (rules != null && rules.length > 0) {
            return Arrays.stream(rules).map(ExclusionRuleType::toExclusionRuleDto).collect(Collectors.toSet());
        }
        return getDefaultExclusionRules();
    }

    public static ExclusionRuleDto toExclusionRuleDto(ExclusionRuleType rule) {
        return new ExclusionRuleDto(rule);
    }

    public static Set<ExclusionRuleDto> getDefaultExclusionRules() {
        Set<ExclusionRuleType> defaultRules = EnumSet.complementOf(EnumSet.of(ExclusionRuleType.PREFER_ECLIPSE_TO_MAVEN));
        return defaultRules.stream().map(ExclusionRuleType::toExclusionRuleDto).collect(Collectors.toSet());
    }

    public static void updateExclusionRules(Set<ExclusionRuleDto> exclusionRules, boolean flag, ExclusionRuleType type) {
        ExclusionRuleDto typeDto = toExclusionRuleDto(type);
        if (exclusionRules.contains(typeDto)) {
            exclusionRules.remove(typeDto);
        }
        if (flag) {
            exclusionRules.add(typeDto);
        }
    }

}

