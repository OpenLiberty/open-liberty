/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.cache.fat;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
public class HazelcastClientTest extends FATServletClient {

    @Server("sessionCacheServerA")
    public static LibertyServer serverA;

    @Server("sessionCacheServer-mphealth")
    public static LibertyServer serverB;

    @ClassRule
    public static RepeatTests repeatRule = RepeatTests.withoutModification().andWith(new CacheManagerRepeatAction());

    @BeforeClass
    public static void setUp() throws Exception {
        serverB.useSecondaryHTTPPort();

        String serverAhazelcastConfigFile = "hazelcast-localhost-only.xml";

        if (FATSuite.isMulticastDisabled()) {
            Log.info(SessionCacheTwoServerTest.class, "setUp", "Disabling multicast in Hazelcast config.");
            serverAhazelcastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";
        }

        String sessionCacheConfigFile = "httpSessionCache_1.xml";
        if (RepeatTestFilter.isRepeatActionActive(CacheManagerRepeatAction.ID)) {
            sessionCacheConfigFile = "httpSessionCache_2.xml";
        }

        String configLocation = new File(serverB.getUserDir() + "/shared/resources/hazelcast/hazelcast-client-localhost-only.xml").getAbsolutePath();
        String rand = UUID.randomUUID().toString();
        serverA.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + rand,
                                            "-Dhazelcast.config.file=" + serverAhazelcastConfigFile,
                                            "-Dsession.cache.config.file=" + sessionCacheConfigFile));
        serverB.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + rand,
                                            "-Dhazelcast.config=" + configLocation,
                                            "-Dsession.cache.config.file=" + sessionCacheConfigFile));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        // Servers should be stopped at this point, but just in case a test fails
        try {
            if (serverB.isStarted())
                serverB.stopServer();
        } finally {
            if (serverA.isStarted())
                serverA.stopServer();
        }
    }

    /**
     * When a hazelcast client restarts, it drives down a code path where Hazelcast loads JCache
     * API using the TCCL. When this code path is taken from a WAB (OSGi app), the JCache API
     * packages need to be exposed to the WABs bundle TCCL. In this test serverB enabled mpHealth-1.0
     * which uses a WAB to deploy the /health/ web endpoint.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testWABClientRestart() throws Exception {
        serverA.startServer(testName.getMethodName() + ".log");
        serverB.startServer(testName.getMethodName() + "-before.log");

        // Triggering a server restart of the hazelcast client will
        serverB.stopServer();
        serverB.startServer(testName.getMethodName() + "-after.log");

        serverB.stopServer();
        serverA.stopServer();
    }
}
