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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.params.ClientPNames;
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
public class MultipleIdentityStoreCustomFormRedirectTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = MultipleIdentityStoreCustomFormRedirectTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String APP_NAME = "JavaEESecMultipleISCustomForm";
    protected static String WAR_NAME = APP_NAME + ".war";
    protected static String XML_NAME = "multipleISCustomForm.xml";
    protected String queryString = "/" + APP_NAME + "/MultipleISCustomFormServlet";
    protected static String loginUri = "/" + APP_NAME + "/customLogin.xhtml";
    protected static String TITLE_LOGIN_PAGE = "Custom Login Sample by using JSF";
    protected static String TITLE_ERROR_PAGE = "A Form login authentication failure occurred";
    protected static boolean REDIRECT = true;
    protected DefaultHttpClient httpclient;   

    protected static LocalLdapServer ldapServer;

    public MultipleIdentityStoreCustomFormRedirectTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        ldapServer = new LocalLdapServer();
        ldapServer.start();

        WCApplicationHelper.addWarToServerApps(myServer, WAR_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.customform", "web.war.servlets.customform.redirect", "web.war.identitystores.ldap.ldap1","web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly");
        myServer.setServerConfigurationFile(XML_NAME);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(APP_NAME);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

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
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleISCustomFormRedirectWith1stIS_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + queryString, REDIRECT, urlBase + loginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, getViewState(response));

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + queryString);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "127.0.0.1:10389");
        verifyNotInGroups(response, "group:localhost:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/cn=group1,ou=groups,o=ibm,c=us, group:127.0.0.1:10389/grantedgroup");
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
    @Mode(TestMode.FULL)
    @Test
    public void testMultipleISCustomFormRedirectWith2ndISonly_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + queryString, REDIRECT, urlBase + loginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, getViewState(response));

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + queryString);

        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, Constants.getRemoteUserFound + LocalLdapServer.ANOTHERUSER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/cn=anothergroup1,ou=anothergroups,o=ibm,c=us, group:localhost:10389/grantedgroup");
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
    @Mode(TestMode.LITE)
    @AllowedFFDC({"javax.naming.AuthenticationException" })
    @Test
    public void testMultipleISCustomFormRedirectWith1stISfail2ndISsuccess_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + queryString, REDIRECT, urlBase + loginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.USER1, LocalLdapServer.ANOTHERPASSWORD, getViewState(response));

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + queryString);

        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/cn=anothergroup1,ou=anothergroups,o=ibm,c=us, group:localhost:10389/grantedgroup");
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
    @Mode(TestMode.FULL)
    @Test
    public void testMultipleISCustomFormRedirectWith1stISuccess_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + queryString, REDIRECT, urlBase + loginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.INVALIDUSER, LocalLdapServer.PASSWORD, getViewState(response));

        // Redirect to the given page, ensure it is the original servlet request and it returns 403 due to authorization failure.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_FORBIDDEN, urlBase + queryString);

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
    @Mode(TestMode.LITE)
    @AllowedFFDC({"javax.naming.AuthenticationException" })
    @Test
    public void testMultipleISCustomFormRedirectWith1st2ndFail_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + queryString, REDIRECT, urlBase + loginUri, TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeCustomFormLogin(httpclient, urlBase + loginUri, LocalLdapServer.USER1, LocalLdapServer.INVALIDPASSWORD, getViewState(response));

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, TITLE_ERROR_PAGE);
        verifyMessageReceivedInMessageLog("CWWKS1652A:.*");
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


}
