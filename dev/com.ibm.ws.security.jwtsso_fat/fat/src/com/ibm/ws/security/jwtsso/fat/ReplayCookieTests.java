/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.apps.CommonFatApplications;
import com.ibm.ws.security.fat.common.apps.jwtbuilder.JwtBuilderServlet;
import com.ibm.ws.security.fat.common.apps.jwtbuilder.ProtectedServlet;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatActions;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;
import com.ibm.ws.security.jwtsso.fat.utils.MessageConstants;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ReplayCookieTests extends CommonSecurityFat {

    protected static Class<?> thisClass = ReplayCookieTests.class;

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private static final ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();

    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME = "fat.server.hostname";
    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTIP = "fat.server.hostip";

    private final JwtFatActions actions = new JwtFatActions();
    private final TestValidationUtils validationUtils = new TestValidationUtils();

    static final String DEFAULT_CONFIG = "server_withBuilderApp.xml";
    static final String APP_NAME_JWT_BUILDER = "jwtbuilder";

    String httpUrlBase = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    String httpsUrlBase = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort();
    String protectedUrl = httpUrlBase + JwtFatConstants.SIMPLE_SERVLET_PATH;
    String defaultUser = JwtFatConstants.TESTUSER;
    String defaultPassword = JwtFatConstants.TESTUSERPWD;

    @BeforeClass
    public static void setUp() throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());

        CommonFatApplications.buildAndDeployApp(server, APP_NAME_JWT_BUILDER, "com.ibm.ws.security.fat.common.apps.jwtbuilder.*");
        server.addInstalledAppForValidation(JwtFatConstants.APP_FORMLOGIN);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(DEFAULT_CONFIG);

    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Re-access the protected resource using the same web conversation (which will include the JWT SSO cookie)
     * Expects:
     * - Upon re-access, should reach the resource without having to log in
     */
    @Test
    public void test_reaccessResource_useSameWebConversation_includeJwtCookie() throws Exception {

        String user = JwtFatConstants.USER_1;
        String password = JwtFatConstants.USER_1_PWD;

        WebClient webClient = new WebClient();
        actions.logInAndObtainJwtCookie(_testName, webClient, protectedUrl, user, password);

        // Access the protected resource again using the same web conversation
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, user));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Delete the JWT SSO cookie from the web conversation
     * - Re-access the protected resource using the same web conversation
     * Expects:
     * - Upon re-access, should receive the login page because a cookie is not present
     */
    @Test
    public void test_reaccessResource_useSameWebConversation_deleteJwtCookie() throws Exception {

        WebClient webClient = new WebClient();
        actions.logInAndObtainJwtCookie(_testName, webClient, protectedUrl, defaultUser, defaultPassword);

        webClient.getCookieManager().clearCookies();

        // Access the protected resource again using the same web conversation
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Re-access the protected resource in a new web conversation without the JWT SSO cookie
     * Expects:
     * - Upon re-access, should receive the login page because a cookie is not present
     */
    @Test
    public void test_reaccessResource_newConversationWithoutJwtCookie() throws Exception {

        actions.logInAndObtainJwtCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        // Access the protected resource again, but without the JWT cookie
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Re-access the protected resource in a new web conversation, including the JWT SSO cookie that was just obtained
     * Expects:
     * - Upon re-access, should reach the resource without having to log in
     */
    @Test
    public void test_reaccessResource_newConversationWithValidJwtCookie() throws Exception {

        Cookie jwtCookie = actions.logInAndObtainJwtCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        // Access the protected again using a new conversation with the JWT SSO cookie included
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Create an identical JWT SSO cookie but with an empty signature segment
     * - Re-access the protected resource in a new web conversation, including the truncated JWT SSO cookie
     * Expects:
     * - Upon re-access, should receive the login page because the JWT cookie is not valid
     * - FFDCs should be emitted because the JWT signature is not valid
     */
    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtException", "com.ibm.websphere.security.jwt.InvalidTokenException",
                    "com.ibm.ws.security.authentication.AuthenticationException" })
    @Test
    public void test_reaccessResource_jwtCookieWithEmptySignature() throws Exception {

        Cookie jwtCookie = actions.logInAndObtainJwtCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        // Remove the cookie signature, but leave the trailing "." so the JWT still has three parts (an empty signature)
        String cookieValue = jwtCookie.getValue();
        String truncatedValue = cookieValue.substring(0, cookieValue.lastIndexOf(".") + 1);
        Cookie truncatedCookie = createIdenticalCookieWithNewValue(jwtCookie, truncatedValue);

        Log.info(thisClass, _testName, "Original cookie value  : " + cookieValue);
        Log.info(thisClass, _testName, "Truncated cookie value : " + truncatedCookie.getValue());

        // Access the protected again using a new conversation with the truncated JWT SSO cookie included
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5524E_ERROR_CREATING_JWT));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6031E_JWT_ERROR_PROCESSING_JWT + ".+"
                                                                                        + "Problem verifying signature"));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, truncatedCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Create an identical JWT SSO cookie but remove the signature segment
     * - Re-access the protected resource in a new web conversation, including the truncated JWT SSO cookie
     * Expects:
     * - Upon re-access, should receive the login page because the JWT cookie is not valid
     * - FFDCs should be emitted because the JWT is not formatted correctly
     */
    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtException", "com.ibm.websphere.security.jwt.InvalidTokenException",
                    "com.ibm.ws.security.authentication.AuthenticationException" })
    @Test
    public void test_reaccessResource_signatureRemovedFromJwtCookie() throws Exception {

        Cookie jwtCookie = actions.logInAndObtainJwtCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        // Remove the entire signature from the JWT, including the "." denoting the signature segment
        String cookieValue = jwtCookie.getValue();
        String truncatedValue = cookieValue.substring(0, cookieValue.lastIndexOf("."));
        Cookie cookieWithoutSignature = createIdenticalCookieWithNewValue(jwtCookie, truncatedValue);

        Log.info(thisClass, _testName, "Original cookie value  : " + cookieValue);
        Log.info(thisClass, _testName, "Truncated cookie value : " + cookieWithoutSignature.getValue());

        // Access the protected again using a new conversation with the truncated JWT SSO cookie included
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5524E_ERROR_CREATING_JWT));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6031E_JWT_ERROR_PROCESSING_JWT + ".+"
                                                                                        + "Invalid JOSE Compact Serialization"));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, cookieWithoutSignature);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Log into the protected resource WITHOUT the JWT SSO feature, obtaining an LTPA token/cookie
     * - Reconfigure the server to enable the JWT SSO feature
     * - useLtpaIfJwtAbsent is not set, meaning we should NOT fall back to use the LTPA token
     * - Re-access the protected resource with the LTPA cookie that was just obtained
     * Expects:
     * - Upon re-access, should receive the login page because the LTPA cookie should not be used for authentication
     */
    @Test
    public void test_obtainLtpa_reconfigureToUseJwtSso_reaccessResourceWithLtpaCookie() throws Exception {

        server.removeInstalledAppForValidation(APP_NAME_JWT_BUILDER);
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_noFeature.xml");

        Cookie ltpaCookie = actions.logInAndObtainLtpaCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        server.addInstalledAppForValidation(APP_NAME_JWT_BUILDER);
        server.reconfigureServerUsingExpandedConfiguration(_testName, DEFAULT_CONFIG);

        // Access the protected again using a new conversation with the LTPA cookie included
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, ltpaCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Log into the protected resource WITHOUT the JWT SSO feature, obtaining an LTPA token/cookie
     * - Reconfigure the server to enable the JWT SSO feature
     * - useLtpaIfJwtAbsent is set to true
     * - Re-access the protected resource with the LTPA cookie that was just obtained
     * Expects:
     * - Upon re-access, should reach the resource without having to log in
     * - User principal (as obtained from HttpServletRequest.getUserPrincipal()) should now be a JWT, not a WSPrincipal
     * - Subject should contain both the original WSPrincipal and a new JWT principal
     */
    @Test
    public void test_obtainLtpa_reconfigureToUseJwtSso_reaccessResourceWithLtpaCookie_useLtpaIfJwtAbsent() throws Exception {

        server.removeInstalledAppForValidation(APP_NAME_JWT_BUILDER);
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_noFeature.xml");

        Cookie ltpaCookie = actions.logInAndObtainLtpaCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_useLtpaIfJwtAbsent_true.xml");

        // Access the protected again using a new conversation with the LTPA cookie included
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, protectedUrl));
        expectations.addExpectations(CommonExpectations.responseTextIncludesCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.responseTextIncludesExpectedRemoteUser(currentAction, defaultUser));
        expectations.addExpectations(CommonExpectations.responseTextIncludesJwtPrincipal(currentAction));
        expectations.addExpectations(CommonExpectations.responseTextIncludesExpectedAccessId(currentAction, JwtFatConstants.BASIC_REALM, defaultUser));
        expectations.addExpectations(CommonExpectations.getJwtPrincipalExpectations(currentAction, defaultUser, JwtFatConstants.DEFAULT_ISS_REGEX));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, ltpaCookie);
        validationUtils.validateResult(response, currentAction, expectations);

        server.addInstalledAppForValidation(APP_NAME_JWT_BUILDER);
    }

    /**
     * Tests:
     * - Log into the protected resource with the JWT SSO feature, obtaining an JWT SSO cookie
     * - Reconfigure the server to disable the JWT SSO feature
     * - Re-access the protected resource with the JWT cookie that was just obtained
     * Expects:
     * - Upon re-access, should receive the login page
     */
    @Test
    public void test_obtainJwt_reconfigureToDisableJwtSso_reaccessResourceWithJwtCookie() throws Exception {

        Cookie jwtCookie = actions.logInAndObtainJwtCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        server.removeInstalledAppForValidation(APP_NAME_JWT_BUILDER);
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_noFeature.xml");

        // Access the protected again using a new conversation with the JWT SSO cookie included
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);

        server.addInstalledAppForValidation(APP_NAME_JWT_BUILDER);
    }

    /**
     * Tests:
     * - Configure mpJwt element with mapToUserRegistry=true
     * - Invoke a protected resource and log in, obtaining a JWT cookie
     * - Re-access the protected resource with the JWT cookie value that was just obtained, but included the JWT in the Authorization header
     * Expects:
     * - TODO
     */
    @Test
    public void test_obtainJwt_reaccessResourceWithJwtInHeader() throws Exception {
        server.removeInstalledAppForValidation(APP_NAME_JWT_BUILDER);
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_mpJwt_mapToUserRegistry_true.xml");

        Cookie jwtCookie = actions.logInAndObtainJwtCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        // Access the protected again using a new conversation with the JWT SSO cookie value included in the header
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrlWithBearerToken(_testName, actions.createWebClient(), protectedUrl, jwtCookie.getValue());
        validationUtils.validateResult(response, currentAction, expectations);

        server.addInstalledAppForValidation(APP_NAME_JWT_BUILDER);
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Access a different protected resource in a new web conversation without the JWT SSO cookie
     * Expects:
     * - Should receive a 401 because a cookie is not present
     */
    @Test
    public void test_obtainJwt_accessNewProtectedResource_withoutJwtCookie() throws Exception {

        actions.logInAndObtainJwtCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        // Access a different protected resource without the JWT cookie
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        String newProtectedUrl = httpUrlBase + JwtFatConstants.JWT_BUILDER_CONTEXT_ROOT + "/protected";

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(currentAction, HttpServletResponse.SC_UNAUTHORIZED));

        Page response = actions.invokeUrl(_testName, newProtectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Access a different protected resource in a new web conversation with the JWT SSO cookie included
     * Expects:
     * - Should reach the other resource without having to log in
     */
    @Test
    public void test_obtainJwt_accessNewProtectedResource_withJwtCookie() throws Exception {

        Cookie jwtCookie = actions.logInAndObtainJwtCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        // Access a different protected resource with the JWT cookie
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        String newProtectedUrl = httpUrlBase + JwtFatConstants.JWT_BUILDER_CONTEXT_ROOT + "/protected";

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(currentAction, HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseUrlExpectation(currentAction, JwtFatConstants.STRING_EQUALS, newProtectedUrl, "Did not reach the expected URL."));
        expectations.addExpectation(Expectation.createResponseExpectation(currentAction, String.format(ProtectedServlet.SUCCESS_MESSAGE, defaultUser),
                                                                          "Did not find the expected success message in the servlet response."));
        expectations.addExpectations(CommonExpectations.getJwtPrincipalExpectations(currentAction, defaultUser, JwtFatConstants.DEFAULT_ISS_REGEX));

        Page response = actions.invokeUrlWithCookie(_testName, newProtectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Manually build a JWT that's missing some required claims
     * - Access a protected resource with the JWT included
     * - Configured MP-JWT consumer trusts all issuers
     * Expects:
     * - Should receive the login page because the token does not contain all of the required claims
     */
    @ExpectedFFDC({ "com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException", "com.ibm.ws.security.authentication.AuthenticationException" })
    @Test
    public void test_buildJwt_missingClaims_accessProtectedResource() throws Exception {

        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_withBuilderApp_consumerTrustsAllIssuers.xml");

        String builderId = "builder_defaults";
        Cookie jwtCookie = buildThirdPartyJwtCookieUsingBuilderApp(builderId);

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5519E_PRINCIPAL_MAPPING_MISSING_ATTR + ".+" + "upn"));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5506E_USERNAME_NOT_FOUND));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5508E_ERROR_CREATING_RESULT));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Obtain a JWT from the token endpoint of a builder configured separately from JWT SSO
     * - Access a protected resource with the JWT included
     * - No explicit jwtSso configuration, so the default MP-JWT consumer will be used
     * Expects:
     * - Should receive the login page because the issuer in the token is not trusted
     */
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException",
                    "com.ibm.ws.security.authentication.AuthenticationException" })
    @Test
    public void test_buildJwt_accessProtectedResource_defaultMpJwtConsumer() throws Exception {

        String builderId = "builder_defaults";
        Cookie jwtCookie = buildThirdPartyJwtCookie(builderId);

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        String issuerRegex = "https://[^/]+/jwt/" + builderId;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6022E_JWT_ISSUER_NOT_TRUSTED + ".+" + issuerRegex));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6031E_JWT_ERROR_PROCESSING_JWT));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5524E_ERROR_CREATING_JWT));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Obtain a JWT from the token endpoint of a builder configured separately from JWT SSO
     * - Access a protected resource with the JWT included
     * - Configured MP-JWT consumer trusts all issuers
     * Expects:
     * - Should successfully reach the protected resource
     */
    @Test
    public void test_buildJwt_accessProtectedResource_issuerTrusted() throws Exception {

        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_withBuilderApp_consumerTrustsAllIssuers.xml");

        String builderId = "builder_defaults";
        Cookie jwtCookie = buildThirdPartyJwtCookie(builderId);

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        String expectedIssuer = "https://[^/]+/jwt/" + builderId;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser, expectedIssuer));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Obtain a JWT from the token endpoint of a builder configured to use a signing key in a different keystore than the server default
     * - Access a protected resource with the JWT included
     * Expects:
     * - Should receive the login page because the token signature cannot be validated
     */
    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException", "com.ibm.websphere.security.jwt.InvalidTokenException",
                    "com.ibm.ws.security.authentication.AuthenticationException" })
    @Test
    public void test_buildJwt_signedWithNonDefaultKey_accessProtectedResource() throws Exception {

        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_withBuilderApp_consumerTrustsAllIssuers.xml");

        String builderId = "builder_signWithUniqueKey";
        Cookie jwtCookie = buildThirdPartyJwtCookie(builderId);

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6041E_JWT_INVALID_SIGNATURE));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6031E_JWT_ERROR_PROCESSING_JWT));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5524E_ERROR_CREATING_JWT));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /********************************************** Helper methods **********************************************/

    private Cookie createIdenticalCookieWithNewValue(Cookie cookieToDuplicate, String newCookieValue) {
        return new Cookie(cookieToDuplicate.getDomain(), cookieToDuplicate.getName(), newCookieValue, cookieToDuplicate.getPath(), cookieToDuplicate.getExpires(), cookieToDuplicate
                        .isSecure(), cookieToDuplicate.isHttpOnly());
    }

    /**
     * Invokes the jwtbuilder web application configured in the server that's used for building JWTs outside of the context of
     * the JWT SSO feature. A JWT is built using the jwtBuilder configuration with the provided ID and returned as a cookie in
     * the response.
     */
    private Cookie buildThirdPartyJwtCookieUsingBuilderApp(String builderId) throws Exception {
        String jwtBuilderUrl = httpUrlBase + JwtFatConstants.JWT_BUILDER_CONTEXT_ROOT + "/build";

        List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
        requestParams.add(new NameValuePair(JwtBuilderServlet.PARAM_BUILDER_ID, builderId));

        WebClient webClient = new WebClient();
        Page response = actions.invokeUrlWithParameters(_testName, webClient, jwtBuilderUrl, requestParams);
        Log.info(thisClass, _testName, "JWT builder app response: " + WebResponseUtils.getResponseText(response));

        Cookie jwtCookie = webClient.getCookieManager().getCookie(JwtFatConstants.JWT_COOKIE_NAME);
        Log.info(thisClass, _testName, "Built JWT cookie: " + jwtCookie);

        return jwtCookie;
    }

    /**
     * Invokes the {@code /token} endpoint of the JWT builder with the provided ID in order to obtain a JWT. The JWT returned from
     * the {@code /token} endpoint will be used as the value for a new JWT SSO cookie that is created and returned by this method.
     */
    private Cookie buildThirdPartyJwtCookie(String builderId) throws Exception {
        String jwtString = getJwtFromTokenEndpoint(builderId);
        Log.info(thisClass, _testName, "Received JWT string : " + jwtString);

        Cookie jwtCookie = new Cookie("*", JwtFatConstants.JWT_COOKIE_NAME, jwtString);
        Log.info(thisClass, _testName, "Built JWT cookie: " + jwtCookie);

        return jwtCookie;
    }

    private String getJwtFromTokenEndpoint(String builderId) throws MalformedURLException, Exception {
        WebRequest request = buildJwtTokenEndpointRequest(builderId);

        WebClient wc = new WebClient();
        wc.getOptions().setUseInsecureSSL(true);

        Page response = actions.submitRequest(_testName, wc, request);
        Log.info(thisClass, _testName, "Response: " + WebResponseUtils.getResponseText(response));

        return extractJwtFromTokenEndpointResponse(response);
    }

    private WebRequest buildJwtTokenEndpointRequest(String builderId) throws MalformedURLException {
        String jwtTokenEndpoint = "/jwt/ibm/api/" + "%s" + "/token";
        String jwtBuilderUrl = httpsUrlBase + String.format(jwtTokenEndpoint, builderId);

        WebRequest request = new WebRequest(new URL(jwtBuilderUrl));
        // Token endpoint requires authentication, so provide credentials
        request.setAdditionalHeader("Authorization", "Basic " + Base64Coder.base64Encode(defaultUser + ":" + defaultPassword));
        return request;
    }

    /**
     * JWT /token endpoint should return a JSON object whose only key, "token", stores the JWT built by the builder.
     */
    private String extractJwtFromTokenEndpointResponse(Page response) throws Exception {
        JsonReader reader = Json.createReader(new StringReader(WebResponseUtils.getResponseText(response)));
        JsonObject jsonResponse = reader.readObject();
        return jsonResponse.getString("token");
    }

}
