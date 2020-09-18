/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.jacc15.fat;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLServletClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.vulnerability.LeakedPasswordChecker;

/**
 *
 */
@Ignore("This is not a test")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public abstract class CommonServletTestScenarios {

    private final static String ALL_SPECIAL_CHARS = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";

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
    protected final static String innerSpaceUser = "user4";
    protected final static String innerSpacePassword = "user4 pwd";
    protected final static String trailingSpaceUser = "user5";
    protected final static String trailingSpacePassword = "user5pwd ";
    protected final static String noRoleComplexUser = "http://www.no-role-realm.com/user/";
    protected final static String complexEmployeeUser = "http://user.employee-realm.com/";
    protected final static String complexManagerUser = "http://user.manager-realm.com/";
    protected final static String complexEmployeeGroupUser = "http://user.employee-group-realm.com/";
    protected final static String complexUnauthorizedGroupUser = "http://user.unauthorized-group-realm.com/";
    protected final static String complexUserSpecialChars = "https://user.test-realm.com:1234/" + ALL_SPECIAL_CHARS + "/end/";
    protected final static String complexUserPassword = "complexpwd";

    // Values to be set by the child class
    protected LibertyServer server;
    protected Class<?> logClass;
    protected ServletClient client;
    protected SSLServletClient sslClient;

    private final LeakedPasswordChecker passwordChecker;

    protected CommonServletTestScenarios(LibertyServer server, Class<?> logClass,
                                         ServletClient client,
                                         SSLServletClient sslClient) {
        this.server = server;
        this.logClass = logClass;
        this.client = client;
        this.sslClient = sslClient;
        this.passwordChecker = new LeakedPasswordChecker(server);
    }

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(logClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(logClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };

    @After
    public void resetConnection() {
        client.resetClientState();
        sslClient.resetClientState();
    }

// Authentication testcase to be removed.
// This testcase did not even fail when JACC rolemapping file is empty.
//    /**
//     * Verify the following:
//     * <LI> Attempt to access an unprotected servlet.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> No challenge or login required to access the unprotected resource
//     * </OL>
//     */
//    @Ignore
//    @Mode(TestMode.LITE)
//    @Test
//    public void testAccessToUnprotectedServlet() throws Exception {
//        String response = client.accessUnprotectedServlet(BasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyUnauthenticatedResponse(response));
//    }

    /**
     * Verify the following:
     * <LI> Attempt to access a servlet that is unprotected because the security constraint does not contain a auth-constraint.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testAccessToNoAuthzConstraintInUnprotectedServlet() throws Exception {
        String response = client.accessUnprotectedServlet(BasicAuthClient.UNPROTECTED_NO_AUTH_CONSTRAINT);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyUnauthenticatedResponse(response));
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
    public void testSimpleEmployeeAuthorized() throws Exception {
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

//Same scenario covered by testSimpleEmployeeAuthorized.
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access a protected servlet configured for basic authentication.
//     * <LI> Login with a valid userId and password.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A 401 Challenge when accessing the protected page for the first time.
//     * <LI> A valid userId and password permit access to the protected servlet.
//     * </OL>
//     */
//    @Mode(TestMode.LITE)
//    @Test
//    public void testSimpleManagerAuthorized() throws Exception {
//        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
//    }

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
    @Test
    public void testSimpleEmployeeRoleByGroupAuthorized() throws Exception {
        //groupUser belongs to group2 which has manager role
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerGroupUser, managerGroupPassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, managerGroupUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
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
    @Test
    public void testSimpleNoRoleUnauthorized() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, noRoleUser, noRolePassword));
    }

// No special role for JACC
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access a protected servlet configured for basic authentication
//     * and all-Authenticated users are permitted access.
//     * <LI> Login with a valid userId and password.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A 401 Challenge when accessing the protected page for the first time.
//     * <LI> A valid userId and password permit access to the protected servlet.
//     * </OL>
//     */
//    @Mode(TestMode.LITE)
//    @Test
//    public void testAllAuthenticatedEmployeeAuthorized() throws Exception {
//        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE, employeeUser, employeePassword);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
//    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication
     * and all-Authenticated users are permitted access.
     * <LI> Login with a valid userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testAllAuthenticatedNoRoleAuthorized() throws Exception {
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE, noRoleUser, noRolePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, noRoleUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet protected which has two sets of security
     * constraints. One which requires Employee role, and another (overlapping)
     * which allows all roles.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the protected resource due
     * to the overlapped all roles constraint (any body can access this servlet).
     * </OL>
     */
    @Test
    public void testOverlapAllAccessEmployeeAuthorized() throws Exception {
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_OVERLAP_ALL_ACCESS, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet protected which has two sets of security
     * constraints. One which requires Employee role, and another (overlapping)
     * which allows all roles.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the protected resource due
     * to the overlapped all roles constraint (any body can access this servlet).
     * </OL>
     */
    @Test
    public void testOverlapAllAccessNonEmployeeAuthorized() throws Exception {
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_OVERLAP_ALL_ACCESS, noRoleUser, noRolePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, noRoleUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet protected which as two sets of security
     * constraints. One which requires allows AllAuthenticated, and the other
     * (overlapping) which has access precluded (no roles allowed).
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization denied to the servlet due to access precluded constraint
     * (no body can access this servlet).
     * </OL>
     */
    @Test
    public void testOverlapAccessPrecludedNotAuthorized() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   client.accessPrecludedServlet(BasicAuthClient.PROTECTED_OVERLAP_ACCESS_PRECLUDED));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication
     * with a valid userId and password (e.g. the user exist in the user registry)
     * but, there is an empty auth-constraint which means access is precluded
     * (no one can access the servlet since the the auth-constraint doesn't identify
     * any roles).
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> An empty auth constraint implies NO ONE has access.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testAccessPrecludedNotAuthorized() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   client.accessPrecludedServlet(BasicAuthClient.PROTECTED_ACCESS_PRECLUDED));
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
    @Test
    public void testEmployeeAuthzConstraintsEmployeeAuthorized() throws Exception {
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

//Similar testcase (testEmployeeAuthzConstraintsEmployeeAuthorized() exists
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access a servlet protected with only the Employee role and configured for basic authentication.
//     * <LI> Login with a valid userId and password that is NOT a member of the Employee role.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A 401 Challenge when accessing the protected page for the first time.
//     * <LI> The user IS NOT permitted access to the protected servlet.
//     * </OL>
//     */
//    @Test
//    public void testEmployeeAuthzConstraintsManagerNotAuthorized() throws Exception {
//        assertTrue("Expected access to be denied, but it was granted",
//                   client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE, managerUser, managerPassword));
//    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet protected with an authorization
     * constraint of * and configured for basic authentication. Access should
     * be allowed to any user that is a member of a role defined in the web.xml.
     * <LI> Login with a valid userId and password that is a member of the Employee role.
     * <LI> Access the same page a second time.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource
     * </OL>
     */
    @Test
    public void testStarAuthzConstraintEmployeeAuthorized() throws Exception {
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_OVERLAP_ALL_ACCESS, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    //Removing Star testcases except one above
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access a servlet protected with an authorization
//     * constraint of * and configured for basic authentication. Access should
//     * be allowed to any user that is a member of a role defined in the web.xml.
//     * <LI> Login with a valid userId and password that is a member of the Manager role.
//     * <LI> Access the same page a second time.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A 401 Challenge when accessing the protected page for the first time.
//     * <LI> A valid userId and password permit access to the protected servlet.
//     * <LI> No challenge or login required to access the protected resource
//     * </OL>
//     */
//    @Mode(TestMode.LITE)
//    @Test
//    public void testStarAuthzConstraintManagerAuthorized() throws Exception {
//        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_OVERLAP_ALL_ACCESS, managerUser, managerPassword);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access a servlet protected with an authorization
//     * constraint of * and configured for basic authentication. Access should
//     * be allowed to any user that is a member of a role defined in the web.xml.
//     * <LI> Login with a valid userId and password that is a member of no
//     * specified role, but is part of AllAuthenticated.
//     * <LI> Access the same page a second time.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A 401 Challenge when accessing the protected page for the first time.
//     * <LI> A valid userId and password permit access to the protected servlet.
//     * <LI> No challenge or login required to access the protected resource
//     * </OL>
//     */
//    @Test
//    public void testStarAuthzConstraintNoRoleUserAuthorized() throws Exception {
//        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_OVERLAP_ALL_ACCESS, noRoleUser, noRolePassword);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, noRoleUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access a servlet protected with an authorization
//     * constraint of * and configured for basic authentication. Access should
//     * be allowed to any user that is a member of a role defined in the web.xml.
//     * <LI> Login with an invalid userId and password.
//     * <LI> Access is denied.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A 401 Challenge when accessing the protected page for the first time.
//     * <LI> An invalid userId and password is not permitted.
//     * </OL>
//     */
//    @Test
//    public void testStarAuthzConstraintInvalidUserNotAuthorized() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_OVERLAP_ALL_ACCESS, invalidUser, invalidPassword));
//    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet with mapping of /* configured for
     * basic authentication and Employee role is permitted access.
     * <LI> Login with a valid userId and password that is a member of the Employee role.
     * <LI> Access the same page a second time.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource
     * </OL>
     */
    @Test
    public void testMatchAnyEmployeeAuthorized() throws Exception {
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_MATCH_ANY_PATTERN + "/*", employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
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
    @Test
    public void testMatchAnyManagerUnauthorized() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_MATCH_ANY_PATTERN + "/*", managerUser, managerPassword));
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
    @Test
    public void testMatchAnyNoRoleUnauthorized() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_MATCH_ANY_PATTERN + "/*", noRoleUser, noRolePassword));
    }

    // Authentication testcase to be removed.
    // This testcase did not even fail when JACC rolemapping file is empty.
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access a servlet that is unprotected because the base path /MatchAny/* is protected by
//     * basic authentication and Employee role, but the /MatchAny/noAuthConstraint contains no auth-constraint and is
//     * unprotected.
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI> No challenge or login required to access the unprotected resource
//     * </OL>
//     */
//    @Test
//    public void testAccessToNoAuthConstraintOverridesBaseConstraint() throws Exception {
//        String response = client.accessUnprotectedServlet(BasicAuthClient.PROTECTED_MATCH_ANY_PATTERN + "/noAuthConstraint");
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyUnauthenticatedResponse(response));
//    }

//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet configured for basic authentication
//     * with a incorrect userId and password (e.g. the user does not exist in the user registry).
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI>A 401 Challenge when accessing the protected page for the first time.
//     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
//     * </OL>
//     */
//    @Mode(TestMode.LITE)
//    @Test
//    public void testIncorrectUserId() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, invalidUser, invalidPassword));
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet configured for basic authentication
//     * with a valid userId and invalid password (e.g. the user does exist in the user registry).
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI>A 401 Challenge when accessing the protected page for the first time.
//     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
//     * </OL>
//     */
//    @Mode(TestMode.LITE)
//    @Test
//    public void testIncorrectPassword() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, invalidPassword));
//    }

//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet configured for basic authentication
//     * with a valid userId and but, no password (e.g. the user does exist in the user registry).
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI>A 401 Challenge when accessing the protected page for the first time.
//     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
//     * </OL>
//     */
//    @Test
//    public void testNoPasswordValidUser() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, null));
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet configured for basic authentication
//     * with a incorrect userId and no password (e.g. the user does not exist in the user registry).
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI>A 401 Challenge when accessing the protected page for the first time.
//     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
//     * </OL>
//     */
//    @Test
//    public void testNoPasswordInvalidUser() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, invalidUser, null));
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet configured for basic authentication
//     * with a valid userId but with a single blank space password (e.g. the user does exist in the user registry).
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI>A 401 Challenge when accessing the protected page for the first time.
//     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
//     * </OL>
//     */
//    @Test
//    public void testBlankPasswordValidUser() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, " "));
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet configured for basic authentication
//     * with a valid userId but with multiple blank spaces in the password (e.g. the user does exist in the user registry).
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI>A 401 Challenge when accessing the protected page for the first time.
//     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
//     * </OL>
//     */
//    @Test
//    public void testBlanksPasswordValidUser() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, "    "));
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet configured for basic authentication
//     * with a blank space userId and a single blank space password.
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI>A 401 Challenge when accessing the protected page for the first time.
//     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
//     * </OL>
//     */
//    @Test
//    public void testBlankUserBlankPassword() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, " ", " "));
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet configured for basic authentication
//     * with a blank space userId and a valid password.
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI>A 401 Challenge when accessing the protected page for the first time.
//     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
//     * </OL>
//     */
//    @Test
//    public void testBlankUserValidPassword() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, " ", employeePassword));
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet configured for basic authentication
//     * with a no userId and a no password.
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI>A 401 Challenge when accessing the protected page for the first time.
//     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
//     * </OL>
//     */
//    @Mode(TestMode.LITE)
//    @Test
//    public void testNoUserNoPassword() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, null, null));
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI>Attempt to access a protected servlet configured for basic authentication
//     * with a valid userId but with a password containing a trailing space (e.g. the user does exist in the user registry).
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI>A 401 Challenge when accessing the protected page for the first time.
//     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
//     * </OL>
//     */
//    @Test
//    public void testTrailingSpacePasswordValidUser() throws Exception {
//        assertTrue("Expected access denied, but it was granted",
//                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_ALL_ROLE, trailingSpaceUser, trailingSpacePassword));
//    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for basic authentication
     * with a valid userId but with a password containing spaces inside the password (e.g. the user does exist in the user registry).
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource
     * </OL>
     */
    @Test
    public void testPasswordContainingSpaceValidUser() throws Exception {
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE, innerSpaceUser, innerSpacePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, innerSpaceUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToEmployee is mapped to Employee
     * <LI>Login with a employee userId and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(MappedToEmployee) is true
     * </OL>
     */
    @Test
    public void testIsUserInRoleSecurityRoleRef_Employee_MappedToEmployee() throws Exception {
        String specifiedRole = "MappedToEmployee";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE + "?role=" + specifiedRole, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, "MappedToEmployee", true));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToManager is mapped to Manager
     * <LI>Login with a employee userId and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(MappedToManager) is false
     * </OL>
     */
    @Test
    public void testIsUserInRoleSecurityRoleRef_Employee_NOT_MappedToManager() throws Exception {
        String specifiedRole = "MappedToManager";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE + "?role=" + specifiedRole, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, "MappedToManager", false));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToEmployee is mapped to Employee
     * <LI>Login with a manager userId and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(MappedToEmployee) is false
     * </OL>
     */
    @Test
    public void testIsUserInRoleSecurityRoleRef_Manager_NOT_MappedToEmployee() throws Exception {
        String specifiedRole = "MappedToEmployee";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE + "?role=" + specifiedRole, managerUser, managerPassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE, "MappedToEmployee", false));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToManager is mapped to Manager
     * <LI>Login with a manager userId and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(MappedToManager) is true
     * </OL>
     */
    @Test
    public void testIsUserInRoleSecurityRoleRef_Manager_MappedToManager() throws Exception {
        String specifiedRole = "MappedToManager";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE + "?role=" + specifiedRole, managerUser, managerPassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE, specifiedRole, true));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToManager is mapped to Manager
     * <LI>Login with a user not mapped to any roles.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(MappedToManager) is false
     * </OL>
     */
    @Test
    public void testIsUserInRoleSecurityRoleRef_NoRolesUser_NOT_MappedToEmployee() throws Exception {
        String specifiedRole = "MappedToEmployee";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE + "?role=" + specifiedRole, noRoleUser, noRolePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, noRoleUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, specifiedRole, false));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToManager is mapped to Manager
     * <LI>Login with a user not mapped to any roles.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(MappedToManager) is false
     * </OL>
     */
    @Test
    public void testIsUserInRoleSecurityRoleRef_NoRolesUser_NOT_MappedToManager() throws Exception {
        String specifiedRole = "MappedToManager";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE + "?role=" + specifiedRole, noRoleUser, noRolePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, noRoleUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, specifiedRole, false));
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
    @Test
    public void testValidUserNoPasswordInLogs() throws Exception {
        client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        passwordChecker.checkForPasswordInAnyFormat(employeePassword);
    }

//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access a protected servlet configured for basic authentication.
//     * <LI> Login with a valid userId and password.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A 401 Challenge when accessing the protected page for the first time.
//     * <LI> A valid userId and password permit access to the protected servlet.
//     * </OL>
//     */
//    @Test
//    public void testInvalidUserNoPasswordInLogs() throws Exception {
//        client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, invalidUser, invalidPassword);
//        passwordChecker.checkForPasswordInAnyFormat(invalidPassword);
//    }

//    /**
//     * <P> Positive test for basic auth to verify that user can log in a protected servlet with a valid cookie
//     * <OL>
//     * <LI> Attempt to access a protected servlet configured for basic auth with a valid userId and password,
//     * <LI> Store the cookie.
//     * <LI> Attempt to access the same servlet again in a new session passing in the cookie
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid userId and password permitted access to the protected servlet.
//     * <LI> Successfully access to the servlet in a new session with the obtained cookie
//     * </OL>
//     */
//    @Mode(TestMode.LITE)
//    @Test
//    public void testLoginWithValidCookie() throws Exception {
//        // Log in and get the SSO Cookie
//        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
//        String ssoCookie = client.getCookieFromLastLogin();
//        assertNotNull("The SSO cookie was not set for the last login (it was null)", ssoCookie);
//
//        // Reset the client state so we get a new session
//        client.resetClientState();
//
//        response = client.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, ssoCookie);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
//    }
//
//    /**
//     * <P> Positive test for basic auth to verify that user can log in a protected servlet with a valid cookie
//     * <OL>
//     * <LI> Attempt to access a protected servlet configured for basic auth with a valid userId and password,
//     * <LI> Store the cookie.
//     * <LI> Attempt to access a different servlet again in a new session passing in the cookie
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid userId and password permitted access to the protected servlet.
//     * <LI> Successfully access to the servlet in a new session with the obtained cookie
//     * </OL>
//     */
//    @Test
//    public void testLoginWithValidCookieToDifferentServlet() throws Exception {
//        // Log in and get the SSO Cookie
//        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
//        String ssoCookie = client.getCookieFromLastLogin();
//        assertNotNull("The SSO cookie was not set for the last login (it was null)", ssoCookie);
//
//        // Reset the client state so we get a new session
//        client.resetClientState();
//
//        response = client.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE, ssoCookie);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
//    }
//
//    @Test
//    public void testLoginWithUnauthorizedCookie() throws Exception {
//        // Log in and get the SSO Cookie
//        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
//        assertTrue("Verification of programmatic APIs failed",
//                   client.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
//        String ssoCookie = client.getCookieFromLastLogin();
//        assertNotNull("The SSO cookie was not set for the last login (it was null)", ssoCookie);
//
//        // Reset the client state so we get a new session
//        client.resetClientState();
//
//        assertTrue("The SSO cookie for the manager employee should not be granted access to the employee servlet",
//                   client.accessProtectedServletWithUnauthorizedCookie(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE, ssoCookie));
//    }
//
//    /**
//     * <OL>Verify that user can't log in a protected servlet with an invalid cookie
//     * <LI> Attempt to access the protected servlet in a new session passing in an invalid cookie.
//     * </OL>
//     * <P>Expected Results:
//     * <OL>
//     * <LI> Failed to access to the protected servlet in a new session with an invalid cookie
//     * </OL>
//     */
//    @Mode(TestMode.LITE)
//    @Test
//    public void testLoginWithInvalidCookie() throws Exception {
//        // log in a protected servlet with an invalid cookie
//        String invalidCookie = "rrNT6b/hVfjPMFQrLk55LhW5WF58drhbF6HFyg6phovi20AWio06hqQ5tBHA5eV05cRDB5Rir0rYd69lfoLPQzQTuDInvalidCookie";
//        assertTrue("An invalid cookie should result in authorization challenge",
//                   client.accessProtectedServletWithInvalidCookie(BasicAuthClient.PROTECTED_SIMPLE, invalidCookie));
//    }

    /**
     * Test the user constraint CONFIDENTIAL, access on https port should
     * be successful.
     */
    //JACC @Mode(TestMode.LITE)
    @Test
    public void testSecureServletOnHTTPS() throws Exception {
        String response = sslClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.SSL_SECURED_SIMPLE, managerUser, managerPassword);
        assertTrue(sslClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
    }

}
