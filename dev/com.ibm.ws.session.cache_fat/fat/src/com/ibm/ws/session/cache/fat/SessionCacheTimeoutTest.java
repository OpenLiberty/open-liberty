/**
 *
 */
package com.ibm.ws.session.cache.fat;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests related to Session Cache Timeouts, using a server with the following session settings:
 * invalidationTimeout="1s"
 * reaperPollInterval="30" //Min allowed to not receive random poll interval between 30-60s
 */
@RunWith(FATRunner.class)
public class SessionCacheTimeoutTest extends FATServletClient {

    @Server("sessionCacheTimeoutServer")
    public static LibertyServer server;

    public static SessionCacheApp appOnelistener = null;

    static final long SLEEP_TIME = 35 * 1000; //35 seconds

    @BeforeClass
    public static void setUp() throws Exception {
        appOnelistener = new SessionCacheApp(server, "session.cache.web", "session.cache.web.listener1");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Test that a session is removed from memory after timeout.
     */
    @Test
    public void testInvalidationTimeout() throws Exception {
        // Initialize a session with some data
        List<String> session = new ArrayList<>();
        String sessionID = appOnelistener.sessionPut("foo", "bar", session, true);
        // Wait until we see one of the session listeners sessionDestroyed() event fire indicating that the session has timed out
        assertNotNull("Expected to find message from a session listener indicating the session expired",
                      server.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000));
        // Verify that repeating the same sessionGet() as before does not locate the expired session
        appOnelistener.sessionGet("foo", null, session);
    }
}
