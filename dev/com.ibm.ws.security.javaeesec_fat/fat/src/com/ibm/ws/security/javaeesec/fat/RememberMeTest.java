/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 */
@MinimumJavaLevel(javaLevel = 1.7, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class RememberMeTest extends JavaEESecTestBase {

    private static final String REMEMBERME_COOKIE_NAME = "JREMEMBERMEID";

    protected static Class<?> logClass = RememberMeTest.class;
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected String queryString = "/JavaEESec/CommonServlet";
    protected static String[] warList = { "JavaEESec.war" };
    protected static String urlBase;
    protected static String urlHttps;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;

    public RememberMeTest() {
        super(myServer, logClass);
    }

    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        if (!OnlyRunInJava7Rule.IS_JAVA_7_OR_HIGHER) {
            return; // skip the test setup
        }

        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESec.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets",
                                               "web.war.mechanisms",
                                               "web.war.mechanisms.rememberme",
                                               "web.war.identitystores",
                                               "web.war.identitystores.rememberme");
        myServer.setServerConfigurationFile("rememberMe.xml");
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("JavaEESec");

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
        urlHttps = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (!OnlyRunInJava7Rule.IS_JAVA_7_OR_HIGHER) {
            return;
        }
        myServer.stopServer();
        myServer.setServerConfigurationFile("server.xml");
    }

    @SuppressWarnings("restriction")
    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
        SSLHelper.establishSSLContext(httpclient, 0, myServer, null, null, null, null, null);
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    @Test
    public void testRememberMe() throws Exception {
        HttpResponse httpResponse = executeGetRequestBasicAuthCreds(httpclient, urlHttps + queryString, javaeesec_basicRoleUser, javaeesec_basicRolePwd);
        String response = processResponse(httpResponse, HttpServletResponse.SC_OK);
        verifyUserResponse(response, getUserPrincipalFound + javaeesec_basicRoleUser, getRemoteUserFound + javaeesec_basicRoleUser);
        Header cookieHeader = getCookieHeader(httpResponse, REMEMBERME_COOKIE_NAME);
        String cookieHeaderString = cookieHeader.toString();

        assertTrue("The Expires parameter must be set.", cookieHeaderString.contains("Expires="));
        assertTrue("The Path parameter must be set.", cookieHeaderString.contains("Path=/"));
        assertTrue("The Secure parameter must be set.", cookieHeaderString.contains("Secure"));
        assertTrue("The HttpOnly parameter must be set.", cookieHeaderString.contains("HttpOnly"));

        httpclient.getCookieStore().clear();
        response = accessWithCookie(httpclient, urlHttps + queryString, REMEMBERME_COOKIE_NAME, getCookieValue(cookieHeader, REMEMBERME_COOKIE_NAME), HttpServletResponse.SC_OK);
        verifyUserResponse(response, getUserPrincipalFound + javaeesec_basicRoleUser, getRemoteUserFound + javaeesec_basicRoleUser);
    }

    @Test
    public void testRememberMeHttpNoCookie() throws Exception {
        HttpResponse httpResponse = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, javaeesec_basicRoleUser, javaeesec_basicRolePwd);
        String response = processResponse(httpResponse, HttpServletResponse.SC_OK);
        verifyUserResponse(response, getUserPrincipalFound + javaeesec_basicRoleUser, getRemoteUserFound + javaeesec_basicRoleUser);
        validateNoCookie(httpResponse, REMEMBERME_COOKIE_NAME);
    }

}
