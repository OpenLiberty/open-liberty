/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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

package com.ibm.ws.security.authentication.tai.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClientImpl;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class FATTest {

    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.authentication.tai.fat");
    private final static Class<?> thisClass = FATTest.class;

    // Keys to help readability of the test
    protected final boolean IS_MANAGER_ROLE = true;
    protected final boolean NOT_MANAGER_ROLE = false;
    protected final boolean IS_EMPLOYEE_ROLE = true;
    protected final boolean NOT_EMPLOYEE_ROLE = false;

    private final static String employeeUser = "user1";
    private final static String employeePassword = "user1pwd";
    private final static String managerUser = "user2";
    private final static String managerPassword = "user2pwd";
    private final static String customUserTAICheck = "TAI scenario: user";
    private final static String customSubjectTAICheck = "TAI scenario: subject";
    private final static String customHashtableTAICheck = "TAI scenario: hashtable";
    private final static String continueTAICheck = "TAI scenario: doNothing";

    private final static String hashtableUniqueIdCheck = "com.ibm.wsspi.security.cred.uniqueId="
                                                         + managerUser;
    private final static String hashtableSecurityNameCheck = "com.ibm.wsspi.security.cred.securityName="
                                                             + managerUser;
    private final static String managerUserFormat = "user:BasicRealm/user2";
    private final static String unqueIdCheck = "com.ibm.wsspi.security.cred.uniqueId=";

    private final static String DEFAULT_CONFIG_FILE = "tai.server.xml.orig";
    private final static String TAI_DISABLE_LTPA_COOKIE_TRUE_CONFIG_FILE = "taiDisableLtpaCookie_true.xml";
    private final static String TAI_DISABLE_LTPA_COOKIE_FALSE_CONFIG_FILE = "taiDisableLtpaCookie_false.xml";
    private final static String TAI_DISABLE_LTPA_COOKIE_FALSE_SHARED_LIB_TRUE_CONFIG_FILE = "taiDisableLtpaCookie_false_sharedLib_true.xml";
    private final static String TAI_DISABLE_LTPA_COOKIE_TRUE_FEATURE2_FALSE_CONFIG_FILE = "taiDisableLtpaCookie_true_feature2_false.xml";
    private final static String TAI_DISABLE_LTPA_COOKIE_FALSE_FEATURE_TRUE_CONFIG_FILE = "taiDisableLtpaCookie_false_feature_true.xml";
    private final static String TAI_LIB_DISABLE_LTPA_COOKIE_FALSE_FEATURE_TRUE_CONFIG_FILE = "taiDisableLtpaCookie_false_lib_and_feature_true.xml";
    private final static String TAI_UNAUTHORIZED_USER_CONFIG_FILE = "taiUnauthorizedUser.xml";
    private final static String TAI_FEATURE_UNAUTHORIZED_USER_CONFIG_FILE = "taiFeatureUnauthorizedUser.xml";
    private final static String TAI_UNAUTHENTICATED_USER_CONFIG_FILE = "taiUnauthenticatedUser.xml";
    private final static String TAI_FEATURE_UNAUTHENTICATED_USER_CONFIG_FILE = "taiFeatureUnauthenticatedUser.xml";
    private final static String TAI_SUBJECT_CONFIG_FILE = "taiSubject.xml";
    private final static String TAI_HASHTABLE_CONFIG_FILE = "taiHashtable.xml";
    private final static String TAI_CONTINUE_FALSE_CONFIG_FILE = "taiContinueFalse.xml";
    private final static String TAI_CONTINUE_TRUE_CONFIG_FILE = "taiContinueTrue.xml";
    private final static String TAI_FEATURE_CONTINUE_TRUE_CONFIG_FILE = "taiFeatureContinueTrue.xml";
    private final static String TAI_FALLBACK_TRUE_UNAUTHORIZED_CONFIG_FILE = "taiFallbackTrueUnauthorized.xml";
    private final static String TAI_FEATURE_FALLBACK_TRUE_UNAUTHORIZED_CONFIG_FILE = "taiFeatureFallbackTrueUnauthorized.xml";
    private final static String TAI_FALLBACK_TRUE_UNAUTHENTICATED_CONFIG_FILE = "taiFallbackTrueUnauthenticated.xml";
    private final static String TAI_FEATURE_FALLBACK_TRUE_UNAUTHENTICATED_CONFIG_FILE = "taiFeatureFallbackTrueUnauthenticated.xml";
    private final static String TAI_MULTIPLE_CONFIG_FILE = "taiMultiple.xml";
    private final static String TAI_FEATURE_MULTIPLE_CONFIG_FILE = "taiFeatureMultiple.xml";
    private final static String TAI_FEATURE_AND_TAI_MULTIPLE_CONFIG_FILE = "taiFeatureAndTaiMultiple.xml";
    private final static String TAI_NONZERO_CONFIG_FILE = "taiNonZero.xml";
    private final static String TAI_RETURN_FALSE_CONFIG_FILE = "taiReturnFalse.xml";
    private final static String TAI_403_CONFIG_FILE = "tai403.xml";
    private final static String TAI_FEATURE_403_CONFIG_FILE = "taiFeature403.xml";
    private final static String TAI_500_CONFIG_FILE = "tai500.xml";
    private final static String TAI_FEATURE_500_CONFIG_FILE = "taiFeature500.xml";
    private final static String TAI_NULL_CONFIG_FILE = "taiNull.xml";
    private final static String TAI_NONE_CONFIG_FILE = "taiNone.xml";
    private final static String TAI_LIBRARY_CONFIG_FILE = "taiLibrary.xml";
    private final static String TAI_JAAS_LOGIN_MODULE_PUBLIC_CREDENTIALS_CONFIG_FILE = "taiJAASLoginModulePublicCredentials.xml";
    private final static String TAI_JAAS_LOGIN_MODULE_SHARED_STATE_CONFIG_FILE = "taiJAASLoginModuleSharedState.xml";
    private final static String TAI_INVOKEFORUNPROTECTEDURI_TRUE_PROCESS_CONFIG_FILE = "taiInvokeForUnprotectedURITrueProcess.xml";
    private final static String TAI_INVOKEFORUNPROTECTEDURI_TRUE_NOT_PROCESS_CONFIG_FILE = "taiInvokeForUnprotectedURITrueNotProcess.xml";
    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    private static final BasicAuthClient baClient = new BasicAuthClient(myServer);

    @BeforeClass
    public static void setUp() throws Exception {
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/customTaiSample-1.0.mf");
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/customTaiSample2-1.0.mf");
        myServer.copyFileToLibertyInstallRoot("lib", "internalfeatures/com.ibm.ws.security.authentication.tai.feature.sample_1.0.jar");
        myServer.copyFileToLibertyInstallRoot("lib", "internalfeatures/com.ibm.ws.security.authentication.tai.feature.sample2_1.0.jar");

        myServer.addInstalledAppForValidation("basicauth");
        myServer.startServer(true);
        verifyServerStarted();
    }

    @After
    public void resetConnection() {
        baClient.resetClientState();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Reset server to use original configuration
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        myServer.stopServer();
        myServer.deleteFileFromLibertyInstallRoot("lib/features/securitylibertyinternals-1.0.mf");
        myServer.deleteFileFromLibertyInstallRoot("lib/features/customTaiSample-1.0.mf");
        myServer.deleteFileFromLibertyInstallRoot("lib/features/customTaiSample2-1.0.mf");
        myServer.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.security.authentication.tai.feature.sample_1.0.jar");
        myServer.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.security.authentication.tai.feature.sample2_1.0.jar");
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with a
     * valid user
     * <LI>Also, invokeForUnprotectedURI="false"
     * invokeForFormLogin="false" allowAppAuthMethodFallback="false" are
     * <LI>default values and are not specifed in server.xml. These should not
     * be required in server.xml
     * <LI>Should return user2 from TAI
     * </OL>
     */
    @Test
    public void testTAI_ValidUser() throws Exception {
        String methodName = "testTAI_ValidUser";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);

        // Even with a protected servlet, authentication goes through TAI
        // successfully, so we are never prompted
        // That's why we call accessUnprotectedServlet method
        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * Tests:
     * - server config specifies disableLtpaCookie(dlc) at TrustAssociation level
     * disableLtpaCookie not specified at customLibrary interceptor level
     * Server config:
     * - trustAssociation id="TAICfg_dlc_true" disableLtpaCookie="true"
     * Expects:
     * - ltpaCookie not found in the response
     */
    @Test
    public void testTAI_disableLtpaCookie_true() throws Exception {
        String methodName = "testTAI_disableLtpaCookie_true";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_DISABLE_LTPA_COOKIE_TRUE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertSSOCookieIsNotPresent(response, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);

        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * Tests:
     * - server config specifies disableLtpaCookie at TrustAssociation level
     * disableLtpaCookie not specified at customLibrary interceptor level
     * Server config:
     * - trustAssociation id="TAICfg_dlc_false" disableLtpaCookie="false"
     * Expects:
     * - ltpaCookie is found in the response
     */
    @Test
    public void testTAI_disableLtpaCookie_false() throws Exception {
        String methodName = "testTAI_disableLtpaCookie_false";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_DISABLE_LTPA_COOKIE_FALSE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertSSOCookieIsPresent(response, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);

        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * Tests:
     * - server config specifies TrustAssociation level and shared library interceptor level
     * - The disableLtpaCookie property in the shared library interceptor should be ignored
     * Server Config:
     * - trustAssociation id="TAICfg_dlc_false" disableLtpaCookie="false"
     * - interceptors id="TAIInt_dlc_true" disableLtpaCookie="true"
     * Expects:
     * - ltpaCookie is found in the response
     */
    @Test
    public void testTAI_disableLtpaCookie_false_sharedLib_true() throws Exception {
        String methodName = "testTAI_disableLtpaCookie_false_sharedLib_true";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_DISABLE_LTPA_COOKIE_FALSE_SHARED_LIB_TRUE_CONFIG_FILE);

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertSSOCookieIsPresent(response, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);

        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * Tests:
     * - interceptor overrides TrustAssociation setting of disableLtpaCookie
     * Server Config:
     * - trustAssociation id="TAICfg_disableLtpaCookie_true_feature_false" disableLtpaCookie="true"
     * User feature interceptor config:
     * - properties:="...,disableLtpaCookie:Boolean=false"
     * Expects:
     * - ltpaCookie is found in the response
     */
    @Test
    public void testTAI_disableLtpaCookie_true_feature_false() throws Exception {
        String methodName = "testTAI_disableLtpaCookie_true_feature_false";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_DISABLE_LTPA_COOKIE_TRUE_FEATURE2_FALSE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertSSOCookieIsPresent(response, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);

        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * Tests:
     * - interceptor overrides TrustAssociation setting of disableLtpaCookie
     * Server Config:
     * - trustAssociation id="TAICfg_disableLtpaCookie_true_feature_true" disableLtpaCookie="false"
     * User feature interceptor config:
     * - properties:="...,disableLtpaCookie:Boolean=true"
     * Expects:
     * - ltpaCookie is not found in the response
     */
    @Test
    public void testTAI_disableLtpaCookie_false_feature_true() throws Exception {
        String methodName = "testTAI_disableLtpaCookie_false_feature_true";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_DISABLE_LTPA_COOKIE_FALSE_FEATURE_TRUE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertSSOCookieIsNotPresent(response, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);

        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * Tests:
     * - Shared Library and userFeature TAI combined
     * Server Config:
     * - trustAssociation id="TAICfg_disableLtpaCookie_true_feature_true" disableLtpaCookie="false"
     * User feature interceptor config:
     * - properties:="...,disableLtpaCookie:Boolean=true"
     * Expects:
     * - ltpaCookie is found in the response
     */
    @Test
    public void testTAI_feature_and_lib_disableLtpaCookie_false() throws Exception {
        String methodName = "testTAI_feature_and_lib_disableLtpaCookie_false";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_LIB_DISABLE_LTPA_COOKIE_FALSE_FEATURE_TRUE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertSSOCookieIsPresent(response, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);

        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with an
     * unauthorized user
     * <LI>Since allowAppAuthMethodFallback="false", should return 403
     * </OL>
     */
    @Test
    public void testTAI_UnauthorizedUser() throws Exception {
        String methodName = "testTAI_UnauthorizedUser";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_UNAUTHORIZED_USER_CONFIG_FILE);

        baClient.accessProtectedServletWithUnauthorizedCredentials(
                                                                   BasicAuthClient.PROTECTED_SIMPLE, employeeUser,
                                                                   employeePassword);
        verifyTAIInitLoad();

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with an
     * unauthorized user
     * <LI>Since allowAppAuthMethodFallback="false", should return 403
     * </OL>
     */
    @Test
    public void testTAI_Feature_UnauthorizedUser() throws Exception {
        String methodName = "testTAI_Feature_UnauthorizedUser";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_FEATURE_UNAUTHORIZED_USER_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        baClient.accessProtectedServletWithUnauthorizedCredentials(
                                                                   BasicAuthClient.PROTECTED_SIMPLE, employeeUser,
                                                                   employeePassword);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with an
     * unauthenticated user
     * <LI>Since allowAppAuthMethodFallback="false", TAI returns
     * AuthenticationException
     * </OL>
     */
    // Because exception is thrown in TAI, FFDC is generated so user knows why
    // TAI authentication failed
    @ExpectedFFDC("com.ibm.ws.security.registry.EntryNotFoundException")
    @Test
    public void testTAI_UnauthenticatedUser() throws Exception {
        String methodName = "testTAI_UnauthenticatedUser";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_UNAUTHENTICATED_USER_CONFIG_FILE);

        String url = "http://" + myServer.getHostname() + ":"
                     + myServer.getHttpDefaultPort() + "/basicauth"
                     + BasicAuthClient.PROTECTED_SIMPLE;

        baClient.access(url, 401);

        verifyTAIInitLoad();

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    // Because exception is thrown in TAI, FFDC is generated so user knows why
    // TAI authentication failed
    @ExpectedFFDC("com.ibm.ws.security.registry.EntryNotFoundException")
    @Test
    public void testTAI_Feature_UnauthenticatedUser() throws Exception {
        String methodName = "testTAI_Feature_UnauthenticatedUser";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_FEATURE_UNAUTHENTICATED_USER_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String url = "http://" + myServer.getHostname() + ":"
                     + myServer.getHttpDefaultPort() + "/basicauth"
                     + BasicAuthClient.PROTECTED_SIMPLE;

        baClient.access(url, 401);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Access UNPROTECTED servlet with TAI negotiateValidateandEstablishTrust with
     * user and customSubject. Also, set invokeForUnprotectedURI="true" in server.xml to
     * allow TAI to be invoked for an unprotected URI
     * </OL>
     */
    @Test
    public void testTAI_UserAndSubject_invokeForUnprotectedURI_true() throws Exception {
        String methodName = "testTAI_UserAndSubject_invokeForUnprotectedURI_true";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_SUBJECT_CONFIG_FILE);

        // Authentication goes through TAI when accessing unprotected servlet
        String response = baClient.accessUnprotectedServlet(BasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        verifyTAIInitLoad();

        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertTrue(customSubjectTAICheck + " not found", response.contains(customSubjectTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Access UNPROTECTED servlet with TAI negotiateValidateandEstablishTrust with
     * user and customSubject. Also, set invokeForUnprotectedURI="false" in server.xml to
     * allow TAI to be invoked for an unprotected URI
     * </OL>
     */
    @Test
    public void testTAI_UserAndSubject_invokeForUnprotectedURI_false() throws Exception {
        String methodName = "testTAI_UserAndSubject_invokeForUnprotectedURI_false";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);

        // Authentication should not go through TAI when accessing unprotected servlet
        String response = baClient.accessUnprotectedServlet(BasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        verifyTAIInitLoad();

        assertFalse(customSubjectTAICheck + " not found", response.contains(customSubjectTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with an
     * unauthenticated user
     * <LI>Since allowAppAuthMethodFallback="false", TAI should not be invoked
     * </OL>
     */
    @Test
    public void testTAI_hadAuthenticationData_with_unprotectedURL_false() throws Exception {
        String methodName = "testTAI_hadAuthenticationData_with_unprotectedURL_false";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        verifyTAIInitLoad();

        // Even with a protected servlet, authentication goes through TAI
        // successfully, so we are never prompted
        // That's why we call accessUnprotectedServlet method
        response = baClient.accessUnprotectedServlet(BasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        assertFalse(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with
     * user and hashtable added to subject
     * </OL>
     */
    @Test
    public void testTAI_UserAndHashtableSubject() throws Exception {
        String methodName = "testTAI_UserAndHashtableSubject";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_HASHTABLE_CONFIG_FILE);

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        verifyTAIInitLoad();

        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertTrue(customHashtableTAICheck + " not found", response.contains(customHashtableTAICheck));
        assertTrue(hashtableUniqueIdCheck + " not found", response.contains(hashtableUniqueIdCheck));
        assertTrue(hashtableSecurityNameCheck + " not found", response.contains(hashtableSecurityNameCheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust that
     * returns TAIResult.create(HttpServletResponse.SC_CONTINUE)
     * <LI>Also, invokeForUnprotectedURI="false"
     * invokeForFormLogin="false" allowAppAuthMethodFallback="false" are
     * <LI>default values and are not specified in server.xml. These should not
     * be required in server.xml
     * <LI>Since allowAppAuthMethodFallback="false", should return 100 when negotiateValidateandEstablishTrust
     * <LI>does not return HttpServletResponse.SC_OK
     * </OL>
     */
    // The default http client can not handle the CONTINUE (100) code
    @Ignore
    @Test
    public void testTAI_ContinueFalse() throws Exception {
        String methodName = "testTAI_ContinueFalse";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_CONTINUE_FALSE_CONFIG_FILE);

        String url = "http://" + myServer.getHostname() + ":"
                     + myServer.getHttpDefaultPort() + "/basicauth/"
                     + BasicAuthClient.PROTECTED_SIMPLE;
        baClient.access(url, 100);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust that
     * returns TAIResult.create(HttpServletResponse.SC_CONTINUE)
     * <LI>and with allowAppAuthMethodFallback="true"
     * <LI>Should fallback to basic auth, so prompt and login with valid user
     * </OL>
     */
    @Test
    public void testTAI_ContinueTrue() throws Exception {
        String methodName = "testTAI_ContinueTrue";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_CONTINUE_TRUE_CONFIG_FILE);

        String response = baClient.accessProtectedServletWithAuthorizedCredentials(
                                                                                   BasicAuthClient.PROTECTED_SIMPLE, employeeUser,
                                                                                   employeePassword);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE,
                                                                                       NOT_MANAGER_ROLE));;

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust that
     * returns TAIResult.create(HttpServletResponse.SC_CONTINUE)
     * <LI>and with allowAppAuthMethodFallback="true"
     * <LI>Should fallback to basic auth, so prompt and login with valid user
     * </OL>
     */
    @Test
    public void testTAI_Feature_ContinueTrue() throws Exception {
        String methodName = "testTAI_Feature_ContinueTrue";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_FEATURE_CONTINUE_TRUE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String response = baClient.accessProtectedServletWithAuthorizedCredentials(
                                                                                   BasicAuthClient.PROTECTED_SIMPLE, employeeUser,
                                                                                   employeePassword);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE,
                                                                                       NOT_MANAGER_ROLE));;

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with an
     * unauthorized user
     * <LI>and with allowAppAuthMethodFallback="true"
     * <LI>TAI authenticates but fails with unauthorized user, should return 403
     * </OL>
     */
    @Test
    public void testTAI_FallbackTrueUnauthorized() throws Exception {
        String methodName = "testTAI_FallbackTrueUnauthorized";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer,
                               TAI_FALLBACK_TRUE_UNAUTHORIZED_CONFIG_FILE);

        baClient.accessPrecludedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        verifyTAIInitLoad();

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with an
     * unauthorized user
     * <LI>and with allowAppAuthMethodFallback="true"
     * <LI>TAI authenticates but fails with unauthorized user, should return 403
     * </OL>
     */
    @Test
    public void testTAI_Feature_FallbackTrueUnauthorized() throws Exception {
        String methodName = "testTAI_Feature_FallbackTrueUnauthorized";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_FEATURE_FALLBACK_TRUE_UNAUTHORIZED_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        baClient.accessPrecludedServlet(BasicAuthClient.PROTECTED_SIMPLE);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with an
     * unauthenticated user
     * <LI>and with allowAppAuthMethodFallback="true"
     * <LI>No fallback since we get AuthenticationException from TAI
     * </OL>
     */
    // Because exception is thrown in TAI, FFDC is generated so user knows why
    // TAI authentication failed
    @ExpectedFFDC("com.ibm.ws.security.registry.EntryNotFoundException")
    @Test
    public void testTAI_FallbackTrueUnauthenticated() throws Exception {
        String methodName = "testTAI_FallbackTrueUnauthenticated";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer,
                               TAI_FALLBACK_TRUE_UNAUTHENTICATED_CONFIG_FILE);

        String url = "http://" + myServer.getHostname() + ":"
                     + myServer.getHttpDefaultPort() + "/basicauth/"
                     + BasicAuthClient.PROTECTED_SIMPLE;
        baClient.access(url, 401);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with an
     * unauthenticated user
     * <LI>and with allowAppAuthMethodFallback="true"
     * <LI>No fallback since we get AuthenticationException from TAI
     * </OL>
     */
    // Because exception is thrown in TAI, FFDC is generated so user knows why
    // TAI authentication failed
    @ExpectedFFDC("com.ibm.ws.security.registry.EntryNotFoundException")
    @Test
    public void testTAI_Feature_FallbackTrueUnauthenticated() throws Exception {
        String methodName = "testTAI_Feature_FallbackTrueUnauthenticated";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_FEATURE_FALLBACK_TRUE_UNAUTHENTICATED_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String url = "http://" + myServer.getHostname() + ":"
                     + myServer.getHttpDefaultPort() + "/basicauth/"
                     + BasicAuthClient.PROTECTED_SIMPLE;
        baClient.access(url, 401);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with multiple TAIs in server.xml and with
     * allowAppAuthMethodFallback="true"
     * <LI>1st TAI has invokeBeforeSSO=true and
     * negotiateValidateandEstablishTrust that returns
     * TAIResult.create(HttpServletResponse.SC_CONTINUE)
     * <LI>2nd TAI has invokeAfterSSO=true and
     * negotiateValidateandEstablishTrust with a valid user and subject
     * <LI>1st TAI should continue, then 2nd TAI should return valid user and
     * subject
     * </OL>
     */
    @Test
    public void testTAI_MultipleTAI() throws Exception {
        String methodName = "testTAI_MultipleTAI";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_MULTIPLE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        verifyTAIInitLoad();
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertTrue(customSubjectTAICheck + " not found", response.contains(customSubjectTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with multiple TAIs in server.xml and with
     * allowAppAuthMethodFallback="true"
     * <LI>1st TAI has invokeBeforeSSO=true and
     * negotiateValidateandEstablishTrust that returns
     * TAIResult.create(HttpServletResponse.SC_CONTINUE)
     * <LI>2nd TAI has invokeAfterSSO=true and
     * negotiateValidateandEstablishTrust with a valid user and subject
     * <LI>1st TAI should continue, then 2nd TAI should return valid user and
     * subject
     * </OL>
     */
    @Test
    public void testTAI_Feature_And_TAI_Multiple() throws Exception {
        String methodName = "testTAI_Feature_And_TAI_Multiple";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_FEATURE_AND_TAI_MULTIPLE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertTrue(customSubjectTAICheck + " not found", response.contains(customSubjectTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with multiple TAIs in server.xml and with
     * allowAppAuthMethodFallback="true"
     * <LI>1st TAI has invokeBeforeSSO=true and
     * negotiateValidateandEstablishTrust that returns
     * TAIResult.create(HttpServletResponse.SC_CONTINUE)
     * <LI>2nd TAI has invokeAfterSSO=true and
     * negotiateValidateandEstablishTrust with a valid user and subject
     * <LI>1st TAI should continue, then 2nd TAI should return valid user and
     * subject
     * </OL>
     */
    @Test
    public void testTAI_Feature_MultipleTAI() throws Exception {
        String methodName = "testTAI_Feature_MultipleTAI";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_FEATURE_MULTIPLE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertTrue(customSubjectTAICheck + " not found", response.contains(customSubjectTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> Protected servlet with TAI initialize() method returns a non-zero return code
     * <LI> Check for this TAI initialize failure in the trace
     * </OL>
     */
    @Test
    public void testTAI_InitializeNonZero() throws Exception {
        String methodName = "testTAI_InitializeNonZero";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        // The non-zero initialize return code is expected to log CWWKS1203E
        myServer.addIgnoredErrors(Arrays.asList("CWWKS1203E"));

        setServerConfiguration(myServer, TAI_NONZERO_CONFIG_FILE);
        verifyTAIInitLoad();

        List<String> initializeString = myServer.findStringsInLogsAndTrace("CWWKS1203E.*1");
        Log.info(thisClass, methodName, "initializeString = " + initializeString);
        assertTrue("CWWKS1203E not found in trace for TAI initialize failure", initializeString.toString().contains("CWWKS1203E"));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> Protected servlet with TAI isTargetInterceptorFalse() method returns false
     * <LI> This TAI isTargetInterceptorFalse() failure will still fallback to basic auth prompt
     * </OL>
     */
    @Test
    public void testTAI_IsTargetInterceptorFalse() throws Exception {
        String methodName = "testTAI_IsTargetInterceptorFalse";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_RETURN_FALSE_CONFIG_FILE);

        String url = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() +
                     "/basicauth/" + BasicAuthClient.PROTECTED_SIMPLE;
        baClient.access(url, 401);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust()
     * method returns 403
     * <LI>This TAI failure causes servlet to return error 403 when negotiateValidateandEstablishTrust
     * <LI>does not return HttpServletResponse.SC_OK
     * </OL>
     */
    @Test
    public void testTAI_Negotiate403() throws Exception {
        String methodName = "testTAI_Negotiate403";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_403_CONFIG_FILE);

        String url = "http://" + myServer.getHostname() + ":"
                     + myServer.getHttpDefaultPort() + "/basicauth/"
                     + BasicAuthClient.PROTECTED_SIMPLE;
        baClient.access(url, 403);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust()
     * method returns 403
     * <LI>This TAI failure causes servlet to return error 403 when negotiateValidateandEstablishTrust
     * <LI>does not return HttpServletResponse.SC_OK
     * </OL>
     */
    @Test
    public void testTAI_Feature_Negotiate403() throws Exception {
        String methodName = "testTAI_Feature_Negotiate403";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_FEATURE_403_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        String url = "http://" + myServer.getHostname() + ":"
                     + myServer.getHttpDefaultPort() + "/basicauth/"
                     + BasicAuthClient.PROTECTED_SIMPLE;
        baClient.access(url, 403);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust()
     * method returns 500
     * <LI>This TAI failure causes servlet to return error 500 when negotiateValidateandEstablishTrust
     * <LI>does not return HttpServletResponse.SC_OK
     * </OL>
     */
    @Test
    public void testTAI_Negotiate500() throws Exception {
        String methodName = "testTAI_Negotiate500";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        // The 500 response is expected to log SRVE0315E / CWWKS9115E
        myServer.addIgnoredErrors(Arrays.asList("SRVE0315E", "CWWKS9115E"));

        setServerConfiguration(myServer, TAI_500_CONFIG_FILE);

        String url = "http://" + myServer.getHostname() + ":"
                     + myServer.getHttpDefaultPort() + "/basicauth/"
                     + BasicAuthClient.PROTECTED_SIMPLE;
        baClient.access(url, 500);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust()
     * method returns 500
     * <LI>This TAI failure causes servlet to return error 500 when negotiateValidateandEstablishTrust
     * <LI>does not return HttpServletResponse.SC_OK
     * </OL>
     */
    @Test
    public void testTAI_Feature_Negotiate500() throws Exception {
        String methodName = "testTAI_Feature_Negotiate500";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_FEATURE_500_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        // The 500 response is expected to log SRVE0315E / CWWKS9115E. Registered after the restart because
        // ignored errors are cleared whenever the server stops.
        myServer.addIgnoredErrors(Arrays.asList("SRVE0315E", "CWWKS9115E"));

        String url = "http://" + myServer.getHostname() + ":"
                     + myServer.getHttpDefaultPort() + "/basicauth/"
                     + BasicAuthClient.PROTECTED_SIMPLE;
        baClient.access(url, 500);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust()
     * method returns null
     * <LI>This TAI failure causes servlet to return error 403 when negotiateValidateandEstablishTrust
     * <LI>does not return HttpServletResponse.SC_OK
     * </OL>
     */
    @Test
    public void testTAI_NegotiateNull() throws Exception {
        String methodName = "testTAI_NegotiateNull";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_NULL_CONFIG_FILE);

        String url = "http://" + myServer.getHostname() + ":"
                     + myServer.getHttpDefaultPort() + "/basicauth/"
                     + BasicAuthClient.PROTECTED_SIMPLE;
        baClient.access(url, 403);

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Dynamic update of libraryRef to point to a new library
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with a
     * valid user
     * <LI>Should return user2 from TAI
     * </OL>
     */
    @Test
    public void testTAI_DynamicUpdateLibrary() throws Exception {
        String methodName = "testTAI_DynamicUpdateLibrary";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_LIBRARY_CONFIG_FILE);

        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                       IS_MANAGER_ROLE));
        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> Dynamic update test to add TAI config to server.xml while server is running, then remove the TAI
     * <LI> config and add TAI again, but with a different config
     * </OL>
     */
    @Test
    public void testTAI_DynamicUpdateAddRemoveAddTAI() throws Exception {
        String methodName = "testTAI_DynamicUpdateAddRemoveAddTAI";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        //Set config with no TAI, then restart server
        setServerConfiguration(myServer, TAI_NONE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        //Verify authentication does NOT go through TAI
        String response = baClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
        assertFalse(customUserTAICheck + " should not be found", response.contains(customUserTAICheck));

        //Add TAI dynamically to server.xml
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);

        //Verify authentication goes through TAI
        baClient.resetClientState();
        response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        //Remove TAI dynamically in server.xml
        setServerConfiguration(myServer, TAI_NONE_CONFIG_FILE);

        //Verify authentication does NOT go through TAI
        baClient.resetClientState();
        response = baClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
        assertFalse(customUserTAICheck + " should not be found", response.contains(customUserTAICheck));

        //Add TAI dynamically to server.xml with different config
        setServerConfiguration(myServer, TAI_HASHTABLE_CONFIG_FILE);

        //Verify authentication goes through TAI
        baClient.resetClientState();
        response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
        assertTrue(customHashtableTAICheck + " not found", response.contains(customHashtableTAICheck));
        assertTrue(hashtableUniqueIdCheck + " not found", response.contains(hashtableUniqueIdCheck));
        assertTrue(hashtableSecurityNameCheck + " not found", response.contains(hashtableSecurityNameCheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with a
     * <LI>valid user (user1). Also, include a hashtable login module adding properties (user2) to
     * <LI>public credentials of subject
     * <LI>Should return user1 from TAI, as well as user2 in subject from hashtable login module
     * </OL>
     */
    @Test
    public void testTAI_JAASLoginModulePublicCredentials() throws Exception {
        String methodName = "testTAI_JAASLoginModulePublicCredentials";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        //Set config with hashtable login module and TAI, then restart server
        setServerConfiguration(myServer, TAI_JAAS_LOGIN_MODULE_PUBLIC_CREDENTIALS_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        //Verify that user1 from TAI is returned
        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));
        //Verify user2 from hashtable login module is returned in public credentials of subject
        assertTrue(unqueIdCheck + managerUserFormat + " not found", response.contains(unqueIdCheck + managerUserFormat));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Protected servlet with TAI negotiateValidateandEstablishTrust with a
     * <LI>valid user (user1). Also, include a hashtable login module adding properties (user2) to
     * <LI>shared state of subject
     * <LI>Should only return user2 from hashtable login module with allowHashtableLoginWithIdOnly="true"
     * </OL>
     */
    @Test
    public void testTAI_JAASLoginModuleSharedState() throws Exception {
        String methodName = "testTAI_JAASLoginModuleSharedState";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        //Set config with hashtable login module and TAI, then restart server
        setServerConfiguration(myServer, TAI_JAAS_LOGIN_MODULE_SHARED_STATE_CONFIG_FILE);
        myServer.restartServer();
        verifyServerStarted();

        //Verify user2 from hashtable login module is returned
        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        assertTrue("Verification of programmatic APIs failed", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI>Both protected and unprotected servlets with invokeForUnprotectedURI="true"
     * <LI>Should return user2 from TAI for both servlets.
     * </OL>
     */
    @Test
    public void testTAI_InterceptedSetInvokeForUnprotectedURITrue() throws Exception {
        String methodName = "testTAI_InterceptedSetInvokeForUnprotectedURITrue";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_INVOKEFORUNPROTECTEDURI_TRUE_PROCESS_CONFIG_FILE);

        // Even with a protected servlet, authentication goes through TAI
        // successfully, so we are never prompted
        // That's why we call accessUnprotectedServlet method
        baClient.resetClientState();
        String response = baClient.accessUnprotectedServlet(BasicAuthClient.PROTECTED_SIMPLE);
        assertTrue("TAI should process for a protected resource.", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                           IS_MANAGER_ROLE));
        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        baClient.resetClientState();
        response = baClient.accessUnprotectedServlet(BasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        assertTrue("TAI should process for an unprotected resource.", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                              IS_MANAGER_ROLE));
        assertTrue(customUserTAICheck + " not found", response.contains(customUserTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * <OL>
     * <LI> accessing protected and unprotected servlets with invokeForUnprotectedURI="true"
     * <LI> for the protected servlet, a server should return user2 from TAI.
     * <LI> for unprotected servlet, unauthenticated should be returned after TAI intercepts the request.
     * </OL>
     */
    @Test
    public void testTAI_NotProcessedSetInvokeForUnprotectedURITrue() throws Exception {
        String methodName = "testTAI_NotProcessedSetInvokeForUnprotectedURITrue";
        Log.info(thisClass, methodName, "Entering test " + methodName);

        setServerConfiguration(myServer, TAI_INVOKEFORUNPROTECTEDURI_TRUE_NOT_PROCESS_CONFIG_FILE);

        baClient.resetClientState();
        String response = baClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
        assertTrue("basic auth should be carried out for a protected resource.", baClient.verifyResponse(response, managerUser, NOT_EMPLOYEE_ROLE,
                                                                                                         IS_MANAGER_ROLE));
        assertTrue(continueTAICheck + " not found", response.contains(continueTAICheck));

        baClient.resetClientState();
        response = baClient.accessUnprotectedServlet(BasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        assertTrue("TAI should not process for an unprotected resource.", baClient.verifyUnauthenticatedResponse(response));
        assertTrue("TAI should intercepts the request and return CONTINUE.", response.contains(continueTAICheck));

        Log.info(thisClass, methodName, "Exiting test " + methodName);
    }

    /**
     * This method is used to set the server.xml
     */
    private static void setServerConfiguration(LibertyServer server,
                                               String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            // Update server.xml
            Log.info(thisClass, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile("/" + serverXML);

            Log.info(thisClass, "setServerConfiguration",
                     "waitForStringInLogUsingMark: CWWKG0016I: Starting server configuration update.");
            assertNotNull("Server did not report configuration update was starting",
                          server.waitForStringInLogUsingMark("CWWKG0016I"));

            Log.info(thisClass, "setServerConfiguration",
                     "waitForStringInLogUsingMark: CWWKG0017I: The server configuration was successfully updated.");
            assertNotNull("Server did not report that the server configuration was successfully modified.",
                          server.waitForStringInLogUsingMark("CWWKG0017I"));

            serverConfigurationFile = serverXML;
        }
    }

    private static void verifyServerStarted() {
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLogUsingMark("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLogUsingMark("CWWKZ0001I"));
    }

    private static void verifyTAIInitLoad() {
        assertNotNull("The Trust Association Interceptor did not init load",
                      myServer.waitForStringInLogUsingMark("CWWKS1200I"));
    }

    private static void assertSSOCookieIsPresent(String response, String ssoCookieName) {
        assertTrue("Did not find the SSO cookie in the response as expected",
                   response.contains("Response Cookie: " + ssoCookieName) && !response.contains("Response Cookie: " + ssoCookieName + "=Null"));
    }

    private static void assertSSOCookieIsNotPresent(String response, String ssoCookieName) {
        assertTrue("Received SSO Cookie, but should not have.", response.contains("Response Cookie: " + ssoCookieName + "=Null"));
    }
}