/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
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

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)

public class EJBModuleTestProtectedServlet extends JavaEESecTestBase {
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = EJBModuleTestUnprotectedServlet.class;
    protected static String urlBase;
    protected static String TEMP_DIR = "test_temp";
    protected static String EJB_BEAN_JAR_NAME = "SecurityEJBinWAR.jar";
    protected static String EJB_SERVLET_NAME = "SecurityEJBBaseServlet";
    protected static String EJB_WAR_NAME = "EjbinWarServletISLdapDb.war";
    protected static String EJB_WAR_PATH = "/EjbinWarServletISLdapDb/";
    protected static String EJB_WAR_NAME2 = "EjbinWarServletISLdap.war";
    protected static String EJB_WAR2_PATH = "/EjbinWarServletISLdap/";
    protected static String EJB_REALM1_WAR_NAME = "AnnotatedEjbinWarServletLdapRealm1.war";
    protected static String EJB_REALM1_WAR_PATH = "/AnnotatedEjbinWarServletLdapRealm1/";
    protected static String EJB_REALM2_WAR_NAME = "AnnotatedEjbinWarServletLdapRealm2.war";
    protected static String EJB_REALM2_WAR_PATH = "/AnnotatedEjbinWarServletLdapRealm2/";
    protected static String EJB_EAR_NAME = "securityejbinwar2.ear";
    protected static String EJB_APP_NAME = EJB_EAR_NAME;
    protected static String EJB_EAR_REALM_NAME = "securityejbinwarrealm.ear";
    protected static String EJB_REALM_APP_NAME = EJB_EAR_REALM_NAME;
    protected static String XML_NAME = "ejbprotectedserver.xml";
    protected static String XML_REALM_NAME = "ejbprotectedrealmserver.xml";
    protected static String JASPIC_RUN_AS_XML_NAME = "ejbprotectedCustomISRunAsserver.xml";
    protected static String LDAP_RUN_AS_XML_NAME = "ejbprotectedLDAPISRunAsserver.xml";
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String SIMPLE_SERVLET = "SimpleServlet";
    protected static String SIMPLE_SERVLET2 = "SimpleServlet2";
    protected static String SIMPLE_SERVLET_REALM1 = "SimpleServletRealm1";
    protected static String SIMPLE_SERVLET_REALM2 = "SimpleServletRealm2";
    protected static String RUNAS_SERVLET = "SimpleServletRunAs";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public EJBModuleTestProtectedServlet() {
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
     * In this test case the following configuration will be used.
     * 1. WAR 1 will use the LDAP Identity Store.
     * 2. WAR 2 will use the LDAP2 Identity Store.
     * </OL>
     * <P> Multiple results are expected.
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testEJBAnnotatedLdapISOnWar1andWar2() throws Exception {
        String response;
        String queryString;
        //create app and setup server
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        Log.info(logClass, getCurrentTestName(), "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean", "web.war.ejb.is.servlet",
                                      "web.war.identitystores.ldap.ldap1", "web.war.identitystores.ldap");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME2, true, EJB_BEAN_JAR_NAME, true, "web.jar.base",
                                      "web.ejb.jar.bean", "web.war.ejb.is.servlet2", "web.war.identitystores.ldap.ldap2", "web.war.identitystores.ldap");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME, EJB_WAR_NAME2);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat2.war", true, JAR_NAME, false, "web.jar.base", "web.war.db2");
        Log.info(logClass, getCurrentTestName(), "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        //start server
        startServer(XML_NAME, EJB_APP_NAME);

        //Test case isUserInRoleLDAPISWar1
        //Access WAR 1 and check UserInRole, sending user1 which exist in the Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running isUserInRoleLDAPISWar1 scenario");
        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbUserResponse(response, Constants.getEJBBeanResponse + Constants.ejb03Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodManager,
                              Constants.getEjbCallerPrincipal + LocalLdapServer.USER1);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isUserInRoleLDAPISWar1");
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
        verifyEjbErrorUserResponse(response, Constants.ejbAuthorizationFailed);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting testisUserNotInRoleLDAPWar1");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case testisUserInRoleBasicUserRegistryFallBackWar1WithAnnotations
        //Access WAR 1 and check for isUserInRole but then fails even when it falls back to UR.
        Log.info(logClass, getCurrentTestName(), "-----Running testisUserInRoleBasicUserRegistryFallBackWar1WithAnnotations scenario");
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user99",
                                                   "user99pwd",
                                                   HttpServletResponse.SC_UNAUTHORIZED);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");

        Log.info(logClass, getCurrentTestName(), "-----Exiting testisUserInRoleBasicUserRegistryFallBackWar1WithAnnotations");
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
     * In this test case the following configuration will be used.
     * 1. WAR 1 will be configured to use a custom Identity Store.
     * 2. WAR 2 will use the LDAP2 Identity Store.
     * </OL>
     * <P> Multiple results are expected.
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC("javax.naming.AuthenticationException")
    public void testEJBCustomISonWAR1AnnotatedLDAPISonWAR2() throws Exception {
        //create app and setup server
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        Log.info(logClass, getCurrentTestName(), "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean", "web.war.ejb.is.servlet",
                                      "web.war.identitystores.ldap.ldap2", "web.war.identitystores.ldap");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME2, true, EJB_BEAN_JAR_NAME, true, "web.jar.base",
                                      "web.ejb.jar.bean", "web.war.ejb.is.servlet2", "web.war.identitystores", "web.war.identitystores.scoped.application");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME, EJB_WAR_NAME2);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat2.war", true, JAR_NAME, false, "web.jar.base", "web.war.db2");
        Log.info(logClass, getCurrentTestName(), "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        //start server
        startServer(XML_NAME, EJB_APP_NAME);

        String queryString;
        String response;

        //Test case isUserInRoleLDAPISWar1
        //Access WAR 1 and check UserInRole, sending user1 which does not exist in this IS.
        Log.info(logClass, getCurrentTestName(), "-------Running isUserInRoleLDAPISWar1 scenario");
        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_UNAUTHORIZED);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
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
        verifyEjbUserResponse(response, Constants.getEJBBeanResponse + Constants.ejb03Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodManager,
                              Constants.getEjbCallerPrincipal + LocalLdapServer.ANOTHERUSER1);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isUserInRoleLDAPISWar2");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case isUserInRoleLDAPISWar1
        //Access WAR 1 and check for UserInRole, sending anotherUser which exist in WAR 1 LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running isUserInRoleLDAPISWar1 scenario");
        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.ANOTHERUSER1,
                                                   LocalLdapServer.ANOTHERPASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbUserResponse(response, Constants.getEJBBeanResponse + Constants.ejb03Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodManager,
                              Constants.getEjbCallerPrincipal + LocalLdapServer.ANOTHERUSER1);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isUserInRoleLDAPISWar1");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case isUserInRoleCustomISWar2
        //Access WAR 2 and check for UserInRole, sending basicRoleUser which exist in WAR 1 IS.
        Log.info(logClass, getCurrentTestName(), "-------Running isUserInRoleCustomISWar2 scenario");
        queryString = EJB_WAR2_PATH + SIMPLE_SERVLET2 + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser,
                                                   Constants.javaeesec_basicRolePwd,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbUserResponse(response, Constants.getEJBBeanResponse + Constants.ejb03Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodManager,
                              Constants.getEjbCallerPrincipal + Constants.javaeesec_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isUserInRoleCustomISWar2");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        //Test case isCallerInRoleCustomISWar1
        //Access WAR 1 and check for isCallerInRole sending basicRoleUser which exist in WAR 2 IS but not in WAR 1.
        //WAR 2 uses custom IS, when using a user that only exist in WAR 2, the authentication should fail on WAR 1.
        Log.info(logClass, getCurrentTestName(), "-----Running isCallerInRoleCustomISWar1 scenario");

        queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser,
                                                   Constants.javaeesec_basicRolePwd,
                                                   HttpServletResponse.SC_UNAUTHORIZED);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isCallerInRoleCustomISWar1");
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
                                                   HttpServletResponse.SC_UNAUTHORIZED);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");

        Log.info(logClass, getCurrentTestName(), "-----Exiting testisUserInRoleBasicUserRegistryFallBackWar1WithAnnotations");
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
     * In this test case the following configuration will be used.
     * 1. WAR 1 will be configured to use Database.
     * 2. WAR 2 will use the LDAP Identity Store.
     * 3. Test cases will run using the RunAsServlet.
     * 4. To test this out, we will be using the local ldap user: User 2.
     * However, in the server.xml the blue3 is configured as the run-as manager.
     * but that user doesn't exist in the database. For that reason it is expected that
     * user 2 will appear in the servlet response.
     * </OL>
     * <P> The user we are sending exist on the LDAP but the RunAs exist on the DB. 200 is expected.
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testEJBRunAsAnnotatedDBOnWar1AndLDAPOnWAR2() throws Exception {

        String response;
        String queryString;
        //create app and setup server
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        Log.info(logClass, getCurrentTestName(), "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean", "web.war.ejb.is.servlet",
                                      "web.war.identitystores.db.derby2", "web.war.identitystores.ldap");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME2, true, EJB_BEAN_JAR_NAME, true, "web.jar.base",
                                      "web.ejb.jar.bean", "web.war.ejb.is.servlet2", "web.war.identitystores.ldap.ldap1", "web.war.identitystores.ldap");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME, EJB_WAR_NAME2);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat2.war", true, JAR_NAME, false, "web.jar.base", "web.war.db2");
        Log.info(logClass, getCurrentTestName(), "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        startServer(XML_NAME, EJB_APP_NAME);

        //Test case testisRunAsUserInRoleLDAPtoDBWar1
        //Access WAR 2 and check for UserInRole, sending AnotherUser1 which exist in WAR 2 Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running testisRunAsUserInRoleLDAPtoDBWar1 scenario");

        queryString = EJB_WAR_PATH + RUNAS_SERVLET + "?testInstance=ejb01&testMethod=runAsSpecified";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER2,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");

        verifyEjbRunAsUserResponse(response, Constants.ejb01Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodRunAsSpecified,
                                   Constants.getEjbCallerPrincipal + Constants.DB_USER3, Constants.DB_USER2);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting testisRunAsUserInRoleLDAPtoDBWar1");
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
     * In this test case the following configuration will be used.
     * 1. WAR 1 will be configured to use Database.
     * 2. WAR 2 will use the custom IS.
     * 3. Test cases will run using the RunAsServlet.
     * </OL>
     * <P> Since the user we will be sending only exist in the custom IS,
     * the test user will not be found and thus it will throw an authentication failure.
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testEJBRunAsCustomISWar1AndDBonWAR2() throws Exception {
        String response;
        String queryString;
        //create app and setup server
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        Log.info(logClass, getCurrentTestName(), "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean", "web.war.ejb.is.servlet",
                                      "web.war.identitystores.db.derby2;");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME2, true, EJB_BEAN_JAR_NAME, true, "web.jar.base",
                                      "web.ejb.jar.bean", "web.war.ejb.is.servlet2", "web.war.identitystores");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME, EJB_WAR_NAME2);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat2.war", true, JAR_NAME, false, "web.jar.base", "web.war.db2");
        Log.info(logClass, getCurrentTestName(), "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        startServer(JASPIC_RUN_AS_XML_NAME, EJB_APP_NAME);

        //Test case testisRunAsUserInRoleLDAPtoDBWar1
        //Access WAR 2 and check for UserInRole, sending AnotherUser1 which exist in WAR 2 Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running testisRunAsUserInRoleLDAPtoDBWar1 scenario");

        queryString = EJB_WAR_PATH + RUNAS_SERVLET + "?testInstance=ejb01&testMethod=runAsSpecified";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser,
                                                   Constants.javaeesec_basicRolePwd,
                                                   HttpServletResponse.SC_UNAUTHORIZED);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting testisRunAsUserInRoleLDAPtoDBWar1");
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
     * In this test case the following configuration will be used.
     * 1. WAR 1 will use the LDAP Identity Store.
     * 2. WAR 2 will use the LDAP2 Identity Store.
     * </OL>
     * <P> Multiple results are expected.
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testEJBRunAsLdapISOnWar1andWar2() throws Exception {
        String response;
        String queryString;
        //create app and setup server
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        Log.info(logClass, getCurrentTestName(), "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean", "web.war.ejb.is.servlet",
                                      "web.war.identitystores.ldap.ldap1", "web.war.identitystores.ldap");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME2, true, EJB_BEAN_JAR_NAME, true, "web.jar.base",
                                      "web.ejb.jar.bean", "web.war.ejb.is.servlet2", "web.war.identitystores.ldap.ldap2", "web.war.identitystores.ldap");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME, EJB_WAR_NAME2);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat2.war", true, JAR_NAME, false, "web.jar.base", "web.war.db2");
        Log.info(logClass, getCurrentTestName(), "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        startServer(LDAP_RUN_AS_XML_NAME, EJB_APP_NAME);

        //Test case isUserInRoleLDAPISWar1
        //Access WAR 1 and check UserInRole, sending RunAsUser1 which exist in the Annotated LDAP IS.
        Log.info(logClass, getCurrentTestName(), "-------Running isUserInRoleLDAPISWar1 scenario");
        queryString = EJB_WAR_PATH + RUNAS_SERVLET + "?testInstance=ejb01&testMethod=runAsSpecified";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.RUNASUSER1,
                                                   LocalLdapServer.PASSWORD,
                                                   HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbRunAsUserResponse(response, Constants.ejb01Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodRunAsSpecified,
                                   Constants.getEjbCallerPrincipal + LocalLdapServer.USER1, LocalLdapServer.USER2);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting isUserInRoleLDAPISWar1");
        httpclient.getConnectionManager().shutdown();
        setupConnection();

        myServer.removeInstalledAppForValidation(EJB_APP_NAME);
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
