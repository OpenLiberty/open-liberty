/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.tokenintrospectprovider;

import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.security.oauth20.AuthnContext;
import com.ibm.wsspi.security.oauth20.TokenIntrospectProvider;

public class OAuth20TokenIntrospectProvider implements TokenIntrospectProvider {
    private final Properties properties = new Properties();
    static final String client_id = "client_id";
    static String resolverId = null;
    String userName = null;

    public OAuth20TokenIntrospectProvider(Dictionary<String, Object> serviceProps) {
        saveDictionary(serviceProps);
    }

    void saveDictionary(Dictionary<String, ?> original) {
        Enumeration<String> keys = original.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            properties.put(key, original.get(key));
        }
        resolverId = (String) original.get("id");
    }

    /** {@inheritDoc} */
    @Override
    public String getUserInfo(AuthnContext authnContext) {
        if (authnContext.getUserName().equals("user2"))
            return null;
        if (authnContext.getUserName().equals("user3"))
            return "{]}]"; // not json format string
        if (authnContext.getUserName().equals("user4"))
            return "{\"error\":\"Error in user4\"}"; //user returns their own error message
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("access_token", authnContext.getAccessToken());
        jsonObject.put("created_at", new Date(authnContext.getCreatedAt()).toString());
        jsonObject.put("expires_in", authnContext.getExpiresIn());
        jsonObject.put("user_name", authnContext.getUserName());
        String[] array = authnContext.getGrantedScopes();
        JSONArray scopes = new JSONArray();
        for (String scope : array) {
            scopes.add(scope);
        }
        jsonObject.put("scopes", scopes);
        JSONArray groupIds = new JSONArray();
        groupIds.add("customGroup");
        jsonObject.put("groupIds", groupIds);
        jsonObject.put("createdBy", "OAuth20TokenIntrospectProvider");
        return jsonObject.toString();
    }

    public String arrayToString(String[] array) {
        String result = "";
        int iCnt = 0;
        for (String string : array) {
            if (iCnt > 0) {
                result = result.concat(", ");
            }
            result = result.concat(string);
            iCnt++;
        }
        return result;
    }

}
