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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.ClientCertAuthClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class CertificateLoginTest {
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.tds.certlogin");
    private static final Class<?> c = CertificateLoginTest.class;
    private static ClientCertAuthClient client;

    protected final static String CLIENT_CERT_SERVLET = "ClientCertServlet";
    protected final static String ksPassword = "security";
    protected final static String AUTH_TYPE_CERT = "CLIENT_CERT";
    protected final static String user1CertFile = "LDAPUser1.jks";
    protected final static String user2CertFile = "LDAPUser2.jks";
    protected final static String user5CertFile = "LDAPUser5.jks";
    protected final static String employeeUser = "LDAPUser1";
    protected final static String user1InvalidCertFile = "LDAPUser1Invalid.jks";
    protected final static String EXACT_DN_SERVER_XML = "clientCertExactDN.xml";
    protected final static String DEFAULT_CONFIG_FILE = "clientcert.server.xml";
    protected final static String INVALID_FILTER_SERVER_XML = "clientCertInvalidFilter.xml";
    protected final static String COMPLEX_FILTERS_SERVER_XML = "clientCertComplexFilter.xml";
    protected final static String NON_MATCHING_FILTER_SERVER_XML = "clientCertNonMatchingFilter.xml";
    protected final static String MULTIPLE_LDAP_SERVER_XML = "clientCertMultipleLDAP.xml";
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
     * <LI> to access the servlet. Certificate has full dn: cn=LDAPUser1,o=IBM,c=US
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_SimpleCertificateFilter() throws Exception {
        String methodName = "testClientCert_SimpleCertificateFilter";
        Log.info(c, methodName, "Entering test " + methodName);

        // Use server.xml configuration with certificateMapMode="cn=${SubjectCN}"
        setServerConfiguration(DEFAULT_CONFIG_FILE);

        client = setupClient(user1CertFile, true);
        String response = client.access("/SimpleServlet", 200);
        verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);

        Log.info(c, methodName, "Exiting test " + methodName);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has full dn: cn=LDAPUser1,o=IBM,c=US
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_ExactDN() throws Exception {
        String methodName = "testClientCert_ExactDN";
        Log.info(c, methodName, "Entering test " + methodName);

        // Use server.xml configuration with certificateMapMode="EXACT_DN"
        setServerConfiguration(EXACT_DN_SERVER_XML);

        client = setupClient(user1CertFile, true);
        String response = client.access("/SimpleServlet", 200);
        verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);

        Log.info(c, methodName, "Exiting test " + methodName);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has cn only: cn=LDAPUser2
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    @AllowedFFDC(value = { "com.ibm.ws.security.registry.RegistryException" })
    public void testClientCert_ExactDNWithCNOnly() throws Exception {
        String methodName = "testClientCert_ExactDNWithCNOnly";
        Log.info(c, methodName, "Entering test " + methodName);

        // Use server.xml configuration with certificateMapMode="EXACT_DN"
        setServerConfiguration(EXACT_DN_SERVER_XML);

        client = setupClient(user2CertFile, true);
        client.access("/SimpleServlet", 401);
        //myServer.waitForStringInLogUsingMark(".*CWWKS1102E:.*"); //CWWKS1102E: CLIENT-CERT Authentication did not succeed for the client certificate with dn CN=LDAPUser2. An internal exception occurred: CWIML4537E: The login operation could not be completed. The specified principal name extracted from certificate is not found in the back-end repository..
        //myServer.waitForStringInLogUsingMark(".*CWIML4537E:.*");
        Log.info(c, methodName, "Exiting test " + methodName);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user (LDAPUser5) that is trusted by the server but NOT authorized
     * <LI> to access the servlet
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> The unauthorized user should get a 403 returned.
     * </OL>
     */
    @Test
    public void testClientCert_CertificateNotAuthorized() throws Exception {
        String methodName = "testClientCert_CertificateNotAuthorized";
        Log.info(c, methodName, "Entering test " + methodName);

        setServerConfiguration(DEFAULT_CONFIG_FILE);

        client = setupClient(user5CertFile, true);
        client.access("/SimpleServlet", 403);

        Log.info(c, methodName, "Exiting test " + methodName);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. certificateFilter="uid=${invalid}"
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is sent to the protected servlet, but since the certificateFilter is invalid in server.xml,
     * <LI> 403 should be returned
     * </OL>
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    public void testClientCert_InvalidFilter() throws Exception {
        String methodName = "testClientCert_InvalidFilter";
        Log.info(c, methodName, "Entering test " + methodName);

        // Use server.xml configuration with certificateFilter="uid=${invalid}"
        setServerConfiguration(INVALID_FILTER_SERVER_XML);

        client = setupClient(user1CertFile, true);
        client.access("/SimpleServlet", 403);
        //myServer.waitForStringInLogUsingMark(".*CWWKS1102E:.*"); // CWWKS1102E: CLIENT-CERT Authentication did not succeed for the client certificate with dn CN=LDAPUser1,O=IBM,C=US. An internal exception occurred: com.ibm.wsspi.security.wim.exception.CertificateMapperException: CWIML0009E: The login operation could not be completed. An unknown certificate attribute ${invalid} was used in the filter specification. Specify a supported certificate filter..
        //myServer.waitForStringInLogUsingMark(".*CWIML0009E:.*");
        Log.info(c, methodName, "Exiting test " + methodName);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has full dn: cn=LDAPUser1,o=IBM,c=US
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_ComplexFilters() throws Exception {
        String methodName = "testClientCert_ComplexFilters";
        Log.info(c, methodName, "Entering test " + methodName);

        // Use server.xml configuration with certificateFilter="(&amp;(uid=${SubjectCN})(initials=${SubjectCN})(cn=${IssuerCN}))"
        setServerConfiguration(COMPLEX_FILTERS_SERVER_XML);

        client = setupClient(user1CertFile, true);
        String response = client.access("/SimpleServlet", 200);
        verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);

        Log.info(c, methodName, "Exiting test " + methodName);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has full dn with invalid o: cn=LDAPUser1,o=INVALID,c=US
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> 403 should be returned since the certificate dn is invalid
     * </OL>
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    public void testClientCert_InvalidDNInCertAndValidFilter() throws Exception {
        String methodName = "testClientCert_InvalidDNInCertAndValidFilter";
        Log.info(c, methodName, "Entering test " + methodName);

        // Use server.xml configuration with certificateFilter="uid=${SubjectO}"
        setServerConfiguration(NON_MATCHING_FILTER_SERVER_XML);

        client = setupClient(user1InvalidCertFile, true);
        client.access("/SimpleServlet", 403);
        myServer.waitForStringInLog("CWIML4537E:", 2000);
        Log.info(c, methodName, "Exiting test " + methodName);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has full dn: cn=LDAPUser1,o=IBM,c=US
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> 403 should be returned since the user LDAPUser1 exists in both LDAPs
     * </OL>
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    public void testClientCert_MultipleLDAPServers() throws Exception {
        String methodName = "testClientCert_MultipleLDAPServers";
        Log.info(c, methodName, "Entering test " + methodName);

        // Use server.xml configuration with multiple LDAPs configured. TDS and AD server with respective certificate filters
        setServerConfiguration(MULTIPLE_LDAP_SERVER_XML);

        client = setupClient(user1CertFile, true);
        client.access("/SimpleServlet", 403);
        myServer.waitForStringInLog("CWIML4538E:"); // com.ibm.websphere.wim.exception.DuplicateLogonIdException: CWIML4538E: The user registry operation could not be completed. More than one record exists for the null principal name in the configured user registries. The principal name must be unique across all the user registries.

        Log.info(c, methodName, "Exiting test " + methodName);
    }

}
