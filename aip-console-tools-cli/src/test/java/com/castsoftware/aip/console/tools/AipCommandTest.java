package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.core.services.ApplicationServiceImpl;
import com.castsoftware.aip.console.tools.core.services.JobsServiceImpl;
import com.castsoftware.aip.console.tools.core.services.RestApiService;
import com.castsoftware.aip.console.tools.core.services.UploadServiceImpl;
import com.castsoftware.aip.console.tools.factories.SpringAwareCommandFactory;
import org.junit.After;
import org.junit.Before;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.Callable;

public abstract class AipCommandTest<T extends Callable<Integer>> {
    protected T aipCommand;
    protected AnnotationConfigApplicationContext context;
    protected CommandLine aipCommandLine;
    private Class<T> classType;

    @MockBean
    protected RestApiService restApiService;
    @MockBean
    protected JobsServiceImpl jobsService;
    @MockBean
    protected UploadServiceImpl uploadService;
    @MockBean
    protected ApplicationServiceImpl applicationService;

    @Before
    public void startup() {
        load(AipIntegrationCliMain.class);
        CommandLine.IFactory factory = this.context.getBean(SpringAwareCommandFactory.class);
        //Works well for our simple implementation
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        classType = (Class<T>) parameterizedType.getActualTypeArguments()[0];

        aipCommand = this.context.getBean(classType);
        aipCommandLine = new CommandLine(aipCommand, factory);
    }

    @After
    public void tearDown() {
        if (this.context != null) {
            this.context.close();
        }
    }

    protected void load(Class<?> config, String... environment) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        //EnvironmentTestUtils.addEnvironment(applicationContext, environment);
        applicationContext.register(config);
        applicationContext.refresh();
        this.context = applicationContext;
    }

}
