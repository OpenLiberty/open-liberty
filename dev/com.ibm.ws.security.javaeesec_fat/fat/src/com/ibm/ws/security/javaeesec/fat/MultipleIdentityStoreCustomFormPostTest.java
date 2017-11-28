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
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.params.BasicHttpParams;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.LocalLdapServer;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

@MinimumJavaLevel(javaLevel = 1.8, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MultipleIdentityStoreCustomFormPostTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = MultipleIdentityStoreCustomFormPostTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String APP_NAME = "JavaEESecMultipleISCustomFormPost";
    protected static String WAR_NAME = APP_NAME + ".war";
    protected static String XML_NAME = "multipleISCustomFormPost.xml";
    protected static String SERVLET_NAME = "form.xhtml";
    protected static String SERVLET_TITLE = "AddressBook";
    protected String queryString = "/" + APP_NAME + "/" + SERVLET_NAME;
    protected static String loginUri = "/" + APP_NAME + "/customLogin.xhtml";
    protected static String TITLE_LOGIN_PAGE = "Custom Login Sample by using JSF";
    protected static String TITLE_ERROR_PAGE = "A Form login authentication failure occurred";

    protected static String PARAM_FIRST = "form:firstName";
    protected static String PARAM_LAST = "form:lastName";
    protected static String PARAM_EMAIL = "form:eMailAddr";
    protected static String PARAM_PHONE = "form:phoneNum";
    protected static String PARAM_OPERATION = "form:j_id_f";
    protected static String VALUE_FIRST = "firstNameValue";
    protected static String VALUE_LAST = "lastNameValue";
    protected static String VALUE_EMAIL = "eMailAddr@value.com";
    protected static String VALUE_PHONE = "123-123-1234";
    protected static String VALUE_OPERATION = "Add";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public MultipleIdentityStoreCustomFormPostTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        ldapServer = new LocalLdapServer();
        ldapServer.start();


    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();

        if (ldapServer != null) {
            ldapServer.stop();
        }
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
        WCApplicationHelper.addWarToServerApps(myServer, WAR_NAME, true, JAR_NAME, false, "web.jar.base", servletPackage, "web.war.servlets.customform", "web.war.identitystores.ldap.ldap1","web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly");
        myServer.setServerConfigurationFile(XML_NAME);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(APP_NAME);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
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
    public void testMultipleISCustomFormPostRedirectWith1stIS_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        startServer("web.war.servlets.customform.post.redirect");

        String response = accessPageNoChallenge(httpclient, urlBase + queryString, HttpServletResponse.SC_OK, SERVLET_NAME);

        List<NameValuePair> params = createPostParams(getViewState(response));

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = postFormLoginPage(httpclient, urlBase + queryString, params, true,  urlBase + loginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, getViewState(response));

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, SERVLET_TITLE);
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
    @AllowedFFDC({"javax.naming.AuthenticationException" })
    public void testMultipleISCustomFormPostRedirectWith2ndISonly_RetryAllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        startServer("web.war.servlets.customform.post.redirect");

        String response = accessPageNoChallenge(httpclient, urlBase + queryString, HttpServletResponse.SC_OK, SERVLET_NAME);
        List<NameValuePair> params = createPostParams(getViewState(response));

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = postFormLoginPage(httpclient, urlBase + queryString, params, true,  urlBase + loginUri, TITLE_LOGIN_PAGE);
        String viewState = getViewState(response);
        // Execute form login for failure.
        String location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.INVALIDPASSWORD, viewState);
        // Redirect to the error page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, TITLE_ERROR_PAGE);
        verifyMessageReceivedInMessageLog("CWWKS1652A:.*");

        // Execute form login for retry
        location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, viewState);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, SERVLET_TITLE);
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
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleISCustomFormPostForwardWith1stIS_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        startServer("web.war.servlets.customform.post.forward");

        String response = accessPageNoChallenge(httpclient, urlBase + queryString, HttpServletResponse.SC_OK, SERVLET_NAME);
        List<NameValuePair> params = createPostParams(getViewState(response));

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = postFormLoginPage(httpclient, urlBase + queryString, params, false,  urlBase + loginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, getViewState(response));

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, SERVLET_TITLE);
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
    @AllowedFFDC({"javax.naming.AuthenticationException" })
    public void testMultipleISCustomFormPostForwardWith2ndISonly_RetryAllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        startServer("web.war.servlets.customform.post.forward");

        String response = accessPageNoChallenge(httpclient, urlBase + queryString, HttpServletResponse.SC_OK, SERVLET_NAME);
        List<NameValuePair> params = createPostParams(getViewState(response));

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = postFormLoginPage(httpclient, urlBase + queryString, params, false,  urlBase + loginUri, TITLE_LOGIN_PAGE);

        String viewState = getViewState(response);

        // Execute form login for failure.
        String location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.INVALIDPASSWORD, viewState);
        // Redirect to the error page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, TITLE_ERROR_PAGE);
        verifyMessageReceivedInMessageLog("CWWKS1652A:.*");

        // Execute form login for retry
        location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, viewState);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, SERVLET_TITLE);
        verifyPostResponse(response, LocalLdapServer.ANOTHERUSER1, VALUE_FIRST, VALUE_LAST, VALUE_EMAIL, VALUE_PHONE);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

/* ------------------------ support methods ----------------------*/
    protected String getViewState(String form) {
        Pattern p = Pattern.compile("[\\s\\S]*value=\"(.+)\".*autocomplete[\\s\\S]*");
        Matcher m = p.matcher(form);
        String viewState = null;
        if (m.matches()) {
            viewState = m.group(1);
        }
        return viewState;
    }

    protected List<NameValuePair> createPostParams(String viewState) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_FIRST, VALUE_FIRST));
        params.add(new BasicNameValuePair(PARAM_LAST, VALUE_LAST));
        params.add(new BasicNameValuePair(PARAM_EMAIL, VALUE_EMAIL));
        params.add(new BasicNameValuePair(PARAM_PHONE, VALUE_PHONE));
        params.add(new BasicNameValuePair(PARAM_OPERATION, VALUE_OPERATION));
        params.add(new BasicNameValuePair("form_SUBMIT", "1"));
        params.add(new BasicNameValuePair("javax.faces.ViewState", viewState));
        return params;
    }

}
