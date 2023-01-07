/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.json.java.JSONObject;

public class TokenResponse {

    private final JSONObject rawResponse;
    private final String idTokenString;
    private final String accessTokenString;
    private final String refreshTokenString;
    private final Instant responseGenerationTime;

    private Map<String, String> responseAsMap = null;

    public TokenResponse(JSONObject rawResponse, String idToken, String accessToken, String refreshToken) {
        this.rawResponse = rawResponse;
        this.idTokenString = idToken;
        this.accessTokenString = accessToken;
        this.refreshTokenString = refreshToken;
        this.responseGenerationTime = Instant.now();
    }

    public String getIdTokenString() {
        return idTokenString;
    }

    public String getAccessTokenString() {
        return accessTokenString;
    }

    public String getRefreshTokenString() {
        return refreshTokenString;
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

    public Instant getResponseGenerationTime() {
        return responseGenerationTime;
    }

}
