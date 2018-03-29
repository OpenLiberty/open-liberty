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

    public static SessionCacheApp appOneListener = null;

    @BeforeClass
    public static void setUp() throws Exception {
        appOneListener = new SessionCacheApp(server, false, "session.cache.web", "session.cache.web.listener1");
        server.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + UUID.randomUUID()));
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
    @Mode(FULL)
    public void testInvalidationTimeout() throws Exception {
        // Initialize a session with some data
        List<String> session = new ArrayList<>();
        String sessionID = appOneListener.sessionPut("testInvalidationTimeout-foo", "bar", session, true);
        // Wait until we see one of the session listeners sessionDestroyed() event fire indicating that the session has timed out
        assertNotNull("Expected to find message from a session listener indicating the session expired",
                      server.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000));
        // Verify that repeating the same sessionGet() as before does not locate the expired session
        appOneListener.sessionGet("testInvalidationTimeout-foo", null, session);
    }

    /**
     * Test that a session can still be used if it was valid when a servlet call began, even after timeout.
     * This mimics SessionDB behavior.
     */
    @Test
    @Mode(FULL)
    public void testServletTimeout() throws Exception {
        List<String> session = new ArrayList<>();
        appOneListener.sessionPut("testServletTimeout-foo2", "bar", session, true);
        appOneListener.invokeServlet("sessionGetTimeout&key=testServletTimeout-foo2&expectedValue=bar", session); //Should still get the value
        appOneListener.sessionGet("testServletTimeout-foo2", null, session);
    }

    /**
     * Tests that a locally cached session is still usable to the end of a servlet call after being invalidated,
     * and is no longer valid in a following servlet call.
     */
    @Test
    @Mode(FULL)
    public void testServletPutTimeout() throws Exception {
        List<String> session = new ArrayList<>();
        appOneListener.invokeServlet("sessionPutTimeout&key=testServletPutTimeout-foo2&value=bar&createSession=true", session);
        appOneListener.sessionGet("testServletPutTimeout-foo2", null, session);
    }

    @Test
    public void testRefreshInvalidation() throws Exception {
        int refreshes = TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.FULL ? 15 : 3;

        for (int attempt = 0; attempt < 5; attempt++) {
            // Initialize a session attribute
            List<String> session = new ArrayList<>();
            appOneListener.sessionPut("testRefreshInvalidation-foo", "bar", session, true);

            // Read the session attribute every 3 seconds, looping several times.  Reading the session attribute will
            // prevent the session from becoming invalid after 5 seconds because it refreshes the timer on each access.
            long start = 0;
            try {
                for (int i = 0; i < refreshes; i++) {
                    start = System.nanoTime();
                    TimeUnit.SECONDS.sleep(3);
                    appOneListener.sessionGet("testRefreshInvalidation-foo", "bar", session);
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
