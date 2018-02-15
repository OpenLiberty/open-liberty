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

import static org.junit.Assert.assertNotNull;

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

@MinimumJavaLevel(javaLevel = 1.8, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)

public class EAREJBModuleTest extends JavaEESecTestBase {
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = AnnotatedEAREJBModuleTest.class;
    protected static String urlBase;
    protected static String TEMP_DIR = "test_temp";
    protected static String EJB_BEAN_JAR_NAME = "SecurityEJBinWAR.jar";
    protected static String EJB_SERVLET_NAME = "SecurityEJBBaseServlet";
    protected static String EJB_WAR_NAME = "EjbinWarServletISLdapDb.war";
    protected static String EJB_WAR_PATH = "/EjbinWarServletISLdapDb/";
    protected static String EJB_WAR_NAME2 = "EjbinWarServletISLdap.war";
    protected static String EJB_WAR2_PATH = "/EjbinWarServletISLdap/";
    protected static String EJB_EAR_NAME = "securityejbinwar2.ear";
    protected static String EJB_APP_NAME = EJB_EAR_NAME;
    protected static String XML_NAME = "ejbserver2.xml";
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String SIMPLE_SERVLET = "SimpleServlet";
    protected static String SIMPLE_SERVLET2 = "SimpleServlet2";
    protected static String RUNAS_SERVLET = "SimpleServletRunAs";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public EAREJBModuleTest() {
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

    protected static void startServer(String config, String appName) throws Exception {
        myServer.setServerConfigurationFile(config);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(appName);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two servlets, the other one contains one servlet.
     * Each war files has one jar file.
     * This test case uses EJB with the purpose of testing Basic Authentication with LDAP Identity Store in WAR1 (ejbinwarservlet).
     * </OL>
     * <P> Expected Results: 200 OK and isUserInRole(true).
     * <OL>
     * <LI>
     * </OL>
     */
    //@Mode(TestMode.LITE)
    //@Test
    public void testAnnotatedLDAPIS() throws Exception {
        //create app and setup server
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        Log.info(logClass, getCurrentTestName(), "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean", "web.war.ejb.is.servlet",
                                      "web.war.identitystores.ldap.ldap1");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME2, true, EJB_BEAN_JAR_NAME, true, "web.jar.base",
                                      "web.ejb.jar.bean", "web.war.ejb.is.servlet2", "web.war.identitystores.ldap.ldap2");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME, EJB_WAR_NAME2);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfatAuthAlias.war", true, JAR_NAME, false, "web.jar.base", "web.war.db");
        Log.info(logClass, getCurrentTestName(), "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        startServer(XML_NAME, EJB_APP_NAME);
        assertNotNull("Application CustomQueryDatabaseServlet does not appear to have started.",
                      myServer.waitForStringInLog("CWWKZ0001I: Application CustomQueryDatabaseServlet started"));

        //Test case isUserInRoleLDAPISWar1
        //Access WAR 1 and check UserInRole, sending user1 which exist in the Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running isUserInRoleLDAPISWar1 scenario");
        String queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, "isUserInRole(Manager): true");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isUserInRoleLDAPISWar1");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case isUserInRoleLDAPISWar2
        //Access WAR 2 and check for UserInRole, sending AnotherUser1 which exist in WAR 2 Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running isUserInRoleLDAPISWar2 scenario");
        queryString = EJB_WAR2_PATH + SIMPLE_SERVLET2 + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.ANOTHERUSER1,
                                                   LocalLdapServer.ANOTHERPASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, "isUserInRole(Manager): true");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isUserInRoleLDAPISWar2");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case isCallerInRoleLDAPWar1
        //Access WAR 1 and check for isCallerInRole sending user2 which exist in WAR 1 Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-----Running isCallerInRoleLDAPWar1 scenario");

        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=declareRoles01";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER2,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER2, "securityContext.isCallerInRole(DeclaredRole01): true");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isCallerInRoleLDAPWar1");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case testisCallerInRoleLDAPWar2
        //Access WAR 2 and check for isCaller in role sending user2 which exist in WAR 1 Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-----Running testisCallerInRoleLDAPWar2 scenario");
        queryString = EJB_WAR2_PATH + SIMPLE_SERVLET2 + "?testInstance=ejb03&testMethod=declareRoles01";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER2,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER2, "securityContext.isCallerInRole(DeclaredRole01): true");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting testisCallerInRoleLDAPWar2");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case testisUserNotInRoleLDAPWar1
        //Access WAR 1 and check for isUserInRole sending an invalid user.
        Log.info(logClass, getCurrentTestName(), "-----Running testisUserNotInRoleLDAPWar1 scenario");

        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.INVALIDUSER,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyExceptionResponse(response, "Error 403: AuthorizationFailed");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting testisUserNotInRoleLDAPWar1");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case testisUserInRoleBasicUserRegistryFallBackWar1WithAnnotations
        //Access WAR 1 and check for isUserInRole but then fails to fall back to UR.
        Log.info(logClass, getCurrentTestName(), "-----Running testisUserInRoleBasicUserRegistryFallBackWar1WithAnnotations scenario");
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user99",
                                                   "user99pwd",
                                                   HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        myServer.removeInstalledAppForValidation(EJB_APP_NAME);
        myServer.stopServer();
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two servlets, the other one contains one servlet.
     * Each war files has one jar file.
     * This test case uses EJB with the purpose of testing Basic Authentication with LDAP Identity Store in WAR1 (ejbinwarservlet).
     * </OL>
     * <P> Expected Results: 200 OK and isUserInRole(true).
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAnnotatedAndNonAnnotatedIS() throws Exception {
        //create app and setup server
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        Log.info(logClass, getCurrentTestName(), "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean", "web.war.ejb.is.servlet",
                                      "web.war.identitystores");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME2, true, EJB_BEAN_JAR_NAME, true, "web.jar.base",
                                      "web.ejb.jar.bean", "web.war.ejb.is.servlet2", "web.war.identitystores.ldap.ldap2");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME, EJB_WAR_NAME2);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfatAuthAlias.war", true, JAR_NAME, false, "web.jar.base", "web.war.db");
        Log.info(logClass, getCurrentTestName(), "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        startServer(XML_NAME, EJB_APP_NAME);
        assertNotNull("Application CustomQueryDatabaseServlet does not appear to have started.",
                      myServer.waitForStringInLog("CWWKZ0001I: Application CustomQueryDatabaseServlet started"));
        String queryString;
        String response;
//TODO uncomment after fetching
//        //Test case isUserInRoleLDAPISWar1
//        //Access WAR 1 and check UserInRole, sending user1 which does not exist in the ldap is.
//        Log.info(logClass, getCurrentTestName(), "-------Running isUserInRoleLDAPISWar1 scenario");
//        String queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
//        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
//        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
//                                                          LocalLdapServer.PASSWORD,
//                                                          HttpServletResponse.SC_FORBIDDEN);
//        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
//        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
//        verifyExceptionResponse(response, "Error 403: AuthorizationFailed");
//        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
//        Log.info(logClass, getCurrentTestName(), "-----Exiting isUserInRoleLDAPISWar1");
//        httpclient.getConnectionManager().shutdown();
//        setupConnection();

        //Test case isUserInRoleLDAPISWar2
        //Access WAR 2 and check for UserInRole, sending AnotherUser1 which exist in WAR 2 Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running isUserInRoleLDAPISWar2 scenario");
        queryString = EJB_WAR2_PATH + SIMPLE_SERVLET2 + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.ANOTHERUSER1,
                                                   LocalLdapServer.ANOTHERPASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, "isUserInRole(Manager): true");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isUserInRoleLDAPISWar2");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case isUserInRoleISWar1
        //Access WAR 2 and check for UserInRole, sending basicRoleUser which exist in WAR 1 IS.
        Log.info(logClass, getCurrentTestName(), "-------Running isUserInRoleISWAR1 scenario");
        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser,
                                                   Constants.javaeesec_basicRolePwd,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleUser, "isUserInRole(Manager): true");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isUserInRoleLDAPISWar2");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case isCallerInRoleISWar2
        //Access WAR 2 and check for isCallerInRole sending basicRoleUser which exist in WAR 1 IS but not in the WAR Servlet
        Log.info(logClass, getCurrentTestName(), "-----Running isCallerInRoleISWar2 scenario");

        queryString = EJB_WAR2_PATH + SIMPLE_SERVLET2 + "?testInstance=ejb03&testMethod=declareRoles01";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser,
                                                   Constants.javaeesec_basicRolePwd,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.javaeesec_basicRoleUser, "isUserInRole(Manager): true");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isCallerInRoleLDAPWar1");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case testisCallerInRoleLDAPWar2
        //Access WAR 2 and check for isCaller in role sending user2 which exist in WAR 1 Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-----Running testisCallerInRoleLDAPWar2 scenario");
        queryString = EJB_WAR2_PATH + SIMPLE_SERVLET2 + "?testInstance=ejb03&testMethod=declareRoles01";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER2,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER2, "securityContext.isCallerInRole(DeclaredRole01): true");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting testisCallerInRoleLDAPWar2");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case testisUserNotInRoleLDAPWar1
        //Access WAR 1 and check for isUserInRole sending an invalid user.
        Log.info(logClass, getCurrentTestName(), "-----Running testisUserNotInRoleLDAPWar1 scenario");

        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.INVALIDUSER,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyExceptionResponse(response, "Error 403: AuthorizationFailed");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting testisUserNotInRoleLDAPWar1");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case testisUserInRoleBasicUserRegistryFallBackWar1WithAnnotations
        //Access WAR 1 and check for isUserInRole but then fails to fall back to UR.
        Log.info(logClass, getCurrentTestName(), "-----Running testisUserInRoleBasicUserRegistryFallBackWar1WithAnnotations scenario");
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user99",
                                                   "user99pwd",
                                                   HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        myServer.removeInstalledAppForValidation(EJB_APP_NAME);
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
