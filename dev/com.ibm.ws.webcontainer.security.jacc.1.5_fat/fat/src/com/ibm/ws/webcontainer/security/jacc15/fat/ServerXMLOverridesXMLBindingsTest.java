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

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.TestConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ServerXMLOverridesXMLBindingsTest extends BasicAuthTest {
    private static final String APP_NAME = "basicauthXML";
    private static final String DEFAULT_CONFIG_FILE = "basicauth.server.orig.xml";
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauth");
    private static Class<?> myLogClass = ServerXMLOverridesXMLBindingsTest.class;
    private static BasicAuthClient myClient;
    private static SSLBasicAuthClient mySSLClient;

    // TestName should be an instance level Rule, not a ClassRule, but static references are made to it,
    // so we will create it as a static field, then keep a reference to it from a non-static field which
    // is annotated with @Rule - junit can make the test method name changes to that field, which should
    // (hopefully) be reflected in the static references as well.
    private static TestName _name = new TestName();

    @Rule
    public TestName name = _name;

    private static final TestConfiguration testConfig = new TestConfiguration(myServer, myLogClass, _name, APP_NAME);

    @BeforeClass
    public static void setUp() throws Exception {

        JACCFatUtils.installJaccUserFeature(myServer);
        JACCFatUtils.transformApps(myServer, "basicauth.war", "basicauthXMI.ear", "basicauthXMInoAuthz.ear", "basicauthXML.ear", "basicauthXMLnoAuthz.ear");

        myServer.addInstalledAppForValidation(APP_NAME);
        testConfig.startServerClean(DEFAULT_CONFIG_FILE);
        //assertNotNull("JACC feature did not report it was starting", myServer.waitForStringInLog("CWWKS2200I")); //Hiroko-Kristen
        //assertNotNull("JACC feature did not report it was ready", myServer.waitForStringInLog("CWWKS2201I")); //Hiroko-Kristen

        myClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/" + APP_NAME);
        mySSLClient = new SSLBasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/" + APP_NAME);
        myClient.setJaccValidation(true);
        mySSLClient.setJaccValidation(true);
    }

    public ServerXMLOverridesXMLBindingsTest() {
        super(myServer, myLogClass, myClient, mySSLClient, APP_NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(myServer);
        }
    }

    /**
     * The application bindings in server.xml have precedence over bindings in ibm-application-bnd.xml.
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password. The user0 was defined to have access
     * to Employee role in server.xml while it does not have access to the same role in
     * ibm-application-bnd.xml.
     * <LI>Access the same page a second time.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource
     * </OL>
     */
    @Test
    public void testServerXMLOverridesXMLBindings() throws Exception {
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, "user0", "user0pwd");
        assertTrue(client.verifyResponse(response, "user0", BasicAuthClient.IS_EMPLOYEE_ROLE, BasicAuthClient.NOT_MANAGER_ROLE));
    }
}
