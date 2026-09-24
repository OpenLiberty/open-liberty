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
package com.ibm.ws.ssl.config;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for OutboundSSLSelections to verify the fix for TS021637904.
 *
 * This test validates that when multiple <outboundConnection> entries with
 * different clientCertificate attributes are configured, each connection
 * gets the correct SSL alias format (sslConfigId:certificateAlias) rather
 * than an accumulated format (sslConfigId:cert1:cert2:cert3).
 */
public class OutboundSSLSelectionsTest {

    private OutboundSSLSelections outboundSelections;
    private Map<String, Object> sslConfig;
    private Set<String> newConnectionInfo;

    @Before
    public void setUp() {
        outboundSelections = new OutboundSSLSelections();
        sslConfig = new HashMap<>();
        newConnectionInfo = new HashSet<>();
    }

    /**
     * Test Case 1: Single outbound connection with certificate
     * This should work both before and after the fix.
     */
    @Test
    public void testSingleOutboundConnectionWithCertificate() {
        // Setup: Single outbound connection
        List<Map<String, Object>> outboundEntries = new ArrayList<>();
        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("host", "host1.example.com");
        entry1.put("port", 8443);
        entry1.put("clientCertificate", "CERT1");
        outboundEntries.add(entry1);

        sslConfig.put("outboundConnection", outboundEntries);

        // Execute
        outboundSelections.loadOutboundConnectionInfo("testSSL", sslConfig, newConnectionInfo);

        // Verify: Should have correct alias format
        Map<String, String> selections = outboundSelections.getDynamicSelections();
        String alias = selections.get("host1.example.com,8443");

        assertNotNull("Alias should not be null", alias);
        assertEquals("Alias should be in correct format", "testSSL:CERT1", alias);

        // Verify alias can be parsed correctly (should have exactly 2 parts)
        String[] parts = alias.split(":");
        assertEquals("Alias should have exactly 2 parts", 2, parts.length);
        assertEquals("SSL config ID should be correct", "testSSL", parts[0]);
        assertEquals("Certificate should be correct", "CERT1", parts[1]);
    }

    /**
     * Test Case 2: Multiple outbound connections with SAME certificate
     * This should work both before and after the fix.
     */
    @Test
    public void testMultipleOutboundConnectionsWithSameCertificate() {
        // Setup: Multiple connections with same certificate
        List<Map<String, Object>> outboundEntries = new ArrayList<>();

        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("host", "host1.example.com");
        entry1.put("port", 8443);
        entry1.put("clientCertificate", "CERT1");
        outboundEntries.add(entry1);

        Map<String, Object> entry2 = new HashMap<>();
        entry2.put("host", "host2.example.com");
        entry2.put("port", 8443);
        entry2.put("clientCertificate", "CERT1");
        outboundEntries.add(entry2);

        sslConfig.put("outboundConnection", outboundEntries);

        // Execute
        outboundSelections.loadOutboundConnectionInfo("testSSL", sslConfig, newConnectionInfo);

        // Verify: Both should have correct alias format
        Map<String, String> selections = outboundSelections.getDynamicSelections();

        String alias1 = selections.get("host1.example.com,8443");
        assertNotNull("First alias should not be null", alias1);
        assertEquals("First alias should be correct", "testSSL:CERT1", alias1);

        String alias2 = selections.get("host2.example.com,8443");
        assertNotNull("Second alias should not be null", alias2);
        assertEquals("Second alias should be correct", "testSSL:CERT1", alias2);
    }

    /**
     * Test Case 3: Multiple outbound connections with DIFFERENT certificates
     * This is the bug scenario - fails before fix, passes after fix.
     *
     * BEFORE FIX: Second connection gets "testSSL:CERT1:CERT2" (WRONG)
     * AFTER FIX: Second connection gets "testSSL:CERT2" (CORRECT)
     */
    @Test
    public void testMultipleOutboundConnectionsWithDifferentCertificates() {
        // Setup: Multiple connections with different certificates (Kyndryl scenario)
        List<Map<String, Object>> outboundEntries = new ArrayList<>();

        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("host", "cs.ct.edac.testa.eu");
        entry1.put("port", 50049);
        entry1.put("clientCertificate", "EDAC_NAPAT_S2S_CT");
        outboundEntries.add(entry1);

        Map<String, Object> entry2 = new HashMap<>();
        entry2.put("host", "esp-proxy.ct.edac.testa.eu");
        entry2.put("port", 50060);
        entry2.put("clientCertificate", "EDAC_NAPAT_ESP_CT");
        outboundEntries.add(entry2);

        Map<String, Object> entry3 = new HashMap<>();
        entry3.put("host", "esp-fallback.ct.edac.testa.eu");
        entry3.put("port", 50170);
        entry3.put("clientCertificate", "EDAC_NAPAT_ESP_CT");
        outboundEntries.add(entry3);

        sslConfig.put("outboundConnection", outboundEntries);

        // Execute
        outboundSelections.loadOutboundConnectionInfo("grpcClientSsl", sslConfig, newConnectionInfo);

        // Verify: Each connection should have correct alias format
        Map<String, String> selections = outboundSelections.getDynamicSelections();

        // First connection
        String alias1 = selections.get("cs.ct.edac.testa.eu,50049");
        assertNotNull("First alias should not be null", alias1);
        assertEquals("First alias should be correct", "grpcClientSsl:EDAC_NAPAT_S2S_CT", alias1);
        String[] parts1 = alias1.split(":");
        assertEquals("First alias should have exactly 2 parts", 2, parts1.length);

        // Second connection - THIS IS THE KEY TEST
        String alias2 = selections.get("esp-proxy.ct.edac.testa.eu,50060");
        assertNotNull("Second alias should not be null", alias2);
        assertEquals("Second alias should be correct (not accumulated)",
                     "grpcClientSsl:EDAC_NAPAT_ESP_CT", alias2);
        String[] parts2 = alias2.split(":");
        assertEquals("Second alias should have exactly 2 parts (not 3)", 2, parts2.length);

        // Verify it's NOT the buggy format
        assertFalse("Second alias should NOT contain accumulated certificates",
                    alias2.contains("EDAC_NAPAT_S2S_CT:EDAC_NAPAT_ESP_CT"));

        // Third connection
        String alias3 = selections.get("esp-fallback.ct.edac.testa.eu,50170");
        assertNotNull("Third alias should not be null", alias3);
        assertEquals("Third alias should be correct (not accumulated)",
                     "grpcClientSsl:EDAC_NAPAT_ESP_CT", alias3);
        String[] parts3 = alias3.split(":");
        assertEquals("Third alias should have exactly 2 parts (not 4)", 2, parts3.length);

        // Verify it's NOT the buggy format
        assertFalse("Third alias should NOT contain accumulated certificates",
                    alias3.contains("EDAC_NAPAT_S2S_CT:EDAC_NAPAT_ESP_CT:EDAC_NAPAT_ESP_CT"));
    }

    /**
     * Test Case 4: Multiple connections, some with and some without certificates
     */
    @Test
    public void testMixedConnectionsWithAndWithoutCertificates() {
        // Setup: Mix of connections with and without certificates
        List<Map<String, Object>> outboundEntries = new ArrayList<>();

        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("host", "host1.example.com");
        entry1.put("port", 8443);
        entry1.put("clientCertificate", "CERT1");
        outboundEntries.add(entry1);

        Map<String, Object> entry2 = new HashMap<>();
        entry2.put("host", "host2.example.com");
        entry2.put("port", 8443);
        // No certificate specified
        outboundEntries.add(entry2);

        Map<String, Object> entry3 = new HashMap<>();
        entry3.put("host", "host3.example.com");
        entry3.put("port", 8443);
        entry3.put("clientCertificate", "CERT2");
        outboundEntries.add(entry3);

        sslConfig.put("outboundConnection", outboundEntries);

        // Execute
        outboundSelections.loadOutboundConnectionInfo("testSSL", sslConfig, newConnectionInfo);

        // Verify
        Map<String, String> selections = outboundSelections.getDynamicSelections();

        String alias1 = selections.get("host1.example.com,8443");
        assertEquals("First alias should include certificate", "testSSL:CERT1", alias1);

        String alias2 = selections.get("host2.example.com,8443");
        assertEquals("Second alias should not include certificate", "testSSL", alias2);

        String alias3 = selections.get("host3.example.com,8443");
        assertEquals("Third alias should be correct (not accumulated)", "testSSL:CERT2", alias3);
        assertFalse("Third alias should NOT contain CERT1", alias3.contains("CERT1"));
    }

    /**
     * Test Case 5: Verify alias format is parseable by getSSLConfigForAlias
     * This simulates what happens when the alias is used for SSL config lookup.
     */
    @Test
    public void testAliasFormatIsParseable() {
        // Setup: Multiple connections with different certificates
        List<Map<String, Object>> outboundEntries = new ArrayList<>();

        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("host", "host1.example.com");
        entry1.put("port", 8443);
        entry1.put("clientCertificate", "CERT1");
        outboundEntries.add(entry1);

        Map<String, Object> entry2 = new HashMap<>();
        entry2.put("host", "host2.example.com");
        entry2.put("port", 8443);
        entry2.put("clientCertificate", "CERT2");
        outboundEntries.add(entry2);

        sslConfig.put("outboundConnection", outboundEntries);

        // Execute
        outboundSelections.loadOutboundConnectionInfo("testSSL", sslConfig, newConnectionInfo);

        // Verify: Simulate what getSSLConfigForAlias does
        Map<String, String> selections = outboundSelections.getDynamicSelections();

        for (String key : selections.keySet()) {
            String alias = selections.get(key);

            // This is what getSSLConfigForAlias does
            if (alias != null && alias.indexOf(":") != -1) {
                String[] split = alias.split(":");

                // The bug causes split.length to be 3 or more, which fails this check
                assertEquals("Alias should split into exactly 2 parts for parsing: " + alias,
                           2, split.length);

                // Verify the parts are correct
                assertEquals("SSL config ID should be testSSL", "testSSL", split[0]);
                assertTrue("Certificate should be CERT1 or CERT2",
                          split[1].equals("CERT1") || split[1].equals("CERT2"));
            }
        }
    }

    /**
     * Test Case 6: Host-only connections (no port specified)
     */
    @Test
    public void testHostOnlyConnectionsWithDifferentCertificates() {
        // Setup: Host-only connections with different certificates
        List<Map<String, Object>> outboundEntries = new ArrayList<>();

        Map<String, Object> entry1 = new HashMap<>();
        entry1.put("host", "host1.example.com");
        // No port specified
        entry1.put("clientCertificate", "CERT1");
        outboundEntries.add(entry1);

        Map<String, Object> entry2 = new HashMap<>();
        entry2.put("host", "host2.example.com");
        // No port specified
        entry2.put("clientCertificate", "CERT2");
        outboundEntries.add(entry2);

        sslConfig.put("outboundConnection", outboundEntries);

        // Execute
        outboundSelections.loadOutboundConnectionInfo("testSSL", sslConfig, newConnectionInfo);

        // Verify
        Map<String, String> selections = outboundSelections.getDynamicSelections();

        String alias1 = selections.get("host1.example.com,*");
        assertEquals("First host-only alias should be correct", "testSSL:CERT1", alias1);

        String alias2 = selections.get("host2.example.com,*");
        assertEquals("Second host-only alias should be correct (not accumulated)",
                     "testSSL:CERT2", alias2);
        assertFalse("Second alias should NOT contain CERT1", alias2.contains("CERT1"));
    }

    /**
     * Test Case 7: Stress test with many connections
     */
    @Test
    public void testManyConnectionsWithDifferentCertificates() {
        // Setup: Many connections with different certificates
        List<Map<String, Object>> outboundEntries = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("host", "host" + i + ".example.com");
            entry.put("port", 8000 + i);
            entry.put("clientCertificate", "CERT" + i);
            outboundEntries.add(entry);
        }

        sslConfig.put("outboundConnection", outboundEntries);

        // Execute
        outboundSelections.loadOutboundConnectionInfo("testSSL", sslConfig, newConnectionInfo);

        // Verify: Each connection should have correct alias
        Map<String, String> selections = outboundSelections.getDynamicSelections();

        for (int i = 1; i <= 10; i++) {
            String key = "host" + i + ".example.com," + (8000 + i);
            String alias = selections.get(key);

            assertNotNull("Alias " + i + " should not be null", alias);
            assertEquals("Alias " + i + " should be correct", "testSSL:CERT" + i, alias);

            // Verify it only contains the current certificate, not accumulated ones
            String[] parts = alias.split(":");
            assertEquals("Alias " + i + " should have exactly 2 parts", 2, parts.length);

            // Verify it doesn't contain any other certificate numbers
            for (int j = 1; j <= 10; j++) {
                if (j != i) {
                    assertFalse("Alias " + i + " should not contain CERT" + j,
                              alias.contains("CERT" + j));
                }
            }
        }
    }
}

// Made with Bob
