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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class BasicAuthWithCustomRegistryTest extends CommonServletTestScenarios {

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauth.custom");
    private static Class<?> myLogClass = BasicAuthWithCustomRegistryTest.class;
    private static BasicAuthClient myClient;
    private static SSLBasicAuthClient mySSLClient;

    @BeforeClass
    public static void setUp() throws Exception {
        JACCFatUtils.installCustomRegistryFeature(myServer);
        myServer.addInstalledAppForValidation("basicauth");

        JACCFatUtils.installJaccUserFeature(myServer);
        JACCFatUtils.transformApps(myServer, "basicauth.war");

        LDAPUtils.addLDAPVariables(myServer);

        myServer.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", myServer.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", myServer.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen

        myClient = new BasicAuthClient(myServer);
        mySSLClient = new SSLBasicAuthClient(myServer);
        myClient.setJaccValidation(true);
        mySSLClient.setJaccValidation(true);
    }

    public BasicAuthWithCustomRegistryTest() {
        super(myServer, myLogClass, myClient, mySSLClient);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            JACCFatUtils.uninstallCustomRegistryFeature(myServer);
            JACCFatUtils.uninstallJaccUserFeature(myServer);
        }
    }

}
