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
public class LoginToContinueELTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = LoginToContinueELTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String APP_IMMEDIATE_NAME = "LTCELImmediate";
    protected static String APP_DEFERRED_NAME = "LTCELDeferred";
    protected static String WAR_IMMEDIATE_NAME = APP_IMMEDIATE_NAME + ".war";
    protected static String WAR_DEFERRED_NAME = APP_DEFERRED_NAME + ".war";
    protected static String WAR_RESOURCE_LOCATION = "LTCELResources";
    protected static String XML_NAME = "LTCEL.xml";
    protected static String IMMEDIATE_SERVLET_NAME = "/" + APP_IMMEDIATE_NAME + "/ImmediateFormLogin";
    protected static String DEFERRED_SERVLET_NAME = "/" + APP_DEFERRED_NAME + "/DeferredFormLogin";
    protected static String ORIGINAL_ERROR = "/original/loginError.jsp";
    protected static String ALTERNATIVE_ERROR = "/alternative/loginError.jsp";
    protected static String INVALID_ERROR = "/invalid/loginError.jsp";
    protected static String ORIGINAL_LOGIN = "/original/login.jsp";
    protected static String ALTERNATIVE_LOGIN = "/alternative/login.jsp";
    protected static String INVALID_LOGIN = "/invalid/login.jsp";

    protected static String ORIGINAL_USE_FORWARD = "false";
    protected static String ALTERNATIVE_USE_FORWARD = "true";
    protected static String INVALID_USE_FORWARD = "null";
    protected static String LOGIN_FORM_IMMEDIATE_URI = "/" + APP_IMMEDIATE_NAME + "/j_security_check";
    protected static String LOGIN_FORM_DEFERRED_URI = "/" + APP_DEFERRED_NAME + "/j_security_check";
    protected static String EL_IMMEDIATE_SERVLET = "/" + APP_IMMEDIATE_NAME + "/LoginToContinueProps";
    protected static String EL_DEFERRED_SERVLET = "/" + APP_DEFERRED_NAME + "/LoginToContinueProps";

    protected static String ORIGINAL_TITLE_LOGIN_PAGE = "Original login page for the form login test";
    protected static String ORIGINAL_TITLE_ERROR_PAGE = "Original Form Login Error Page";
    protected static String ALTERNATIVE_TITLE_LOGIN_PAGE = "Alternative login page for the form login test";
    protected static String ALTERNATIVE_TITLE_ERROR_PAGE = "Alternative Form Login Error Page";

    protected static String PARAM_ERROR_PAGE = "?errorPage=";
    protected static String PARAM_LOGIN_PAGE = "&loginPage=";
    protected static String PARAM_USE_FORWARD = "&useForwardToLogin=";

    protected static String ORIGINAL_DEFERRED_SETTING = EL_DEFERRED_SERVLET + PARAM_ERROR_PAGE + ORIGINAL_ERROR + PARAM_LOGIN_PAGE + ORIGINAL_LOGIN + PARAM_USE_FORWARD
                                                        + ORIGINAL_USE_FORWARD;
    protected static String ALTERNATIVE_DEFERRED_SETTING = EL_DEFERRED_SERVLET + PARAM_ERROR_PAGE + ALTERNATIVE_ERROR + PARAM_LOGIN_PAGE + ALTERNATIVE_LOGIN + PARAM_USE_FORWARD
                                                           + ALTERNATIVE_USE_FORWARD;
    protected static String INVALID_IMMEDIATE_SETTING = EL_IMMEDIATE_SERVLET + PARAM_ERROR_PAGE + INVALID_ERROR + PARAM_LOGIN_PAGE + INVALID_LOGIN + PARAM_USE_FORWARD
                                                        + INVALID_USE_FORWARD;

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
    public static void setUp() throws Exception {
        WCApplicationHelper.addWarToServerApps(myServer, WAR_IMMEDIATE_NAME, true, WAR_RESOURCE_LOCATION, JAR_NAME, false, "web.jar.base", "web.war.servlets.el.ltc.immediate",
                                               "web.war.servlets.el.ltc", "web.war.identitystores", "web.war.identitystores.scoped.application");
        WCApplicationHelper.addWarToServerApps(myServer, WAR_DEFERRED_NAME, true, WAR_RESOURCE_LOCATION, JAR_NAME, false, "web.jar.base", "web.war.servlets.el.ltc.deferred",
                                               "web.war.servlets.el.ltc", "web.war.identitystores", "web.war.identitystores.scoped.application");
        myServer.setServerConfigurationFile(XML_NAME);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(APP_IMMEDIATE_NAME);
        myServer.addInstalledAppForValidation(APP_DEFERRED_NAME);
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
    public void cleanupConnection() throws Exception {
        httpclient.getConnectionManager().shutdown();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> EL is only resolved once even multiple call was made.
     * </OL>
     */
    @Test
    public void testLoginToContinueELImmediate() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // since EL supposes to be resolved while deploying the application, set invalid values to verify not to pick them up.
        accessPageNoChallenge(httpclient, urlBase + INVALID_IMMEDIATE_SETTING, HttpServletResponse.SC_OK, SETTING_MESSAGE);
        // 1st call.
        String response = getFormLoginPage(httpclient, urlBase + IMMEDIATE_SERVLET_NAME, true, urlBase + "/" + APP_IMMEDIATE_NAME + ORIGINAL_LOGIN, ORIGINAL_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_IMMEDIATE_URI, USERID, PASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + IMMEDIATE_SERVLET_NAME);
        // restart the client.
        httpclient.getConnectionManager().shutdown();
        // invoke second time, and make sure that the original setting is still in effect.
        setupConnection();
        response = getFormLoginPage(httpclient, urlBase + IMMEDIATE_SERVLET_NAME, true, urlBase + "/" + APP_IMMEDIATE_NAME + ORIGINAL_LOGIN, ORIGINAL_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_IMMEDIATE_URI, USERID, PASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + IMMEDIATE_SERVLET_NAME);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> EL is resolved every invocations.
     * </OL>
     */
    @Test
    public void testLoginToContinueELDeferredLoginPage() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // set to the original setting.
        accessPageNoChallenge(httpclient, urlBase + ORIGINAL_DEFERRED_SETTING, HttpServletResponse.SC_OK, SETTING_MESSAGE);
        // 1st call.
        String response = getFormLoginPage(httpclient, urlBase + DEFERRED_SERVLET_NAME, true, urlBase + "/" + APP_DEFERRED_NAME + ORIGINAL_LOGIN, ORIGINAL_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_DEFERRED_URI, USERID, PASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + DEFERRED_SERVLET_NAME);
        // restart the client.
        httpclient.getConnectionManager().shutdown();
        // invoke second time, and make sure that the original setting is still in effect.
        setupConnection();
        // change EL setting.
        accessPageNoChallenge(httpclient, urlBase + ALTERNATIVE_DEFERRED_SETTING, HttpServletResponse.SC_OK, SETTING_MESSAGE);
        // invoke 2nd time based on the config change.
        response = getFormLoginPage(httpclient, urlBase + DEFERRED_SERVLET_NAME, false, urlBase + "/" + APP_DEFERRED_NAME + ALTERNATIVE_LOGIN, ALTERNATIVE_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_DEFERRED_URI, USERID, PASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + DEFERRED_SERVLET_NAME);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> EL is resolved every invocations.
     * </OL>
     */
    @Test
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    public void testLoginToContinueELDeferredErrorPage() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // set to the original setting.
        accessPageNoChallenge(httpclient, urlBase + ORIGINAL_DEFERRED_SETTING, HttpServletResponse.SC_OK, SETTING_MESSAGE);
        // 1st call.
        String response = getFormLoginPage(httpclient, urlBase + DEFERRED_SERVLET_NAME, true, urlBase + "/" + APP_DEFERRED_NAME + ORIGINAL_LOGIN, ORIGINAL_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_DEFERRED_URI, USERID, INVALIDPASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, ORIGINAL_TITLE_ERROR_PAGE);
        // restart the client.
        httpclient.getConnectionManager().shutdown();
        // invoke second time, and make sure that the original setting is still in effect.
        setupConnection();
        // change EL setting.
        accessPageNoChallenge(httpclient, urlBase + ALTERNATIVE_DEFERRED_SETTING, HttpServletResponse.SC_OK, SETTING_MESSAGE);
        // invoke 2nd time based on the config change.
        response = getFormLoginPage(httpclient, urlBase + DEFERRED_SERVLET_NAME, false, urlBase + "/" + APP_DEFERRED_NAME + ALTERNATIVE_LOGIN, ALTERNATIVE_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + LOGIN_FORM_DEFERRED_URI, USERID, INVALIDPASSWORD, true);
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, ALTERNATIVE_TITLE_ERROR_PAGE);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
