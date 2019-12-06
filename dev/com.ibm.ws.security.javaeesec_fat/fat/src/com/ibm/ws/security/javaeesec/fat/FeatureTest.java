/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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

import componenttest.annotation.ExpectedFFDC;
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
public class FeatureTest extends JavaEESecTestBase {
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
    protected static String EJB_EAR_NAME = "securityejbinwar2.ear";
    protected static String EJB_EAR_NAME_noPermission = "securityejbinwar3.ear";
    protected static String EJB_APP_NAME = EJB_EAR_NAME;
    protected static String EJB_APP_NAME_noPermission = EJB_EAR_NAME_noPermission;
    protected static String XML_NAME = "ejbprotectedserver.xml";
    protected static String APP_SEC_1_XML_NAME = "ejbprotectedserverAppSecurity1.xml";
    protected static String APP_SEC_2_XML_NAME = "ejbprotectedserverAppSecurity2.xml";
    protected static String JASPIC_RUN_AS_XML_NAME = "ejbprotectedCustomISRunAsserver.xml";
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String SIMPLE_SERVLET = "SimpleServlet";
    protected static String SIMPLE_SERVLET2 = "SimpleServlet2";
    protected static String RUNAS_SERVLET = "SimpleServletRunAs";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public FeatureTest() {
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
    @Test
    public void testEJBAppSecurity30() throws Exception {
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
        Log.info(logClass, "setUp()", "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
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
     * 3. AppSecurity feature 2.0 Will be used.
     * </OL>
     * <P> The application will fail to start because the feature does not support Identity Store.
     * <OL>
     * <LI>
     * </OL>
     */
    @Test
    @ExpectedFFDC(value = { "java.lang.NoClassDefFoundError", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testEJBAppSecurity20() throws Exception {
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
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME_noPermission, true, EJB_WAR_NAME, EJB_WAR_NAME2);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME_noPermission);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat2.war", true, JAR_NAME, false, "web.jar.base", "web.war.db2");
        Log.info(logClass, "setUp()", "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        startServer(APP_SEC_2_XML_NAME, EJB_APP_NAME_noPermission);
        assertNotNull("Expected class not found error",
                      myServer.waitForStringInLog("CWNEN00\\d\\dW: Resource annotations on the methods of the web.ejb.jar.bean..*"));
        assertNotNull("Application was able not able to start",
                      myServer.waitForStringInLog("CWWKZ0002E: An exception occurred while starting the application securityejbinwar3."));

        myServer.removeInstalledAppForValidation(EJB_APP_NAME_noPermission);
        myServer.stopServer("CWNEN0049W:*", "CWNEN0050W:*", "CWWKZ0106E:*", "CWWKZ0002E:*");
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
     * 3. AppSecurity feature 2.0 Will be used.
     * </OL>
     * <P> The application will fail to start because the feature does not support Identity Store.
     * <OL>
     * <LI>
     * </OL>
     */
    @Test
    @ExpectedFFDC(value = { "java.lang.NoClassDefFoundError", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testEJBAppSecurity10() throws Exception {
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
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME_noPermission, true, EJB_WAR_NAME, EJB_WAR_NAME2);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME_noPermission);
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat2.war", true, JAR_NAME, false, "web.jar.base", "web.war.db2");
        Log.info(logClass, "setUp()", "-----EAR app created");

        Log.info(logClass, getCurrentTestName(), "-----Accessing Application to test scenarios...");
        startServer(APP_SEC_1_XML_NAME, EJB_APP_NAME_noPermission);
        assertNotNull("Expected class not found error",
                      myServer.waitForStringInLog("CWNEN00\\d\\dW: Resource annotations on the methods of the web.ejb.jar.bean..*"));
        assertNotNull("Application was able not able to start",
                      myServer.waitForStringInLog("CWWKZ0002E: An exception occurred while starting the application securityejbinwar3."));

        myServer.removeInstalledAppForValidation(EJB_APP_NAME_noPermission);
        myServer.stopServer("CWNEN0049W:*", "CWNEN0050W:*", "CWWKZ0106E:*", "CWWKZ0002E:*");
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
