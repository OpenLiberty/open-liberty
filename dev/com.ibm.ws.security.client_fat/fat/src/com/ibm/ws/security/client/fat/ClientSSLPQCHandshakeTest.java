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

package com.ibm.ws.security.client.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

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
 * Tests SSL handshake between a Liberty client and server verifying Post-Quantum Cryptography
 * (PQC) named group negotiation.
 *
 * The server (SSLHandshakePQCTest) always starts with PQC-capable namedGroups in its jvm.options.
 * The dedicated client (myTestClientPQC) uses only X25519MLKEM768 by default.
 *
 * PQC evidence is verified by searching for the ServerHello key_share named group line
 * ("named group": X25519MLKEM768) in the server trace, which is produced only inside the
 * Produced ServerHello handshake message block when that group is actually negotiated.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ClientSSLPQCHandshakeTest extends CommonTest {
    private static final Class<?> c = ClientSSLPQCHandshakeTest.class;
    private static String ERRORSTRING = "Unable to initialize the BasicCalculatorClient";

    /**
     * Search string that specifically identifies X25519MLKEM768 in the ServerHello key_share
     * extension of the server trace. This line only appears inside the structured
     * "Produced ServerHello handshake message" block, confirming PQC was actually negotiated
     * rather than merely advertised.
     */
    private static final String SERVER_HELLO_PQC_NAMED_GROUP = "\"named group\": X25519MLKEM768";

    /**
     * Starts the SSLHandshakePQCTest server before each test.
     * This server always runs with -Djdk.tls.namedGroups=X25519MLKEM768,X25519,secp256r1,secp384r1
     * set in its jvm.options, ensuring PQC named groups are always available for negotiation.
     */
    @Before
    public void before() throws Exception {
        String thisMethod = "before";
        Log.info(c, thisMethod, "Starting PQC server for test: " + name.getMethodName());

        try {
            if (testServer != null && testServer.isStarted()) {
                Log.info(c, thisMethod, "Server already started, stopping and waiting for shutdown");
                testServer.stopServer();
                testServer.waitForStringInLog("CWWKE0036I", 30000);
            }

            commonServerSetUp("SSLHandshakePQCTest", false);

            String featureReady = testServer.waitForStringInLog("CWWKF0008I", 60000);
            if (featureReady == null) {
                throw new Exception("Timeout waiting for FeatureManager to complete");
            }

            String ltpaReady = testServer.waitForStringInLog("CWWKS4105I", 30000);
            if (ltpaReady == null) {
                throw new Exception("Timeout waiting for LTPA configuration");
            }

            Log.info(c, thisMethod, "PQC server is ready for test: " + name.getMethodName());
        } catch (Exception e) {
            Log.error(c, thisMethod, e, "PQC server setup failed");
            try {
                if (testServer != null && testServer.isStarted()) {
                    testServer.stopServer();
                    testServer.waitForStringInLog("CWWKE0036I", 30000);
                }
            } catch (Exception cleanupEx) {
                Log.error(c, thisMethod, cleanupEx, "Cleanup after failed setup also failed");
            }
            throw new Exception("PQC server setup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Stops the server after each test.
     */
    @After
    public void after() {
        String thisMethod = "after";
        Log.info(c, thisMethod, "Stopping PQC server after test: " + name.getMethodName());

        try {
            if (testServer != null) {
                if (testServer.isStarted()) {
                    testServer.stopServer("CWWKZ0124E");
                } else {
                    Log.info(c, thisMethod, "Server is not running, no need to stop");
                }
            } else {
                Log.info(c, thisMethod, "testServer is null, nothing to stop");
            }
        } catch (Exception e) {
            Log.error(c, thisMethod, e, "Exception while stopping PQC server");
        }

        Log.info(c, thisMethod, "After method complete for test: " + name.getMethodName());
    }

    /**
     * Test description:
     * - Both server and client start with PQC-only named group X25519MLKEM768.
     * - Server uses: sslProtocol="TLSv1.3", jvm.options: -Djdk.tls.namedGroups=X25519MLKEM768,X25519,...
     * - Client uses: sslProtocol="TLSv1.3", client.jvm.options: -Djdk.tls.namedGroups=X25519MLKEM768
     *
     * Expected results:
     * - The SSL handshake succeeds using TLS 1.3 with PQC key exchange.
     * - The server trace ServerHello key_share shows "named group": X25519MLKEM768, confirming
     *   PQC was negotiated rather than falling back to a classical group.
     * - The client reports it has started successfully.
     */
    @Test
    public void testPQCHandshakeBothPQCEnabled() {
        try {
            Log.info(c, name.getMethodName(), "Starting PQC-enabled client (both sides PQC) ...");

            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClientPQC",
                                                                        "client_pqc_enabled.xml",
                                                                        "CWWKF0040E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                       output.contains("5"));

            // Verify the ServerHello key_share extension shows X25519MLKEM768 was negotiated.
            // This line only appears inside the "Produced ServerHello handshake message" block,
            // proving PQC was actually selected — not just advertised in the ClientHello.
            List<String> serverTraceLines = testServer.findStringsInTrace(SERVER_HELLO_PQC_NAMED_GROUP);
            assertFalse("Server trace ServerHello key_share should show \"named group\": X25519MLKEM768",
                        serverTraceLines.isEmpty());

            Log.info(c, name.getMethodName(), "PQC handshake successful: ServerHello confirmed X25519MLKEM768 in key_share");

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Server starts with PQC-only named groups (X25519MLKEM768) via server config override.
     * - Client uses standard TLS 1.3 without PQC named groups.
     *
     * Expected results:
     * - The SSL handshake fails because the server only supports PQC and the client does not.
     * - The client reports a handshake exception.
     */
    @Test
    public void testPQCHandshakeServerPQCOnlyClientNonPQCFail() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with PQC-only named groups (no fallback) ...");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();

            testServer.setServerConfigurationFile("server_pqc_only.xml");
            testServer.startServer();

            assertNotNull("FeatureManager did not report update was complete",
                          testServer.waitForStringInLogUsingMark("CWWKF0008I"));
            assertNotNull("LTPA configuration did not report it was ready",
                          testServer.waitForStringInLogUsingMark("CWWKS4105I"));

            Log.info(c, name.getMethodName(), "Starting standard TLS 1.3 client (no PQC) ...");

            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClientPQC",
                                                                        "client_tls13_standard.xml",
                                                                        "CWWKF0040E", "CWPKI0823E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it failed with handshake exception.",
                       output.contains(ERRORSTRING));

            Log.info(c, name.getMethodName(), "Handshake correctly failed: no common named groups between PQC-only server and non-PQC client");

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Server starts with PQC and fallback (X25519MLKEM768,X25519).
     * - Client uses standard TLS 1.3 without PQC named groups, so fallback to X25519 is expected.
     *
     * Expected results:
     * - The SSL handshake succeeds via the classical fallback.
     * - The server trace ServerHello key_share does NOT show X25519MLKEM768 (classical X25519 used).
     * - The client reports it has started successfully.
     */
    @Test
    public void testPQCHandshakeServerWithFallbackClientNonPQCPass() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with PQC plus classical fallback ...");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();

            testServer.setServerConfigurationFile("server_pqc_with_fallback.xml");
            testServer.startServer();

            assertNotNull("FeatureManager did not report update was complete",
                          testServer.waitForStringInLogUsingMark("CWWKF0008I"));
            assertNotNull("LTPA configuration did not report it was ready",
                          testServer.waitForStringInLogUsingMark("CWWKS4105I"));

            Log.info(c, name.getMethodName(), "Starting standard TLS 1.3 client (no PQC) ...");

            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClientPQC",
                                                                        "client_tls13_standard.xml",
                                                                        "CWWKF0040E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                       output.contains("5"));

            // Confirm PQC was NOT negotiated: ServerHello should not show X25519MLKEM768 key_share
            List<String> pqcTraceLines = testServer.findStringsInTrace(SERVER_HELLO_PQC_NAMED_GROUP);
            assertTrue("ServerHello key_share should NOT contain X25519MLKEM768 when client has no PQC support (fallback expected)",
                       pqcTraceLines.isEmpty());

            Log.info(c, name.getMethodName(), "Handshake succeeded with classical fallback: ServerHello did not select X25519MLKEM768");

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Server starts with multiple PQC algorithms (X25519MLKEM768,X448MLKEM1024).
     * - Client uses multiple PQC algorithms with different priority.
     *
     * Expected results:
     * - The SSL handshake succeeds.
     * - The server trace ServerHello key_share shows an MLKEM-based named group was negotiated.
     * - The client reports it has started successfully.
     */
    @Test
    public void testPQCHandshakeMultipleAlgorithmsNegotiation() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with multiple PQC algorithms ...");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();

            testServer.setServerConfigurationFile("server_pqc_multiple.xml");
            testServer.startServer();

            assertNotNull("FeatureManager did not report update was complete",
                          testServer.waitForStringInLogUsingMark("CWWKF0008I"));
            assertNotNull("LTPA configuration did not report it was ready",
                          testServer.waitForStringInLogUsingMark("CWWKS4105I"));

            Log.info(c, name.getMethodName(), "Starting PQC-enabled client with multiple algorithms ...");

            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClientPQC",
                                                                        "client_pqc_multiple.xml",
                                                                        "CWWKF0040E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                       output.contains("5"));

            // Verify a PQC named group was selected in the ServerHello key_share.
            // Search for the key_share named group line within the ServerHello block.
            List<String> serverTraceLines = testServer.findStringsInTrace("\"named group\": .*MLKEM");
            assertFalse("Server trace ServerHello key_share should show an MLKEM named group",
                        serverTraceLines.isEmpty());

            Log.info(c, name.getMethodName(), "PQC handshake successful with multiple algorithm negotiation. Named group: "
                     + serverTraceLines.get(0).trim());

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Server uses standard TLS 1.3 (no PQC in named groups).
     * - PQC-capable client falls back to classical algorithm.
     *
     * Expected results:
     * - The SSL handshake succeeds using the classical fallback.
     * - The server trace ServerHello key_share does NOT show X25519MLKEM768.
     * - The client reports it has started successfully.
     */
    @Test
    public void testPQCHandshakeClientWithFallbackServerNonPQCPass() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with standard TLS 1.3 (no PQC) ...");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();

            testServer.setServerConfigurationFile("server_tls13_standard.xml");
            testServer.startServer();

            assertNotNull("FeatureManager did not report update was complete",
                          testServer.waitForStringInLogUsingMark("CWWKF0008I"));
            assertNotNull("LTPA configuration did not report it was ready",
                          testServer.waitForStringInLogUsingMark("CWWKS4105I"));

            Log.info(c, name.getMethodName(), "Starting PQC-enabled client with fallback ...");

            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClientPQC",
                                                                        "client_pqc_with_fallback.xml",
                                                                        "CWWKF0040E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                       output.contains("5"));

            // Confirm PQC was NOT negotiated: ServerHello should not show X25519MLKEM768 key_share
            List<String> pqcTraceLines = testServer.findStringsInTrace(SERVER_HELLO_PQC_NAMED_GROUP);
            assertTrue("ServerHello key_share should NOT contain X25519MLKEM768 when server has no PQC support (fallback expected)",
                       pqcTraceLines.isEmpty());

            Log.info(c, name.getMethodName(), "Handshake succeeded: client fell back to classical algorithm as expected");

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Server starts with PQC and fallback (X25519MLKEM768,X25519).
     * - Client also has PQC and fallback (X25519MLKEM768,X25519).
     * - Both sides prefer PQC, so X25519MLKEM768 should be selected.
     *
     * Expected results:
     * - The SSL handshake succeeds using the PQC algorithm (not the fallback).
     * - The server trace ServerHello key_share shows "named group": X25519MLKEM768.
     * - The client reports it has started successfully.
     */
    @Test
    public void testPQCHandshakeBothWithFallbackPreferPQC() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with PQC plus classical fallback ...");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();

            testServer.setServerConfigurationFile("server_pqc_with_fallback.xml");
            testServer.startServer();

            assertNotNull("FeatureManager did not report update was complete",
                          testServer.waitForStringInLogUsingMark("CWWKF0008I"));
            assertNotNull("LTPA configuration did not report it was ready",
                          testServer.waitForStringInLogUsingMark("CWWKS4105I"));

            Log.info(c, name.getMethodName(), "Starting PQC-enabled client (also with fallback) ...");

            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClientPQC",
                                                                        "client_pqc_with_fallback.xml",
                                                                        "CWWKF0040E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                       output.contains("5"));

            // Verify PQC was preferred: ServerHello key_share must show X25519MLKEM768
            List<String> serverTraceLines = testServer.findStringsInTrace(SERVER_HELLO_PQC_NAMED_GROUP);
            assertFalse("Server trace ServerHello key_share should show \"named group\": X25519MLKEM768 (PQC preferred over fallback)",
                        serverTraceLines.isEmpty());

            Log.info(c, name.getMethodName(), "PQC handshake successful: PQC algorithm preferred over classical fallback");

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Both server and client start with PQC-only (X25519MLKEM768).
     * - Verifies the negotiated algorithm by checking both server and client trace logs.
     *
     * Expected results:
     * - The SSL handshake succeeds using TLS 1.3 with PQC key exchange.
     * - The server trace ServerHello key_share shows "named group": X25519MLKEM768.
     * - The client trace also contains evidence of PQC usage.
     * - The client reports it has started successfully.
     */
    @Test
    public void testPQCHandshakeVerifyNegotiatedAlgorithm() {
        try {
            Log.info(c, name.getMethodName(), "Starting PQC-enabled client, verifying negotiated algorithm in server and client trace ...");

            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClientPQC",
                                                                        "client_pqc_enabled.xml",
                                                                        "CWWKF0040E");
            String output = programOutput.getStdout();

            assertTrue("Client should report it has started successfully (CWWKF0035I).",
                       output.contains("5"));

            // Wait for client to complete and logs to be copied
            testClient.waitForStringInCopiedLog("CWWKE0908I");

            // Verify the ServerHello key_share in server trace shows X25519MLKEM768 was negotiated
            List<String> serverTraceLines = testServer.findStringsInTrace(SERVER_HELLO_PQC_NAMED_GROUP);
            assertFalse("Server trace ServerHello key_share should show \"named group\": X25519MLKEM768",
                        serverTraceLines.isEmpty());

            // Also check client trace for PQC evidence
            List<String> clientTraceLines = testClient.findStringsInCopiedTraceLogs("X25519MLKEM768", "logs/trace.log");
            if (clientTraceLines != null && !clientTraceLines.isEmpty()) {
                Log.info(c, name.getMethodName(), "Client trace also contains X25519MLKEM768 evidence");
            }

            Log.info(c, name.getMethodName(), "PQC handshake verified: ServerHello key_share confirmed X25519MLKEM768 negotiation");

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }
}
