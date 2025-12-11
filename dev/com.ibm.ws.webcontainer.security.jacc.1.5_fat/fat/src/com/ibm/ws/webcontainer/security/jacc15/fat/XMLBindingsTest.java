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
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.TestConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Performs the same tests as BasicAuthTest, but against an EAR with ibm-application-bnd.xml bindings.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class XMLBindingsTest extends BasicAuthTest {
    private static final String APP_NAME = "basicauthXML";
    private static final String DEFAULT_CONFIG_FILE = "bindings.server.orig.xml";
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.bindings");
    private static Class<?> myLogClass = XMLBindingsTest.class;
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
        JACCFatUtils.transformApps(myServer, "basicauthXMI.ear", "basicauthXMInoAuthz.ear", "basicauthXML.ear", "basicauthXMLnoAuthz.ear");

        myServer.addInstalledAppForValidation(APP_NAME);
        testConfig.startServerClean(DEFAULT_CONFIG_FILE);

        myClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/" + APP_NAME);
        mySSLClient = new SSLBasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/" + APP_NAME);
        myClient.setJaccValidation(true);
        mySSLClient.setJaccValidation(true);

        assertNotNull("JACC feature did not report it was starting", myServer.waitForStringInLog("CWWKS2850I"));
        assertNotNull("JACC feature did not report it was ready", myServer.waitForStringInLog("CWWKS2851I"));
    }

    public XMLBindingsTest() {
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
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for basic authentication
     * with a valid userId and password (e.g. the user exist in the user registry)
     * but, user0 does not have permission to access this protected servlet
     * as specified in ibm-application-bnd.xml (user0 belongs to group1).
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI>Authorization denied, 403, to the protected resource.
     * </OL>
     */
    /*
     * JACC does not support xmi/xml overrides
     *
     * @Test
     * public void testOverrides() throws Exception {
     * testConfig.setServerConfiguration(DEFAULT_CONFIG_FILE);
     * assertTrue(client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, "user0", "user0pwd"));
     * }
     */

    // JACC does not support authorization table nor binding
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet as the authorized user.
//     * <LI>Uninstall the app
//     * <LI>Reinstall the app with the modified bindings configuration in the server.xml,
//     * so that the user is no longer authorized
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI> The user is authorized for the first access.
//     * <LI> The user is not authorized for the second access, after the app is reinstalled.
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testCleanupForAuthzTable() throws Exception {
//        testConfig.setServerConfiguration(DEFAULT_CONFIG_FILE);
//
//        // employee should be authorized
//        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
//        assertTrue(client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
//
//        client.resetClientState();
//
//        // manager should be authorized
//        response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
//        assertTrue(client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
//
//        client.resetClientState();
//
//        try {
//            removeApps();
//
//            // change the server.xml to install the ears with the modified bindings
//            // employee should now NOT be authorized
//            testConfig.setServerConfiguration("/unauthzForEarBindings.xml");
//            testConfig.assertApplicationStarted();
//
//            // employee should not be authorized
//            Log.info(logClass, "testCleanupForAuthzTable", "Verifying authorization after cleanup");
//            assertTrue(client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword));
//
//            client.resetClientState();
//
//            // manager should still be authorized
//            response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
//            assertTrue(client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
//
//            client.resetClientState();
//        } finally {
//            restoreOriginalConfiguration();
//        }
//    }

    /**
     * Employee role defined in ibm-application-bnd.xml but not in server.xml
     */
    @Test
    public void testMergeBindingsAndServerXML() throws Exception {
        String methodName = "testMergeBindingsAndServerXML";
        testConfig.setServerConfiguration(DEFAULT_CONFIG_FILE);

        // employee should be authorized
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue(client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

        client.resetClientState();

        // manager should be authorized
        response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
        assertTrue(client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));

        client.resetClientState();

        try {
            removeApps();

            // change the server.xml to install the ears with the modified bindings
            // employee is defined only in bindings file
            testConfig.setServerConfiguration("/mergeBindingsAndServerXML.xml");
            testConfig.assertApplicationStarted();

            // employee (user1) should be authorized
            Log.info(logClass, methodName, "Verifying authorization of user1");
            response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
            assertTrue(client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

            client.resetClientState();

            // manager (user2) should still be authorized
            Log.info(logClass, methodName, "Verifying authorization of user2");
            response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
            assertTrue(client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));

            client.resetClientState();
        } finally {
            restoreOriginalConfiguration();
        }
    }

    private void removeApps() throws Exception {
        testConfig.setServerConfiguration("/noAppsInstalled.xml");
        testConfig.assertApplicationStopped();
    }

    private void restoreOriginalConfiguration() throws Exception {
        removeApps();

        testConfig.setServerConfiguration(DEFAULT_CONFIG_FILE);
        testConfig.assertApplicationStarted();
    }

}
