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

import static org.junit.Assert.assertNotNull;

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
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description: Check that no user/group defined in the access-id works.
 */
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class HttpAuthenticationMechanismDBNoUserTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.db.nouser.fat");
    protected static Class<?> logClass = HttpAuthenticationMechanismDBNoUserTest.class;
    protected String queryString = "/DatabaseAnnotatedBasicAuthServlet/JavaEESecAnnotatedBasic";
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;

    @Before
    public void setupConnection() {
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);

        httpclient = new DefaultHttpClient(httpParams);
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    public HttpAuthenticationMechanismDBNoUserTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.basic");
        WCApplicationHelper.addWarToServerApps(myServer, "DatabaseAnnotatedBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.annotatedbasic.db");
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat.war", true, JAR_NAME, false, "web.jar.base", "web.war.db");

        myServer.startServer(true);
        assertNotNull("Application DBServlet does not appear to have started.",
                      myServer.waitForStringInLog("CWWKZ0001I: Application DBServlet started"));
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer("CWWKS1922W", "CWWKS1923W", "CWWKS1924W");
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    @Test
    public void testAllowedAccessUser() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // check based on user
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER1,
                                                          Constants.DB_USER1_PWD,
                                                          HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.DB_USER1, Constants.getRemoteUserFound + Constants.DB_USER1);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    @Test
    public void testAllowedAccessGroup() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER2,
                                                          Constants.DB_USER2_PWD,
                                                          HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.DB_USER2, Constants.getRemoteUserFound + Constants.DB_USER2);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with an invalid userId and password and verify that
     * <LI> JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * </OL>
     */
    @Test
    public void testInvalidUser() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "baduser",
                                        "pwd",
                                        HttpServletResponse.SC_UNAUTHORIZED);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with an user that exists, but is not authorized and verify that
     * <LI> JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * </OL>
     */
    @Test
    public void testValidUser_NoAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER3,
                                        Constants.DB_USER3_PWD,
                                        HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
