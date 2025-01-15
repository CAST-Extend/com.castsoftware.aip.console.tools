package com.castsoftware.aip.console.tools;

import com.castsoftware.aip.console.tools.commands.SnapshotCommand;
import com.castsoftware.aip.console.tools.core.dto.jobs.CreateJobsRequest;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobRequestBuilder;
import com.castsoftware.aip.console.tools.core.dto.jobs.JobType;
import com.castsoftware.aip.console.tools.core.exceptions.ApplicationServiceException;
import com.castsoftware.aip.console.tools.core.utils.Constants;
import  com.castsoftware.aip.console.tools.core.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import picocli.CommandLine;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class SnapshotCommandTest extends AipCommandTest<SnapshotCommand> {
    @Test
    public void testSnapshotCommand_WithDefaultParams() {
        String[] sb = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "-S", TestConstants.TEST_SNAPSHOT_NAME,
                "--process-imaging",
        };

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(TestConstants.TEST_CREATRE_APP, aipCommand.getApplicationName());
        assertEquals(TestConstants.TEST_VERSION_NAME, aipCommand.getVersionName());
        assertEquals(TestConstants.TEST_SNAPSHOT_NAME, aipCommand.getSnapshotName());
        assertEquals(true, aipCommand.getSharedOptions().isVerbose());
        assertEquals(true, aipCommand.isProcessImaging());
    }
    @Test
    public void testSnapshotCommand_WithSomeParameters() {
        String[] sb = new String[]{"--apikey",
                TestConstants.TEST_API_KEY, "--app-name=" + TestConstants.TEST_CREATRE_APP,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "--process-imaging","--snapshot-date","2025-01-15T15:14:00"
        };

        aipCommandLine.parseArgs(sb);
        assertEquals(TestConstants.TEST_API_KEY, aipCommand.getSharedOptions().getApiKey());
        assertEquals(TestConstants.TEST_CREATRE_APP, aipCommand.getApplicationName());
        assertEquals(TestConstants.TEST_VERSION_NAME, aipCommand.getVersionName());
        assertEquals(true, aipCommand.getSharedOptions().isVerbose());
        assertEquals(true, aipCommand.isProcessImaging());

         assertEquals("2025-01-15T15:14:00", aipCommand.getSnapshotDateString());

    }

    @Test(expected = CommandLine.MissingParameterException.class)
    public void testSnapshotCommand_WithMissingRequiredParams() {
        //Missing the application name
        String[] sb = new String[]{"--apikey", TestConstants.TEST_API_KEY,
                "--version-name", TestConstants.TEST_VERSION_NAME,
                "-S", TestConstants.TEST_SNAPSHOT_NAME,
                "--process-imaging",
        };

        aipCommandLine.parseArgs(sb);
    }
}
