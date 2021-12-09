/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.cache.fat.infinispan.container;

import static com.ibm.ws.session.cache.fat.infinispan.container.FATSuite.infinispan;
import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests related to Session Cache Timeouts, using two servers with the following session settings:
 * invalidationTimeout="5s"
 * reaperPollInterval="30" //Min allowed to not receive random poll interval between 30-60s
 */
@RunWith(FATRunner.class)
public class SessionCacheTwoServerTimeoutTest extends FATServletClient {
    public static final Class<?> c = SessionCacheTwoServerTimeoutTest.class;

    @Server("com.ibm.ws.session.cache.fat.infinispan.container.timeoutServerA")
    public static LibertyServer serverA;

    @Server("com.ibm.ws.session.cache.fat.infinispan.container.timeoutServerB")
    public static LibertyServer serverB;

    public static SessionCacheApp appA;
    public static SessionCacheApp appB;

    @BeforeClass
    public static void setUp() throws Exception {

        if (JakartaEE9Action.isActive()) {
            RemoteFile originalResourceDir = LibertyFileManager.getLibertyFile(serverA.getMachine(), serverA.getInstallRoot() + "/usr/shared/resources/infinispan");
            RemoteFile jakartaResourceDir = LibertyFileManager.getLibertyFile(serverA.getMachine(), serverA.getInstallRoot() + "/usr/shared/resources/infinispan-jakarta");

            /* transform any test resources to jakartaee-9 equivalents */
            ResourceTransformationHelper.transformResourcestoEE9(originalResourceDir, jakartaResourceDir, serverA, serverB);
        }

        appA = new SessionCacheApp(serverA, false, "session.cache.infinispan.web", "session.cache.infinispan.web.listener1");

        // severB requires a listener as sessions created on serverA can be destroyed on serverB via a timeout. See test testCacheInvalidationTwoServer.
        appB = new SessionCacheApp(serverB, false, "session.cache.infinispan.web", "session.cache.infinispan.web.listener1");
        serverB.useSecondaryHTTPPort();

        serverA.addEnvVar("INF_SERVERLIST", infinispan.getContainerIpAddress() + ":" + infinispan.getMappedPort(11222));
        serverB.addEnvVar("INF_SERVERLIST", infinispan.getContainerIpAddress() + ":" + infinispan.getMappedPort(11222));

        // Use HTTP session on serverA before running any tests, so that the time it takes to initialize
        // the JCache provider does not interfere with timing of tests. Invoking this before starting
        // serverB, also ensures the JCache provider cluster in serverA is ready to accept a node from
        // serverB. Otherwise, serverB could start up its own separate cluster.
        serverA.startServer();
        List<String> sessionA = new ArrayList<>();
        appA.sessionPut("init-app-A", "A", sessionA, true);
        appA.invalidateSession(sessionA);

        serverB.startServer();

        // Use HTTP session on serverB before running any tests, so that the time it takes to initialize
        // the JCache provider does not interfere with timing of tests.
        List<String> sessionB = new ArrayList<>();
        appB.sessionPut("init-app-B", "B", sessionB, true);
        appB.invalidateSession(sessionB);
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
     * Test that a session created on one server is not accessible on another server after it is
     * invalidated on the first server.
     */
    @Test
    @Mode(FULL)
    public void testInvalidationTimeoutTwoServer() throws Exception {
        // Initialize a session with some data
        List<String> session = new ArrayList<>();
        String sessionID = appA.sessionPut("testInvalidationTimeoutTwoServer-foo", "bar", session, true);
        appB.sessionGet("testInvalidationTimeoutTwoServer-foo", "bar", session);

        // Create two threads to check both serverA and serverB for the "notified of sessionDestroyed for..." message.
        // Then use invokeAny to wait for one of the servers to destroy the session via timeout.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<String> serverAResult = () -> {
            return serverA.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000);
        };
        Callable<String> serverBResult = () -> {
            return serverB.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000);
        };
        // Wait until we see one of the session listeners sessionDestroyed() event fire indicating that the session has timed out
        String result = pool.invokeAny(Arrays.asList(serverAResult, serverBResult), 5 * 60 * 1000 * 2, TimeUnit.MILLISECONDS);

        assertNotNull("Expected to find message from a session listener indicating the session expired",
                      result);
        // Verify that repeating the same sessionGet() as before does not locate the expired session
        appB.sessionGet("testInvalidationTimeoutTwoServer-foo", null, session);
    }

    /**
     * Test that a session, created on one server, which invalidates during a servlet call on a second server
     * cannot access the session, which has not been cached locally.
     *
     * (This test is for ensuring Session Database parity)
     */
    @Test
    @Mode(FULL)
    public void testInvalidationServletNoLocalCacheTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        appA.sessionPut("testInvalidationServletNoLocalCacheTwoServer-foo", "bar", session, true);
        appB.invokeServlet("sessionGetTimeout&key=testInvalidationServletNoLocalCacheTwoServer-foo", session); //expecting null value
        appB.sessionGet("testInvalidationServletNoLocalCacheTwoServer-foo", null, session);
    }

    /**
     * Test that a session, created on one server, which invalidates during a servlet call on a second server
     * can access the session, which has been cached locally.
     *
     * (This test is for ensuring Session Database parity)
     */
    @Test
    @Mode(FULL)
    public void testInvalidationServletLocalCacheTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        if (session != null) {
            appA.sessionPut("testInvalidationServletLocalCacheTwoServer-foo", "bar", session, true);
            appB.sessionGet("testInvalidationServletLocalCacheTwoServer-foo", "bar", session); //Caches the session locally.
            appB.invokeServlet("sessionGetTimeout&key=testInvalidationServletLocalCacheTwoServer-foo&expectedValue=bar", session); //expecting to receive bar from local cache
            appB.sessionGet("testInvalidationServletLocalCacheTwoServer-foo", null, session);
        }
    }

    /**
     * Test that a session which is created on Server A is removed from the Session Cache once timed out on server B
     */
    @Test
    @Mode(FULL)
    public void testCacheInvalidationServletNoLocalCacheTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        String sessionID = appA.sessionPut("testCacheInvalidationServletNoLocalCacheTwoServer-foo", "bar", session, true);
        appB.invokeServlet("sessionGetTimeoutCacheCheck&key=testCacheInvalidationServletNoLocalCacheTwoServer-foo", session);
        appA.invokeServlet("cacheCheck&key=testCacheInvalidationServletNoLocalCacheTwoServer-foo&sid=" + sessionID, session);
    }

    /**
     * Test that a session which is created on Server A is removed from the Session Cache once timed out on server B,
     * even if it has been locally cached.
     */
    @Test
    @Mode(FULL)
    public void testCacheInvalidationLocalCacheTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        appA.sessionPut("testCacheInvalidationLocalCacheTwoServer-foo", "bar", session, true);
        appB.sessionGet("testCacheInvalidationLocalCacheTwoServer-foo", "bar", session);
        appB.invokeServlet("sessionGetTimeoutCacheCheck&key=testCacheInvalidationLocalCacheTwoServer-foo", session);
    }

    /**
     * Test that after a session is invalidated it is removed from both caches.
     */
    @Test
    @Mode(FULL)
    public void testCacheInvalidationTwoServer() throws Exception {
        List<String> session = new ArrayList<>();
        String sessionID = appA.sessionPut("testCacheInvalidationTwoServer-foo", "bar", session, true);

        // Create two threads to check both serverA and serverB for the "notified of sessionDestroyed for..." message.
        // Then use invokeAny to wait for one of the servers to destroy the session via timeout.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<String> serverAResult = () -> {
            return serverA.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000);
        };
        Callable<String> serverBResult = () -> {
            return serverB.waitForStringInLog("notified of sessionDestroyed for " + sessionID, 5 * 60 * 1000);
        };
        String result = pool.invokeAny(Arrays.asList(serverAResult, serverBResult), 5 * 60 * 1000 * 2, TimeUnit.MILLISECONDS);

        assertNotNull("Expected to find message from a session listener indicating the session expired",
                      result);
        appB.invokeServlet("cacheCheck&key=testCacheInvalidationTwoServer-foo&sid=" + sessionID, session);
        appA.invokeServlet("cacheCheck&key=testCacheInvalidationTwoServer-foo&sid=" + sessionID, session);
    }

    /**
     * Ensure that if Infinispan exception is ever resolved, that we are notified and can switch our tests back.
     * Error Thrown: ISPN021011: Incompatible cache value types specified, expected class java.lang.String but class java.lang.Object was specified
     */
    @Test
    public void testInfinispanClassCastException() throws Exception {
        appA.invokeServlet("testInfinispanClassCastException&shouldFail=true", null);
    }
}
