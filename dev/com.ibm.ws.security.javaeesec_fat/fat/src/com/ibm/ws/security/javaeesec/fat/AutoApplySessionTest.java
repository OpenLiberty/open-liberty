/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class AutoApplySessionTest extends JavaEESecTestBase {

    private static final String COOKIE_NAME = "LtpaToken2";

    protected static Class<?> logClass = AutoApplySessionTest.class;
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected String queryString = "/JavaEESec/CommonServlet";
    protected static String urlBase;
    protected static String urlHttps;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;

    public AutoApplySessionTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESec.war", true, JAR_NAME, false, "web.jar.base", "web.war.servlets",
                                               "web.war.mechanisms",
                                               "web.war.mechanisms.autoapplysession",
                                               "web.war.identitystores", "web.war.identitystores.scoped.application");
        myServer.setServerConfigurationFile("commonServer.xml");
        myServer.startServer(true);
        myServer.addInstalledAppForValidation("JavaEESec");

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
        urlHttps = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort();

        /*
         * Wait for the SSL endpoint to start.
         */
        myServer.waitForStringInLog("CWWKO0219I");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        myServer.stopServer();
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
    public void testAutoApplySession() throws Exception {
        HttpResponse httpResponse = executeGetRequestBasicAuthCreds(httpclient, urlHttps + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd);
        String response = processResponse(httpResponse, HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleUser);
        Header cookieHeader = getCookieHeader(httpResponse, COOKIE_NAME);
        String cookieHeaderString = cookieHeader.toString();

        assertFalse("The Expires element must not be set.", cookieHeaderString.contains("Expires="));
        assertTrue("The Path element must be set.", cookieHeaderString.contains("Path=/"));
        assertFalse("The Secure element must not be set.", cookieHeaderString.contains("Secure")); // By default ssoRequiresSSL is set to false
        assertTrue("The HttpOnly element must be set.", cookieHeaderString.contains("HttpOnly"));

        httpclient.getCookieStore().clear();
        response = accessWithCookie(httpclient, urlHttps + queryString, COOKIE_NAME, getCookieValue(cookieHeader, COOKIE_NAME), HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleUser, Constants.getRemoteUserFound + Constants.javaeesec_basicRoleUser);
    }

}
