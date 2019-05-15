package io.jenkins.plugins.aipconsole;

import org.apache.commons.lang3.StringUtils;
import org.jvnet.localizer.ResourceBundleHolder;

import java.util.MissingResourceException;

public class JobStepTranslationHelper {
    private static final ResourceBundleHolder holder = ResourceBundleHolder.get(io.jenkins.plugins.aipconsole.Messages.class);
    private final static String MESSAGES_STEP_KEY_PREFIX = "JobsSteps.";

    private JobStepTranslationHelper() {

    }

    public static String getStepTranslation(String stepName) {
        try {
            return StringUtils.isBlank(stepName) ? stepName : holder.format(MESSAGES_STEP_KEY_PREFIX+stepName);
        } catch (MissingResourceException e) {
            // missing resource ? No translation is available, just return the name
            return stepName;
        }
    }
}
