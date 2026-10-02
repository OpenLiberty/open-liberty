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
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyClientFactory;

/**
 * Tests SSL handshake between a Liberty client and server verifying Post-Quantum Cryptography
 * (PQC) named group negotiation.
 *
 * Named groups are controlled exclusively via -Djdk.tls.namedGroups in jvm.options —
 *
 * PQC evidence is verified by searching for the ServerHello key_share named group line
 * ("named group": X25519MLKEM768) in the server trace, which is produced only inside the
 * "Produced ServerHello handshake message" block when that group is actually negotiated.
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

    /** Baseline server jvm.options captured in {@link #before()} and restored in {@link #after()}. */
    private Map<String, String> originalServerJvmOptions;

    /** Baseline client jvm.options captured in {@link #before()} and restored in {@link #after()}. */
    private Map<String, String> originalClientJvmOptions;

    /**
     * Starts the SSLHandshakePQCTest server before each test and captures the baseline
     * jvm.options for both server and client so they can be restored in {@link #after()}.
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
            originalServerJvmOptions = testServer.getJvmOptionsAsMap();

            testClient = LibertyClientFactory.getLibertyClient("myTestClientPQC");
            originalClientJvmOptions = testClient.getJvmOptionsAsMap();

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
     * Stops the server and restores the original jvm.options for both server and client after each test.
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

        try {
            if (testServer != null && originalServerJvmOptions != null) {
                Log.info(c, thisMethod, "Restoring server jvm.options to baseline: " + originalServerJvmOptions);
                testServer.setJvmOptions(originalServerJvmOptions);
            }
        } catch (Exception e) {
            Log.error(c, thisMethod, e, "Failed to restore server jvm.options");
        }

        try {
            if (testClient != null && originalClientJvmOptions != null) {
                Log.info(c, thisMethod, "Restoring client jvm.options to baseline: " + originalClientJvmOptions);
                testClient.setJvmOptions(originalClientJvmOptions);
            }
        } catch (Exception e) {
            Log.error(c, thisMethod, e, "Failed to restore client jvm.options");
        }

        Log.info(c, thisMethod, "After method complete for test: " + name.getMethodName());
    }

    /**
     * Test description:
     * - Neither client nor server has explicit named group configuration.
     * - Both sides use their published jvm.options defaults: -Djdk.tls.namedGroups= is empty,
     *   so the JDK falls back to its built-in default named groups on both ends.
     *
     * Expected results:
     * - The SSL handshake succeeds using TLS 1.3 with PQC key exchange.
     * - The server trace ServerHello key_share shows "named group": X25519MLKEM768, confirming
     *   both sides independently defaulted to a compatible PQC group.
     * - The client does not report an error.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testPQCHandshakeNoNamedGroupsOnServerAndClient() {
        try {
            // No jvm.options manipulation needed — the published defaults for both server and
            // client already have -Djdk.tls.namedGroups= empty, so JDK built-in defaults apply.
            // The server is already running from before(); the client uses the same published defaults.
            Log.info(c, name.getMethodName(), "Running default-config test: no named groups on either side");

            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClientPQC", "client_pqc_enabled.xml", "CWWKF0040E");
            String output = programOutput.getStdout();

            assertFalse("Client should not report an error — both sides should negotiate using JDK default named groups.",
                        output.contains(ERRORSTRING));

            // Both sides used JDK defaults — X25519MLKEM768 is in the default list on both ends.
            // Confirm it was selected in the ServerHello key_share.
            List<String> serverTraceLines = testServer.findStringsInTrace(SERVER_HELLO_PQC_NAMED_GROUP);
            assertFalse("Server trace ServerHello key_share should show \"named group\": X25519MLKEM768 " +
                        "when both client and server rely on JDK default named groups with no explicit config",
                        serverTraceLines.isEmpty());

            Log.info(c, name.getMethodName(), "PQC handshake succeeded with no named group config on either side: both JDK defaults include X25519MLKEM768");

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Server is restarted with classical-only named groups: -Djdk.tls.namedGroups appended to
     *   the baseline jvm.options as x25519,secp256r1,secp384r1 — no PQC group on the server.
     * - Client uses its published jvm.options default: -Djdk.tls.namedGroups= is empty,
     *   so the JDK falls back to its built-in defaults which include X25519MLKEM768.
     *
     * Expected results:
     * - The SSL handshake succeeds using TLS 1.3 with a classical key exchange group.
     * - The server trace shows "Ignore unsupported named group: X25519MLKEM768", confirming
     *   the server explicitly discarded the PQC group and fell back to a classical group.
     * - The client does not report an error.
     */
    @Test
    public void testPQCHandshakeServerClassicalOnlyClientNoNamedGroupConfig() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with classical-only named groups");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();

            // Append the classical-only override onto the captured baseline — no ML-KEM hybrid.
            // The client keeps its published default (empty namedGroups → JDK built-in defaults,
            // which include X25519MLKEM768), so the server will discard it and fall back.
            Map<String, String> serverOpts = testServer.getJvmOptionsAsMap();
            serverOpts.put("-Djdk.tls.namedGroups", "x25519,secp256r1,secp384r1");
            testServer.setJvmOptions(serverOpts);

            testServer.startServer();

            assertNotNull("FeatureManager did not report update was complete",
                          testServer.waitForStringInLogUsingMark("CWWKF0008I"));
            assertNotNull("LTPA configuration did not report it was ready",
                          testServer.waitForStringInLogUsingMark("CWWKS4105I"));

            Log.info(c, name.getMethodName(), "Starting client with no named group configuration against classical-only server");

            // No client jvm.options manipulation needed — the published client.jvm.options already
            // has -Djdk.tls.namedGroups= empty, so JDK built-in defaults (including PQC) apply.
            ProgramOutput programOutput = commonClientSetUpWithCalcArgs("myTestClientPQC", "client_pqc_enabled.xml", "CWWKF0040E");
            String output = programOutput.getStdout();

            assertFalse("Client should not report an error — both sides share classical groups and handshake should succeed.",
                        output.contains(ERRORSTRING));

            // The server rejected the client's X25519MLKEM768 key share and fell back to a classical group.
            // This log line is written by the server JVM when it discards a group it does not support,
            // confirming PQC was not negotiated.
            List<String> ignoredPQCLines = testServer.findStringsInTrace("Ignore unsupported named group: X25519MLKEM768");
            assertFalse("Server trace should show X25519MLKEM768 was ignored when server is restricted to classical named groups only",
                        ignoredPQCLines.isEmpty());

            Log.info(c, name.getMethodName(), "Handshake succeeded with classical group: server classical-only restriction correctly prevented PQC negotiation");

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }
}
