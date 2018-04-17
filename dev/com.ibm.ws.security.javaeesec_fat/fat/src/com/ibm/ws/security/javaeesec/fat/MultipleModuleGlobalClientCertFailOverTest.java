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

import java.io.File;
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
import com.ibm.ws.security.javaeesec.fat_helper.SSLHelper;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@MinimumJavaLevel(javaLevel = 1.8, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MultipleModuleGlobalClientCertFailOverTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.clientcert.fat");
    protected static Class<?> logClass = MultipleModuleGlobalClientCertFailOverTest.class;
    protected static String urlBase;
    protected static String TEMP_DIR = "test_temp";
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String MODULE1_ROOT = "multipleModule1";
    protected static String MODULE1_NAME = "JavaEESecMultipleISForm";
    protected static String WAR1_NAME = MODULE1_NAME + ".war";
    protected static String MODULE2_ROOT = "multipleModule2";
    protected static String MODULE2CUSTOM_NAME = "JavaEESecMultipleISCustomForm";
    protected static String WAR2CUSTOM_NAME = MODULE2CUSTOM_NAME + ".war";
    protected static String XML_CLIENT_CERT_FAILOVER_TO_BA_NAME = "globalClientCertFailOverToBA.xml";
    protected static String XML_CLIENT_CERT_FAILOVER_TO_FORM_NAME = "globalClientCertFailOverToForm.xml";
    protected static String APP_NAME = "multipleModule";
    protected static String EAR_NAME = APP_NAME + ".ear";
    protected static String APP1_SERVLET = "/" + MODULE1_ROOT + "/FormServlet";
    protected static String APP2_SERVLET = "/" + MODULE2_ROOT + "/MultipleISCustomFormServlet";

    protected static String MODULE1_LOGIN = "/" + MODULE1_ROOT + "/login.jsp";
    protected static String MODULE1_LOGINFORM = "/" + MODULE1_ROOT + "/j_security_check";
    protected static String MODULE2_CUSTOMLOGIN = "/" + MODULE2_ROOT + "/customLogin.xhtml";
    protected static String MODULE1_TITLE_LOGIN_PAGE = "login page for the form login test";
    protected static String MODULE2_TITLE_CUSTOMLOGIN_PAGE = "Custom Login Sample by using JSF";

    protected static String IS1_REALM_NAME = "127.0.0.1:10389";
    protected static String IS2_REALM_NAME = "localhost:10389";

    protected static String IS1_GROUP_REALM_NAME = "group:127.0.0.1:10389/";
    protected static String IS2_GROUP_REALM_NAME = "group:localhost:10389/";

    protected static String IS1_GROUPS = "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/grantedgroup, group:127.0.0.1:10389/group1";
    protected static String IS2_GROUPS = "group:localhost:10389/grantedgroup2, group:localhost:10389/anothergroup1, group:localhost:10389/grantedgroup";

    protected final static String CERTUSER1_KEYFILE = "certuser1.jks";
    protected final static String CERTUSER4_KEYFILE = "certuser4.jks";
    protected final static String KEYSTORE_PASSWORD = "s3cur1ty";
    protected final static String LDAP_UR_REALM_NAME = "MyLdapRealm";
    protected final static String LDAP_UR_GROUPS = "group:MyLdapRealm/cn=certgroup1,ou=groups,o=ibm,c=us";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public MultipleModuleGlobalClientCertFailOverTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        ldapServer = new LocalLdapServer();
        ldapServer.start();

        // create module1, form login, redirect, ldap1. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR1_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.form.get.redirect",
                                      "web.war.identitystores.ldap.ldap1", "web.war.identitystores.custom.grouponly", "web.war.identitystores.custom.realm1");
        // create module2, custom form login, forward, ldap2. grouponly.
        WCApplicationHelper.createWar(myServer, TEMP_DIR, WAR2CUSTOM_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.customform",
                                      "web.war.servlets.customform.get.forward", "web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly",
                                      "web.war.identitystores.custom.realm2");

        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EAR_NAME, true, WAR1_NAME, WAR2CUSTOM_NAME);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EAR_NAME);

        startServer(XML_CLIENT_CERT_FAILOVER_TO_BA_NAME, APP_NAME);
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
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    protected static void startServer(String config, String appName) throws Exception {
        myServer.setServerConfigurationFile(config);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(appName);
        urlBase = "https://" + myServer.getHostname() + ":" + myServer.getHttpDefaultSecurePort();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * <LI> The container overrides the authentication to client cert.
     * <LI> The client sends a valid certificate.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the client certificate login is carried out.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWarsOverrideClientCertWithFailOverToBASuccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_FAILOVER_TO_BA_NAME);
        // ------------- accessing module1 ---------------
        // No matter how to configure the app, it is overridden by the client cert authentication.
        setupClient(CERTUSER1_KEYFILE);
        String response = accessPageNoChallenge(httpclient, urlBase + APP1_SERVLET, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);

        verifyResponse(response, LocalLdapServer.CERTUSER1, LDAP_UR_REALM_NAME, null, LDAP_UR_GROUPS);
        httpclient.getConnectionManager().shutdown();
        // ------------- accessing module2 ---------------

        setupConnection();
        setupClient(CERTUSER1_KEYFILE);
        // No matter how to configure the app, it is overridden by the client cert authentication.
        response = accessPageNoChallenge(httpclient, urlBase + APP2_SERVLET, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyResponse(response, LocalLdapServer.CERTUSER1, LDAP_UR_REALM_NAME, null, LDAP_UR_GROUPS);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * <LI> The container overrides the authentication to client cert.
     * <LI> The client does not send a valid certificate.
     * <LI> Fail over to original login.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the login is successful with original login method.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWarsOverrideClientCertWithFailOverToBAFailOverSuccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_FAILOVER_TO_BA_NAME);
        // ------------- accessing module1 ---------------
        // since the certificate won't be sent, fallback to original  Form login and get redirect location for LdapIdentityStoreDefinision on this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        setupClient(CERTUSER4_KEYFILE);
        String response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyResponse(response, LocalLdapServer.USER1, IS1_REALM_NAME, IS2_GROUP_REALM_NAME, IS1_GROUPS);
        httpclient.getConnectionManager().shutdown();
        // ------------- accessing module2 ---------------

        setupConnection();
        setupClient(CERTUSER4_KEYFILE);
        // since the certificate won't be sent, custom form login and get redirect location with a user which exists in ldapidentitystore definision in this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyResponse(response, LocalLdapServer.ANOTHERUSER1, IS2_REALM_NAME, IS1_GROUP_REALM_NAME, IS2_GROUPS);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }


    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * <LI> The container overrides the authentication to client cert.
     * <LI> The client sends a valid certificate.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the client certificate login is carried out.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWarsOverrideClientCertWithFailOverToFormSuccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_FAILOVER_TO_FORM_NAME);
        // ------------- accessing module1 ---------------
        // No matter how to configure the app, it is overridden by the client cert authentication.
        setupClient(CERTUSER1_KEYFILE);
        String response = accessPageNoChallenge(httpclient, urlBase + APP1_SERVLET, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);

        verifyResponse(response, LocalLdapServer.CERTUSER1, LDAP_UR_REALM_NAME, null, LDAP_UR_GROUPS);
        httpclient.getConnectionManager().shutdown();
        // ------------- accessing module2 ---------------

        setupConnection();
        setupClient(CERTUSER1_KEYFILE);
        // No matter how to configure the app, it is overridden by the client cert authentication.
        response = accessPageNoChallenge(httpclient, urlBase + APP2_SERVLET, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyResponse(response, LocalLdapServer.CERTUSER1, LDAP_UR_REALM_NAME, null, LDAP_UR_GROUPS);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * <LI> The container overrides the authentication to client cert.
     * <LI> The client does not send a valid certificate.
     * <LI> Fail over to original login.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the login is successful with original login method.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWarsOverrideClientCertWithFailOverToFormFailOverSuccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_FAILOVER_TO_FORM_NAME);
        // ------------- accessing module1 ---------------
        // since the certificate won't be sent, fallback to original  Form login and get redirect location for LdapIdentityStoreDefinision on this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        setupClient(CERTUSER4_KEYFILE);
        String response = getFormLoginPage(httpclient, urlBase + APP1_SERVLET, true, urlBase + MODULE1_LOGIN, MODULE1_TITLE_LOGIN_PAGE);
        String location = executeFormLogin(httpclient, urlBase + MODULE1_LOGINFORM, LocalLdapServer.USER1, LocalLdapServer.PASSWORD, true);
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP1_SERVLET);
        verifyResponse(response, LocalLdapServer.USER1, IS1_REALM_NAME, IS2_GROUP_REALM_NAME, IS1_GROUPS);
        httpclient.getConnectionManager().shutdown();
        // ------------- accessing module2 ---------------

        setupConnection();
        setupClient(CERTUSER4_KEYFILE);
        // since the certificate won't be sent, custom form login and get redirect location with a user which exists in ldapidentitystore definision in this module.
        // Send servlet query to get form login page. Since auto redirect is disabled, if forward is not set, this would return 302 and location.
        response = getFormLoginPage(httpclient, urlBase + APP2_SERVLET, false, urlBase + MODULE2_CUSTOMLOGIN, MODULE2_TITLE_CUSTOMLOGIN_PAGE);
        location = executeCustomFormLogin(httpclient, urlBase + MODULE2_CUSTOMLOGIN, LocalLdapServer.ANOTHERUSER1, LocalLdapServer.ANOTHERPASSWORD, getViewState(response));
        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, urlBase + APP2_SERVLET);
        verifyResponse(response, LocalLdapServer.ANOTHERUSER1, IS2_REALM_NAME, IS1_GROUP_REALM_NAME, IS2_GROUPS);
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

    protected void verifyResponse(String response, String user, String realm, String invalidRealm, String groups) {
        verifyUserResponse(response, Constants.getUserPrincipalFound + user, Constants.getRemoteUserFound + user);
        verifyRealm(response, realm);
        if (invalidRealm != null) {
            verifyNotInGroups(response, invalidRealm); // make sure that there is no realm name from the second IdentityStore.
        }
        verifyGroups(response, groups);
    }

    private void setupClient(String certFile) {
        String ksFile = myServer.pathToAutoFVTTestFiles + "/clientcert/" + certFile;
        SSLHelper.establishSSLContext(httpclient, myServer.getHttpDefaultSecurePort(), myServer, ksFile, KEYSTORE_PASSWORD, null, null);
    }

}
