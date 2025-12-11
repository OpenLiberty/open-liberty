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

import static componenttest.annotation.SkipForRepeat.CHECKPOINT_RULE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLServletClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.CheckpointRule;
import componenttest.rules.repeater.CheckpointRule.ServerMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@CheckpointTest(alwaysRun = true)
public class BasicAuthTest extends CommonServletTestScenarios {
    private static String DEFAULT_CONFIG_FILE = "basicauth.server.orig.xml";
    private static LibertyServer myServer;
    private static Class<?> myLogClass = BasicAuthTest.class;
    private static BasicAuthClient myClient;
    private static SSLBasicAuthClient mySSLClient;
    private static String appName = "basicauth";
    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    // TestName should be an instance level Rule, not a ClassRule, but static references are made to it,
    // so we will create it as a static field, then keep a reference to it from a non-static field which
    // is annotated with @Rule - junit can make the test method name changes to that field, which should
    // (hopefully) be reflected in the static references as well.
    private static TestName _name = new TestName();

    @ClassRule
    public static CheckpointRule checkpointRule = new CheckpointRule()
                                                      .setConsoleLogName(BasicAuthTest.class.getSimpleName()+ ".log")
                                                      .setServerSetup(BasicAuthTest::serverSetUp)
                                                      .setServerStart(BasicAuthTest::serverStart)
                                                      .setServerTearDown(BasicAuthTest::serverTearDown);

    public static LibertyServer serverSetUp(ServerMode mode) throws Exception {
        myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauth");
        JACCFatUtils.installJaccUserFeature(myServer);
        myServer.setServerConfigurationFile(DEFAULT_CONFIG_FILE);
        JACCFatUtils.transformApps(myServer, "basicauth.war", "basicauthXMI.ear", "basicauthXMInoAuthz.ear", "basicauthXML.ear", "basicauthXMLnoAuthz.ear");
        myClient = new BasicAuthClient(myServer);
        mySSLClient = new SSLBasicAuthClient(myServer);
        myClient.setJaccValidation(true);
        mySSLClient.setJaccValidation(true);
        return myServer;
    }

    public static void serverStart(ServerMode mode, LibertyServer server) throws Exception {
        server.startServer();
        if (server.getValidateApps() && !CheckpointRule.isActive()) { // If this build is Java 7 or above
            verifyServerStartedWithJaccFeature(server);
        }
    }

    public static void serverTearDown(ServerMode mode, LibertyServer server) throws Exception {
        try {
            server.stopServer("CWWKZ0013E");
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(server);
        }
    }

    @Rule
    public TestName name = _name;

    protected static void verifyServerStartedWithJaccFeature(LibertyServer server) {
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen
    }

    public BasicAuthTest() {
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
    @SuppressWarnings("static-access")
    protected BasicAuthTest(LibertyServer server, Class<?> logClass,
                            ServletClient client,
                            SSLServletClient sslClient,
                            String appName) {
        super(server, logClass, client, sslClient);
        this.appName = appName;
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToEmployee is mapped to Employee
     * <LI>Login with a employee userId and password. Call isUserInRole with a
     * <LI>rolename of "*".
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(*) is false
     * </OL>
     */
    @Test
    public void testIsUserInRoleStarRole_Employee() throws Exception {
        String specifiedRole = "*";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE + "?role=" + specifiedRole, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, "*", false));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Start off with JACC enabled state, employee authorization is successful
     * <LI> Update server.xml to JACC disabled one. employee authorization fails
     * <LI> Change server.xml back to JACC enabled one. employee is authorized again.
     * * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> The user is authorized for the first access.
     * <LI> The user is not authorized for the second access, after the app is reinstalled.
     * <LI> The user is authorized for the third access.
     * </OL>
     */
    @Test
    @SkipForRepeat({ CHECKPOINT_RULE })
    public void testDynamicJaccOnOffOn() throws Exception {
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

        Log.info(logClass, "testDynamicJaccOnOffOn", "Jacc enabled. User was able to access. ");
        client.resetClientState();

        try {
            // change the server.xml file to remove the apps
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/noAppsInstalled.xml");

            server.waitForStringInLogUsingMark("CWWKZ0009I: The application " + appName + " has stopped successfully.");
            server.waitForStringInLogUsingMark("CWWKG0017I");
            server.waitForStringInLogUsingMark("CWWKF0008I");

            //change the server.xml to add the apps with the modified bindings
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/noJaccNoRoles.xml");
            server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + appName + " started ");
            server.waitForStringInLogUsingMark("CWWKG0017I");

            //authorization should now fail for the same user accessing the same servlet
            Log.info(logClass, "testDynamicJaccOnOffOn", "Jacc turned off, verify user can no longer access");
            assertTrue("Expected access to be denied, but it was granted",
                       client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword));
            client.resetClientState();

            // change the server.xml file to remove the apps
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/noAppsInstalled.xml");

            server.waitForStringInLogUsingMark("CWWKZ0009I: The application " + appName + " has stopped successfully.");
            server.waitForStringInLogUsingMark("CWWKG0017I");
            server.waitForStringInLogUsingMark("CWWKF0008I");

            //change the server.xml to add the apps with the modified bindings
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/yesJacc.xml");

//
//            server.stopServer();
//            myServer.uninstallUserBundle("com.ibm.ws.security.authorization.jacc.testprovider_1.0");
//            myServer.uninstallUserFeature("jaccTestProvider-1.0");
//            server.installUserBundle("com.ibm.ws.security.authorization.jacc.testprovider_1.0");
//            server.installUserFeature("jaccTestProvider-1.0");
//            server.startServer();

            server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + appName + " started ");
            server.waitForStringInLogUsingMark("CWWKG0017I");

//            assertNotNull("JACC feature did not report it was starting", myServer.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
//            assertNotNull("JACC feature did not report it was ready", myServer.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen

            response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);

            assertTrue("After Jacc re-enabled:  Verification of programmatic APIs failed",
                       client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
            client.resetClientState();

        } finally {
            //restore original configuration
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/noAppsInstalled.xml");
            server.waitForStringInLogUsingMark("CWWKZ0009I: The application " + appName + " has stopped successfully.");
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/basicauth.server.orig.xml");
            server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + appName + " started ");
        }
    }

// Removing from JACC test.   JACC does not use authorization table.  JACC also does not honor xmi/xml bind files
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
//    @Mode(TestMode.LITE)
//    @Test
//    public void testCleanupForAuthzTable() throws Exception {
//        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
//
//        client.resetClientState();
//
//        response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
//
//        client.resetClientState();
//
//        try {
//            // change the server.xml file to remove the apps
//            server.setMarkToEndOfLog();
//            server.setServerConfigurationFile("/noAppsInstalled.xml");
//
//            server.waitForStringInLogUsingMark("CWWKZ0009I: The application " + appName + " has stopped successfully.");
//            server.waitForStringInLogUsingMark("CWWKG0017I");
//            server.waitForStringInLogUsingMark("CWWKF0008I");
//
//            //change the server.xml to add the apps with the modified bindings
//            server.setMarkToEndOfLog();
//            server.setServerConfigurationFile("/unauthzForServerXMLBindings.xml");
//            server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + appName + " started ");
//            server.waitForStringInLogUsingMark("CWWKG0017I");
//
//            //authorization should now fail for the same user accessing the same servlet
//            Log.info(logClass, "testCleanupForAuthzTable", "Verifying access after cleanup");
//            assertTrue("Expected access to be denied, but it was granted",
//                       client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword));
//
//            client.resetClientState();
//
//            //authorization should still be successful for manager
//            response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
//            assertTrue("Verification of programmatic APIs failed",
//                       client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
//        } finally {
//            //restore original configuration
//            server.setMarkToEndOfLog();
//            server.setServerConfigurationFile("/noAppsInstalled.xml");
//            server.waitForStringInLogUsingMark("CWWKZ0009I: The application " + appName + " has stopped successfully.");
//            server.setMarkToEndOfLog();
//            server.setServerConfigurationFile("/basicauth.server.orig.xml");
//            server.waitForStringInLogUsingMark("CWWKZ0001I: Application " + appName + " started ");
//        }
//    }

}
