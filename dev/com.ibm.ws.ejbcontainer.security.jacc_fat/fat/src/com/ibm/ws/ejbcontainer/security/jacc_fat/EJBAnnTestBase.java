/*******************************************************************************
 * Copyright (c) 2020,2024 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class EJBAnnTestBase {

    protected static String serverConfigurationFile = Constants.DEFAULT_CONFIG_FILE;

    // Values to be set by the child class
    protected static LibertyServer server;
    protected static Class<?> logClass;
    protected static ServletClient client;
    protected static EJBAnnTestBaseHelper testHelper;

    private static final Class<?> c = EJBAnnTestBase.class;

    public static void configureBootStrapProperties(LibertyServer server, Map<String, String> properties) throws Exception, IOException, FileNotFoundException {
        Properties bootStrapProperties = new Properties();
        File bootStrapPropertiesFile = new File(server.getFileFromLibertyServerRoot("bootstrap.properties").getAbsolutePath());
        if (bootStrapPropertiesFile.isFile()) {
            try (InputStream in = new FileInputStream(bootStrapPropertiesFile)) {
                bootStrapProperties.load(in);
            }
        }
        bootStrapProperties.putAll(properties);
        try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
            bootStrapProperties.store(out, "");
        }
    }

    public static void commonSetup(Class<?> classLog, String serverName, String appName, String servletName, String contextRoot) throws Exception {
        String thisMethod = "commonSetUp";
        Log.info(c, thisMethod, "***************Starting CommonSetup for test: " + classLog + " ***************");
        logClass = classLog;

        if (serverName != null) {
            server = LibertyServerFactory.getLibertyServer(serverName);
        }
        client = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, servletName, contextRoot);

        JACCFatUtils.installJaccUserFeature(server);

        if (FeatureReplacementAction.isCheckpointRepeatActionActive()) {
            Map<String, String> properties = new HashMap<>();
            properties.put("websphere.java.security.exempt", "true");
            configureBootStrapProperties(server, properties);

            LibertyServer.setValidateApps(false);
            testHelper = new EJBAnnTestBaseHelper(server, client, true);
        } else {
            testHelper = new EJBAnnTestBaseHelper(server, client, false);
        }

        switch (serverName) {
            case Constants.SERVER_EJB:
            case Constants.SERVER_EJB_AUDIT:
                JACCFatUtils.transformApps(server, "ejbinstandalone.war", "securityejb.ear", "securityejbinwar.ear");
                break;
            case Constants.SERVER_EJB_BINDINGS:
                JACCFatUtils.transformApps(server, "securityejbInWarEarXML.ear", "securityejbXML.ear");
                break;
            case Constants.SERVER_EJBJAR:
            case Constants.SERVER_EJBJAR_AUDIT:
                JACCFatUtils.transformApps(server, "securityejbjar.ear");
                break;
            case Constants.SERVER_EJBJAR_INWAR:
                JACCFatUtils.transformApps(server, "ejbjarinstandaloneM02.war", "ejbjarinstandaloneM08.war", "ejbjarinstandaloneX02.war",
                                           "securityejbjarInWarEarM01.ear", "securityejbjarInWarEarM07.ear", "securityejbjarInWarEarX01.ear");
                break;
            case Constants.SERVER_EJBJAR_MC:
                JACCFatUtils.transformApps(server, "ejbjarinstandaloneMC06.war", "securityejbjarInWarEarMC06.ear", "securityejbjarMC.ear");
                break;
            case Constants.SERVER_EJBJAR_MERGE_BINDINGS:
                JACCFatUtils.transformApps(server, "securityejbjarInWarEarM07XMLmerge.ear", "securityejbjarXMLmerge.ear");
                break;
            case Constants.SERVER_EJB_MERGE_BINDINGS:
                JACCFatUtils.transformApps(server, "securityejbInWarEarXMLMerge.ear", "securityejbXMLmerge.ear");
                break;
            case Constants.SERVER_JACC_DYNAMIC:
                JACCFatUtils.transformApps(server, "securityejbinwar.ear");
                break;
        }

        testHelper.startServer(null, appName);
        // Wait for feature update to complete
        assertNotNull("FeatureManager did not report update was complete", server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Application did not start", server.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("LTPA configuration did not report it was ready", server.waitForStringInLog("CWWKS4105I"));
        if (server.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaccFeature(server);
        }
        Log.info(c, thisMethod, "***************CommonSetup Completed for test: " + classLog + " ***************");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "commonTearDown", "**Running Common tear down");
        if (server != null) {
            try {
                server.stopServer("SRVE9967W", // Quite a few
                                  "CWWKS9112W", // PureAnnServletToEJBRunAsTest, EJBJarMixM07ExtTest, EJBJarMixM08ExtTest, EJBJarMixM09ExtTest
                                  "CWWKS9405E", // EJBJarMixM07ExtTest, EJBJarMixM08ExtTest, EJBJarMixM09ExtTest
                                  "CNTR0338W", // Quite a few - Ambiguous EJB Binding
                                  "CWWKG0027W" // Occasional CWWKG0027W: Timeout while updating server configuration.
                );
            } finally {
                JACCFatUtils.uninstallJaccUserFeature(server);
            }
        }
    }

    @After
    public void resetConnection() {
        Log.info(c, "commonResetConnection", "Common reset connection");
        client.resetClientState();
    }

    protected static void verifyServerStartedWithJaccFeature(LibertyServer server) {
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog(MessageConstants.JACC_SERVICE_STARTING));
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog(MessageConstants.JACC_SERVICE_STARTED));
    }

    public void generateAccessDeniedResponseFromServlet(String queryString, String roleUser, String rolePwd) {
        assertTrue("403 not found in response", client.accessProtectedServletWithUnauthorizedCredentials(queryString, roleUser, rolePwd));

    }

    public String generateResponseFromServlet(String queryString, String roleUser, String rolePwd) {
        Log.info(c, "generateResponseFromServlet", "Performing accessing protected servlet....");
        return client.accessProtectedServletWithAuthorizedCredentials(queryString, roleUser, rolePwd);
    }

    protected void verifyResponseWithoutDeprecated(String response, String getCallerPrincipal, String isCallerInRoleManager, String isCallerInRoleEmployee, String isDeclaredRole) {
        verifyResponseWithoutDeprecated(response, getCallerPrincipal, isCallerInRoleManager, isCallerInRoleEmployee);
        mustContain(response, isDeclaredRole);
    }

    protected void verifyResponseWithoutDeprecated(String response, String getCallerPrincipal, String isCallerInRoleManager, String isCallerInRoleEmployee) {
        Log.info(c, "verifyResponseWithoutDeprecated", "Verify response = " + response);
        mustContain(response, getCallerPrincipal);
        mustContain(response, isCallerInRoleManager);
        mustContain(response, isCallerInRoleEmployee);
    }

    protected void verifyResponse(String response, String getCallerPrincipal, String getCallerIdentity, String isCallerInRoleManager, String isCallerInRoleEmployee) {
        Log.info(c, "verifyResponse", "Verify response = " + response);

        /*
         * EE9 removed getCallerIdentity() from SessionContext.
         */
        if (JakartaEEAction.isEE9OrLaterActive()) {
            verifyResponseWithoutDeprecated(response, getCallerPrincipal, isCallerInRoleManager, isCallerInRoleEmployee);
        } else {
            mustContain(response, getCallerPrincipal);
            mustContain(response, getCallerIdentity);
            mustContain(response, isCallerInRoleManager);
            mustContain(response, isCallerInRoleEmployee);
        }
    }

    private void mustContain(String response, String target) {
        assertTrue(target + " not found in response", response.contains(target));
    }

    protected void verifyResponse(String response, String getCallerPrincipal, String getCallerIdentity, String isCallerInRoleManager, String isCallerInRoleEmployee,
                                  String isDeclaredRole) {
        verifyResponse(response, getCallerPrincipal, getCallerIdentity, isCallerInRoleManager, isCallerInRoleEmployee);
        mustContain(response, isDeclaredRole);
    }

    protected void verifyException(String response, String exMsg, String msgTxt) {
        Log.info(c, "verifyException", "Verify response = " + response);
        assertTrue("Failed to find exception: EJBAccessException in response", response.contains(exMsg));
        assertTrue("Failed to CWWKS Message: " + msgTxt, response.contains(msgTxt));
    }

    protected void verifyExceptionWithMethod(String response, String exMsg, String msgTxt, String method) {
        Log.info(c, "verifyExceptionWithMethod", "Verify response = " + response);
        verifyException(response, exMsg, msgTxt);
        assertTrue("Failed to find method name " + method + " in authorization failed message", response.contains(method));
    }

    protected void verifyExceptionWithUserAndRole(String response, String exMsg, String msgTxt, String user, String method) {
        Log.info(c, "verifyExceptionWithUserAndRole", "Verify response = " + response);
        verifyException(response, exMsg, msgTxt);
        assertTrue("Failed to find user name " + user + " in authorization failed message", response.contains(user));
        assertTrue("Failed to find method " + method + " in authorization failed message for user not granted access ", response.contains(method));
    }
}