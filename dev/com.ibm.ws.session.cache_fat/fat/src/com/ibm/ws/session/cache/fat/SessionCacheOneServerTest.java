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

@RunWith(FATRunner.class)
public class SessionCacheOneServerTest extends FATServletClient {

    @Server("sessionCacheServer")
    public static LibertyServer server;

    public static SessionCacheApp app = null;

    @BeforeClass
    public static void setUp() throws Exception {
        app = new SessionCacheApp(server, "session.cache.web", "session.cache.web.listener1", "session.cache.web.listener2");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Verify that two HttpSessionListeners both receive events when sessions are created and destroyed.
     */
    @Test
    public void testHttpSessionListeners() throws Exception {
        List<String> session1 = new ArrayList<>();
        String sessionId1 = app.sessionPut("testHttpSessionListeners-key1", (byte) 10, session1, true);
        try {
            // Registered HttpSessionListeners listener1 and listener2 must be notified of
            // the creation of session1, and at this point must contain no record of it being destroyed,

            app.invokeServlet("testHttpSessionListener&listener=listener1" +
                              "&sessionCreated=" + sessionId1 +
                              "&sessionNotDestroyed=" + sessionId1,
                              null);

            app.sessionGet("testHttpSessionListeners-key1", (byte) 10, session1);

            app.invokeServlet("testHttpSessionListener&listener=listener2" +
                              "&sessionCreated=" + sessionId1 +
                              "&sessionNotDestroyed=" + sessionId1,
                              null);
        } finally {
            // Invalidating the session should cause sessionDestroyed to be sent to the listeners
            app.invalidateSession(session1);
        }

        List<String> session2 = new ArrayList<>();
        String sessionId2 = app.sessionPut("testHttpSessionListeners-key2", true, session2, true);
        try {
            // Registered HttpSessionListeners listener1 and listener2 must be notified of
            // the creation of session2, and at this point must contain no record of it being destroyed.
            // They should however, indicate that session1 was destroyed,

            app.invokeServlet("testHttpSessionListener&listener=listener1" +
                              "&sessionCreated=" + sessionId2 +
                              "&sessionDestroyed=" + sessionId1 +
                              "&sessionNotDestroyed=" + sessionId2,
                              null);

            app.invokeServlet("testHttpSessionListener&listener=listener2" +
                              "&sessionCreated=" + sessionId2 +
                              "&sessionDestroyed=" + sessionId1 +
                              "&sessionNotDestroyed=" + sessionId2,
                              null);

            app.sessionGet("testHttpSessionListeners-key2", true, session2);
        } finally {
            // Invalidating the session should cause sessionDestroyed to be sent to the listeners
            app.invalidateSession(session2);
        }

        // Registered HttpSessionListeners listener1 and listener2 must be notified of
        // the destruction of session2,

        app.invokeServlet("testHttpSessionListener&listener=listener1" +
                          "&sessionDestroyed=" + sessionId2,
                          null);
        app.invokeServlet("testHttpSessionListener&listener=listener2" +
                          "&sessionDestroyed=" + sessionId2,
                          null);
    }

    /**
     * Ensure that various types of objects can be stored in a session,
     * serialized when the session is evicted from memory, and deserialized
     * when the session is accessed again.
     */
    @Test
    public void testSerialization() throws Exception {
        List<String> session = new ArrayList<>();
        app.invokeServlet("testSerialization", session);
        try {
            app.invokeServlet("evictSession", null);
            app.invokeServlet("testSerialization_complete", session);
        } finally {
            app.invalidateSession(session);
        }
    }

    /**
     * Ensure that various types of objects can be stored in a session,
     * serialized when the session is evicted from memory, and deserialized
     * when the session is accessed again.
     */
    @Test
    public void testSerializeDataSource() throws Exception {
        List<String> session = new ArrayList<>();
        app.invokeServlet("testSerializeDataSource", session);
        try {
            app.invokeServlet("evictSession", null);
            app.invokeServlet("testSerializeDataSource_complete", session);
        } finally {
            app.invalidateSession(session);
        }
    }

    /**
     * Ensure that after waiting at least pollingRate + invalidationTimeout a session expires.
     */
    @Test
    public void testInvalidationTimeout() throws Exception {
        // Initialize a session with some data
        List<String> session = new ArrayList<>();
        String sessionID = app.sessionPut("foo", "bar", session, true);
        app.sessionGet("foo", "bar", session);

        // Wait until we see one of the session listeners sessionDestroyed() event fire indicating that the session has timed out
        assertNotNull("Expected to find message from a session listener indicating the session expired",
                      server.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000));

        // Verify that repeating the same sessionGet() as before does not locate the expired session
        app.sessionGet("foo", null, session);
    }

}
