/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.javaeesec.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
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
import com.ibm.ws.security.javaeesec.fat_helper.ServerHelper;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@MinimumJavaLevel(javaLevel = 1.8)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class BasicAuthenticationMechanismTest extends JavaEESecTestBase {

    private static final String COOKIE_NAME = "jaspicSession";
    private static final String JAR_NAME = "JavaEESecBase.jar";
    private static final String queryString = "/JavaEESecBasicAuthServlet/JavaEESecBasic";

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    private static Class<?> logClass = BasicAuthenticationMechanismTest.class;
    private static String urlHttp;
    private static String urlHttps;

    private DefaultHttpClient httpclient;

    public BasicAuthenticationMechanismTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ServerHelper.setupldapServer();
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.basic");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecAnnotatedBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.annotatedbasic");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEEsecFormAuth.war", true, JAR_NAME, false, "web.jar.base", "web.war.formlogin");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEEsecFormAuthRedirect.war", true, JAR_NAME, false, "web.jar.base", "web.war.redirectformlogin");
        myServer.startServer(true);
        urlHttp = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
        urlHttps = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        ServerHelper.commonStopServer(myServer, Constants.HAS_LDAP_SERVER);
    }

    @Before
    public void setUp() {
        httpclient = new DefaultHttpClient();
        SSLHelper.establishSSLContext(httpclient, 0, myServer, null, null, null, null, null);
    }

    @After
    public void tearDown() {
        httpclient.getConnectionManager().shutdown();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify that
     * <LI> Authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Verify response contains the appropriate required information.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void testBasicAuthValidUserInRole_AllowedAccess() throws Exception {
        String response = executeGetRequestBasicAuthCreds(httpclient, urlHttp + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleUser);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication using annotation.
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify that
     * <LI> Authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Verify response contains the appropriate required information.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC(value = { "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void testAnnotatedBasicAuthValidUserInRole_AllowedAccess() throws Exception {
        String response = executeGetRequestBasicAuthCreds(httpclient, urlHttp + "/JavaEESecAnnotatedBasicAuthServlet/JavaEESecAnnotatedBasic",
                                                          Constants.javaeesec_basicRoleLDAPUser,
                                                          Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleLDAPUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleLDAPUser);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication using annotation.
     * <LI> Login with an invalid userId and password in the javaeesec_basic role and verify that
     * <LI> Authentication Failure error occurs.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401
     * <LI> Verify response contains the appropriate required information.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuthValidUserInRole_DeniedAccess() throws Exception {
        executeGetRequestBasicAuthCreds(httpclient, urlHttp + queryString, Constants.jaspi_invalidUser, Constants.jaspi_invalidPwd,
                                        HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication using annotation.
     * <LI> Login with an valid userId but invalid password in the javaeesec_basic role and verify that
     * <LI> Authentication Failure error occurs.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401
     * <LI> Verify response contains the appropriate required information.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testBasicAuthValidUserInRole_DeniedAccess_WrongPassword() throws Exception {
        executeGetRequestBasicAuthCreds(httpclient, urlHttp + queryString, Constants.javaeesec_basicRoleUser, Constants.jaspi_invalidPwd,
                                        HttpServletResponse.SC_FORBIDDEN);
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSOForBasicAuthenticationMechanismDefinition() throws Exception {
        String cookieHeaderString = driveResourceFlow(urlHttps + "/JavaEESecAnnotatedBasicAuthServlet/JavaEESecAnnotatedBasic");
        assertCookie(cookieHeaderString, false, true);
        String response = redriveFlowWithCookieOnly(urlHttps + "/JavaEESecAnnotatedBasicAuthServlet/JavaEESecAnnotatedBasic", HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleLDAPUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleLDAPUser);
    }

    private String driveResourceFlow(String resource) throws Exception, IOException {
        HttpResponse httpResponse = executeGetRequestBasicAuthCreds(httpclient, resource, Constants.javaeesec_basicRoleLDAPUser, Constants.javaeesec_basicRolePwd);
        String response = processResponse(httpResponse, HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleLDAPUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleLDAPUser);
        Header cookieHeader = getCookieHeader(httpResponse, COOKIE_NAME);
        return cookieHeader.toString();
    }

    private void assertCookie(String cookieHeaderString, boolean secure, boolean httpOnly) {
        assertTrue("The Path parameter must be set.", cookieHeaderString.contains("Path=/"));
        assertEquals("The Secure parameter must" + (secure == true ? "" : " not" + " be set."), secure, cookieHeaderString.contains("Secure"));
        assertEquals("The HttpOnly parameter must" + (httpOnly == true ? "" : " not" + " be set."), httpOnly, cookieHeaderString.contains("HttpOnly"));
    }

    private String redriveFlowWithCookieOnly(String resource, int expectedStatusCode) throws Exception {
        httpclient.getCredentialsProvider().clear();
        return executeGetRequestNoAuthCreds(httpclient, resource, expectedStatusCode);
    }

}
