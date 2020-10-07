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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
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
public class MultipleIdentityStoreFormPostTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = MultipleIdentityStoreFormPostTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String APP_REDIRECT_NAME = "FormPostRedirect";
    protected static String APP_FORWARD_NAME = "FormPostForward";
    protected static String WAR_REDIRECT_NAME = APP_REDIRECT_NAME + ".war";
    protected static String WAR_FORWARD_NAME = APP_FORWARD_NAME + ".war";
    protected static String WAR_RESOURCE_LOCATION = "FormPostResources";
    protected static String XML_NAME = "multipleISFormPost.xml";
    protected static String SERVLET_NAME = "FormPostServlet";
    protected String redirectQueryString = "/" + APP_REDIRECT_NAME + "/" + SERVLET_NAME;
    protected String forwardQueryString = "/" + APP_FORWARD_NAME + "/" + SERVLET_NAME;
    protected static String redirectLoginUri = "/" + APP_REDIRECT_NAME + "/login.jsp";
    protected static String forwardLoginUri = "/" + APP_FORWARD_NAME + "/login.jsp";
    protected static String redirectLoginformUri = "/" + APP_REDIRECT_NAME + "/j_security_check";
    protected static String forwardLoginformUri = "/" + APP_FORWARD_NAME + "/j_security_check";
    protected static String TITLE_LOGIN_PAGE = "login page for the form login test";
    protected static String TITLE_ERROR_PAGE = "A Form login authentication failure occurred";

    protected static String PARAM_FIRST = "firstName";
    protected static String PARAM_LAST = "lastName";
    protected static String PARAM_EMAIL = "eMailAddr";
    protected static String PARAM_PHONE = "phoneNum";
    protected static String PARAM_OPERATION = "operation";
    protected static String PARAM_SUBMIT = "submitAdd";
    protected static String VALUE_FIRST = "firstNameValue";
    protected static String VALUE_LAST = "lastNameValue";
    protected static String VALUE_EMAIL = "eMailAddr@value.com";
    protected static String VALUE_PHONE = "123-123-1234";
    protected static String VALUE_OPERATION = "Add";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public MultipleIdentityStoreFormPostTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        ldapServer = new LocalLdapServer();
        ldapServer.start();

        WCApplicationHelper.addWarToServerApps(myServer, WAR_REDIRECT_NAME, true, WAR_RESOURCE_LOCATION, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.post.redirect",
                                               "web.war.identitystores.ldap.ldap1", "web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly",
                                               "web.war.identitystores.ldap");
        WCApplicationHelper.addWarToServerApps(myServer, WAR_FORWARD_NAME, true, WAR_RESOURCE_LOCATION, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.post.forward",
                                               "web.war.identitystores.ldap.ldap1", "web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly",
                                               "web.war.identitystores.ldap");

        myServer.setServerConfigurationFile(XML_NAME);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(APP_REDIRECT_NAME);
        myServer.addInstalledAppForValidation(APP_FORWARD_NAME);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            if (ldapServer != null) {
                ldapServer.stop();
            }
        }
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
     * <LI> Authentication is done by the first priority IdentityStroe
     * <LI> Additional groups are added by the IdentityStore which only support getting groups.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Veirfy the realm name is the same as the IdentityStore ID of the 1st IdentityStore.
     * <LI> Veirfy the list of groups contains the group name of 1st and 3rd groups only
     * <LI> Veirfy the list of groups does not contain the group name of 2nd identitystore.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleISFormPostRedirectWith1stIS_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        List<NameValuePair> params = createPostParams();

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = postFormLoginPage(httpclient, urlBase + redirectQueryString, params, true, urlBase + redirectLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + redirectLoginformUri, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, SERVLET_NAME);
        verifyPostResponse(response, LocalLdapServer.USER1, VALUE_FIRST, VALUE_LAST, VALUE_EMAIL, VALUE_PHONE);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> First authentication is failed due to invalid password, then retry.
     * <LI> Authentication is done by the second priority IdentityStroe because the user id does not exist in the first IdentityStore.
     * <LI> Additional groups are added by the IdentityStore which only support getting groups.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Veirfy the realm name is the same as the IdentityStore ID of the 2nd IdentityStore.
     * <LI> Veirfy the list of groups contains the group name of 1st and 3rd groups only
     * <LI> Veirfy the list of groups does not contain the group name of 1st identitystore.
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    public void testMultipleISFormPostRedirectWith2ndISonly_RetryAllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        List<NameValuePair> params = createPostParams();

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = postFormLoginPage(httpclient, urlBase + redirectQueryString, params, true, urlBase + redirectLoginUri, TITLE_LOGIN_PAGE);

        // Execute form login for failure.
        String location = executeFormLogin(httpclient, urlBase + redirectLoginformUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.INVALIDPASSWORD, true);
        // Redirect to the error page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, TITLE_ERROR_PAGE);
        verifyMessageReceivedInMessageLog("CWWKS1652A:.*");

        // Execute form login for retry
        location = executeFormLogin(httpclient, urlBase + redirectLoginformUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, SERVLET_NAME);
        verifyPostResponse(response, LocalLdapServer.ANOTHERUSER1, VALUE_FIRST, VALUE_LAST, VALUE_EMAIL, VALUE_PHONE);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the first priority IdentityStroe
     * <LI> Additional groups are added by the IdentityStore which only support getting groups.
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
    public void testMultipleISFormPostForwardWith1stIS_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        List<NameValuePair> params = createPostParams();

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = postFormLoginPage(httpclient, urlBase + forwardQueryString, params, false, urlBase + forwardLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + forwardLoginformUri, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, SERVLET_NAME);
        verifyPostResponse(response, LocalLdapServer.USER1, VALUE_FIRST, VALUE_LAST, VALUE_EMAIL, VALUE_PHONE);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> First authentication is failed due to invalid password, then retry.
     * <LI> Authentication is done by the second priority IdentityStroe because the user id does not exist in the first IdentityStore.
     * <LI> Additional groups are added by the IdentityStore which only support getting groups.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Veirfy the realm name is the same as the IdentityStore ID of the 2nd IdentityStore.
     * <LI> Veirfy the list of groups contains the group name of 1st and 3rd groups only
     * <LI> Veirfy the list of groups does not contain the group name of 1st identitystore.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    public void testMultipleISFormPostForwardWith2ndISonly_RetryAllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        List<NameValuePair> params = createPostParams();

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = postFormLoginPage(httpclient, urlBase + forwardQueryString, params, false, urlBase + forwardLoginUri, TITLE_LOGIN_PAGE);

        // Execute form login for failure.
        String location = executeFormLogin(httpclient, urlBase + forwardLoginformUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.INVALIDPASSWORD, true);
        // Redirect to the error page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, TITLE_ERROR_PAGE);
        verifyMessageReceivedInMessageLog("CWWKS1652A:.*");

        // Execute form login for retry
        location = executeFormLogin(httpclient, urlBase + forwardLoginformUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, SERVLET_NAME);
        verifyPostResponse(response, LocalLdapServer.ANOTHERUSER1, VALUE_FIRST, VALUE_LAST, VALUE_EMAIL, VALUE_PHONE);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    protected static List<NameValuePair> createPostParams() {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_FIRST, VALUE_FIRST));
        params.add(new BasicNameValuePair(PARAM_LAST, VALUE_LAST));
        params.add(new BasicNameValuePair(PARAM_EMAIL, VALUE_EMAIL));
        params.add(new BasicNameValuePair(PARAM_PHONE, VALUE_PHONE));
        params.add(new BasicNameValuePair(PARAM_OPERATION, VALUE_OPERATION));
        params.add(new BasicNameValuePair(PARAM_SUBMIT, VALUE_OPERATION));
        return params;
    }

}
