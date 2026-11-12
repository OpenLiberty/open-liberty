/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.csiv2.fat;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.csiv2.fat.common.CommonTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test cases for identity assertion scenarios covering rows 5 through 8 of CSIv2 spec table 10.4 - TSS Interpretation of Client Credentials After Validation.
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class IdentityAssertionTest extends CommonTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        upstreamServer = setupUpstreamServer("client_certSupported_ltpaSupported_assertionPrincipalName.xml");
        downstreamServer = setupDownstreamServer("certSupported_ltpaSupported_assertionPresumedTrust.xml");
        upstreamServer.setAllTestsRestartServer(true);
        downstreamServer.setAllTestsRestartServer(true);
        setupBasicAuthClient();
    }

// Table 10.4, Row 5
    /*
     * Row 5: None, None, P1 (Rule 2-Presumed Trust)
     * Client and server with no client certificate authentication (No transport layer), no authentication layers, and identity assertion
     * yield Asserted subject.
     */

    /*
     * Row 5: None, None, P1 (Rule 2-Presumed Trust)
     * Client and server's client certificate authentication set to Never-Never, authentication layers with Never-Never, and identity assertion
     * yield Asserted subject.
     */

    /*
     * Row 5: None, None, P1 (Rule 2-Presumed Trust)
     * Client and server's client certificate authentication set to Never-Never, authentication layers with Never-Supported, and identity assertion
     * yield Asserted subject.
     */

    /*
     * Row 5: None, None, P1 (Rule 2-Presumed Trust)
     * Client and server's client certificate authentication set to Never-Never, authentication layers with Supported-Never, and identity assertion
     * yield Asserted subject.
     */

    /*
     * Row 5: None, None, P1 (Rule 2-Presumed Trust)
     * Client and server's client certificate authentication set to Never-Supported, authentication layers with Never-Never, and identity assertion
     * yield Asserted subject.
     */
    @Test
    public void testIdentityAssertion_NeverTransSupp_NeverLTPAAuthNeverLTPA_IdentAssEnabledSecAttrPresumedTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certNever_ltpaNever_assertion.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_ltpaNever_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /*
     * Row 5: None, None, P1 (Rule 2-Presumed Trust)
     * Client and server's client certificate authentication set to Suppored-Never, authentication layers with Never-Never, and identity assertion
     * yield Asserted subject.
     */

// Table 10.4, Row 6

    /*
     * Row 6: None, P2B, P1 (Rule 2-Presumed Trust)
     * Client and server with no client certificate authentication (Never-Never), authentication layer compatible(GSSUP Supported-Supported), and identity assertion
     * yield Asserted subject.
     */

    /*
     * Row 6: None, P2B, P1 (Rule 2-Presumed Trust)
     * Client and server with no client certificate authentication (Never-Never), authentication layer compatible(LTPA Supported-Supported), and identity assertion
     * yield Asserted subject.
     */

    /*
     * Row 6: None, P2B, P1 (Rule 3-Backward Trust Validation)
     * Client and server with no client certificate authentication (Never-Never), authentication layer compatible(GSSUP Supported-Supported), and identity assertion
     * yield Asserted subject.
     */

    /*
     * Row 6: None, P2B, P1 (Rule 3-Backward Trust Validation)
     * Client and server with no client certificate authentication (Never-Never), authentication layer compatible(LTPA Supported-Supported), and identity assertion
     * yield Asserted subject.
     */

// Table 10.4, Row 7

    /*
     * Row 7: P2A, None, P1 (Rule 2-Presumed Trust)
     * Client and server with client certificate authentication (Supported-Supported), no authentication layer, and identity assertion
     * yield Asserted subject.
     * TODO: Check client logic that determines if assertion is taking place.
     */

    /*
     * Row 7: P2A, None, P1 (Rule 2-Presumed Trust)
     * Client and server with client certificate authentication (Supported-Required), no authentication layer, and identity assertion
     * yield Asserted subject.
     */

    /*
     * Row 7: P2A, None, P1 (Rule 2-Presumed Trust)
     * Client and server with client certificate authentication (Supported-Required), authentication layers (Supported-Never), and identity assertion
     * yield Asserted subject.
     */
    @Test
    public void testIdentityAssertion_SuppTransReq_SuppGSSUPNeverGSSUP_PrincipalNameSecAttrPresumedTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_gssupSupported_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certRequired_gssupNever_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /*
     * Row 7: P2A, None, P1 (Rule 2-Presumed Trust)
     * Client and server with client certificate authentication (Supported-Required), authentication layers (Never-Supported), and identity assertion
     * yield Asserted subject.
     */
    @Test
    public void testIdentityAssertion_SuppTransReq_NeverGSSUPAuthSuppGSSUP_IdentAssEnabledSecAttrPresumedTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_gssupNever_assertion.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certRequired_gssupSupported_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /*
     * Row 7: P2A, None, P1 (Rule 3-Backward Trust Validation)
     * Client and server with client certificate authentication (Supported-Supported), no authentication layer, and identity assertion
     * yield Asserted subject.
     */

    /*
     * Row 7: P2A, None, P1 (Rule 3-Backward Trust Validation)
     * Client and server with client certificate authentication (Supported-Required), no authentication layer, and identity assertion
     * yield Asserted subject.
     */
//    @Test
    public void testIdentityAssertion_SuppTransReq_NoAuthNo_PrincipalNameSecAttrBackwardTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_noAuth_assertion.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certRequired_noAuth_assertionBackwardTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /*
     * Row 7: P2A, None, P1 (Rule 3-Backward Trust Validation)
     * Client and server with client certificate authentication (Supported-Required), authentication layers (Supported-Never), and identity assertion
     * yield Asserted subject.
     */
    @Test
    public void testIdentityAssertion_SuppTransReq_SuppGSSUPAuthNeverGSSUP_IdentAssEnabledSecAttrBackwardTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_gssupSupported_assertion.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certRequired_gssupNever_assertionBackwardTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /*
     * Row 7: P2A, None, P1 (Rule 3-Backward Trust Validation)
     * Client and server with client certificate authentication (Supported-Required), authentication layers (Never-Supported), and identity assertion
     * yield Asserted subject.
     */
    @Test
    public void testIdentityAssertion_SuppTransSupp_NeverLTPAAuthSuppGSSUP_IdentAssEnabledSecAttrBackwardTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_ltpaNever_assertion.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_gssupSupported_assertionBackwardTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

// Table 10.4, Row 8

    /*
     * Row 8: P2A, P2B (by rule 1a), P1 (Rule 2-Presumed Trust)
     * Client and server with client certificate authentication (Supported-Supported), authentication layer compatible (Supported-Supported), and identity assertion
     * yield Asserted subject.
     */
    @Test
    public void testIdentityAssertion_SuppTransSupp_SuppLTPAAuthSuppLTPA_PrincipalNameSecAttrPresumedTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_ltpaSupported_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_ltpaSupported_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /*
     * Row 8: P2A, P2B (by rule 1a), P1 (Rule 2-Presumed Trust)
     * Client and server with client certificate authentication (Supported-Required), authentication layer compatible (Supported-Supported), and identity assertion
     * yield Asserted subject.
     */
    @Test
    public void testIdentityAssertion_SuppTransSupp_SuppGSSUPAuthSuppGSSUP_IdentAssEnabledSecAttrPresumedTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_gssupSupported_assertion.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_gssupSupported_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    @Test
    public void testIdentityAssertion_SuppTransSupp_NoAuthNo_PrincipalNameSecAttrPresumedTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_noAuth_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_noAuth_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    @Test
    public void testIdentityAssertion_SuppTransReq_NoAuthNo_PrincipalNameSecAttrPresumedTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_noAuth_assertion.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certRequired_noAuth_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /*
     * Row 8: P2A, P2B (by rule 1a), P1 (Rule 3-Backward Trust Validation)
     * Client and server with client certificate authentication (Supported-Supported), authentication layer compatible (Supported-Supported), and identity assertion
     * yield Asserted subject.
     */
    @Test
    public void testIdentityAssertion_SuppTransSupp_SuppGSSUPAuthSuppGSSUP_PrincipalNameSecAttrPrincipalName_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_gssupSupported_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_gssupSupported_assertionPrincipalName.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertSubjectInResponse(response, user);
    }

    /*
     * Row 8: P2A, P2B (by rule 1a), P1 (Rule 3-Backward Trust Validation)
     * Client and server with client certificate authentication (Supported-Supported), authentication layer compatible (Supported-Supported), and identity assertion
     * yield Asserted subject.
     */
    @Test
    public void testIdentityAssertion_SuppTransSupp_SuppLTPAAuthSuppLTPA_PrincipalNameSecAttrPrincipalName_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_ltpaSupported_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_ltpaSupported_assertionPrincipalName.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /*
     * Row 8: P2A, P2B (by rule 1a), P1 (Rule 3-Backward Trust Validation)
     * Client and server with client certificate authentication (Supported-Required), authentication layer compatible (Supported-Supported), and identity assertion
     * yield Asserted subject.
     */
    @Test
    public void testIdentityAssertion_SuppTransReq_SuppLTPAAuthSuppLTPA_PrincipalNameSecAttrBackwardTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_ltpaSupported_assertion.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certRequired_ltpaSupported_assertionBackwardTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    @Test
    public void testIdentityAssertion_SuppTransSupp_NoAuthNo_PrincipalNameSecAttrBackwardTrust_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_noAuth_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_noAuth_assertionBackwardTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

    /*
     * Negative and serviceability scenarios only after this point
     */

    /*
     * Certificate authentication, no authentication layer, assertion with cert chain NOT trusted
     * yields CORBA_NOPERMISSION.
     */
    @Test
    public void testIdentityAssertion_SuppTransSupp_NoAuthNo_PrincipalNameSecAttrNoTrust_noPermission() throws Exception {
        upstreamServer.restartServer("client_certSupported_noAuth_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_noAuth_assertionNoTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlow();

        assertTrue("The servlet must receive a NO_PERMISSION exception.",
                   response.contains("org.omg.CORBA.NO_PERMISSION") && response.contains("vmcid: 0x49424000 minor code: 0x30d  completed: No"));
    }

    /**
     * Client and server configured with two different realm and confirm the authentication
     * is successful
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testIdentityAssertion_SuppTransSupp_SuppGSSUPAuthSuppGSSUP_CustomRealmSecAttrPrincipalName_assertedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_gssupSupported_Assertion_customRealm.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_gssupSupported_assertionPrincipalName.xml", testName, downstreamApps, false, null);
        String response = driveDownstreamFlow();
        assertSubjectInResponse(response, user);
    }

}
