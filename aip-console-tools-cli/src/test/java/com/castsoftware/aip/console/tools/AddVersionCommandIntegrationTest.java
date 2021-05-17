package com.castsoftware.aip.console.tools;

import org.assertj.core.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("INTEGRATION_PROFILE_TEST")
public class AddVersionCommandIntegrationTest {

    @Autowired
    private AipConsoleToolsCommandTest aipConsoleToolsCommandTest;

    @Test
    public void testAddVersionCommand() {
        String[] args = Arrays.array(
                "deliver", "--apikey", "mU8cxSEw.9rnybaSe0P9eawM10LmIPIFFGMEEUdfc",
                "-n", "Normal-mode-App", "-f", "Tests-1.22.1/Demo-12-03-21/", "-v", "V2", "--clone", "false");

        aipConsoleToolsCommandTest.main(args);
    }
}
