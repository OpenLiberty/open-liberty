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

import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;

@MinimumJavaLevel(javaLevel = 1.8, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MultipleModuleTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = MultipleModuleTest.class;
    protected static String urlBase;
    protected static String TEMP_DIR = "test_temp";
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String IS_JAR_NAME = "IdentityStores.jar";
    protected static String HAM_JAR_NAME = "AuthMechs.jar";
    protected static String MODULE1_ROOT = "multipleModule1";
    protected static String MODULE1_NAME = "JavaEESecMultipleISForm";
    protected static String WAR1_NAME = MODULE1_NAME + ".war";
    protected static String MODULE2_ROOT = "multipleModule2";
    protected static String MODULE2_NAME = "JavaEESecMultipleISForm2";
    protected static String WAR2_NAME = MODULE2_NAME + ".war";
    protected static String MODULE2CUSTOM_NAME = "JavaEESecMultipleISCustomForm";
    protected static String WAR2CUSTOM_NAME = MODULE2CUSTOM_NAME + ".war";
    protected static String XML_NAME = "multipleModule.xml";
    protected static String XML2_NAME = "multipleModule2.xml";
    protected static String APP_NAME = "multipleModule";
    protected static String APP2_NAME = "multipleModule2";
    protected static String EAR_NAME = APP_NAME + ".ear";
    protected static String EAR2_NAME = APP2_NAME + ".ear";
    protected static String APP1_SERVLET = "/" + MODULE1_ROOT + "/MultipleISFormServlet";
    protected static String APP2_SERVLET = "/" + MODULE2_ROOT + "/MultipleISCustomFormServlet";

    protected static String COMMON_APP1_SERVLET = "/" + MODULE1_ROOT + "/SecuredServlet";
    protected static String COMMON_APP2_SERVLET = "/" + MODULE2_ROOT + "/SecuredServlet";
    protected static String MODULE1_LOGIN = "/" + MODULE1_ROOT + "/login.jsp";
    protected static String MODULE1_LOGINFORM = "/" + MODULE1_ROOT + "/j_security_check";
    protected static String MODULE2_CUSTOMLOGIN = "/" + MODULE2_ROOT + "/customLogin.xhtml";
    protected static String MODULE2_LOGIN = "/" + MODULE2_ROOT + "/login.jsp";
    protected static String MODULE2_LOGINFORM = "/" + MODULE2_ROOT + "/j_security_check";
    protected static String MODULE1_TITLE_LOGIN_PAGE = "login page for the form login test";
    protected static String MODULE1_TITLE_ERROR_PAGE = "A Form login authentication failure occurred";
    protected static String MODULE2_TITLE_LOGIN_PAGE = "login page for the form login2 test";
    protected static String MODULE2_TITLE_ERROR_PAGE = "A Form login2 authentication failure occurred";
    protected static String MODULE2_TITLE_CUSTOMLOGIN_PAGE = "Custom Login Sample by using JSF";
    protected static String MODULE2_TITLE_CUSTOMERROR_PAGE = "A Form login authentication failure occurred";

    protected static String REALM1_USER = "realm1user";
    protected static String REALM1_PASSWORD = "s3cur1ty";
    protected static String REALM2_USER = "realm2user";
    protected static String REALM2_PASSWORD = "s3cur1ty";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public MultipleModuleTest() {
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
    public void cleanupConnection() throws Exception{
        httpclient.getConnectionManager().shutdown();
        myServer.stopServer();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    protected void startServer(String config, String appName) throws Exception {
        myServer.setServerConfigurationFile(config);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(appName);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     *      and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> In this case, the IdentityStores which are defined by LdapIdentityStoreDefinision are visible from any module, however,
     *      the one which are bundled with each module is only visible within the module.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWars() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // create module1, form login, redirect, ldap1. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR1_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.redirect", "web.war.identitystores.ldap.ldap1","web.war.identitystores.custom.grouponly","web.war.identitystores.custom.realm1");
        // create module2, custom form login, forward, ldap2. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR2CUSTOM_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.customform", "web.war.servlets.customform.get.forward", "web.war.identitystores.ldap.ldap2","web.war.identitystores.custom.grouponly","web.war.identitystores.custom.realm2");

        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EAR_NAME, true, WAR1_NAME, WAR2CUSTOM_NAME);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EAR_NAME);

        startServer(XML_NAME, APP_NAME);

        // ------------- accessing module1 ---------------
        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "127.0.0.1:10389");
        verifyNotInGroups(response, "group:localhost:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/cn=group1,ou=groups,o=ibm,c=us, group:127.0.0.1:10389/grantedgroup");

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on the other module.

        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, Constants.getRemoteUserFound + LocalLdapServer.ANOTHERUSER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/cn=anothergroup1,ou=anothergroups,o=ibm,c=us, group:localhost:10389/grantedgroup");

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.

        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, REALM1_USER, REALM1_PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + REALM1_USER, Constants.getRemoteUserFound + REALM1_USER);
        verifyRealm(response, "Realm1");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:Realm1/grantedgroup2, group:Realm1/grantedgroup, group:Realm1/realm1group1, group:Realm1/realm1group2");

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in the different module.
        // this should fail.
        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, REALM2_USER, REALM2_PASSWORD, true);

        // Redirect to the given page, ensure that this is an error page since there is no user exist in the identitystores.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, MODULE1_TITLE_ERROR_PAGE);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // ------------- accessing module2 ---------------
        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, Constants.getRemoteUserFound + LocalLdapServer.ANOTHERUSER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/cn=anothergroup1,ou=anothergroups,o=ibm,c=us, group:localhost:10389/grantedgroup");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in another module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "127.0.0.1:10389");
        verifyNotInGroups(response, "group:localhost:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/cn=group1,ou=groups,o=ibm,c=us, group:127.0.0.1:10389/grantedgroup");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, REALM2_USER, REALM2_PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + REALM2_USER, Constants.getRemoteUserFound + REALM2_USER);
        verifyRealm(response, "Realm2");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:Realm2/grantedgroup2, group:Realm2/realm2group2, group:Realm2/realm2group1, group:Realm2/grantedgroup");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in the different module.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, REALM1_USER, REALM1_PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, MODULE2_TITLE_CUSTOMERROR_PAGE);


        myServer.removeInstalledAppForValidation(APP_NAME);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     *      which is packaged in a jar file.
     *      and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> In this case, the IdentityStores which are defined by LdapIdentityStoreDefinision are visible from any module, however,
     *      the one which are bundled with each module is only visible within the module.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWarsWithModuleJar() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // create module1, form login, redirect, ldap1. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR1_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.redirect", "web.war.identitystores.ldap.ldap1","web.war.identitystores.custom.grouponly","web.jar.realm1");
        // create module2, custom form login, forward, ldap2. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR2CUSTOM_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.customform", "web.war.servlets.customform.get.forward", "web.war.identitystores.ldap.ldap2","web.war.identitystores.custom.grouponly","web.jar.realm2");

        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EAR_NAME, true, WAR1_NAME, WAR2CUSTOM_NAME);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EAR_NAME);

        startServer(XML_NAME, APP_NAME);

        // ------------- accessing module1 ---------------
        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "127.0.0.1:10389");
        verifyNotInGroups(response, "group:localhost:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/cn=group1,ou=groups,o=ibm,c=us, group:127.0.0.1:10389/grantedgroup");

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on the other module.

        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, Constants.getRemoteUserFound + LocalLdapServer.ANOTHERUSER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/cn=anothergroup1,ou=anothergroups,o=ibm,c=us, group:localhost:10389/grantedgroup");

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.

        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, REALM1_USER, REALM1_PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + REALM1_USER, Constants.getRemoteUserFound + REALM1_USER);
        verifyRealm(response, "Realm1");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:Realm1/grantedgroup2, group:Realm1/grantedgroup, group:Realm1/realm1group1, group:Realm1/realm1group2");

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in the different module.
        // this should fail.
        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, REALM2_USER, REALM2_PASSWORD, true);

        // Redirect to the given page, ensure that this is an error page since there is no user exist in the identitystores.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, MODULE1_TITLE_ERROR_PAGE);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // ------------- accessing module2 ---------------
        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, Constants.getRemoteUserFound + LocalLdapServer.ANOTHERUSER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/cn=anothergroup1,ou=anothergroups,o=ibm,c=us, group:localhost:10389/grantedgroup");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in another module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "127.0.0.1:10389");
        verifyNotInGroups(response, "group:localhost:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/cn=group1,ou=groups,o=ibm,c=us, group:127.0.0.1:10389/grantedgroup");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, REALM2_USER, REALM2_PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + REALM2_USER, Constants.getRemoteUserFound + REALM2_USER);
        verifyRealm(response, "Realm2");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:Realm2/grantedgroup2, group:Realm2/realm2group2, group:Realm2/realm2group1, group:Realm2/grantedgroup");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in the different module.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, REALM1_USER, REALM1_PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, MODULE2_TITLE_CUSTOMERROR_PAGE);

        myServer.removeInstalledAppForValidation(APP_NAME);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files and common jar file. Each war files contains one LdapIdentityStoreDefinision,
     *      and one FormHttpAuthenticationMechanismDefinision which points to different form. There is an identitystore in the jar
     *      file which is placed as a library of the ear.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> users in the common ear identitystore is visible from both war file
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWithCommonJar() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // create module1, form login, redirect, ldap1. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR1_NAME, true, null, false, "web.war.servlets.form.get.redirect", "web.war.identitystores.ldap.ldap1","web.war.identitystores.custom.grouponly");
        // create module2, custom form login, forward, ldap2. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR2CUSTOM_NAME, true, null, false, "web.war.servlets.customform", "web.war.servlets.customform.get.forward", "web.war.identitystores.ldap.ldap2","web.war.identitystores.custom.grouponly");
        WCApplicationHelper.createJar(myServer, TEMP_DIR, IS_JAR_NAME, true, "web.jar.base", "web.jar.common.identitystores");

        EnterpriseArchive ear = WCApplicationHelper.createEar(myServer, TEMP_DIR, EAR_NAME, true);
        WCApplicationHelper.packageWars(myServer, TEMP_DIR, ear, WAR1_NAME, WAR2CUSTOM_NAME);
        WCApplicationHelper.packageJars(myServer, TEMP_DIR, ear, IS_JAR_NAME);
        WCApplicationHelper.exportEar(myServer, TEMP_DIR, ear);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EAR_NAME);

        startServer(XML_NAME, APP_NAME);

        // ------------- accessing module1 ---------------
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, "commonuser1", LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + "commonuser1", Constants.getRemoteUserFound + "commonuser1");
        verifyRealm(response, "CommonIdentityStore");
        verifyNotInGroups(response, "group:localhost:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:CommonIdentityStore/grantedgroup2, group:CommonIdentityStore/commonGroup2, group:CommonIdentityStore/commonGroup1, group:CommonIdentityStore/grantedgroup");

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // ------------- accessing module2 ---------------
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);

        // Execute Form login and get redirect location.
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, "commonuser2", LocalLdapServer.PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + "commonuser2", Constants.getRemoteUserFound + "commonuser2");
        verifyRealm(response, "CommonIdentityStore");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:CommonIdentityStore/grantedgroup2, group:CommonIdentityStore/commonGroup2, group:CommonIdentityStore/commonGroup1, group:CommonIdentityStore/grantedgroup");

        myServer.removeInstalledAppForValidation(APP_NAME);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains only one FormHttpAuthenticationMechanismDefinition in a jar file which is stored as a library
     *      in a war file. There are two modules in the package of which has one LdapIdentityStoreDefinision and one custom identitystore.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> 
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWithModuleHAMJar() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // create module1, ldap1. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR1_NAME, true, HAM_JAR_NAME, true, "web.jar.base", "web.war.servlets.secured", "web.war.identitystores.ldap.ldap1","web.war.identitystores.custom.grouponly", "web.jar.mechanisms.form.get.forward");
        // create module2, custom form login, forward, ldap2. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR2_NAME, true, HAM_JAR_NAME, true, "web.jar.base", "web.war.servlets.secured", "web.war.identitystores.ldap.ldap2","web.war.identitystores.custom.grouponly", "web.jar.mechanisms.form.get.forward");

        EnterpriseArchive ear = WCApplicationHelper.createEar(myServer, TEMP_DIR, EAR2_NAME, true);
        WCApplicationHelper.packageWars(myServer, TEMP_DIR, ear, WAR1_NAME, WAR2_NAME);
        WCApplicationHelper.exportEar(myServer, TEMP_DIR, ear);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EAR2_NAME);

        startServer(XML2_NAME, APP2_NAME);

        // ------------- accessing module1 ---------------
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + COMMON_APP1_SERVLET, false, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + COMMON_APP1_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "127.0.0.1:10389");
        verifyNotInGroups(response, "group:localhost:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/cn=group1,ou=groups,o=ibm,c=us, group:127.0.0.1:10389/grantedgroup");

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // ------------- accessing module2 ---------------
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + COMMON_APP2_SERVLET, false, urlBase + MODULE2_LOGIN, MODULE2_TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        location = executeFormLogin(httpclient, urlBase + MODULE2_LOGINFORM, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + COMMON_APP2_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, Constants.getRemoteUserFound + LocalLdapServer.ANOTHERUSER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/cn=anothergroup1,ou=anothergroups,o=ibm,c=us, group:localhost:10389/grantedgroup");

        myServer.removeInstalledAppForValidation(APP2_NAME);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains only one FormHttpAuthenticationMechanismDefinition in a jar file which is stored as a library
     *      in an ear file. There are two modules in the package of which has one LdapIdentityStoreDefinision and one custom identitystore.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> 
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWithCommonHAMJar() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // create module1, ldap1. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR1_NAME, true, null, false, "web.war.servlets.secured", "web.war.identitystores.ldap.ldap1","web.war.identitystores.custom.grouponly");
        // create module2, custom form login, forward, ldap2. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR2_NAME, true, null, false, "web.war.servlets.secured", "web.war.identitystores.ldap.ldap2","web.war.identitystores.custom.grouponly");
        WCApplicationHelper.createJar(myServer, TEMP_DIR, HAM_JAR_NAME, true, "web.jar.base", "web.jar.mechanisms.form.get.forward");

        EnterpriseArchive ear = WCApplicationHelper.createEar(myServer, TEMP_DIR, EAR2_NAME, true);
        WCApplicationHelper.packageWars(myServer, TEMP_DIR, ear, WAR1_NAME, WAR2_NAME);
        WCApplicationHelper.packageJars(myServer, TEMP_DIR, ear, HAM_JAR_NAME);
        WCApplicationHelper.exportEar(myServer, TEMP_DIR, ear);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EAR2_NAME);

        startServer(XML2_NAME, APP2_NAME);

        // ------------- accessing module1 ---------------
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + COMMON_APP1_SERVLET, false, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + COMMON_APP1_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "127.0.0.1:10389");
        verifyNotInGroups(response, "group:localhost:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/cn=group1,ou=groups,o=ibm,c=us, group:127.0.0.1:10389/grantedgroup");

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // ------------- accessing module2 ---------------
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + COMMON_APP2_SERVLET, false, urlBase + MODULE2_LOGIN, MODULE2_TITLE_LOGIN_PAGE);

        // Execute Form login and get redirect location.
        location = executeFormLogin(httpclient, urlBase + MODULE2_LOGINFORM, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + COMMON_APP2_SERVLET);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, Constants.getRemoteUserFound + LocalLdapServer.ANOTHERUSER1);
        verifyRealm(response, "localhost:10389");
        verifyNotInGroups(response, "group:127.0.0.1:10389/");  // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:localhost:10389/grantedgroup2, group:localhost:10389/cn=anothergroup1,ou=anothergroups,o=ibm,c=us, group:localhost:10389/grantedgroup");

        myServer.removeInstalledAppForValidation(APP2_NAME);
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
