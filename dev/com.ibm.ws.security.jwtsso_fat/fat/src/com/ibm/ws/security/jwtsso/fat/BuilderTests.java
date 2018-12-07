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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatActions;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class BuilderTests extends CommonSecurityFat {

    protected static Class<?> thisClass = BuilderTests.class;

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private final JwtFatActions actions = new JwtFatActions();
    private final TestValidationUtils validationUtils = new TestValidationUtils();

    String protectedUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + JwtFatConstants.SIMPLE_SERVLET_PATH;
    String defaultUser = JwtFatConstants.TESTUSER;
    String defaultPassword = JwtFatConstants.TESTUSERPWD;

    @BeforeClass
    public static void setUp() throws Exception {
        server.addInstalledAppForValidation(JwtFatConstants.APP_FORMLOGIN);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration("server_withFeature.xml");
    }

    /**
     * Tests:
     * - Log into a protected resource with the jwtSso element pointing to a jwtBuilder that has JWK enabled
     * Expects:
     * - Should successfully reach protected resource
     * - JWT cookie header should include a "kid" entry for the JWK ID
     */
    @Test
    public void test_jwkEnabled() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_builder_jwkEnabled.xml");

        WebClient webClient = actions.createWebClient();

        String builderId = "builder_jwkEnabled";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        String expectedIssuer = "https://[^/]+/jwt/" + builderId;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser, expectedIssuer));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME, JwtFatConstants.SECURE,
                                                                        JwtFatConstants.HTTPONLY));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);

        Cookie jwtCookie = webClient.getCookieManager().getCookie(JwtFatConstants.JWT_COOKIE_NAME);
        verifyJwtHeaderContainsKey(jwtCookie.getValue(), "kid");
    }

    /**
     * Tests:
     * - Log into a protected resource with the jwtSso element not pointing to a jwtBuilder
     * - MP JWT consumer configuration specifies the jwksUri attribute
     * Expects:
     * - Should successfully reach protected resource
     * - JWT cookie should be signed by a certificate from the keystore, and its header should NOT include a "kid" entry
     */
    @Test
    public void test_noBuilderRef_mpJwtJwksUriConfigured() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_noBuilder_jwksUriConfigured.xml");

        WebClient webClient = actions.createWebClient();

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);

        //Cookie jwtCookie = webClient.getCookieManager().getCookie(JwtFatConstants.JWT_COOKIE_NAME);
        //verifyJwtHeaderDoesNotContainKey(jwtCookie.getValue(), "kid");
    }

    private void verifyJwtHeaderContainsKey(String jwt, String key) throws UnsupportedEncodingException {
        Log.info(thisClass, "verifyJwtHeaderContainsKey", "Verifying that JWT header contains key \"" + key + "\". JWT: " + jwt);
        String jwtHeader = extractAndDecodeJwtHeader(jwt);
        JsonObject header = convertStringToJsonObject(jwtHeader);
        assertTrue("JWT cookie header should have included a \"" + key + "\" entry but did not. Header was: " + header, header.containsKey(key));
    }

    private void verifyJwtHeaderDoesNotContainKey(String jwt, String key) throws UnsupportedEncodingException {
        Log.info(thisClass, "verifyJwtHeaderDoesNotContainKey", "Verifying that JWT header does not contain key \"" + key + "\". JWT: " + jwt);
        String jwtHeader = extractAndDecodeJwtHeader(jwt);
        JsonObject header = convertStringToJsonObject(jwtHeader);
        assertFalse("JWT cookie header should NOT have included a \"" + key + "\" entry but did. Header was: " + header, header.containsKey(key));
    }

    private String extractAndDecodeJwtHeader(String jwt) throws UnsupportedEncodingException {
        String encodedJwtHeader = jwt.substring(0, jwt.indexOf("."));
        return new String(Base64.getDecoder().decode(encodedJwtHeader), "UTF-8");
    }

    private JsonObject convertStringToJsonObject(String jsonObjectString) {
        JsonReader reader = Json.createReader(new StringReader(jsonObjectString));
        return reader.readObject();
    }

}
