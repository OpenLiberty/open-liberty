/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso.fat.config;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * FAT tests covering SAML Content Security Policy (CSP) header behaviour.
 * Validates SAML redirect page behaviour under Content Security Policy nonce enforcement.
 * when a nonce-based policy is active.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SAMLContentSecurityPolicyTests extends SAMLConfigCommonTests {

    private static final Class<?> thisClass = SAMLContentSecurityPolicyTests.class;

    /********************************************************
     * Tests
     ********************************************************/

    /*************************************************
     * contentSecurityPolicy — nonce-based CSP
     *************************************************/

    /**
     * Config attribute: contentSecurityPolicy
     * Validates that the SAML redirect page works end-to-end when
     * contentSecurityPolicy is configured with a %NONCE% placeholder.
     * The inline form-submit script on the redirect page must receive the
     * generated nonce so the browser does not block it when a strict
     * nonce-based CSP header is present.
     *
     * @throws Exception
     */
    @Test
    public void testCspHeader_withNonce_redirectPageHasNoOnloadAttribute() throws Exception {

        testSAMLServer.reconfigServer("server_csp_withNonce.xml", _testName, null, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW,
                helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
    }

    /**
     * Config attribute: contentSecurityPolicy
     * End-to-end solicited SP-initiated flow succeeds when contentSecurityPolicy
     * uses the %NONCE% placeholder.
     *
     * @throws Exception
     */
    @Test
    public void testCspHeader_withNonce_flowCompletesSuccessfully() throws Exception {

        testSAMLServer.reconfigServer("server_csp_withNonce.xml", _testName, null, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW,
                helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
    }

    /**
     * Config attribute: contentSecurityPolicy (absent)
     * Baseline — the solicited SP-initiated flow still completes successfully
     * when no contentSecurityPolicy attribute is set on the samlWebSso20 element.
     *
     * @throws Exception
     */
    @Test
    public void testCspHeader_noCsp_flowCompletesSuccessfully() throws Exception {

        testSAMLServer.reconfigServer("server_csp_noCsp.xml", _testName, null, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW,
                helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
    }

    /**
     * Config attribute: contentSecurityPolicy
     * IDP-initiated flow also completes successfully when contentSecurityPolicy
     * uses the %NONCE% placeholder. Confirms the nonce mechanism does not
     * interfere with IDP-initiated SAML.
     *
     * @throws Exception
     */
    @Test
    public void testCspHeader_withNonce_idpInitiatedFlowCompletesSuccessfully() throws Exception {

        testSAMLServer.reconfigServer("server_csp_withNonce.xml", _testName, null, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        IDP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.IDP_INITIATED_FLOW,
                helpers.setDefaultGoodSAMLIDPInitiatedExpectations(updatedTestSettings));
    }

    /**
     * Config attribute: contentSecurityPolicy (no %NONCE% placeholder)
     * Solicited SP-initiated flow completes successfully when a CSP header is
     * configured but does not contain the %NONCE% placeholder. No nonce is
     * emitted and the form-submit script is still present on the redirect page.
     *
     * @throws Exception
     */
    @Test
    public void testCspHeader_noNoncePlaceholder_flowCompletesSuccessfully() throws Exception {

        testSAMLServer.reconfigServer("server_csp_noNoncePlaceholder.xml", _testName, null, SAMLConstants.JUNIT_REPORTING);

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();

        solicited_SP_initiated_SAML(_testName, updatedTestSettings, SAMLConstants.SOLICITED_SP_INITIATED_FLOW,
                helpers.setDefaultGoodSAMLSolicitedSPInitiatedExpectations(updatedTestSettings));
    }
}
