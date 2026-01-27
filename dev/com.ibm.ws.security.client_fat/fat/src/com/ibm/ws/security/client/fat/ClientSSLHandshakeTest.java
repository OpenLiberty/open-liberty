/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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

package com.ibm.ws.security.client.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Tests SSL handshake between a Liberty client and server using the +/- cipher modifier syntax
 * in the enabledCiphers attribute of the ssl element.
 *
 * The +/- modifier syntax allows adding (+) or removing (-) cipher suites relative to the
 * JDK's default supported cipher list. For example:
 * - "+TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA" adds a specific cipher to the defaults
 * - "-TLS_RSA*" removes all ciphers matching the pattern from the defaults
 *
 * These tests verify that the CSIv2 transport layer correctly applies the +/- modifier
 * logic during SSL handshake negotiation between client and server.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ClientSSLHandshakeTest extends CommonTest {
    private static final Class<?> c = ClientSSLHandshakeTest.class;
    private static String ERRORSTRING = "Unable to initialize the BasicCalculatorClient";

    /**
     * Starts the server before each test.
     * The server is configured with a specific set of ECDHE-based cipher suites.
     * The server uses the same keystore as the myTestClient so the client already
     * trusts the server's certificate.
     */
    @Before
    public void before() throws Exception {
        String thisMethod = "before";
        Log.info(c, thisMethod, "Starting server for test: " + name.getMethodName());

        try {
            // Ensure server is fully stopped before starting
            if (testServer != null && testServer.isStarted()) {
                Log.info(c, thisMethod, "Server already started, stopping and waiting for shutdown");
                testServer.stopServer();
                // Wait for server shutdown message to ensure ports are released
                testServer.waitForStringInLog("CWWKE0036I", 30000);
            }
            
            // Start server with default configuration
            commonServerSetUp("SSLHandshakeTest", false);
            
            // Wait for server to be ready with timeout
            String featureReady = testServer.waitForStringInLog("CWWKF0008I", 60000);
            if (featureReady == null) {
                throw new Exception("Timeout waiting for FeatureManager to complete");
            }
            
            String ltpaReady = testServer.waitForStringInLog("CWWKS4105I", 30000);
            if (ltpaReady == null) {
                throw new Exception("Timeout waiting for LTPA configuration");
            }
            
            Log.info(c, thisMethod, "Server is ready for test: " + name.getMethodName());
        } catch (Exception e) {
            Log.error(c, thisMethod, e, "Server setup failed");
            // Try to clean up if setup failed
            try {
                if (testServer != null && testServer.isStarted()) {
                    testServer.stopServer();
                    // Wait for server shutdown message to ensure ports are released
                    testServer.waitForStringInLog("CWWKE0036I", 30000);
                }
            } catch (Exception cleanupEx) {
                Log.error(c, thisMethod, cleanupEx, "Cleanup after failed setup also failed");
            }
            throw new Exception("Server setup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Stops the server after each test.
     */
    @After
    public void after() {
        String thisMethod = "after";
        Log.info(c, thisMethod, "Stopping server after test: " + name.getMethodName());
        
        try {
            if (testServer != null) {
                if (testServer.isStarted()) {
                    Log.info(c, thisMethod, "Server is running, stopping it");
                    testServer.stopServer("CWWKZ0124E");
                } else {
                    Log.info(c, thisMethod, "Server is not running, no need to stop");
                }
            } else {
                Log.info(c, thisMethod, "testServer is null, nothing to stop");
            }
        } catch (Exception e) {
            Log.error(c, thisMethod, e, "Exception while stopping server");
            // Don't rethrow - we want other tests to continue
        }
        
        Log.info(c, thisMethod, "After method complete for test: " + name.getMethodName());
    }

    /**
     * Test description:
     * - Start the client with a cipher modifier that uses '+' to add a specific ECDHE cipher
     *   that is also supported by the server.
     * - The client uses: enabledCiphers="+TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"
     * - The server supports: TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA (among others)
     *
     * Expected results:
     * - The SSL handshake should succeed because the client and server share a common cipher.
     * - The client should report it has started successfully.
     */
    @Test
    public void testHandshakeWithPlusCipherModifierPass() {
        try {
            Log.info(c, name.getMethodName(), "Starting the client with '+' cipher modifier (expected to pass) ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_plus_pass.xml", "CWWKF0040E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                    output.contains("5"));
        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Start the client with a cipher modifier that uses '-' to remove all ECDHE ciphers,
     *   leaving no ciphers in common with the server (which only supports ECDHE ciphers).
     * - The client uses: enabledCiphers="-TLS_ECDHE*"
     * - The server supports only ECDHE-based ciphers.
     *
     * Expected results:
     * - The SSL handshake should fail because no common cipher suites remain after removal.
     */
    @Test
    public void testHandshakeWithMinusCipherModifierFail() {
        try {
            Log.info(c, name.getMethodName(), "Starting the client with '-' cipher modifier removing ECDHE ciphers (expected to fail) ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_minus_fail.xml", "CWWKF0040E", "CWPKI0823E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it failed with handshake exception.", output.contains(ERRORSTRING));
        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }


    /**
     * Test description:
     * - Start the client with an invalid enable ciphers attribute
     *   leaving no ciphers in common with the server.
     * - The client uses: enabledCiphers="TLS_INVALID_CIPHER"
     * - The server supports only TLS-based ciphers.
     *
     * Expected results:
     * - The SSL handshake should fail because no common cipher suites remain.
     */
    @Test
    public void testHandshakeWithInvalidCipher() {
        try {
            Log.info(c, name.getMethodName(), "Starting the client with wildcard '-' cipher modifier (expected to fail) ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_invalid.xml", "CWWKF0040E", "CWPKI0823E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it failed with handshake exception.",
                    output.contains(ERRORSTRING));
        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Start the client with an empty enable ciphers attribute
     *   leaving no ciphers in common with the server.
     * - The client uses: enabledCiphers=""
     * - The server supports only TLS-based ciphers.
     *
     * Expected results:
     * - The SSL handshake should pass because the effective JDK list is used
     */
    @Test
    public void testHandshakeWithEmptyCipher() {
        try {
            Log.info(c, name.getMethodName(), "Starting the client with wildcard '-' cipher modifier (expected to fail) ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_empty.xml", "CWWKF0040E", "CWPKI0823E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                    output.contains("5"));

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Start the client with enabledCiphers that has both filter criteria (+/-) and static entries (mixed mode).
     * - The client uses: enabledCiphers="TLS_AES_128_GCM_SHA256 +TLS_AES_256_GCM_SHA384"
     * - This configuration is not allowed - cannot mix static entries with filter modifiers.
     *
     * Expected results:
     * - Error CWPKI0837E should be logged indicating mixed mode is not allowed.
     * - The JDK default cipher list will be used without modifications.
     * - The SSL handshake should succeed using the JDK effective list.
     */
    @Test
    public void testHandshakeWithMixedModeCiphers() {
        try {
            Log.info(c, name.getMethodName(), "Starting the client with mixed mode ciphers (static + filter) - error expected, the JDK effective list used ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_mixed_mode.xml", "CWWKF0040E", "CWPKI0837E");
            String output = programOutput.getStdout();

            assertTrue("Client should log CWPKI0837E error for mixed mode cipher configuration.",
                    output.contains("CWPKI0837E"));
            assertTrue("Client should report it has started successfully using the JDK effective list (CWWKF0035I).",
                    output.contains("5"));

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Start the client with enabledCiphers that has a wildcard specified in a static entry.
     * - The client uses: enabledCiphers="*TLS_RSA*"
     * - Wildcards in static entries (without +/-) are not allowed.
     *
     * Expected results:
     * - Error CWPKI0841E should be logged indicating wildcard in static entry is not allowed.
     * - The JDK default cipher list will be used without modifications.
     * - The SSL handshake should succeed using the JDK effective list.
     */
    @Test
    public void testHandshakeWithWildcardInStaticEntry() {
        try {
            Log.info(c, name.getMethodName(), "Starting the client with wildcard in static entry - error expected, the JDK effective list used ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_wildcard_in_static.xml", "CWWKF0040E", "CWPKI0841E");
            String output = programOutput.getStdout();

            assertTrue("Client should log CWPKI0841E error for wildcard in static entry.",
                    output.contains("CWPKI0841E"));
            assertTrue("Client should report it has started successfully using the JDK effective list (CWWKF0035I).",
                    output.contains("5"));

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Start the client with enabledCiphers that has a wildcard specified in a + (add) entry.
     * - The client uses: enabledCiphers="+TLS_RSA*"
     * - Wildcards are only allowed in - (remove) entries, not in + (add) entries.
     *
     * Expected results:
     * - Error CWPKI0840E should be logged indicating wildcard in + entry is not allowed.
     * - The JDK default cipher list will be used without modifications.
     * - The SSL handshake should succeed using the JDK effective list.
     */
    @Test
    public void testHandshakeWithWildcardInPlusEntry() {
        try {
            Log.info(c, name.getMethodName(), "Starting the client with wildcard in + entry - error expected, the JDK effective list used ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_wildcard_plus.xml", "CWWKF0040E", "CWPKI0840E");
            String output = programOutput.getStdout();

            assertTrue("Client should log CWPKI0840E error for wildcard in + entry.",
                    output.contains("CWPKI0840E"));
            assertTrue("Client should report it has started successfully using the JDK effective list (CWWKF0035I).",
                    output.contains("5"));

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - The client uses no enabledCiphers
     *
     * Expected results:
     * - The JDK effective cipher list will be used without modifications.
     * - The SSL handshake should succeed using the JDK effective list.
     */
    @Test
    public void testHandshakeWithNoEnabledCiphersAttribute() {
        try {
            Log.info(c, name.getMethodName(), "Starting the client with wildcard in, the JDK effective list will be used ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_default.xml", "CWWKF0040E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully using the JDK effective list (CWWKF0035I).",
                    output.contains("5"));

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Restart the server with securityLevel set to HIGH in the SSL configuration.
     * - The securityLevel attribute is ignored.
     * - Use a valid client configuration to connect.
     * - Verify that the trace logs include all JDK effective ciphers (since securityLevel is ignored).
     *
     * Expected results:
     * - Info message CWPKI0838I is logged to the server log files.
     * - The SSL handshake should succeed using the JDK effective list.
     * - The trace should show the full JDK default cipher list (not filtered by securityLevel).
     */
    @Test
    public void testSecurityLevelHigh() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with securityLevel=HIGH ...");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();
            testServer.setServerConfigurationFile("server_security_level_high.xml");

            testServer.startServer();

            // Wait for the securityLevel message first (appears during config processing)
            assertNotNull("Server should log CWPKI0838I info message for securityLevel attribute.",
                        testServer.waitForStringInLogUsingMark("CWPKI0838I"));
            // Run the client with a valid configuration
            Log.info(c, name.getMethodName(), "Starting the client with valid configuration ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_plus_pass.xml", "CWWKF0040E");
            String output = programOutput.getStdout();
            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                    output.contains("5"));
            
            // Extract cipher list from server's trace to verify the JDK effective list are used
            // Search for adjustSupportedCiphers in server's trace.log
            List<String> traceLines = testServer.findStringsInTrace("adjustSupportedCiphers");
            
            assertFalse("Server trace should contain adjustSupportedCiphers entries", traceLines.isEmpty());
            
            // Parse the trace lines to extract the cipher list
            Pattern pattern = Pattern.compile("< adjustSupportedCiphers -> \\(\\d+\\)\\s+(.*?)\\s+Exit");
            List<String> effectiveCiphers = new ArrayList<>();
            
            for (String line : traceLines) {
                if (!line.contains("Entry")) {  // Skip Entry lines
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String cipherString = matcher.group(1).trim();
                        effectiveCiphers = Arrays.asList(cipherString.split("[,\\s]+"));
                        break;  // Use the first Exit line found
                    }
                }
            }
            
            assertFalse("Effective cipher list should not be empty", effectiveCiphers.isEmpty());
            
            // Verify that the cipher list contains typical JDK default ciphers
            // Since securityLevel is ignored, we should see the full JDK list
            Log.info(c, name.getMethodName(), "Server trace verification: Found " + effectiveCiphers.size() +
                    " ciphers in the effective list (the JDK effective list, securityLevel ignored)");
            
            // Log a few sample ciphers for verification
            if (effectiveCiphers.size() > 0) {
                Log.info(c, name.getMethodName(), "Sample ciphers from server trace: " +
                        effectiveCiphers.subList(0, Math.min(5, effectiveCiphers.size())));
            }
            
            // Verify we have a reasonable number of ciphers (the JDK effective list typically have 30+ ciphers)
            assertTrue("Expected JDK default cipher list to have at least 20 ciphers, but found: " + effectiveCiphers.size(),
                    effectiveCiphers.size() > 20);

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }
    /**
     * Test description:
     * - Restart the server with securityLevel set to LOW in the SSL configuration.
     * - The securityLevel attribute is ignored.
     * - LOW is considered a weak cipher specification.
     * - Use a valid client configuration to connect.
     *
     * Expected results:
     * - Info message CWPKI0838I and warning CWPKI0839W are logged to the server log files.
     * - The SSL handshake should succeed using the JDK effective list.
     */
    @Test
    public void testSecurityLevelLow() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with securityLevel=LOW ...");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();
            testServer.setServerConfigurationFile("server_security_level_low.xml");

            testServer.startServer();

            // Wait for the securityLevel messages (appears during config processing)
            assertNotNull("Server should log CWPKI0838I info message for securityLevel attribute.",
                        testServer.waitForStringInLogUsingMark("CWPKI0838I"));
            assertNotNull("Server should log CWPKI0839W warning for weak cipher specification.",
                        testServer.waitForStringInLogUsingMark("CWPKI0839W"));
            
            // Run the client with a valid configuration
            Log.info(c, name.getMethodName(), "Starting the client with valid configuration ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_plus_pass.xml", "CWWKF0040E", "CWWKI0003E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                    output.contains("5"));

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Restart the server with two SSL configurations, both having securityLevel set to LOW.
     * - The securityLevel attribute is ignored.
     * - LOW is considered a weak cipher specification.
     * - Use a valid client configuration to connect.
     *
     * Expected results:
     * - Info message CWPKI0838I should be logged twice (once for each SSL config).
     * - Warning message CWPKI0839W should be logged twice (once for each SSL config).
     * - The SSL handshake should succeed using the JDK effective list.
     */
    @Test
    public void testTwoSSLConfigsWithSecurityLevelLow() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with two SSL configs both having securityLevel=LOW ...");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();
            testServer.setServerConfigurationFile("server_two_ssl_configs.xml");

            testServer.startServer();

            // Wait for the securityLevel messages (appears during config processing)
            // Should appear twice - once for each SSL config
            List<String> infoMessages = testServer.findStringsInLogsUsingMark("CWPKI0838I", testServer.getDefaultLogFile());
            assertNotNull("Server should log CWPKI0838I info messages for securityLevel attribute.", infoMessages);
            assertTrue("Server should log CWPKI0838I twice (once for each SSL config), but found: " + infoMessages.size(),
                    infoMessages.size() >= 2);
            
            List<String> warningMessages = testServer.findStringsInLogsUsingMark("CWPKI0839W", testServer.getDefaultLogFile());
            assertNotNull("Server should log CWPKI0839W warning messages for weak cipher specification.", warningMessages);
            assertTrue("Server should log CWPKI0839W twice (once for each SSL config), but found: " + warningMessages.size(),
                    warningMessages.size() >= 2);
            
            Log.info(c, name.getMethodName(), "Found " + infoMessages.size() + " CWPKI0838I messages and " +
                    warningMessages.size() + " CWPKI0839W messages as expected for two SSL configs.");
            
            // Run the client with a valid configuration
            Log.info(c, name.getMethodName(), "Starting the client with valid configuration ...");
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_plus_pass.xml", "CWWKF0040E", "CWWKI0003E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                    output.contains("5"));

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Start the client with cipher modifiers that remove all ECDHE ciphers with a wildcard,
     *   then re-add a specific ECDHE cipher.
     * - The client uses: enabledCiphers="-TLS_ECDHE* +TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"
     * - This test verifies the trace output to confirm ciphers were removed and then re-added.
     *
     * Expected results:
     * - The trace should show that ECDHE ciphers were removed by the wildcard.
     * - The trace should show that TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA was re-added.
     * - The SSL handshake should succeed.
     */
    @Test
    public void testHandshakeWithWildcardRemovalAndReAddVerifyTrace() {
        try {
            Log.info(c, name.getMethodName(), "Starting the client with wildcard removal and re-add, verifying trace ...");
            
            // Set mark before starting client to capture trace
            testServer.setMarkToEndOfLog();
            
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClient", "client_handshake_remove_readd.xml", "CWWKF0040E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                    output.contains("5"));
            
            // Wait for client to complete and logs to be copied
            testClient.waitForStringInCopiedLog("CWWKE0908I");
            
            // Extract cipher list from trace in client's copied logs
            List<String> finalCiphers = extractCipherListFromTrace();
            
            assertFalse("Final cipher list should not be empty", finalCiphers.isEmpty());
            
            // Verify that TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA is in the final list (was re-added)
            assertTrue("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA should be in final cipher list after re-add",
                    finalCiphers.contains("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"));
            
            // Verify that other ECDHE ciphers were removed (should not be in the list)
            // Count how many ECDHE ciphers are in the final list
            long ecdheCount = finalCiphers.stream()
                    .filter(cipher -> cipher.startsWith("TLS_ECDHE"))
                    .count();
            
            // Should only have 1 ECDHE cipher (the one we re-added)
            assertTrue("Should have exactly 1 ECDHE cipher (the re-added one), but found: " + ecdheCount,
                    ecdheCount == 1);
            
            Log.info(c, name.getMethodName(), "Trace verification successful. Final cipher list contains " +
                    finalCiphers.size() + " ciphers with 1 ECDHE cipher as expected.");
            
        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Helper method to extract cipher list from adjustSupportedCiphers Exit trace line.
     * Parses lines like: "< adjustSupportedCiphers -> (39) TLS_AES_256_GCM_SHA384 TLS_AES_128_GCM_SHA256 ... Exit"
     * Uses findStringsInCopiedTraceLogs() to search in the client's ssl_trace.log file.
     */
    private List<String> extractCipherListFromTrace() throws Exception {

        // Use findStringsInCopiedTraceLogs to search for the trace pattern in ssl_trace.log
        List<String> traceLines = testClient.findStringsInCopiedTraceLogs("adjustSupportedCiphers.*","logs/ssl_trace.log");
        
        Log.info(c, "extractCipherListFromTrace", "Found " + traceLines.size() + " trace lines in ssl_trace.log");
        
        // Debug: print all found lines
        for (int i = 0; i < traceLines.size(); i++) {
            Log.info(c, "extractCipherListFromTrace", "Trace line " + i + ": " + traceLines.get(i));
        }
        
        Pattern pattern = Pattern.compile(
                "< adjustSupportedCiphers -> \\(\\d+\\)\\s+(.*?)\\s+Exit");

        List<String> lastCipherList = new ArrayList<>();

        for (String line : traceLines) {
            if (!line.contains("Entry")) {  // Skip Entry lines
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String cipherString = matcher.group(1).trim();
                    Log.info(c, "extractCipherListFromTrace", "Found cipher string: " + cipherString);
                    lastCipherList = Arrays.asList(cipherString.split("[,\\s]+"));
                }
            }
        }

        Log.info(c, "extractCipherListFromTrace", "Returning cipher list with " + lastCipherList.size() + " ciphers");
        return lastCipherList;
    }
    
    }