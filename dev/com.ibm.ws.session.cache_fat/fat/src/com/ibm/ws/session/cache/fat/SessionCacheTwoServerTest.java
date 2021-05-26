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

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

@RunWith(FATRunner.class)
public class SessionCacheTwoServerTest extends FATServletClient {

    @Server("sessionCacheServerA")
    public static LibertyServer serverA;

    @Server("sessionCacheServerB")
    public static LibertyServer serverB;

    public static SessionCacheApp appA;
    public static SessionCacheApp appB;

    @BeforeClass
    public static void setUp() throws Exception {
        appA = new SessionCacheApp(serverA, true, "session.cache.web"); // no HttpSessionListeners are registered by this app
        appB = new SessionCacheApp(serverB, true, "session.cache.web", "session.cache.web.cdi", "session.cache.web.listener1");
        serverB.useSecondaryHTTPPort();

        String hazelcastConfigFile = "hazelcast-localhost-only.xml";
        String osName = System.getProperty("os.name").toLowerCase();

        if (FATSuite.isMulticastDisabled() || osName.contains("mac os") || osName.contains("macos")) {
            Log.info(SessionCacheTwoServerTest.class, "setUp", "Disabling multicast in Hazelcast config.");
            hazelcastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";
        }

        String configLocation = new File(serverB.getUserDir() + "/shared/resources/hazelcast/" + hazelcastConfigFile).getAbsolutePath();
        String rand = UUID.randomUUID().toString();
        serverA.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + rand,
                                            "-Dhazelcast.config.file=" + hazelcastConfigFile));
        serverB.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + rand,
                                            "-Dhazelcast.config=" + configLocation));

        serverA.startServer();

        // Since we initialize the JCache provider lazily, use an HTTP session on serverA before starting serverB,
        // so that the JCache provider has fully initialized on serverA. Otherwise, serverB might start up its own
        // cluster and not join to the cluster created on serverA.
        List<String> sessionA = new ArrayList<>();
        appA.sessionPut("init-app-A", "A", sessionA, true);
        appA.invalidateSession(sessionA);

        serverB.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            testFailover();
        } finally {
            try {
                if (serverA.isStarted())
                    serverA.stopServer();
            } finally {
                if (serverB.isStarted())
                    serverB.stopServer();
            }
        }
    }

    /**
     * Test lifecycle of cache for http sessions by putting data into a server,
     * shutting down that server, and verifying the data failed over to the other server
     */
    // No @Test because this is called manually in @AfterClass
    private static void testFailover() throws Exception {
        // Put some data into Server A and shut it down
        List<String> session = new ArrayList<>();
        appA.sessionPut("testFailover-1", "foo", session, true);
        appA.sessionGet("testFailover-1", "foo", session);
        serverA.stopServer();

        // Now verify the cache failed over to Server B
        appB.sessionGet("testFailover-1", "foo", session);
        serverB.stopServer();

        if (TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.FULL) {
            // Starting server A again should result in a fresh cache that does not contain the original stuff
            serverA.startServer("testFailover.log");
            appA.sessionGet("testFailover-1", null, session);
            serverA.stopServer();
        }
    }

    /**
     * Ensure that various types of objects can be stored in a session,
     * serialized when the session is evicted from memory, and deserialized
     * when the session is accessed again.
     */
    @Test
    public void testBasicSerialization() throws Exception {
        List<String> session = new ArrayList<>();
        if (session != null) {
            try {
                appA.invokeServlet("testSerialization", session);
                appB.invokeServlet("testSerialization_complete", session);
            } finally {
                appA.invalidateSession(session);
                appB.invalidateSession(session);
            }
        }
    }

    /**
     * Test that calling session.invalidate() on one server will propagate the
     * invalidation to the other server
     */
    @Test
    public void testCrossServerInvalidation() throws Exception {
        List<String> session = new ArrayList<>();
        if (session != null) {
            appA.invokeServlet("testSerialization", session);
            appB.invokeServlet("testSerialization_complete", session);

            appA.invalidateSession(session);
            appB.invokeServlet("testSessionEmpty", session);
        }
    }

    /**
     * Verify that the HttpSessionListener receives events when sessions are created and destroyed.
     */
    @Test
    public void testHttpSessionListener() throws Exception {
        List<String> session1 = new ArrayList<>();
        if (session1 != null) {
            String sessionId1 = appB.sessionPut("testHttpSessionListener-key1a", (short) 100, session1, true);
            try {
                // Registered HttpSessionListeners listener1 must be notified of the creation of session1,
                // and at this point must contain no record of it being destroyed,

                appB.invokeServlet("testHttpSessionListener&listener=listener1" +
                                   "&sessionCreated=" + sessionId1 +
                                   "&sessionNotDestroyed=" + sessionId1,
                                   null);

                appB.sessionPut("testHttpSessionListener-key1b", 1000, session1, false);
                appB.sessionPut("testHttpSessionListener-key1c", 10000l, session1, false);

                appB.sessionGet("testHttpSessionListener-key1a", (short) 100, session1);
                appB.sessionGet("testHttpSessionListener-key1b", 1000, session1);
                appB.sessionGet("testHttpSessionListener-key1c", 10000l, session1);
            } finally {
                // Invalidating the session should cause sessionDestroyed to be sent to the listeners
                appB.invalidateSession(session1);
            }

            List<String> session2 = new ArrayList<>();
            if (session2 != null) {
                String sessionId2 = appB.sessionPut("testHttpSessionListener-key2", 'v', session2, true);
                try {
                    // Registered HttpSessionListener listener1 must be notified of the creation of session2,
                    // and at this point must contain no record of it being destroyed.
                    // It should however, indicate that session1 was destroyed,

                    appB.invokeServlet("testHttpSessionListener&listener=listener1" +
                                       "&sessionCreated=" + sessionId2 +
                                       "&sessionDestroyed=" + sessionId1 +
                                       "&sessionNotDestroyed=" + sessionId2,
                                       null);

                    appB.sessionGet("testHttpSessionListener-key2", 'v', session2);
                } finally {
                    // Invalidating the session should cause sessionDestroyed to be sent to the listeners
                    appB.invalidateSession(session2);
                }

                // Registered HttpSessionListener listener1 must be notified of the destruction of session2,

                appB.invokeServlet("testHttpSessionListener&listener=listener1" +
                                   "&sessionDestroyed=" + sessionId2,
                                   null);
            }
        }
    }

    /**
     * Test httpSessionCache's writeContents configuration.
     * App B on server B uses the default of ONLY_SET_ATTRIBUTES, which means that an update made locally to an attribute
     * without performing a putAttribute will not be written to the persistent store even though it remains in the local cache.
     * App A on server A uses GET_AND_SET_ATTRIBUTES, which means that an update made locally to an attribute after getting it
     * without performing a putAttribute will be written to the persistent store.
     */
    @Test
    public void testModifyWithoutPut() throws Exception {
        List<String> session = new ArrayList<>();
        if (session != null) {
            appA.sessionPut("testModifyWithoutPut-key", new StringBuffer("MyValue"), session, true);
            try {
                appB.invokeServlet("testStringBufferAppendWithoutSetAttribute&key=testModifyWithoutPut-key", session);
                // appA should not see the update because it does not get written to the persistent store without a putAttribute per writeContents=ONLY_SET_ATTRIBUTES
                appA.sessionGet("testModifyWithoutPut-key&compareAsString=true", new StringBuffer("MyValue"), session);

                appB.sessionPut("testModifyWithoutPut-key", new StringBuffer("MyNewValue"), session, false);
                appA.invokeServlet("testStringBufferAppendWithoutSetAttribute&key=testModifyWithoutPut-key", session);
                // appB should see the update because it is written to the persistent store despite lack of a putAttribute. This is due to writeContents=GET_AND_SET_ATTRIBUTES
                appB.sessionGet("testModifyWithoutPut-key&compareAsString=true", new StringBuffer("MyNewValueAppended"), session);
                // appA sees the update which is made locally in the in-memory cache as well as to the persistent store
                appA.sessionGet("testModifyWithoutPut-key&compareAsString=true", new StringBuffer("MyNewValueAppended"), session);
            } finally {
                appA.invalidateSession(session);
            }
        }
    }

    /**
     * Verify that CacheMXBean and CacheStatisticsMXBean are not registered.
     */
    @Test
    public void testMXBeansNotEnabled() throws Exception {
        appA.invokeServlet("testMXBeansNotEnabled", new ArrayList<>());
    }

    /**
     * Verify that SessionScoped CDI bean preserves its state across session calls.
     */
    @Test
    public void testSessionScopedBean() throws Exception {
        List<String> session = new ArrayList<>();
        if (session != null) {
            String sessionId = appB.sessionPut("testSessionScopedBean-key", 123.4f, session, true);
            try {
                String response1 = FATSuite.run(serverB, SessionCacheApp.APP_NAME + "/SessionCDITestServlet", "testUpdateSessionScopedBean&newValue=SSB1", session);
                String response2 = FATSuite.run(serverB, SessionCacheApp.APP_NAME + "/SessionCDITestServlet", "testWeldSessionAttributes&sessionId=" + sessionId, session);
                String response3 = FATSuite.run(serverB, SessionCacheApp.APP_NAME + "/SessionCDITestServlet", "testUpdateSessionScopedBean&newValue=SSB2", session);
                String response4 = FATSuite.run(serverB, SessionCacheApp.APP_NAME + "/SessionCDITestServlet", "testWeldSessionAttributes&sessionId=" + sessionId, session);
                String response5 = FATSuite.run(serverB, SessionCacheApp.APP_NAME + "/SessionCDITestServlet", "testUpdateSessionScopedBean&newValue=SSB3", session);
                String response6 = FATSuite.run(serverB, SessionCacheApp.APP_NAME + "/SessionCDITestServlet", "testWeldSessionAttributes&sessionId=" + sessionId, session);

                // Verify that the value is updated as observed by the application
                assertTrue(response1, response1.contains("previous value for SessionScopedBean: [null]"));
                assertTrue(response3, response3.contains("previous value for SessionScopedBean: [SSB1]"));
                assertTrue(response5, response5.contains("previous value for SessionScopedBean: [SSB2]"));

                // Verify that the value is updated in the cache iself
                int start;
                start = response2.indexOf("bytes for WELD_S#0: [") + 21;
                assertNotSame(response2, 20, start);
                String response2weld0 = response2.substring(start, response2.indexOf("]", start));

                start = response2.indexOf("bytes for WELD_S#1: [") + 21;
                assertNotSame(response2, 20, start);
                String response2weld1 = response2.substring(start, response2.indexOf("]", start));

                start = response4.indexOf("bytes for WELD_S#0: [") + 21;
                assertNotSame(response4, 20, start);
                String response4weld0 = response4.substring(start, response4.indexOf("]", start));

                start = response4.indexOf("bytes for WELD_S#1: [") + 21;
                assertNotSame(response4, 20, start);
                String response4weld1 = response4.substring(start, response4.indexOf("]", start));

                start = response6.indexOf("bytes for WELD_S#0: [") + 21;
                assertNotSame(response6, 20, start);
                String response6weld0 = response6.substring(start, response6.indexOf("]", start));

                start = response6.indexOf("bytes for WELD_S#1: [") + 21;
                assertNotSame(response6, 20, start);
                String response6weld1 = response6.substring(start, response6.indexOf("]", start));

                // TODO switch all of these to assertFalse once the weld bug is fixed or successfully worked around so that updates get written
                assertTrue(response2weld0.equals(response4weld0));
                assertTrue(response2weld1.equals(response4weld1));
                assertTrue(response4weld0.equals(response6weld0));
                assertTrue(response4weld1.equals(response6weld1));
            } finally {
                appB.invalidateSession(session);
            }
        }
    }

    /**
     * Ensure that if setMaxInactiveInterval(int interval) is called on a session
     * that that value overrides the invalidation time configured in server.xml
     * for the given session.
     */
    @Test
    public void testMaxInactiveInterval() throws Exception {
        List<String> session = new ArrayList<>();
        if (session != null) {
            appA.sessionPut("testMaxInactiveInterval-key", 55901, session, true);
            appB.sessionGet("testMaxInactiveInterval-key", 55901, session);
            appA.invokeServlet("setMaxInactiveInterval", session); //set max inactive interval to 1 second

            for (int attempt = 0; attempt < 5; attempt++) {
                try {
                    Thread.sleep(3000); //wait for 3 seconds
                    appB.invokeServlet("testSessionEmpty", session);
                    return; //testSessionEmpty passed so the session has been invalidated
                } catch (AssertionError e) {
                    //We are likely on a slow machine, we'll try again
                }
            }
            fail("The session was not invalidated after 5 attempts.  This is likely due to a slow machine.");
        }
    }
}
