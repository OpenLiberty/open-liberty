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
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.LocalLdapServer;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@MinimumJavaLevel(javaLevel = 1.8, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LoginToContinueELTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = LoginToContinueELTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String APP_NAME = "JavaEESecFormELTest";
    protected static String WAR_NAME = APP_NAME + ".war";
    protected static String XML_NAME = "formEL.xml";
    protected static String IMMEDIATE_SERVLET_NAME = "/" + APP_NAME + "/ImmediateFormLogin";
    protected static String DEFERRED_SERVLET_NAME = "/" + APP_NAME + "/DeferredFormLogin";
    protected static String ORIGINAL_ERROR = "/original/loginError.jsp";
    protected static String ALTERNATIVE_ERROR = "/alternative/loginError.jsp";
    protected static String INVALID_ERROR = "/invalid/loginError.jsp";
    protected static String ORIGINAL_LOGIN = "/original/login.jsp";
    protected static String ALTERNATIVE_LOGIN = "/alternative/login.jsp";
    protected static String INVALID_LOGIN = "/invalid/login.jsp";
    protected static String ORIGINAL_LOGIN_URI = "/" + APP_NAME + ORIGINAL_LOGIN;
    protected static String ALTERNATIVE_LOGIN_URI = "/" + APP_NAME + ALTERNATIVE_LOGIN;

    protected static String ORIGINAL_USE_FORWARD = "false";
    protected static String ALTERNATIVE_USE_FORWARD = "true";
    protected static String INVALID_USE_FORWARD = "null";
    protected static String LOGIN_FORM_URI = "/" + APP_NAME + "/j_security_check";
    protected static String EL_SERVLET = "/" + APP_NAME + "/LoginToContinueProps";

    protected static String ORIGINAL_TITLE_LOGIN_PAGE = "Original login page for the form login test";
    protected static String ORIGINAL_TITLE_ERROR_PAGE = "Original Form Login Error Page";
    protected static String ALTERNATIVE_TITLE_LOGIN_PAGE = "Alternative login page for the form login test";
    protected static String ALTERNATIVE_TITLE_ERROR_PAGE = "Alternative Form Login Error Page";

    protected static String PARAM_ERROR_PAGE = "?errorPage=";
    protected static String PARAM_LOGIN_PAGE = "&loginPage=";
    protected static String PARAM_USE_FORWARD = "&useForwardToLogin=";

    protected static String ORIGINAL_SETTING = EL_SERVLET + PARAM_ERROR_PAGE + ORIGINAL_ERROR + PARAM_LOGIN_PAGE + ORIGINAL_LOGIN + PARAM_USE_FORWARD + ORIGINAL_USE_FORWARD;
    protected static String ALTERNATIVE_SETTING = EL_SERVLET + PARAM_ERROR_PAGE + ALTERNATIVE_ERROR + PARAM_LOGIN_PAGE + ALTERNATIVE_LOGIN + PARAM_USE_FORWARD
                                                  + ALTERNATIVE_USE_FORWARD;
    protected static String INVALID_SETTING = EL_SERVLET + PARAM_ERROR_PAGE + INVALID_ERROR + PARAM_LOGIN_PAGE + INVALID_LOGIN + PARAM_USE_FORWARD + INVALID_USE_FORWARD;

    protected static String SETTING_MESSAGE = "ServletName: LoginToContinueTest";

    protected static String USERID = "jaspiuser1";
    protected static String PASSWORD = "s3cur1ty";
    protected static String INVALIDPASSWORD = "invalid";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public LoginToContinueELTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {}

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
        myServer.setServerConfigurationFile("server.xml");
    }

    @Before
    public void setupConnection() {
        // disable auto redirect.
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);

        httpclient = new DefaultHttpClient(httpParams);
    }

    @After
    public void cleanupConnection() throws Exception {
        httpclient.getConnectionManager().shutdown();
        myServer.stopServer();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    protected void startServer(String servletPackage) throws Exception {
        WCApplicationHelper.addWarToServerApps(myServer, WAR_NAME, true, JAR_NAME, false, "web.jar.base", servletPackage, "web.war.servlets.el.ltc", "web.war.identitystores",
                                               "web.war.identitystores.scoped.application");
        myServer.setServerConfigurationFile(XML_NAME);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(APP_NAME);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> EL is only resolved once even multiple call was made.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testLoginToContinueELImmediate() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        startServer("web.war.servlets.el.ltc.immediate");
        // since EL supposes to be resolved while deploying the application, set invalid values to verify not to pick them up.
        accessPageNoChallenge(httpclient, urlBase + INVALID_SETTING, HttpServletResponse.SC_OK, SETTING_MESSAGE);
        // 1st call.
        String response = getFormLoginPage(httpclient, urlBase + IMMEDIATE_SERVLET_NAME, true, urlBase + ORIGINAL_LOGIN_URI, ORIGINAL_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_URI, USERID, PASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + IMMEDIATE_SERVLET_NAME);
        // restart the client.
        httpclient.getConnectionManager().shutdown();
        // invoke second time, and make sure that the original setting is still in effect.
        setupConnection();
        response = getFormLoginPage(httpclient, urlBase + IMMEDIATE_SERVLET_NAME, true, urlBase + ORIGINAL_LOGIN_URI, ORIGINAL_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_URI, USERID, PASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + IMMEDIATE_SERVLET_NAME);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> EL is resolved every invocations.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testLoginToContinueELDeferredLoginPage() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        startServer("web.war.servlets.el.ltc.deferred");
        // 1st call.
        String response = getFormLoginPage(httpclient, urlBase + DEFERRED_SERVLET_NAME, true, urlBase + ORIGINAL_LOGIN_URI, ORIGINAL_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_URI, USERID, PASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + DEFERRED_SERVLET_NAME);
        // restart the client.
        httpclient.getConnectionManager().shutdown();
        // invoke second time, and make sure that the original setting is still in effect.
        setupConnection();
        // change EL setting.
        accessPageNoChallenge(httpclient, urlBase + ALTERNATIVE_SETTING, HttpServletResponse.SC_OK, SETTING_MESSAGE);
        // invoke 2nd time based on the config change.
        response = getFormLoginPage(httpclient, urlBase + DEFERRED_SERVLET_NAME, false, urlBase + ALTERNATIVE_LOGIN_URI, ALTERNATIVE_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_URI, USERID, PASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + DEFERRED_SERVLET_NAME);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> EL is resolved every invocations.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    public void testLoginToContinueELDeferredErrorPage() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        startServer("web.war.servlets.el.ltc.deferred");
        // 1st call.
        String response = getFormLoginPage(httpclient, urlBase + DEFERRED_SERVLET_NAME, true, urlBase + ORIGINAL_LOGIN_URI, ORIGINAL_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_URI, USERID, INVALIDPASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, ORIGINAL_TITLE_ERROR_PAGE);
        // restart the client.
        httpclient.getConnectionManager().shutdown();
        // invoke second time, and make sure that the original setting is still in effect.
        setupConnection();
        // change EL setting.
        accessPageNoChallenge(httpclient, urlBase + ALTERNATIVE_SETTING, HttpServletResponse.SC_OK, SETTING_MESSAGE);
        // invoke 2nd time based on the config change.
        response = getFormLoginPage(httpclient, urlBase + DEFERRED_SERVLET_NAME, false, urlBase + ALTERNATIVE_LOGIN_URI, ALTERNATIVE_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_URI, USERID, INVALIDPASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, ALTERNATIVE_TITLE_ERROR_PAGE);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
