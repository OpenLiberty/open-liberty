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

import com.ibm.json.java.JSONObject;

public class TokenResponse {

    private final String idToken;
    private final String accessToken;
    private final String refreshToken;

    public TokenResponse(String idToken, String accessToken, String refreshToken) {
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

    public String toJson() {
        JSONObject json = new JSONObject();
        json.put(TokenConstants.ID_TOKEN, idToken);
        json.put(TokenConstants.ACCESS_TOKEN, accessToken);
        json.put(TokenConstants.REFRESH_TOKEN, refreshToken);
        return json.toString();
    }
}
