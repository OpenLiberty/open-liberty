/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jca.fat.regr;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class InboundSecurityTest extends JCAFATTest implements RarTests {
    private final static Class<?> c = InboundSecurityTest.class;
    private final String servletName = "InboundSecurityTestServlet";
    private static ServerConfiguration originalServerConfig;

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, true);
        buildFvtAppEar();

        originalServerConfig = server.getServerConfiguration().clone();
        server.startServer("InboundSecurityTest.log");
        server.waitForStringInLog("CWWKE0002I");
        server.waitForStringInLog("CWWKZ0001I:.*fvtapp"); // Wait for application start.
        server.waitForStringInLog("CWWKF0011I");
        server.waitForStringInLog("CWWKS4104A"); // Wait for Ltpa keys to be generated
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer("J2CA0677E", // EXPECTED
                              "J2CA0668E", // EXPECTED
                              "J2CA0669E: .*CallerPrincipalCallback", // EXPECTED
                              "J2CA0670E: .*NonExistentRealm/JosephABCDEF", // EXPECTED
                              "J2CA0671E", // EXPECTED
                              "J2CA0673W: .*(Caller|Group|Password)(Principal|Validation)Callback", // EXPECTED
                              "J2CA0674E: .*(null|Joseph) .*PasswordValidationCallback", // EXPECTED
                              "J2CA0675E: The user name Joseph, provided by the PasswordValidationCallback, and the user name Susan, provided by the CallerPrincipalCallback, do not match", // EXPECTED
                              "J2CA0676E: .*CallerPrincipalCallback", // EXPECTED
                              "J2CA0678W: Group testGrpNonExistent" // EXPECTED
            );
        } finally {
            server.updateServerConfiguration(originalServerConfig);
        }
    }

    @Test
    public void testCallerIdentityPropagationFromCallerPrincipalCallbackStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationFromCallerPrincipalCallbackStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationFromCallerPrincipalCallbackScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationFromCallerPrincipalCallbackScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationFailureForDifferentRealmStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationFailureForDifferentRealmStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0670E not found.",
                      server.waitForStringInLog(INVALID_USER_NAME_IN_PRINCIPAL_J2CA0670));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("Expected Message J2CA0672E not found.",
                      server.waitForStringInLog(ERROR_HANDLING_CALLBACK_J2CA0672));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationFailureForDifferentRealmScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationFailureForDifferentRealmScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0670E not found.",
                      server.waitForStringInLog(INVALID_USER_NAME_IN_PRINCIPAL_J2CA0670));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("Expected Message J2CA0672E not found.",
                      server.waitForStringInLog(ERROR_HANDLING_CALLBACK_J2CA0672));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationFailureForMultipleCPCStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationFailureForMultipleCPCStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0676E not found.",
                      server.waitForStringInLog(MULTIPLE_CALLERPRINCIPALCALLBACKS_NOT_SUPPORTED_J2CA0676));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("Expected Message J2CA0672E not found.",
                      server.waitForStringInLog(ERROR_HANDLING_CALLBACK_J2CA0672));

    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationFailureForMultipleCPCScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationFailureForMultipleCPCScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0676E not found.",
                      server.waitForStringInLog(MULTIPLE_CALLERPRINCIPALCALLBACKS_NOT_SUPPORTED_J2CA0676));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("Expected Message J2CA0672E not found.",
                      server.waitForStringInLog(ERROR_HANDLING_CALLBACK_J2CA0672));

    }

    @Test
    public void testCallerIdentityPropagationDiffSubjectInCallback() throws Exception {
        String testName = "testCallerIdentityPropagationDiffSubjectInCallback";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
    }

    @Test
    public void testCallerIdentityPropagationDiffSubjectInCallbackStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationDiffSubjectInCallbackStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
    }

    @Test
    public void testCallerIdentityPropagationDiffSubjectInCallbackScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationDiffSubjectInCallbackScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
    }

    @Test
    @AllowedFFDC
    public void testFailureAuthenticatedSubjectandCPC() throws Exception {
        String testName = "testFailureAuthenticatedSubjectandCPC";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull(
                      "Expected Message J2CA0677E not found.",
                      server.waitForStringInLog(AUTHENTICATED_SUBJECT_NOT_SUPPORTED_J2CA0677));
    }

    @Test
    @AllowedFFDC
    public void testFailureAuthenticatedSubjectandCPCStartWork() throws Exception {
        String testName = "testFailureAuthenticatedSubjectandCPCStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull(
                      "Expected Message J2CA0677E not found.",
                      server.waitForStringInLog(AUTHENTICATED_SUBJECT_NOT_SUPPORTED_J2CA0677));
    }

    @Test
    @AllowedFFDC
    public void testFailureAuthenticatedSubjectandCPCScheduleWork() throws Exception {
        String testName = "testFailureAuthenticatedSubjectandCPCScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull(
                      "Expected Message J2CA0677E not found.",
                      server.waitForStringInLog(AUTHENTICATED_SUBJECT_NOT_SUPPORTED_J2CA0677));
    }

    @Test
    public void testUnauthenticatedEstablishmentNullCallerPrincipal() throws Exception {
        String testName = "testUnauthenticatedEstablishmentNullCallerPrincipal";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testUnauthenticatedEstablishmentNullCallerPrincipalStartWork() throws Exception {
        String testName = "testUnauthenticatedEstablishmentNullCallerPrincipalStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testUnauthenticatedEstablishmentNullCallerPrincipalScheduleWork() throws Exception {
        String testName = "testUnauthenticatedEstablishmentNullCallerPrincipalScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptySubject() throws Exception {
        String testName = "testUnauthenticatedEstablishmentEmptySubject";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptySubjectStartWork() throws Exception {
        String testName = "testUnauthenticatedEstablishmentEmptySubjectStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptySubjectScheduleWork() throws Exception {
        String testName = "testUnauthenticatedEstablishmentEmptySubjectScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubject() throws Exception {
        String testName = "testCallerIdentityPropagationFromExecutionSubject";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubjectStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationFromExecutionSubjectStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubjectScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationFromExecutionSubjectScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationFailureForMultiplePrincipalNoCPC() throws Exception {
        String testName = "testCallerIdentityPropagationFailureForMultiplePrincipalNoCPC";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "Expected Message J2CA0669E not found.",
                      server.waitForStringInLog(CALLERPRINCIPAL_NOT_PROVIDED_J2CA0669));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationFailureForMultiplePrincipalNoCPCStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationFailureForMultiplePrincipalNoCPCStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "Expected Message J2CA0669E not found.",
                      server.waitForStringInLog(CALLERPRINCIPAL_NOT_PROVIDED_J2CA0669));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationFailureForMultiplePrincipalNoCPCScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationFailureForMultiplePrincipalNoCPCScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "Expected Message J2CA0669E not found.",
                      server.waitForStringInLog(CALLERPRINCIPAL_NOT_PROVIDED_J2CA0669));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    public void testCallerIdentityPropagationForNestedWork() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInChild() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkNoIdentityInChild";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInChildStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkNoIdentityInChildStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInChildScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkNoIdentityInChildScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoSecCtxChild() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkNoSecCtxChild";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoSecCtxChildStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkNoSecCtxChildStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoSecCtxChildScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkNoSecCtxChildScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInParent() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkNoIdentityInParent";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInParentStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkNoIdentityInParentStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInParentScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationForNestedWorkNoIdentityInParentScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVC() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPCPVC";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVCStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPCPVCStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVCScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPCPVCScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationNonMatchingCPCPVC() throws Exception {
        String testName = "testCallerIdentityPropagationNonMatchingCPCPVC";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0675E not found.",
                      server.waitForStringInLog(USER_NAME_MISMATCH_J2CA0675));
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationNonMatchingCPCPVCStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationNonMatchingCPCPVCStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0675E not found.",
                      server.waitForStringInLog(USER_NAME_MISMATCH_J2CA0675));
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationNonMatchingCPCPVCScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationNonMatchingCPCPVCScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0675E not found.",
                      server.waitForStringInLog(USER_NAME_MISMATCH_J2CA0675));
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVCValidGPC() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPCPVCValidGPC";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVCValidGPCStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPCPVCValidGPCStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVCValidGPCScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPCPVCValidGPCScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationValidCPCInvalidGPC() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCInvalidGPC";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0678W not found.",
                      server.waitForStringInLog(INVALID_GROUP_ENCOUNTERED_J2CA0678));

    }

    @Test
    public void testCallerIdentityPropagationValidCPCInvalidGPCStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCInvalidGPCStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0678W not found.",
                      server.waitForStringInLog(INVALID_GROUP_ENCOUNTERED_J2CA0678));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCInvalidGPCScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCInvalidGPCScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("Expected Message J2CA0678W not found.",
                      server.waitForStringInLog(INVALID_GROUP_ENCOUNTERED_J2CA0678));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCInvalidSubject() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCGPCInvalidSubject";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCInvalidSubjectStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCGPCInvalidSubjectStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCInvalidSubjectScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCGPCInvalidSubjectScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationUnexpectedError() throws Exception {
        String testName = "testCallerIdentityPropagationUnexpectedError";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "The log does NOT contain a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("The log does NOT contain Unexpected error occurred.",
                      server.waitForStringInLog("Unexpected error occurred."));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationUnexpectedErrorStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationUnexpectedErrorStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "The log does NOT contain a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("The log does NOT contain Unexpected error occurred.",
                      server.waitForStringInLog("Unexpected error occurred."));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationUnexpectedErrorScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationUnexpectedErrorScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "The log does NOT contain a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("The log does NOT contain Unexpected error occurred.",
                      server.waitForStringInLog("Unexpected error occurred."));
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPC2PVC() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPC2PVC";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPC2PVCStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPC2PVCStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPC2PVCScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPC2PVCScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipal() throws Exception {
        String testName = "testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipal";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipalStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipalStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipalScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipalScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptyCallerPrincipal() throws Exception {
        String testName = "testUnauthenticatedEstablishmentEmptyCallerPrincipal";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptyCallerPrincipalStartWork() throws Exception {
        String testName = "testUnauthenticatedEstablishmentEmptyCallerPrincipalStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptyCallerPrincipalScheduleWork() throws Exception {
        String testName = "testUnauthenticatedEstablishmentEmptyCallerPrincipalScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationValidCPCEmptyGPCEntry() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCEmptyGPCEntry";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationValidCPCEmptyGPCEntryStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCEmptyGPCEntryStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testCallerIdentityPropagationValidCPCEmptyGPCEntryScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCEmptyGPCEntryScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationMatchingEmptyCPCPVC() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingEmptyCPCPVC";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "The log does NOT contain a J2CA0674E error message.",
                      server.waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674));
        assertNotNull("The log does NOT contain a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log does NOT contain a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationMatchingEmptyCPCPVCStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingEmptyCPCPVCStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "The log does NOT contain a J2CA0674E error message.",
                      server.waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674));
        assertNotNull("The log does NOT contain a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log does NOT contain a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationMatchingEmptyCPCPVCScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingEmptyCPCPVCScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull(
                      "The log does NOT contain a J2CA0674E error message.",
                      server.waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674));
        assertNotNull("The log does NOT contain a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log does NOT contain a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationMatchingCPCPVCInvalidPassword() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPCPVCInvalidPassword";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        String str = server
                        .waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674);
        if (str == null) {
            str = server
                            .waitForStringInLog(PASSWORD_VALIDATION_FAILED_J2CA0684);
        }
        assertNotNull(
                      "The log does NOT contain a J2CA0674E or J2CA0684E error message.", str);
        assertNotNull("The log does NOT contain a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log does NOT contain a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));

    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationMatchingCPCPVCInvalidPasswordStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPCPVCInvalidPasswordStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        String str = server
                        .waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674);
        if (str == null) {
            str = server
                            .waitForStringInLog(PASSWORD_VALIDATION_FAILED_J2CA0684);
        }
        assertNotNull(
                      "The log does NOT contain a J2CA0674E or J2CA0684E error message.", str);

        assertNotNull("The log does NOT contain a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log does NOT contain a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @AllowedFFDC
    public void testCallerIdentityPropagationMatchingCPCPVCInvalidPasswordScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationMatchingCPCPVCInvalidPasswordScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        String str = server
                        .waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674);
        if (str == null) {
            str = server
                            .waitForStringInLog(PASSWORD_VALIDATION_FAILED_J2CA0684);
        }
        assertNotNull(
                      "The log does NOT contain a J2CA0674E or J2CA0684E error message.", str);

        assertNotNull("The log does NOT contain a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log does NOT contain a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCNullSubject() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCGPCNullSubject";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCNullSubjectStartWork() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCGPCNullSubjectStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCNullSubjectScheduleWork() throws Exception {
        String testName = "testCallerIdentityPropagationValidCPCGPCNullSubjectScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log does NOT contain a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    public void testSuccessAuthenticatedSubject() throws Exception {
        String testName = "testSuccessAuthenticatedSubject";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testSuccessAuthenticatedSubjectStartWork() throws Exception {
        String testName = "testSuccessAuthenticatedSubjectStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testSuccessAuthenticatedSubjectScheduleWork() throws Exception {
        String testName = "testSuccessAuthenticatedSubjectScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
    }

    @Test
    public void testEJBInvocationStartWork() throws Exception {
        String testName = "testEJBInvocationStartWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("The log does NOT contain a EJBDEMOEXECUTE message.",
                      server.waitForStringInLog("EJBDEMOEXECUTE"));
        assertNotNull("The log does NOT contain a SECJ0053E error message.",
                      server.waitForStringInLog(AUTHORIZATION_FAILED_SECJ0053E));
    }

    @Test
    public void testEJBInvocationScheduleWork() throws Exception {
        String testName = "testEJBInvocationScheduleWork";
        Log.info(c, testName, "Executing " + testName);
        runInJCAFATServlet("fvtweb", servletName, testName);
        assertNotNull("The log does NOT contain a EJBDEMOEXECUTE message.",
                      server.waitForStringInLog("EJBDEMOEXECUTE"));
        assertNotNull("The log does NOT contain a SECJ0053E error message.",
                      server.waitForStringInLog(AUTHORIZATION_FAILED_SECJ0053E));
    }

}
