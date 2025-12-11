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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * Performs Dynamic Annotation tests.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DynamicAnnotationsTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.loginmethod");
    private static final Class<?> thisClass = DynamicAnnotationsTest.class;
    private static CommonTestHelper testHelper = new CommonTestHelper();

    // Keys to help readability of the test
    protected final boolean IS_MANAGER_ROLE = true;
    protected final boolean NOT_MANAGER_ROLE = false;
    protected final boolean IS_EMPLOYEE_ROLE = true;
    protected final boolean NOT_EMPLOYEE_ROLE = false;

    private static BasicAuthClient basicAuthClient;
    private static BasicAuthClient secureBasicAuthClient;
    private static BasicAuthClient conflictBasicAuthClient;
    private static String authTypeBasic = "BASIC";

    private final static String DYNAMIC_ANNOTATIONS_PURE_SERVLET = "DynamicAnnotationPure";
    private final static String DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT = "/dynamicAnnotationPure";
    private final static String DYNAMIC_ANNOTATIONS_CONFLICT_SERVLET = "DynamicAnnotationConflict";
    private final static String DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT = "/dynamicAnnotationConflict";
    private final static String METADATA_COMPLETE_TRUE_CONTEXT_ROOT = "/metadataCompleteTrueWebXML";

    // Users defined by role
    private final static String employeeUser = "user1";
    private final static String employeePassword = "user1pwd";
    private final static String managerUser = "user2";
    private final static String managerPassword = "user2pwd";
    private final static String declaredManagerUser = "user7";
    private final static String declaredManagerPassword = "user7pwd";
    private final static String declaredManagerDynUser = "user8";
    private final static String declaredManagerDynPassword = "user8pwd";
    private final static String runAsUserEmployee = "user98";
    private final static String runAsUserEmployeeCheck = "\\A[\\s\\S]*RunAs subject: Subject:\\s*Principal: WSPrincipal:" + runAsUserEmployee + "\\s[\\s\\S]*\\z";
    private final static String runAsUserManager = "user99";
    private final static String runAsUserManagerCheck = "\\A[\\s\\S]*RunAs subject: Subject:\\s*Principal: WSPrincipal:" + runAsUserManager + "\\s[\\s\\S]*\\z";

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };

    @BeforeClass
    public static void setUp() throws Exception {
        /*
         * These tests have not been configured to run with the local LDAP server.
         */
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);

        JACCFatUtils.installJaccUserFeature(server);
        JACCFatUtils.transformApps(server, "loginmethod.ear");

        server.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        /*
         * Max wait time based on Windows test failure sample: CWWKZ0001I: Application loginmethod started in 290.026 seconds.
         */
        Log.info(thisClass, "setup", "Waiting for loginmethod to start: CWWKZ0001I: Application loginmethod started");
        assertNotNull("The applicaiton loginmethod did not report as started, if this is a Windows run, may need more time to complete",
                      server.waitForStringInLog("CWWKZ0001I: Application loginmethod started", 300000));

        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I"));
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I"));

        basicAuthClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, DYNAMIC_ANNOTATIONS_PURE_SERVLET, DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT);
        secureBasicAuthClient = new SSLBasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, DYNAMIC_ANNOTATIONS_PURE_SERVLET, DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT);
        conflictBasicAuthClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, DYNAMIC_ANNOTATIONS_CONFLICT_SERVLET, DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT);
        basicAuthClient.setJaccValidation(true);
        secureBasicAuthClient.setJaccValidation(true);
        conflictBasicAuthClient.setJaccValidation(true);
    }

    protected static void verifyServerStartedWithJaccFeature(LibertyServer server) {
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("SRVE9956W", "SRVE8500W");
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(server);
        }
    }

    /**
     *
     * DynamicAnnotationPure1 - All methods are denied
     *
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using the GET method.
     * <LI> The dynamic annotation specifies that all methods are denied access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure1GetDeniedAccess() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure1";
        testHelper.accessGetProtectedServletWithInvalidCredentials(testUrl, employeeUser, employeePassword, null, server);
    }

    /**
     *
     * DynamicAnnotationPure2 - All methods are unprotected but require SSL
     *
     */

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method and SSL.
     * <LI> The dynamic annotations specify that SSL is required.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testDynAnnPure2GetUnprotectedWithSSL() throws Exception {
        String response = secureBasicAuthClient.accessUnprotectedServlet("/DynamicAnnotationPure2");
        assertTrue("Verification of programmatic APIs failed",
                   secureBasicAuthClient.verifyUnauthenticatedResponse(response));
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method without using SSL.
     * <LI> The dynamic annotations specify that SSL is required, so a request via http is redirected to https.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the unprotected servlet.
     * <LI> The request is redirected to https.
     * </OL>
     */
    @Test
    public void testDynAnnPure2GetSSLRedirect() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure2";
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure2";
        testHelper.testSSLRedirectGet(testUrl, employeeUser, employeePassword, secureUrl, server);
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet through POST without using SSL.
     * <LI> The dynamic annotations specify that SSL is required.
     * <LI> The request redirects to SSL with GET method because SSL is required.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the unprotected servlet.
     * <LI> The request is redirected to https.
     * </OL>
     */
    @Test
    public void testDynAnnPure2PostSSLRedirect() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure2";
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure2";
        testHelper.testSSLRedirectPost(testUrl, employeeUser, employeePassword, 200, secureUrl, server);
    }

    /**
     *
     * DynamicAnnotationPure3 - All methods (eg. CUSTOM) are denied access except:
     * GET requires Manager, POST requires Employee and SSL. @RunAs Manager
     *
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The dynamic annotations specify that Manager is required for GET, and @RunAs is set to Manager.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Manager userId (user2) and password is passed in to the servlet and allowed access
     * </OL>
     */
    @Test
    public void testDynAnnPure3GetWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure3b";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, managerUser, managerPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The dynamic annotations specify that Manager is required for GET, but @RunAs is set to Employee.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Employee userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure3GetFailWithEmployee() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   basicAuthClient.accessProtectedServletWithInvalidCredentials("/DynamicAnnotationPure3b", employeeUser, employeePassword));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The dynamic annotations specify that Employee is required for POST, and @RunAs is set to Manager.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Employee userId (user1) and password is passed in to the servlet.
     * <LI> Access is permitted and RunAs is set to Manager.
     * </OL>
     */
    @Test
    public void testDynAnnPure3PostWithEmployee() throws Exception {
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure3b";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(secureUrl, employeeUser, employeePassword, secureUrl, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST and SSL.
     * <LI> The dynamic annotations specify that Employee is required for POST, and @RunAs is set to Manager.
     * <LI> The dynamic annotations require SSL.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Manager userId (user2) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure3PostFailWithManager() throws Exception {
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure3b";
        testHelper.accessPostProtectedServletWithInvalidCredentials(secureUrl, managerUser, managerPassword, secureUrl, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST without SSL.
     * <LI> The dynamic annotations specify that Employee is required for POST, and @RunAs is set to Employee.
     * <LI> The dynamic annotations require SSL. Without SSL, servlet redirects to SSL with GET method, which requires Manager.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Employee userId (user2) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure3PostFailWithoutSSL() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure3b";
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure3b";
        testHelper.testSSLRedirectPost(testUrl, employeeUser, employeePassword, 403, secureUrl, server);
    }

    /**
     *
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>Access is denied for the CUSTOM method.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet for the doCUSTOM call.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure3CustomDeniedAccess() throws Exception {
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure3b";
        String response = testHelper.processDoCustom(testUrl, false, port, employeeUser, employeePassword, server);
        assertTrue("Expecting 403 response code", response.contains("HTTP/1.0 403"));
    }

    /**
     *
     * DynamicAnnotationPure4 - All methods (eg. CUSTOM) are unprotected except GET requires DeclaredManagerDyn (user8), POST is denied
     *
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The dynamic annotations specify that DeclaredManagerDyn is required for GET.
     * <LI> Login with a valid DeclaredManagerDyn userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid DeclaredManagerDyn userId (user8) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnPure4GetWithDeclaredManagerDyn() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure4b";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, declaredManagerDynUser, declaredManagerDynPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(declaredManagerDynUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The dynamic annotations specify that DeclaredManagerDyn is required for GET.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) and password is passed in to the servlet for the doCUSTOM call.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure4GetFailWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure4b";
        testHelper.accessGetProtectedServletWithInvalidCredentials(testUrl, managerUser, managerPassword, null, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using the POST method.
     * <LI> The dynamic annotation specifies that the POST method is denied access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure4PostDeniedAccess() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure4b";
        testHelper.accessPostProtectedServletWithInvalidCredentials(testUrl, employeeUser, employeePassword, null, server);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a unprotected servlet using a java call to make a doCustom call to the URL.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>User can access the servlet from the doCUSTOM call
     * </OL>
     */
    @Test
    public void testDynAnnPure4CustomUnprotected() throws Exception {
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure4b";
        String response = testHelper.processDoCustom(testUrl, false, port, null, null, server);
        assertTrue("getRemoteUser is not null.", response.contains("getRemoteUser: null"));
    }

    /**
     *
     * DynamicAnnotationPure5 - All methods (eg. POST) require DeclaredManager (user7) except GET is unprotected, CUSTOM requires Employee (user1). @RunAs set to Manager (user99)
     *
     */

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource, but should RunAs Manager (user99)
     * </OL>
     */
    @Test
    public void testDynAnnPure5GetUnprotected() throws Exception {
        String response = basicAuthClient.accessUnprotectedServlet("/DynamicAnnotationPure5");
        assertTrue("Verification of programmatic APIs failed",
                   basicAuthClient.verifyUnauthenticatedResponse(response));
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The dynamic annotations specify that DeclaredManager is required for POST. @RunAs set to Manager
     * <LI> Login with a valid DeclaredManager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid DeclaredManager userId (user7) and RunAs Manager (user99) is allowed access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnPure5PostWithDeclaredManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure5";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(testUrl, declaredManagerUser, declaredManagerPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(declaredManagerUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The dynamic annotations specify that DeclaredManager is required for POST. @RunAs set to Manager
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) and password is passed in to the servlet for the doCUSTOM call.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure5PostFailWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure5";
        testHelper.accessPostProtectedServletWithInvalidCredentials(testUrl, managerUser, managerPassword, null, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The dynamic annotations specify that DeclaredManager is required for POST. @RunAs set to Manager
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) and password is passed in to the servlet for the doCUSTOM call.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure5PostFailWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/DynamicAnnotationPure5";
        testHelper.accessPostProtectedServletWithInvalidCredentials(testUrl, employeeUser, employeePassword, null, server);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>The id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet for the doCUSTOM call
     * <LI> Access is allowed to the protected resource with RunAs set to Manager.
     * </OL>
     */
    @Test
    public void testDynAnnPure5CustomWithEmployee() throws Exception {
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/dynamicAnnotation/DynamicAnnotationPure5";
        String response = testHelper.processDoCustom(testUrl, true, port, employeeUser, employeePassword, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>The id and pw are base 64 encoded
     * <LI>The servlet requires the Employee role.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user2) and password is passed in to the servlet for the doCUSTOM call
     * <LI> Authorization denied, 403, to the protected resource
     * </OL>
     */
    @Test
    public void testDynAnnPure5CustomFailWithManager() throws Exception {
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/dynamicAnnotation/DynamicAnnotationPure5";
        String response = testHelper.processDoCustom(testUrl, true, port, managerUser, managerPassword, server);
        assertTrue("Expecting 403 response code", response.contains("HTTP/1.0 403"));
    }

    /**
     *
     * DynamicAnnotationPure6 - GET allows all roles, POST requires Manager, CUSTOM requires Employee and SSL. @RunAs set to Manager
     * Includes multiple URL patterns:
     * - for /DynamicAnnotationPure6, URL has conflict with web.xml so is unprotected
     * - for /dynamicAnnotation/DynamicAnnotationPure6, URL is unique and follows dynamic constraints defined above
     *
     */

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Web.xml, which takes precedent for this URL, specifies unprotected for servlet
     * </OL>
     */
    @Test
    public void testDynAnnPure6GetUnprotected() throws Exception {
        String response = basicAuthClient.accessUnprotectedServlet("/DynamicAnnotationPure6");
        assertTrue("Verification of programmatic APIs failed",
                   basicAuthClient.verifyUnauthenticatedResponse(response));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET and an alternate URL.
     * <LI> The dynamic annotations specify that all authenticated users are allowed for GET. @RunAs set to Manager (user99)
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnPure6GetWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/dynamicAnnotation/DynamicAnnotationPure6";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The dynamic annotations specify that Manager is required for POST. @RunAs set to Manager (user99)
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnPure6PostWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/dynamicAnnotation/DynamicAnnotationPure6";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(testUrl, managerUser, managerPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The dynamic annotations specify that Manager is required for POST.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Employee userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure6PostFailWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/dynamicAnnotation/DynamicAnnotationPure6";
        testHelper.accessPostProtectedServletWithInvalidCredentials(testUrl, employeeUser, employeePassword, null, server);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>The id and pw are base 64 encoded
     * <LI>A request via http is redirected to https.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet for the doCUSTOM call
     * <LI> and should return the correct values for the passed-in user.
     * <LI> The request is redirected to https. Access is allowed to the protected resource with RunAs set to Manager.
     * </OL>
     */
    @Test
    public void testDynAnnPure6CustomWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/dynamicAnnotation/DynamicAnnotationPure6";
        String sslTestUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT
                            + "/dynamicAnnotation/DynamicAnnotationPure6";

        // First access without SSL
        int port = server.getHttpDefaultPort();
        String response = testHelper.processDoCustom(testUrl, true, port, employeeUser, employeePassword, server);
        // validate the request was redirected to https URL
        assertTrue("Failed to redirect to https site.", response.contains("Location: " + sslTestUrl));

        // Then access https URL
        int securePort = server.getHttpDefaultSecurePort();
        response = testHelper.processDoCustom(sslTestUrl, true, securePort, employeeUser, employeePassword, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL with SSL.
     * <LI>The id and pw are base 64 encoded
     * <LI>The servlet requires the Employee role and SSL.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A Manager userId (user2) and password is passed in to the servlet for the doCUSTOM call.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnPure6CustomFailWithManager() throws Exception {
        int port = server.getHttpDefaultSecurePort();
        String testUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + DYNAMIC_ANNOTATIONS_PURE_CONTEXT_ROOT
                         + "/dynamicAnnotation/DynamicAnnotationPure6";
        String response = testHelper.processDoCustom(testUrl, true, port, managerUser, managerPassword, server);
        assertTrue("Expecting 403 response code", response.contains("HTTP/1.0 403"));
    }

    /**
     * DynamicAnnotationConflict1 - Name and URL conflict with web.xml, so web.xml takes precedent
     *
     * Web.xml - unprotected
     * Dynamic annotation - N/A
     *
     * RESULT: All methods are unprotected
     */

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method.
     * <LI> web.xml, which takes precedent, is unprotected for all methods
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testDynAnnConflict1GetUnprotected() throws Exception {
        String response = conflictBasicAuthClient.accessUnprotectedServlet("/DynamicAnnotationConflict1");
        assertTrue("Verification of programmatic APIs failed",
                   conflictBasicAuthClient.verifyUnauthenticatedResponse(response));
    }

    /**
     * DynamicAnnotationConflict2 - Name and URL conflict with static annotation, so static annotation takes precedent
     *
     * Static annotation - All methods require Employee (user1) role
     * Dynamic annotation - N/A
     *
     * RESULT: All methods require Employee (user1)
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> Static annotation, which takes precedent, specific that GET requires Employee role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnConflict2GetWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict2";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     * DynamicAnnotationConflict3 - RunAs and security constraint conflict with web.xml, so web.xml takes precedent
     *
     * Web.xml - All methods are unprotected, RunAs Manager (user99)
     * Dynamic annotation - All methods require Employee (user1) role, RunAs Employee (user98)
     * web-fragment.xml - RunAs Employee (user98), which is ignored due to conflict with web.xml
     *
     * RESULT: All methods are unprotected, RunAs Manager
     */

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method.
     * <LI> web.xml, which takes precedent, is unprotected for all methods and RunAs Manager
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testDynAnnConflict3GetUnprotected() throws Exception {
        String response = conflictBasicAuthClient.accessUnprotectedServlet("/DynamicAnnotationConflict3");
        assertTrue("Verification of programmatic APIs failed",
                   conflictBasicAuthClient.verifyUnauthenticatedResponse(response));
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a unprotected servlet using a java call to make a doCustom call to the URL.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>User can access the unprotected servlet from the doCUSTOM call
     * </OL>
     */
    @Test
    public void testDynAnnConflict3CustomUnprotected() throws Exception {
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict3";
        String response = testHelper.processDoCustom(testUrl, false, port, null, null, server);
        assertTrue("getRemoteUser is not null.", response.contains("getRemoteUser: null"));
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an protected servlet using GET method.
     * <LI> The programmatic annotation protects the servlet with the Employee role
     * <LI> The web.xml, which takes precedence for RunAS, is set to the Manager role
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet for the GET call
     * <LI> and should return the correct values for the passed-in user, runAs Manager
     * </OL>
     */
    @Test
    public void testDynAnnConflict3GetNew_ConstrainsFromDynamic_RunAsFromDD() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict3New";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);

        assertTrue("Verification of programmatic APIs failed", conflictBasicAuthClient.verifyResponse(response, employeeUser, true, false));
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * DynamicAnnotationConflict4 - RunAs and security constraint conflict with static annotation, so dynamic takes precedent
     *
     * Static annotation - All methods require Manager (user2) role, RunAs Employee (user98)
     * Dynamic annotation - All methods require Employee (user1) role, RunAs Manager (user99)
     *
     * RESULT: For /DynamicAnnotationConflict4 and /DynamicAnnotationConflict4a, follow security constraints from dynamic and RunAs from static. All methods require
     * Employee (user1) role, RunAs Employee (user98)
     * For /DynamicAnnotationConflict4b, follow RunAs and security constraints from dynamic. All methods require Employee (user1) role, RunAs Manager (user99)
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet /DynamicAnnotationConflict4 using GET.
     * <LI> Static annotation, which takes precedent, specific that GET requires Employee role, RunAs Employee.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnConflict4GetWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict4";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserEmployee, response.matches(runAsUserEmployeeCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet /DynamicAnnotationConflict4a using POST.
     * <LI> The dynamic annotations specify that Employee is required for POST, RunAs Employee.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnConflict4aPostWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict4a";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserEmployee, response.matches(runAsUserEmployeeCheck));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet /DynamicAnnotationConflict4b using a java call to make a doCustom call to the URL.
     * <LI>The id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet for the doCUSTOM call
     * <LI> and should return the correct values for the passed-in user, runAs Manager
     * </OL>
     */
    @Test
    public void testDynAnnConflict4bCustomWithEmployee() throws Exception {
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict4b";
        String response = testHelper.processDoCustom(testUrl, true, port, employeeUser, employeePassword, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * DynamicAnnotationConflict5 - RunAs conflict with web.xml and static annotation, so web.xml takes precedent
     *
     * Web.xml - All methods are unprotected, RunAs Manager (user99)
     * Static annotation - All methods require Manager (user2) role, RunAs Employee (user98)
     * Dynamic annotation - All methods require Employee (user1) role, RunAs Employee (user98)
     *
     * RESULT: All methods are unprotected, RunAs Manager (user99)
     */

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method.
     * <LI> web.xml, which takes precedent, is unprotected for all methods and RunAs Manager
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testDynAnnConflict5GetUnprotected() throws Exception {
        String response = conflictBasicAuthClient.accessUnprotectedServlet("/DynamicAnnotationConflict5");
        assertTrue("Verification of programmatic APIs failed",
                   conflictBasicAuthClient.verifyUnauthenticatedResponse(response));
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a unprotected servlet using a java call to make a doCustom call to the URL.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>User can access the unprotected servlet from the doCUSTOM call
     * </OL>
     */
    @Test
    public void testDynAnnConflict5CustomUnprotected() throws Exception {
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict5";
        String response = testHelper.processDoCustom(testUrl, false, port, null, null, server);
        assertTrue("getRemoteUser is not null.", response.contains("getRemoteUser: null"));
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * DynamicAnnotationConflict6 - Wildcard test, with same classname but no URL conflict
     *
     * Web.xml - All methods are unprotected for /DynamicAnnotationConflict6/a
     * Static annotation - All methods (GET, CUSTOM) require Manager (user2) role, except POST which is denied for /DynamicAnnotationConflict6/b
     * Dynamic annotation - All methods (POST, CUSTOM) requires Manager (user2) role, except GET which requires SSL and Employee (user1) role for /DynamicAnnotationConflict6/c
     * web-fragment.xml - Run-as role Manager (user99) for /DynamicAnnotationConflict6/a
     *
     * RESULT: Follow web.xml for /DynamicAnnotationConflict6/a, but also run-as role from web-fragment.xml
     * Static annotation for /DynamicAnnotationConflict6/b,
     * Dynamic annotation for /DynamicAnnotationConflict6/c
     */

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet /DynamicAnnotationConflict6/a using GET method.
     * <LI> web.xml, which takes precedent, is unprotected for all methods
     * <LI> web-fragment.xml, which takes precedent for run-as role, is Manager (user99)
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testDynAnnConflict6aGetUnprotected() throws Exception {
        String response = conflictBasicAuthClient.accessUnprotectedServlet("/DynamicAnnotationConflict6/a");
        assertTrue("Verification of programmatic APIs failed",
                   conflictBasicAuthClient.verifyUnauthenticatedResponse(response));
        assertTrue("RunAs user not found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet /DynamicAnnotationConflict6/b using GET.
     * <LI> Static annotation, which takes precedent, specific that GET requires Manager role
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnConflict6bGetWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict6/b";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, managerUser, managerPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet /DynamicAnnotationConflict6/b using the POST method.
     * <LI> Static annotation, which takes precedent, specifies that the POST method is denied access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnConflict6bPostDeniedAccess() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict6/b";
        testHelper.accessPostProtectedServletWithInvalidCredentials(testUrl, employeeUser, employeePassword, null, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet /DynamicAnnotationConflict6/c using GET and SSL.
     * <LI> Dynamic annotation, which takes precedent, specify that Employee and SSL is required for GET
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnConflict6cGetWithEmployee() throws Exception {
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT
                           + "/DynamicAnnotationConflict6/c";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(secureUrl, employeeUser, employeePassword, secureUrl, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet /DynamicAnnotationConflict6/c using GET and SSL.
     * <LI> Dynamic annotation, which takes precedent, specify that Employee and SSL is required for GET
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Manager userId (user2) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testDynAnnConflict6cGetFailWithManager() throws Exception {
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT
                           + "/DynamicAnnotationConflict6/c";
        testHelper.accessGetProtectedServletWithInvalidCredentials(secureUrl, managerUser, managerPassword, secureUrl, server);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet /DynamicAnnotationConflict6/c using a java call to make a doCustom call to the URL.
     * <LI>The id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user2) and password is passed in to the servlet for the doCUSTOM call
     * <LI> and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testDynAnnConflict6cCustomWithManager() throws Exception {
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict6/c";
        String response = testHelper.processDoCustom(testUrl, true, port, managerUser, managerPassword, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
    }

    /**
     * DynamicAnnotationConflict7 - Test with getServletRegistration() where servlet is defined in web.xml, and constraint is defined in static annotation
     *
     * Web.xml - Only servlet defined
     * Static annotation - All methods require Manager (user2) role
     * Dynamic annotation - getServletRegistration() called on existing servlet. addMapping has URL conflict
     *
     * RESULT: Constraint in static annotation takes precedent. URL conflict in addMapping will be ignored
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> Static annotation, which takes precedent, specify that Manager is required for POST
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Manager userId (user2) and password is permitted access to the protected servlet
     * </OL>
     */
    @Test
    public void testDynAnnConflict7PostWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT
                         + "/DynamicAnnotationConflict7";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(testUrl, managerUser, managerPassword, testUrl, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
    }

    /**
     * DynamicAnnotationConflict8 - Test with getServletRegistration() where servlet and URL are defined in web.xml, and constraint is defined in static annotation
     *
     * Web.xml - Servlet and URL defined
     * Static annotation - All methods require Employee (user1) role
     * Dynamic annotation - getServletRegistration() called on existing servlet. addMapping with new URL
     *
     * RESULT: Constraint in static annotation should take precedent with existing URL /DynamicAnnotationConflict8/a as well as new URL /DynamicAnnotationConflict8/b defined in
     * dyanmic
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet /DynamicAnnotationConflict8/a using POST.
     * <LI> Static annotation, which takes precedent, specify that Employee is required for POST
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnConflict8aPostWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict8/a";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet /DynamicAnnotationConflict8/b using a java call to make a doCustom call to the URL.
     * <LI>The id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet for the doCUSTOM call
     * <LI> and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testDynAnnConflict8bCustomWithEmployee() throws Exception {
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict8/b";
        String response = testHelper.processDoCustom(testUrl, true, port, employeeUser, employeePassword, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     * DynamicAnnotationConflict9 - Test with getServletRegistration() where servlet, URL, and constraint are defined in web.xml, and constraint is defined in static annotation
     * for additional URLs
     *
     * Web.xml - servlet, URL, and constraint defined, which is unprotected for all roles
     * Static annotation - All methods require Employee (user1) role
     * Dynamic annotation - getServletRegistration() called on existing servlet. addMapping with new URL
     *
     * RESULT: Constraint in web.xml should take precedent with existing URL /DynamicAnnotationConflict9/a, constraint in static annotation should take precedent with new URL
     * /DynamicAnnotationConflict9/b
     */

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet /DynamicAnnotationConflict9/a using GET method.
     * <LI> Web.xml, which takes precedent, is unprotected for all roles
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testDynAnnConflict9aGetUnprotected() throws Exception {
        String response = conflictBasicAuthClient.accessUnprotectedServlet("/DynamicAnnotationConflict9/a");
        assertTrue("Verification of programmatic APIs failed",
                   conflictBasicAuthClient.verifyUnauthenticatedResponse(response));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet /DynamicAnnotationConflict9/b using POST.
     * <LI> Static annotation, which takes precedent, specify that Employee is required for POST
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testDynAnnConflict9bPostWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + DYNAMIC_ANNOTATIONS_CONFLICT_CONTEXT_ROOT + "/DynamicAnnotationConflict9/b";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     *
     * Verify the following:
     * <LI>Even with metadata-complete=true in web.xml, this does not affect dynamic annotations
     * <LI>Attempt to access a protected servlet with the GET method
     * <LI>Access is denied for the GET method.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testMetadataCompleteTrueWebXML3GetDenied() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + METADATA_COMPLETE_TRUE_CONTEXT_ROOT + "/MetadataCompleteTrueWebXML3";
        testHelper.accessGetProtectedServletWithInvalidCredentials(testUrl, employeeUser, employeePassword, null, server);
    }

    /**
     *
     * Verify the following:
     * <LI>web.xml - empty
     * <LI>web-fragment.xml in webFragmentDeploymentFailureForRunAs1.jar - servlet with run-as Employee
     * <LI>web-fragment.xml in webFragmentDeploymentFailureForRunAs2.jar - servlet with run-as Manager
     * <LI>RESULTS - Due to run-as conflict in web-fragment.xml files, app should not install
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> Due to run-as conflict in web-fragment.xml files, app should not install
     * </OL>
     */
    //Expect an FFDC here to let users know why app fails to install
    @ExpectedFFDC("com.ibm.wsspi.adaptable.module.UnableToAdaptException")
    @AllowedFFDC({ "com.ibm.ws.container.service.metadata.MetaDataException", "java.util.concurrent.ExecutionException", "com.ibm.websphere.security.WSSecurityException",
                   "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void testWebFragmentDeploymentFailureForRunAs() throws Exception {
        LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.webFragmentDeploymentFailureForRunAs");
        try {
            libertyServer.addInstalledAppForValidation("webFragmentDeploymentFailureForRunAs");
            libertyServer.startServer(true);
        } catch (Exception e) {
            assertTrue("Expect app to fail, but got: " + e.getMessage(),
                       e.getMessage().contains("exception occurred while starting the application webFragmentDeploymentFailureForRunAs"));
        } finally {
            if (libertyServer.isStarted()) {
                libertyServer.stopServer("CWWKZ0002E", "CWWKO0221E");
            }
        }
    }

    /**
     *
     * Verify the following:
     * <LI>web.xml - empty
     * <LI>web-fragment.xml in webFragmentDeploymentFailureForAuthConstraint1.jar - servlet with role Employee
     * <LI>web-fragment.xml in webFragmentDeploymentFailureForAuthConstraint2.jar - servlet with role Manager
     * <LI>RESULTS - Due to auth-constraint conflict in web-fragment.xml files, app should not install
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> Due to auth-constraint conflict in web-fragment.xml files, app should not install
     * </OL>
     */
    //Expect an FFDC here to let users know why app fails to install
    @ExpectedFFDC("com.ibm.wsspi.adaptable.module.UnableToAdaptException")
    @AllowedFFDC({ "com.ibm.ws.container.service.metadata.MetaDataException", "java.util.concurrent.ExecutionException" })
    @Test
    public void testWebFragmentDeploymentFailureForAuthConstraint() throws Exception {
        LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.webFragmentDeploymentFailureForAuthConstraint");
        try {
            libertyServer.addInstalledAppForValidation("webFragmentDeploymentFailureForAuthConstraint");
            libertyServer.startServer(true);
        } catch (Exception e) {
            assertTrue("Expect app to fail, but got: " + e.getMessage(),
                       e.getMessage().contains("exception occurred while starting the application webFragmentDeploymentFailureForAuthConstraint"));
        } finally {
            if (libertyServer.isStarted()) {
                libertyServer.stopServer("CWWKZ0002E", "CWWKO0221E");
            }
        }
    }

    /**
     *
     * Verify the following:
     * <LI>web.xml - empty
     * <LI>web-fragment.xml in webFragmentDeploymentFailureForUserDataConstraint1.jar - servlet with role Employee
     * <LI>web-fragment.xml in webFragmentDeploymentFailureForUserDataConstraint2.jar - servlet with role Manager
     * <LI>RESULTS - Due to user-data-constraint conflict in web-fragment.xml files, app should not install
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> Due to user-data-constraint conflict in web-fragment.xml files, app should not install
     * </OL>
     */
    //Expect an FFDC here to let users know why app fails to install
    @ExpectedFFDC("com.ibm.wsspi.adaptable.module.UnableToAdaptException")
    @AllowedFFDC({ "com.ibm.ws.container.service.metadata.MetaDataException", "java.util.concurrent.ExecutionException" })
    @Test
    public void testWebFragmentDeploymentFailureForUserDataConstraint() throws Exception {
        LibertyServer libertyServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.webFragmentDeploymentFailureForUserDataConstraint");
        try {
            libertyServer.addInstalledAppForValidation("webFragmentDeploymentFailureForUserDataConstraint");
            libertyServer.startServer(true);
        } catch (Exception e) {
            assertTrue("Expect app to fail, but got: " + e.getMessage(),
                       e.getMessage().contains("exception occurred while starting the application webFragmentDeploymentFailureForUserDataConstraint"));
        } finally {
            if (libertyServer.isStarted()) {
                libertyServer.stopServer("CWWKZ0002E", "CWWKO0221E");
            }
        }
    }
}
