/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for cacheNamePrefix configuration with Hazelcast.
 * This feature allows customers to customize session cache names when multiple
 * Liberty instances share the same Hazelcast cluster.
 *
 * Uses a single server instance with setServerConfigurationFile() to test
 * different prefix configurations sequentially.
 */
@RunWith(FATRunner.class)
public class SessionCachePrefixTest extends FATServletClient {

    @Server("sessionCachePrefixServer")
    public static LibertyServer server;

    public static SessionCacheApp app = null;

    @ClassRule
    public static RepeatTests repeatRule = RepeatTests.withoutModification().andWith(new CacheManagerRepeatAction());

    @BeforeClass
    public static void setUp() throws Exception {
        app = new SessionCacheApp(server, true, "session.cache.web", "session.cache.web.listener1");

        String hazelcastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";
        String configLocation = new File(server.getUserDir() + "/shared/resources/hazelcast/" + hazelcastConfigFile).getAbsolutePath();
        
        List<String> jvmOptions = Arrays.asList("-Dhazelcast.group.name=" + UUID.randomUUID(),
                                                "-Dhazelcast.config=" + configLocation);
        
        server.setJvmOptions(jvmOptions);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKG0058E", "CWWKO0221E");
        }
    }

    /**
     * Test Scenario 1: Custom prefix configured
     * Verify that cache names use the custom prefix "testPrefix_" when configured.
     */
    @Test
    public void testCustomPrefix() throws Exception {
        server.setServerConfigurationFile("server_customPrefix.xml");
        server.startServer();
        
        try {
            List<String> session = new ArrayList<>();
            String sessionId = app.sessionPut("testKey", "testValue", session, true);
            assertNotNull("Session ID should not be null", sessionId);

            // Verify session data can be retrieved
            app.sessionGet("testKey", "testValue", session);

            // Check logs for custom prefix "testPrefix_"
            assertNotNull("Should find custom prefix in cache names",
                         server.waitForStringInTrace("testPrefix_com\\.ibm\\.ws\\.session\\.(attr|meta)", 30000));

            Log.info(SessionCachePrefixTest.class, "testCustomPrefix",
                    "Successfully verified custom prefix 'testPrefix_' in cache names");
        } finally {
            server.stopServer("CWWKG0058E", "CWWKO0221E");
        }
    }

    /**
     * Test Scenario 2: Empty string prefix
     * Verify that an empty string prefix behaves like the default (no prefix).
     */
    @Test
    public void testEmptyStringPrefix() throws Exception {
        server.setServerConfigurationFile("server_emptyPrefix.xml");
        server.startServer();
        
        try {
            List<String> session = new ArrayList<>();
            String sessionId = app.sessionPut("emptyKey", "emptyValue", session, true);
            assertNotNull("Session ID should not be null", sessionId);

            // Verify session data can be retrieved
            app.sessionGet("emptyKey", "emptyValue", session);

            // Check logs for standard cache name pattern (empty prefix should behave like default)
            assertNotNull("Should find standard cache name pattern in logs",
                         server.waitForStringInTrace("com\\.ibm\\.ws\\.session\\.(attr|meta)\\.default_host", 30000));

            Log.info(SessionCachePrefixTest.class, "testEmptyStringPrefix",
                    "Successfully verified empty string prefix behaves like default");
        } finally {
            server.stopServer("CWWKG0058E", "CWWKO0221E");
        }
    }

    /**
     * Test Scenario 3: Special characters in prefix
     * Verify that special characters (dash, underscore, dot, colon) work in prefix.
     */
    @Test
    public void testSpecialCharactersInPrefix() throws Exception {
        server.setServerConfigurationFile("server_specialPrefix.xml");
        server.startServer();
        
        try {
            List<String> session = new ArrayList<>();
            String sessionId = app.sessionPut("specialKey", "specialValue", session, true);
            assertNotNull("Session ID should not be null", sessionId);

            // Verify session data can be retrieved
            app.sessionGet("specialKey", "specialValue", session);

            // Check logs for prefix with special characters: "app-v1.2_prod:"
            assertNotNull("Should find prefix with special characters in cache names",
                         server.waitForStringInTrace("app-v1\\.2_prod:com\\.ibm\\.ws\\.session\\.(attr|meta)", 30000));

            Log.info(SessionCachePrefixTest.class, "testSpecialCharactersInPrefix",
                    "Successfully verified special characters in prefix");
        } finally {
            server.stopServer("CWWKG0058E", "CWWKO0221E");
        }
    }
}
