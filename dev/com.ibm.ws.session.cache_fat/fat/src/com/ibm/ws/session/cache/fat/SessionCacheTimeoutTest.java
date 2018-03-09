/**
 *
 */
package com.ibm.ws.session.cache.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests related to Session Cache Timeouts, using a server with the following session settings:
 * invalidationTimeout="5s"
 * reaperPollInterval="30" //Min allowed to not receive random poll interval between 30-60s
 */
@RunWith(FATRunner.class)
public class SessionCacheTimeoutTest extends FATServletClient {
    public static final Class<?> c = SessionCacheTimeoutTest.class;

    @Server("sessionCacheTimeoutServer")
    public static LibertyServer server;

    public static SessionCacheApp appOnelistener = null;

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
        String sessionID = appOnelistener.sessionPut("testInvalidationTimeout-foo", "bar", session, true);
        // Wait until we see one of the session listeners sessionDestroyed() event fire indicating that the session has timed out
        assertNotNull("Expected to find message from a session listener indicating the session expired",
                      server.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000));
        // Verify that repeating the same sessionGet() as before does not locate the expired session
        appOnelistener.sessionGet("testInvalidationTimeout-foo", null, session);
    }

    @Test
    public void testRefreshInvalidation() throws Exception {
        int refreshes = TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.FULL ? 15 : 3;

        for (int attempt = 0; attempt < 5; attempt++) {
            // Initialize a session attribute
            List<String> session = new ArrayList<>();
            appOnelistener.sessionPut("testRefreshInvalidation-foo", "bar", session, true);

            // Read the session attribute every 3 seconds, looping several times.  Reading the session attribute will
            // prevent the session from becoming invalid after 5 seconds because it refreshes the timer on each access.
            long start = 0;
            try {
                for (int i = 0; i < refreshes; i++) {
                    start = System.nanoTime();
                    TimeUnit.SECONDS.sleep(3);
                    appOnelistener.sessionGet("testRefreshInvalidation-foo", "bar", session);
                }
                return; // test successful
            } catch (AssertionError e) {
                long elapsed = System.nanoTime() - start;
                if (TimeUnit.NANOSECONDS.toMillis(elapsed) > 4500) {
                    Log.info(c, testName.getMethodName(), "Ignoring failure because too much time has elapsed (slow sytem)");
                    continue;
                } else {
                    throw e;
                }
            }
        }
        fail("The machine was too slow to run this test after attempting it 5 times.");
    }
}
