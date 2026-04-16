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
package com.ibm.ws.session.cache.fat.infinispan;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests for the cacheNamePrefix attribute of httpSessionCache.
 * This feature allows customers to customize session cache names when multiple
 * Liberty instances share the same Infinispan cluster.
 */
@MaximumJavaLevel(javaLevel = 22)
@RunWith(FATRunner.class)
public class SessionCachePrefixTest extends FATServletClient {

    @Server("com.ibm.ws.session.cache.fat.infinispan.prefix.default")
    public static LibertyServer serverDefault;

    @Server("com.ibm.ws.session.cache.fat.infinispan.prefix.custom")
    public static LibertyServer serverCustom;

    @Server("com.ibm.ws.session.cache.fat.infinispan.prefix.pod1")
    public static LibertyServer serverPod1;

    @Server("com.ibm.ws.session.cache.fat.infinispan.prefix.pod2")
    public static LibertyServer serverPod2;

    public static SessionCacheApp appDefault;
    public static SessionCacheApp appCustom;
    public static SessionCacheApp appPod1;
    public static SessionCacheApp appPod2;

    @ClassRule
    public static RepeatTests repeatRule = RepeatTests.withoutModification().andWith(new CacheManagerRepeatAction());

    @BeforeClass
    public static void setUp() throws Exception {
        // Initialize apps for each server
        appDefault = new SessionCacheApp(serverDefault, true, "session.cache.infinispan.web");
        appCustom = new SessionCacheApp(serverCustom, true, "session.cache.infinispan.web");
        appPod1 = new SessionCacheApp(serverPod1, true, "session.cache.infinispan.web");
        appPod2 = new SessionCacheApp(serverPod2, true, "session.cache.infinispan.web");

        // Use secondary HTTP ports for additional servers
        serverCustom.useSecondaryHTTPPort();
        serverPod1.useSecondaryHTTPPort();
        serverPod2.useSecondaryHTTPPort();

        // Set up JVM options for all servers with unique cluster name
        String rand = UUID.randomUUID().toString();
        setupServer(serverDefault, rand);
        setupServer(serverCustom, rand);
        setupServer(serverPod1, rand);
        setupServer(serverPod2, rand);
    }

    private static void setupServer(LibertyServer server, String clusterName) {
        Map<String, String> options = server.getJvmOptionsAsMap();
        options.put("-Dinfinispan.cluster.name", clusterName);
        options.put("-Djgroups.bind.address", "127.0.0.1");
        server.setJvmOptions(options);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            stopServer(serverDefault);
        } finally {
            try {
                stopServer(serverCustom);
            } finally {
                try {
                    stopServer(serverPod1);
                } finally {
                    stopServer(serverPod2);
                }
            }
        }
    }

    private static void stopServer(LibertyServer server) {
        try {
            if (server != null && server.isStarted()) {
                Log.info(SessionCachePrefixTest.class, "stopServer", "Stopping server: " + server.getServerName());
                server.stopServer();
            }
        } catch (Exception e) {
            Log.info(SessionCachePrefixTest.class, "stopServer", "Exception during server shutdown: " + e.getMessage());
        }
    }

    /**
     * Test Scenario 1: Default behavior (no cacheNamePrefix configured)
     * Verify that cache names use the standard "com.ibm.ws.session.attr." and
     * "com.ibm.ws.session.meta." prefixes when cacheNamePrefix is not specified.
     */
    @Test
    public void testDefaultCacheNames() throws Exception {
        serverDefault.startServer();

        try {
            // Create a session and put data
            List<String> session = new ArrayList<>();
            String sessionId = appDefault.sessionPut("testKey", "testValue", session, true);
            assertNotNull("Session ID should not be null", sessionId);

            // Verify session data can be retrieved
            appDefault.sessionGet("testKey", "testValue", session);

            // Check logs for standard cache name pattern
            assertNotNull("Should find standard cache name pattern in logs",
                         serverDefault.waitForStringInLog("com\\.ibm\\.ws\\.session\\.(attr|meta)\\.default_host", 30000));

            Log.info(SessionCachePrefixTest.class, "testDefaultCacheNames", 
                    "Successfully verified default cache name behavior");
        } finally {
            serverDefault.stopServer();
        }
    }

    /**
     * Test Scenario 2: Custom prefix configured
     * Verify that cache names use the custom prefix "testPrefix_" when configured.
     */
    @Test
    public void testCustomPrefix() throws Exception {
        serverCustom.startServer();

        try {
            // Create a session and put data
            List<String> session = new ArrayList<>();
            String sessionId = appCustom.sessionPut("testKey", "customValue", session, true);
            assertNotNull("Session ID should not be null", sessionId);

            // Verify session data can be retrieved
            appCustom.sessionGet("testKey", "customValue", session);

            // Check logs for custom prefix in cache names
            assertNotNull("Should find custom prefix 'testPrefix_' in cache names",
                         serverCustom.waitForStringInLog("testPrefix_com\\.ibm\\.ws\\.session\\.(attr|meta)", 30000));

            Log.info(SessionCachePrefixTest.class, "testCustomPrefix",
                    "Successfully verified custom prefix 'testPrefix_' in cache names");
        } finally {
            serverCustom.stopServer();
        }
    }

    /**
     * Test Scenario 3: Multi-pod with different prefixes
     * Verify that two Liberty instances with different prefixes can coexist
     * in the same Infinispan cluster without cache name collisions.
     */
    @Test
    public void testMultiPodDifferentPrefixes() throws Exception {
        // Start pod1 first
        serverPod1.startServer();

        // Initialize JCache provider on pod1 before starting pod2
        List<String> sessionPod1Init = new ArrayList<>();
        appPod1.sessionPut("init-pod1", "init", sessionPod1Init, true);
        appPod1.invalidateSession(sessionPod1Init);

        // Start pod2
        serverPod2.startServer();

        // Wait for 2-node cluster formation
        assertNotNull("Infinispan 2-node cluster did not form within 60 seconds",
                     serverPod1.waitForStringInLog("ISPN000094.*\\(2\\)", 60000));

        try {
            // Create sessions on both pods
            List<String> sessionPod1 = new ArrayList<>();
            List<String> sessionPod2 = new ArrayList<>();

            String sessionId1 = appPod1.sessionPut("pod1Key", "pod1Value", sessionPod1, true);
            String sessionId2 = appPod2.sessionPut("pod2Key", "pod2Value", sessionPod2, true);

            assertNotNull("Pod1 session ID should not be null", sessionId1);
            assertNotNull("Pod2 session ID should not be null", sessionId2);

            // Verify each pod can retrieve its own data
            appPod1.sessionGet("pod1Key", "pod1Value", sessionPod1);
            appPod2.sessionGet("pod2Key", "pod2Value", sessionPod2);

            // Verify distinct cache names in logs
            assertNotNull("Should find 'pod1_' prefix in pod1 cache names",
                         serverPod1.waitForStringInLog("pod1_com\\.ibm\\.ws\\.session\\.(attr|meta)", 30000));
            assertNotNull("Should find 'pod2_' prefix in pod2 cache names",
                         serverPod2.waitForStringInLog("pod2_com\\.ibm\\.ws\\.session\\.(attr|meta)", 30000));

            Log.info(SessionCachePrefixTest.class, "testMultiPodDifferentPrefixes",
                    "Successfully verified distinct cache names for pod1 and pod2");
        } finally {
            serverPod1.stopServer();
            serverPod2.stopServer();
        }
    }

    /**
     * Test Scenario 9: Failover with custom prefix
     * Verify that session failover works correctly when using a custom prefix.
     */
    @Test
    public void testFailoverWithPrefix() throws Exception {
        // Start both pods
        serverPod1.startServer();

        // Initialize JCache provider on pod1
        List<String> initSession = new ArrayList<>();
        appPod1.sessionPut("init-failover", "init", initSession, true);
        appPod1.invalidateSession(initSession);

        serverPod2.startServer();

        // Wait for cluster formation
        assertNotNull("Infinispan 2-node cluster did not form within 60 seconds",
                     serverPod1.waitForStringInLog("ISPN000094.*\\(2\\)", 60000));

        try {
            // Create session on pod1
            List<String> session = new ArrayList<>();
            String sessionId = appPod1.sessionPut("failoverKey", "failoverValue", session, true);
            assertNotNull("Session ID should not be null", sessionId);

            // Verify data on pod1
            appPod1.sessionGet("failoverKey", "failoverValue", session);

            // Wait for session replication to pod2
            Thread.sleep(2000);

            // Stop pod1
            Log.info(SessionCachePrefixTest.class, "testFailoverWithPrefix", "Stopping pod1 for failover test");
            serverPod1.stopServer();

            // Verify session data is available on pod2 (failover)
            appPod2.sessionGet("failoverKey", "failoverValue", session);

            Log.info(SessionCachePrefixTest.class, "testFailoverWithPrefix",
                    "Successfully verified session failover with custom prefix");
        } finally {
            if (serverPod1.isStarted()) {
                serverPod1.stopServer();
            }
            serverPod2.stopServer();
        }
    }
}

