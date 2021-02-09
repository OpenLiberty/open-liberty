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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RememberMeTest extends JavaEESecTestBase {

    private static final String REMEMBERME_COOKIE_NAME = "JREMEMBERMEID";

    protected static Class<?> logClass = RememberMeTest.class;
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected String queryString = "/JavaEESec/CommonServlet";
    protected static String[] warList = { "JavaEESec.war" };
    protected static String urlHttp;
    protected static String urlHttps;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;

    public RememberMeTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESec.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets",
                                               "web.war.mechanisms",
                                               "web.war.mechanisms.rememberme",
                                               "web.war.identitystores",
                                               "web.war.identitystores.scoped.application",
                                               "web.war.identitystores.rememberme");

        WCApplicationHelper.addWarToServerApps(myServer, "SecureOnlyFalseHttpOnlyFalseRememberMe.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets",
                                               "web.war.mechanisms",
                                               "web.war.mechanisms.rememberme.secureonlyfalse",
                                               "web.war.identitystores",
                                               "web.war.identitystores.scoped.application",
                                               "web.war.identitystores.rememberme");

        WCApplicationHelper.addWarToServerApps(myServer, "UnprotectedRememberMe.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets.unprotected.rememberme",
                                               "web.war.mechanisms",
                                               "web.war.mechanisms.rememberme",
                                               "web.war.identitystores",
                                               "web.war.identitystores.scoped.application",
                                               "web.war.identitystores.rememberme");

        myServer.setServerConfigurationFile("rememberMe.xml");
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("JavaEESec");
        myServer.addInstalledAppForValidation("SecureOnlyFalseHttpOnlyFalseRememberMe");
        myServer.addInstalledAppForValidation("UnprotectedRememberMe");

        urlHttp = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
        urlHttps = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        ServerHelper.commonStopServer(myServer);
    }

    @SuppressWarnings("restriction")
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

    @Test
    public void testRememberMe() throws Exception {
        String cookieHeaderString = driveRememerMeFlow(urlHttps + queryString);
        assertRememberMeCookie(cookieHeaderString, true, true);
        String response = redriveFlowWithRememberMeCookieOnly(urlHttps + queryString, HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleUser);
    }

    @Test
    public void testRememberMeCookieIsReceivedButNotSentBackForHttpWhenUsingCookieSecureOnly() throws Exception {
        String cookieHeaderString = driveRememerMeFlow(urlHttp + queryString);
        assertRememberMeCookie(cookieHeaderString, true, true);
        redriveFlowWithRememberMeCookieOnly(urlHttp + queryString, HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void testRememberMeWithCookieSecureOnlyFalseHttpOnlyFalse() throws Exception {
        String cookieHeaderString = driveRememerMeFlow(urlHttp + "/SecureOnlyFalseHttpOnlyFalseRememberMe/CommonServlet");
        assertRememberMeCookie(cookieHeaderString, false, false);
        redriveFlowWithRememberMeCookieOnly(urlHttp + "/SecureOnlyFalseHttpOnlyFalseRememberMe/CommonServlet", HttpServletResponse.SC_OK);
    }

    @Test
    public void testRememberMeWithSecurityContextAuthenticate() throws Exception {
        HttpResponse httpResponse = executeGetRequestNoAuthCreds(httpclient, urlHttps + "/UnprotectedRememberMe/UnprotectedServlet?rememberMe=true");
        String response = processResponse(httpResponse, HttpServletResponse.SC_OK);
        mustContain(response, "SecurityContext authenticate AuthenticationStatus: SUCCESS");
        Header cookieHeader = getCookieHeader(httpResponse, REMEMBERME_COOKIE_NAME);
        assertRememberMeCookie(cookieHeader.toString(), true, true);
    }

    @Test
    public void testRememberMeWithSecurityContextAuthenticateAndRememberMeFalse() throws Exception {
        HttpResponse httpResponse = executeGetRequestNoAuthCreds(httpclient, urlHttps + "/UnprotectedRememberMe/UnprotectedServlet?rememberMe=false");
        String response = processResponse(httpResponse, HttpServletResponse.SC_OK);
        mustContain(response, "SecurityContext authenticate AuthenticationStatus: SUCCESS");
        validateNoCookie(httpResponse, REMEMBERME_COOKIE_NAME);
    }

    private String driveRememerMeFlow(String resource) throws Exception, IOException {
        HttpResponse httpResponse = executeGetRequestBasicAuthCreds(httpclient, resource, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd);
        String response = processResponse(httpResponse, HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleUser);
        Header cookieHeader = getCookieHeader(httpResponse, REMEMBERME_COOKIE_NAME);
        return cookieHeader.toString();
    }

    private void assertRememberMeCookie(String cookieHeaderString, boolean secure, boolean httpOnly) {
        assertTrue("The Expires parameter must be set.", cookieHeaderString.contains("Expires="));
        assertTrue("The Path parameter must be set.", cookieHeaderString.contains("Path=/"));
        assertEquals("The Secure parameter must" + (secure == true ? "" : " not" + " be set."), secure, cookieHeaderString.contains("Secure"));
        assertEquals("The HttpOnly parameter must" + (httpOnly == true ? "" : " not" + " be set."), httpOnly, cookieHeaderString.contains("HttpOnly"));
    }

    private String redriveFlowWithRememberMeCookieOnly(String resource, int expectedStatusCode) throws Exception {
        httpclient.getCredentialsProvider().clear();
        return executeGetRequestNoAuthCreds(httpclient, resource, expectedStatusCode);
    }

}
