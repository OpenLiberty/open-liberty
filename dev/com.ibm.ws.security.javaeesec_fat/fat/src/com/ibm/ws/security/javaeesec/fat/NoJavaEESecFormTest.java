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

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.LocalLdapServer;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class NoJavaEESecFormTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = NoJavaEESecFormTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String APP_NAME = "NoJavaEESecForm";
    protected static String WAR_NAME = APP_NAME + ".war";
    protected static String XML_NAME = "nojavaeesec.xml";
    protected String queryString = "/" + APP_NAME + "/NoJavaEESecFormServlet";
    protected static String loginUri = "/" + APP_NAME + "/login.jsp";
    protected static String loginformUri = "/" + APP_NAME + "/j_security_check";
    protected static String TITLE_LOGIN_PAGE = "login page for the form login test";
    protected static String TITLE_ERROR_PAGE = "A Form login authentication failure occurred";
    protected static boolean REDIRECT = true;
    private static String USER1 = "user1";
    private static String GROUP1 = "group1";
    private static String USER2 = "user2";
    private static String INVALIDUSER1 = "invaliduser1";
    private static String PASSWORD = "s3cur1ty";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public NoJavaEESecFormTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        WCApplicationHelper.addWarToServerApps(myServer, WAR_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.nojavaeesec", "web.war.servlets.nojavaeesec.form");
        myServer.setServerConfigurationFile(XML_NAME);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(APP_NAME);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
    }

    @Before
    public void setupConnection() {
        // disable auto redirect.
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);

        httpclient = new DefaultHttpClient(httpParams);
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> The server starts without any error even there is no JSR375 application.
     * <LI> non JSR 375 Form login application works properly for authentication.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Veirfy the realm name is the same as the IdentityStore ID of the 1st IdentityStore.
     * <LI> Veirfy the list of groups contains the group name of 1st and 3rd groups only
     * <LI> Veirfy the list of groups does not contain the group name of 2nd identitystore.
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testNoJavaEESec_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + queryString, REDIRECT, urlBase + loginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + loginformUri, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + queryString);
        verifyUserResponse(response, Constants.getUserPrincipalFound + USER1, Constants.getRemoteUserFound + USER1);
        verifyRealm(response, "NoJavaEESecRealm");
        verifyGroups(response, "group:NoJavaEESecRealm/group1");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> The server starts without any error even there is no JSR375 application.
     * <LI> non JSR 375 Form login application works properly for authentication.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 302
     * <LI> The error page is returned.
     * <LI> Veirfy the CWWKS9104A message is logged.
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testNoJavaEESecAuthorizationFailure_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + queryString, REDIRECT, urlBase + loginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + loginformUri, USER2, PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns 403 due to authorization failure.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_FORBIDDEN, urlBase + queryString);

        verifyMessageReceivedInMessageLog("CWWKS9104A:.*" + USER2 + ".*" + GROUP1);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> The server starts without any error even there is no JSR375 application.
     * <LI> non JSR 375 Form login application works properly for authentication.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 302
     * <LI> Redirect to the error page.
     * <LI> Veirfy the CWWKS1652A message is logged.
     * </OL>
     */
    @Mode(TestMode.FULL)
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    @Test
    public void testNoJavaEESecAuthenticationFailure_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + queryString, REDIRECT, urlBase + loginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + loginformUri, INVALIDUSER1, PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, TITLE_ERROR_PAGE);
        verifyMessageReceivedInMessageLog("CWWKS1100A:.*" + INVALIDUSER1 + ".*");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }
}
