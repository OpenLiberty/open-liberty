package com.ibm.ws.security.javaeesec.fat;

import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
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

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
/**
 * Test Description:
 */
@MinimumJavaLevel(javaLevel = 1.7, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class HttpAuthenticationMechanismDBTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.db.fat");
    protected static Class<?> logClass = HttpAuthenticationMechanismDBTest.class;
    protected String queryString = "/DatabaseAnnotatedBasicAuthServlet/JavaEESecAnnotatedBasic";
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;

    public HttpAuthenticationMechanismDBTest() {
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
        myServer.stopServer();

    }

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
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
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiAnnotatedDBBasicAuthValidUserInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // check based on user
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER1,
                                                          Constants.DB_USER1_PWD,
                                                          HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.DB_USER1, Constants.getRemoteUserFound + Constants.DB_USER1);

        // check based on group
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER2,
                                                   Constants.DB_USER2_PWD,
                                                   HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + Constants.DB_USER2, Constants.getRemoteUserFound + Constants.DB_USER2);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and invalid password in the javaeesec_basic role and verify that
     * <LI> JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiAnnotatedDBBasicAuthValidUserBadPwd() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER1,
                                        "badpwd",
                                        HttpServletResponse.SC_FORBIDDEN);
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
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiAnnotatedDBBasicAuthInvalidUser() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "baduser",
                                        "pwd",
                                        HttpServletResponse.SC_FORBIDDEN);
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
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiAnnotatedDBBasicAuthValidUser_NoAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER3,
                                        Constants.DB_USER3_PWD,
                                        HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with an userId that will cause multiple users to be returned from the database and verify that
     * <LI> JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiAnnotatedDBBasicAuthDuplicateUser() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER_DUPE,
                                        Constants.DB_USER1_PWD,
                                        HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with an userId that has no password defined in the database and verify that
     * <LI> JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiAnnotatedDBBasicAuthNullPassword() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.DB_USER_NOPWD,
                                        Constants.DB_USER1_PWD,
                                        HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
