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

@MinimumJavaLevel(javaLevel = 8, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)

public class EJBModuleTestUnprotectedServlet extends JavaEESecTestBase {
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = EJBModuleTestUnprotectedServlet.class;
    protected static String urlBase;
    protected static String TEMP_DIR = "test_temp";
    protected static String EJB_BEAN_JAR_NAME = "SecurityEJBinWAR.jar";
    protected static String EJB_SERVLET_NAME = "SecurityEJBBaseServlet";
    protected static String EJB_WAR_NAME = "AnnotatedEjbinWarServletLdapDB.war";
    protected static String EJB_WAR_PATH = "/AnnotatedEjbinWarServletLdapDB/";
    protected static String EJB_WAR_NAME2 = "AnnotatedEjbinWarServletLdap.war";
    protected static String EJB_WAR2_PATH = "/AnnotatedEjbinWarServletLdap/";
    protected static String EJB_EAR_NAME = "securityejbinwar.ear";
    protected static String EJB_APP_NAME = EJB_EAR_NAME;
    protected static String XML_NAME = "ejbunprotectedserver.xml";
    protected static String JASPIC_RUN_AS_XML_NAME = "ejbunprotectedCustomISRunAsserver.xml";
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String SIMPLE_SERVLET = "SimpleServlet";
    protected static String SIMPLE_SERVLET2 = "SimpleServlet2";
    protected static String RUNAS_SERVLET = "SimpleServletRunAs";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public EJBModuleTestUnprotectedServlet() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(logClass, "setUp()", "-----setting up test");
        ldapServer = new LocalLdapServer();
        ldapServer.start();

        Log.info(logClass, "setUp()", "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean", "web.war.ejb.annotated.servlet");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME2, true, EJB_BEAN_JAR_NAME, true, "web.jar.base",
                                      "web.ejb.jar.bean", "web.war.ejb.annotated.servlet2");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME, EJB_WAR_NAME2);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat2.war", true, JAR_NAME, false, "web.jar.base", "web.war.db2");
        Log.info(logClass, "setUp()", "-----EAR app created");

        startServer(XML_NAME, EJB_APP_NAME);

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
     * One war file contains two unprotected servlets, the other one contains one unprotected servlet.
     * Each war files has one jar file.
     * This test case uses EJB with the purpose of testing Basic Authentication with LDAP Identity Store in WAR1 (ejbinwarservlet).
     * </OL>
     * <P> Expected Results: 200 OK and isUserInRole(true).
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testisUserInRoleLDAPISWar1() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbUserResponse(response, Constants.getEJBBeanResponse + Constants.ejb03Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodManager,
                              Constants.getEjbCallerPrincipal + LocalLdapServer.USER1);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two unprotected servlets, the other one contains one unprotected servlet.
     * Each war files has one jar file.
     * This test case uses EJB with the purpose of testing Basic Authentication with LDAP Identity Store in WAR2 (ejbinwarservlet2).
     * </OL>
     * <P> Expected Results: 200 OK and isUserInRole(true).
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testisUserInRoleLDAPISWar2() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = EJB_WAR2_PATH + SIMPLE_SERVLET2 + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");

        verifyEjbUserResponse(response, Constants.getEJBBeanResponse + Constants.ejb03Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodManager,
                              Constants.getEjbCallerPrincipal + LocalLdapServer.USER1);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two unprotected servlets, the other one contains one unprotected servlet.
     * Each war files has one jar file.
     * This test case uses EJB with the purpose of testing Basic Authentication with the use of LDAP and DB Identity Store.
     * The user will try to authenticate as LDAPUser, authentication will succeed however the RunAs config will be picked up
     * DB user will be used with employee access given.
     * </OL>
     * <P> Expected Results: 200 OK and isUserInRole(true).
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testisRunAsUserInRoleLDAPtoDBWar1() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = EJB_WAR_PATH + RUNAS_SERVLET + "?testInstance=ejb01&testMethod=runAsSpecified";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER2,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbRunAsUserResponse(response, Constants.ejb01Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodRunAsSpecified,
                                   Constants.getEjbCallerPrincipal + Constants.DB_USER3, Constants.DB_USER2);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two unprotected servlets, the other one contains one unprotected servlet.
     * Each war files has one jar file.
     * This test case uses EJB with the purpose of testing Basic Authentication with LDAP Identity Store in WAR1 (ejbinwarservlet).
     * </OL>
     * <P> Expected Results: Servlet is not protected so a 200 OK and isUserInRole(false).
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testisUserNotInRoleLDAPWar1() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.INVALIDUSER,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbErrorUserResponse(response, Constants.ejbAccessException);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two unprotected servlets, the other one contains one unprotected servlet.
     * Each war files has one jar file.
     * This test case uses EJB with the purpose of testing Basic Authentication with LDAP Identity Store in WAR2 (ejbinwarservlet2).
     * </OL>
     * <P> Expected Results: 200 OK and isUserInRole(true).
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testisUserNotInRoleLDAPWar2() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = EJB_WAR2_PATH + SIMPLE_SERVLET2 + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.INVALIDUSER,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbErrorUserResponse(response, Constants.ejbAccessException);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two unprotected servlets, the other one contains one unprotected servlet.
     * Each war files has one jar file.
     * Test case tries to access unprotected servlet with invalid user. RunAs is expected to be called.
     * </OL>
     * <P> Expected Results: 200 OK and isCallerInRole(false).
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testRunAsInvalidUserUnprotectedServletInRoleLDAPWar1() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = EJB_WAR_PATH + RUNAS_SERVLET + "?testInstance=ejb01&testMethod=runAsSpecified";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.INVALIDUSER,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbRunAsUserResponse(response, Constants.ejb01Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodRunAsSpecified,
                                   Constants.getEjbCallerPrincipal + Constants.DB_USER3, Constants.DB_USER2);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two unprotected servlets, the other one contains one unprotected servlet.
     * Each war files has one jar file.
     * This test case uses EJB with the purpose of testing Basic Authentication but falling back to the server's UR.
     * </OL>
     * <P> Expected Results: 401 Due to the application uses IS.
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testisUserInRoleBasicUserRegistryFallBackWar1WithAnnotations() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user99",
                                                          "user99pwd",
                                                          HttpServletResponse.SC_UNAUTHORIZED);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two unprotected servlets, the other one contains one unprotected servlet.
     * Each war files has one jar file.
     * This test case uses EJB with the purpose of testing Basic Authentication with DB Identity Store.
     * Will try to access the first servlet in war 1 using DB user configured in the second servlet the same WAR
     * </OL>
     * <P> Expected Results: 200 OK and isUserInRole(true).
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testDBUserAccessLDAPServletWar1() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = EJB_WAR_PATH + SIMPLE_SERVLET + "?testInstance=ejb02&testMethod=employeeAndManager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER1,
                                                          Constants.DB_USER1_PWD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbUserResponse(response, Constants.getEJBBeanResponse + Constants.ejb02Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodEmployeeAndManager,
                              Constants.getEjbCallerPrincipal + Constants.DB_USER1);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file that contains two war files.
     * One war file contains two unprotected servlets, the other one contains one unprotected servlet.
     * Each war files has one jar file.
     * This test case uses EJB with the purpose of testing Basic Authentication with DB Identity Store.
     * Will try to access the first servlet in WAR 2 using DB user configured in the second servlet of WAR 1
     * </OL>
     * <P> Expected Results: 200 OK and isUserInRole(true).
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testDBUserAccessWAR2Servlet() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = EJB_WAR2_PATH + SIMPLE_SERVLET2 + "?testInstance=ejb02&testMethod=employeeAndManager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER1,
                                                          Constants.DB_USER1_PWD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyEjbUserResponse(response, Constants.getEJBBeanResponse + Constants.ejb02Bean, Constants.getEjbBeanMethodName + Constants.ejbBeanMethodEmployeeAndManager,
                              Constants.getEjbCallerPrincipal + Constants.DB_USER1);
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
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
