/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

import static org.junit.Assert.fail;

import javax.net.ssl.SSLPeerUnverifiedException;
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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MultipleModuleGlobalClientCertTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.clientcert.fat");
    protected static Class<?> logClass = MultipleModuleGlobalClientCertTest.class;
    protected static String urlBase;
    protected static String TEMP_DIR = "test_temp";
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String MODULE1_ROOT = "multipleModule1";
    protected static String MODULE1_NAME = "JavaEESecMultipleISForm";
    protected static String WAR1_NAME = MODULE1_NAME + ".war";
    protected static String MODULE2_ROOT = "multipleModule2";
    protected static String MODULE2CUSTOM_NAME = "JavaEESecMultipleISCustomForm";
    protected static String WAR2CUSTOM_NAME = MODULE2CUSTOM_NAME + ".war";
    protected static String XML_CLIENT_CERT_NO_FAILOVER_NAME = "globalClientCertNoFailOver.xml";
    protected static String XML_CLIENT_CERT_NO_FAILOVER_SUPPORT_NAME = "globalClientCertNoFailOverSupport.xml";
    protected static String APP_NAME = "multipleModule";
    protected static String EAR_NAME = APP_NAME + ".ear";
    protected static String APP1_SERVLET = "/" + MODULE1_ROOT + "/FormServlet";
    protected static String APP2_SERVLET = "/" + MODULE2_ROOT + "/MultipleISCustomFormServlet";

    protected final static String CERTUSER1_KEYFILE = "certuser1.jks";
    protected final static String CERTUSER2_KEYFILE = "certuser2.jks";
    protected final static String CERTUSER3_KEYFILE = "certuser3.jks";
    protected final static String CERTUSER4_KEYFILE = "certuser4.jks";
    protected final static String KEYSTORE_PASSWORD = "s3cur1ty";
    protected final static String LDAP_UR_REALM_NAME = "MyLdapRealm";
    protected final static String LDAP_UR_GROUPS = "group:MyLdapRealm/cn=certgroup1,ou=groups,o=ibm,c=us";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public MultipleModuleGlobalClientCertTest() {
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

        startServer(XML_CLIENT_CERT_NO_FAILOVER_NAME, APP_NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer("CWWKO0801E");
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

    protected static void startServer(String config, String appName) throws Exception {
        serverConfigurationFile = config;
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
     * <LI> The ssl setting on the server side requires client cert auth.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the client certificate login is carried out.
     * </OL>
     */
    @Test
    public void testMultipleModuleWarsOverrideClientCertRequiredSuccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_NO_FAILOVER_NAME, APP_NAME);
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
     * <LI> The client does not send a certificate.
     * <LI> The ssl setting on the server side requires client cert auth.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the exception is caught.
     * </OL>
     */
    @Test
    public void testMultipleModuleWarsOverrideClientCertRequiredNoCertFailure() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_NO_FAILOVER_NAME, APP_NAME);
        try {
            setupClient(CERTUSER4_KEYFILE);
            accessPageExpectException(httpclient, urlBase + APP1_SERVLET);
            fail("Excepted SSL error did not occur");
        } catch (SSLPeerUnverifiedException e) {
            Log.info(logClass, getCurrentTestName(), "SSLPeerUnverifiedException is caught which is expected.");
            // good.
        } catch (Exception e) {
            fail("Caught unexpected exception: " + e);
        }
        // ------------- accessing module2 ---------------
        // No matter how to configure the app, it is overridden by the client cert authentication.
        try {
            setupClient(CERTUSER4_KEYFILE);
            accessPageExpectException(httpclient, urlBase + APP2_SERVLET);
            fail("Excepted SSL error did not occur");
        } catch (SSLPeerUnverifiedException e) {
            Log.info(logClass, getCurrentTestName(), "SSLPeerUnverifiedException is caught which is expected.");
            // good.
        } catch (Exception e) {
            fail("Caught unexpected exception: " + e);
        }
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * <LI> The container overrides the authentication to client cert.
     * <LI> The client sends a valid certificate but not in a granted group.
     * <LI> The ssl setting on the server side requires client cert auth.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the 403 error is returned.
     * </OL>
     */
    @Test
    public void testMultipleModuleWarsOverrideClientCertValidCertRequiredAuthzFailure() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_NO_FAILOVER_NAME, APP_NAME);
        // setup client with invaliduser. certificate.
        // ------------- accessing module1 ---------------
        // No matter how to configure the app, it is overridden by the client cert authentication.
        setupClient(CERTUSER2_KEYFILE);
        accessPageNoChallenge(httpclient, urlBase + APP1_SERVLET, HttpServletResponse.SC_FORBIDDEN, urlBase + APP1_SERVLET);
        httpclient.getConnectionManager().shutdown();
        // ------------- accessing module2 ---------------

        setupConnection();
        setupClient(CERTUSER2_KEYFILE);
        // No matter how to configure the app, it is overridden by the client cert authentication.
        accessPageNoChallenge(httpclient, urlBase + APP2_SERVLET, HttpServletResponse.SC_FORBIDDEN, urlBase + APP2_SERVLET);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * <LI> The container overrides the authentication to client cert.
     * <LI> The client sends an invalid certificate which user does not exist in the ldap.
     * <LI> The ssl setting on the server side requires client cert auth.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the 403 error is returned.
     * </OL>
     */
    @Test
    public void testMultipleModuleWarsOverrideClientCertRequiredInvalidCertFailure() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_NO_FAILOVER_NAME, APP_NAME);
        // setup client with invaliduser. certificate.
        // ------------- accessing module1 ---------------
        // No matter how to configure the app, it is overridden by the client cert authentication.
        setupClient(CERTUSER3_KEYFILE);
        accessPageNoChallenge(httpclient, urlBase + APP1_SERVLET, HttpServletResponse.SC_FORBIDDEN, urlBase + APP1_SERVLET);
        httpclient.getConnectionManager().shutdown();
        // ------------- accessing module2 ---------------

        setupConnection();
        setupClient(CERTUSER3_KEYFILE);
        // No matter how to configure the app, it is overridden by the client cert authentication.
        accessPageNoChallenge(httpclient, urlBase + APP2_SERVLET, HttpServletResponse.SC_FORBIDDEN, urlBase + APP2_SERVLET);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * <LI> The container overrides the authentication to client cert.
     * <LI> The client sends a valid certificate.
     * <LI> The ssl setting on the server side supports client cert auth.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the client certificate login is carried out.
     * </OL>
     */
    @Test
    public void testMultipleModuleWarsOverrideClientCertSupportSuccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_NO_FAILOVER_SUPPORT_NAME, APP_NAME);
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
     * <LI> The client does not send a certificate.
     * <LI> The ssl setting on the server side supports client cert auth.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the exception is caught.
     * </OL>
     */
    @Test
    public void testMultipleModuleWarsOverrideClientCertSupportNoCertFailure() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_NO_FAILOVER_SUPPORT_NAME, APP_NAME);
        // setup client with invaliduser. certificate.
        // ------------- accessing module1 ---------------
        // No matter how to configure the app, it is overridden by the client cert authentication.
        setupClient(CERTUSER4_KEYFILE);
        accessPageNoChallenge(httpclient, urlBase + APP1_SERVLET, HttpServletResponse.SC_FORBIDDEN, urlBase + APP1_SERVLET);
        httpclient.getConnectionManager().shutdown();
        // ------------- accessing module2 ---------------

        setupConnection();
        setupClient(CERTUSER4_KEYFILE);
        // No matter how to configure the app, it is overridden by the client cert authentication.
        accessPageNoChallenge(httpclient, urlBase + APP2_SERVLET, HttpServletResponse.SC_FORBIDDEN, urlBase + APP2_SERVLET);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * <LI> The container overrides the authentication to client cert.
     * <LI> The client sends a valid certificate but not in a granted group.
     * <LI> The ssl setting on the server side supports client cert auth.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the 403 error is returned.
     * </OL>
     */
    @Test
    public void testMultipleModuleWarsOverrideClientCertSupportValidCertAuthzFailure() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_NO_FAILOVER_SUPPORT_NAME, APP_NAME);
        // setup client with invaliduser. certificate.
        // ------------- accessing module1 ---------------
        // No matter how to configure the app, it is overridden by the client cert authentication.
        setupClient(CERTUSER2_KEYFILE);
        accessPageNoChallenge(httpclient, urlBase + APP1_SERVLET, HttpServletResponse.SC_FORBIDDEN, urlBase + APP1_SERVLET);
        httpclient.getConnectionManager().shutdown();
        // ------------- accessing module2 ---------------

        setupConnection();
        setupClient(CERTUSER2_KEYFILE);
        // No matter how to configure the app, it is overridden by the client cert authentication.
        accessPageNoChallenge(httpclient, urlBase + APP2_SERVLET, HttpServletResponse.SC_FORBIDDEN, urlBase + APP2_SERVLET);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * <LI> The container overrides the authentication to client cert.
     * <LI> The client sends an invalid certificate which user does not exist in the ldap.
     * <LI> The ssl setting on the server side supports client cert auth.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Verify that the 403 error is returned.
     * </OL>
     */
    @Test
    public void testMultipleModuleWarsOverrideClientCertSupportInvalidCertFailure() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        setServerConfiguration(XML_CLIENT_CERT_NO_FAILOVER_SUPPORT_NAME, APP_NAME);
        // setup client with invaliduser. certificate.
        // ------------- accessing module1 ---------------
        // No matter how to configure the app, it is overridden by the client cert authentication.
        setupClient(CERTUSER3_KEYFILE);
        accessPageNoChallenge(httpclient, urlBase + APP1_SERVLET, HttpServletResponse.SC_FORBIDDEN, urlBase + APP1_SERVLET);
        httpclient.getConnectionManager().shutdown();
        // ------------- accessing module2 ---------------

        setupConnection();
        setupClient(CERTUSER3_KEYFILE);
        // No matter how to configure the app, it is overridden by the client cert authentication.
        accessPageNoChallenge(httpclient, urlBase + APP2_SERVLET, HttpServletResponse.SC_FORBIDDEN, urlBase + APP2_SERVLET);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

/* ------------------------ support methods ---------------------- */
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
