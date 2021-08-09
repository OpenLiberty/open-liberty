/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.test.speccompliance;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.OAuthConstants;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.test.base.BaseConfig;
import com.ibm.oauth.core.test.base.BaseTestCase;
import com.ibm.oauth.core.test.base.MockServletRequest;
import com.ibm.oauth.core.test.base.MockServletResponse;

/*
 * Test case flows reused from Shindig's OAuth provider to validate spec compliance
 */

public class OAuth2AuthCodeFlowTest extends BaseTestCase {

    protected static final String SIMPLE_ACCESS_TOKEN = "TEST_TOKEN";
    protected static final String PUBLIC_CLIENT_ID = "key";
    protected static final String TESTUSER = "testuser";
    protected static final String CONF_CLIENT_ID = "key";
    protected static final String CONF_CLIENT_SECRET = "secret";
    protected static final String RESPONSE_TYPE_CODE = "code";
    protected static final String GRANT_TYPE_AZN_CODE = "authorization_code";
    protected static final String GRANT_TYPE_REFRESH = "refresh_token";
    protected static final String DUMMY_STATE = "abcd1234";
    protected static final String[] TEST_SCOPES = new String[] { "scope1",
                                                                "scope2" };

    protected static final String PUBLIC_REDIRECT_URI = "http://localhost:9080/oauth/client.jsp";
    protected static final String CONF_REDIRECT_URI = "http://localhost:9080/oauth/client.jsp";

    final String MAPKEY_OAUTHRESULT = "MAPKEY_OAUTHRESULT";
    final String MAPKEY_REPONSETEXT = "MAPKEY_REPONSETEXT";

    Map<String, Object> baseAuthzCodeFlowTestAuthorizeStep(
                                                           OAuthComponentConfiguration config, String clientId,
                                                           String redirectUri, String[] scopes) {
        if (config == null) {
            config = new BaseConfig();
        }
        initializeOAuthFramework(config);

        MockServletResponse resp = new MockServletResponse();
        StringWriter responseBuffer = new StringWriter();
        resp.setWriter(responseBuffer);

        OAuthResult oresult = oauth20.processAuthorization(TESTUSER, clientId,
                                                           redirectUri, RESPONSE_TYPE_CODE, DUMMY_STATE, scopes, resp);
        String responseString = responseBuffer.toString();

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(MAPKEY_OAUTHRESULT, oresult);
        result.put(MAPKEY_REPONSETEXT, responseString);

        return result;
    }

    void validateAuthorizeStepResults(Map<String, Object> results) {
        OAuthResult oresult = (OAuthResult) results.get(MAPKEY_OAUTHRESULT);

        assertNotNull(oresult);
        if (oresult.getStatus() != OAuthResult.STATUS_OK) {
            oresult.getCause().printStackTrace();
            fail();
        }
        assertNull(oresult.getCause());

        String code = oresult.getAttributeList()
                        .getAttributeValueByNameAndType("code",
                                                        OAuthConstants.ATTRTYPE_RESPONSE_ATTRIBUTE);
        assertNotNull(code);

        String state = oresult.getAttributeList()
                        .getAttributeValueByNameAndType("state",
                                                        OAuthConstants.ATTRTYPE_RESPONSE_ATTRIBUTE);
        assertEquals(state, DUMMY_STATE);
    }

    Map<String, Object> baseAuthzCodeFlowTestTokenStep(String clientId,
                                                       String clientSecret, String redirectUri, String code,
                                                       String grantType) {
        // component has to be initialized before calling this method

        MockServletRequest req = new MockServletRequest();
        req.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                      "application/x-www-form-urlencoded");
        if (clientId != null) {
            req.setParameter("client_id", clientId);
        }
        if (clientSecret != null) {
            req.setParameter("client_secret", clientSecret);
        }
        if (redirectUri != null) {
            req.setParameter("redirect_uri", redirectUri);
        }
        if (code != null) {
            req.setParameter("code", code);
        }
        req.setParameter("grant_type", grantType);
        req.setMethod("POST");
        req.setServletPath("/oauth2");
        req.setPathInfo("/access_token");

        MockServletResponse resp = new MockServletResponse();
        StringWriter responseBuffer = new StringWriter();
        resp.setWriter(responseBuffer);

        OAuthResult oresult = oauth20.processTokenRequest(null, req, resp);
        String responseString = responseBuffer.toString();

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(MAPKEY_OAUTHRESULT, oresult);
        result.put(MAPKEY_REPONSETEXT, responseString);

        return result;
    }

    Map<String, Object> baseRefreshTokenStep(String clientId,
                                             String clientSecret, String refreshToken, String scope,
                                             String grantType) {
        // component has to be initialized before calling this method

        MockServletRequest req = new MockServletRequest();
        req.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                      "application/x-www-form-urlencoded");
        if (clientId != null) {
            req.setParameter("client_id", clientId);
        }
        if (clientSecret != null) {
            req.setParameter("client_secret", clientSecret);
        }
        if (refreshToken != null) {
            req.setParameter("refresh_token", refreshToken);
        }
        if (scope != null) {
            req.setParameter("scope", scope);
        }
        req.setParameter("grant_type", grantType);
        req.setMethod("POST");
        req.setServletPath("/oauth2");
        req.setPathInfo("/access_token");

        MockServletResponse resp = new MockServletResponse();
        StringWriter responseBuffer = new StringWriter();
        resp.setWriter(responseBuffer);

        OAuthResult oresult = oauth20.processTokenRequest(null, req, resp);
        String responseString = responseBuffer.toString();

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(MAPKEY_OAUTHRESULT, oresult);
        result.put(MAPKEY_REPONSETEXT, responseString);

        return result;
    }

    void validateTokenStepResults(Map<String, Object> results) {
        OAuthResult oresult = (OAuthResult) results.get(MAPKEY_OAUTHRESULT);
        String responseString = (String) results.get(MAPKEY_REPONSETEXT);

        assertNotNull(oresult);
        if (oresult.getStatus() != OAuthResult.STATUS_OK) {
            oresult.getCause().printStackTrace();
            fail();
        }
        assertEquals(oresult.getStatus(), OAuthResult.STATUS_OK);
        assertNull(oresult.getCause());

        // check compulsory response params
        try {
            JSONObject json = JSONObject.parse(responseString);
            assertTrue(json.containsKey("token_type"));
            assertTrue(json.containsKey("access_token"));
            assertTrue("Bearer".equals(json.get("token_type")));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    Map<String, Object> runAndValidateStandardAuthorizationFlow(
                                                                OAuthComponentConfiguration config, String clientId,
                                                                String clientSecret, String redirectUri, String[] scopes) {
        Map<String, Object> authorizeResults = baseAuthzCodeFlowTestAuthorizeStep(
                                                                                  config, clientId, redirectUri, scopes);
        validateAuthorizeStepResults(authorizeResults);

        // extract code - already validated
        OAuthResult oresult = (OAuthResult) authorizeResults
                        .get(MAPKEY_OAUTHRESULT);
        String code = oresult.getAttributeList()
                        .getAttributeValueByNameAndType("code",
                                                        OAuthConstants.ATTRTYPE_RESPONSE_ATTRIBUTE);

        Map<String, Object> tokenResults = baseAuthzCodeFlowTestTokenStep(
                                                                          clientId, clientSecret, redirectUri, code, GRANT_TYPE_AZN_CODE);
        validateTokenStepResults(tokenResults);
        return tokenResults;
    }

    /**
     * Test retrieving an access token using a public client
     * 
     * @throws Exception
     */
    public void testGetAccessTokenPublic() throws Exception {
        BaseConfig config = new BaseConfig();
        config
                        .putConfigPropertyValues(
                                                 OAuthComponentConfigurationConstants.OAUTH20_ALLOW_PUBLIC_CLIENTS,
                                                 new String[] { "true" });

        runAndValidateStandardAuthorizationFlow(config, PUBLIC_CLIENT_ID, null,
                                                PUBLIC_REDIRECT_URI, TEST_SCOPES);
    }

    /**
     * Test retrieving an access token using a confidential client
     * 
     * @throws Exception
     */
    public void testGetAccessTokenConfidential() throws Exception {
        BaseConfig config = new BaseConfig();

        runAndValidateStandardAuthorizationFlow(config, CONF_CLIENT_ID,
                                                CONF_CLIENT_SECRET, CONF_REDIRECT_URI, TEST_SCOPES);
    }

    /**
     * Test retrieving an authorization code using a confidential client without
     * setting redirect URI
     * 
     * The redirect URI is registered with this client, so omitting it should
     * still generate a response using the registered redirect URI.
     * 
     * @throws Exception
     */
    public void testGetAuthorizationCodeNoRedirect() throws Exception {
        BaseConfig config = new BaseConfig();

        runAndValidateStandardAuthorizationFlow(config, CONF_CLIENT_ID,
                                                CONF_CLIENT_SECRET, null, TEST_SCOPES);
    }

    /**
     * Test retrieving an authorization code using a confidential client with a
     * bad redirect URI on the token step
     * 
     * The redirect URI is registered with this client, so passing a redirect
     * that doesn't match the registered value should generate an error per the
     * OAuth 2.0 spec.
     * 
     * See Section 3.1.2.3 under
     * http://tools.ietf.org/html/draft-ietf-oauth-v2-20#section-3.1.2
     * 
     * 
     * @throws Exception
     */
    public void testGetAuthorizationCodeBadRedirect() throws Exception {
        BaseConfig config = new BaseConfig();

        Map<String, Object> authorizeResults = baseAuthzCodeFlowTestAuthorizeStep(
                                                                                  config, CONF_CLIENT_ID, CONF_REDIRECT_URI, TEST_SCOPES);
        validateAuthorizeStepResults(authorizeResults);

        // extract code - already validated
        OAuthResult oresult = (OAuthResult) authorizeResults
                        .get(MAPKEY_OAUTHRESULT);
        String code = oresult.getAttributeList()
                        .getAttributeValueByNameAndType("code",
                                                        OAuthConstants.ATTRTYPE_RESPONSE_ATTRIBUTE);

        // call token endpoint with bad redirect
        Map<String, Object> tokenResults = baseAuthzCodeFlowTestTokenStep(
                                                                          CONF_CLIENT_ID, CONF_CLIENT_SECRET, "https://badredirect.com",
                                                                          code, GRANT_TYPE_AZN_CODE);
        oresult = (OAuthResult) tokenResults.get(MAPKEY_OAUTHRESULT);

        // should fail, bad redirect
        assertEquals(oresult.getStatus(), OAuthResult.STATUS_FAILED);
        assertNotNull(oresult.getCause());
    }

    /**
     * Test attempting to get an access token using a bad client secret with a
     * confidential client.
     */
    public void testGetAccessTokenBadConfidentialClientParams()
                    throws Exception {
        BaseConfig config = new BaseConfig();

        Map<String, Object> authorizeResults = baseAuthzCodeFlowTestAuthorizeStep(
                                                                                  config, CONF_CLIENT_ID, CONF_REDIRECT_URI, TEST_SCOPES);
        validateAuthorizeStepResults(authorizeResults);

        // extract code - already validated
        OAuthResult oresult = (OAuthResult) authorizeResults
                        .get(MAPKEY_OAUTHRESULT);
        String code = oresult.getAttributeList()
                        .getAttributeValueByNameAndType("code",
                                                        OAuthConstants.ATTRTYPE_RESPONSE_ATTRIBUTE);

        // call token endpoint with bad secret
        Map<String, Object> tokenResults = baseAuthzCodeFlowTestTokenStep(
                                                                          CONF_CLIENT_ID, "bad_secret", CONF_REDIRECT_URI, code,
                                                                          GRANT_TYPE_AZN_CODE);
        oresult = (OAuthResult) tokenResults.get(MAPKEY_OAUTHRESULT);

        // should fail, bad redirect
        assertEquals(oresult.getStatus(), OAuthResult.STATUS_FAILED);
        assertNotNull(oresult.getCause());
    }

    /**
     * Test attempting to get an access token with an unregistered client ID
     * 
     * @throws Exception
     */
    public void testGetAccessTokenBadClient() throws Exception {
        BaseConfig config = new BaseConfig();

        Map<String, Object> authorizeResults = baseAuthzCodeFlowTestAuthorizeStep(
                                                                                  config, "bad_client_id", CONF_REDIRECT_URI, TEST_SCOPES);
        OAuthResult oresult = (OAuthResult) authorizeResults
                        .get(MAPKEY_OAUTHRESULT);

        // should fail, bad redirect
        assertEquals(oresult.getStatus(), OAuthResult.STATUS_FAILED);
        assertNotNull(oresult.getCause());
    }

    /**
     * Test attempting to get an access token with a bad grant type
     * 
     * @throws Exception
     */
    public void testGetAccessTokenBadGrantType() throws Exception {
        BaseConfig config = new BaseConfig();

        Map<String, Object> authorizeResults = baseAuthzCodeFlowTestAuthorizeStep(
                                                                                  config, CONF_CLIENT_ID, CONF_REDIRECT_URI, TEST_SCOPES);
        validateAuthorizeStepResults(authorizeResults);

        // extract code - already validated
        OAuthResult oresult = (OAuthResult) authorizeResults
                        .get(MAPKEY_OAUTHRESULT);
        String code = oresult.getAttributeList()
                        .getAttributeValueByNameAndType("code",
                                                        OAuthConstants.ATTRTYPE_RESPONSE_ATTRIBUTE);

        // call token endpoint with bad grant type
        Map<String, Object> tokenResults = baseAuthzCodeFlowTestTokenStep(
                                                                          CONF_CLIENT_ID, CONF_CLIENT_SECRET, CONF_REDIRECT_URI, code,
                                                                          "bad_grant_type");
        oresult = (OAuthResult) tokenResults.get(MAPKEY_OAUTHRESULT);

        // should fail, bad redirect
        assertEquals(oresult.getStatus(), OAuthResult.STATUS_FAILED);
        assertNotNull(oresult.getCause());
    }

    /**
     * Test attempting to get an access token with an invalid authorization code
     * 
     * @throws Exception
     */
    public void testGetAccessTokenBadAuthCode() throws Exception {
        BaseConfig config = new BaseConfig();

        Map<String, Object> authorizeResults = baseAuthzCodeFlowTestAuthorizeStep(
                                                                                  config, CONF_CLIENT_ID, CONF_REDIRECT_URI, TEST_SCOPES);
        validateAuthorizeStepResults(authorizeResults);

        // call token endpoint with bad redirect
        Map<String, Object> tokenResults = baseAuthzCodeFlowTestTokenStep(
                                                                          CONF_CLIENT_ID, CONF_CLIENT_SECRET, CONF_REDIRECT_URI,
                                                                          "bad_code", GRANT_TYPE_AZN_CODE);
        OAuthResult oresult = (OAuthResult) tokenResults
                        .get(MAPKEY_OAUTHRESULT);

        // should fail, bad redirect
        assertEquals(oresult.getStatus(), OAuthResult.STATUS_FAILED);
        assertNotNull(oresult.getCause());
    }

    /**
     * Test attempting to illegally re-use an authorization code
     */
    public void testReuseAuthorizationCode() throws Exception {
        BaseConfig config = new BaseConfig();

        Map<String, Object> authorizeResults = baseAuthzCodeFlowTestAuthorizeStep(
                                                                                  config, CONF_CLIENT_ID, CONF_REDIRECT_URI, TEST_SCOPES);
        validateAuthorizeStepResults(authorizeResults);

        // extract code - already validated
        OAuthResult oresult = (OAuthResult) authorizeResults
                        .get(MAPKEY_OAUTHRESULT);
        String code = oresult.getAttributeList()
                        .getAttributeValueByNameAndType("code",
                                                        OAuthConstants.ATTRTYPE_RESPONSE_ATTRIBUTE);

        Map<String, Object> tokenResults = baseAuthzCodeFlowTestTokenStep(
                                                                          CONF_CLIENT_ID, CONF_CLIENT_SECRET, CONF_REDIRECT_URI, code,
                                                                          GRANT_TYPE_AZN_CODE);
        validateTokenStepResults(tokenResults);

        // now try to re-use authorization code
        Map<String, Object> tokenResults2 = baseAuthzCodeFlowTestTokenStep(
                                                                           CONF_CLIENT_ID, CONF_CLIENT_SECRET, CONF_REDIRECT_URI, code,
                                                                           GRANT_TYPE_AZN_CODE);
        oresult = (OAuthResult) tokenResults2.get(MAPKEY_OAUTHRESULT);

        // should fail, bad redirect
        assertEquals(oresult.getStatus(), OAuthResult.STATUS_FAILED);
        assertNotNull(oresult.getCause());
    }

    /**
     * Test attempting to use a refresh token to get a new access token
     */
    public void testRefreshToken() throws Exception {

        BaseConfig config = new BaseConfig();

        Map<String, Object> tokenResults = runAndValidateStandardAuthorizationFlow(
                                                                                   config, CONF_CLIENT_ID, CONF_CLIENT_SECRET, CONF_REDIRECT_URI,
                                                                                   TEST_SCOPES);

        // extract refresh token
        String responseString = (String) tokenResults.get(MAPKEY_REPONSETEXT);
        String refreshToken = null;
        try {
            JSONObject json = JSONObject.parse(responseString);
            if (!json.containsKey("refresh_token"))
                return;
            refreshToken = (String) json.get("refresh_token");
            assertNotNull(refreshToken);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        Map<String, Object> tokenResults2 = baseRefreshTokenStep(
                                                                 CONF_CLIENT_ID, CONF_CLIENT_SECRET, refreshToken, null,
                                                                 GRANT_TYPE_REFRESH);
        validateTokenStepResults(tokenResults2);
    }

    /**
     * Using a standard authorization code flow, test that if scope is returned
     * with the access token, and the scope string was actually a string
     * containing all numerics, that the scope is returned as a JSON string and
     * not an integer.
     * 
     * @throws Exception
     */
    public void testNumericScopeString() throws Exception {
        BaseConfig config = new BaseConfig();

        String[] numericScopes = new String[] { "123" };
        Map<String, Object> tokenResults = runAndValidateStandardAuthorizationFlow(
                                                                                   config, CONF_CLIENT_ID, CONF_CLIENT_SECRET, CONF_REDIRECT_URI,
                                                                                   numericScopes);
        String responseString = (String) tokenResults.get(MAPKEY_REPONSETEXT);

        // if scope is returned, make sure it was returned as a string
        JSONObject json = JSONObject.parse(responseString);
        if (json.containsKey("scope")) {
            Object obj = json.get("scope");
            String className = obj.getClass().getName();
            assertEquals(className, String.class.getName());
        }
    }

    /**
     * Using a standard authorization code flow, test that if multiple scopes of
     * the same value were requested that the returned access token only
     * contains the scope value once.
     * 
     * @throws Exception
     */
    public void testMultipleScopeString() throws Exception {
        BaseConfig config = new BaseConfig();

        String[] multiScopes = new String[] { "scope1", "scope2", "scope1" };
        String[] expectedScopes = new String[] { "scope1", "scope2" };
        Set<String> expectedScopeSet = new HashSet<String>(Arrays.asList(expectedScopes));
        Map<String, Object> tokenResults = runAndValidateStandardAuthorizationFlow(
                                                                                   config, CONF_CLIENT_ID, CONF_CLIENT_SECRET, CONF_REDIRECT_URI,
                                                                                   multiScopes);
        String responseString = (String) tokenResults.get(MAPKEY_REPONSETEXT);

        // if scope is returned, make sure it didn't return multiple scopes with the same value
        String[] scopes = getScopesFromResponseString(responseString);
        if (scopes != null) {
            assertEquals(2, scopes.length);
            Set<String> scopeSet = new HashSet<String>(Arrays.asList(scopes));
            for (int i = 0; i < expectedScopes.length; i++) {
                assertTrue(scopeSet.contains(expectedScopes[i]));
            }
            assertEquals(expectedScopeSet, scopeSet);
        }
    }
}
