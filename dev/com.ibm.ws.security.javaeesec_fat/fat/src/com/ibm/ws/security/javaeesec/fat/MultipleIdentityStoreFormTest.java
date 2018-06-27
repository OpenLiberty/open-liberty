/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@MinimumJavaLevel(javaLevel = 8, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MultipleIdentityStoreFormTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = MultipleIdentityStoreFormTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String WAR_RESOURCE_LOCATION = "FormGetResources";
    protected static String APP_REDIRECT_NAME = "FormRedirect";
    protected static String APP_FORWARD_NAME = "FormForward";
    protected static String WAR_REDIRECT_NAME = APP_REDIRECT_NAME + ".war";
    protected static String WAR_FORWARD_NAME = APP_FORWARD_NAME + ".war";
    protected static String XML_NAME = "multipleISForm.xml";
    protected String redirectQueryString = "/" + APP_REDIRECT_NAME + "/FormServlet";
    protected String forwardQueryString = "/" + APP_FORWARD_NAME + "/FormServlet";
    protected static String redirectLoginUri = "/" + APP_REDIRECT_NAME + "/login.jsp";
    protected static String forwardLoginUri = "/" + APP_FORWARD_NAME + "/login.jsp";
    protected static String redirectLoginformUri = "/" + APP_REDIRECT_NAME + "/j_security_check";
    protected static String forwardLoginformUri = "/" + APP_FORWARD_NAME + "/j_security_check";
    protected static String TITLE_LOGIN_PAGE = "login page for the form login test";
    protected static String TITLE_ERROR_PAGE = "A Form login authentication failure occurred";
    protected static boolean REDIRECT = true;
    protected static boolean FORWARD = false;
    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public MultipleIdentityStoreFormTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        ldapServer = new LocalLdapServer();
        ldapServer.start();

        WCApplicationHelper.addWarToServerApps(myServer, WAR_REDIRECT_NAME, true, WAR_RESOURCE_LOCATION,JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.redirect", "web.war.identitystores.ldap.ldap1",
                                               "web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly");
        WCApplicationHelper.addWarToServerApps(myServer, WAR_FORWARD_NAME, true, WAR_RESOURCE_LOCATION, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.forward", "web.war.identitystores.ldap.ldap1",
                                               "web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly");
        myServer.setServerConfigurationFile(XML_NAME);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(APP_REDIRECT_NAME);
        myServer.addInstalledAppForValidation(APP_FORWARD_NAME);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
        if (ldapServer != null) {
            ldapServer.stop();
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
    @Test
    public void testMultipleISFormRedirectWith1stIS_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + redirectQueryString, REDIRECT, urlBase + redirectLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + redirectLoginformUri, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + redirectQueryString);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "127.0.0.1:10389");
        verifyNotInGroups(response, "group:localhost:10389/"); // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/grantedgroup, group:127.0.0.1:10389/group1");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the second priority IdentityStroe because the user id does not exist in the first IdentityStore.
     * <LI> Additional groups are added by the IdentityStore which only support getting groups.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Veirfy the realm name is the same as the IdentityStore ID of the 2nd IdentityStore.
     * <LI> Veirfy the list of groups contains the group name of 1st and 3rd groups only
     * <LI> Veirfy the list of groups does not contain the group name of 1st identitystore.
     * <LI> Verify LtpaToken2 is set, and WASReqURL cookie is being removed.
     * </OL>
     */
    @Test
    public void testMultipleISFormRedirectWith2ndISonly_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + redirectQueryString, REDIRECT, urlBase + redirectLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String [] sessionCookie = {"LtpaToken2"};
        String location = executeFormLogin(httpclient, urlBase + redirectLoginformUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true, null, sessionCookie);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String [] wasReqURLCookie = {"WASReqURL"};
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + redirectQueryString, wasReqURLCookie);

        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, Constants.getRemoteUserFound + LocalLdapServer.ANOTHERUSER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/"); // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/anothergroup1, group:localhost:10389/grantedgroup");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the second priority IdentityStroe because the password does not match in the 1st IdentityStore.
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
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    @Test
    public void testMultipleISFormRedirectWith1stISfail2ndISsuccess_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + redirectQueryString, REDIRECT, urlBase + redirectLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + redirectLoginformUri, LocalLdapServer.USER1, LocalLdapServer.ANOTHERPASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + redirectQueryString);

        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/"); // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/anothergroup1, group:localhost:10389/grantedgroup");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the first priority IdentityStroe but fails authorization check.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 302
     * <LI> The error page is returned.
     * <LI> Veirfy the CWWKS9104A message is logged.
     * </OL>
     */
    @Test
    public void testMultipleISFormRedirectWith1stISuccess_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + redirectQueryString, REDIRECT, urlBase + redirectLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + redirectLoginformUri, LocalLdapServer.INVALIDUSER, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns 403 due to authorization failure.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_FORBIDDEN, urlBase + redirectQueryString);

        verifyMessageReceivedInMessageLog("CWWKS9104A:.*" + LocalLdapServer.INVALIDUSER + ".*" + LocalLdapServer.GRANTEDGROUP);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is failed for both the 1st and 2nd IdentityStore.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 302
     * <LI> Redirect to the error page.
     * <LI> Veirfy the CWWKS1652A message is logged.
     * </OL>
     */
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    @Test
    public void testMultipleISFormRedirectWith1st2ndFail_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + redirectQueryString, REDIRECT, urlBase + redirectLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + redirectLoginformUri, LocalLdapServer.USER1, LocalLdapServer.INVALIDPASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, TITLE_ERROR_PAGE);
        verifyMessageReceivedInMessageLog("CWWKS1652A:.*");
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
    @Test
    public void testMultipleISFormForwardWith1stIS_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + forwardQueryString, FORWARD, urlBase + forwardLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + forwardLoginformUri, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + forwardQueryString);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "127.0.0.1:10389");
        verifyNotInGroups(response, "group:localhost:10389/"); // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/grantedgroup, group:127.0.0.1:10389/group1");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
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
    @Test
    public void testMultipleISFormForwardWith2ndISonly_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + forwardQueryString, FORWARD, urlBase + forwardLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + forwardLoginformUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + forwardQueryString);

        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, Constants.getRemoteUserFound + LocalLdapServer.ANOTHERUSER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/"); // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/anothergroup1, group:localhost:10389/grantedgroup");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the second priority IdentityStroe because the password does not match in the 1st IdentityStore.
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
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    @Test
    public void testMultipleISFormForwardWith1stISfail2ndISsuccess_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + forwardQueryString, FORWARD, urlBase + forwardLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + forwardLoginformUri, LocalLdapServer.USER1, LocalLdapServer.ANOTHERPASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + forwardQueryString);

        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/"); // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/anothergroup1, group:localhost:10389/grantedgroup");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the first priority IdentityStroe but fails authorization check.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 302
     * <LI> The error page is returned.
     * <LI> Veirfy the CWWKS9104A message is logged.
     * </OL>
     */
    @Test
    public void testMultipleISFormForwardWith1stISuccess_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + forwardQueryString, FORWARD, urlBase + forwardLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + forwardLoginformUri, LocalLdapServer.INVALIDUSER, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns 403 due to authorization failure.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_FORBIDDEN, urlBase + forwardQueryString);

        verifyMessageReceivedInMessageLog("CWWKS9104A:.*" + LocalLdapServer.INVALIDUSER + ".*" + LocalLdapServer.GRANTEDGROUP);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is failed for both the 1st and 2nd IdentityStore.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 302
     * <LI> Redirect to the error page.
     * <LI> Veirfy the CWWKS1652A message is logged.
     * </OL>
     */
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    @Test
    public void testMultipleISFormForwardWith1st2ndFail_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + forwardQueryString, FORWARD, urlBase + forwardLoginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + forwardLoginformUri, LocalLdapServer.USER1, LocalLdapServer.INVALIDPASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, TITLE_ERROR_PAGE);
        verifyMessageReceivedInMessageLog("CWWKS1652A:.*");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
