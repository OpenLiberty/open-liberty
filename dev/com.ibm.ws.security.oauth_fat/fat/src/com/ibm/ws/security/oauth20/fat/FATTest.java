/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

public class FATTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.oauth-2.0.fat");
    private final Class<?> c = FATTest.class;

    // URL of first page that client will invoke
    // static String firstClientUrl = "http://SP-HOST-NAME:SP-HTTP-PORT/oauthclient/client.jsp";
    static String firstClientUrl = "";

    /**
     * Sets up any configuration required for running the OAuth tests.
     * Currently, it just starts the server, which should start the
     * applications in dropins.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        LDAPUtils.addLDAPVariables(server);

        server.startServer();
        server.waitForStringInLog("port " + server.getHttpDefaultPort());
        firstClientUrl = "http://localhost:" + server.getHttpDefaultPort() + "/" + Constants.OAUTHCLIENT_APP + "/client.jsp";
        assertNotNull("The application did not report is was available",
                      server.waitForStringInLog("CWWKT0016I.*oauth2"));

    }

    /**
     * TestDescription:
     *
     * Initial test: just verify that the server and the oauth web application start
     *
     */
    @Test
    public void testOAuthServerStart() throws Exception {
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
