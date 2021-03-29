package com.castsoftware.aip.console.tools.core.utils;

import com.castsoftware.aip.console.tools.core.exceptions.ApiCallException;
import com.castsoftware.aip.console.tools.core.exceptions.ApiCallNoStackTraceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class LogUtilsTests {
    @Test
    public void testHideLogSensitiveInformation() {
        String line0 = "\t\tINF;Mon 09/07/2020 15:56:47.06; 9/7/2020 3:56 PM New Connection Profile Inserted: app_as_admin_mngt on CastStorageService _ HostMachine:2280 \n";
        String replaced0 = LogUtils.replaceAllSensitiveInformation(line0);
        assertThat(replaced0.contains("HostMachine:2280"), is(false));

        line0 = "\t\tINF;Mon 09/07/2020 15:56:14.42; ____Connected to HostMachine:2280 database postgres \n";
        replaced0 = LogUtils.replaceAllSensitiveInformation(line0);
        assertThat(replaced0.contains("HostMachine:2280"), is(false));

        line0 = "INF: 2020-09-07 15:57:04: Server 'HostMachine:2280 on CastStorageService' found\n";
        replaced0 = LogUtils.replaceAllSensitiveInformation(line0);
        assertThat(replaced0.contains("HostMachine:2280"), is(false));

        line0 = "INF;Mon 09/07/2020 15:56:14.08; ** HOST: MAchine_Name  ";
        replaced0 = LogUtils.replaceAllSensitiveInformation(line0);
        assertThat(replaced0.contains("MAchine_Name"), is(false));

        line0 = "\t\tINF;Mon 09/07/2020 15:56:14.10; ** PORT: 2280 \n";
        replaced0 = LogUtils.replaceAllSensitiveInformation(line0);
        assertThat(replaced0.contains("2280"), is(false));

        line0 = "\t\t\tLog file path: C:\\ProgramData\\CAST\\AipConsole-1.19-dev\\AipNode\\logs\\external_logs\\9923a9e7-d106-43fd-8335-387673043b56\\import_preferences\\import_preferences-20200907-155647.txt\n";
        replaced0 = LogUtils.replaceAllSensitiveInformation(line0);
        assertThat(replaced0.contains("C:\\ProgramData\\CAST\\AipConsole-1.19-dev\\AipNode\\logs"), is(false));

        line0 = "\t\t\t-logFilePath: C:\\ProgramData\\CAST\\AipConsole-1.19-dev\\AipNode\\logs\\external_logs\\9923a9e7-d106-43fd-8335-387673043b56\\import_preferences\\import_preferences-20200907-155647.txt\n";
        replaced0 = LogUtils.replaceAllSensitiveInformation(line0);
        assertThat(replaced0.contains("C:\\ProgramData\\CAST\\AipConsole-1.19-dev\\AipNode\\logs\\external_logs"), is(false));

        line0 = "-licenseKey: CAST_R&D:Some/valid-key";
        replaced0 = LogUtils.replaceAllSensitiveInformation(line0);
        assertThat(replaced0.contains("-licenseKey: " + LogUtils.REPLACEMENT_STR), is(true));

        line0 = "--apiKey: one.valid.key";
        replaced0 = LogUtils.replaceAllSensitiveInformation(line0);
        assertThat(replaced0.contains("--apiKey: " + LogUtils.REPLACEMENT_STR), is(true));

        line0 = "--apiKey=\"BYxRnywP.TNSS0gXt8GB2v7oVZCRHzMspITeoiT1Q\"";
        replaced0 = LogUtils.replaceAllSensitiveInformation(line0);
        assertThat(replaced0.contains("--apiKey=" + LogUtils.REPLACEMENT_STR), is(true));
    }


    private ApiCallException getThrowableApiCallException(boolean verbose, int httpStatus, String message) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<? extends Exception> callException = verbose ? ApiCallException.class :
                ApiCallNoStackTraceException.class;
        return (ApiCallException) callException.getConstructor(int.class, String.class).newInstance(httpStatus, message);
    }

    @Test
    public void fake() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ApiCallException thisException = getThrowableApiCallException(false, 201, "Unable to login to AIP Console");
        assertThat(thisException instanceof ApiCallNoStackTraceException, is(true));
    }
}
