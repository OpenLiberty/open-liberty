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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigurationImpl;
import com.ibm.oauth.core.test.base.BaseConfig;
import com.ibm.oauth.core.test.base.BaseTestCase;
import com.ibm.oauth.core.test.base.MockPrincipal;
import com.ibm.oauth.core.test.base.MockServletRequest;
import com.ibm.oauth.core.test.base.MockServletResponse;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

public class OAuth2ImplicitFlowTest extends BaseTestCase {

    /**
     * Test retrieving an access token using a public client with a redirect uri
     *
     * @throws Exception
     */
    public void testGetAccessTokenWithRedirectParamAndState() throws Exception {
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = "http://localhost:9080/oauth/client.jsp";
        String responseType = "token";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "PRESERVEME";
        MockServletResponse response = new MockServletResponse();

        OAuthResult result = null;

        result = oauth20.processAuthorization(username, clientId, redirectUri, responseType, state, scope, response);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());
        assertTrue(result.getAttributeList().getAttributeValueByName("state").equals(state));
        assertNotNull(result.getAttributeList().getAttributeValueByName("access_token"));
        assertTrue(result.getAttributeList().getAttributeValueByName("token_type").equals("Bearer"));

    }

    /**
     * Test retrieving an access token using a public client with redirect uri
     *
     * @throws Exception
     */
    public void testGetAccessTokenNoRedirectParam() throws Exception {
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = null;// "http://localhost:9080/oauth/client.jsp";
        String responseType = "token";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse response = new MockServletResponse();

        OAuthResult result = null;

        result = oauth20.processAuthorization(username, clientId, redirectUri, responseType, state, scope, response);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());
        assertNotNull(result.getAttributeList().getAttributeValueByName("access_token"));
        assertTrue(result.getAttributeList().getAttributeValueByName("token_type").equals("Bearer"));
    }

    /**
     * Test attempting to retrieve an access token using a bad redirect URI
     *
     * @throws Exception
     */
    public void testGetAccessTokenWithBadRedirect() throws Exception {
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = "bad_redirect";
        String responseType = "token";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse response = new MockServletResponse();

        OAuthResult result = null;

        result = oauth20.processAuthorization(username, clientId, redirectUri, responseType, state, scope, response);
        // should fail, bad redirect
        assertEquals(result.getStatus(), OAuthResult.STATUS_FAILED);
        assertNotNull(result.getCause());
    }

    /**
     * Test attempting to retrieve an access token using a bad client id
     *
     * @throws Exception
     */
    public void testGetAccessTokenWithBadClientID() throws Exception {
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "bad_client_id";
        String redirectUri = "http://localhost:9080/oauth/client.jsp";
        String responseType = "token";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse response = new MockServletResponse();

        OAuthResult result = null;

        result = oauth20.processAuthorization(username, clientId, redirectUri, responseType, state, scope, response);
        // should fail, bad client it
        assertEquals(result.getStatus(), OAuthResult.STATUS_FAILED);
        assertNotNull(result.getCause());
    }

    /**
     * Test retrieving an access token and if multiple scopes of the same value
     * were requested that the returned access token only contains the scope
     * value once.
     *
     * @throws Exception
     */
    public void testMultipleScopeString() throws Exception {
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = "http://localhost:9080/oauth/client.jsp";
        String responseType = "token";
        String[] scope = new String[] { "scope1", "scope2", "scope1" };
        String[] expectedScopes = new String[] { "scope1", "scope2" };
        Set<String> expectedScopeSet = new HashSet<String>(Arrays.asList(expectedScopes));
        String state = "PRESERVEME";
        MockServletResponse response = new MockServletResponse();

        OAuthResult result = null;

        result = oauth20.processAuthorization(username, clientId, redirectUri, responseType, state, scope, response);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());
        assertTrue(result.getAttributeList().getAttributeValueByName("state").equals(state));
        assertNotNull(result.getAttributeList().getAttributeValueByName("access_token"));
        assertTrue(result.getAttributeList().getAttributeValueByName("token_type").equals("Bearer"));

        // if scope is returned, make sure it didn't return multiple scopes with the same value
        String[] scopes = result.getAttributeList().getAttributeValuesByName("scope");
        if (scopes != null) {
            assertEquals(2, scopes.length);
            Set<String> scopeSet = new HashSet<String>(Arrays.asList(scopes));
            for (int i = 0; i < expectedScopes.length; i++) {
                assertTrue(scopeSet.contains(expectedScopes[i]));
            }
            assertEquals(expectedScopeSet, scopeSet);
        }
    }

    /**
     * Test retrieving an access token using a public client with a redirect uri
     * for new processAuthorization method.
     *
     * @throws Exception
     */
    public void testGetAccessTokenNoScopeForNewProcessAuthorization() throws Exception {
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = "http://localhost:9080/oauth/client.jsp";
        String responseType = "token";
        String state = "PRESERVEME";
        MockServletResponse response = new MockServletResponse();
        MockServletRequest request = new MockServletRequest();

        request.setUserPrincipal(new MockPrincipal(username));
        request.setParameter(OAuth20Constants.CLIENT_ID, clientId);
        request.setParameter(OAuth20Constants.REDIRECT_URI, redirectUri);
        request.setParameter(OAuth20Constants.RESPONSE_TYPE, responseType);
        request.setParameter(OAuth20Constants.STATE, state);

        OAuthResult result = null;

        result = oauth20.processAuthorization(request, response, null);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());
        assertTrue(result.getAttributeList().getAttributeValueByName("state").equals(state));
        assertNotNull(result.getAttributeList().getAttributeValueByName("access_token"));
        assertTrue(result.getAttributeList().getAttributeValueByName("token_type").equals("Bearer"));

    }

    /**
     * Test retrieving an access token using a public client with a redirect uri
     * for new processAuthorization method.
     *
     * @throws Exception
     */
    public void testGetAccessTokenNoStateForNewProcessAuthorization() throws Exception {
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = "http://localhost:9080/oauth/client.jsp";
        String responseType = "token";
        String scope = "openid profile";
        // String state = "PRESERVEME";
        MockServletResponse response = new MockServletResponse();
        MockServletRequest request = new MockServletRequest();

        request.setUserPrincipal(new MockPrincipal(username));
        request.setParameter(OAuth20Constants.CLIENT_ID, clientId);
        request.setParameter(OAuth20Constants.REDIRECT_URI, redirectUri);
        request.setParameter(OAuth20Constants.RESPONSE_TYPE, responseType);
        request.setParameter(OAuth20Constants.SCOPE, scope);

        OAuthResult result = null;

        result = oauth20.processAuthorization(request, response, null);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());
        assertNotNull(result.getAttributeList().getAttributeValueByName("access_token"));
        assertTrue(result.getAttributeList().getAttributeValueByName("token_type").equals("Bearer"));

    }

    /**
     * Test retrieving an access token using a public client with a redirect uri
     * for new processAuthorization method.
     *
     * @throws Exception
     */
    // TODO - this is failing and need to figure out why
    public void _testResponseTypeOfOpenIdForNewProcessAuthorization() throws Exception {
        BaseConfig config = new BaseConfig();
        config.putConfigPropertyValues(OAuth20ConfigurationImpl.OAUTH20_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME,
                new String[] { OIDCConstants.DEFAULT_OIDC10_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME });

        initializeOAuthFramework(config);

        String username = "testuser";
        String clientId = "key";
        String redirectUri = "http://localhost:9080/oauth/client.jsp";
        String responseType = "id_token token";
        String scope = "openid profile";
        String state = "PRESERVEME";
        AttributeList options = new AttributeList();
        MockServletResponse response = new MockServletResponse();
        MockServletRequest request = new MockServletRequest();

        request.setUserPrincipal(new MockPrincipal(username));
        request.setParameter(OAuth20Constants.CLIENT_ID, clientId);
        request.setParameter(OAuth20Constants.REDIRECT_URI, redirectUri);
        request.setParameter(OAuth20Constants.RESPONSE_TYPE, responseType);
        request.setParameter(OAuth20Constants.STATE, state);
        request.setParameter(OAuth20Constants.SCOPE, scope);

        options.setAttribute(OAuth20Constants.STATE, OAuth20Constants.ATTRTYPE_PARAM_QUERY, new String[] { state });
        options.setAttribute(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, OAuth20Constants.ATTRTYPE_REQUEST, new String[] { "noncevalue2" });
        options.setAttribute(OAuth20Constants.SCOPE, OAuth20Constants.ATTRTYPE_PARAM_QUERY, new String[] { "openid profile" });

        OAuthResult result = null;

        result = oauth20.processAuthorization(request, response, options);

        assertNotNull(result);
        if (result.getStatus() != OAuthResult.STATUS_OK) {
            result.getCause().printStackTrace();
            fail();
        }
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());
        assertTrue(result.getAttributeList().getAttributeValueByName("state").equals(state));
        assertNotNull(result.getAttributeList().getAttributeValueByName("access_token"));
        assertTrue(result.getAttributeList().getAttributeValueByName("token_type").equals("Bearer"));

    }

}
