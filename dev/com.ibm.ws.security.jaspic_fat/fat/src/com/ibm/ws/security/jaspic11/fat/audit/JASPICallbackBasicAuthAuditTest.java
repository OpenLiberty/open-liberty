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
 * The test verifies that SECURITY_AUTHN and SECURITY_AUTHZ audit events are emitted when JASPI callbacks are used
 * to authenticate the user and when the application-bnd access-id is used to authorize the user to access the resource.
 *
 * The application-bnd in server.xml is used to determine access when either the user or group is in the
 * required jaspi_basic role. The application-bnd for the servlet contains the following access-ids:
 *
 * <user name="jaspiuser101" access-id="user:JaspiRealm/jaspiuser101" />
 * <group name="JASPIGroup" access-id="group:JaspiRealm/JASPIGroup"/>
 *
 * This test also verifies that a user will see consistent information across the SECURITY_AUTHN and SECURITY_AUTHZ records for
 * - target.name
 * - target.appname
 * - target.realm
 * - target.credential.type
 * - target.credential.token (user name)
 *
 * Note that this test relies on the group name for the Group Principal callback to be passed in the group.name property
 * in the bnd.bnd file for the JASPI provider bundle.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO audit-1.0 has not been transformed for EE9
public class JASPICallbackBasicAuthAuditTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.callback.audit");
    protected static Class<?> logClass = JASPICallbackBasicAuthAuditTest.class;
    protected static String queryStringBasic = "/JASPICallbackTestBasicAuthServlet/JASPIBasic";

    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPICallbackBasicAuthAuditTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPICallbackTestBasicAuthServlet.war", "JASPICallbackTestFormLoginServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_CALLBACK_APP);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaspiFeature(myServer);
            AuditCommonTest.verifyAuditAndAuditFileHandlerReady(myServer);
        }

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
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
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Caller principal callback only with a user that is
     * <LI> NOT in the user registry and verify that callback completed and user is able to access
     * <LI> the protected servlet based on the access_id for the user being in jaspi_basic role needed for servlet access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Subject contains correct values for getUserPrincipal, getRemoteUser
     * </OL>
     * <P> The audit log should contain audit records for SECURITY_AUTHN after authenticating using JASPI callbacks and
     * for SECURITY_AUTHZ after using the access-id in the application-bnd to perform the authorization decision.
     */
    @Test
    public void testJaspiBasicAuth_CPCBOnly_UserNotInRegistryAccessIdInRole_Successful_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + CPCB_CALLBACK, jaspi_notInRegistryInBasicRoleUser,
                                                          jaspi_notInRegistryInBasicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryInBasicRoleUser, getRemoteUserFound + jaspi_notInRegistryInBasicRoleUser);
        verifyNoGroupIdsResponse(response);
        verifyRunAsUserResponse(response, jaspi_notInRegistryInBasicRoleUser);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPICallbackTestBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_CALLBACK_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=401",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPICallbackTestBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_CALLBACK_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser101",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_JASPI_AUTHTYPE + "=" + AuditEvent.CRED_TYPE_JASPI_AUTH,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPICallbackTestBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_CALLBACK_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiuser101",
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
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Caller principal and Group principal callbacks with a user that is
     * <LI> NOT in the user registry and the user is not in the role required to access the servlet. Use GPCB to add
     * <LI> a group which has access_id defined with access to the jaspi_basic role needed for the servlet access.
     * <LI> Access is allowed to the protected servlet based on application-bnd information with access_id for group in role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Subject contains correct values for getUserPrincipal, getRemoteUser
     * </OL>
     * <P> The audit log should contain audit records for SECURITY_AUTHN after authenticating using JASPI callbacks and
     * for SECURITY_AUTHZ after using the access-id in the application-bnd to perform the authorization decision.
     */
    @Test
    public void testJaspiBasicAuth_CPCBandGPCB_UserNotInRegistry_GroupAccessIdInRole_Successful_Audit() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + AuditCommonTest.DEFAULT_AUDIT_LOG);
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + CPCBGPCB_CALLBACK, jaspi_notInRegistryNotInRoleUser,
                                                          jaspi_notInRegistryNotInRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryNotInRoleUser, getRemoteUserFound + jaspi_notInRegistryNotInRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, JASPI_GROUP);
        verifyRunAsUserResponse(response, jaspi_notInRegistryNotInRoleUser);

        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPICallbackTestBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_CALLBACK_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=401",
                                                 AuditEvent.REASON_TYPE + "=" + AuditEvent.REASON_TYPE_HTTP,
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_CHALLENGE),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHN,
                                                 AuditEvent.TARGET_NAME + "=/JASPICallbackTestBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_CALLBACK_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiUser100",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_JASPI_PROVIDER + "=" + "class com.ibm.ws.security.jaspi.test.AuthProvider",
                                                 AuditEvent.TARGET_JASPI_AUTHTYPE + "=" + AuditEvent.CRED_TYPE_JASPI_AUTH,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/JASPICallbackTestBasicAuthServlet/JASPIBasic",
                                                 AuditEvent.TARGET_APPNAME + "=" + DEFAULT_CALLBACK_APP,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_JASPIC,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=jaspiUser100",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=" + "[jaspi_basic]",
                                                 AuditEvent.TARGET_METHOD + "=" + AuditEvent.TARGET_METHOD_GET,
                                                 AuditEvent.TARGET_REALM + "=JaspiRealm",
                                                 AuditEvent.REASON_CODE + "=200",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS));

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
