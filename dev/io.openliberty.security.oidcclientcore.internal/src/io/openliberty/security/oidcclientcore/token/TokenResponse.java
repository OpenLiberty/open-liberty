/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    public TokenResponse(JSONObject rawResponse) {
        this.responseGenerationTime = Instant.now();

        Map<String, String> tokens = getTokensFromJson(rawResponse);
        this.rawResponse = rawResponse;
        this.idTokenString = tokens.get(TokenConstants.ID_TOKEN);
        this.accessTokenString = tokens.get(TokenConstants.ACCESS_TOKEN);
        this.refreshTokenString = tokens.get(TokenConstants.REFRESH_TOKEN);
    }

    private Map<String, String> getTokensFromJson(JSONObject json) {
        Map<String, String> tokens = new HashMap<>();
        List<String> tokenTypes = Arrays.asList(TokenConstants.TOKEN_TYPES);

        @SuppressWarnings("unchecked")
        Iterator<String> iterator = json.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = json.get(key);
            if (value instanceof String && tokenTypes.contains(key)) {
                tokens.put(key, value.toString());
            }
        }

        return tokens;
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
