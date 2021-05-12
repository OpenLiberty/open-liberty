/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.cache.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
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

    public static SessionCacheApp app = null;
    public List<List<String>> cleanupSessions = new ArrayList<>();

    @BeforeClass
    public static void setUp() throws Exception {
        app = new SessionCacheApp(server, false, "session.cache.web", "session.cache.web.listener1");

//        String hazelcastConfigFile = "hazelcast-localhost-only.xml";
//
//        if (FATSuite.isMulticastDisabled()) {
//            Log.info(SessionCacheTimeoutTest.class, "setUp", "Disabling multicast in Hazelcast config.");
//            hazelcastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";
//        }

        String hazelcastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";

        server.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + UUID.randomUUID(),
                                           "-Dhazelcast.config.file=" + hazelcastConfigFile));
        server.startServer();

        // Access a session before the main test logic to ensure that delays caused by lazy initialization
        // of the JCache provider do not interfere with the test's timing.
        List<String> session = new ArrayList<>();
        app.sessionPut("setup-session", "initval", session, true);
        app.invalidateSession(session);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @After
    public void cleanupSession() throws Exception {
        for (List<String> session : cleanupSessions)
            app.invalidateSession(session);
    }

    /**
     * Test that a session is removed from memory after timeout.
     */
    @Test
    @Mode(FULL)
    public void testInvalidationTimeout() throws Exception {
        // Initialize a session with some data
        List<String> session = newSession();
        String sessionID = app.sessionPut("testInvalidationTimeout-foo", "bar", session, true);
        // Wait until we see one of the session listeners sessionDestroyed() event fire indicating that the session has timed out
        assertNotNull("Expected to find message from a session listener indicating the session expired",
                      server.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000));
        // Verify that repeating the same sessionGet() as before does not locate the expired session
        app.sessionGet("testInvalidationTimeout-foo", null, session);
    }

    /**
     * Test that a session can still be used if it was valid when a servlet call began, even after timeout.
     * This mimics SessionDB behavior.
     */
    @Test
    @Mode(FULL)
    public void testServletTimeout() throws Exception {
        List<String> session = newSession();
        app.sessionPut("testServletTimeout-foo2", "bar", session, true);
        app.invokeServlet("sessionGetTimeout&key=testServletTimeout-foo2&expectedValue=bar", session); //Should still get the value
        app.sessionGet("testServletTimeout-foo2", null, session);
    }

    /**
     * Tests that a locally cached session is still usable to the end of a servlet call after being invalidated,
     * and is no longer valid in a following servlet call.
     */
    @Test
    @Mode(FULL)
    public void testServletPutTimeout() throws Exception {
        List<String> session = newSession();
        app.invokeServlet("sessionPutTimeout&key=testServletPutTimeout-foo2&value=bar&createSession=true", session);
        app.sessionGet("testServletPutTimeout-foo2", null, session);
    }

    /**
     * Tests that after a session times out session attributes are removed from the cache.
     */
    @Test
    @Mode(FULL)
    public void testCacheInvalidationAfterTimeout() throws Exception {
        List<String> session = newSession();
        String sessionID = app.sessionPut("testCacheInvalidationAfterTimeout-foo", "bar", session, true);
        // Wait until we see one of the session listeners sessionDestroyed() event fire indicating that the session has timed out
        assertNotNull("Expected to find message from a session listener indicating the session expired",
                      server.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000));
        app.invokeServlet("cacheCheck&key=testCacheInvalidationAfterTimeout-foo", session);
    }

    /**
     * Test that the cache is invalidated after reaching invalidation timeout during a servlet call.
     */
    @Test
    @Mode(FULL)
    public void testCacheInvalidationAfterServletTimeout() throws Exception {
        List<String> session = newSession();
        app.sessionPut("testCacheInvalidationAfterServletTimeout-foo", "bar", session, true);
        app.invokeServlet("sessionGetTimeoutCacheCheck&key=testCacheInvalidationAfterServletTimeout-foo", session);
    }

    /**
     * Tests that accessing a session prevents invalidation.
     */
    @Test
    public void testRefreshInvalidation() throws Exception {
        int refreshes = TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.FULL ? 15 : 3;

        for (int attempt = 0; attempt < 5; attempt++) {
            // Initialize a session attribute
            List<String> session = newSession();
            if (session != null) {
                app.sessionPut("testRefreshInvalidation-foo", "bar", session, true);

                // Read the session attribute every 3 seconds, looping several times.  Reading the session attribute will
                // prevent the session from becoming invalid after 5 seconds because it refreshes the timer on each access.
                long start = 0, prevStart = 0;
                try {
                    for (int i = 0; i < refreshes; i++) {
                        prevStart = start;
                        start = System.nanoTime();
                        TimeUnit.SECONDS.sleep(3);
                        app.sessionGet("testRefreshInvalidation-foo", "bar", session);
                    }
                    return; // test successful
                } catch (AssertionError e) {
                    long elapsed = System.nanoTime() - start;
                    if (TimeUnit.NANOSECONDS.toMillis(elapsed) > 4500
                        || prevStart > 0 && start - prevStart > TimeUnit.SECONDS.toNanos(4)) {
                        Log.info(c, testName.getMethodName(), "Ignoring failure because too much time has elapsed (slow sytem)");
                        continue;
                    } else {
                        throw e;
                    }
                }
            }
        }
        fail("The machine was too slow to run this test after attempting it 5 times.");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testTimeoutExtension() throws Exception {
        // Initialize a new session, and increase the session timeout to 500s
        List<String> session = newSession();
        app.invokeServlet("testTimeoutExtensionA", session);
        app.sessionPut("testTimeoutExtension-foo", "bar", session, false);

        // wait for the session to become invalidated if it were to have the normal timeout
        TimeUnit.SECONDS.sleep(35);

        // Verify the session is still around and has the 500s timeout set, along with other session properties
        app.invokeServlet("testTimeoutExtensionB", session);
        app.sessionGet("testTimeoutExtension-foo", "bar", session);
    }

    /**
     * Creates a new session (client side only) that will be explicitly invalidated after the end of the test case.
     * We need to explicitly invalidate because the server is configured with allowOverflow=false
     */
    private List<String> newSession() {
        ArrayList<String> s = new ArrayList<>();
        cleanupSessions.add(s);
        return s;
    }
}
