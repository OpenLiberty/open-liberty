package com.ibm.ws.jdbc.fat.derby;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Each Derby configuration variant is tested in isolation, because the notice
 * is only issued once per JVM and detection failure of one variant would
 * otherwise be masked by another.
 */
@RunWith(FATRunner.class)
public class DerbyNoticeTest {

    public static final String SERVER_NAME = "com.ibm.ws.jdbc.fat.derby.notice";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @Before
    public void setUp() throws Exception {
        server.saveServerConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (server.isStarted())
                server.stopServer("DSRA8071W");
        } finally {
            server.restoreServerConfiguration();
        }
    }

    /**
     * Starts the server in beta or GA mode with only the specified data sources configured.
     */
    private void startWithDataSources(boolean beta, String... dataSourceIds) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        List<String> keep = Arrays.asList(dataSourceIds);
        for (String id : Arrays.asList("derbyEmbedded1", "derbyEmbedded2", "derbyClient", "derbyGeneric"))
            if (!keep.contains(id))
                config.getDataSources().removeById(id);
        server.updateServerConfiguration(config);
        server.setJvmOptions(Collections.singletonList("-Dcom.ibm.ws.beta.edition=" + beta));
        server.startServer();
    }

    private void assertNotices(int derbyWarnings) throws Exception {
        assertEquals("Unexpected number of DSRA8071W messages", derbyWarnings, server.findStringsInLogs("DSRA8071W").size());
        assertEquals("Expected DSRA8072I notice to not appear in logs", 0, server.findStringsInLogs("DSRA8072I").size());
    }

    /**
     * properties.derby.embedded, with multiple data sources: warning is logged once with no application deployed.
     */
    @Test
    public void testDerbyEmbeddedBetaNotice() throws Exception {
        startWithDataSources(true, "derbyEmbedded1", "derbyEmbedded2");
        assertNotices(1);
    }

    /**
     * properties.derby.client: warning is logged once with no application deployed.
     */
    @Test
    public void testDerbyClientBetaNotice() throws Exception {
        startWithDataSources(true, "derbyClient");
        assertNotices(1);
    }

    /**
     * Generic data source using a Derby driver class: warning is logged once with no application deployed.
     */
    @Test
    public void testDerbyGenericBetaNotice() throws Exception {
        startWithDataSources(true, "derbyGeneric");
        assertNotices(1);
    }

    /**
     * GA server (beta property false) with all Derby variants logs neither message.
     */
    @Test
    public void testDerbyGaNoNotice() throws Exception {
        startWithDataSources(false, "derbyEmbedded1", "derbyEmbedded2", "derbyClient", "derbyGeneric");
        assertNotices(0);
    }
}
