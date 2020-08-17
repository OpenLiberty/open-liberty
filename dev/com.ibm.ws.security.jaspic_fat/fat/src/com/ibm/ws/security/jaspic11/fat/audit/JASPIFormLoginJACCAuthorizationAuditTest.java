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
 * The test verifies that the correct audit records are emitted for JASPI authentication in conjunction with JACC authorization.
 * Both the JASPI and JACC providers are supplied as user defined features.
 * This tests access to a protected,form login servlet that requires jaspi to authenticate using a callback.
 * Access to the servlet also requires that the user be in the jaspi_form role for JACC authorization. The JACC
 * authorization check uses values in the roleMapping.props file as referenced in server.xml to authorize the authenticated user.
 *
 * This tests for audit records following both positive and negative JACC authorization following JASPI authentication. Two SECURITY_AUTHZ audit
 * records will be generated -- one with JACC specific information and one standard AUTHZ record. These tests verify that the following information
 * is included and consistent across the audit records for authentication and authorization for the user:
 * target.credential.type - JASPIC
 * target.credential.token - user name
 * target.realm - realm name
 * target.method - GET
 * target.name - URI
 * target.appaname - application name
 *
 * The test server depends upon having a roleMapping.props file under <server_name>/resources/security
 * The file contains the following role definitions for form login servlet and JACC.
 *
 * # <application name>::<role name>::<type : USER, GROUP or SPECIAL>::<accessid of user, group, or special subject name (either EVERYONE or ALLAUTHENTICATEDUSERS)):
 *
 * JASPIFormLoginServlet::jaspi_form::USER::user:JaspiRealm/jaspiuser102
 * JASPIFormLoginServlet::jaspi_form::GROUP::group:JaspiRealm/JASPIGroup
 * JASPIFormLoginServlet::jaspi_form::USER::user:JaspiRealm/jaspiuser3
 * JASPIFormLoginServlet::jaspi_form::GROUP::group:JaspiRealm/group5
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO audit-1.0 has not been transformed for EE9
public class JASPIFormLoginJACCAuthorizationAuditTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.jacc.audit");
    protected static Class<?> logClass = JASPIFormLoginJACCAuthorizationAuditTest.class;
    protected static String queryString = "/JASPIFormLoginServlet/JASPIForm";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIFormLoginJACCAuthorizationAuditTest() {
        super(myServer, logClass);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.installJaccUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIBasicAuthServlet.war", "JASPIFormLoginServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_FORM_APP);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaspiFeatureAndJacc(myServer);
            AuditCommonTest.verifyAuditAndAuditFileHandlerReady(myServer);
        }

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
        JASPIFatUtils.uninstallJaspiUserFeature(myServer);
        JASPIFatUtils.uninstallJaccUserFeature(myServer);

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
     * <LI> Attempt to access a protected servlet configured for form login with JASPI
     * <LI> authentication and JACC authorization. Login with a valid userId not in the registry with
     * <LI> caller principal and group principal callback where the group is in the required
     * <LI> jaspi_form role for JACC as defined in the JACC roleMappings.xml file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and the servlet is accessed and displays subject.
     * </OL>
     * The audit.log contains:
     * <OL>
     * <LI> 1) SECURITY_AUTHN event with 302 redirect for Form Login with JASPI
     * <LI> 2) Successful SECURITY_AUTHN 200 success
     * <LI> 3) SECURITY_AUTHZ record specific to JACC showing
     * <LI> target realm name
     * <LI> target name
     * <LI> target appname
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiUser100
     * <LI> JACC container - web
     * <LI> JACC permissions - GET
     * <LI> 4) Successful SECURITY_AUTHZ record showing the role names
     * <LI> target realm name
     * <LI> target name
     * <LI> target appname
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiUser100
     * <LI> role names - jaspi_form
     * </OL>
     */
//    @Test
    public void testJaspiFormLoginJACC_ValidUserNotInRegistryWhereGroupInRole_CPCBGPCBCallback_AllowedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString + CPCBGPCB_CALLBACK, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_notInRegistryNotInRoleUser, jaspi_notInRegistryNotInRolePwd,
                                           CPCBGPCB_CALLBACK_DESCR);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.

        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryNotInRoleUser, getRemoteUserFound + jaspi_notInRegistryNotInRoleUser);

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
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiUser100",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIForm",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiUser100",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_ROLE_NAMES + "=" + "[jaspi_form]",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=web",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIForm",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiUser100",
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
     * <LI> Attempt to access a protected servlet configured for form login with JASPI
     * <LI> authentication and JACC authorization. Login with a valid userId and password with Password valdation callback and
     * <LI> a valid registry user that is NOT in the required jaspi_form role specified in the JACC roleMappings.xml file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and that JACC authorization failed --
     * <LI> CWWKS9124A: Authorization by the JACC provider failed for user jaspiuser4 while invoking JASPIFormLoginServlet on /JASPIForm.
     * </OL>
     * The audit.log contains:
     * <OL>
     * <LI> 1) SECURITY_AUTHN event with 302 redirect for Form Login with JASPI
     * <LI> 2) Successful SECURITY_AUTHN event showing
     * <LI> JASPI provider name
     * <LI> target realm name
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser4
     * <LI> JASPI auth type - FORM
     * <LI> 3) SECURITY_AUTHZ specific to JACC showing FAILURE and 403
     * <LI> target realm name
     * <LI> target name
     * <LI> target appname
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser4
     * <LI> role names - jaspi_form ???
     * <LI> JACC container - web
     * <LI> JACC permissions - GET
     * <LI> 4) SECURITY_AUTHZ showing FAILURE and 403
     * <LI> target realm name
     * <LI> target name
     * <LI> target appname
     * <LI> credential type JASPIC
     * <LI> credential token - jaspiuser4
     * <LI> role names - jaspi_form
     * </OL>
     */
    @Test
    public void testJaspiFormLoginJACC_ValidUserInRegistryNotInRole_PVCBCallback_DeniedAccess_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString + PVCB_CALLBACK, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_noRoleUser, jaspi_noRolePwd, PVCB_CALLBACK_DESCR);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        executeGetRequestNoAuthCreds(httpclient, location, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(MSG_JACC_AUTHORIZATION_FAILED + jaspi_noRoleUser);

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
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser4",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=" + "[jaspi_form]",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=403",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=web",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPIFormLoginServlet/JASPIForm",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_FORM_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser4",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=" + "[jaspi_form]",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=403",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE));

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
