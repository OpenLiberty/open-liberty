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
 */
@RunWith(FATRunner.class)
public class SessionCachePrefixTest extends FATServletClient {

    @Server("sessionCachePrefixServer")
    public static LibertyServer serverCustom;

    @Server("sessionCachePrefixServer_empty")
    public static LibertyServer serverEmpty;

    @Server("sessionCachePrefixServer_special")
    public static LibertyServer serverSpecial;

    public static SessionCacheApp appCustom = null;
    public static SessionCacheApp appEmpty = null;
    public static SessionCacheApp appSpecial = null;

    @ClassRule
    public static RepeatTests repeatRule = RepeatTests.withoutModification().andWith(new CacheManagerRepeatAction());

    @BeforeClass
    public static void setUp() throws Exception {
        appCustom = new SessionCacheApp(serverCustom, true, "session.cache.web", "session.cache.web.listener1");
        appEmpty = new SessionCacheApp(serverEmpty, true, "session.cache.web", "session.cache.web.listener1");
        appSpecial = new SessionCacheApp(serverSpecial, true, "session.cache.web", "session.cache.web.listener1");

        String sessionCacheConfigFile = "httpSessionCache_1.xml";
        if (RepeatTestFilter.isRepeatActionActive(CacheManagerRepeatAction.ID)) {
            sessionCacheConfigFile = "httpSessionCache_2.xml";
        }

        String hazelcastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";
        String configLocation = new File(serverCustom.getUserDir() + "/shared/resources/hazelcast/" + hazelcastConfigFile).getAbsolutePath();
        
        List<String> jvmOptions = Arrays.asList("-Dhazelcast.group.name=" + UUID.randomUUID(),
                                                "-Dhazelcast.config=" + configLocation,
                                                "-Dsession.cache.config.file=" + sessionCacheConfigFile);
        
        serverCustom.setJvmOptions(jvmOptions);
        serverEmpty.setJvmOptions(jvmOptions);
        serverSpecial.setJvmOptions(jvmOptions);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (serverCustom != null && serverCustom.isStarted()) {
                serverCustom.stopServer("CWWKG0058E", "CWWKO0221E");
            }
        } finally {
            try {
                if (serverEmpty != null && serverEmpty.isStarted()) {
                    serverEmpty.stopServer("CWWKG0058E", "CWWKO0221E");
                }
            } finally {
                if (serverSpecial != null && serverSpecial.isStarted()) {
                    serverSpecial.stopServer("CWWKG0058E", "CWWKO0221E");
                }
            }
        }
    }

    /**
     * Test Scenario 1: Custom prefix configured
     * Verify that cache names use the custom prefix "testPrefix_" when configured.
     */
    @Test
    public void testCustomPrefix() throws Exception {
        serverCustom.startServer();
        
        try {
            List<String> session = new ArrayList<>();
            String sessionId = appCustom.sessionPut("testKey", "testValue", session, true);
            assertNotNull("Session ID should not be null", sessionId);

            // Verify session data can be retrieved
            appCustom.sessionGet("testKey", "testValue", session);

            // Check logs for custom prefix "testPrefix_"
            assertNotNull("Should find custom prefix in cache names",
                         serverCustom.waitForStringInTrace("testPrefix_com\\.ibm\\.ws\\.session\\.(attr|meta)", 30000));

            Log.info(SessionCachePrefixTest.class, "testCustomPrefix",
                    "Successfully verified custom prefix 'testPrefix_' in cache names");
        } finally {
            serverCustom.stopServer("CWWKG0058E", "CWWKO0221E");
        }
    }

    /**
     * Test Scenario 2: Empty string prefix
     * Verify that an empty string prefix behaves like the default (no prefix).
     */
    @Test
    public void testEmptyStringPrefix() throws Exception {
        serverEmpty.startServer();
        
        try {
            List<String> session = new ArrayList<>();
            String sessionId = appEmpty.sessionPut("emptyKey", "emptyValue", session, true);
            assertNotNull("Session ID should not be null", sessionId);

            // Verify session data can be retrieved
            appEmpty.sessionGet("emptyKey", "emptyValue", session);

            // Check logs for standard cache name pattern (empty prefix should behave like default)
            assertNotNull("Should find standard cache name pattern in logs",
                         serverEmpty.waitForStringInTrace("com\\.ibm\\.ws\\.session\\.(attr|meta)\\.default_host", 30000));

            Log.info(SessionCachePrefixTest.class, "testEmptyStringPrefix",
                    "Successfully verified empty string prefix behaves like default");
        } finally {
            serverEmpty.stopServer("CWWKG0058E", "CWWKO0221E");
        }
    }

    /**
     * Test Scenario 3: Special characters in prefix
     * Verify that special characters (dash, underscore, dot, colon) work in prefix.
     */
    @Test
    public void testSpecialCharactersInPrefix() throws Exception {
        serverSpecial.startServer();
        
        try {
            List<String> session = new ArrayList<>();
            String sessionId = appSpecial.sessionPut("specialKey", "specialValue", session, true);
            assertNotNull("Session ID should not be null", sessionId);

            // Verify session data can be retrieved
            appSpecial.sessionGet("specialKey", "specialValue", session);

            // Check logs for prefix with special characters: "app-v1.2_prod:"
            assertNotNull("Should find prefix with special characters in cache names",
                         serverSpecial.waitForStringInTrace("app-v1\\.2_prod:com\\.ibm\\.ws\\.session\\.(attr|meta)", 30000));

            Log.info(SessionCachePrefixTest.class, "testSpecialCharactersInPrefix",
                    "Successfully verified special characters in prefix");
        } finally {
            serverSpecial.stopServer("CWWKG0058E", "CWWKO0221E");
        }
    }
}
