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
 * Test cases for identity assertions of anonymous covering rows 9 through 12 of CSIv2 spec table 10.4 - TSS Interpretation of Client Credentials After Validation.
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class IdentityAssertionAnonymousTest extends CommonTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        upstreamServer = setupUpstreamServer("client_certSupported_ltpaSupported_assertionPrincipalName.xml");
        downstreamServer = setupDownstreamServer("certSupported_ltpaSupported_assertionPresumedTrust.xml");
        upstreamServer.setAllTestsRestartServer(true);
        downstreamServer.setAllTestsRestartServer(true);
        setupBasicAuthClient();
    }

// Table 10.4, Row 9

    /*
     * Row 9: None, None, Anonymous (Rule 4-Trust any to assert Anonymous)
     * Client and server with no client certificate authentication (No SSL), authentication layers set to Never, and identity assertion
     * yield Anonymous (Unauthenticated) subject.
     */
    @Test
    public void testIdentityAssertionAnonymous_NoSSLTransNoSSL_NeverAuthNever_NoIdentAssSecAttrNoIdentAss_unauthenticatedSubject() throws Exception {
        upstreamServer.restartServer("client_noSSL_authNever_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("noSSL_authNever_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlowUnprotected();
        assertNoPermission(response);
    }

    /*
     * Row 9: None, None, Anonymous (Rule 4-Trust any to assert Anonymous)
     * Client and server's client certificate authentication set to Never-Never, authentication layers with Never-Never, and identity assertion
     * yield Anonymous (Unauthenticated) subject.
     */
    @Test
    public void testIdentityAssertionAnonymous_NeverTransNever_NeverLTPAAuthNeverLTPA_PrincipalNameSecAttrPresumedTrust_unauthenticatedSubject() throws Exception {
        upstreamServer.restartServer("client_certNever_ltpaNever_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certNever_ltpaNever_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlowUnprotected();
        assertNoPermission(response);
    }

// Table 10.4, Row 10

    /*
     * Row 10: None, P2B, Anonymous (Rule 4-Trust any to assert Anonymous)
     * Client and server with no client certificate authentication (Never-Never), authentication layer compatible(GSSUP Supported-Supported), and identity assertion
     * yield Anonymous (Unauthenticated) subject.
     */

    /*
     * Row 10: None, P2B, Anonymous (Rule 4-Trust any to assert Anonymous)
     * Client and server with no client certificate authentication (Never-Never), authentication layer compatible(LTPA Supported-Supported), and identity assertion
     * yield Anonymous (Unauthenticated) subject.
     */

// Table 10.4, Row 11

    /*
     * Row 11: P2A, None, Anonymous (Rule 4-Trust any to assert Anonymous)
     * Client and server with client certificate authentication (Supported-Required), no authentication layer, and identity assertion
     * yield Anonymous (Unauthenticated) subject.
     */

    /*
     * Row 11: P2A, None, Anonymous (Rule 4-Trust any to assert Anonymous)
     * Client and server with client certificate authentication (Supported-Required), authentication layers (Supported-Never), and identity assertion
     * yield Anonymous (Unauthenticated) subject.
     */
    @Test
    public void testIdentityAssertionAnonymous_SuppTransReq_SuppLTPAAuthNeverLTPA_PrincipalNameSecAttrPresumedTrust_unauthenticatedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_ltpaSupported_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certRequired_ltpaNever_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlowUnprotected();
        assertNoPermission(response);
    }

// Table 10.4, Row 12

    /*
     * Row 12: P2A, P2B (by rule 1a), Anonymous (Rule 4-Trust any to assert Anonymous)
     * Client and server with client certificate authentication (Supported-Required), authentication layer compatible(GSSUP Supported-Supported), and identity assertion
     * yield Anonymous (Unauthenticated) subject.
     */
    @Test
    public void testIdentityAssertionAnonymous_SuppTransReq_SuppGSSUPAuthSuppGSSUP_PrincipalNameSecAttrPresumedTrust_unauthenticatedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_gssupSupported_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certRequired_gssupSupported_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlowUnprotected();
        assertNoPermission(response);
    }

    /*
     * Row 12: P2A, P2B (by rule 1a), Anonymous (Rule 4-Trust any to assert Anonymous)
     * Client and server with client certificate authentication (Supported-Supported), authentication layer compatible(LTPA Supported-Supported), and identity assertion
     * yield Anonymous (Unauthenticated) subject.
     */
    @Test
    public void testIdentityAssertionAnonymous_SuppTransSupp_SuppLTPAAuthSuppLTPA_PrincipalNameSecAttrPresumedTrust_unauthenticatedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_ltpaSupported_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certSupported_ltpaSupported_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlowUnprotected();
        assertNoPermission(response);
    }

    /*
     * Row 12: P2A, P2B (by rule 1a), Anonymous (Rule 4-Trust any to assert Anonymous)
     * Client and server with client certificate authentication (Supported-Required), authentication layer compatible(LTPA Supported-Supported), and identity assertion
     * yield Anonymous (Unauthenticated) subject.
     */
    @Test
    public void testIdentityAssertionAnonymous_SuppTransReq_SuppLTPAAuthSuppLTPA_PrincipalNameSecAttrPresumedTrust_unauthenticatedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_ltpaSupported_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certRequired_ltpaSupported_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlowUnprotected();
        assertNoPermission(response);
    }

    /*
     * Row 12: P2A, P2B (by rule 1a), Anonymous (Rule 4-Trust any to assert Anonymous)
     * Client and server with client certificate authentication (Supported-Required), no authentication layer (defaults), and identity assertion
     * yield Anonymous (Unauthenticated) subject.
     */
    @Test
    public void testIdentityAssertionAnonymous_SuppTransSupp_NoAuthNo_PrincipalNameSecAttrPresumedTrust_unauthenticatedSubject() throws Exception {
        upstreamServer.restartServer("client_certSupported_noAuthenticationLayer_assertionPrincipalName.xml", testName, upstreamApps, false, null);
        downstreamServer.restartServer("certRequired_noAuthenticationLayer_assertionPresumedTrust.xml", testName, downstreamApps, false, null);

        String response = driveDownstreamFlowUnprotected();
        assertNoPermission(response);
    }

    private void assertNoPermission(String response) {
        boolean noPermissionFound = response.contains("org.omg.CORBA.NO_PERMISSION");
        assertTrue("The servlet must receive a NO_PERMISSION exception.", noPermissionFound);
    }
}
