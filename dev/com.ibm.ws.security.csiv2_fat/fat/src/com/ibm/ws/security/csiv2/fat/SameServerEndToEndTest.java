/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.csiv2.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class SameServerEndToEndTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.csiv2.fat.downstream.server");
    private static BasicAuthClient myClient;
    private static Class<?> logClass = SameServerEndToEndTest.class;
    private static String user = "user1";
    private static String pwd = "user1pwd";

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        myClient = new BasicAuthClient(server.getHostname(), server.getHttpSecondaryPort(), BasicAuthClient.DEFAULT_REALM, "ClientServlet", "/csiv2Client");
    }

    private static void performPreTestAssertions() {
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The EAR application did not report is was available",
                      server.waitForStringInLog("CWWKZ0001I.*DownstreamApp"));
    }

    @After
    public void resetConnection() throws Exception {
        // Temporarily stop server between tests until we have dynamic configuration
        if (server != null && server.isStarted()) {
            server.stopServer(".*"); // log validation is skipped for this server, as in WS-CD-Open
        }
        myClient.resetClientState();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(".*"); // log validation is skipped for this server, as in WS-CD-Open
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testLtpaAuthenticationSupportedServerGssupLtpa() throws Exception {
        Log.info(logClass, name.getMethodName(), "Entering " + name.getMethodName());

        server.setServerConfigurationFile("gssupAuthenticationSupportedServerGssupLtpa.xml");
        server.startServer(name.getMethodName() + ".log");
        performPreTestAssertions();

        String queryString = "/ClientServlet";
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, user, pwd);

        assertTrue("Failed to find expected callerSubject: " + user, response.contains("callerSubject: Subject:"));
        assertTrue("Failed to find expected principal: " + user, response.contains("Principal: WSPrincipal:" + user));

        Log.info(logClass, name.getMethodName(), "Exiting " + name.getMethodName());
    }

}
