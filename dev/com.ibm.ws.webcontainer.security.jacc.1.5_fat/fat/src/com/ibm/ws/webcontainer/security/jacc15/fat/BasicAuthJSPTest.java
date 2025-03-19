/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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

package com.ibm.ws.webcontainer.security.jacc15.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLServletClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class BasicAuthJSPTest extends CommonServletTestScenarios {
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauth");
    private static Class<?> myLogClass = BasicAuthJSPTest.class;
    private static BasicAuthClient myClient;
    private static SSLBasicAuthClient mySSLClient;
    private String appName = "basicauth";

    @BeforeClass
    public static void setUp() throws Exception {
        myServer.addInstalledAppForValidation("basicauth");
        //LDAPUtils.addLDAPVariables(myServer);

        JACCFatUtils.installJaccUserFeature(myServer);
        JACCFatUtils.transformApps(myServer, "basicauth.war", "basicauthXMI.ear", "basicauthXMInoAuthz.ear", "basicauthXML.ear", "basicauthXMLnoAuthz.ear");

        myServer.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", myServer.waitForStringInLog("CWWKS2200I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", myServer.waitForStringInLog("CWWKS2201I")); //Hiroko-Kristen

        myClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_JSP_NAME, BasicAuthClient.DEFAULT_JSP_CONTEXT_ROOT);
        mySSLClient = new SSLBasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_JSP_NAME, BasicAuthClient.DEFAULT_JSP_CONTEXT_ROOT);
        myClient.setJaccValidation(true);
        mySSLClient.setJaccValidation(true);

    }

    public BasicAuthJSPTest() {
        super(myServer, myLogClass, myClient, mySSLClient);
    }

    /**
     * Pass-through constructor so ServerXMLOverrides* and XM*Bindings
     * tests can sub-class this class.
     *
     * @param server
     * @param logClass
     * @param client
     * @param sslClient
     */
    protected BasicAuthJSPTest(LibertyServer server, Class<?> logClass,
                               ServletClient client,
                               SSLServletClient sslClient,
                               String appName) {
        super(server, logClass, client, sslClient);
        this.appName = appName;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(myServer);
        }
    }

}
