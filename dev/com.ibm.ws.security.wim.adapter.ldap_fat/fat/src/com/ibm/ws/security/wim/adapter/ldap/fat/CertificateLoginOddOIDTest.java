/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.ClientCertAuthClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class CertificateLoginOddOIDTest {
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.tds.certlogin");
    private static final Class<?> c = CertificateLoginOddOIDTest.class;
    private static ClientCertAuthClient client;

    protected final static String CLIENT_CERT_SERVLET = "ClientCertServlet";
    protected final static String ksPassword = "security";
    protected final static String AUTH_TYPE_CERT = "CLIENT_CERT";
    protected final static String user1CertFile = "LDAPUser1ExtraOID.jks";
    protected final static String employeeUser = "LDAPUser1";
    protected final static String DEFAULT_CONFIG_FILE = "clientcert.server.xml";
    protected static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(myServer);
        Log.info(c, "setUp", "Starting the server... ");
        myServer.addInstalledAppForValidation("clientcert");
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

    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        myServer.stopServer();
    }

    /**
     * This is an internal method used to setup the ClientCertAuthClient
     */
    private static ClientCertAuthClient setupClient(String certFile, boolean secure) {
        if (secure) {
            String ksFile = myServer.pathToAutoFVTTestFiles + File.separator + "clientcert" + File.separator + certFile;
            client = new ClientCertAuthClient(myServer.getHostname(), myServer.getHttpDefaultSecurePort(), true, myServer, CLIENT_CERT_SERVLET, "/clientcert", ksFile, ksPassword);
        } else {
            client = new ClientCertAuthClient(myServer.getHostname(), myServer.getHttpDefaultSecurePort(), false, myServer, CLIENT_CERT_SERVLET, "/clientcert", null, null);
        }
        return client;
    }

    public void verifyProgrammaticAPIValues(String loginUser, String test3, String authType) {
        // Verify programmatic APIs
        assertTrue("Failed to find expected getAuthType: " + loginUser, test3.contains("getAuthType: " + authType));
        assertTrue("Failed to find expected getRemoteUser: " + loginUser, test3.contains("getRemoteUser: " + loginUser));
        assertTrue("Failed to find expected getUserPrincipal: " + loginUser, test3.contains("getUserPrincipal: " + "WSPrincipal:" + loginUser));
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
            serverConfigurationFile = serverXML;
        }
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has: cn=LDAPUser1,o=IBM,c=US,EMAILADDRESS=badWolf@badwolf.com
     * <LI> Testing the forward port of PM76997 to Liberty
     * <LI> This test requires wim trace to be enabled. Specifically, LdapConfigManager needs to be traced.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> We should log. Trace should review that the EMAILADDRESS=badWolf@badwolf.com is listed correctly.
     * </OL>
     */
    @Test
    public void testClientCertWithEmailOID() throws Exception {
        String methodName = "testClientCert_SimpleCertificateFilter";
        Log.info(c, methodName, "Entering test " + methodName);

        // Use server.xml configuration with certificateMapMode="cn=${SubjectCN}"
        setServerConfiguration(DEFAULT_CONFIG_FILE);

        client = setupClient(user1CertFile, true);
        String response = client.access("/SimpleServlet", 200);
        verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);

        List<String> errMsgs = myServer.findStringsInLogsAndTrace("1.2.840.113549.1.9.1");
        assertTrue("Should not have getName version of email address for mapping certificate: " + errMsgs, errMsgs.size() < 3);

        errMsgs = myServer.findStringsInLogsAndTrace("CN=LDAPUser1,C=US,O=IBM,EMAILADDRESS=badWolf@badwolf.com");
        assertFalse("(WIM trace required for this test) Should have the toString with spaces removed version: " + errMsgs, errMsgs.isEmpty());

        Log.info(c, methodName, "Exiting test " + methodName);
    }

}
