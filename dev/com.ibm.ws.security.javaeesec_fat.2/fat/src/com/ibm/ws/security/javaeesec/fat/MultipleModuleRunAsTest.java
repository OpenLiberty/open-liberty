/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MultipleModuleRunAsTest extends JavaEESecTestBase {

    protected static String portNumber = System.getProperty("ldap.1.port");
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = MultipleModuleRunAsTest.class;
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
    protected static String MODULE3_ROOT = "multipleModule3";
    protected static String MODULE3_NAME = "NoJavaEESecRunAs";
    protected static String WAR3_NAME = MODULE3_NAME + ".war";
    protected static String XML_BASE_NAME = "multipleModuleBasicRegBase.xml";
    protected static String XML_NAME = "multipleModuleRunAs.xml";
    protected static String XML_FALLBACK_NAME = "multipleModuleRunAsFallBack.xml";
    protected static String XML_MIXED_NAME = "multipleModuleRunAsMixed.xml";
    protected static String XML_APPBNDXML_NAME = "multipleModuleRunAsMixedAppBndXml.xml";
    protected static String APP_NAME = "multipleModuleRunAs";
    protected static String APP_MIXED_NAME = "multipleModuleRunAsMixed";
    protected static String APP_APPBNDXML_NAME = "multipleModuleRunAsMixedAppBndXml";
    protected static String EAR_NAME = APP_NAME + ".ear";
    protected static String EAR_MIXED_NAME = APP_MIXED_NAME + ".ear";
    protected static String EAR_APPBNDXML_NAME = APP_APPBNDXML_NAME + ".ear";
    protected static String APP1_SERVLET = "/" + MODULE1_ROOT + "/MultipleISFormRunAsServlet";
    protected static String APP2_SERVLET = "/" + MODULE2_ROOT + "/MultipleISCustomFormRunAsServlet";
    protected static String APP3_SERVLET = "/" + MODULE3_ROOT + "/NoJavaEESecRunAsServlet";

    protected static String MODULE1_LOGIN = "/" + MODULE1_ROOT + "/login.jsp";
    protected static String MODULE1_LOGINFORM = "/" + MODULE1_ROOT + "/j_security_check";
    protected static String MODULE2_CUSTOMLOGIN = "/" + MODULE2_ROOT + "/customLogin.xhtml";
    protected static String MODULE1_TITLE_LOGIN_PAGE = "login page for the form login test";
    protected static String MODULE2_TITLE_CUSTOMLOGIN_PAGE = "Custom Login Sample by using JSF";

    protected static String REALM1_USER = "realm1user";
    protected static String REALM1_PASSWORD = "s3cur1ty";
    protected static String REALM2_USER = "realm2user";
    protected static String REALM2_PASSWORD = "s3cur1ty";
    protected static String BASIC_USER1 = "basicuser1";
    protected static String BASIC_USER1_PASSWORD = "s3cur1ty";
    protected static String BASIC_RUNASUSER1 = "basicrunasuser1";
    protected static String BASIC_RUNASUSER1_PASSWORD = "s3cur1ty";

    protected static String IS1_REALM_NAME = "127.0.0.1:" + portNumber;
    protected static String IS2_REALM_NAME = "localhost:" + portNumber;
    protected static String REALM1_REALM_NAME = "Realm1";
    protected static String REALM2_REALM_NAME = "Realm2";
    protected static String BASIC_REALM_NAME = "NoJavaEESecRealm";

    protected static String IS1_GROUP_REALM_NAME = "group:127.0.0.1:" + portNumber + "/";
    protected static String IS2_GROUP_REALM_NAME = "group:localhost:" + portNumber + "/";

    protected static String IS1_GROUPS = "group:127.0.0.1:" + portNumber + "/grantedgroup2, group:127.0.0.1:" + portNumber + "/grantedgroup, group:127.0.0.1:" + portNumber
                                         + "/group1";
    protected static String IS2_GROUPS = "group:localhost:" + portNumber + "/grantedgroup2, group:localhost:" + portNumber + "/anothergroup1, group:localhost:" + portNumber
                                         + "/grantedgroup";
    protected static String REALM1_GROUPS = "group:Realm1/grantedgroup2, group:Realm1/grantedgroup, group:Realm1/realm1group1, group:Realm1/realm1group2";
    protected static String REALM2_GROUPS = "group:Realm2/grantedgroup2, group:Realm2/realm2group2, group:Realm2/realm2group1, group:Realm2/grantedgroup";
    protected static String BASIC_GROUPS = "group:NoJavaEESecRealm/basicgroup1";
    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public MultipleModuleRunAsTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        ldapServer = new LocalLdapServer();
        ldapServer.start();

        myServer.setServerConfigurationFile(XML_BASE_NAME);
        myServer.startServer(true);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer("CWWKS0005E");
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
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> In this case, the IdentityStores which are defined by LdapIdentityStoreDefinision are visible from any module, however,
     * the one which are bundled with each module is only visible within the module.
     * <LI> runas user is properly set.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWarsAllRunAs() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        assumeNotWindowsEe9();

        // create module1, form login, redirect, ldap1. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR1_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.redirectrunas",
                                      "web.war.identitystores.ldap.ldap1", "web.war.identitystores.custom.grouponly", "web.war.identitystores.custom.realm1",
                                      "web.war.identitystores.ldap");
        // create module2, custom form login, forward, ldap2. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR2CUSTOM_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.customform",
                                      "web.war.servlets.customform.get.forwardrunas", "web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly",
                                      "web.war.identitystores.custom.realm2", "web.war.identitystores.ldap");

        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EAR_NAME, true, WAR1_NAME, WAR2CUSTOM_NAME);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EAR_NAME);

        myServer.setServerConfigurationFile(XML_NAME);
        myServer.addInstalledAppForValidation(APP_NAME);

        // ------------- accessing module1 ---------------
        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyResponse(response, LocalLdapServer.USER1, IS1_REALM_NAME, IS2_GROUP_REALM_NAME, IS1_GROUPS, LocalLdapServer.RUNASUSER1);
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on the other module.

        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        // since runas user is authenticated by IS1, the realm name of IS1 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, LocalLdapServer.ANOTHERUSER1, IS2_REALM_NAME, null, IS2_GROUPS, LocalLdapServer.RUNASUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.
        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, REALM1_USER, REALM1_PASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        // since runas user is authenticated by IS1, the realm name of IS1 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, REALM1_USER, REALM1_REALM_NAME, null, REALM1_GROUPS, LocalLdapServer.RUNASUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // ------------- accessing module2 ---------------
        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyResponse(response, LocalLdapServer.ANOTHERUSER1, IS2_REALM_NAME, IS1_GROUP_REALM_NAME, IS2_GROUPS, LocalLdapServer.ANOTHERRUNASUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in another module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        // since runas user is authenticated by IS2, the realm name of IS2 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, LocalLdapServer.USER1, IS1_REALM_NAME, null, IS1_GROUPS, LocalLdapServer.ANOTHERRUNASUSER1);
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, REALM2_USER, REALM2_PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        // since runas user is authenticated by IS2, the realm name of IS2 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, REALM2_USER, REALM2_REALM_NAME, null, REALM2_GROUPS, LocalLdapServer.ANOTHERRUNASUSER1);
        httpclient.getConnectionManager().shutdown();

        myServer.setMarkToEndOfLog();
        myServer.setServerConfigurationFile(XML_BASE_NAME);
        myServer.removeInstalledAppForValidation(APP_NAME);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * <LI> the runas user setting has either invalid user name, or invalid password. With that, the runas subject will fall back
     * to the caller subject.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> In this case, the IdentityStores which are defined by LdapIdentityStoreDefinision are visible from any module, however,
     * the one which are bundled with each module is only visible within the module.
     * <LI> runas user falls back to the caller subject.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC("javax.naming.AuthenticationException")
    public void testMultipleModuleWarsAllRunAsFallBackToCaller() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        assumeNotWindowsEe9();

        // create module1, form login, redirect, ldap1. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR1_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.redirectrunas",
                                      "web.war.identitystores.ldap.ldap1", "web.war.identitystores.custom.grouponly", "web.war.identitystores.custom.realm1",
                                      "web.war.identitystores.ldap");
        // create module2, custom form login, forward, ldap2. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR2CUSTOM_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.customform",
                                      "web.war.servlets.customform.get.forwardrunas", "web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly",
                                      "web.war.identitystores.custom.realm2", "web.war.identitystores.ldap");

        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EAR_NAME, true, WAR1_NAME, WAR2CUSTOM_NAME);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EAR_NAME);

        myServer.setServerConfigurationFile(XML_FALLBACK_NAME);
        myServer.addInstalledAppForValidation(APP_NAME);

        // ------------- accessing module1 ---------------
        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        // make sure that runas subject is set as caller subject.
        verifyResponse(response, LocalLdapServer.USER1, IS1_REALM_NAME, IS2_GROUP_REALM_NAME, IS1_GROUPS, LocalLdapServer.USER1);
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on the other module.

        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        // make sure that runas subject is set as caller subject.
        verifyResponse(response, LocalLdapServer.ANOTHERUSER1, IS2_REALM_NAME, IS1_GROUP_REALM_NAME, IS2_GROUPS, LocalLdapServer.ANOTHERUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.
        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, REALM1_USER, REALM1_PASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        // make sure that runas subject is set as caller subject.
        verifyResponse(response, REALM1_USER, REALM1_REALM_NAME, IS1_GROUP_REALM_NAME, REALM1_GROUPS, REALM1_USER);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // ------------- accessing module2 ---------------
        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        // make sure that runas subject is set as caller subject.
        verifyResponse(response, LocalLdapServer.ANOTHERUSER1, IS2_REALM_NAME, IS1_GROUP_REALM_NAME, IS2_GROUPS, LocalLdapServer.ANOTHERUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in another module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        // make sure that runas subject is set as caller subject.
        verifyResponse(response, LocalLdapServer.USER1, IS1_REALM_NAME, IS2_GROUP_REALM_NAME, IS1_GROUPS, LocalLdapServer.USER1);
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, REALM2_USER, REALM2_PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        // make sure that runas subject is set as caller subject.
        verifyResponse(response, REALM2_USER, REALM2_REALM_NAME, IS2_GROUP_REALM_NAME, REALM2_GROUPS, REALM2_USER);
        httpclient.getConnectionManager().shutdown();

        myServer.setMarkToEndOfLog();
        myServer.setServerConfigurationFile(XML_BASE_NAME);
        myServer.removeInstalledAppForValidation(APP_NAME);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains three war files. two war files contain one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form, the one war file does not contain any JSR375
     * code.
     * <LI> RunAs user is configured for all war files.
     * <LI> UserRegistry is configured for non JSR375 war file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> In this case, the IdentityStores which are defined by LdapIdentityStoreDefinision are visible from any module, however,
     * the one which are bundled with each module is only visible within the module.
     * <LI> runas user is properly set.
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testMultipleModuleWarsWithNoJavaEESecWarAllRunAs() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        assumeNotWindowsEe9();

        // create module1, form login, redirect, ldap1. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR1_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.redirectrunas",
                                      "web.war.identitystores.ldap.ldap1", "web.war.identitystores.custom.grouponly", "web.war.identitystores.custom.realm1",
                                      "web.war.identitystores.ldap");
        // create module2, custom form login, forward, ldap2. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR2CUSTOM_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.customform",
                                      "web.war.servlets.customform.get.forwardrunas", "web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly",
                                      "web.war.identitystores.custom.realm2", "web.war.identitystores.ldap");

        // create module3, non JSR375 form login
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR3_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.nojavaeesec", "web.war.servlets.nojavaeesec.runas");

        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EAR_MIXED_NAME, true, WAR1_NAME, WAR2CUSTOM_NAME, WAR3_NAME);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EAR_MIXED_NAME);

        myServer.setServerConfigurationFile(XML_MIXED_NAME);
        myServer.addInstalledAppForValidation(APP_MIXED_NAME);

        // ------------- accessing module1 ---------------
        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyResponse(response, LocalLdapServer.USER1, IS1_REALM_NAME, IS2_GROUP_REALM_NAME, IS1_GROUPS, LocalLdapServer.RUNASUSER1);
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on the other module.

        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        // since runas user is authenticated by IS1, the realm name of IS1 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, LocalLdapServer.ANOTHERUSER1, IS2_REALM_NAME, null, IS2_GROUPS, LocalLdapServer.RUNASUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.
        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, REALM1_USER, REALM1_PASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        // since runas user is authenticated by IS1, the realm name of IS1 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, REALM1_USER, REALM1_REALM_NAME, null, REALM1_GROUPS, LocalLdapServer.RUNASUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // ------------- accessing module2 ---------------
        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyResponse(response, LocalLdapServer.ANOTHERUSER1, IS2_REALM_NAME, IS1_GROUP_REALM_NAME, IS2_GROUPS, LocalLdapServer.ANOTHERRUNASUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in another module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        // since runas user is authenticated by IS2, the realm name of IS2 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, LocalLdapServer.USER1, IS1_REALM_NAME, null, IS1_GROUPS, LocalLdapServer.ANOTHERRUNASUSER1);
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, REALM2_USER, REALM2_PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        // since runas user is authenticated by IS2, the realm name of IS2 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, REALM2_USER, REALM2_REALM_NAME, null, REALM2_GROUPS, LocalLdapServer.ANOTHERRUNASUSER1);
        httpclient.getConnectionManager().shutdown();

        setupConnection();
        // ------------- accessing module3 ---------------
        // Execute Basic login for Non JavaEESec servlet.
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + APP3_SERVLET, BASIC_USER1, BASIC_USER1_PASSWORD, HttpServletResponse.SC_OK);

        verifyResponse(response, BASIC_USER1, BASIC_REALM_NAME, IS1_GROUP_REALM_NAME, BASIC_GROUPS, BASIC_RUNASUSER1);
        httpclient.getConnectionManager().shutdown();

        myServer.setMarkToEndOfLog();
        myServer.setServerConfigurationFile(XML_BASE_NAME);
        myServer.removeInstalledAppForValidation(APP_MIXED_NAME);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains three war files. two war files contain one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form, the one war file does not contain any JSR375
     * code.
     * <LI> RunAs user is configured for all war files.
     * <LI> UserRegistry is configured for non JSR375 war file.
     * <LI> ibm-application-bnd.xml file is packaged with ear file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> In this case, the IdentityStores which are defined by LdapIdentityStoreDefinision are visible from any module, however,
     * the one which are bundled with each module is only visible within the module.
     * <LI> runas user is properly set.
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testMultipleModuleWarsWithNoJavaEESecWarAllRunAsAppBndXml() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        assumeNotWindowsEe9();

        // create module1, form login, redirect, ldap1. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR1_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.redirectrunas",
                                      "web.war.identitystores.ldap.ldap1", "web.war.identitystores.custom.grouponly", "web.war.identitystores.custom.realm1",
                                      "web.war.identitystores.ldap");
        // create module2, custom form login, forward, ldap2. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR2CUSTOM_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.customform",
                                      "web.war.servlets.customform.get.forwardrunas", "web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly",
                                      "web.war.identitystores.custom.realm2", "web.war.identitystores.ldap");

        // create module3, non JSR375 form login
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR3_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.nojavaeesec", "web.war.servlets.nojavaeesec.runas");

        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EAR_APPBNDXML_NAME, true, WAR1_NAME, WAR2CUSTOM_NAME, WAR3_NAME);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EAR_APPBNDXML_NAME);

        myServer.setServerConfigurationFile(XML_APPBNDXML_NAME);
        myServer.addInstalledAppForValidation(APP_APPBNDXML_NAME);

        // ------------- accessing module1 ---------------
        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        String response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyResponse(response, LocalLdapServer.USER1, IS1_REALM_NAME, IS2_GROUP_REALM_NAME, IS1_GROUPS, LocalLdapServer.RUNASUSER1);
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for LdapIdentityStoreDefinision on the other module.

        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        // since runas user is authenticated by IS1, the realm name of IS1 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, LocalLdapServer.ANOTHERUSER1, IS2_REALM_NAME, null, IS2_GROUPS, LocalLdapServer.RUNASUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.
        response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, REALM1_USER, REALM1_PASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        // since runas user is authenticated by IS1, the realm name of IS1 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, REALM1_USER, REALM1_REALM_NAME, null, REALM1_GROUPS, LocalLdapServer.RUNASUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // ------------- accessing module2 ---------------
        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyResponse(response, LocalLdapServer.ANOTHERUSER1, IS2_REALM_NAME, IS1_GROUP_REALM_NAME, IS2_GROUPS, LocalLdapServer.ANOTHERRUNASUSER1);

        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location with a user which exists in ldapidentitystore definision in another module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        // since runas user is authenticated by IS2, the realm name of IS2 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, LocalLdapServer.USER1, IS1_REALM_NAME, null, IS1_GROUPS, LocalLdapServer.ANOTHERRUNASUSER1);
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        // Execute Form login and get redirect location for custom identity store in this module.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, REALM2_USER, REALM2_PASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        // since runas user is authenticated by IS2, the realm name of IS2 shows up in the response, therefore skip validating invalid realm.
        verifyResponse(response, REALM2_USER, REALM2_REALM_NAME, null, REALM2_GROUPS, LocalLdapServer.ANOTHERRUNASUSER1);
        httpclient.getConnectionManager().shutdown();

        setupConnection();
        // ------------- accessing module3 ---------------
        // Execute Basic login for Non JavaEESec servlet.
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + APP3_SERVLET, BASIC_USER1, BASIC_USER1_PASSWORD, HttpServletResponse.SC_OK);

        verifyResponse(response, BASIC_USER1, BASIC_REALM_NAME, IS1_GROUP_REALM_NAME, BASIC_GROUPS, BASIC_RUNASUSER1);
        httpclient.getConnectionManager().shutdown();

        myServer.setMarkToEndOfLog();
        myServer.setServerConfigurationFile(XML_BASE_NAME);
        myServer.removeInstalledAppForValidation(APP_APPBNDXML_NAME);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

/* ------------------------ support methods ---------------------- */
    protected String getViewState(String form) {
        Pattern p = Pattern.compile("[\\s\\S]*value=\"(.+)\".*autocomplete[\\s\\S]*");
        Matcher m = p.matcher(form);
        String viewState = null;
        if (m.matches()) {
            viewState = m.group(1);
        }
        return viewState;
    }

    protected void verifyResponse(String response, String user, String realm, String invalidRealm, String groups, String runas) {
        verifyUserResponse(response, Constants.getUserPrincipalFound + user, Constants.getRemoteUserFound + user);
        verifyRealm(response, realm);
        if (invalidRealm != null) {
            verifyNotInGroups(response, invalidRealm); // make sure that there is no realm name from the second IdentityStore.
        }
        verifyGroups(response, groups);
        verifyRunAsUserResponse(response, runas);
    }

}
