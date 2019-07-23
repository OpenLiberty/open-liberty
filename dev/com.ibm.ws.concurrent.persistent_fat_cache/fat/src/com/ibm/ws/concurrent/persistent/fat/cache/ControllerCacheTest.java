/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.cache;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test case for experimenting with JCache behavior that could be useful to a
 * controller implementation for the persistent executor.
 */
@RunWith(FATRunner.class)
public class ControllerCacheTest extends FATServletClient {

    private static final String APP_NAME = "controllerCacheApp";

    @Server("serverA")
    public static LibertyServer serverA;

    @Server("serverB")
    public static LibertyServer serverB;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(serverA, APP_NAME, "test.controller.cache.web");
        ShrinkHelper.defaultApp(serverB, APP_NAME, "test.controller.cache.web");
        serverB.useSecondaryHTTPPort();

        String rand = UUID.randomUUID().toString();
        String configLocation = new File(serverA.getUserDir() + "/shared/resources/hazelcast/hazelcast-localhost-only-multicastDisabled.xml").getAbsolutePath();
        serverA.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + rand,
                                            "-Dhazelcast.config=" + configLocation));
        serverB.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + rand,
                                            "-Dhazelcast.config=" + configLocation));

        serverA.startServer(ControllerCacheTest.class.getSimpleName() + "-A.log");
        runTest(serverA, APP_NAME + "/ControllerCacheTestServlet?keyInitServerA=A", "putEntries");

        serverB.startServer(ControllerCacheTest.class.getSimpleName() + "-B.log");
        runTest(serverB, APP_NAME + "/ControllerCacheTestServlet?keyInitServerB=B", "putEntries");
    }

    @AfterClass
    public static void cleanup() throws Exception {
        try {
            if (serverB.isStarted())
                serverB.stopServer();
        } finally {
            if (serverA.isStarted())
                serverA.stopServer();
        }
    }

    /**
     * A basic that adds and removes a cache entry to demonstrate that the JCache provider is set up correctly and working.
     */
    @Test
    public void testBasicAddAndRemove() throws Exception {
        runTest(serverA, APP_NAME + "/ControllerCacheTestServlet", testName);
    }

    /**
     * In order for the more meaningful tests to work, the two servers need to be accessing the same cache. Prove this is happening.
     */
    @Test
    public void testBasicUpdatesVisibleAcrossServers() throws Exception {
        runTest(serverA, APP_NAME + "/ControllerCacheTestServlet?test=testBasicUpdatesVisibleAcrossServers" +
                         "&keyBasicUpdateVisible=ServerAValue",
                "putEntries");

        runTest(serverB, APP_NAME + "/ControllerCacheTestServlet?test=testBasicUpdatesVisibleAcrossServers" +
                         "&keyBasicUpdateVisible=ServerAValue",
                "getEntries");
    }

    /**
     * Verify that cache-entry-created events are sent to both servers.
     */
    @Test
    public void testCacheEntryCreatedListener() throws Exception {
        runTest(serverA, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryCreatedListener&keyCECL1=A1&keyCECL2=A2", "putEntries");
        runTest(serverB, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryCreatedListener&keyCECL3=B1", "putEntries");

        runTest(serverA, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryCreatedListener&keyCECL1=A1&keyCECL2=A2&keyCECL3=B1", "waitForCreatedNotifications");
        runTest(serverB, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryCreatedListener&keyCECL1=A1&keyCECL2=A2&keyCECL3=B1", "waitForCreatedNotifications");
    }

    /**
     * Verify that cache-entry-expired events are sent to both servers, and that if a cache entry is
     * re-created after expiry, then it can expire again, with notifications again sent to both servers.
     */
    @Test
    public void testCacheEntryExpiredListener() throws Exception {
        runTest(serverA, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryExpiredListener&keyCEEL1=Aa", "putEntries");
        runTest(serverB, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryExpiredListener&keyCEEL2=Bb", "putEntries");

        runTest(serverA, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryExpiredListener&keyCEEL1=Aa&keyCEEL2=Bb", "waitForExpiredNotifications");
        runTest(serverB, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryExpiredListener&keyCEEL1=Aa&keyCEEL2=Bb", "waitForExpiredNotifications");

        runTest(serverA, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryExpiredListener&keyCEEL1=Cc", "putEntries");
        runTest(serverA, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryExpiredListener&keyCEEL1=Cc", "waitForExpiredNotifications");
        runTest(serverB, APP_NAME + "/ControllerCacheTestServlet?test=testCacheEntryExpiredListener&keyCEEL1=Cc", "waitForExpiredNotifications");
    }
}
