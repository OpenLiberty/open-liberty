/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
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

package com.ibm.ws.jca.fat.regr;

import static org.junit.Assert.assertNotNull;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.CheckpointRule;
import componenttest.rules.repeater.CheckpointRule.ServerMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Subset of tests for rapid regression.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@CheckpointTest(alwaysRun = true)
public class InboundSecurityTestRapid extends JCAFATTest implements RarTests {
    private final static Class<?> c = InboundSecurityTestRapid.class;
    private final String servletName = "InboundSecurityTestServlet";
    private static ServerConfiguration originalServerConfig;

    @ClassRule
    public static CheckpointRule checkpointRule = new CheckpointRule()
                    .setConsoleLogName(InboundSecurityTestRapid.class.getSimpleName() + ".log")
                    .addUnsupportedRepeatIDs(EmptyAction.ID)
                    .setServerSetup(InboundSecurityTestRapid::serverSetUp)
                    .setServerStart(InboundSecurityTestRapid::serverStart)
                    .setServerTearDown(InboundSecurityTestRapid::serverTearDown);

    public static LibertyServer serverSetUp(ServerMode mode) throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, true);
        buildFvtAppEar();
        return server;
    }

    public static void serverStart(ServerMode mode, LibertyServer server) throws Exception {
        /*
         * Start the server.
         */
        originalServerConfig = server.getServerConfiguration().clone();
        server.startServer();
        server.waitForStringInLog("CWWKE0002I");
        server.waitForStringInLog("CWWKZ0001I:.*fvtapp"); // Wait for application start.
        server.waitForStringInLog("CWWKF0011I");
        server.waitForStringInLog("CWWKS4104A"); // Wait for Ltpa keys to be generated
    }

    public static void serverTearDown(ServerMode mode, LibertyServer server) throws Exception {
        try {
            server.stopServer("J2CA0670E: .*NonExistentRealm/JosephABCDEF", // EXPECTED
                              "J2CA0671E", // EXPECTED
                              "J2CA0668E", // EXPECTED
                              "J2CA0676E: .*CallerPrincipalCallback" // EXPECTED
            );
        } finally {
            server.updateServerConfiguration(originalServerConfig);
        }
    }

    @Test
    public void testCallerIdentityPropagationFromCallerPrincipalCallback() throws Exception {
        String testName = "testCallerIdentityPropagationFromCallerPrincipalCallback";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationFailureForDifferentRealm() throws Exception {
        String testName = "testCallerIdentityPropagationFailureForDifferentRealm";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0670E not found.",
                      server.waitForStringInLog(INVALID_USER_NAME_IN_PRINCIPAL_J2CA0670));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("Expected Message J2CA0672E not found.",
                      server.waitForStringInLog(ERROR_HANDLING_CALLBACK_J2CA0672));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationFailureForMultipleCPC() throws Exception {
        String testName = "testCallerIdentityPropagationFailureForMultipleCPC";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0676E not found.",
                      server.waitForStringInLog(MULTIPLE_CALLERPRINCIPALCALLBACKS_NOT_SUPPORTED_J2CA0676));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("Expected Message J2CA0672E not found.",
                      server.waitForStringInLog(ERROR_HANDLING_CALLBACK_J2CA0672));

    }

    @Test
    public void testEJBInvocation() throws Exception {
        String testName = "testEJBInvocation";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("The log contains a EJBDEMOEXECUTE message.",
                      server.waitForStringInLog("EJBDEMOEXECUTE"));
        assertNotNull("The log contains a SECJ0053E error message.",
                      server.waitForStringInLog(AUTHORIZATION_FAILED_SECJ0053E));
    }

}
