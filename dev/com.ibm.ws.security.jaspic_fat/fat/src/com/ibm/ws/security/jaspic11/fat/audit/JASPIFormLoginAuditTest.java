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
 * The test verifies that Form Login results in authentication and authorization audit records emitted for both
 * positive and negative cases when the JASPI user feature is present in the server.xml and
 * the application web.xml contains <login-config> with <auth-method>FORM</auth-method>.
 *
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO audit-1.0 has not been transformed for EE9
public class JASPIFormLoginAuditTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.audit");
    protected static Class<?> logClass = JASPIFormLoginAuditTest.class;
    protected static String queryString = "/JASPIFormLoginServlet/JASPIForm";
    protected static String queryStringUnprotected = "/JASPIFormLoginServlet/JASPIUnprotected";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIFormLoginAuditTest() {
        super(myServer, logClass);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIBasicAuthServlet.war", "JASPIFormLoginServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_FORM_APP);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaspiFeature(myServer);
            AuditCommonTest.verifyAuditAndAuditFileHandlerReady(myServer);
        }

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Ignore error on server stop in defect 258461 -CWWKE0700W: [com.ibm.jbatch.container.ws.impl.BatchLocationServiceImpl(379)] Could not get service from ref {javax.management.DynamicMBean}
        myServer.stopServer("CWWKZ0014W", "SRVE8094W", "CWWKE0700W");
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

    @Rule
    public TestName name = new TestName();

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login with JASPI activated.
     * <LI> Login with a valid userId and password where user is in a group that is in
     * <LI> jaspi_form role and verify that JASPI authentication occurs and establishes return values
     * <LI> for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Messages.log contains lines to show that JASPI authentication was processed on form display:
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> ---JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Messages.log contains line to show validateRequest called submitting the form with valid user and password
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> NOTE: Product design does not allow for secureResponse to be called here so it is not checked.
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     * The audit.log contains:
     * <OL>
     * <LI> 1) SECURITY_AUTHN event with 302 redirect for Form Login with JASPI
     * <LI> 2) Successful SECURITY_AUTHN event showing
     * <LI> JASPI provider name
     * <LI> realm name
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser5
     * <LI> JASPI auth type - FORM
     * <LI> 2) Successful SECURITY_AUTHZ event showing
     * <LI> realm name
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser5
     * <LI> role names - jaspi_form
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiFormLoginValidUserWhereGroupInRole_AllowedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_formRoleGroupUser, jaspi_formRoleGroupPwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.

        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_formRoleGroupUser, getRemoteUserFound + jaspi_formRoleGroupUser);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIForm",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_AUTHTYPE + "=" + AuditEvent.CRED_TYPE_FORM,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=302",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_REDIRECT),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIForm",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser5",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIForm",
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser5",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=" + "[jaspi_form]",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login.
     * <LI> Login with a valid userId and password but the user is not in jaspi_form role
     * <LI> required to access the servlet and verify that JASPI authentication is processed but
     * <LI> authorization fails because the user is not in the required role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Message CWWKS9104A in messages.log indicating authorization failed for user and role.
     * </OL>
     * The audit.log contains:
     * <OL>
     * <LI> 1) SECURITY_AUTHN event with 302 redirect for Form Login with JASPI
     * <LI> 2) Successful SECURITY_AUTHN event showing
     * <LI> JASPI provider name
     * <LI> realm name
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser4
     * <LI> JASPI auth type - FORM
     * <LI> 2) Successful SECURITY_AUTHZ showing FAILURE and 403
     * <LI> realm name
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser4
     * <LI> role names - jaspi_form
     * </OL>
     */
    @Test
    public void testJaspiFormLoginValidUserNoRole_DeniedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_noRoleUser, jaspi_noRolePwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        executeGetRequestNoAuthCreds(httpclient, location, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog("CWWKS9104A:.*" + jaspi_noRoleUser + ".*" + FormRole);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIForm",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_AUTHTYPE + "=" + AuditEvent.CRED_TYPE_FORM,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=302",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_REDIRECT),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIForm",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser4",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIForm",
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser4",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=" + "[jaspi_form]",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=403",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE));

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login.
     * <LI> Login with a valid userId in jaspi_form role, but null password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Form Login Error page displays
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Message CWWKS1652A in messages.log indicating authentication failed for user with userID.
     * </OL>
     * The audit.log contains:
     * <OL>
     * <LI> 1) SECURITY_AUTHN event with 302 redirect for Form Login with
     * <LI> credential type JASPIC
     * <LI> jaspi provider name
     * <LI> jaspi auth type FORM
     * <LI> 2) SECURITY_AUTHN event with 403 failure with credential type JASPIC
     * Note: the username and realm are not present because this information is not returned to the runtime from the JASPI provider
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiFormLoginValidUserNullPassword_DeniedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_formRoleUser, "BadPassword");
        String response = executeGetRequestNoAuthCreds(httpclient, location, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedOnFormLoginError(response, DEFAULT_JASPI_PROVIDER);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED + DEFAULT_FORM_LOGIN_PAGE);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIForm",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_AUTHTYPE + "=" + AuditEvent.CRED_TYPE_FORM,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=302",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_REDIRECT),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/j_security_check",
                                                 AuditEvent.TARGET_APPNAME + "=null",
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_POST,
                                                 AuditEvent.REASON_CODE + "=403",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_DENIED));
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a unprotected servlet without userid and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> The security runtime sets isMandatory=false to indicate that no authentication is required for unprotected.
     * <LI> The JASPI provider displays the login page to prompt the user for ID and password. When entered, the servlet is accessed.
     * </OL>
     * The audit.log contains:
     * <OL>
     * <LI> 1) SECURITY_AUTHN event with 302 redirect for Form Login with JASPI
     * <LI> 2) Successful SECURITY_AUTHN event showing
     * <LI> JASPI provider name
     * <LI> realm name
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser3
     * <LI> JASPI auth type - FORM
     * <LI> 2) Successful SECURITY_AUTHZ event showing
     * <LI> realm name
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser3
     * <LI> No roles since this is unprotected servlet access
     * </OL>
     */
//    @Test
    public void testJaspiFormLogin_UnprotectedURL_TestProviderRequiresAuthn_AllowedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        // Send servlet query to unprotected URL and provider displays form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryStringUnprotected, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedUnprotectedInMessageLog();

        // Fill in userid/password Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_formRoleUser, jaspi_formRolePwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_formRoleUser, getRemoteUserFound + jaspi_formRoleUser);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIUnprotected",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_AUTHTYPE + "=" + AuditEvent.CRED_TYPE_FORM,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=302",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_REDIRECT),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIUnprotected",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser3",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIUnprotected",
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser3",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));
        asserts.assertNotFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                    AuditEvent.TARGET_ROLE_NAMES + "=" + "[jaspi_form]"));

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
