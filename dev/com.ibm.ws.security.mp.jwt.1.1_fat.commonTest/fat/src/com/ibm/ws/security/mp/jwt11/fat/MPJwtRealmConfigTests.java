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
package com.ibm.ws.security.mp.jwt11.fat;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.security.fat.common.jwt.JwtConstants;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt11MPConfigTests;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT tests for the mpJwt realmName and realmIdentifier configuration attributes.
 *
 * Resolution order:
 * 1. realmName (static override) wins if set.
 * 2. Claim named by realmIdentifier (default "realm") provides the realm.
 * 3. iss claim is the fallback.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtRealmConfigTests extends MPJwt11MPConfigTests {

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat")
    public static LibertyServer resourceServer;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat.builder")
    public static LibertyServer jwtBuilderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private final TestValidationUtils validationUtils = new TestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {
        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml", false);
        setUpAndStartRSServerForApiTests(resourceServer, jwtBuilderServer, "rs_server_orig_withAudience.xml", false);
        skipRestoreServerTracker.addServer(resourceServer);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Extract the issuer string from a built token, stripping any trailing slash.
     */
    private String getIssuerFromToken(JwtTokenForTest jwtTokenTools) {
        String issuer = jwtTokenTools.getJsonPayload().getString("iss");
        if (issuer != null && issuer.endsWith("/")) {
            issuer = issuer.substring(0, issuer.length() - 1);
        }
        return issuer;
    }

    /**
     * Invoke all test apps with the given bearer token and validate against the supplied expectations.
     */
    private void invokeAppsAndValidate(String builtToken, Expectations expectations) throws Exception {
        WebClient webClient = actions.createWebClient();
        try {
            for (TestApps app : setTestAppArray(resourceServer)) {
                Page response = actions.invokeUrlWithBearerToken(_testName, webClient, app.getUrl(), builtToken);
                validationUtils.validateResult(response, expectations);
            }
        } finally {
            actions.destroyWebClient(webClient);
        }
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * Default config: no realmIdentifier or realmName override.
     * The default realmIdentifier is "realm". The token has no "realm" claim.
     * Expect realm to fall back to the iss claim value.
     */
    @Test
    public void MPJwtRealmConfig_default_noRealmClaim_fallsBackToIss() throws Exception {
        // Use the base config — no realm-specific attributes set
        resourceServer.restoreServerConfigurationAndWaitForApps();

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer);
        JwtTokenForTest jwtTokenTools = new JwtTokenForTest(builtToken);
        String expectedRealm = getIssuerFromToken(jwtTokenTools);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseFullExpectation(MPJwt11FatConstants.STRING_CONTAINS,
                "com.ibm.wsspi.security.cred.uniqueId=user:" + expectedRealm + "/",
                "Response did NOT contain uniqueId with realm equal to the iss value [" + expectedRealm + "]"));

        invokeAppsAndValidate(builtToken, expectations);
    }

    /**
     * realmIdentifier="realm" (the default) and the token contains a "realm" claim.
     * Expect the realm claim value to be used as the realm.
     */
    @Test
    public void MPJwtRealmConfig_realmIdentifier_default_realmClaimPresent() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_realmIdentifier_realmClaim.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, MPJwt11FatConstants.TESTUSER));
        extraClaims.add(new NameValuePair("realm", "TokenRealm"));
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT_withAudience", extraClaims);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseFullExpectation(MPJwt11FatConstants.STRING_CONTAINS,
                "com.ibm.wsspi.security.cred.uniqueId=user:TokenRealm/",
                "Response did NOT contain uniqueId with realm equal to the 'realm' claim value 'TokenRealm'"));

        invokeAppsAndValidate(builtToken, expectations);
    }

    /**
     * realmIdentifier="tenant" and the token contains a "tenant" claim.
     * Expect the tenant claim value to be used as the realm.
     */
    @Test
    public void MPJwtRealmConfig_realmIdentifier_customClaim_present() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_realmIdentifier_tenantClaim.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, MPJwt11FatConstants.TESTUSER));
        extraClaims.add(new NameValuePair("tenant", "TenantA"));
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT_withAudience", extraClaims);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseFullExpectation(MPJwt11FatConstants.STRING_CONTAINS,
                "com.ibm.wsspi.security.cred.uniqueId=user:TenantA/",
                "Response did NOT contain uniqueId with realm equal to the 'tenant' claim value 'TenantA'"));

        invokeAppsAndValidate(builtToken, expectations);
    }

    /**
     * realmIdentifier="tenant" but the token does NOT contain a "tenant" claim.
     * Expect realm to fall back to the iss claim.
     */
    @Test
    public void MPJwtRealmConfig_realmIdentifier_customClaim_absent_fallsBackToIss() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_realmIdentifier_tenantClaim.xml");

        // No "tenant" extra claim — token will not have it
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT_withAudience");
        JwtTokenForTest jwtTokenTools = new JwtTokenForTest(builtToken);
        String expectedRealm = getIssuerFromToken(jwtTokenTools);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseFullExpectation(MPJwt11FatConstants.STRING_CONTAINS,
                "com.ibm.wsspi.security.cred.uniqueId=user:" + expectedRealm + "/",
                "Response did NOT contain uniqueId with realm equal to iss when 'tenant' claim is absent"));

        invokeAppsAndValidate(builtToken, expectations);
    }

    /**
     * realmName="CorporateRealm" set (no realmIdentifier override).
     * Expect the static realm name to be used regardless of token claims.
     */
    @Test
    public void MPJwtRealmConfig_realmName_static_overridesToken() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_realmName_static.xml");

        // Add a "realm" claim in the token — realmName should win over it
        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, MPJwt11FatConstants.TESTUSER));
        extraClaims.add(new NameValuePair("realm", "TokenRealm"));
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT_withAudience", extraClaims);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseFullExpectation(MPJwt11FatConstants.STRING_CONTAINS,
                "com.ibm.wsspi.security.cred.uniqueId=user:CorporateRealm/",
                "Response did NOT contain uniqueId with static realm 'CorporateRealm' (realmName should override token claim)"));

        invokeAppsAndValidate(builtToken, expectations);
    }

    /**
     * Both realmName="StaticRealm" and realmIdentifier="tenant" are set.
     * The token contains a "tenant" claim.
     * Expect realmName to take precedence — "StaticRealm" is used, not "TenantA".
     */
    @Test
    public void MPJwtRealmConfig_realmName_overrides_realmIdentifier() throws Exception {
        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_realmName_overrides_realmIdentifier.xml");

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, MPJwt11FatConstants.TESTUSER));
        extraClaims.add(new NameValuePair("tenant", "TenantA"));
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT_withAudience", extraClaims);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseFullExpectation(MPJwt11FatConstants.STRING_CONTAINS,
                "com.ibm.wsspi.security.cred.uniqueId=user:StaticRealm/",
                "Response did NOT contain uniqueId with 'StaticRealm' — realmName should override realmIdentifier"));

        invokeAppsAndValidate(builtToken, expectations);
    }
}
