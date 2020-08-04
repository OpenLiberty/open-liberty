/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jaspic11.fat.audit;

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

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.RecentAuditFileStream;
import com.ibm.ws.security.jaspic11.fat.JASPIFatUtils;
import com.ibm.ws.security.jaspic11.fat.JASPITestBase;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 *
 * The test verifies that security audit records are emitted for JASPI Basic Authentication followed by authorization
 * for both positive and negative cases.
 *
 * The test access a protected servlet, verifies that the JASPI provider was invoked to
 * make the authentication decision and verifies that the servlet response contains the correct
 * values for getAuthType, getUserPrincipal and getRemoteUser after JASPI authentication.
 *
 * Then the tests check for the correct audit records in the audit.log.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO audit-1.0 has not been transformed for EE9
public class JASPIBasicAuthenticationAuditTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.audit");
    protected static Class<?> logClass = JASPIBasicAuthenticationAuditTest.class;
    protected String queryString = "/JASPIBasicAuthServlet/JASPIBasic";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIBasicAuthenticationAuditTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIBasicAuthServlet.war", "JASPIFormLoginServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_APP);
        verifyServerStartedWithJaspiFeature(myServer);
        AuditCommonTest.verifyAuditAndAuditFileHandlerReady(myServer);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Ignore error on server stop in defect 258461 -CWWKE0700W: [com.ibm.jbatch.container.ws.impl.BatchLocationServiceImpl(379)] Could not get service
        myServer.stopServer("CWWKZ0014W", "CWWKE0700W");

        JASPIFatUtils.uninstallJaspiUserFeature(myServer);
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
     * <LI> Login with a valid userId and password in the jaspi_basic role and verify that
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
     * The audit.log contains:
     * <OL>
     * <LI> 1) SECURITY_AUTHN event with 401 challenge for Basic auth with JASPI
     * <LI> 2) Successful SECURITY_AUTHN event showing
     * <LI> JASPI provider name
     * <LI> realm name
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser1
     * <LI> JASPI auth type - BASIC
     * <LI> 2) Successful SECURITY_AUTHZ event showing
     * <LI> realm name
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser1
     * <LI> role names - jaspi_basic
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiBasicAuthValidUserInRole_AllowedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=401",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser1",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_JASPI_AUTHTYPE + "=" + AuditEvent.CRED_TYPE_JASPI_AUTH,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser1",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=" + "[jaspi_basic]",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an unprotected servlet with no user and password with JASPI activated and
     * <LI> verify that JASPI authentication is processed. When Basic authentication is used, the JASPI test provider will check the value of
     * <LI> javax.security.auth.message.MessagePolicy.isMandatory=false and will return Authn.SUCCESS (200) without
     * <LI> authenticating the user.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 and JASPI authentication provider is invoked and returns without requiring user authentication
     * <LI> Servlet prints values for getUserPrincipal and getRemoteUser show null.
     * </OL>
     * The audit.log contains:
     * <OL>
     * <LI> 1) Successful SECURITY_AUTHN event (no 401 challenge) since JASPI returns 200 without authenticating user
     * <LI> JASPI provider name
     * <LI> credential type JASPIC since JASPI processes request on unprotected servlet call
     * <LI> no credential token since unprotected
     * <LI> 2) Successful SECURITY_AUTHZ event showing
     * <LI> credential type JASPIC
     * <LI> no role names since unprotected servlet
     * </OL>
     */
    @Test
    public void testJaspiBasicAuth_UnprotectedServletUserNoAuthCreds_ProviderDoesNotRequireAuthn_AllowedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryStringUnprotected = "/JASPIBasicAuthServlet/JASPIUnprotected";
        String response = executeGetRequestNoAuthCreds(httpclient, urlBase + queryStringUnprotected, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedUnprotectedInMessageLog();
        verifyUserResponse(response, getUserPrincipalNull, getRemoteUserNull);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIUnprotected",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIUnprotected",
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
        asserts.assertNotFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                    AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE));
        asserts.assertNotFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                    AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser1"));
        asserts.assertNotFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                    AuditEvent.TARGET_ROLE_NAMES + "=" + "[jaspi_basic]"));
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and password but not in the required jaspi_basic role and verify
     * <LI> that JASPI authentication is processed and authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Message CWWKS9104A in messages.log indicating authorization failed for user and role.
     * </OL>
     * The audit.log contains:
     * <OL>
     * <LI> 1) Successful SECURITY_AUTHN event
     * <LI> 2) Failed SECURITY_AUTHZ event with 403
     * <LI> credential type JASPIC
     * <LI> credential token jaspiuser4
     * <LI> roles jaspi_basic to show the role that was required
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiBasicAuthValidUserNoRole_DeniedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_noRoleUser, jaspi_noRolePwd, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(MSG_AUTHORIZATION_FAILED + jaspi_noRoleUser + ".*" + BasicRole);
        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=401",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser4",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser4",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=" + "[jaspi_basic]",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=403",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE));
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and bad password in the jaspi_basic role and verify that JASPI
     * <LI> authentication is processed and authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401
     * <LI> Message CWWKS1652A in messages.log indicating authentication failed for user with userID.
     * </OL>
     * <P> Audit log contains:
     * <OL>
     * <LI> AUTHN result failure with code 403 for credential type JASPIC
     * Note that the user which failed authentication is not included because the JASPI provider processes the request and does
     * not return the user name to the security runtime.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiBasicAuthBadUserPassword_DeniedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_invalidPwd, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED + queryString);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=401",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=403",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED));

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with an invalid userId and invalid password and verify that JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401
     * <LI> Message CWWKS1652A in messages.log indicating authentication failed for user with userID.
     * </OL>
     * <P> Audit log contains:
     * <OL>
     * <LI> AUTHN result failure with code 401 for credential type JASPIC
     * Note that the user which failed authentication is not included because the JASPI provider processes the request and does
     * not return the user name to the security runtime.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthBadUser_DeniedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_invalidUser, jaspi_invalidPwd, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED + queryString);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=401",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=403",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED));

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
