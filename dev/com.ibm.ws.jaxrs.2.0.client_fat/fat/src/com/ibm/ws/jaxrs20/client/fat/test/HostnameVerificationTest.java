/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.client.fat.test;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.jaxrs.client.fat.hostnameverification.servlet.HostnameVerificationClientTestServlet;

@RunWith(FATRunner.class)
public class HostnameVerificationTest extends FATServletClient {

    private static final String appName = "simpleSSL";

    @Server("com.ibm.ws.jaxrs.2.0.client.fat.HostnameVerificationTest")
    @TestServlet(servlet = HostnameVerificationClientTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Build an application and export it to the dropins directory
        ShrinkHelper.defaultDropinApp(server, appName, "io.openliberty.jaxrs.client.fat.simpleSSL", "io.openliberty.jaxrs.client.fat.hostnameverification.servlet");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("server.log", true);
            assertNotNull("The server did not start", server.waitForStringInLog("CWWKF0011I"));
            assertNotNull("FeatureManager did not report update was complete", server.waitForStringInLog("CWWKF0008I"));

            // appSecurity-2.0 is not needed for SSL
            // You MUST wait for the security service to report that it is ready.
//            assertNotNull("The security service did not report that it was ready.", server.waitForStringInLog("CWWKS0008I"));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (server != null) {
            // CWPKI0824E: SSL HANDSHAKE FAILURE:  Host name verification error while connecting to host [localhost].
            // CWPKI0063W: Hostname verification is disabled for mySSLConfigNoHNV. TLS/SSL connections do not check server identities to verify that the client is communicating with the correct server.
            server.stopServer("CWWKO0801E", "CWWKE1102W", "CWPKI0824E", "CWPKI0063W");  //ignore server quiesce timeouts due to slow test machines
        }
    }

    @Before
    public void beforeTest() {}

    @After
    public void afterTest() {}
}
