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

import com.ibm.oauth.core.api.OAuthComponentFactory;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.Attribute;
import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.test.base.BaseConfig;
import com.ibm.oauth.core.test.base.BaseTestCase;
import com.ibm.oauth.core.test.base.MockServletRequest;
import com.ibm.oauth.core.test.base.MockServletResponse;

/*
 */
public class PermittedFlowsTest extends BaseTestCase {

    public void testGoodDefaultConfiguration() {
        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = {
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_AUTH_CODE,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_IMPLICIT,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_OWNER_PASSWORD,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_REFRESH_TOKEN };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);
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

    public void testNoGrantTypesAtAll() {
        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = {};
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

        try {
            OAuthComponentFactory.getOAuthComponentInstance(config);
        } catch (OAuthConfigurationException oace) {
            // correct exception received
        } catch (OAuthException oae) {
            oae.printStackTrace();
            fail("Wrong exception received");
        }
    }

    public void testInvalidGrantType() {
        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = { "just_an_invalid_grant_type" };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

        try {
            OAuthComponentFactory.getOAuthComponentInstance(config);
        } catch (OAuthConfigurationException oace) {
            // correct exception received
        } catch (OAuthException oae) {
            oae.printStackTrace();
            fail("Wrong exception received");
        }
    }

    public void testNoOAUTH20_GRANT_TYPE_AUTH_CODE() {
        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = {
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_IMPLICIT,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_OWNER_PASSWORD,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_REFRESH_TOKEN };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

        initializeOAuthFramework(config);

        // Make access token request

        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();

        request.setParameter(OAuth20Constants.CLIENT_ID, "key");
        request.setParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.setParameter(OAuth20Constants.GRANT_TYPE,
                OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
        request.setParameter(OAuth20Constants.RESPONSE_TYPE,
                OAuth20Constants.RESPONSE_TYPE_CODE);
        request.setParameter(OAuth20Constants.CODE, "bad_value");
        request.setParameter(OAuth20Constants.REDIRECT_URI, "bad_value");
        request.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                OAuth20Constants.HTTP_CONTENT_TYPE_FORM);

        StringWriter responseBuffer = new StringWriter();
        response.setWriter(responseBuffer);

        OAuthResult result = oauth20.processTokenRequest(null, request,
                response);
        assertTrue(result.getStatus() != OAuthResult.STATUS_OK);
        assertTrue(result.getCause() instanceof OAuthConfigurationException);

    }

    public void testOAUTH20_GRANT_TYPE_AUTH_CODE() {
        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = { OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_AUTH_CODE };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

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

    }

    public void testNoOAUTH20_GRANT_TYPE_IMPLICIT() {
        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = {
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_AUTH_CODE,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_OWNER_PASSWORD,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_REFRESH_TOKEN };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = null;// "http://localhost:9080/snoop";
        String responseType = "token";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse response = new MockServletResponse();

        OAuthResult result = null;

        result = oauth20.processAuthorization(username, clientId, redirectUri,
                responseType, state, scope, response);

        assertTrue(result.getStatus() != OAuthResult.STATUS_OK);
        assertTrue(result.getCause() instanceof OAuthConfigurationException);
    }

    public void testOAUTH20_GRANT_TYPE_IMPLICIT() {

        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = { OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_IMPLICIT };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = null;// "http://localhost:9080/snoop";
        String responseType = "token";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse response = new MockServletResponse();

        OAuthResult result = null;

        result = oauth20.processAuthorization(username, clientId, redirectUri,
                responseType, state, scope, response);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());

    }

    public void testImplicitTryAtAGrantToken() {
        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();

        request.setParameter(OAuth20Constants.CLIENT_ID, "key");
        request.setParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.setParameter(OAuth20Constants.GRANT_TYPE,
                OAuth20Constants.GRANT_TYPE_IMPLICIT);
        request.setParameter(OAuth20Constants.RESPONSE_TYPE,
                OAuth20Constants.RESPONSE_TYPE_TOKEN);
        request.setParameter(OAuth20Constants.REDIRECT_URI,
                "http://localhost:9080/oauth/client.jsp");
        request.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                OAuth20Constants.HTTP_CONTENT_TYPE_FORM);

        StringWriter responseBuffer = new StringWriter();
        response.setWriter(responseBuffer);

        OAuthResult result = oauth20.processTokenRequest(null, request,
                response);

        assertTrue(result.getStatus() != OAuthResult.STATUS_OK);
        assertTrue(result.getCause() instanceof OAuth20InvalidGrantTypeException);
    }

    public void testNoOAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS() {
        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = {
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_AUTH_CODE,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_IMPLICIT,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_OWNER_PASSWORD,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_REFRESH_TOKEN };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

        initializeOAuthFramework(config);

        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();

        request.setParameter(OAuth20Constants.CLIENT_ID, "key");
        request.setParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.setParameter(OAuth20Constants.GRANT_TYPE,
                OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS);

        StringWriter responseBuffer = new StringWriter();
        response.setWriter(responseBuffer);

        OAuthResult result = null;

        result = oauth20.processTokenRequest(null, request, response);

        assertTrue(result.getStatus() != OAuthResult.STATUS_OK);
        assertTrue(result.getCause() instanceof OAuthConfigurationException);
    }

    public void testOAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS() {

        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = { OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

        initializeOAuthFramework(config);

        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();

        request.setParameter(OAuth20Constants.CLIENT_ID, "key");
        request.setParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.setParameter(OAuth20Constants.GRANT_TYPE,
                OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS);

        StringWriter responseBuffer = new StringWriter();
        response.setWriter(responseBuffer);

        OAuthResult result = null;

        result = oauth20.processTokenRequest(null, request, response);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertNull(result.getCause());
    }

    public void testNoOAUTH20_GRANT_TYPE_OWNER_PASSWORD() {
        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = {
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_AUTH_CODE,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_IMPLICIT,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_REFRESH_TOKEN };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

        initializeOAuthFramework(config);

        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();

        request.setParameter(OAuth20Constants.USERNAME, "username");
        request.setParameter(OAuth20Constants.PASSWORD, "password");
        request.setParameter(OAuth20Constants.CLIENT_ID, "key");
        request.setParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.setParameter(OAuth20Constants.GRANT_TYPE,
                OAuth20Constants.GRANT_TYPE_PASSWORD);
        request.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                OAuth20Constants.HTTP_CONTENT_TYPE_FORM);

        StringWriter responseBuffer = new StringWriter();
        response.setWriter(responseBuffer);

        OAuthResult result = null;

        result = oauth20.processTokenRequest(null, request, response);

        assertTrue(result.getStatus() != OAuthResult.STATUS_OK);
        assertTrue(result.getCause() instanceof OAuthConfigurationException);

    }

    public void testOAUTH20_GRANT_TYPE_OWNER_PASSWORD() {
        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = { OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_OWNER_PASSWORD };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

        initializeOAuthFramework(config);

        MockServletRequest request = new MockServletRequest();
        MockServletResponse response = new MockServletResponse();

        request.setParameter(OAuth20Constants.USERNAME, "username");
        request.setParameter(OAuth20Constants.PASSWORD, "password");
        request.setParameter(OAuth20Constants.CLIENT_ID, "key");
        request.setParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.setParameter(OAuth20Constants.GRANT_TYPE,
                OAuth20Constants.GRANT_TYPE_PASSWORD);
        request.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                OAuth20Constants.HTTP_CONTENT_TYPE_FORM);

        StringWriter responseBuffer = new StringWriter();
        response.setWriter(responseBuffer);

        OAuthResult result = null;

        result = oauth20.processTokenRequest(null, request, response);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertNull(result.getCause());
    }

    public void testNoOAUTH20_GRANT_TYPE_REFRESH_TOKEN() {
        // Start out with the full auth code path to get the refresh token, then
        // try to refresh it

        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = {
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_AUTH_CODE,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_IMPLICIT,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_OWNER_PASSWORD };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

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

        request = new MockServletRequest();
        response = new MockServletResponse();

        request.setParameter(OAuth20Constants.CLIENT_ID, "key");
        request.setParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.setParameter(OAuth20Constants.GRANT_TYPE,
                OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN);
        request.setParameter(OAuth20Constants.REFRESH_TOKEN, refresh_token);
        request.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                OAuth20Constants.HTTP_CONTENT_TYPE_FORM);

        responseBuffer = new StringWriter();
        response.setWriter(responseBuffer);

        result = oauth20.processTokenRequest(null, request, response);

        assertTrue(result.getStatus() != OAuthResult.STATUS_OK);
        assertTrue(result.getCause() instanceof OAuthConfigurationException);
    }

    public void testOAUTH20_GRANT_TYPE_REFRESH_TOKEN() {
        // Start out with the full auth code path to get the refresh token, then
        // refresh it

        BaseConfig config = new BaseConfig();
        String[] GRANT_TYPES_ALLOWED = {
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_AUTH_CODE,
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPE_REFRESH_TOKEN };
        config.putConfigPropertyValues(
                OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED,
                GRANT_TYPES_ALLOWED);

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

        boolean refreshUsed = false;

        for (Attribute a : result.getAttributeList().getAllAttributes()) {
            if (a.toString().equals(OAuth20Constants.REFRESH_TOKEN))
                refreshUsed = true;
        }

        if (refreshUsed) {
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

            request = new MockServletRequest();
            response = new MockServletResponse();

            request.setParameter(OAuth20Constants.CLIENT_ID, "key");
            request.setParameter(OAuth20Constants.CLIENT_SECRET, "secret");
            request.setParameter(OAuth20Constants.GRANT_TYPE,
                    OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN);
            request.setParameter(OAuth20Constants.REFRESH_TOKEN, refresh_token);
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
        }
    }

}
