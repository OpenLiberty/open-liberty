/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;

public class TokenKeeper {

    protected static Class<?> thisClass = TokenKeeper.class;

    public static CommonValidationTools validationTools = new CommonValidationTools();

    private String access_token = null;
    private String refresh_token = null;
    private String id_token = null;

    public TokenKeeper(Object response, String clientType) throws Exception {

        access_token = getAccessTokenFromResponse(response);
        refresh_token = getRefreshTokenFromResponse(response, clientType);
        id_token = getIdTokenFromResponse(response);

    }

    private String getRefreshTokenFromResponse(Object response, String clientType) throws Exception {

        String refreshKey = Constants.REFRESH_TOKEN_KEY;
        if (clientType.contains(SocialConstants.SOCIAL)) {
            refreshKey = "refresh token";
        }

        String refreshToken = validationTools.getTokenFromResponse(response, refreshKey);
        Log.info(thisClass, "getRefreshToken", "Refresh Token: " + refreshToken);
        return refreshToken;

    }

    private String getIdTokenFromResponse(Object response) throws Exception {

        String idToken = validationTools.getTokenFromResponse(response, Constants.ID_TOKEN_KEY);
        Log.info(thisClass, "getIdToken", "id_token:  " + idToken);
        return idToken;

    }

    private String getAccessTokenFromResponse(Object response) throws Exception {

        String accessToken = validationTools.getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY);
        Log.info(thisClass, "getAccessToken", "access_token:  " + accessToken);
        return accessToken;

    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public String getIdToken() {
        return id_token;
    }

    public String getAccessToken() {
        return access_token;
    }

}
