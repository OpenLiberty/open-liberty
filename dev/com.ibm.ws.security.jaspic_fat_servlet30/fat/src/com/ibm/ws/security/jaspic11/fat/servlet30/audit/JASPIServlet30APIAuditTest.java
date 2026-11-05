/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.jaspic11.fat.servlet30.audit;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.RecentAuditFileStream;
import com.ibm.ws.security.jaspic11.fat.servlet30.JASPITestBase;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 *
 * The test verifies that the correct audit records are emitted for JASPI processing of the Servlet 3.0 methods login,
 * logout and authenticate methods. For login, no audit record is generated because the login() method is not support with
 * JASPIC. For authenticate, a SECURITY_AUTHN record is emitted, and for logout() a SECURITY_API_AUTHN_TERMINATE record is emitted.
 *
 * The test accesses a servlet which issues the method calls to login, logout and authenticate.
 *
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class JASPIServlet30APIAuditTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.servlet30.audit");
    protected static Class<?> logClass = JASPIServlet30APIAuditTest.class;
    protected static String protectedQueryString = "/JASPIServlet30/ProtectedJASPIServlet30";
    protected static String unprotectedQueryString = "/JASPIServlet30/JASPIServlet30";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIServlet30APIAuditTest() {
        super(myServer, logClass);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        myServer.installUserBundle("com.ibm.ws.security.jaspic.test_1.0");
        myServer.installUserFeature("jaspicUserTestFeature-1.0");
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_SERVLET30_APP);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaspiFeature(myServer);
            AuditCommonTest.verifyAuditAndAuditFileHandlerReady(myServer);
        }

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
        myServer.uninstallUserBundle("com.ibm.ws.security.jaspic.test_1.0");
        myServer.uninstallUserFeature("jaspicUserTestFeature-1.0");

    }

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    @Rule
    public TestName name = new TestName();

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet which uses programmatic security and invokes the logout() method followed by the
     * <LI> authenticate() method for a protected URL first without a userid/pwd and the with a valid user and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401 from unauthenticated request and JASPI processing
     * <LI> Return code 200 from request with valid userid/ pw
     * <LI> Responses contains:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> isAuthenticated returns true
     * <LI> getAuthType returns BASIC
     * <LI> getUserPrincipal returns WSPrincipal:jaspiuser1
     * <LI> gerRemoteUser returns jaspiuser1
     * </OL>
     * The audit log should contain the following:
     * <OL>
     * <LI> SECURITY_AUTHN with 401 for unauthenticated request
     * <LI> SECURITY_AUTHN with 401 challenge followed by SECURITY_AUTHN 200 success when servlet is accessed
     * <LI> SECURITY_AUTHN_TERMINATE with 200 success for logout() processed by JASPI JASPIServlet30 servlet with JASPI provider and auth type
     * <LI> SECURITY_AUTHN with 200 success for authenticate() processed by JASPI JASPIServlet30 servlet
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiServlet30Authenticate_ProtectedURL_Successful_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryParms = "?method=authenticate1&protected=true";
        // 1st verify 401 challenge, which implies url is protected, then re-issue authenticate request
        String response = executeGetRequestNoAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, HttpServletResponse.SC_UNAUTHORIZED);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, NO_NAME_VALIDATION);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIServlet30/ProtectedJASPIServlet30",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_SERVLET30_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=401",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE));

        recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        asserts = new AuditAsserts(logClass, recent);

        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, jaspi_servlet30User,
                                                   jaspi_servlet30UserPwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_SERVLET30_NAME);
        verifyAuthenticateMethodProcessedInMessageLog("authenticate1");
        verifyAuthenticatedResponse(response, getAuthTypeJaspi, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIServlet30/ProtectedJASPIServlet30",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_SERVLET30_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=401",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIServlet30/ProtectedJASPIServlet30",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_SERVLET30_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser1",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_JASPI_AUTHTYPE + "=" + AuditEvent.CRED_TYPE_JASPI_AUTH,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.TARGET_PARAMS + "=method=authenticate1&protected=true",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPIServlet30/ProtectedJASPIServlet30",
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser1",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=" + "[Servlet30]",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditConstants.EVENT_NAME + "=" + AuditConstants.SECURITY_API_AUTHN_TERMINATE,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser1",
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_NAME + "=/JASPIServlet30/ProtectedJASPIServlet30",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_SERVLET30_APP,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_JASPI_AUTHTYPE + "=" + AuditEvent.CRED_TYPE_JASPI_AUTH,
                                                 AuditConstants.OUTCOME + "=" + AuditConstants.SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIServlet30/ProtectedJASPIServlet30",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_SERVLET30_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser1",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_JASPI_AUTHTYPE + "=" + AuditEvent.CRED_TYPE_JASPI_AUTH,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.TARGET_PARAMS + "=method=authenticate1&protected=true",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet which uses programmatic security and invokes
     * <LI> the Servlet 3.0 login method with valid user and password. Login should result in servlet exception when
     * <LI> JASPI authentication is active.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Response from login is javax.servlet.ServletException: login method invoked while JASPI authentication active
     * <LI> The audit log should NOT contain an entry for SECURITY_API_AUTHN since the login() method is not supported for JASPI.
     * </OL>
     *
     */
    @Test
    public void testJaspiServlet30Login_ValidUserAndPasswordWithProtectedURL_FailsWithServletException() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryParms = "?method=login,user=" + jaspi_basicRoleUser + ",password=" + jaspi_basicRolePwd;
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyExceptionResponse(response, SERVLET_EXCEPTION_JASPI_LOGIN);
        asserts.assertNotFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_API_AUTHN));

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
