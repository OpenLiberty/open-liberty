/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.javaeesec.fat;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.LocalLdapServer;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 *
 * The test verifies that Form Login is handled by the JASPI provider for both
 * positive and negative cases when the JASPI user feature is present in the server.xml and
 * the application web.xml contains <login-config> with <auth-method>FORM</auth-method>.
 *
 * The test access a protected servlet, verifies that the JASPI provider was invoked to
 * make the authentication decision and verifies that the servlet response contains the correct
 * values for getAuthType, getUserPrincipal and getRemoteUser after JASPI authentication.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class FormHttpAuthenticationMechanismTest extends JavaEESecTestBase {

    private static final String JAR_NAME = "JavaEESecBase.jar";

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    private static Class<?> logClass = FormHttpAuthenticationMechanismTest.class;
    private static LocalLdapServer ldapServer;
    private static String urlHttp;
    private static String urlHttps;

    private DefaultHttpClient httpclient;

    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
//    private static ServerConfiguration emptyConfiguration = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ldapServer = new LocalLdapServer();
        ldapServer.start();
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEEsecFormAuth.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.forward",
                                               "web.war.identitystores.ldap.ldap1", "web.war.identitystores.ldap");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEEsecFormAuthRedirect.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.redirect",
                                               "web.war.identitystores.ldap.ldap1", "web.war.identitystores.ldap");
        myServer.setServerConfigurationFile("form.xml");
        myServer.startServer(true);
        urlHttp = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
        urlHttps = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            if (ldapServer != null) {
                ldapServer.stop();
            }
        }
    }

    @SuppressWarnings("restriction")
    @Before
    public void setUp() {
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);
        httpclient = new DefaultHttpClient(httpParams);
        SSLHelper.establishSSLContext(httpclient, 0, myServer, null, null, null, null, null);
    }

    @After
    public void tearDown() {
        httpclient.getConnectionManager().shutdown();
    }

    @Rule
    public TestName name = new TestName();

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    public FormHttpAuthenticationMechanismTest() {
        super(myServer, logClass);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login with JASPI activated.
     * <LI> Login with a valid userId and password in the grantedgroup role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    @Test
    public void testJaspiFormLoginValidUserInRole_AllowedAccess() throws Exception {
        myServer.setMarkToEndOfLog();
        String response = executeGetRequestFormCreds(httpclient, urlHttp + Constants.DEFAULT_FORM_LOGIN_PAGE, false, urlHttp + "/JavaEEsecFormAuth/login.jsp",
                                                     "login page for the form login test", urlHttp + "/JavaEEsecFormAuth/j_security_check",
                                                     LocalLdapServer.USER1, LocalLdapServer.PASSWORD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login with JASPI activated.
     * <LI> Login with a valid userId and password in the grantedgroup role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Messages.log contains lines to show that JASPI authentication was processed on form display:
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    @Test
    public void testJaspiFormLoginValidUserInRoleRedirect_AllowedAccess() throws Exception {
        myServer.setMarkToEndOfLog();
        String response = executeGetRequestFormCreds(httpclient, urlHttp + Constants.DEFAULT_REDIRECT_FORM_LOGIN_PAGE, true, urlHttp + "/JavaEEsecFormAuthRedirect/login.jsp",
                                                     "login page for the form login test", urlHttp + "/JavaEEsecFormAuthRedirect/j_security_check",
                                                     LocalLdapServer.USER1, LocalLdapServer.PASSWORD, HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
    }
}
