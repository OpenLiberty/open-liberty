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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Performs Static Annotation tests.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class StaticAnnotationsTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.loginmethod");
    private static final Class<?> thisClass = StaticAnnotationsTest.class;
    final static int httpDefault = server.getHttpDefaultPort();
    final static int httpsDefault = server.getHttpDefaultSecurePort();

    private static CommonTestHelper testHelper = new CommonTestHelper();
    private static BasicAuthClient basicAuthClient;
    private static BasicAuthClient secureBasicAuthClient;
    private static BasicAuthClient webXMLBasicAuthClient;
    private static BasicAuthClient mixedBasicAuthClient;
    private static BasicAuthClient metadataWebXMLBasicAuthClient;
    private static BasicAuthClient metadataWebFragmentBasicAuthClient;
    private static String authTypeBasic = "BASIC";

    private final static String STATIC_ANNOTATIONS_PURE_SERVLET = "StaticAnnotationPure";
    private final static String STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT = "/staticAnnotationPure";
    private final static String STATIC_ANNOTATIONS_WEBXML_SERVLET = "StaticAnnotationWebXML";
    private final static String STATIC_ANNOTATIONS_WEBXML_CONTEXT_ROOT = "/staticAnnotationWebXML";
    private final static String STATIC_ANNOTATIONS_MIXED_SERVLET = "StaticAnnotationMixed";
    private final static String STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT = "/staticAnnotationMixed";
    private final static String METADATA_COMPLETE_TRUE_WEBXML_SERVLET = "MetadataCompleteTrueWebXML";
    private final static String METADATA_COMPLETE_TRUE_WEBXML_CONTEXT_ROOT = "/metadataCompleteTrueWebXML";
    private final static String METADATA_COMPLETE_TRUE_WEBFRAGMENT_SERVLET = "MetadataCompleteTrueWebFragment";
    private final static String METADATA_COMPLETE_TRUE_WEBFRAGMENT_CONTEXT_ROOT = "/metadataCompleteTrueWebFragment";

    // Users defined by role
    private final static String employeeUser = "user1";
    private final static String employeePassword = "user1pwd";
    private final static String managerUser = "user2";
    private final static String managerPassword = "user2pwd";
    private final static String declaredManagerUser = "user7";
    private final static String declaredManagerPassword = "user7pwd";
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

        if (server.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaccFeature(server);
        }

        basicAuthClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, STATIC_ANNOTATIONS_PURE_SERVLET, STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT);
        secureBasicAuthClient = new SSLBasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, STATIC_ANNOTATIONS_PURE_SERVLET, STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT);
        webXMLBasicAuthClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, STATIC_ANNOTATIONS_WEBXML_SERVLET, STATIC_ANNOTATIONS_WEBXML_CONTEXT_ROOT);
        mixedBasicAuthClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, STATIC_ANNOTATIONS_MIXED_SERVLET, STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT);
        metadataWebXMLBasicAuthClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, METADATA_COMPLETE_TRUE_WEBXML_SERVLET, METADATA_COMPLETE_TRUE_WEBXML_CONTEXT_ROOT);
        metadataWebFragmentBasicAuthClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, METADATA_COMPLETE_TRUE_WEBFRAGMENT_SERVLET, METADATA_COMPLETE_TRUE_WEBFRAGMENT_CONTEXT_ROOT);
        basicAuthClient.setJaccValidation(true);
        secureBasicAuthClient.setJaccValidation(true);
        webXMLBasicAuthClient.setJaccValidation(true);
        mixedBasicAuthClient.setJaccValidation(true);
        metadataWebXMLBasicAuthClient.setJaccValidation(true);
        metadataWebFragmentBasicAuthClient.setJaccValidation(true);

        assertNotNull("The default http port should open: " + httpDefault,
                      server.waitForStringInLog("CWWKO0219I.* " + httpDefault));
        assertNotNull("The default https port should open: " + httpsDefault,
                      server.waitForStringInLog("CWWKO0219I.* " + httpsDefault));
    }

    protected static void verifyServerStartedWithJaccFeature(LibertyServer server) {
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("SRVE9956W", "SRVE8500W", "SRVE0190E");
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(server);
        }
    }

    /**
     *
     * StaticAnnotationPure1 - All methods are denied
     *
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using the GET method.
     * <LI> The servlet annotation specifies that all methods are denied access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn1GetDeniedAccess() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure1";
        testHelper.accessGetProtectedServletWithInvalidCredentials(testUrl, employeeUser, employeePassword, null, server);
    }

    /**
     *
     * StaticAnnotationPure2 - All methods are unprotected but require SSL
     *
     */

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method and SSL.
     * <LI> The servlet annotations specify that SSL is required.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testPureAnn2GetUnprotectedWithSSL() throws Exception {
        String response = secureBasicAuthClient.accessUnprotectedServlet("/StaticAnnotationPure2");
        assertTrue("Verification of programmatic APIs failed",
                   secureBasicAuthClient.verifyUnauthenticatedResponse(response));
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method without using SSL.
     * <LI> The servlet annotations specify that SSL is required, so a request via http is redirected to https.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the unprotected servlet.
     * <LI> The request is redirected to https.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testPureAnn2GetSSLRedirect() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure2";
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure2";
        testHelper.testSSLRedirectGet(testUrl, employeeUser, employeePassword, secureUrl, server);
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet through POST without using SSL.
     * <LI> The servlet annotations specify that SSL is required.
     * <LI> The request redirects to SSL with GET method because SSL is required.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the unprotected servlet.
     * <LI> The request is redirected to https.
     * </OL>
     */
    @Test
    public void testPureAnn2PostSSLRedirect() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure2";
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure2";
        testHelper.testSSLRedirectPost(testUrl, employeeUser, employeePassword, 200, secureUrl, server);
    }

    /**
     *
     * StaticAnnotationPure3 - All methods (ex. CUSTOM) are denied access except:
     * GET requires Manager, POST requires Employee and SSL,
     * RunAs Employee
     *
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations specify that Manager is required for GET, but @RunAs is set to Employee.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn3GetWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure3";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, managerUser, managerPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserEmployee, response.matches(runAsUserEmployeeCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations specify that Manager is required for GET, but @RunAs is set to Employee.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Employee userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn3GetFailWithEmployee() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   basicAuthClient.accessProtectedServletWithInvalidCredentials("/StaticAnnotationPure3", employeeUser, employeePassword));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The servlet annotations specify that Employee is required for POST, and @RunAs is set to Employee.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn3PostWithEmployee() throws Exception {
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure3";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(secureUrl, employeeUser, employeePassword, secureUrl, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
        assertTrue("RunAs user not found: " + runAsUserEmployee, response.matches(runAsUserEmployeeCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST and SSL.
     * <LI> The servlet annotations specify that Employee is required for POST, and @RunAs is set to Employee.
     * <LI> The servlet annotations require SSL.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Manager userId (user2) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn3PostFailWithManager() throws Exception {
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure3";
        testHelper.accessPostProtectedServletWithInvalidCredentials(secureUrl, managerUser, managerPassword, secureUrl, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST without SSL.
     * <LI> The servlet annotations specify that Employee is required for POST, and @RunAs is set to Employee.
     * <LI> The servlet annotations require SSL. Without SSL, servlet redirects to SSL with GET method, which requires Manager.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Employee userId (user2) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn3PostFailWithoutSSL() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure3";
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure3";
        testHelper.testSSLRedirectPost(testUrl, employeeUser, employeePassword, 403, secureUrl, server);
    }

    /**
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
    public void testPureAnn3CustomDeniedAccess() throws Exception {
        String methodName = "testPureAnn3CustomDeniedAccess";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure3";
        String rs = testHelper.processDoCustom(testUrl, false, port, employeeUser, employeePassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        assertTrue("Expecting 403 response code", rs.contains("HTTP/1.0 403"));
    }

    /**
     *
     * StaticAnnotationPure4 - All methods (ex. CUSTOM) are unprotected except GET requires DeclaredManager (user3), POST is denied
     *
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations specify that DeclaredManager is required for GET.
     * <LI> Login with a valid DeclaredManager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid DeclaredManager userId (user7) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn4GetWithDeclaredManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure4";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, declaredManagerUser, declaredManagerPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(declaredManagerUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations specify that DeclaredManager is required for GET.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) and password is passed in to the servlet for the doCUSTOM call.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn4GetFailWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure4";
        testHelper.accessGetProtectedServletWithInvalidCredentials(testUrl, managerUser, managerPassword, null, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using the POST method.
     * <LI> The servlet annotation specifies that the POST method is denied access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn4PostDeniedAccess() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure4";
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
    public void testPureAnn4CustomUnprotected() throws Exception {
        String methodName = "testPureAnn4CustomUnprotected";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure4";
        String rs = testHelper.processDoCustom(testUrl, false, port, null, null, server);
        Log.info(thisClass, methodName, "response: " + rs);
        assertTrue("getRemoteUser is not null.", rs.contains("getRemoteUser: null"));
    }

    /**
     *
     * StaticAnnotationPure5 - All methods (ex. POST) require Manager except GET is unprotected, CUSTOM requires Employee
     *
     */

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testPureAnn5GetUnprotected() throws Exception {
        String response = basicAuthClient.accessUnprotectedServlet("/staticAnnotation/StaticAnnotationPure5");
        assertTrue("Verification of programmatic APIs failed",
                   basicAuthClient.verifyUnauthenticatedResponse(response));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The servlet annotations specify that Manager is required for POST.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn5PostWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/staticAnnotation/StaticAnnotationPure5";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(testUrl, managerUser, managerPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The servlet annotations specify that Manager is required for POST.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) and password is passed in to the servlet for the doCUSTOM call.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn5PostFailWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/staticAnnotation/StaticAnnotationPure5";
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
     * <LI> and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testPureAnn5CustomWithEmployee() throws Exception {
        String methodName = "testPureAnn5CustomWithEmployee";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/staticAnnotation/StaticAnnotationPure5";
        String rs = testHelper.processDoCustom(testUrl, true, port, employeeUser, employeePassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        testHelper.verifyProgrammaticAPIValues(employeeUser, rs, authTypeBasic);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>The id and pw are base 64 encoded
     * <LI>The servlet requires the Employee role.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A Manager userId (user2) and password is passed in to the servlet for the doCUSTOM call
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn5CustomFailWithManager() throws Exception {
        String methodName = "testPureAnn5CustomFailWithManager";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/staticAnnotation/StaticAnnotationPure5";
        String rs = testHelper.processDoCustom(testUrl, true, port, managerUser, managerPassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        assertTrue("Expecting 403 response code", rs.contains("HTTP/1.0 403"));
    }

    /**
     *
     * StaticAnnotationPure6 - GET allows all roles, POST requires Manager, CUSTOM requires Employee and SSL
     * - includes multiple URL patterns
     *
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations specify that all authenticated users are allowed for GET.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn6GetWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure6";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, managerUser, managerPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET and an alternate URL.
     * <LI> The servlet annotations specify that all authenticated users are allowed for GET.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permits access to the protected servlet.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testPureAnn6GetWithEmployeeAltURL() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/staticAnnotation/StaticAnnotationPure6";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The servlet annotations specify that Manager is required for POST.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn6PostWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure6";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(testUrl, managerUser, managerPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The servlet annotations specify that Manager is required for POST.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Employee userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn6PostFailWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure6";
        testHelper.accessPostProtectedServletWithInvalidCredentials(testUrl, employeeUser, employeePassword, null, server);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL with SSL.
     * <LI>The id and pw are base 64 encoded
     * <LI>The servlet requires the Employee role and SSL.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet for the doCUSTOM call
     * <LI> and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testPureAnn6CustomWithEmployeeSSL() throws Exception {
        String methodName = "testPureAnn6CustomWithEmployeeSSL";
        int port = server.getHttpDefaultSecurePort();
        String testUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure6";
        String rs = testHelper.processDoCustom(testUrl, true, port, employeeUser, employeePassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        testHelper.verifyProgrammaticAPIValues(employeeUser, rs, authTypeBasic);
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
    public void testPureAnn6CustomFailWithManagerSSL() throws Exception {
        String methodName = "testPureAnn6CustomFailWithManagerSSL";
        int port = server.getHttpDefaultSecurePort();
        String testUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure6";
        String rs = testHelper.processDoCustom(testUrl, true, port, managerUser, managerPassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        assertTrue("Expecting 403 response code", rs.contains("HTTP/1.0 403"));
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
     * <LI> The request is redirected to https.
     * </OL>
     */
    @Test
    public void testPureAnn6CustomSSLRedirect() throws Exception {
        String methodName = "testPureAnn6CustomSSLRedirect";

        // First access without SSL
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure6";
        String sslTestUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure6";
        String rs = testHelper.processDoCustom(testUrl, true, port, employeeUser, employeePassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        // validate the request was redirected to https URL
        assertTrue("Failed to redirect to https site.", rs.contains("Location: " + sslTestUrl));

        //access https URL
        int securePort = server.getHttpDefaultSecurePort();
        rs = testHelper.processDoCustom(sslTestUrl, true, securePort, employeeUser, employeePassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        testHelper.verifyProgrammaticAPIValues(employeeUser, rs, authTypeBasic);
    }

    /**
     *
     * StaticAnnotationWebXML1 -
     * Web.xml - security constraint with exact match of url and no methods specified; therefore all methods are unprotected
     * Servlet - All methods are denied access
     * RESULTS - No security on any methods
     *
     */

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
    public void testWebXMLAnn1CustomUnprotected() throws Exception {
        String methodName = "testWebXMLAnn1CustomUnprotected";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_WEBXML_CONTEXT_ROOT + "/StaticAnnotationWebXML1";
        String rs = testHelper.processDoCustom(testUrl, false, port, null, null, server);
        Log.info(thisClass, methodName, "response: " + rs);
        assertTrue("getRemoteUser is not null.", rs.contains("getRemoteUser: null"));
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using POST method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testWebXMLAnn1PostUnprotected() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_WEBXML_CONTEXT_ROOT + "/StaticAnnotationWebXML1";
        testHelper.accessPostUnprotectedServlet(testUrl);
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testWebXMLAnn1GetUnprotected() throws Exception {
        String response = webXMLBasicAuthClient.accessUnprotectedServlet("/StaticAnnotationWebXML1");
        assertTrue("Verification of programmatic APIs failed",
                   webXMLBasicAuthClient.verifyUnauthenticatedResponse(response));
    }

    /**
     *
     * StaticAnnotationWebXML2 -
     * Web.xml - security constraint with exact match of url with POST but no roles
     * Servlet - All methods are denied access
     * RESULTS - No security on any methods
     *
     */

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
    public void testWebXMLAnn2CustomUnprotected() throws Exception {
        String methodName = "testWebXMLAnn2CustomUnprotected";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_WEBXML_CONTEXT_ROOT + "/StaticAnnotationWebXML2";
        String rs = testHelper.processDoCustom(testUrl, false, port, null, null, server);
        Log.info(thisClass, methodName, "response: " + rs);
        assertTrue("getRemoteUser is not null.", rs.contains("getRemoteUser: null"));
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using POST method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testWebXMLAnn2PostUnprotected() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_WEBXML_CONTEXT_ROOT + "/StaticAnnotationWebXML2";
        testHelper.accessPostUnprotectedServlet(testUrl);
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testWebXMLAnn2GetUnprotected() throws Exception {
        String response = webXMLBasicAuthClient.accessUnprotectedServlet("/StaticAnnotationWebXML2");
        assertTrue("Verification of programmatic APIs failed",
                   webXMLBasicAuthClient.verifyUnauthenticatedResponse(response));
    }

    /**
     *
     * StaticAnnotationMixed1 -
     * Web.xml - security constraint with exact match of url for all authenticated, POST is omitted
     * Servlet - Roles allowed is all authenticated
     * RESULTS - GET and custom have All Authenticated, POST is no security
     *
     */

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>The id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user2) and password is passed in to the servlet for the doCUSTOM call
     * <LI> and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testMixedAnn1CustomWithManager() throws Exception {
        String methodName = "testMixedAnn1CustomWithManager";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT + "/StaticAnnotationMixed1";
        String rs = testHelper.processDoCustom(testUrl, true, port, managerUser, managerPassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        testHelper.verifyProgrammaticAPIValues(managerUser, rs, authTypeBasic);
    }

    /**
     *
     * StaticAnnotationMixed2/a -
     * Web.xml - is unprotected for StaticAnnotationMixed2/a
     * Servlet - Roles allowed are all authenticated, POST is deny all
     * RESULTS - GET and custom are protected with all authenticated role, POST is unprotected
     *
     */

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
    public void testMixedAnn2aCustomUnprotected() throws Exception {
        String methodName = "testMixedAnn2aCustomUnprotected";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT + "/StaticAnnotationMixed2/a";
        String rs = testHelper.processDoCustom(testUrl, false, port, null, null, server);
        Log.info(thisClass, methodName, "response: " + rs);
        assertTrue("getRemoteUser is not null.", rs.contains("getRemoteUser: null"));
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using POST method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testMixedAnn2aPostUnprotected() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT + "/StaticAnnotationMixed2/a";
        testHelper.accessPostUnprotectedServlet(testUrl);
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using GET method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testMixedAnn2aGetUnprotected() throws Exception {
        String response = mixedBasicAuthClient.accessUnprotectedServlet("/StaticAnnotationMixed2/a");
        assertTrue("Verification of programmatic APIs failed",
                   basicAuthClient.verifyUnauthenticatedResponse(response));
    }

    /**
     *
     * StaticAnnotationMixed2/b -
     * Web.xml - does not contain constraints for exact url information
     * Servlet - Roles allowed are all authenticated, POST is deny all
     * RESULTS - GET and custom are protected with all authenticated role, POST is deny all
     *
     */

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>The id and pw are base 64 encoded
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet for the doCUSTOM call
     * <LI> and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testMixedAnn2bCustomWithEmployee() throws Exception {
        String methodName = "testMixedAnn2bCustomWithEmployee";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT + "/StaticAnnotationMixed2/b";
        String rs = testHelper.processDoCustom(testUrl, true, port, employeeUser, employeePassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        testHelper.verifyProgrammaticAPIValues(employeeUser, rs, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using the POST method.
     * <LI> The servlet annotation specifies that the POST method is denied access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testMixedAnn2bPostDeniedAccess() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT + "/StaticAnnotationMixed2/b";
        testHelper.accessPostProtectedServletWithInvalidCredentials(testUrl, employeeUser, employeePassword, null, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations specify that all authenticated users are allowed for GET.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testMixedAnn2bGetWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT + "/StaticAnnotationMixed2/b";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     *
     * StaticAnnotationMixedFragment1 -
     * Web.xml - no security constraints
     * Web-fragment.xml - security constraint with exact match of url for all authenticated, POST is unprotected
     * Servlet - Roles allowed is all authenticated
     * RESULTS - GET and custom have All Authenticated, POST is unprotected
     *
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations and web.xml specify that all authenticated users are allowed for GET.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testMixedAnnFragment1GetWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT + "/StaticAnnotationMixedFragment1";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using POST method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testMixedAnnFragment1PostUnprotected() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT + "/StaticAnnotationMixedFragment1";
        testHelper.accessPostUnprotectedServlet(testUrl);
    }

    /**
     *
     * StaticAnnotationMixedFragment2 -
     * Web.xml - security constraint with exact match of url for all authenticated, POST is unprotected
     * Web-fragment.xml - no security constraints
     * Servlet - Roles allowed is all authenticated
     * RESULTS - GET and custom have All Authenticated, POST is unprotected
     *
     */

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations and web.xml specify that all authenticated users are allowed for GET.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Employee userId (user1) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testMixedAnnFragment2GetWithEmployee() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT + "/StaticAnnotationMixedFragment2";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <LI> Attempt to access an unprotected servlet using POST method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * </OL>
     */
    @Test
    public void testMixedAnnFragment2PostUnprotected() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_MIXED_CONTEXT_ROOT + "/StaticAnnotationMixedFragment2";
        testHelper.accessPostUnprotectedServlet(testUrl);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Web.xml - does not define MetadataCompleteTrueWebXML1, so URL does not exist
     * <LI> Servlet - All methods are denied access
     * <LI> RESULTS - Because metadata-complete=true in web.xml, static annotations are ignored
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access a URL that does not exist in web.xml. Expect 404
     * </OL>
     */
    @Test
    public void testMetadataCompleteTrueWebXML1GetFileNotFound() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + METADATA_COMPLETE_TRUE_WEBXML_CONTEXT_ROOT + "/MetadataCompleteTrueWebXML1";
        metadataWebXMLBasicAuthClient.access(testUrl, 404);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Web.xml - Unprotected for GET method for /MetadataCompleteTrueWebXMLFragment2
     * <LI> web-fragment.xml - RunAs Manager for /MetadataCompleteTrueWebXMLFragment2
     * <LI> Servlet - All methods require Manager access
     * <LI> RESULTS - Because metadata-complete=true in web.xml, static annotations are ignored
     * <LI> Also, web-fragment.xml constraints are ignored, so RunAs manager in web-fragment.xml is ignored
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> GET is unprotected with no RunAs user
     * </OL>
     */
    @Test
    public void testMetadataCompleteTrueWebXML2GetUnprotected() throws Exception {
        String response = metadataWebXMLBasicAuthClient.accessUnprotectedServlet("/MetadataCompleteTrueWebXMLFragment2");
        assertTrue("Verification of programmatic APIs failed",
                   metadataWebXMLBasicAuthClient.verifyUnauthenticatedResponse(response));
        assertFalse("RunAs user should NOT be found: " + runAsUserManager, response.matches(runAsUserManagerCheck));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Web.xml - does not define MetadataCompleteTrueWebFragment1
     * <LI> web-fragment.xml - does not define MetadataCompleteTrueWebFragment1, but metadata-complete=true is set
     * <LI> Servlet - All methods are denied access
     * <LI> RESULTS - Because metadata-complete=true in web-fragment.xml, static annotations are ignored
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access a URL that does not exist in web.xml. Expect 404
     * </OL>
     */
    @Test
    public void testMetadataCompleteTrueWebFragment1GetFileNotFound() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + METADATA_COMPLETE_TRUE_WEBFRAGMENT_CONTEXT_ROOT
                         + "/MetadataCompleteTrueWebFragment1";
        metadataWebXMLBasicAuthClient.access(testUrl, 404);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Web.xml - does not define MetadataCompleteTrueWebFragment2
     * <LI> web-fragment.xml - Servlet name and URL defined for MetadataCompleteTrueWebFragment2, but no security constraint
     * <LI> Servlet - All methods are denied access
     * <LI> RESULTS - Because metadata-complete=true in web-fragment.xml, static annotations should be ignored
     * <LI> However, since web fragment jar does NOT contain MetadataCompleteTrueWebFragment2 class, static annotations are followed
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> GET is denied access
     * </OL>
     */
    @Test
    public void testMetadataCompleteTrueWebFragment2GetDenied() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + METADATA_COMPLETE_TRUE_WEBFRAGMENT_CONTEXT_ROOT
                         + "/MetadataCompleteTrueWebFragment2";
        metadataWebFragmentBasicAuthClient.access(testUrl, 403);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations specify that Manager is required for GET.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn7GetWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure7";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, managerUser, managerPassword, null, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations specify that Manager is required for GET.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Employee userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn7GetFailWithEmployee() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   basicAuthClient.accessProtectedServletWithInvalidCredentials("/StaticAnnotationPure7", employeeUser, employeePassword));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The servlet annotations specify that Manager is required for POST.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn7PostWithManager() throws Exception {
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure7";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(secureUrl, managerUser, managerPassword, secureUrl, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST and SSL.
     * <LI> The servlet annotations specify that Manager is required for POST.
     * <LI> The servlet annotations require SSL.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> An Employee userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn7PostFailWithEmployee() throws Exception {
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure7";
        testHelper.accessPostProtectedServletWithInvalidCredentials(secureUrl, employeeUser, employeePassword, secureUrl, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST without SSL.
     * <LI> The servlet annotations specify that Manager is required for POST.
     * <LI> The servlet annotations require SSL. Without SSL, servlet redirects to SSL with GET method, which requires Manager.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A e userId (user2) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn7PostFailWithoutSSL() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure7";
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure7";
        testHelper.testSSLRedirectPost(testUrl, employeeUser, employeePassword, 403, secureUrl, server);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>Access is granted for the CUSTOM method with Manager since it is set by HttpConstraint.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn7CustomAccess() throws Exception {
        String methodName = "testPureAnn7CustomAccess";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure7";
        String rs = testHelper.processDoCustom(testUrl, true, port, managerUser, managerPassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        testHelper.verifyProgrammaticAPIValues(managerUser, rs, authTypeBasic);

    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations specify that Employee is required for GET.
     * <LI> Login with a valid Employee userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user1) permits access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn8GetWithManager() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure8";
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(testUrl, employeeUser, employeePassword, null, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using GET.
     * <LI> The servlet annotations specify that Employee is required for GET.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A Manager userId (user2) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn8GetFailWithManager() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   basicAuthClient.accessProtectedServletWithInvalidCredentials("/StaticAnnotationPure8", managerUser, managerPassword));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST.
     * <LI> The servlet annotations specify that Manager is required for POST.
     * <LI> Login with a valid Manager userId and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn8PostWithManager() throws Exception {
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure8";
        String response = testHelper.accessPostProtectedServletWithAuthorizedCredentials(secureUrl, managerUser, managerPassword, secureUrl, server);
        testHelper.verifyProgrammaticAPIValues(managerUser, response, authTypeBasic);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST and SSL.
     * <LI> The servlet annotations specify that Manager is required for POST.
     * <LI> The servlet annotations require SSL.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> An Employee userId (user1) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn8PostFailWithEmployee() throws Exception {
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure8";
        testHelper.accessPostProtectedServletWithInvalidCredentials(secureUrl, employeeUser, employeePassword, secureUrl, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet using POST without SSL.
     * <LI> The servlet annotations specify that Manager is required for POST.
     * <LI> The servlet annotations require SSL. Without SSL, servlet redirects to SSL with GET method, which requires Manager.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A e userId (user2) and password is passed in to the servlet.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testPureAnn8PostFailWithoutSSL() throws Exception {
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure8";
        String secureUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure8";
        testHelper.testSSLRedirectPost(testUrl, managerUser, managerPassword, 403, secureUrl, server);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet using a java call to make a doCustom call to the URL.
     * <LI>Access is granted for the CUSTOM method with Manager since it is set by HttpConstraint.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid Manager userId (user2) permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testPureAnn8CustomAccess() throws Exception {
        String methodName = "testPureAnn7CustomAccess";
        int port = server.getHttpDefaultPort();
        String testUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + STATIC_ANNOTATIONS_PURE_CONTEXT_ROOT + "/StaticAnnotationPure8";
        String rs = testHelper.processDoCustom(testUrl, true, port, managerUser, managerPassword, server);
        Log.info(thisClass, methodName, "response: " + rs);
        testHelper.verifyProgrammaticAPIValues(managerUser, rs, authTypeBasic);

    }
}
