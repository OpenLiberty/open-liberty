/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.jacc15.fat.audit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.RecentAuditFileStream;
import com.ibm.ws.webcontainer.security.jacc15.fat.JACCFatUtils;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.TestConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
//public class BasicAuthAuditAUTHZTest extends CommonServletTestScenarios {
public class BasicAuthAuditAUTHZTest {
    private static String DEFAULT_CONFIG_FILE = "basicauthaudit.server.orig.xml";
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauth.audit");
    private static Class<?> myLogClass = BasicAuthAuditAUTHZTest.class;
    private static BasicAuthClient myClient;
    private static SSLBasicAuthClient mySSLClient;
    private static String appName = "basicauth";

    // Keys to help readability of the test
    protected final boolean IS_MANAGER_ROLE = true;
    protected final boolean NOT_MANAGER_ROLE = false;
    protected final boolean IS_EMPLOYEE_ROLE = true;
    protected final boolean NOT_EMPLOYEE_ROLE = false;

    // Users defined by role
    protected final static String realm = "BasicRealm";
    protected final static String invalidUser = "invalidUser";
    protected final static String invalidPassword = "invalidPwd";
    protected final static String employeeUser = "user1";
    protected final static String employeePassword = "user1pwd";
    protected final static String managerUser = "user2";
    protected final static String managerPassword = "user2pwd";
    protected final static String managerGroupUser = "user6";
    protected final static String managerGroupPassword = "user6pwd";
    protected final static String noRoleUser = "user3";
    protected final static String noRolePassword = "user3pwd";
    protected DefaultHttpClient httpclient;
    protected static String urlBase;

    // TestName should be an instance level Rule, not a ClassRule, but static references are made to it,
    // so we will create it as a static field, then keep a reference to it from a non-static field which
    // is annotated with @Rule - junit can make the test method name changes to that field, which should
    // (hopefully) be reflected in the static references as well.
    private static TestName _name = new TestName();

    /**
     * Need the first repeat to make sure that audit-2.0 from a previous repeat gets put back to audit-1.0
     */
    @ClassRule
    public static RepeatTests auditRepeat = RepeatTests.with(new FeatureReplacementAction("audit-2.0", "audit-1.0").forServerConfigPaths("publish/files/"
                                                                                                                                         + DEFAULT_CONFIG_FILE).fullFATOnly()).andWith(new FeatureReplacementAction("audit-1.0", "audit-2.0").forServerConfigPaths("publish/files/"
                                                                                                                                                                                                                                                                   + DEFAULT_CONFIG_FILE));

    @Rule
    public TestName name = _name;
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(myLogClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(myLogClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };
    private static final TestConfiguration testConfig = new TestConfiguration(myServer, myLogClass, _name, appName);

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void resetConnection() {
        myClient.resetClientState();
        httpclient.getConnectionManager().shutdown();

    }

    @BeforeClass
    public static void setUp() throws Exception {
        myServer.addInstalledAppForValidation(appName);
        //LDAPUtils.addLDAPVariables(myServer);

        JACCFatUtils.installJaccUserFeature(myServer);
        JACCFatUtils.transformApps(myServer, "basicauth.war", "basicauthXMI.ear", "basicauthXMInoAuthz.ear", "basicauthXML.ear", "basicauthXMLnoAuthz.ear");

        testConfig.startServerClean(DEFAULT_CONFIG_FILE);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaccFeature(myServer);
        }
        AuditCommonTest.verifyAuditAndAuditFileHandlerReady(myServer);

        myClient = new BasicAuthClient(myServer);
        mySSLClient = new SSLBasicAuthClient(myServer);
        myClient.setJaccValidation(true);
        mySSLClient.setJaccValidation(true);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
    }

    protected static void verifyServerStartedWithJaccFeature(LibertyServer server) {
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen
    }

    public BasicAuthAuditAUTHZTest() {
        //super(myServer, myLogClass, myClient, mySSLClient);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer("CWWKE1102W", "CWWKE1106W");
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(myServer);
        }
    }

    /**
     * Verify the following:
     *
     * Attempt to access a protected servlet configured for basic authentication.
     * Login with a valid userId and password.
     *
     * Expected Results:
     *
     * Expect to see redirect and success events, no denied events should be found
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void testValidUserAndValidPaswordForJACCAudit() throws Exception {

        RecentAuditFileStream recent = new RecentAuditFileStream(myServer.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(myLogClass, recent);

        String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   myClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/SimpleServlet",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=web",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET"),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/SimpleServlet",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                                                 AuditEvent.TARGET_REALM + "=BasicRealm",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[Employee, Manager]"));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSimpleEmployeeAuthorizedForJACCAudit() throws Exception {

        RecentAuditFileStream recent = new RecentAuditFileStream(myServer.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(myLogClass, recent);

        String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   myClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/SimpleServlet",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=web",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET"),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/SimpleServlet",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                                                 AuditEvent.TARGET_REALM + "=BasicRealm",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[Employee, Manager]"));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>Login with a valid userId and password that belongs to a group associated with role.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSimpleEmployeeRoleByGroupAuthorizedForJACCAudit() throws Exception {

        RecentAuditFileStream recent = new RecentAuditFileStream(myServer.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(myLogClass, recent);

        //groupUser belongs to group2 which has manager role
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerGroupUser, managerGroupPassword);
        assertTrue("Verification of programmatic APIs failed",
                   myClient.verifyResponse(response, managerGroupUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/SimpleServlet",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user6",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=web",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET"),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/SimpleServlet",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user6",
                                                 AuditEvent.TARGET_REALM + "=BasicRealm",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[Employee, Manager]"));

    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication
     * with a valid userId and password (e.g. the user exist in the user registry)
     * but, this user does not have permission to access this protected servlet.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSimpleNoRoleUnauthorizedForJACCAudit() throws Exception {

        RecentAuditFileStream recent = new RecentAuditFileStream(myServer.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(myLogClass, recent);

        assertTrue("Expected access to be denied, but it was granted",
                   myClient.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, noRoleUser, noRolePassword));

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/SimpleServlet",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user3",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=web",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET"),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/SimpleServlet",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user3",
                                                 AuditEvent.TARGET_REALM + "=BasicRealm",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[Employee, Manager]"));

    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet protected with only the Employee role and
     * configured for basic authentication.
     * <LI> Login with a valid userId and password that is a member of the Employee role.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> Access is granted.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testEmployeeAuthzConstraintsEmployeeAuthorizedForJACCAudit() throws Exception {

        RecentAuditFileStream recent = new RecentAuditFileStream(myServer.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(myLogClass, recent);

        String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   myClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/EmployeeRoleServlet",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=web",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET"),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/EmployeeRoleServlet",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_SUCCESS,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                                                 AuditEvent.TARGET_REALM + "=BasicRealm",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[Employee]"));

    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet protected with only the Employee role and configured for basic authentication.
     * <LI> Login with a valid userId and password that is NOT a member of the Employee role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> The user IS NOT permitted access to the protected servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMatchAnyManagerUnauthorizedForJACCAudit() throws Exception {

        RecentAuditFileStream recent = new RecentAuditFileStream(myServer.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(myLogClass, recent);

        assertTrue("Expected access to be denied, but it was granted",
                   myClient.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_MATCH_ANY_PATTERN + "/*", managerUser, managerPassword));

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/MatchAny/*",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user2",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=web",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET"),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/MatchAny/*",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user2",
                                                 AuditEvent.TARGET_REALM + "=BasicRealm",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[Employee]"));

    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet protected with only the Employee role and configured for basic authentication.
     * <LI> Login with a valid userId and password that is NOT a member of the Employee role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> The user IS NOT permitted access to the protected servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMatchAnyNoRoleUnauthorizedForJACCAudit() throws Exception {

        RecentAuditFileStream recent = new RecentAuditFileStream(myServer.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(myLogClass, recent);

        assertTrue("Expected access to be denied, but it was granted",
                   myClient.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_MATCH_ANY_PATTERN + "/*", noRoleUser, noRolePassword));

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/MatchAny/*",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user3",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=web",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET"),
                                  asserts.asJson(AuditEvent.EVENTNAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_NAME + "=/basicauth/MatchAny/*",
                                                 AuditEvent.OUTCOME + "=" + AuditEvent.OUTCOME_FAILURE,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user3",
                                                 AuditEvent.TARGET_REALM + "=BasicRealm",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[Employee]"));

    }

}
