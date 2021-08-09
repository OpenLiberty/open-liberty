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

import static com.ibm.oauth.core.internal.oauth20.OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.test.base.BaseConfig;
import com.ibm.oauth.core.test.base.BaseTestCase;
import com.ibm.oauth.core.test.base.MockServletRequest;
import com.ibm.oauth.core.test.base.MockServletResponse;
import com.ibm.ws.security.oauth20.web.ClientAuthorization;

/*
 * Test case flows reused from Shindig's OAuth provider to validate spec compliance
 */

public class OAuth2ResourceOwnerPasswordCredentialsFlowTest extends
                BaseTestCase {

    final String MAPKEY_OAUTHRESULT = "MAPKEY_OAUTHRESULT";
    final String MAPKEY_REPONSETEXT = "MAPKEY_REPONSETEXT";

    Map<String, Object> baseResourceOwnerPasswordCredentialsFlowTest(
                                                                     String clientId, String clientSecret, String username,
                                                                     String password, String scope) {
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);

        MockServletRequest req = new MockServletRequest();
        req.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                      "application/x-www-form-urlencoded");
        if (clientId != null) {
            req.setParameter("client_id", clientId);
        }
        if (clientSecret != null) {
            req.setParameter("client_secret", clientSecret);
        }
        if (username != null) {
            req.setParameter("username", username);
        }
        if (password != null) {
            req.setParameter("password", password);
        }
        if (scope != null) {
            req.setParameter("scope", scope);
        }

        // handle scope on client_credentials oauth20 request
        AttributeList attrList = new AttributeList();
        String[] scopes = scope == null ? new String[0] : scope.split(" ");
        ClientAuthorization auth2Leg = new ClientAuthorization();
        attrList.setAttribute(OAuth20Constants.SCOPE,
                              ATTRTYPE_PARAM_OAUTH_REQUEST,
                              auth2Leg.getUniqueArray(scopes));
        req.setAttribute(OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, attrList);

        req.setParameter("grant_type", "password");
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

    void validateSuccessResults(Map<String, Object> results) {
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

    /**
     * Test normal resource owner password cred flow
     * 
     * @throws Exception
     */
    public void testResourceOwnerPasswordCredentialsFlowParams()
                    throws Exception {

        Map<String, Object> results = baseResourceOwnerPasswordCredentialsFlowTest(
                                                                                   "key", "secret", "user1", "pass1", "scope1 scope2");
        validateSuccessResults(results);
    }

    /**
     * Test normal resource owner password cred flow with numeric scope
     * 
     * @throws Exception
     */
    public void testNumericScopeString() throws Exception {

        Map<String, Object> results = baseResourceOwnerPasswordCredentialsFlowTest(
                                                                                   "key", "secret", "user1", "pass1", "123");
        validateSuccessResults(results);
        String responseString = (String) results.get(MAPKEY_REPONSETEXT);

        // check compulsory response params
        JSONObject json = JSONObject.parse(responseString);

        // if scope is returned, make sure it was returned as a string
        if (json.containsKey("scope")) {
            Object obj = json.get("scope");
            String className = obj.getClass().getName();
            assertEquals(className, String.class.getName());
        }
    }

    /**
     * Test normal resource owner password cred flow and if multiple scopes of
     * the same value were requested that the returned access token only
     * contains the scope value once.
     * 
     * @throws Exception
     */
    public void testMultipleScopeString() throws Exception {

        String[] expectedScopes = new String[] { "scope1", "scope2" };
        Set<String> expectedScopeSet = new HashSet<String>(Arrays.asList(expectedScopes));
        Map<String, Object> results = baseResourceOwnerPasswordCredentialsFlowTest(
                                                                                   "key", "secret", "user1", "pass1", "scope1 scope2 scope1");
        validateSuccessResults(results);
        String responseString = (String) results.get(MAPKEY_REPONSETEXT);

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
