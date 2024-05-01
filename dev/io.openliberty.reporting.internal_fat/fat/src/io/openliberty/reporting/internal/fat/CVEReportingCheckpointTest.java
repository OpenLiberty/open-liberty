package io.openliberty.reporting.internal.fat;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Cve Reporting checkpoint test class. We only want to test the config for this
 * feature as everything else will be the same as expected for any other server
 * with or without InstantOn enabled.
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class CVEReportingCheckpointTest extends FATServletClient {

    public static final String SERVER_NAME = "io.openliberty.reporting.checkpoint.server";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        server.saveServerConfiguration();
    }

    @After
    public void tearDown() throws Exception {

        if (server.isStarted()) {
            server.stopServer();
        }

        server.restoreServerConfiguration();
    }

    @Test
    public void testIsEnabledByDefault() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();
        server.checkpointRestore();
        server.addIgnoredErrors(Collections.singletonList("CWWKF1704W"));

        assertNotNull("The feature is disabled", server.waitForStringInLog("CWWKF1700I:.*"));
    }

    @Test
    public void testIsDisabled() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        ServerConfiguration config = server.getServerConfiguration();
        config.getCVEReporting().setEnabled(false);
        server.updateServerConfiguration(config);
        server.startServer();
        server.checkpointRestore();

        assertNotNull("The feature is enabled", server.waitForStringInLog("CWWKF1701I:.*"));
    }

    @Test
    public void testIsEnabled() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        ServerConfiguration config = server.getServerConfiguration();
        config.getCVEReporting().setEnabled(true);
        server.updateServerConfiguration(config);
        server.startServer();
        server.checkpointRestore();
        server.addIgnoredErrors(Collections.singletonList("CWWKF1704W"));

        assertNotNull("The feature is disabled", server.waitForStringInLog("CWWKF1700I:.*"));
    }

    @Test
    public void testConfigChangeAfterRestore() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();

        configureEnvVariable(server, singletonMap("ENABLED_A", "false"));

        server.checkpointRestore();
        server.addIgnoredErrors(Collections.singletonList("CWWKF1704W"));

        assertNotNull("The feature is enabled", server.waitForStringInLog("CWWKF1701I:.*"));
    }

    static void configureEnvVariable(LibertyServer server, Map<String, String> newEnv) throws Exception {
        Properties serverEnvProperties = new Properties();
        serverEnvProperties.putAll(newEnv);
        File serverEnvFile = new File(server.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
        try (OutputStream out = new FileOutputStream(serverEnvFile)) {
            serverEnvProperties.store(out, "");
        }
    }

}
