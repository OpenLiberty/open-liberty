/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.token.impl;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.ws.security.oauth20.util.UTC;
import com.ibm.wsspi.security.oauth20.token.WSOAuth20Token;

public class WSOAuth20TokenHelper {

    public static WSOAuth20Token createToken(HttpServletRequest req, HttpServletResponse res,
            OAuthResult oResult, String providerName) {
        WSOAuth20TokenImpl oauthToken = new WSOAuth20TokenImpl();

        String token = null;
        String expireUTC = null;
        Date expire = null;

        AttributeList attributeList = oResult.getAttributeList();

        String clientId = attributeList.getAttributeValueByName(OAuth20Constants.OAUTH_TOKEN_CLIENT_ID);
        oauthToken.setClientID(clientId);

        String username = attributeList.getAttributeValueByName(OAuth20Constants.USERNAME);
        oauthToken.setUser(username);

        String[] scope = attributeList
                .getAttributeValuesByName(OAuth20Constants.SCOPE);
        oauthToken.setScope(scope);

        token = attributeList.getAttributeValueByName(OAuth20Constants.ACCESS_TOKEN);
        oauthToken.setTokenString(token);

        expireUTC = attributeList.getAttributeValueByName(OAuth20Constants.RESPONSEATTR_EXPIRES);
        try {
            expire = UTC.parse(expireUTC);
            oauthToken.setExpirationTime(expire.getTime());
        } catch (Exception e) {

        }

        oauthToken.setProvider(providerName);

        int hash = token.hashCode();
        String cacheKey = (new StringBuffer()).append(providerName).append(hash).toString();
        oauthToken.setCacheKey(cacheKey);

        return oauthToken;
    }
}
