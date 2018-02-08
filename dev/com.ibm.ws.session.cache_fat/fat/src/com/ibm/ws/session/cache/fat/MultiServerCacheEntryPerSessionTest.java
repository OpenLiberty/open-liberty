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
public class MultiServerCacheEntryPerSessionTest extends FATServletClient {

    @Server("sessionCacheServerA")
    public static LibertyServer serverA;

    @Server("sessionCacheServerB")
    public static LibertyServer serverB;

    public static SessionCacheApp appA;
    public static SessionCacheApp appB;

    @BeforeClass
    public static void setUp() throws Exception {
        appA = new SessionCacheApp(serverA);
        appB = new SessionCacheApp(serverB);
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
