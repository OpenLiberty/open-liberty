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
package com.ibm.oauth.core.test;

import java.io.StringWriter;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.test.base.BaseConfig;
import com.ibm.oauth.core.test.base.BaseTestCase;
import com.ibm.oauth.core.test.base.MockServletRequest;
import com.ibm.oauth.core.test.base.MockServletResponse;

public class OAuth20ErrorFlowsTest extends BaseTestCase {

    protected static final String CLIENT_ID = "key";

    protected static final String CLIENT_SECRET = "secret";

    protected static final String CLIENT_ID_NULL_REDIRECT_URI = "key3";

    protected static final String CLIENT_ID_EMPTY_STRING_REDIRECT_URI = "key4";

    protected static final String CLIENT_ID_MULTIPLE_REDIRECT_URIS = "key5";

    protected static final String REDIRECT_URI = "http://localhost:9080/oauth/client.jsp";

    protected static final String AUTHORIZATION_CODE_RESPONSE_TYPE = "code";

    protected static final String AUTHORIZATION_TOKEN_RESPONSE_TYPE = "token";

    protected static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    protected static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";

    protected static final String[] AUTHORIZATION_RESPONSE_TYPES = new String[] {
                                                                                 AUTHORIZATION_CODE_RESPONSE_TYPE, AUTHORIZATION_TOKEN_RESPONSE_TYPE };

    protected static final String TOKEN_TYPE_CODE = "code";

    protected static final String TOKEN_TYPE_REFRESH = "refresh_token";

    protected static final String EMPTY_STRING = "";

    protected static final String NULL_STRING = null;

    protected static final String USERNAME = "testuser";

    protected static final String MISSING_PARAMETER_MESSAGE = "CWOAU0033E: A required runtime parameter was missing: ";

    protected static final String MISSING_USERNAME_MESSAGE = MISSING_PARAMETER_MESSAGE
                                                             + "username";
    protected static final String MISSING_REDIRECT_URI_MESSAGE = MISSING_PARAMETER_MESSAGE
                                                                 + "redirect_uri";

    // TODO: message not looked for. May need more tests?
    protected static final String INVALID_REDIRECT_URI_MESSAGE = "CWOAU0026E: The redirect URI parameter was invalid: ";

    protected static final String MISSING_CLIENT_ID_MESSAGE = MISSING_PARAMETER_MESSAGE
                                                              + "client_id";
    protected static final String MISSING_CODE_MESSAGE = MISSING_PARAMETER_MESSAGE
                                                         + "code";
    protected static final String MISSING_REFRESH_TOKEN_MESSAGE = MISSING_PARAMETER_MESSAGE
                                                                  + "refresh_token";
    protected static final String MISSING_ACCESS_TOKEN_MESSAGE = MISSING_PARAMETER_MESSAGE
                                                                 + "access_token";
    protected static final String MISSING_RESPONSE_TYPE_MESSAGE = MISSING_PARAMETER_MESSAGE
                                                                  + "response_type";

    protected static final String MISSING_GRANT_TYPE_MESSAGE = "CWOAU0025E: The grant_type parameter was invalid: ";

    protected static final String PUBLIC_CLIENT_ACCESS_CONFIDENTIAL_TOKEN_ENDPOINT_MESSAGE = "CWOAU0035E: A public client attempted to access the token endpoint and public clients are forbidden in this component configuration. The client_id is: ";

    protected static final String RECEIVED_REDIRECT_URI_MISMATCH_START = "CWOAU0032E: The received redirect URI: ";

    protected static final String RECEIVED_REDIRECT_URI_MISMATCH_END = " does not match the redirect URI the grant was issued to: ";

    protected static final String BAD_CODE_MESSAGE_START = "CWOAU0029E: The token with key: ";

    protected static final String BAD_CODE_MESSAGE_END = " type: authorization_grant subType: authorization_code was not found in the token cache.";

    protected static final String BAD_REFRESH_TOKEN_MESSAGE_START = "CWOAU0029E: The token with key: ";

    protected static final String BAD_REFRESH_TOKEN_MESSAGE_END = " type: authorization_grant subType: refresh_token was not found in the token cache.";

    protected static final String INVALID_REGISTERED_REDIRECT_URI_MESSAGE = "CWOAU0055E: The redirect URI specified in the registered client of the OAuth provider is not valid: ";

    protected static final String MISMATCHED_REDIRECT_URI_MESSAGE_START = "CWOAU0056E: The redirect URI parameter [";

    protected static final String MISMATCHED_REDIRECT_URI_MESSAGE_END = "] provided in the OAuth or OpenID Connect request did not match any of the redirect URIs registered with the OAuth provider ";

    // TODO
    protected static final String EXPIRED_TOKEN_START = "CWOAU0058E: The token with key: [";

    protected static final String EXPIRED_TOKEN_END = "], type: [authorization_grant], and subType: [authorization_code] has expired.";

    // TODO
    protected static final String INVALID_TOKEN_ASSOCIATION_START = "CWOAU0059E: The token with key: [";

    protected static final String INVALID_TOKEN_ASSOCIATION_END = "], type: [authorization_grant], and subType: [authorization_code] is not associated with a valid client.";

    protected static final String MISMATCHED_REDIRECT_URI_NULL_REDIRECT_URI = "CWOAU0060E: The redirect URI included in the OAuth or OpenID Connect request is null, but a non-null redirect URI is provided in the request for the authorization grant: ";

    public void testAuthorizeResponseTypeNull() throws Exception {
        authorizeError(NULL_STRING, CLIENT_ID, REDIRECT_URI, USERNAME,
                       MISSING_RESPONSE_TYPE_MESSAGE);
    }

    public void testAuthorizeResponseTypeEmptyString() throws Exception {
        authorizeError(EMPTY_STRING, CLIENT_ID, REDIRECT_URI, USERNAME,
                       MISSING_RESPONSE_TYPE_MESSAGE);
    }

    public void testAuthorizeClientIdNull() throws Exception {
        for (int i = 0; i < AUTHORIZATION_RESPONSE_TYPES.length; i++) {
            String responseType = AUTHORIZATION_RESPONSE_TYPES[i];
            authorizeError(responseType, NULL_STRING, REDIRECT_URI, USERNAME,
                           MISSING_CLIENT_ID_MESSAGE);
        }
    }

    public void testAuthorizeClientIdEmptyString() throws Exception {
        for (int i = 0; i < AUTHORIZATION_RESPONSE_TYPES.length; i++) {
            String responseType = AUTHORIZATION_RESPONSE_TYPES[i];
            authorizeError(responseType, EMPTY_STRING, REDIRECT_URI, USERNAME,
                           MISSING_CLIENT_ID_MESSAGE);
        }
    }

    public void testAuthorizeRedirectUriNullRegisteredRedirectUriNull()
                    throws Exception {
        for (int i = 0; i < AUTHORIZATION_RESPONSE_TYPES.length; i++) {
            String responseType = AUTHORIZATION_RESPONSE_TYPES[i];
            authorizeError(responseType, CLIENT_ID_NULL_REDIRECT_URI,
                           NULL_STRING, USERNAME, MISSING_REDIRECT_URI_MESSAGE);
        }
    }

    public void testAuthorizeRedirectUriEmptyStringRegisteredRedirectUriNull()
                    throws Exception {
        for (int i = 0; i < AUTHORIZATION_RESPONSE_TYPES.length; i++) {
            String responseType = AUTHORIZATION_RESPONSE_TYPES[i];
            authorizeError(responseType, CLIENT_ID_NULL_REDIRECT_URI,
                           EMPTY_STRING, USERNAME, MISSING_REDIRECT_URI_MESSAGE);
        }
    }

    public void testAuthorizeRedirectUriNullRegisteredRedirectUriEmptyString()
                    throws Exception {
        for (int i = 0; i < AUTHORIZATION_RESPONSE_TYPES.length; i++) {
            String responseType = AUTHORIZATION_RESPONSE_TYPES[i];
            authorizeError(responseType, CLIENT_ID_EMPTY_STRING_REDIRECT_URI,
                           NULL_STRING, USERNAME, INVALID_REGISTERED_REDIRECT_URI_MESSAGE);
        }
    }

    public void testAuthorizeRedirectUriEmptyStringRegisteredRedirectUriEmptyString()
                    throws Exception {
        for (int i = 0; i < AUTHORIZATION_RESPONSE_TYPES.length; i++) {
            String responseType = AUTHORIZATION_RESPONSE_TYPES[i];
            authorizeError(responseType, CLIENT_ID_EMPTY_STRING_REDIRECT_URI,
                           EMPTY_STRING, USERNAME, INVALID_REGISTERED_REDIRECT_URI_MESSAGE);
        }
    }

    public void testAuthorizedRedirectUriNotIncludedInRegisteredRedirectUris() {
        String redirectUris = "http://localhost/other/redirect1 http://localhost/other/redirect2";
        for (int i = 0; i < AUTHORIZATION_RESPONSE_TYPES.length; i++) {
            String responseType = AUTHORIZATION_RESPONSE_TYPES[i];
            authorizeError(responseType, CLIENT_ID_MULTIPLE_REDIRECT_URIS, REDIRECT_URI, USERNAME,
                           MISMATCHED_REDIRECT_URI_MESSAGE_START + REDIRECT_URI + MISMATCHED_REDIRECT_URI_MESSAGE_END + "[" + redirectUris + "].");
        }
    }

    public void testAuthorizeUsernameNull() throws Exception {
        for (int i = 0; i < AUTHORIZATION_RESPONSE_TYPES.length; i++) {
            String responseType = AUTHORIZATION_RESPONSE_TYPES[i];
            authorizeError(responseType, CLIENT_ID, REDIRECT_URI, NULL_STRING,
                           MISSING_USERNAME_MESSAGE);
        }
    }

    public void testAuthorizeUsernameEmptyString() throws Exception {
        for (int i = 0; i < AUTHORIZATION_RESPONSE_TYPES.length; i++) {
            String responseType = AUTHORIZATION_RESPONSE_TYPES[i];
            authorizeError(responseType, CLIENT_ID, REDIRECT_URI, EMPTY_STRING,
                           MISSING_USERNAME_MESSAGE);
        }
    }

    public void testCodeClientIdNullPublicClient() throws Exception {
        String code = getAuthorizationCode(CLIENT_ID, true);
        tokenError(NULL_STRING, NULL_STRING, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, code, TOKEN_TYPE_CODE, MISSING_CLIENT_ID_MESSAGE,
                   true);
    }

    public void testCodeClientIdEmptyStringPublicClient() throws Exception {
        String code = getAuthorizationCode(CLIENT_ID, true);
        tokenError(EMPTY_STRING, NULL_STRING, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, code, TOKEN_TYPE_CODE, MISSING_CLIENT_ID_MESSAGE,
                   true);
    }

    public void testCodeClientIdNullConfidentialClient() throws Exception {
        String code = getAuthorizationCode(CLIENT_ID, false);
        tokenError(NULL_STRING, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, code, TOKEN_TYPE_CODE, MISSING_CLIENT_ID_MESSAGE,
                   true);
    }

    public void testCodeClientIdEmptyStringConfidentialClient()
                    throws Exception {
        String code = getAuthorizationCode(CLIENT_ID, false);
        tokenError(EMPTY_STRING, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, code, TOKEN_TYPE_CODE, MISSING_CLIENT_ID_MESSAGE,
                   true);
    }

    public void testCodeClientSecretNullConfidentialClient() throws Exception {
        String code = getAuthorizationCode(CLIENT_ID, false);
        tokenError(CLIENT_ID, NULL_STRING, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, code, TOKEN_TYPE_CODE,
                   PUBLIC_CLIENT_ACCESS_CONFIDENTIAL_TOKEN_ENDPOINT_MESSAGE
                                   + CLIENT_ID, true);
    }

    public void testCodeClientSecretEmptyStringConfidentialClient()
                    throws Exception {
        String code = getAuthorizationCode(CLIENT_ID, false);
        tokenError(CLIENT_ID, EMPTY_STRING, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, code, TOKEN_TYPE_CODE,
                   PUBLIC_CLIENT_ACCESS_CONFIDENTIAL_TOKEN_ENDPOINT_MESSAGE
                                   + CLIENT_ID, true);
    }

    public void testCodeGrantTypeNull() throws Exception {
        String code = getAuthorizationCode(CLIENT_ID, true);
        tokenError(CLIENT_ID, CLIENT_SECRET, NULL_STRING, REDIRECT_URI, code,
                   TOKEN_TYPE_CODE, MISSING_GRANT_TYPE_MESSAGE + NULL_STRING, true);
    }

    public void testCodeGrantTypeEmptyString() throws Exception {
        String code = getAuthorizationCode(CLIENT_ID, true);
        tokenError(CLIENT_ID, CLIENT_SECRET, EMPTY_STRING, REDIRECT_URI, code,
                   TOKEN_TYPE_CODE, MISSING_GRANT_TYPE_MESSAGE, true);
    }

    // TODO
//    public void testCodeGrantTypeExpired() throws Exception {
//      String code = getAuthorizationCode(CLIENT_ID, true);
//      tokenError();
//    }

    // TODO
//    public void testCodeGrantTypeNotAssociatedWithValidClient() throws Exception {
//        String code = getAuthorizationCode(CLIENT_ID, true);
//        tokenError();
//    }

    public void testCodeRedirectUriNullRegisteredRedirectUriNullMismatch()
                    throws Exception {
        String code = getAuthorizationCode(CLIENT_ID_NULL_REDIRECT_URI, true);
        tokenError(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   NULL_STRING, code, TOKEN_TYPE_CODE,
                   MISMATCHED_REDIRECT_URI_NULL_REDIRECT_URI + REDIRECT_URI,
                   true);
    }

    public void testCodeRedirectUriEmptyStringRegisteredRedirectUriNullMismatch()
                    throws Exception {
        String code = getAuthorizationCode(CLIENT_ID_NULL_REDIRECT_URI, true);
        tokenError(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   EMPTY_STRING, code, TOKEN_TYPE_CODE,
                   MISMATCHED_REDIRECT_URI_NULL_REDIRECT_URI + REDIRECT_URI,
                   true);
    }

    public void testCodeRedirectUriNullRegisteredRedirectUriEmptyStringMismatch()
                    throws Exception {
        String code = getAuthorizationCode(CLIENT_ID_EMPTY_STRING_REDIRECT_URI,
                                           true);
        tokenError(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   NULL_STRING, code, TOKEN_TYPE_CODE,
                   MISMATCHED_REDIRECT_URI_NULL_REDIRECT_URI + REDIRECT_URI,
                   true);
    }

    public void testCodeRedirectUriEmptyStringRegisteredRedirectUriEmptyStringMismatch()
                    throws Exception {
        String code = getAuthorizationCode(CLIENT_ID_EMPTY_STRING_REDIRECT_URI,
                                           true);
        tokenError(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   EMPTY_STRING, code, TOKEN_TYPE_CODE,
                   MISMATCHED_REDIRECT_URI_NULL_REDIRECT_URI + REDIRECT_URI,
                   true);
    }

    public void testCodeNull() throws Exception {
        tokenError(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, NULL_STRING, TOKEN_TYPE_CODE,
                   MISSING_CODE_MESSAGE, true);
    }

    public void testCodeEmptyString() throws Exception {
        tokenError(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, EMPTY_STRING, TOKEN_TYPE_CODE,
                   MISSING_CODE_MESSAGE, true);
    }

    public void testCodeMismatch() throws Exception {
        String code = getAuthorizationCode(CLIENT_ID_EMPTY_STRING_REDIRECT_URI,
                                           true)
                      + "bad";
        tokenError(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, code, TOKEN_TYPE_CODE, BAD_CODE_MESSAGE_START
                                                        + code + BAD_CODE_MESSAGE_END, true);
    }

    public void testCodeReuse() throws Exception {
        String code = getAuthorizationCode(CLIENT_ID_EMPTY_STRING_REDIRECT_URI,
                                           true);
        tokenError(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, code, TOKEN_TYPE_CODE, "", false);
        tokenError(CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_CODE_GRANT_TYPE,
                   REDIRECT_URI, code, TOKEN_TYPE_CODE, BAD_CODE_MESSAGE_START
                                                        + code + BAD_CODE_MESSAGE_END, true);
    }

    public void testRefreshClientIdNullPublicClient() throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, NULL_STRING, true);
        tokenError(NULL_STRING, NULL_STRING, REFRESH_TOKEN_GRANT_TYPE,
                   REDIRECT_URI, refreshToken, TOKEN_TYPE_REFRESH,
                   MISSING_CLIENT_ID_MESSAGE, true);
    }

    public void testRefreshClientIdEmptyStringPublicClient() throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, NULL_STRING, true);
        tokenError(EMPTY_STRING, NULL_STRING, REFRESH_TOKEN_GRANT_TYPE,
                   REDIRECT_URI, refreshToken, TOKEN_TYPE_REFRESH,
                   MISSING_CLIENT_ID_MESSAGE, true);
    }

    public void testRefreshClientIdNullConfidentialClient() throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, CLIENT_SECRET, false);
        tokenError(NULL_STRING, CLIENT_SECRET, REFRESH_TOKEN_GRANT_TYPE,
                   REDIRECT_URI, refreshToken, TOKEN_TYPE_REFRESH,
                   MISSING_CLIENT_ID_MESSAGE, true);
    }

    public void testRefreshClientIdEmptyStringConfidentialClient()
                    throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, CLIENT_SECRET, false);
        tokenError(EMPTY_STRING, CLIENT_SECRET, REFRESH_TOKEN_GRANT_TYPE,
                   REDIRECT_URI, refreshToken, TOKEN_TYPE_REFRESH,
                   MISSING_CLIENT_ID_MESSAGE, true);
    }

    public void testRefreshClientSecretNullConfidentialClient()
                    throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, CLIENT_SECRET, false);
        tokenError(CLIENT_ID, NULL_STRING, REFRESH_TOKEN_GRANT_TYPE,
                   REDIRECT_URI, refreshToken, TOKEN_TYPE_REFRESH,
                   PUBLIC_CLIENT_ACCESS_CONFIDENTIAL_TOKEN_ENDPOINT_MESSAGE
                                   + CLIENT_ID, true);
    }

    public void testRefreshClientSecretEmptyStringConfidentialClient()
                    throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, CLIENT_SECRET, false);
        tokenError(CLIENT_ID, EMPTY_STRING, REFRESH_TOKEN_GRANT_TYPE,
                   REDIRECT_URI, refreshToken, TOKEN_TYPE_REFRESH,
                   PUBLIC_CLIENT_ACCESS_CONFIDENTIAL_TOKEN_ENDPOINT_MESSAGE
                                   + CLIENT_ID, true);
    }

    public void testRefreshGrantTypeNull() throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, CLIENT_SECRET, true);
        tokenError(CLIENT_ID, CLIENT_SECRET, NULL_STRING, REDIRECT_URI,
                   refreshToken, TOKEN_TYPE_REFRESH, MISSING_GRANT_TYPE_MESSAGE
                                                     + NULL_STRING, true);
    }

    public void testRefreshGrantTypeEmptyString() throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, CLIENT_SECRET, true);
        tokenError(CLIENT_ID, CLIENT_SECRET, EMPTY_STRING, REDIRECT_URI,
                   refreshToken, TOKEN_TYPE_REFRESH, MISSING_GRANT_TYPE_MESSAGE,
                   true);
    }

    public void testRefreshNull() throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, CLIENT_SECRET, true);
        if (refreshToken != null)
            tokenError(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN_GRANT_TYPE,
                       REDIRECT_URI, NULL_STRING, TOKEN_TYPE_REFRESH,
                       MISSING_REFRESH_TOKEN_MESSAGE, true);
    }

    public void testRefreshEmptyString() throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, CLIENT_SECRET, true);
        if (refreshToken != null)
            tokenError(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN_GRANT_TYPE,
                       REDIRECT_URI, EMPTY_STRING, TOKEN_TYPE_REFRESH,
                       MISSING_REFRESH_TOKEN_MESSAGE, true);
    }

    public void testRefreshMismatch() throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, CLIENT_SECRET, true);
        if (refreshToken != null) {
            refreshToken = getRefreshToken(CLIENT_ID, NULL_STRING, true)
                           + "bad";
            tokenError(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN_GRANT_TYPE,
                       REDIRECT_URI, refreshToken, TOKEN_TYPE_REFRESH,
                       BAD_REFRESH_TOKEN_MESSAGE_START + refreshToken
                                       + BAD_REFRESH_TOKEN_MESSAGE_END, true);
        }
    }

    public void testRefreshReuse() throws Exception {
        String refreshToken = getRefreshToken(CLIENT_ID, CLIENT_SECRET, true);
        if (refreshToken != null) {
            refreshToken = getRefreshToken(CLIENT_ID, NULL_STRING, true)
                           + "bad";
            tokenError(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN_GRANT_TYPE,
                       REDIRECT_URI, refreshToken, TOKEN_TYPE_REFRESH, "", false);
            tokenError(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN_GRANT_TYPE,
                       REDIRECT_URI, refreshToken, TOKEN_TYPE_REFRESH,
                       BAD_REFRESH_TOKEN_MESSAGE_START + refreshToken
                                       + BAD_REFRESH_TOKEN_MESSAGE_END, true);
        }
    }

    public void testNullAccessTokenResourceRequest() throws Exception {
        AttributeList attrs = new AttributeList();
        attrs.setAttribute("access_token", null, new String[] { NULL_STRING });
        resourceError(attrs, MISSING_ACCESS_TOKEN_MESSAGE);
    }

    public void testEmptyStringAccessTokenResourceRequest() throws Exception {
        AttributeList attrs = new AttributeList();
        attrs.setAttribute("access_token", null, new String[] { EMPTY_STRING });
        resourceError(attrs, MISSING_ACCESS_TOKEN_MESSAGE);
    }

    public void testNullAttrsResourceRequest() throws Exception {
        AttributeList attrs = null;
        resourceError(attrs, MISSING_ACCESS_TOKEN_MESSAGE);
    }

    public void testEmptyAttrsResourceRequest() throws Exception {
        AttributeList attrs = new AttributeList();
        resourceError(attrs, MISSING_ACCESS_TOKEN_MESSAGE);
    }

    private void authorizeError(String responseType, String clientId,
                                String redirectUri, String username, String expectedMessage) {
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);

        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse responseauth = new MockServletResponse();

        StringWriter responseBuffer = new StringWriter();
        responseauth.setWriter(responseBuffer);

        OAuthResult result = oauth20.processAuthorization(username, clientId,
                                                          redirectUri, responseType, state, scope, responseauth);

        assertNotNull(result);
        if (result.getStatus() == OAuthResult.STATUS_OK) {
            fail();
        } else {
            Exception cause = result.getCause();
            assertNotNull(cause);
            assertEquals(expectedMessage, cause.getMessage());
        }
    }

    private void tokenError(String clientId, String clientSecret,
                            String grantType, String redirectUri, String value, String type,
                            String expectedMessage, boolean expectFail) {

        MockServletRequest req = new MockServletRequest();
        req.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                      "application/x-www-form-urlencoded");

        req.setParameter("client_id", clientId);
        if (clientSecret != null && clientSecret.length() > 0) {
            req.setParameter("client_secret", clientSecret);
        }

        if (type != null & type.equals(TOKEN_TYPE_CODE)) {
            req.setParameter("redirect_uri", redirectUri);
        }

        req.setParameter("grant_type", grantType);
        req.setParameter(type, value);
        req.setMethod("POST");
        req.setServletPath("/oauth2");
        req.setPathInfo("/access_token");

        MockServletResponse resp = new MockServletResponse();
        StringWriter responseBuffer = new StringWriter();
        resp.setWriter(responseBuffer);

        OAuthResult result = oauth20.processTokenRequest(null, req, resp);
        assertNotNull(result);
        if (expectFail) {
            if (result.getStatus() == OAuthResult.STATUS_OK) {
                fail();
            } else {
                Exception cause = result.getCause();
                assertNotNull(cause);
                assertEquals(expectedMessage, cause.getMessage());
            }
        }
    }

    private void resourceError(AttributeList attrs, String expectedMessage) {

        OAuthResult result = oauth20.processResourceRequest(attrs);

        assertNotNull(result);
        if (result.getStatus() == OAuthResult.STATUS_OK) {
            fail();
        } else {
            Exception cause = result.getCause();
            assertNotNull(cause);
            assertEquals(expectedMessage, cause.getMessage());
        }
    }

    private String getAuthorizationCode(String clientId, boolean allowPublicClients) {
        BaseConfig config = new BaseConfig();

        config.putConfigPropertyValues(OAuthComponentConfigurationConstants.OAUTH20_ALLOW_PUBLIC_CLIENTS,
                                       new String[] { new Boolean(allowPublicClients).toString() });

        initializeOAuthFramework(config);

        String responseType = AUTHORIZATION_CODE_RESPONSE_TYPE;
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse responseauth = new MockServletResponse();

        StringWriter responseBuffer = new StringWriter();
        responseauth.setWriter(responseBuffer);

        // Default to CLIENT_ID if none is specified
        if (clientId == null || clientId.length() > 0) {
            clientId = CLIENT_ID;
        }

        OAuthResult result = oauth20.processAuthorization(USERNAME, clientId,
                                                          REDIRECT_URI, responseType, state, scope, responseauth);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertNull(result.getCause());

        String code = result.getAttributeList().getAttributeValueByName("authorization_code_id");

        assertNotNull(code);

        return code;
    }

    private String getRefreshToken(String clientId, String clientSecret,
                                   boolean allowPublicClients) {

        BaseConfig config = new BaseConfig();
        config
                        .putConfigPropertyValues(
                                                 OAuthComponentConfigurationConstants.OAUTH20_ALLOW_PUBLIC_CLIENTS,
                                                 new String[] { new Boolean(allowPublicClients)
                                                                 .toString() });

        initializeOAuthFramework(config);

        String code = getAuthorizationCode(clientId, allowPublicClients);

        MockServletRequest req = new MockServletRequest();
        req.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                      "application/x-www-form-urlencoded");
        req.setParameter("client_id", clientId);
        if (clientSecret != null && clientSecret.length() > 0) {
            req.setParameter("client_secret", clientSecret);
        }
        req.setParameter("redirect_uri", REDIRECT_URI);

        req.setParameter("grant_type", AUTHORIZATION_CODE_GRANT_TYPE);
        req.setParameter(TOKEN_TYPE_CODE, code);
        req.setMethod("POST");
        req.setServletPath("/oauth2");
        req.setPathInfo("/access_token");

        MockServletResponse resp = new MockServletResponse();
        StringWriter responseBuffer = new StringWriter();
        resp.setWriter(responseBuffer);

        OAuthResult result = oauth20.processTokenRequest(null, req, resp);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertNull(result.getCause());

        String responseString = responseBuffer.toString();
        String refreshToken = extractRefreshToken(responseString);
        return refreshToken;
    }
}
