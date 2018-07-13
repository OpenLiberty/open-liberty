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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
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

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@MinimumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class EJBModuleRealmTest extends JavaEESecTestBase {
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = EJBModuleTestUnprotectedServlet.class;
    protected static String urlBase;
    protected static String TEMP_DIR = "test_temp";
    protected static String EJB_BEAN_JAR_NAME = "SecurityEJBinWAR.jar";
    protected static String EJB_REALM1_WAR_NAME = "AnnotatedEjbinWarServletLdapRealm1.war";
    protected static String EJB_REALM1_WAR_PATH = "/AnnotatedEjbinWarServletLdapRealm1/";
    protected static String EJB_REALM2_WAR_NAME = "AnnotatedEjbinWarServletLdapRealm2.war";
    protected static String EJB_REALM2_WAR_PATH = "/AnnotatedEjbinWarServletLdapRealm2/";
    protected static String EJB_EAR_REALM_NAME = "securityejbinwarrealm.ear";
    protected static String EJB_REALM_APP_NAME = EJB_EAR_REALM_NAME;
    protected static String EJB_EAR_REALM2_NAME = "securityejbinwarrealm2.ear";
    protected static String EJB_REALM2_APP_NAME = EJB_EAR_REALM2_NAME;
    protected static String XML_NAME = "ejbprotectedserver.xml";
    protected static String XML_REALM_NAME = "ejbprotectedrealmserver.xml";
    protected static String XML_INCORRECT_REALM = "ejbprotectedrealmserverincorrectrealm.xml";
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String SIMPLE_SERVLET_REALM1 = "SimpleServletRealm1";
    protected static String SIMPLE_SERVLET_REALM2 = "SimpleServletRealm2";
    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public EJBModuleRealmTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(logClass, "setUp()", "-----setting up test");
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

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    protected static void startServer(String config, String appName, String appName2) throws Exception {
        myServer.setServerConfigurationFile(config);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(appName);
        myServer.addInstalledAppForValidation(appName2);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two servlets, the other one contains one servlet.
     * Each war files has one jar file.
     * In this test case the following configuration will be used.
     * 1. WAR 1 will use the LDAP Identity Store.
     * 2. WAR 2 will use the LDAP2 Identity Store.
     * </OL>
     * <P> Users will be able to access the application since they are using the correct realm.
     * <OL>
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBAnnotatedLdapISRealmOnWar1andWar2CorrectRealm() throws Exception {
        String response;
        String queryString;
        //create app and setup server
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        Log.info(logClass, getCurrentTestName(), "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_REALM1_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean",
                                      "web.war.ejb.annotated.servlet.realm1", "web.war.identitystores.ldap.ldap1");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_REALM2_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base",
                                      "web.ejb.jar.bean", "web.war.ejb.annotated.servlet.realm2", "web.war.identitystores.ldap.ldap2");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_REALM_NAME, true, EJB_REALM1_WAR_NAME);
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_REALM2_NAME, true, EJB_REALM2_WAR_NAME);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_REALM_NAME);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_REALM2_NAME);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat2.war", true, JAR_NAME, false, "web.jar.base", "web.war.db2");
        Log.info(logClass, "setUp()", "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        startServer(XML_REALM_NAME, EJB_REALM_APP_NAME, EJB_REALM2_APP_NAME);

        //Test case USER1AccessEAR1
        //Access WAR 1 and check UserInRole, sending user1 which exist in the Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running USER1AccessEAR1 scenario");
        queryString = EJB_REALM1_WAR_PATH + SIMPLE_SERVLET_REALM1 + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbUserResponse(response, Constants.getEJBBeanResponse + Constants.ejb03Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodManager,
                              Constants.getEjbCallerPrincipal + LocalLdapServer.USER1);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting USER1AccessEAR1");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case USER1AccessEAR2
        //Access EAR 2 and check UserInRole, sending user1 which does exist in the Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running USER1AccessEAR2 scenario");
        queryString = EJB_REALM2_WAR_PATH + SIMPLE_SERVLET_REALM2 + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                   LocalLdapServer.ANOTHERPASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbUserResponse(response, Constants.getEJBBeanResponse + Constants.ejb03Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodManager,
                              Constants.getEjbCallerPrincipal + LocalLdapServer.USER1);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting USER1AccessEAR2");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        myServer.removeInstalledAppForValidation(EJB_REALM_APP_NAME);
        myServer.removeInstalledAppForValidation(EJB_REALM2_APP_NAME);
        myServer.stopServer();
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two servlets, the other one contains one servlet.
     * Each war files has one jar file.
     * In this test case the following configuration will be used.
     * 1. WAR 1 will use the LDAP Identity Store.
     * 2. WAR 2 will use the LDAP2 Identity Store.
     * </OL>
     * <P> Users will fail to access the apps because of wrong realm.
     * <OL>
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBAnnotatedLdapISRealmOnWar1andWar2IncorrectRealm() throws Exception {
        String response;
        String queryString;
        //create app and setup server
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        Log.info(logClass, getCurrentTestName(), "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_REALM1_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean",
                                      "web.war.ejb.annotated.servlet.realm1", "web.war.identitystores.ldap.ldap1");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_REALM2_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base",
                                      "web.ejb.jar.bean", "web.war.ejb.annotated.servlet.realm2", "web.war.identitystores.ldap.ldap2");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_REALM_NAME, true, EJB_REALM1_WAR_NAME);
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_REALM2_NAME, true, EJB_REALM2_WAR_NAME);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_REALM_NAME);
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_REALM2_NAME);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat2.war", true, JAR_NAME, false, "web.jar.base", "web.war.db2");
        Log.info(logClass, "setUp()", "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        startServer(XML_INCORRECT_REALM, EJB_REALM_APP_NAME, EJB_REALM2_APP_NAME);

        //Test case USER1AccessEAR1
        //Access WAR 1 and check UserInRole, sending user1 which exist in the Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running USER1AccessEAR1 scenario");
        queryString = EJB_REALM1_WAR_PATH + SIMPLE_SERVLET_REALM1 + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbErrorUserResponse(response, Constants.ejbAuthorizationFailed);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting USER1AccessEAR1");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case USER1AccessEAR2
        //Access EAR 2 and check UserInRole, sending user1 which does exist in the Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running USER1AccessEAR2 scenario");
        queryString = EJB_REALM2_WAR_PATH + SIMPLE_SERVLET_REALM2 + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                   LocalLdapServer.ANOTHERPASSWORD,
                                                   HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbErrorUserResponse(response, Constants.ejbAuthorizationFailed);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting USER1AccessEAR2");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        myServer.removeInstalledAppForValidation(EJB_REALM_APP_NAME);
        myServer.removeInstalledAppForValidation(EJB_REALM2_APP_NAME);
        myServer.stopServer();
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

}
