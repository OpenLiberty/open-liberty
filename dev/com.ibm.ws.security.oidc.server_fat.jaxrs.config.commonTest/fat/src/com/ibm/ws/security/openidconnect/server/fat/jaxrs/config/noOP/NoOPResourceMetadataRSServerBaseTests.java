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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

/**
 * Abstract base class for RFC 9728 Protected Resource Metadata tests on the Resource Server.
 *
 * <h3>Background</h3>
 * <p>Per the sequence diagram in the feature specification, when a request arrives at the
 * RS without a Bearer token and {@code inboundPropagation="required"} is configured:
 * <ol>
 *   <li>The RS rejects the request locally — <strong>no call is made to the Authorization
 *       Server</strong>.</li>
 *   <li>The RS responds with {@code HTTP 401 Unauthorized}.</li>
 *   <li>The {@code WWW-Authenticate} response header carries:
 *       {@code Bearer realm="oauth"[, resource_metadata="https://…/.well-known/oauth-protected-resource"]}
 *       where the {@code resource_metadata} parameter is only present when the
 *       {@code <protectedResourceMetadata>} sub-element is configured</li>
 * </ol>
 *
 *
 * <p>These tests belong in the {@code noOP} package because the 401-without-token scenario
 * does not require any Authorization Server communication — the RS acts independently.
 * The OP server entry is created with {@code skipServerStart=true} so the test infrastructure
 * performs its standard bookkeeping without actually launching an AS process.
 *
 * <h3>Subclassing contract</h3>
 * <p>Each concrete subclass owns its own {@code @Test} methods (named to include the server
 * variant) and delegates execution to the protected helpers defined here.  This keeps test
 * names unique and unambiguous in reports while all shared logic stays in one place.
 *
 * <p>Concrete subclasses: {@link NoOPResourceMetadataRSServerTest},
 * {@link NoOPNoResourceMetadataRSServerTests}.
 */
public abstract class NoOPResourceMetadataRSServerBaseTests extends CommonTest {

    private static final Class<?> thisClass = NoOPResourceMetadataRSServerBaseTests.class;

    /**
     * Nominal "OP" server name – only used so that {@code commonSetUp} can complete its
     * port-resolution setup; {@code skipServerStart=true} prevents the process from
     * actually launching.
     */
    protected static final String OPServerName =
            "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";

    /** Standard RS server. */
    protected static final String RSServerName =
            "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";

    protected static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    
    protected static String targetProvider = null;
    protected static String flowType = null;

    /**
     * Shared setup called by all subclasses.
     *
     * @param rsServerName  Liberty server name for the RS
     * @param rsConfigFile  RS config file inside {@code publish/servers/<rsServerName>/configs/}
     */
    protected static void commonSetupBeforeTest(String rsServerName, String rsConfigFile) throws Exception {

        msgUtils.printClassName(NoOPResourceMetadataRSServerBaseTests.class.toString());
        Log.info(NoOPResourceMetadataRSServerBaseTests.class, "commonSetupBeforeTest",
                "Prep for test - RS server: " + rsServerName + ", config: " + rsConfigFile);

        // OP side – we only need the minimal infrastructure, not a running server
        List<String> extraMsgsOP = new ArrayList<String>();
        List<String> extraAppsOP = new ArrayList<String>();
        TestServer.addTestApp(null, extraMsgsOP, Constants.OP_SAMPLE_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraAppsOP, null, Constants.OP_CLIENT_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraAppsOP, extraMsgsOP, Constants.OP_TAI_APP, Constants.OIDC_OP);

        // RS side
        List<String> extraMsgsRS = new ArrayList<String>();
        List<String> extraAppsRS = new ArrayList<String>();
        extraAppsRS.add(Constants.HELLOWORLD_SERVLET);

        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        testSettings = new TestSettings();

        /*
         * skipServerStart=true prevents the OP from actually starting.  The flag is reset
         * to false automatically inside commonSetUp so the next call (for the RS) proceeds
         * normally.
         */
        skipServerStart = true;
        testOPServer = commonSetUp(OPServerName, "server_orig.xml", Constants.OIDC_OP,
                extraAppsOP, Constants.DO_NOT_USE_DERBY, extraMsgsOP,
                null, Constants.OIDC_OP, true, true, tokenType, certType);
        SecurityFatHttpUtils.saveServerPorts(testOPServer.getServer(), Constants.BVT_SERVER_1_PORT_NAME_ROOT);

        genericTestServer = commonSetUp(rsServerName, rsConfigFile, Constants.GENERIC_SERVER,
                extraAppsRS, Constants.DO_NOT_USE_DERBY, extraMsgsRS,
                null, Constants.OIDC_OP, true, true, tokenType, certType);
        SecurityFatHttpUtils.saveServerPorts(genericTestServer.getServer(), Constants.BVT_SERVER_3_PORT_NAME_ROOT);

        // The ${oAuthOidcRSValidationType} variable insert is flagged as invalid by the config
        // evaluator – suppress the resulting warning so it does not fail the test run.
        genericTestServer.addIgnoredServerException(
                MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "validationMethod");

        targetProvider = Constants.OIDCCONFIGSAMPLE_APP;
        flowType = Constants.WEB_CLIENT_FLOW;

        testSettings.setRSProtectedResource(
                genericTestServer.getHttpsString() + Constants.HELLOWORLD_PROTECTED_RESOURCE);
    }

    /**
     * Shared logic for the scenario that no token results to 401 + WWW-Authenticate".
     *
     * <p>Per RFC 6750 (3.1) the RS MUST respond with {@code HTTP 401} and a
     * {@code WWW-Authenticate: Bearer} header when no token is presented.  The RS
     * makes the rejection decision <em>locally</em>; {@code CWWKS1726E} in the RS log
     * confirms no round-trip to the OP was made.  The concrete subclass adds the
     * {@code resource_metadata} expectation appropriate to its configuration via
     * {@link #addResourceMetadataExpectations(List)}.
     */
    protected void doNoToken401Test(String testName) throws Exception {
        Log.info(thisClass, testName,
                "Starting test - RS should return 401 with WWW-Authenticate, no OP communication");

        TestSettings updatedTestSettings =
                rsTools.updateRSProtectedResource(testSettings, "helloworld_resourceMetadata");

        // RFC 6750 §3.1: 401 when no token is presented with inboundPropagation=required
        List<validationData> expectations =
                validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);

        // RFC 6750 §3: WWW-Authenticate header must be present and use the Bearer auth-scheme
        String expectedAuthHeader = WWW_AUTHENTICATE_HEADER.toUpperCase() + ": " + Constants.BEARER;
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                "Response did not include a WWW-Authenticate header with Bearer auth-scheme.",
                null, expectedAuthHeader);

        // RFC 9728: resource_metadata presence/absence depends on config (supplied by subclass)
        addResourceMetadataExpectations(expectations);

        Log.info(thisClass, testName, "Invoking RS protected resource without any Bearer token");
        // Pass null as the token so no Authorization header is added to the request
        helpers.invokeRSProtectedResource(_testName, Constants.POSTMETHOD, null, null,
                updatedTestSettings, expectations);

        Log.info(thisClass, testName,
                "Test completed - verified 401 with WWW-Authenticate and no OP communication");
    }

    /**
     * Shared logic for the "malformed token that results to 401 + WWW-Authenticate" scenario.
     *
     * <p>When a syntactically invalid Bearer token (e.g. {@code xxx.yyy.zzz}) is presented,
     * the RS cannot validate it and responds with {@code HTTP 401 Unauthorized}, just as it
     * does when no token is supplied.  RFC 9728 does not explicitly address this case, but
     * the RS SHOULD include the {@code resource_metadata} parameter in the challenge when
     * {@code <protectedResourceMetadata>} is configured (to help the caller obtain a valid
     * token).  The concrete subclass confirms presence or absence of the parameter via
     * {@link #addResourceMetadataExpectations(List)}.
     *
     * <p>The RS server is configured with {@code validationMethod="introspect"}, so it
     * forwards the token to the OP's introspect endpoint regardless of whether it looks
     * like a JWT.  Because the OP is not started in these noOP tests, the connection is
     * refused and the RS logs {@code CWWKS1727E}.  
     * We are allowing this FFDC so the test does not fail.
     */
    protected void doMalformedToken401Test(String testName) throws Exception {
        Log.info(thisClass, testName,
                "Starting test - RS should return 401 with WWW-Authenticate for a malformed token");

        TestSettings updatedTestSettings =
                rsTools.updateRSProtectedResource(testSettings, "helloworld_resourceMetadata");

        // RFC 6750 [3.1]: 401 when the Bearer token is invalid
        List<validationData> expectations =
                validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);

        // RFC 6750 [3]: WWW-Authenticate header must be present and use the Bearer auth-scheme
        String expectedAuthHeader = WWW_AUTHENTICATE_HEADER.toUpperCase() + ": " + Constants.BEARER;
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                "Response did not include a WWW-Authenticate header with Bearer auth-scheme.",
                null, expectedAuthHeader);

        // The RS is configured with validationMethod="introspect", so it attempts a remote call to
        // the OP's introspect endpoint to validate the token. Since the OP is not running in these
        // noOP tests, the call fails with a connection error and the RS logs CWWKS1727E.
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations,
                Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG,
                Constants.STRING_CONTAINS,
                "Did not find CWWKS1727E error-validating-access-token message in RS logs "
                        + "(expected when the RS fails to reach the OP introspect endpoint).",
                MessageConstants.CWWKS1727E_ERROR_VALIDATING_ACCESS_TOKEN);
        
        // RFC 9728: resource_metadata presence/absence depends on config (supplied by subclass)
        addResourceMetadataExpectations(expectations);

        // Use a syntactically JWT-like but semantically invalid token
        String malformedToken = "xxx.yyy.zzz";
        Log.info(thisClass, testName,
                "Invoking RS protected resource with malformed Bearer token: " + malformedToken);
        helpers.invokeRsProtectedResource(_testName, null, malformedToken,
                updatedTestSettings, expectations);

        Log.info(thisClass, testName,
                "Test completed - verified 401 with WWW-Authenticate for malformed token");
    }
    
    /**
     * Appends {@code resource_metadata} expectations that are specific to the subclass.
     *
     * @param expectations mutable list of expectations to augment
     */
    protected abstract void addResourceMetadataExpectations(List<validationData> expectations) throws Exception;
}
