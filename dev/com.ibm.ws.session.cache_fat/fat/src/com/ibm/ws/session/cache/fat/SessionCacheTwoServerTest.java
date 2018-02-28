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
        appA = new SessionCacheApp(serverA, "session.cache.web"); // no HttpSessionListeners are registered by this app
        appB = new SessionCacheApp(serverB, "session.cache.web", "session.cache.web.listener1");
        serverB.useSecondaryHTTPPort();

        serverA.startServer();
        serverB.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            serverA.stopServer();
        } finally {
            serverB.stopServer();
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
        try {
            appA.invokeServlet("testSerialization", session);
            appB.invokeServlet("testSerialization_complete", session);
        } finally {
            appA.invalidateSession(session);
            appB.invalidateSession(session);
        }
    }

    /**
     * Test that calling session.invalidate() on one server will propagate the
     * invalidation to the other server
     */
    @Test
    public void testCrossServerInvalidation() throws Exception {
        List<String> session = new ArrayList<>();
        appA.invokeServlet("testSerialization", session);
        appB.invokeServlet("testSerialization_complete", session);

        appA.invalidateSession(session);
        appB.invokeServlet("testSessionEmpty", session);
    }

    /**
     * Verify that the HttpSessionListener receives events when sessions are created and destroyed.
     */
    @Test
    public void testHttpSessionListener() throws Exception {
        List<String> session1 = new ArrayList<>();
        String sessionId1 = appB.sessionPut("testHttpSessionListener-key1a", (short) 100, session1, true);
        try {
            // Registered HttpSessionListeners listener1 must be notified of the creation of session1,
            // and at this point must contain no record of it being destroyed,

            appB.invokeServlet("testHttpSessionListener&listener=listener1" +
                               "&sessionCreated=" + sessionId1 +
                               "&sessionNotDestroyed=" + sessionId1,
                               null);

            //appB.sessionPut("testHttpSessionListener-key1b", 1000, session1, false); // TODO fails with these uncommented, need to determine why
            //appB.sessionPut("testHttpSessionListener-key1c", 10000l, session1, false);

            appB.sessionGet("testHttpSessionListener-key1a", (short) 100, session1);
            //appB.sessionGet("testHttpSessionListener-key1b", 1000, session1);
            //appB.sessionGet("testHttpSessionListener-key1c", 10000l, session1);
        } finally {
            // Invalidating the session should cause sessionDestroyed to be sent to the listeners
            appB.invalidateSession(session1);
        }

        List<String> session2 = new ArrayList<>();
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

    // @Test // TODO still in progress
    public void testMaxSessions() throws Exception {
        List<String> session1 = new ArrayList<>();
        List<String> session2 = new ArrayList<>();
        appA.sessionPut("testMaxSessions-session1", "hello", session1, true);
        appB.sessionGet("testMaxSessions-session1", "hello", session1);

        // Starting a new session should push session1 out of the cache
        appA.sessionPut("testMaxSessions-session2", "hello2", session2, true);

        // Verify that session2 is in the server and session1 is not
        appA.sessionGet("testMaxSessions-session2", "hello2", session2);
        appB.sessionGet("testMaxSessions-session2", "hello2", session2);
        appB.sessionGet("testMaxSessions-session1", null, session1);
        appA.sessionGet("testMaxSessions-session1", null, session1);
    }
}
