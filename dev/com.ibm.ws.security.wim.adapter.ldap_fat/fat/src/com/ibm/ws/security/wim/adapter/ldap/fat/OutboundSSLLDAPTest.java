/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class OutboundSSLLDAPTest {
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.outbound.ssl");
    private static final Class<?> c = OutboundSSLLDAPTest.class;

    protected final static String BASIC_AUTH_SERVLET = "basicauth";
    protected final static String LDAP_DEFAULT_OUTBOUND_SSL = "LDAPwithDefaultOutboundSSL.xml";
    protected final static String LDAP_OUTBOUND_FILTER = "LDAPwithDynamicOutboundSSL.xml";
    protected final static String DEFAULT_CONFIG_FILE = "outboundSSL.server.xml";

    private static final boolean IS_MANAGER_ROLE = true;
    private static final boolean NOT_EMPLOYEE_ROLE = false;
    private static final String ldapManagerUser = "LDAPUser2";
    private static final String ldapManagerPassword = "security";
    protected static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    @Rule
    public TestName name = new TestName();

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @Before
    public void setUp() throws Exception {
        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(myServer);
        Log.info(c, "setUp", "Starting the server... ");
        myServer.addInstalledAppForValidation("basicauth");
        myServer.setServerConfigurationFile("/" + DEFAULT_CONFIG_FILE);
        myServer.startServer(true);

        //Make sure the application has come up before proceeding
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));
        assertNotNull("Server did not came up",
                      myServer.waitForStringInLog("CWWKF0011I"));

        //We are expecting SSL errors on the server startup since, we should not trust the LDAP server
        //with the configuration used while starting the server.
        assertNotNull("Did not get the expect Handshake error from LDAP",
                      myServer.waitForStringInLog("FFDC1015I:"));

    }

    @After
    public void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        myServer.stopServer("CWPKI0022E:", "CWPKI0823E:", "CWPKI0815W", "CWPKI0063W");
    }

    /**
     * This is an internal method used to set the server.xml
     */
    private static void setServerConfiguration(String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            //Set mark
            myServer.setMarkToEndOfLog(myServer.getDefaultLogFile());

            // Update server.xml
            Log.info(c, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
            myServer.setServerConfigurationFile("/" + serverXML);
            Log.info(c, "setServerConfiguration",
                     "waitForStringInLogUsingMark: CWWKG0017I: The server configuration was successfully updated.");
            myServer.waitForStringInLogUsingMark("CWWKG0017I");
            myServer.waitForStringInLogUsingMark("CWWKO0219I:.*defaultHttpEndpoint-ssl");

            serverConfigurationFile = serverXML;
        }
    }

    /*
     * Test to make sure LDAP server can be accessed using default outbound SSL configuration.
     * Accessing a protected servlet doing a login with LDAP user registry configured with SSL.
     * Test to make sure the correct SSL default outbound configuration is used. If we can
     * successfully login then LDAP is using the correct configuration.
     */
    @Test
    @AllowedFFDC(value = { "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "javax.naming.CommunicationException" })
    public void testLDAPUsingDefaultOutboundSSLConfiguration() throws Exception {
        Log.info(c, name.getMethodName(), "Entering " + name.getMethodName());

        setServerConfiguration(LDAP_DEFAULT_OUTBOUND_SSL);

        // Hit the servlet and verify we can access
        // if we can successfully access it then that means the server has SSL access to LDAP
        SSLBasicAuthClient sslClient = new SSLBasicAuthClient(myServer, SSLBasicAuthClient.DEFAULT_REALM, SSLBasicAuthClient.DEFAULT_SERVLET_NAME, SSLBasicAuthClient.DEFAULT_CONTEXT_ROOT, null, null, null, null, "SSL");

        String response = sslClient.accessProtectedServletWithAuthorizedCredentials(SSLBasicAuthClient.PROTECTED_MANAGER_ROLE, ldapManagerUser, ldapManagerPassword);
        assertTrue("Did not get the expected response",
                   sslClient.verifyResponse(response, ldapManagerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));

        Log.info(c, name.getMethodName(), "Exiting " + name.getMethodName());
    }

    /*
     * Test to make sure LDAP server can be accessed using dynamic outbound SSL configuration.
     * Accessing a protected servlet doing a login with LDAP user registry configured with SSL.
     * Test to make sure the correct SSL outbound filter configuration is used. If we can
     * successfully login then LDAP is using the correct configuration.
     */
    @Test
    @AllowedFFDC(value = { "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "javax.naming.CommunicationException" })
    public void testLDAPUsingDynamicOutboundSSLConfiguration() throws Exception {
        // This test can only run with real LDAP servers, otherwise we set the primary and backup LDAP to the same host/port and that's an invalid outbound SSL config.
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);

        Log.info(c, name.getMethodName(), "Entering " + name.getMethodName());

        setServerConfiguration(LDAP_OUTBOUND_FILTER);

        // Hit the servlet and verify we can access
        // if we can successfully access it then that means the server has SSL access to LDAP
        SSLBasicAuthClient sslClient = new SSLBasicAuthClient(myServer, SSLBasicAuthClient.DEFAULT_REALM, SSLBasicAuthClient.DEFAULT_SERVLET_NAME, SSLBasicAuthClient.DEFAULT_CONTEXT_ROOT, null, null, null, null, "SSL");

        String response = sslClient.accessProtectedServletWithAuthorizedCredentials(SSLBasicAuthClient.PROTECTED_MANAGER_ROLE, ldapManagerUser, ldapManagerPassword);
        assertTrue("Did not get the expected response",
                   sslClient.verifyResponse(response, ldapManagerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));

        Log.info(c, name.getMethodName(), "Exiting " + name.getMethodName());
    }

}
