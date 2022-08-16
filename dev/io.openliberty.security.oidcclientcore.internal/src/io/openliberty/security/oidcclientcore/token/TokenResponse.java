/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.json.java.JSONObject;

public class TokenResponse {

    private final JSONObject rawResponse;
    private final String idToken;
    private final String accessToken;
    private final String refreshToken;

    private Map<String, String> responseAsMap = null;

    public TokenResponse(JSONObject rawResponse, String idToken, String accessToken, String refreshToken) {
        this.rawResponse = rawResponse;
        this.idToken = idToken;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> asMap() {
        if (responseAsMap != null) {
            return responseAsMap;
        }
        if (rawResponse == null) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        Set<String> keys = rawResponse.keySet();
        for (String key : keys) {
            map.put(key, rawResponse.get(key).toString());
        }
        responseAsMap = new HashMap<>(map);
        return map;
    }

}
