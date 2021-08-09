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

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.test.base.BaseConfig;
import com.ibm.oauth.core.test.base.BaseTestCase;
import com.ibm.oauth.core.test.base.MockServletRequest;
import com.ibm.oauth.core.test.base.MockServletResponse;

/*
 * Validate custom config values cause desired changes.
 */
public class CustomConfigValueTest extends BaseTestCase {
    @Test
    public void testCustomOAUTH20_ACCESS_TOKEN_LENGTH() {
        int CUSTOM_LENGTH = 100;
        BaseConfig config = new BaseConfig();
        config.setUniqueId("testCustomOAUTH20_ACCESS_TOKEN_LENGTH");
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_ACCESS_TOKEN_LENGTH,
                new String[] { CUSTOM_LENGTH + "" });
        initializeOAuthFramework(config);

        try {
            OAuthResult result = processResourceRequestAttributes();
            assertNotNull(result);
            assertEquals(OAuthResult.STATUS_OK, result.getStatus());
        } catch (OAuthException e) {
            fail("got an exception: " + e);
            e.printStackTrace();
        }

        String responseString = makeAccessTokenRequest();
        String token = extractAccessToken(responseString);
        assertEquals(token.length(), CUSTOM_LENGTH);
    }

    public void testCustomPublicClients() {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_ALLOW_PUBLIC_CLIENTS,
                new String[] { "true" });
        initializeOAuthFramework(config);

        String responseType = "code";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse responseauth = new MockServletResponse();

        StringWriter responseBuffer = new StringWriter();
        responseauth.setWriter(responseBuffer);

        OAuthResult result = oauth20.processAuthorization("testuser", "key",
                "http://localhost:9080/oauth/client.jsp", responseType, state,
                scope, responseauth);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertNull(result.getCause());

        String code = result.getAttributeList().getAttributeValueByName(
                "authorization_code_id");
        assertNotNull(code);

        MockServletRequest req = new MockServletRequest();
        req.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                "application/x-www-form-urlencoded");
        req.setParameter("client_id", "key");
        req.setParameter("grant_type", "authorization_code");
        req.setParameter("redirect_uri",
                "http://localhost:9080/oauth/client.jsp");
        req.setParameter("code", code);
        req.setMethod("GET");
        req.setServletPath("/oauth2");
        req.setPathInfo("/access_token");

        MockServletResponse resp = new MockServletResponse();
        responseBuffer = new StringWriter();
        resp.setWriter(responseBuffer);

        result = oauth20.processTokenRequest(null, req, resp);
        String responseString = responseBuffer.toString();
        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());

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

    public void testCustomCodeLength() {
        BaseConfig config = new BaseConfig();
        String[] CODE_LENGTH = { "200" };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_CODE_LENGTH,
                CODE_LENGTH);

        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = "http://localhost:9080/oauth/client.jsp";
        String responseType = "code";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse responseauth = new MockServletResponse();

        StringWriter responseBuffer = new StringWriter();
        responseauth.setWriter(responseBuffer);

        OAuthResult result = oauth20.processAuthorization(username, clientId,
                redirectUri, responseType, state, scope, responseauth);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertNull(result.getCause());

        String code = result.getAttributeList().getAttributeValueByName(
                "authorization_code_id");
        assertNotNull(code);
        assertEquals(code.length(), 200);
    }

    public void testCustomRefreshTokenLength() {
        BaseConfig config = new BaseConfig();
        String[] REFRESH_TOKEN_LENGTH = { "250" };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_REFRESH_TOKEN_LENGTH,
                REFRESH_TOKEN_LENGTH);

        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = "http://localhost:9080/oauth/client.jsp";
        String responseType = "code";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse responseauth = new MockServletResponse();

        StringWriter responseBuffer = new StringWriter();
        responseauth.setWriter(responseBuffer);

        OAuthResult result = oauth20.processAuthorization(username, clientId,
                redirectUri, responseType, state, scope, responseauth);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertNull(result.getCause());

        String code = result.getAttributeList().getAttributeValueByName(
                "authorization_code_id");
        assertNotNull(code);

        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();

        request.setParameter(OAuth20Constants.CLIENT_ID, "key");
        request.setParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.setParameter(OAuth20Constants.GRANT_TYPE,
                OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
        request.setParameter(OAuth20Constants.RESPONSE_TYPE,
                OAuth20Constants.RESPONSE_TYPE_CODE);
        request.setParameter(OAuth20Constants.CODE, code);
        request.setParameter(OAuth20Constants.REDIRECT_URI, redirectUri);
        request.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                OAuth20Constants.HTTP_CONTENT_TYPE_FORM);

        responseBuffer = new StringWriter();
        response.setWriter(responseBuffer);

        result = oauth20.processTokenRequest(null, request, response);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());

        String responseString = responseBuffer.toString();
        String refresh_token = extractRefreshToken(responseString);
        if (refresh_token != null)
            assertEquals(refresh_token.length(), 250);
        // TODO: may be better to check if refresh_token is allowed rather than checking for null
    }

    public void testCustomNoRefreshToken() {
        BaseConfig config = new BaseConfig();
        String[] ISSUE_REFRESH_TOKEN = { "false" };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_ISSUE_REFRESH_TOKEN,
                ISSUE_REFRESH_TOKEN);

        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = "http://localhost:9080/oauth/client.jsp";
        String responseType = "code";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse responseauth = new MockServletResponse();

        StringWriter responseBuffer = new StringWriter();
        responseauth.setWriter(responseBuffer);

        OAuthResult result = oauth20.processAuthorization(username, clientId,
                redirectUri, responseType, state, scope, responseauth);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertNull(result.getCause());

        String code = result.getAttributeList().getAttributeValueByName(
                "authorization_code_id");
        assertNotNull(code);

        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();

        request.setParameter(OAuth20Constants.CLIENT_ID, "key");
        request.setParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.setParameter(OAuth20Constants.GRANT_TYPE,
                OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
        request.setParameter(OAuth20Constants.RESPONSE_TYPE,
                OAuth20Constants.RESPONSE_TYPE_CODE);
        request.setParameter(OAuth20Constants.CODE, code);
        request.setParameter(OAuth20Constants.REDIRECT_URI, redirectUri);
        request.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                OAuth20Constants.HTTP_CONTENT_TYPE_FORM);

        responseBuffer = new StringWriter();
        response.setWriter(responseBuffer);

        result = oauth20.processTokenRequest(null, request, response);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());

        String responseString = responseBuffer.toString();
        String refresh_token = extractRefreshToken(responseString);
        assertNull(refresh_token);
    }

    public void testGoodDefaultConfiguration() {
        BaseConfig config = new BaseConfig();
        config.setUniqueId("testGoodDefaultConfiguration");
        initializeOAuthFramework(config);

        try {
            OAuthResult result = processResourceRequestAttributes();
            assertNotNull(result);
            assertEquals(OAuthResult.STATUS_OK, result.getStatus());
        } catch (OAuthException e) {
            fail("got an exception: " + e);
            e.printStackTrace();
        }
    }

}
