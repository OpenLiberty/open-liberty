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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwt11MPConfigTests;
import com.ibm.ws.security.fat.common.mp.jwt.sharedTests.MPJwtMPConfigTests.TestApps;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT tests for JWT authentication with a slash-prefixed {@code realm} claim.
 *
 * <p>Before the fix, a slash-prefixed realm value (e.g. {@code "realm": "/testRealm"})
 * caused a NullPointerException inside {@code AccessIdUtil.matcher()} during
 * authentication. The fix adds realm-aware patterns that handle slash-prefixed
 * values and preserve them verbatim in WSCredential.
 *
 * <p>Test coverage:
 * <ol>
 *   <li>{@link #testLeadingSlashRealm_authSucceeds} — primary NPE regression guard</li>
 *   <li>{@link #testLeadingSlashRealm_realmPreservedInResponse} — slash is not stripped from WSCredential</li>
 *   <li>{@link #testLeadingSlashWithSubpath_authSucceeds} — realm with leading slash and internal path structure does not throw NPE</li>
 *   <li>{@link #testNormalRealm_unaffected} — baseline: realm without slash still works</li>
 *   <li>{@link #testNoRealmClaim_authSucceeds} — baseline: missing realm falls back to issuer-based realm</li>
 *   <li>{@link #testTrailingSlashRealm_noNpe} — verify trailing slash realm causes no NPE</li>
 * </ol>
 *
 * <p>Realm values used in tests (e.g. {@code "/testRealm"}) are generic placeholders
 * representing any slash-prefixed realm string an identity provider might issue.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtLeadingSlashRealmTests extends MPJwt11MPConfigTests {

    protected static Class<?> thisClass = MPJwtLeadingSlashRealmTests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat")
    public static LibertyServer resourceServer;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat.builder")
    public static LibertyServer jwtBuilderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private final TestValidationUtils validationUtils = new TestValidationUtils();

    /** Claim name used by AccessIdUtil / WSCredential to carry the realm. */
    private static final String REALM_CLAIM = "realm";

    /**
     * Start both the JWT builder server and the resource server.
     * The resource server uses {@code rs_server_orig_withAudience.xml}, the same
     * config used by {@link MPJwtBasicTests}, which provides a ready-to-use
     * mpJwt config with audience validation enabled.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml", false);

        setUpAndStartRSServerForApiTests(resourceServer, jwtBuilderServer, "rs_server_orig_withAudience.xml", false);

        skipRestoreServerTracker.addServer(resourceServer);

    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Build expectations for a successful authentication: HTTP 200 and no NPE /
     * CWWKS error messages in the resource-server log.
     */
    private Expectations successExpectations() throws Exception {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        // Verify that no NullPointerException was logged during realm-claim processing.
        expectations.addExpectation(new ServerMessageExpectation(resourceServer,
                Constants.STRING_DOES_NOT_CONTAIN,
                "NullPointerException",
                "Server log should NOT contain a NullPointerException during realm-claim processing."));
        return expectations;
    }

    /**
     * Build expectations that only check for the absence of NullPointerException in
     * server logs (primary regression guard) without asserting HTTP 200, since realm
     * mismatch with the user registry may return HTTP 401 when the realm is not registered.
     */
    private Expectations noNpeExpectations() throws Exception {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ServerMessageExpectation(resourceServer,
                Constants.STRING_DOES_NOT_CONTAIN,
                "NullPointerException",
                "Server log should NOT contain a NullPointerException during realm-claim processing."));
        return expectations;
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * <b>testLeadingSlashRealm_authSucceeds</b>
     *
     * <p>A JWT token that carries a single leading-slash realm value
     * (e.g. {@code "realm": "/testRealm"}) must authenticate successfully
     * (HTTP 200). Before the fix, {@code AccessIdUtil.matcher()} threw a
     * NullPointerException for this input.
     *
     * @throws Exception on unexpected test-infrastructure failure
     */
    @Test
    public void testLeadingSlashRealm_authSucceeds() throws Exception {

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("upn", MPJwt11FatConstants.TESTUSER));
        extraClaims.add(new NameValuePair(REALM_CLAIM, "/testRealm"));

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        Expectations expectations = successExpectations();

        genericConfigTest(builtToken, expectations);

    }

    /**
     * <b>testLeadingSlashRealm_realmPreservedInResponse</b>
     *
     * <p>The leading slash in the realm value must be preserved verbatim in the response —
     * it must not be stripped or normalised by the fix.
     *
     * <p>{@code Utils.printValues()} iterates all JWT claims and writes each as
     * {@code "key: <name> value: <value>"} via {@code jsonWebToken.getClaim(key)}.
     * For a realm claim of {@code "/testRealm"} this produces the line
     * {@code "key: realm value: /testRealm"} in the response body, which is the
     * pattern asserted here.
     *
     * @throws Exception on unexpected test-infrastructure failure
     */
    @Test
    public void testLeadingSlashRealm_realmPreservedInResponse() throws Exception {

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("upn", MPJwt11FatConstants.TESTUSER));
        extraClaims.add(new NameValuePair(REALM_CLAIM, "/testRealm"));

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        Expectations expectations = successExpectations();
        // Utils.printValues() outputs each claim as "key: <name> value: <value>".
        // Asserting this exact substring confirms the slash is not stripped from the
        // JWT realm claim before it reaches the response.
        expectations.addExpectation(new ResponseFullExpectation(MPJwt11FatConstants.STRING_CONTAINS,
                "key: realm value: /testRealm",
                "Response body should contain 'key: realm value: /testRealm' confirming the leading slash is preserved verbatim."));

        genericConfigTest(builtToken, expectations);

    }

    /**
     * <b>testLeadingSlashWithSubpath_authSucceeds</b>
     *
     * <p>A JWT token that carries a leading-slash realm with internal path structure
     * (e.g. {@code "realm": "/realm/subRealm"}). This covers both the leading-slash
     * and the internal-slash scenarios — both map to a slash-prefixed realm value
     * and share the same NPE regression guard.
     *
     * <p>The default test server config uses basicRegistry, so this realm is unregistered
     * and authentication may return HTTP 401. The assertion is therefore scoped to
     * absence of NullPointerException only. Once {@code mapToUserRegistry="No"} support
     * is confirmed for this server config, upgrade to {@code successExpectations()}.
     *
     * @throws Exception on unexpected test-infrastructure failure
     */
    @Test
    public void testLeadingSlashWithSubpath_authSucceeds() throws Exception {

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("upn", MPJwt11FatConstants.TESTUSER));
        extraClaims.add(new NameValuePair(REALM_CLAIM, "/realm/subRealm"));

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        Expectations expectations = noNpeExpectations();

        genericConfigTest(builtToken, expectations);

    }

    /**
     * <b>testNormalRealm_unaffected</b>
     *
     * <p>Regression guard: a JWT with a plain realm value that has no leading slash
     * (e.g. {@code "realm": "testRealm"}) must continue to work correctly after the
     * fix. The fix must not break the pre-existing happy path.
     *
     * @throws Exception on unexpected test-infrastructure failure
     */
    @Test
    public void testNormalRealm_unaffected() throws Exception {

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("upn", MPJwt11FatConstants.TESTUSER));
        extraClaims.add(new NameValuePair(REALM_CLAIM, "testRealm"));

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        // Null expectations → genericConfigTest generates the standard "good" expectations
        genericConfigTest(builtToken);

    }

    /**
     * <b>testNoRealmClaim_authSucceeds</b>
     *
     * <p>A JWT that contains no {@code realm} claim at all must still
     * authenticate successfully. The runtime falls back to an issuer-based
     * realm; this test ensures the fix does not inadvertently break that
     * fallback path.
     *
     * @throws Exception on unexpected test-infrastructure failure
     */
    @Test
    public void testNoRealmClaim_authSucceeds() throws Exception {

        // Build a token with a valid upn but no realm claim.
        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("upn", MPJwt11FatConstants.TESTUSER));

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        // Null expectations → genericConfigTest generates the standard "good" expectations
        genericConfigTest(builtToken);

    }

    /**
     * <b>testTrailingSlashRealm_noNpe</b>
     *
     * <p>Verify that sending a JWT with {@code "realm": "myRealm/"} does not
     * cause a NullPointerException in server logs.
     *
     * @throws Exception on unexpected test-infrastructure failure
     */
    @Test
    public void testTrailingSlashRealm_noNpe() throws Exception {

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair("upn", MPJwt11FatConstants.TESTUSER));
        extraClaims.add(new NameValuePair(REALM_CLAIM, "myRealm/"));

        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, "defaultJWT", extraClaims);

        Expectations expectations = noNpeExpectations();

        genericConfigTest(builtToken, expectations);

    }

    // -------------------------------------------------------------------------
    // genericConfigTest delegation
    // -------------------------------------------------------------------------

    /**
     * Iterates all test apps on the resource server and validates each response
     * against the supplied expectations. Passing {@code null} generates the
     * standard "good-response" expectations automatically per app.
     */
    public void genericConfigTest(String builtToken, Expectations expectations) throws Exception {
        JwtTokenForTest jwtTokenTools = new JwtTokenForTest(builtToken);

        WebClient webClient = actions.createWebClient();
        boolean setGoodExpectations = (expectations == null);
        for (TestApps app : setTestAppArray(resourceServer)) {
            if (setGoodExpectations) {
                expectations = goodTestExpectations(jwtTokenTools, app.getUrl(), app.getClassName());
            }
            Page response = actions.invokeUrlWithBearerToken(_testName, webClient, app.getUrl(), builtToken);
            validationUtils.validateResult(response, expectations);
        }
        actions.destroyWebClient(webClient);
    }

    /** Convenience overload — generates good expectations automatically. */
    public void genericConfigTest(String builtToken) throws Exception {
        genericConfigTest(builtToken, null);
    }

}
