package com.castsoftware.aip.console.tools.factories;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

/**
 * Custom command factory.
 * Prevents Picocli from creating command instances without the autowiring of some services
 */
@Component
@Slf4j
public class SpringAwareCommandFactory implements CommandLine.IFactory, ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        return applicationContext.getBean(cls);
    }

}
