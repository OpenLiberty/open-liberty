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

import java.util.Arrays;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

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
    //@TestServlet(servlet = ControllerCacheTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverA;

    //@Server("serverB")
    //public static LibertyServer serverB;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(serverA, APP_NAME, "test.controller.cache.web");

        String serverAhazelcastConfigFile = "hazelcast-localhost-only.xml";

        if (FATSuite.isMulticastDisabled()) {
            Log.info(ControllerCacheTest.class, "setUp", "Disabling multicast in Hazelcast config.");
            serverAhazelcastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";
        }

        String rand = UUID.randomUUID().toString();
        serverA.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + rand,
                                            "-Dhazelcast.config.file=" + serverAhazelcastConfigFile));
        serverA.startServer(ControllerCacheTest.class.getSimpleName() + "-A.log");

        //serverB.useSecondaryHTTPPort();
        //String configLocation = new File(serverB.getUserDir() + "/shared/resources/hazelcast/hazelcast-client-localhost-only.xml").getAbsolutePath();
        //serverB.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + rand,
        //                                    "-Dhazelcast.config=" + configLocation));
        //serverB.startServer(ControllerCacheTest.class.getSimpleName() + "-B.log");
    }

    @AfterClass
    public static void cleanup() throws Exception {
        try {
            //if (serverB.isStarted())
            //    serverB.stopServer();
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
}
