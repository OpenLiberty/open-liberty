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

/**
 * Note:
 * 1. User registry:
 * At this time, the test uses users, passwords, roles, and groups predefined in server.xml as
 * test user registry.
 *
 * TODO:  use different user registry
 *
 * 2. The constraints (which servlets can be accessed by which user/group/role) are defined in web.xml
 *
 * 3. Note on *Overlap* test:
 * When there are more than one constraints applied to the same servlet, the least constraint will win,
 * e.g.,
 *   <auth-constraint id="AuthConstraint_5">
 <role-name>Employee</role-name>
 </auth-constraint>

 and

 <security-constraint id="SecurityConstraint_5">
 <web-resource-collection id="WebResourceCollection_5">
 <web-resource-name>Protected with overlapping * and Employee roles</web-resource-name>
 <url-pattern>/OverlapNoConstraintServlet</url-pattern>
 <http-method>GET</http-method>
 <http-method>POST</http-method>
 </web-resource-collection>
 <auth-constraint id="AuthConstraint_5">
 <role-name>*</role-name>
 </auth-constraint>
 </security-constraint>

 servlet OverlapNoConstraintServlet will allow access to all roles since
 the role = * (any role) and role =  Employee are combined and * will win.

 */

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
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
@Mode(TestMode.FULL)
public class ClientCertTest {

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.clientcert");
    private final static Class<?> logClass = ClientCertTest.class;
    private static CommonTestHelper testHelper = new CommonTestHelper();
    private static ClientCertAuthClient client;

    protected final static String CLIENT_CERT_SERVLET = "ClientCertServlet";
    protected final static String ksPassword = "security";
    protected final static String AUTH_TYPE_CERT = "CLIENT_CERT";
    protected final static String AUTH_TYPE_BASIC = "BASIC";
    protected final static String AUTH_TYPE_FORM = "FORM";
    protected final static String employeeUser = "LDAPUser1";
    protected final static String employeePassword = "security";
    protected final static String managerUser = "LDAPUser2";
    protected final static String user1CertFile = "LDAPUser1.jks";
    protected final static String user1InvalidCertFile = "LDAPUser1Invalid.jks";
    protected final static String user2CertFile = "LDAPUser2.jks";
    protected final static String user5CertFile = "LDAPUser5.jks";
    protected final static String invalidUserCertFile = "invalidUser.jks";
    protected final static String user1ADCertFile = "LDAPUser1AD.jks";
    protected final static String DEFAULT_CONFIG_FILE = "clientcert.server.orig.xml";
    protected final static String EXACT_DN_SERVER_XML = "clientCertExactDN.xml";
    protected final static String NO_FAILOVER_SERVER_XML = "clientCertNoFailover.xml";
    protected final static String INITIALS_FILTER_SERVER_XML = "clientCertInitialsFilter.xml";
    protected final static String MULTIPLE_FILTERS_SERVER_XML = "clientCertMultipleFilters.xml";
    protected final static String INVALID_DN_SERVER_XML = "clientCertInvalidDN.xml";
    protected final static String INVALID_FILTER_SERVER_XML = "clientCertInvalidFilter.xml";
    protected final static String AD_SERVER_FILTER_XML = "clientCertADWithFilter.xml";
    protected final static String AD_SERVER_EXACT_DN_XML = "clientCertADWithExactDN.xml";
    protected final static String AD_MULTIPLE_FILTERS_SERVER_XML = "clientCertADMultipleFilters.xml";
    protected final static String AD_ISSUER_CN_SERVER_XML = "clientCertADIssuerCN.xml";
    protected final static String AD_SUBJECT_DN_SERVER_XML = "clientCertADSubjectDN.xml";
    protected final static String AD_SERIAL_NUMBER_SERVER_XML = "clientCertADSerialNumber.xml";
    protected static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    protected final static String FAIL_OVER_AUTH_METHOD_BASIC = "clientcertFailoverAuthMethodBasic.xml";
    protected final static String FAIL_OVER_AUTH_METHOD_BASIC2 = "clientcertFailoverAuthMethodBasic2.xml";
    protected final static String FAIL_OVER_AUTH_METHOD_FORM = "clientcertFailoverAuthMethodForm.xml";

    /*
     * This is a the DER-formatted client certificate which will be used in the WSCC header
     * To generate this header using user LDAPUser2 in LDAPUser2.jks (provided):
     * # export certificate as DER:
     * keytool -export -alias LDAPUser2 -keystore LDAPUser2.jks -storepass security -file LDAPUser2.crt.der
     * # convert DER certificate to PEM:
     * openssl x509 -in LDAPUser5.crt.der -inform DER -out LDAPUser5.crt.pem -outform PEM
     */
    protected final static String user2CertWSCCHeader = "MIICYjCCAiCgAwIBAgIET9ZYzjALBgcqhkjOOAQDBQAwFDESMBAGA1UEAxMJTERBUFVzZXIyMB4XDTEyMDYxMTIwNDU"
                                                        + "wMloXDTIyMDYwOTIwNDUwMlowFDESMBAGA1UEAxMJTERBUFVzZXIyMIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/"
                                                        + "U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClp"
                                                        + "J+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjx"
                                                        + "UjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLR"
                                                        + "JFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKL"
                                                        + "Zl6Ae1UlZAFMO/7PSSoDgYUAAoGBALGljs2ahzYbSbeCaltBy+krGm4jNLlwvS81Zpgjqh2A15fHGTAvvrGArk36I"
                                                        + "Q3jycONKUeEeyrlDGyJlrB+YZ6sFZXYbhsAPeLbIIthU1V7ahRuZtOavQao+nGV7gbGUb410e5iuwx8NHIEzhwH0l"
                                                        + "C4CDPRuHGaRXaXReKSOLbgMAsGByqGSM44BAMFAAMvADAsAhR4JFtZj3PkGBEuIgC19XhRY67phwIUIg1xfWb7gb2"
                                                        + "maTnxH/EPUulzDsE=";
    protected final static String user2CertInvalidWSCCHeader = "MIICYjCCAiCgAwIBAgIET9ZYzjALBgcqhkjOOAQDBQAwFDESMBAGA1UEAxMJTERBUFVzZXIyMB4XDTEyMDYxMTIwNDU"
                                                               + "wMloXDTIyMDYwOTIwNDUwMlowFDESMBAGA1UEAxMJTERBUFVzZXIyMIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/"
                                                               + "U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClp"
                                                               + "J+f6AR7ECLCT7up1/64xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjx"
                                                               + "UjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLR"
                                                               + "JFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKL"
                                                               + "Zl6Ae1UlZAGMO/7PSSoDgYUAAoGBALGljs2ahzYbSbeCaltBy+krGm4jNLlwvS81Zpgjqh2A15fHGTAvvrGArk36I"
                                                               + "Q3jycONKUeEeyrlDGyJlrB+YZ6sFZXYbhsAPeLbIIthU1V7ahRuZtOavQao+nGV7gbGUb410e5iuwx8NHIEzhwH0l"
                                                               + "C4CDPRuHGaRXaXReKSOLbgMAsGByqSSM44BAMFAAMvADAsAhR4JFtZj3PkGBEuIgC19XhRY67phwIUIg1xfWb7gb2"
                                                               + "maTnxH/EPUulzDsE=";
    protected final static String user5CertWSCCHeader = "MIICmDCCAlagAwIBAgIETtVkXzALBgcqhkjOOAQDBQAwLzELMAkGA1UEBhMCVVMxDDAKBgNVBAoTA0lCTTESMBAGA1UEAxMJTERBUFVzZXI"
                                                        + "1MB4XDTExMTEyOTIzMDE1MVoXDTIxMTEyNjIzMDE1MVowLzELMAkGA1UEBhMCVVMxDDAKBgNVBAoTA0lCTTESMBAGA1UEAxMJTERBUFVz"
                                                        + "ZXI1MIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm"
                                                        + "1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAh"
                                                        + "UAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6Ew"
                                                        + "oFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYUA"
                                                        + "AoGBANqAPGZDtRgdQTvwFXGZaiNYMOuiQAJHLX97tTeO/N76PZNPQTI0+DTeJ82E1n62VI6qjZYhNVOS8pZyPinve2wOyHV8jRQgkXXui"
                                                        + "A8r/C/WMXXwtkRKQEr4CxUY7vqSpEXVMIkUoZGESHUAc1TSk4RuP5DoW5WjfAT2CzCcRd2nMAsGByqGSM44BAMFAAMvADAsAhQ51XvpJt"
                                                        + "/6xVd80F7A39E7woLAUQIUDB29n05fxwKd/3kjae8oX9pi7Iw=";

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(logClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(logClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };

    @BeforeClass
    public static void setUp() throws Exception {
        LDAPUtils.addLDAPVariables(myServer);

        JACCFatUtils.installJaccUserFeature(myServer);
        JACCFatUtils.transformApps(myServer, "clientcert.war");

        myServer.addInstalledAppForValidation("clientcert");
        myServer.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));
        assertNotNull("JACC feature did not report it was starting", myServer.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", myServer.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen
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
    public void testClientCert_FullDNCertificate() throws Exception {
        setServerConfiguration(DEFAULT_CONFIG_FILE);
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));

        client = setupClient(user1CertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import an invalid certificate and use in conjunction with a valid $WSCC header
     * <LI> to access the servlet. Certificate has only cn: cn=LDAPUser2
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_ValidCertificate_WSCC() throws Exception {
        setServerConfiguration(DEFAULT_CONFIG_FILE);
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));

        // Use an invalid user certificate so we know the WSCC header is in use
        client = setupClient(invalidUserCertFile, true);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("$WSCC", user2CertWSCCHeader);
        String response = client.accessProtectedServletWithValidHeaders("/SimpleServlet", headers, false);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, AUTH_TYPE_CERT);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import an invalid certificate and use in conjunction with an invalid $WSCC header
     * <LI> (bad algorithm) to access the servlet. Certificate has only cn: cn=LDAPUser2
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> There should be a signature algorithm mismatch exception, with a 403 returned.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "java.security.cert.CertificateException" })
    public void testClientCert_InvalidCertificateAlgorithm_WSCC() throws Exception {
        setServerConfiguration(DEFAULT_CONFIG_FILE);
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));

        // Use an invalid user certificate so we know the WSCC header is in use
        client = setupClient(invalidUserCertFile, true);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("$WSCC", user2CertInvalidWSCCHeader);
        client.accessProtectedServletWithInvalidHeaders("/SimpleServlet", headers, false, 401);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert via the $WSCC header.
     * <LI> Import an invalid certificate and use in conjunction with an invalid $WSCC header
     * <LI> containing LDAPUser5, who is trusted by the server but NOT authorized
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> The invalid user should get a 403 returned.
     * </OL>
     */
    @Test
    public void testClientCert_CertificateNotAuthorized_WSCC() throws Exception {
        setServerConfiguration(DEFAULT_CONFIG_FILE);
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));

        // Use an invalid user certificate so we know the WSCC header is in use
        client = setupClient(invalidUserCertFile, true);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("$WSCC", user5CertWSCCHeader);
        client.accessProtectedServletWithInvalidHeaders("/SimpleServlet", headers, false, 403);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has only cn: cn=LDAPUser2
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_CNOnlyCertificate() throws Exception {
        setServerConfiguration(DEFAULT_CONFIG_FILE);
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));

        client = setupClient(user2CertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, AUTH_TYPE_CERT);
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
        setServerConfiguration(DEFAULT_CONFIG_FILE);
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));

        client = setupClient(user5CertFile, true);
        client.access("/SimpleServlet", 403);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import an invalid user in certificate that does not exist in the registry
     * <LI> Server.xml has allowFailOverToBasicAuth="true"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> The invalid user certificate should get a 401 returned and failover to basic auth
     * <LI> We can then log into the basic auth prompt with a valid user and password
     * </OL>
     */
    @Test
    public void testClientCert_InvalidUserInCertificate_allowFailOverToBasicAuth() throws Exception {
        setServerConfiguration(DEFAULT_CONFIG_FILE);
        assertNotNull("We need to wait for the SSL port to be open",
                      myServer.waitForStringInLog("CWWKO0219I:.*defaultHttpEndpoint-ssl"));

        client = setupClient(invalidUserCertFile, true);
        client.access("/SimpleServlet", 401);
        String url = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort() + "/clientcert/SimpleServlet";
        String response = client.accessAndAuthenticate(url, employeeUser, employeePassword, 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_BASIC);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import an invalid user in certificate that does not exist in the registry
     * <LI> Server.xml has allowFailOverToAuthMethod="BASIC"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> The invalid user certificate should get a 401 returned and failover to basic auth
     * <LI> We can then log into the basic auth prompt with a valid user and password
     * </OL>
     */
    @Test
    public void testClientCert_InvalidUserInCertificate_allowFailOverToAuthMethod_basicAuth() throws Exception {
        setServerConfiguration(FAIL_OVER_AUTH_METHOD_BASIC);

        client = setupClient(invalidUserCertFile, true);
        client.access("/SimpleServlet", 401);
        String url = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort() + "/clientcert/SimpleServlet";
        String response = client.accessAndAuthenticate(url, employeeUser, employeePassword, 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_BASIC);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import an invalid user in certificate that does not exist in the registry
     * <LI> Server.xml has allowAuthenticationFailOverToAuthMethod="BASIC"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> The invalid user certificate should get a 401 returned and failover to basic auth
     * <LI> We can then log into the basic auth prompt with a valid user and password
     * </OL>
     */
    @Test
    public void testClientCert_InvalidUserInCertificate_allowFailOverToAuthMethod_basicAuth_newProp() throws Exception {
        setServerConfiguration(FAIL_OVER_AUTH_METHOD_BASIC2);

        client = setupClient(invalidUserCertFile, true);
        client.access("/SimpleServlet", 401);
        String url = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort() + "/clientcert/SimpleServlet";
        String response = client.accessAndAuthenticate(url, employeeUser, employeePassword, 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_BASIC);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import an invalid user in certificate that does not exist in the registry
     * <LI> Server.xml has allowAuthenticationFailOverToAuthMethod="FORM"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> The invalid user certificate failed so it should fallback to form login page
     * </OL>
     */
    @Test
    public void testClientCert_InvalidUserInCertificate_allowFailOverToAuthMethod_formLogin() throws Exception {
        setServerConfiguration(FAIL_OVER_AUTH_METHOD_FORM);
        client = setupClient(invalidUserCertFile, true);
        String url = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort() + "/clientcert/SimpleServlet";
        client.certAuthFailOverToFormLogin(url);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Don't import any certificate to the client then access the servlet
     * <LI> Server.xml has allowFailOverToBasicAuth="true"
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> With no certificate the client should get a 401 returned and failover to basic auth
     * <LI> We can then log into the basic auth prompt with a valid user and password
     * </OL>
     */
    @Test
    public void testClientCert_NoCertificate() throws Exception {
        setServerConfiguration(DEFAULT_CONFIG_FILE);

        client = setupClient(null, false);
        String url = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/clientcert/SimpleServlet";
        String response = client.accessAndAuthenticate(url, employeeUser, employeePassword, 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_BASIC);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import an invalid user in certificate that is not in the registry
     * <LI> Server.xml has allowFailOverToBasicAuth="false"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> The invalid certificate user should get a 403 returned since there is no failover
     * <LI> to basic auth and we use unauthenticated subject
     * </OL>
     */
    @Test
    public void testClientCert_InvalidUserInCertificateNoFailover() throws Exception {
        // Use server.xml configuration with allowFailOverToBasicAuth="false"
        setServerConfiguration(NO_FAILOVER_SERVER_XML);

        client = setupClient(invalidUserCertFile, true);
        client.access("/SimpleServlet", 403);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Don't import any certificate to the client then access the servlet
     * <LI> Server.xml has allowFailOverToBasicAuth="false"
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> The no certificate should get a 403 returned since there is no failover
     * <LI> to basic auth and we use unauthenticated subject
     * </OL>
     */
    @Test
    public void testClientCert_NoCertificateNoFailover() throws Exception {
        // Use server.xml configuration with allowFailOverToBasicAuth="false"
        setServerConfiguration(NO_FAILOVER_SERVER_XML);

        client = setupClient(null, false);
        String url = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/clientcert/SimpleServlet";
        client.accessAndAuthenticate(url, employeeUser, employeePassword, 403);
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
        // Use server.xml configuration with certificateMapMode="EXACT_DN"
        setServerConfiguration(EXACT_DN_SERVER_XML);

        client = setupClient(user1CertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);
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
    @AllowedFFDC({ "com.ibm.ws.security.registry.RegistryException",
                   "com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException",
                   "javax.naming.NameNotFoundException",
                   "com.ibm.ws.security.registry.CertificateMapFailedException" })
    public void testClientCert_ExactDNWithCNOnly() throws Exception {
        // Use server.xml configuration with certificateMapMode="EXACT_DN"
        setServerConfiguration(EXACT_DN_SERVER_XML);

        client = setupClient(user2CertFile, true);
        client.access("/SimpleServlet", 401);
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
    public void testClientCert_InitialsFilter() throws Exception {
        // Use server.xml configuration with certificateFilter="initials=${SubjectCN}"
        setServerConfiguration(INITIALS_FILTER_SERVER_XML);

        client = setupClient(user1CertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);
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
    public void testClientCert_MultipleFilters() throws Exception {
        // Use server.xml configuration with certificateFilter="(&(uid=${SubjectCN})(initials=${SubjectCN})(objectclass=ePerson))"
        setServerConfiguration(MULTIPLE_FILTERS_SERVER_XML);

        client = setupClient(user1CertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);
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
     * <LI> 401 should be returned since the certificate dn is invalid
     * </OL>
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.security.registry.RegistryException",
                   "com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException" })
    public void testClientCert_InvalidDNWithFilter() throws Exception {
        // Use server.xml configuration with certificateFilter="uid=${SubjectO}"
        setServerConfiguration(INVALID_DN_SERVER_XML);

        client = setupClient(user1InvalidCertFile, true);
        client.access("/SimpleServlet", 401);
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
     * <LI> 401 should be returned since the certificate dn is invalid
     * </OL>
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.security.registry.RegistryException",
                   "com.ibm.wsspi.security.wim.exception.PasswordCheckFailedException",
                   "javax.naming.NameNotFoundException",
                   "com.ibm.ws.security.registry.CertificateMapFailedException" })
    public void testClientCert_InvalidDNWithExactDN() throws Exception {
        // Use server.xml configuration with certificateMapMode="EXACT_DN"
        setServerConfiguration(EXACT_DN_SERVER_XML);

        client = setupClient(user1InvalidCertFile, true);
        client.access("/SimpleServlet", 401);
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
     * <LI> 401 should be returned
     * </OL>
     */
    @Test
    @AllowedFFDC({ "com.ibm.wsspi.security.wim.exception.WIMException",
                   "com.ibm.ws.security.registry.RegistryException",
                   "com.ibm.wsspi.security.wim.exception.CertificateMapperException" })
    public void testClientCert_InvalidFilter() throws Exception {
        // Use server.xml configuration with certificateFilter="uid=${invalid}"
        setServerConfiguration(INVALID_FILTER_SERVER_XML);

        client = setupClient(user1CertFile, true);
        client.access("/SimpleServlet", 401);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has full dn: cn=LDAPUser1,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_ActiveDirectoryWithFilter() throws Exception {
        // Use server.xml configuration with Active Directory and certificateFilter="sAMAccountName=${SubjectCN}"
        setServerConfiguration(AD_SERVER_FILTER_XML);

        client = setupClient(user1ADCertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has full dn: cn=LDAPUser1,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_ActiveDirectoryWithExactDN() throws Exception {
        // Use server.xml configuration with Active Directory and certificateMapMode="EXACT_DN"
        setServerConfiguration(AD_SERVER_EXACT_DN_XML);

        client = setupClient(user1ADCertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate dn has only cn: cn=LDAPUser2
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "com.ibm.wsspi.security.wim.exception.WIMSystemException",
                   "com.ibm.wsspi.security.wim.exception.WIMException",
                   "com.ibm.ws.security.registry.RegistryException",
                   "javax.naming.NamingException" })
    public void testClientCert_ActiveDirectoryWithExactDNWithCNOnly() throws Exception {
        // Use server.xml configuration with Active Directory and certificateMapMode="EXACT_DN"
        setServerConfiguration(AD_SERVER_EXACT_DN_XML);

        client = setupClient(user2CertFile, true);
        client.access("/SimpleServlet", 401);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has full dn: cn=LDAPUser1,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_ActiveDirectoryMultipleFilters() throws Exception {
        // Use server.xml configuration with certificateFilter="(&(uid=${SubjectCN})(displayName=${SubjectCN}))"
        setServerConfiguration(AD_MULTIPLE_FILTERS_SERVER_XML);

        client = setupClient(user1ADCertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. certificateFilter="distinguishedName=${SubjectDN}"
     * <LI> AD returns "distinguishedName=CN=LDAPUser1,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM"
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_ActiveDirectorySubjectDN() throws Exception {
        // Use server.xml configuration with certificateFilter="distinguishedName=${SubjectDN}"
        setServerConfiguration(AD_SUBJECT_DN_SERVER_XML);

        client = setupClient(user1ADCertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has full dn: cn=LDAPUser1,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com
     * <LI> Certificate returns "Issuer: CN=LDAPUser1, CN=users, DC=secfvt2, DC=austin, DC=ibm, DC=com" and AD returns "displayName=LDAPUser1"
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_ActiveDirectoryIssuerCN() throws Exception {
        // Use server.xml configuration with certificateFilter="displayName=${IssuerCN}"
        setServerConfiguration(AD_ISSUER_CN_SERVER_XML);

        client = setupClient(user1ADCertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for client cert.
     * <LI> Import a certificate with a user that is trusted by the server and authorized
     * <LI> to access the servlet. Certificate has full dn: cn=LDAPUser1,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com
     * <LI> Certificate returns "Serial number: 1339601470" and AD has this set for LDAPUser1: "description=1339601470"
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user certificate is permitted access to the protected servlet.
     * </OL>
     */
    @Test
    public void testClientCert_ActiveDirectorySerialNumber() throws Exception {
        // Use server.xml configuration with certificateFilter="description=${SerialNumber}"
        setServerConfiguration(AD_SERIAL_NUMBER_SERVER_XML);

        client = setupClient(user1ADCertFile, true);
        String response = client.access("/SimpleServlet", 200);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, AUTH_TYPE_CERT);
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
        client.setJaccValidation(true);
        return client;
    }

    /**
     * This is an internal method used to set the server.xml
     */
    private static void setServerConfiguration(String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Update server.xml
            Log.info(logClass, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
            myServer.setMarkToEndOfLog();
            myServer.setServerConfigurationFile("/" + serverXML);
            Log.info(logClass, "setServerConfiguration",
                     "waitForStringInLogUsingMark: CWWKG0017I: The server configuration was successfully updated.");
            myServer.waitForStringInLogUsingMark("CWWKG0017I");
            serverConfigurationFile = serverXML;
        }
    }

}
