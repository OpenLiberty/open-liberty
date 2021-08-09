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

import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.test.base.BaseTestCase;
import com.ibm.oauth.core.test.base.MockServletRequest;
import com.ibm.oauth.core.test.base.MockServletResponse;

/*
 * Simple end-to-end functional tests to exercise OAuth20Component APIs
 */
public class OAuth20ComponentTest extends BaseTestCase {

    public void testSimpleProcessAuthorization() {
        String username = "testuser";
        String clientId = "key";
        String redirectUri = null;// "http://localhost:9080/snoop";
        String responseType = "code";
        String[] scope = new String[] { "scope1", "scope2" };
        String state = "";
        MockServletResponse response = new MockServletResponse();

        OAuthResult result = null;

        result = oauth20.processAuthorization(username, clientId, redirectUri,
                                              responseType, state, scope, response);

        assertNotNull(result);
        assertEquals(result.getStatus(), OAuthResult.STATUS_OK);
        assertNull(result.getCause());
    }

    public void testSimpleProcessTokenRequest() {
        String responseString = makeAccessTokenRequest();
        assertNotNull(responseString);

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

        String token = extractAccessToken(responseString);
        assertNotNull(token);
        assertTrue(token.length() > 5);
    }

    public void testSimplePocessResourceRequestAttributes() {

        String responseString = makeAccessTokenRequest();
        String token = extractAccessToken(responseString);
        assertNotNull(token);

        AttributeList attrs = new AttributeList();
        attrs.setAttribute("host", null, new String[] { "localhost" });
        attrs.setAttribute("port", null, new String[] { "9080" });
        attrs.setAttribute("method", null, new String[] { "GET" });
        attrs.setAttribute("path", null, new String[] { "/oauth" });
        attrs.setAttribute("scheme", null, new String[] { "http" });
        attrs.setAttribute("access_token", null, new String[] { token });

        OAuthResult result = null;

        result = oauth20.processResourceRequest(attrs);

        assertNotNull(result);
        assertEquals(OAuthResult.STATUS_OK, result.getStatus());
        assertNull(result.getCause());
    }

    public void testSimplePocessResourceRequestServlet() {

        String responseString = makeAccessTokenRequest();
        String token = extractAccessToken(responseString);
        assertNotNull(token);

        MockServletRequest request = new MockServletRequest();

        request.setHeader(OAuth20Constants.HTTP_HEADER_AUTHORIZATION, "Bearer "
                                                                      + token);

        OAuthResult result = null;

        result = oauth20.processResourceRequest(request);

        assertNotNull(result);
        assertEquals(OAuthResult.STATUS_OK, result.getStatus());
        assertNull(result.getCause());

    }

}
