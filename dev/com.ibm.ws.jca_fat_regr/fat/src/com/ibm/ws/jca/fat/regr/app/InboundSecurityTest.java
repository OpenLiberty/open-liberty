/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.regr.app;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.jca.fat.regr.util.JCAFATTest;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServerFactory;
import suite.r80.base.jca16.RarTests;
import suite.r80.base.jca16.TestSetupUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class InboundSecurityTest extends JCAFATTest implements RarTests {
    private final static Class<?> c = InboundSecurityTest.class;
    private final String servletName = "InboundSecurityTestServlet";
    private static ServerConfiguration originalServerConfig;

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, false);
        originalServerConfig = server.getServerConfiguration().clone();

        TestSetupUtils.setUpFvtApp(server);
        TestSetupUtils.setUpGwcApp(server);

        server.startServer("InboundSecurityTest.log");
        server.waitForStringInLog("CWWKE0002I");
        server.waitForStringInLog("CWWKZ0001I:.*fvtapp"); // Wait for application start.
        server.waitForStringInLog("CWWKF0011I");
        server.waitForStringInLog("CWWKS4104A"); // Wait for Ltpa keys to be generated
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (server.isStarted()) {
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
            }
        } finally {
            server.updateServerConfiguration(originalServerConfig);
        }
    }

    @Test
    public void testCallerIdentityPropagationFromCallerPrincipalCallbackStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationFromCallerPrincipalCallbackScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    @ExpectedFFDC({ "com.ibm.websphere.security.WSSecurityException", "java.util.concurrent.RejectedExecutionException" })
    public void testCallerIdentityPropagationFailureForDifferentRealmStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
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
    @ExpectedFFDC({ "com.ibm.websphere.security.WSSecurityException", "java.util.concurrent.RejectedExecutionException" })
    public void testCallerIdentityPropagationFailureForDifferentRealmScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
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
    @ExpectedFFDC({ "com.ibm.websphere.security.WSSecurityException", "java.util.concurrent.RejectedExecutionException" })
    public void testCallerIdentityPropagationFailureForMultipleCPCStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
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
    @ExpectedFFDC({ "com.ibm.websphere.security.WSSecurityException", "java.util.concurrent.RejectedExecutionException" })
    public void testCallerIdentityPropagationFailureForMultipleCPCScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
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
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
    }

    @Test
    public void testCallerIdentityPropagationDiffSubjectInCallbackStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
    }

    @Test
    public void testCallerIdentityPropagationDiffSubjectInCallbackScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
    }

    @Test
    @ExpectedFFDC({ "java.util.concurrent.RejectedExecutionException", "javax.resource.spi.work.WorkCompletedException" })
    public void testFailureAuthenticatedSubjectandCPC() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull(
                      "Expected Message J2CA0677E not found.",
                      server.waitForStringInLog(AUTHENTICATED_SUBJECT_NOT_SUPPORTED_J2CA0677));
    }

    @Test
    @ExpectedFFDC("java.util.concurrent.RejectedExecutionException")
    public void testFailureAuthenticatedSubjectandCPCStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull(
                      "Expected Message J2CA0677E not found.",
                      server.waitForStringInLog(AUTHENTICATED_SUBJECT_NOT_SUPPORTED_J2CA0677));
    }

    @Test
    @ExpectedFFDC("java.util.concurrent.RejectedExecutionException")
    public void testFailureAuthenticatedSubjectandCPCScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull(
                      "Expected Message J2CA0677E not found.",
                      server.waitForStringInLog(AUTHENTICATED_SUBJECT_NOT_SUPPORTED_J2CA0677));
    }

    @Test
    public void testUnauthenticatedEstablishmentNullCallerPrincipal() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testUnauthenticatedEstablishmentNullCallerPrincipalStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testUnauthenticatedEstablishmentNullCallerPrincipalScheduleWork() throws Exception {
        ;
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptySubject() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptySubjectStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptySubjectScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubject() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubjectStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubjectScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    @ExpectedFFDC({ "java.util.concurrent.RejectedExecutionException", "javax.resource.spi.work.WorkCompletedException" })
    public void testCallerIdentityPropagationFailureForMultiplePrincipalNoCPC() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "Expected Message J2CA0669E not found.",
                      server.waitForStringInLog(CALLERPRINCIPAL_NOT_PROVIDED_J2CA0669));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @ExpectedFFDC({ "java.util.concurrent.RejectedExecutionException" })
    @AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "java.util.concurrent.ExecutionException" })
    public void testCallerIdentityPropagationFailureForMultiplePrincipalNoCPCStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "Expected Message J2CA0669E not found.",
                      server.waitForStringInLog(CALLERPRINCIPAL_NOT_PROVIDED_J2CA0669));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @ExpectedFFDC("java.util.concurrent.RejectedExecutionException")
    public void testCallerIdentityPropagationFailureForMultiplePrincipalNoCPCScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "Expected Message J2CA0669E not found.",
                      server.waitForStringInLog(CALLERPRINCIPAL_NOT_PROVIDED_J2CA0669));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    public void testCallerIdentityPropagationForNestedWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInChild() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInChildStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInChildScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoSecCtxChild() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoSecCtxChildStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoSecCtxChildScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInParent() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInParentStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInParentScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVC() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVCStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVCScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    @ExpectedFFDC({ "java.util.concurrent.RejectedExecutionException", "javax.resource.spi.work.WorkCompletedException" })
    public void testCallerIdentityPropagationNonMatchingCPCPVC() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull("Expected Message J2CA0675E not found.",
                      server.waitForStringInLog(USER_NAME_MISMATCH_J2CA0675));
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @ExpectedFFDC({ "java.util.concurrent.RejectedExecutionException" })
    @AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "java.util.concurrent.ExecutionException" })
    public void testCallerIdentityPropagationNonMatchingCPCPVCStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull("Expected Message J2CA0675E not found.",
                      server.waitForStringInLog(USER_NAME_MISMATCH_J2CA0675));
        assertNotNull("Expected Message J2CA0668E not found.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "Expected Message J2CA0671E not found.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @ExpectedFFDC("java.util.concurrent.RejectedExecutionException")
    @AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "java.util.concurrent.ExecutionException" })
    public void testCallerIdentityPropagationNonMatchingCPCPVCScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
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
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVCValidGPCStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPCPVCValidGPCScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationValidCPCInvalidGPC() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull("Expected Message J2CA0678W not found.",
                      server.waitForStringInLog(INVALID_GROUP_ENCOUNTERED_J2CA0678));

    }

    @Test
    public void testCallerIdentityPropagationValidCPCInvalidGPCStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull("Expected Message J2CA0678W not found.",
                      server.waitForStringInLog(INVALID_GROUP_ENCOUNTERED_J2CA0678));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCInvalidGPCScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull("Expected Message J2CA0678W not found.",
                      server.waitForStringInLog(INVALID_GROUP_ENCOUNTERED_J2CA0678));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCInvalidSubject() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCInvalidSubjectStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCInvalidSubjectScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "Expected Message J2CA0673W not found.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    @ExpectedFFDC({ "java.util.concurrent.RejectedExecutionException", "javax.resource.spi.work.WorkCompletedException" })
    public void testCallerIdentityPropagationUnexpectedError() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "The log contains a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("The log contains Unexpected error occurred.",
                      server.waitForStringInLog("Unexpected error occurred."));
    }

    @Test
    @ExpectedFFDC("java.util.concurrent.RejectedExecutionException")
    @AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "java.util.concurrent.ExecutionException" })
    public void testCallerIdentityPropagationUnexpectedErrorStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "The log contains a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("The log contains Unexpected error occurred.",
                      server.waitForStringInLog("Unexpected error occurred."));
    }

    @Test
    @ExpectedFFDC("java.util.concurrent.RejectedExecutionException")
    public void testCallerIdentityPropagationUnexpectedErrorScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "The log contains a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
        assertNotNull("The log contains Unexpected error occurred.",
                      server.waitForStringInLog("Unexpected error occurred."));
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPC2PVC() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPC2PVCStartWork() throws Exception {
        ;
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationMatchingCPC2PVCScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipal() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipalStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipalScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptyCallerPrincipal() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptyCallerPrincipalStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testUnauthenticatedEstablishmentEmptyCallerPrincipalScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationValidCPCEmptyGPCEntry() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationValidCPCEmptyGPCEntryStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testCallerIdentityPropagationValidCPCEmptyGPCEntryScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    @ExpectedFFDC({ "java.util.concurrent.RejectedExecutionException" })
    @AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "java.util.concurrent.ExecutionException" })
    public void testCallerIdentityPropagationMatchingEmptyCPCPVC() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "The log contains a J2CA0674E error message.",
                      server.waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674));
        assertNotNull("The log contains a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log contains a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @ExpectedFFDC("java.util.concurrent.RejectedExecutionException")
    @AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "java.util.concurrent.ExecutionException" })
    public void testCallerIdentityPropagationMatchingEmptyCPCPVCStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "The log contains a J2CA0674E error message.",
                      server.waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674));
        assertNotNull("The log contains a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log contains a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @ExpectedFFDC("java.util.concurrent.RejectedExecutionException")
    @AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "java.util.concurrent.ExecutionException" })
    public void testCallerIdentityPropagationMatchingEmptyCPCPVCScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull(
                      "The log contains a J2CA0674E error message.",
                      server.waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674));
        assertNotNull("The log contains a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log contains a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @ExpectedFFDC({ "java.util.concurrent.RejectedExecutionException" })
    @AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "java.util.concurrent.ExecutionException" })
    public void testCallerIdentityPropagationMatchingCPCPVCInvalidPassword() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        String str = server.waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674);
        if (str == null) {
            str = server.waitForStringInLog(PASSWORD_VALIDATION_FAILED_J2CA0684);
        }
        assertNotNull(
                      "The log contains a J2CA0674E or J2CA0684E error message.", str);
        assertNotNull("The log contains a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log contains a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));

    }

    @Test
    @ExpectedFFDC("java.util.concurrent.RejectedExecutionException")
    @AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "java.util.concurrent.ExecutionException" })
    public void testCallerIdentityPropagationMatchingCPCPVCInvalidPasswordStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        String str = server.waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674);
        if (str == null) {
            str = server.waitForStringInLog(PASSWORD_VALIDATION_FAILED_J2CA0684);
        }
        assertNotNull(
                      "The log contains a J2CA0674E or J2CA0684E error message.", str);

        assertNotNull("The log contains a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log contains a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    @ExpectedFFDC("java.util.concurrent.RejectedExecutionException")
    @AllowedFFDC({ "javax.resource.spi.work.WorkCompletedException", "java.util.concurrent.ExecutionException" })
    public void testCallerIdentityPropagationMatchingCPCPVCInvalidPasswordScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        String str = server.waitForStringInLog(INVALID_USERNAME_PASSWORD_INBOUND_J2CA0674);
        if (str == null) {
            str = server.waitForStringInLog(PASSWORD_VALIDATION_FAILED_J2CA0684);
        }
        assertNotNull(
                      "The log contains a J2CA0674E or J2CA0684E error message.", str);

        assertNotNull("The log contains a J2CA0668E error message.",
                      server.waitForStringInLog(CUSTOM_CREDENTIALS_MISSING_J2CA0668));
        assertNotNull(
                      "The log contains a J2CA0671E error message.",
                      server.waitForStringInLog(SECURITY_CONTEXT_NOT_ASSOCIATED_J2CA0671));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCNullSubject() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "The log contains a J2CA0673W warning message.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCNullSubjectStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "The log contains a J2CA0673W warning message.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    public void testCallerIdentityPropagationValidCPCGPCNullSubjectScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        String CPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the CallerPrincipalCallback.";
        String GPCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the GroupPrincipalCallback.";
        String PVCMessage = "J2CA0673W: The execution subject provided by the WorkManager does not match the subject supplied by the PasswordValidationCallback.";
        assertNotNull(
                      "The log contains a J2CA0673W warning message.",
                      server.waitForStringInLog(EXECUTION_CALLBACK_SUBJECT_MISMATCH_J2CA0673));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a CallerPrincipalCallback.",
                      server.waitForStringInLog(CPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a GroupPrincipalCallback.",
                      server.waitForStringInLog(GPCMessage));
        assertNotNull(
                      "The log contains a J2CA0673W warning message for a PasswordValidationCallback.",
                      server.waitForStringInLog(PVCMessage));
    }

    @Test
    public void testSuccessAuthenticatedSubject() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testSuccessAuthenticatedSubjectStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testSuccessAuthenticatedSubjectScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
    }

    @Test
    public void testEJBInvocationStartWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull("The log contains a EJBDEMOEXECUTE message.",
                      server.waitForStringInLog("EJBDEMOEXECUTE"));
        assertNotNull("The log contains a SECJ0053E error message.",
                      server.waitForStringInLog(AUTHORIZATION_FAILED_SECJ0053E));
    }

    @Test
    public void testEJBInvocationScheduleWork() throws Exception {
        runInJCAFATServlet("fvtweb", servletName, getTestMethodSimpleName());
        assertNotNull("The log contains a EJBDEMOEXECUTE message.",
                      server.waitForStringInLog("EJBDEMOEXECUTE"));
        assertNotNull("The log contains a SECJ0053E error message.",
                      server.waitForStringInLog(AUTHORIZATION_FAILED_SECJ0053E));
    }

}
