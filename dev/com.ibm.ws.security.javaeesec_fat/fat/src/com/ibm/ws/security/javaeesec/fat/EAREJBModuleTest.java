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
@MinimumJavaLevel(javaLevel = 1.8, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)

public class EAREJBModuleTest extends JavaEESecTestBase {
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = EAREJBModuleTest.class;
    protected static String urlBase;
    protected static String TEMP_DIR = "test_temp";
    protected static String EJB_BEAN_JAR_NAME = "SecurityEJBinWAR.jar";
    protected static String EJB_SERVLET_NAME = "SecurityEJBBaseServlet";
    protected static String EJB_WAR_NAME = "ejbinwarservlet.war";
    protected static String EJB_EAR_NAME = "securityejbinwar.ear";
    protected static String EJB_APP_NAME = "securityejbinwar.ear";
    protected static String XML_NAME = "ejbserver.xml";

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

        Log.info(logClass, "setUp()", "-----Creating EAR app.");

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.jar.base", "web.ejb.jar.bean", "web.war.ejb.servlet");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);
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

    String getServletURL() {
        return "SimpleServlet";
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains one war and one jar file. It uses EJB with the purpose of testing Basic Authentication with LDAP
     * Identity Store.
     * </OL>
     * <P> Expected Results: 200 OK user is in role for manager.
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testisUserInRole() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = "/securityejbinwar/" + getServletURL() + "?testInstance=ejb03&testMethod=manager";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, "isUserInRole(Manager): true");
        Log.info(logClass, getCurrentTestName(), "-------------End of Verification of Response");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains one war and one jar file. It uses EJB with the purpose of testing Basic Authentication with LDAP
     * Identity Store.
     * </OL>
     * <P> Expected Results: 200 OK caller is in role for manager.
     * <OL>
     * <LI>
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testisCallerInRole() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryString = "/securityejbinwar/" + getServletURL() + "?testInstance=ejb03&testMethod=declareRoles01";
        Log.info(logClass, getCurrentTestName(), "-------------Executing BasicAuthCreds");
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER2,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_OK);
        Log.info(logClass, getCurrentTestName(), "-------------End of Response");
        Log.info(logClass, getCurrentTestName(), "-------------Verifying Response");
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER2, "securityContext.isCallerInRole(DeclaredRole01): true");
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
