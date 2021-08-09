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
package com.ibm.oauth.core.test.base;

import java.io.IOException;
import java.io.StringWriter;

import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.OAuthComponentFactory;
import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.oauth20.OAuth20Component;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;

import junit.framework.TestCase;

public abstract class BaseTestCase extends TestCase {

    protected OAuth20Component oauth20 = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        BaseConfig config = new BaseConfig();
        initializeOAuthFramework(config);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        oauth20 = null;
    }

    protected void initializeOAuthFramework(OAuthComponentConfiguration config) {
        OAuthComponentInstance componentInstance = null;
        try {
            componentInstance = OAuthComponentFactory.getOAuthComponentInstance(config);
        } catch (OAuthException oae) {
            oae.printStackTrace();
            fail(oae.getMessage());
        }
        oauth20 = componentInstance.getOAuth20Component();

    }

    protected String makeAccessTokenRequest() {
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
        return responseBuffer.toString();
    }

    // Input format:
    // {"access_token":"6YSYj0uCvL3O6fnFvQRy7oMt9MhpMipy4mzrhvD4","token_type":
    // "bearer","expires_in":3600}
    protected String extractAccessToken(String jsonString) {
        String result = null;
        try {
            JSONObject json = JSONObject.parse(jsonString);
            result = (String) json.get("access_token");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    // Input format:
    // {"access_token":"peO8jC2JmFXaWAbIPFhos0LKNFWUfylkH81KPGoc","token_type":
    // "bearer","expires_in":3600,"scope":"scope1 scope2","refresh_token":
    // "TFEoA9fOhQ3GFjohJ1QksUKlmE7mei0iXlUJKsfrl1FRnzjPdg"}
    protected String extractRefreshToken(String jsonString) {
        String result = null;
        try {
            JSONObject json = JSONObject.parse(jsonString);
            if (json.containsKey("refresh_token")) {
                result = (String) json.get("refresh_token");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public OAuthResult processResourceRequestAttributes() throws OAuthException {
        String json = makeAccessTokenRequest();
        String token = extractAccessToken(json);
        assertNotNull(token);

        AttributeList attrs = new AttributeList();
        attrs.setAttribute("host", null, new String[] { "localhost" });
        attrs.setAttribute("port", null, new String[] { "9080" });
        attrs.setAttribute("method", null, new String[] { "GET" });
        attrs.setAttribute("path", null, new String[] { "/oauth" });
        attrs.setAttribute("scheme", null, new String[] { "http" });
        attrs.setAttribute("access_token", null, new String[] { token });

        OAuthResult result = oauth20.processResourceRequest(attrs);
        return result;
    }

    protected String[] getScopesFromResponseString(String responseString) throws Exception {
        String[] result = null;
        JSONObject json = JSONObject.parse(responseString);
        if (json.containsKey("scope")) {
            Object obj = json.get("scope");
            if (obj != null) {
                String scopes = obj.toString();
                result = scopes.split(" ");
            }
        }
        return result;
    }

}
