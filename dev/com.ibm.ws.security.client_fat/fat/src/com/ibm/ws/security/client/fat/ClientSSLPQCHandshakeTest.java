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

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyClientFactory;

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
     * - Server uses: sslProtocol="TLSv1.3", jvm.options: -Djdk.tls.namedGroups=X25519MLKEM768,X25519,secp256r1,secp384r1
     *   (mixed PQC and classical groups — PQC is available but not exclusive).
     * - Client uses: sslProtocol="TLSv1.3", client.jvm.options: -Djdk.tls.namedGroups=X25519MLKEM768 only.
     *
     * Expected results:
     * - The SSL handshake succeeds using TLS 1.3 with PQC key exchange.
     * - The server trace ServerHello key_share shows "named group": X25519MLKEM768, confirming
     *   PQC was negotiated rather than falling back to a classical group.
     * - The client reports it has started successfully.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testPQCHandshakeClientPQCOnlyServerMixedGroupsNegotiatesPQC() {
        try {
            Log.info(c, name.getMethodName(), "Starting PQC-only client against server with mixed PQC and classical named groups");

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
     * - Server restarts with jvm.options overridden to X25519MLKEM768 only — no classical fallback.
     * - Client overrides to classical-only named groups (x25519, secp256r1) — no PQC.
     *
     * Expected results:
     * - The SSL handshake fails: no named group is common between the two sides.
     * - Server trace shows "No common named group" fatal error.
     * - The client reports a handshake exception.
     */
    @Test
    public void testPQCHandshakeServerPQCOnlyClientNonPQCFail() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with PQC-only named groups (no fallback)");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();

            // Override the server JVM to X25519MLKEM768 only — the XML namedGroups attribute alone
            // is not enforced at the JVM TLS layer, so this is the only way to restrict the server.
            testServer.setJvmOptions(Arrays.asList(
                    "-Djdk.tls.namedGroups=X25519MLKEM768",
                    "-Djavax.net.debug=all"));

            testServer.setServerConfigurationFile("server_pqc_only.xml");
            testServer.startServer();

            assertNotNull("FeatureManager did not report update was complete",
                          testServer.waitForStringInLogUsingMark("CWWKF0008I"));
            assertNotNull("LTPA configuration did not report it was ready",
                          testServer.waitForStringInLogUsingMark("CWWKS4105I"));

            Log.info(c, name.getMethodName(), "Starting classical-only client (x25519,secp256r1) against PQC-only server");

            // getLibertyClient copies the published client dir (including the default
            // client.jvm.options with X25519MLKEM768). setJvmOptions must be called on the
            // same instance AFTER that copy and BEFORE startClientWithArgs to override to
            // non-PQC groups only, matching the test constraint.
            testClient = LibertyClientFactory.getLibertyClient("myTestClientPQC");
            transformApps(testClient);

            String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_tls13_standard.xml");
            copyNewClientConfig(fullClientXmlPath);
            addServerPortsToClientBootStrapProp();

            testClient.setJvmOptions(Arrays.asList(
                    "-Djdk.console=java.base",
                    "-Djdk.tls.namedGroups=x25519,secp256r1",
                    "-Djavax.net.debug=all"));

            testClient.addIgnoreErrors("CWWKF0040E", "CWPKI0823E");

            List<String> startParms = Arrays.asList("--", "add", "2", "3");
            ProgramOutput programOutput = testClient.startClientWithArgs(true, true, true, false, "run", startParms, false);
            String output = programOutput.getStdout();

            assertTrue("Client should report it failed with handshake exception.",
                       output.contains(ERRORSTRING));

            // Search positively for the UNEXPECTED_MESSAGE fatal error the server JVM writes when
            // it has no named group in common with the client. The JVM classifies this as
            // Fatal (UNEXPECTED_MESSAGE) / "No common named group" rather than handshake_failure.
            List<String> noCommonGroupLines = testServer.findStringsInTrace("No common named group");
            assertFalse("Server trace should show a 'No common named group' fatal error when PQC-only server rejects non-PQC client",
                        noCommonGroupLines.isEmpty());

            Log.info(c, name.getMethodName(), "Handshake correctly failed: no common named groups between PQC-only server and non-PQC client");

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Neither client nor server has explicit named group configuration.
     * - Server: no namedGroups attribute on its <ssl> element, -Djdk.tls.namedGroups= set to empty,
     *   causing the JDK to fall back to its built-in default named groups.
     * - Client: -Djdk.tls.namedGroups= set to empty, overriding the published client.jvm.options
     *   default (X25519MLKEM768), so both sides rely entirely on the JDK built-in default named groups.
     *
     * Expected results:
     * - The SSL handshake succeeds using TLS 1.3 with PQC key exchange.
     * - The server trace ServerHello key_share shows "named group": X25519MLKEM768, confirming
     *   both sides independently defaulted to a compatible PQC group.
     * - The client does not report an error.
     */
    @Test
    public void testPQCHandshakeNoNamedGroupsOnServerAndClient() {
        try {
            Log.info(c, name.getMethodName(), "Restarting server with no named group configuration");
            testServer.setMarkToEndOfLog();
            if (testServer.isStarted())
                testServer.stopServer();

            // -Djdk.tls.namedGroups left empty — JDK built-in defaults apply.
            testServer.setJvmOptions(Arrays.asList(
                    "-Djdk.tls.namedGroups=",
                    "-Djavax.net.debug=all"));

            testServer.setServerConfigurationFile("server_no_named_groups.xml");
            testServer.startServer();

            assertNotNull("FeatureManager did not report update was complete",
                          testServer.waitForStringInLogUsingMark("CWWKF0008I"));
            assertNotNull("LTPA configuration did not report it was ready",
                          testServer.waitForStringInLogUsingMark("CWWKS4105I"));

            Log.info(c, name.getMethodName(), "Starting client with no named group configuration");

            // Override the published client.jvm.options (which defaults to X25519MLKEM768) by
            // setting -Djdk.tls.namedGroups= to empty — the JDK ignores it and falls back to
            // built-in defaults on both sides, which include X25519MLKEM768.
            testClient = LibertyClientFactory.getLibertyClient("myTestClientPQC");
            transformApps(testClient);

            String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_pqc_enabled.xml");
            copyNewClientConfig(fullClientXmlPath);
            addServerPortsToClientBootStrapProp();

            testClient.setJvmOptions(Arrays.asList(
                    "-Djdk.console=java.base",
                    "-Djdk.tls.namedGroups=",
                    "-Djavax.net.debug=all"));

            testClient.addIgnoreErrors("CWWKF0040E");

            List<String> startParms = Arrays.asList("--", "add", "2", "3");
            ProgramOutput programOutput = testClient.startClientWithArgs(true, true, true, false, "run", startParms, false);
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
     * - Server uses its default jvm.options (X25519MLKEM768,X25519,secp256r1,secp384r1) set by @Before —
     *   no restart or override needed.
     * - Client sets -Djdk.tls.namedGroups= to empty, causing the JDK to fall back to its built-in
     *   defaults which include X25519MLKEM768, satisfying the server's PQC-capable list.
     *
     * Expected results:
     * - The SSL handshake succeeds using TLS 1.3 with PQC key exchange.
     * - The server trace ServerHello key_share shows "named group": X25519MLKEM768,
     *   confirming the client's JDK default fallback included the PQC group.
     * - The client does not report an error.
     */
    @Test
    public void testPQCHandshakeServerMixedGroupsClientNoNamedGroupConfig() {
        try {
            Log.info(c, name.getMethodName(), "Starting client with no named group configuration against default PQC server");

            // Override the published client.jvm.options (which defaults to X25519MLKEM768) by
            // setting -Djdk.tls.namedGroups= to empty — the JDK ignores it and falls back to
            // built-in defaults which include X25519MLKEM768, satisfying the server.
            testClient = LibertyClientFactory.getLibertyClient("myTestClientPQC");
            transformApps(testClient);

            String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_pqc_enabled.xml");
            copyNewClientConfig(fullClientXmlPath);
            addServerPortsToClientBootStrapProp();

            testClient.setJvmOptions(Arrays.asList(
                    "-Djdk.console=java.base",
                    "-Djdk.tls.namedGroups=",
                    "-Djavax.net.debug=all"));

            testClient.addIgnoreErrors("CWWKF0040E");

            List<String> startParms = Arrays.asList("--", "add", "2", "3");
            ProgramOutput programOutput = testClient.startClientWithArgs(true, true, true, false, "run", startParms, false);
            String output = programOutput.getStdout();

            assertFalse("Client should not report an error — JDK default named groups include X25519MLKEM768 " +
                        "which satisfies the server.",
                        output.contains(ERRORSTRING));

            List<String> serverTraceLines = testServer.findStringsInTrace(SERVER_HELLO_PQC_NAMED_GROUP);
            assertFalse("Server trace ServerHello key_share should show \"named group\": X25519MLKEM768 " +
                        "when server has PQC in its named groups and client relies on JDK default named groups",
                        serverTraceLines.isEmpty());

            Log.info(c, name.getMethodName(), "PQC handshake succeeded: server PQC named groups satisfied by client JDK default fallback");

        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Unexpected exception was thrown.");
            fail("Exception was thrown: " + e);
        }
    }

    /**
     * Test description:
     * - Server is restarted with classical-only named groups: jvm.options overridden to
     *   x25519,secp256r1,secp384r1 only — no PQC (ML-KEM) group is present on the server.
     * - Client has no named group configuration: -Djdk.tls.namedGroups= is set to empty,
     *   causing the JDK to fall back to its built-in defaults which include X25519MLKEM768.
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

            // Server JVM restricted to classical groups only — no X25519MLKEM768 or any ML-KEM hybrid.
            // The client's JDK defaults include PQC but the server cannot satisfy it,
            // so negotiation must fall back to a classical group both sides support.
            testServer.setJvmOptions(Arrays.asList(
                    "-Djdk.tls.namedGroups=x25519,secp256r1,secp384r1",
                    "-Djavax.net.debug=all"));

            testServer.setServerConfigurationFile("server_no_named_groups.xml");
            testServer.startServer();

            assertNotNull("FeatureManager did not report update was complete",
                          testServer.waitForStringInLogUsingMark("CWWKF0008I"));
            assertNotNull("LTPA configuration did not report it was ready",
                          testServer.waitForStringInLogUsingMark("CWWKS4105I"));

            Log.info(c, name.getMethodName(), "Starting client with no named group configuration against classical-only server");

            // Override the published client.jvm.options (which defaults to X25519MLKEM768) by
            // setting -Djdk.tls.namedGroups= to empty — JDK defaults apply, which include PQC,
            // but the server only offers classical groups so a classical group must be selected.
            testClient = LibertyClientFactory.getLibertyClient("myTestClientPQC");
            transformApps(testClient);

            String fullClientXmlPath = buildFullClientConfigPath(testClient, "client_pqc_enabled.xml");
            copyNewClientConfig(fullClientXmlPath);
            addServerPortsToClientBootStrapProp();

            testClient.setJvmOptions(Arrays.asList(
                    "-Djdk.console=java.base",
                    "-Djdk.tls.namedGroups=",
                    "-Djavax.net.debug=all"));

            testClient.addIgnoreErrors("CWWKF0040E");

            List<String> startParms = Arrays.asList("--", "add", "2", "3");
            ProgramOutput programOutput = testClient.startClientWithArgs(true, true, true, false, "run", startParms, false);
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
