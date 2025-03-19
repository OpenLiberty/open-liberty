/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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

package com.ibm.ws.webcontainer.security.jacc15.fat;

/**
 * Note:
 * 1. User registry:
 * At this time, the test uses users, passwords, roles, and groups predefined in server.xml as
 * test user registry.
 *
 * TODO:  use different user registry
 *
 * 2. The constraints (which servlets can be accessed by which user/group/role) are defined in web.xml
 *
 * 3. Note on *Overlap* test:
 * When there are more than one constraints applied to the same servlet, the least constraint will win,
 * e.g.,
 *   <auth-constraint id="AuthConstraint_5">
 <role-name>Employee</role-name>
 </auth-constraint>

 and

 <security-constraint id="SecurityConstraint_5">
 <web-resource-collection id="WebResourceCollection_5">
 <web-resource-name>Protected with overlapping * and Employee roles</web-resource-name>
 <url-pattern>/OverlapNoConstraintServlet</url-pattern>
 <http-method>GET</http-method>
 <http-method>POST</http-method>
 </web-resource-collection>
 <auth-constraint id="AuthConstraint_5">
 <role-name>*</role-name>
 </auth-constraint>
 </security-constraint>

 servlet OverlapNoConstraintServlet will allow access to all roles since
 the role = * (any role) and role =  Employee are combined and * will win.

 */

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class OSGIAppTest {

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.osgi");
    private final Class<?> logClass = OSGIAppTest.class;

    protected final static String employeeUser = "user1";
    protected final static String employeePassword = "user1pwd";
    protected final static String noRoleUser = "user3";
    protected final static String noRolePassword = "user3pwd";
    protected final static String invalidUser = "invalidUser";
    protected final static String invalidPassword = "invalidPwd";
    protected final boolean IS_MANAGER_ROLE = true;
    protected final boolean NOT_MANAGER_ROLE = false;
    protected final boolean IS_EMPLOYEE_ROLE = true;
    protected final boolean NOT_EMPLOYEE_ROLE = false;

    private static final BasicAuthClient baClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/BasicAuthWAB");
    private static final FormLoginClient flClient = new FormLoginClient(myServer, FormLoginClient.DEFAULT_SERVLET_NAME, "/FormLoginWAB");

    static {
        baClient.setJaccValidation(true);
        flClient.setJaccValidation(true);
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

    @BeforeClass
    public static void setUp() throws Exception {
        JACCFatUtils.installJaccUserFeature(myServer);
        JACCFatUtils.transformApps(myServer, "OSGIAppEBA.eba");

        myServer.addInstalledAppForValidation("OSGIAppEBA");
        myServer.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("JACC feature did not report it was starting", myServer.waitForStringInLog("CWWKS2850I"));
        assertNotNull("JACC feature did not report it was ready", myServer.waitForStringInLog("CWWKS2851I"));

        assertNotNull("Application ",
                      myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("URL not available " + myServer.waitForStringInLog("CWWKT0016I"));
    }

    @After
    public void resetConnection() {
        baClient.resetClientState();
        flClient.resetClientState();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(myServer);
        }
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
    public void testOSGIBA_SimpleEmployeeAuthorized() throws Exception {
        String response = baClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   baClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
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
    public void testOSGIBA_SimpleNoRoleUnauthorized() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   baClient.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, noRoleUser, noRolePassword));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for basic authentication
     * with a incorrect userId and password (e.g. the user does not exist in the user registry).
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>A 401 Challenge when accessing the protected page for the first time.
     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
     * </OL>
     */
    @Test
    public void testOSGIBA_IncorrectUserId() throws Exception {
        assertTrue("Expected access denied, but it was granted",
                   baClient.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SIMPLE, invalidUser, invalidPassword));
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
    public void testOSGIFL_SimpleEmployeeAuthorized() throws Exception {
        String response = flClient.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   flClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
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
    public void testOSGIFL_SimpleNoRoleUnauthorized() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   flClient.accessProtectedServletWithUnauthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, noRoleUser, noRolePassword));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for basic authentication
     * with a incorrect userId and password (e.g. the user does not exist in the user registry).
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>A 401 Challenge when accessing the protected page for the first time.
     * <LI>AuthorizationRequiredException when attempting to access the protected resource.
     * </OL>
     */
    @Test
    public void testOSGIFL_IncorrectUserId() throws Exception {
        assertTrue("Expected access denied, but it was granted",
                   flClient.accessProtectedServletWithInvalidCredentials(FormLoginClient.PROTECTED_SIMPLE, invalidUser, invalidPassword));
    }
}
