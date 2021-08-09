/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.common.cl;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.ibm.oauth.core.api.attributes.AttributeList;

import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class BuildResponseTypeUtil {
    private static final TraceComponent tc = Tr.register(BuildResponseTypeUtil.class);
    private static final String CLASS = BuildResponseTypeUtil.class.getName();
    private static Logger log = Logger.getLogger(CLASS);

    public static void buildResponseGrantType(AttributeList attributeList, List<OAuth20Token> tokens) {
        String methodName = "buildResponseGrantType";
        log.entering(CLASS, methodName);

        try {
            for (OAuth20Token token : tokens) {
                String strTokenType = token.getType();
                if (OAuth20Constants.ACCESS_TOKEN.equals(strTokenType)) {
                    handleAccessToken(attributeList, token);
                } else if ("authorization_grant".equals(strTokenType)) {
                    handleRefreshToken(attributeList, token);
                } else if (OIDCConstants.ID_TOKEN.equals(strTokenType)) {
                    handleIDToken(attributeList, token);
                } else {
                    // tr.error
                    log.logp(Level.FINEST, CLASS, methodName, "Unknown token type:'" + strTokenType + "'");
                }
            }
        } finally {
            log.exiting(CLASS, methodName);
        }
    }

    public static final void handleAccessToken(AttributeList attributeList, OAuth20Token token) {
        String accessToken = token.getTokenString();
        attributeList.setAttribute(OAuth20Constants.ACCESS_TOKEN, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { accessToken });

        String accessTokenId = token.getId();
        attributeList.setAttribute(OAuth20Constants.ACCESS_TOKEN_ID, OAuth20Constants.ATTRTYPE_RESPONSE_META, new String[] { accessTokenId });

        String type = token.getSubType();
        attributeList.setAttribute(OAuth20Constants.TOKEN_TYPE, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { type });

        String expires = OAuth20TokenHelper.expiresInSeconds(token);
        attributeList.setAttribute(OAuth20Constants.EXPIRES_IN, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { expires });

        String stateId = token.getStateId();
        attributeList.setAttribute(OAuth20Constants.STATE_ID, OAuth20Constants.ATTRTYPE_RESPONSE_STATE, new String[] { stateId });

        String[] scope = token.getScope();
        attributeList.setAttribute(OAuth20Constants.SCOPE, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, scope);
    }

    public static final void handleRefreshToken(AttributeList attributeList, OAuth20Token refresh) {
        String refreshToken = refresh.getTokenString();
        attributeList.setAttribute(OAuth20Constants.REFRESH_TOKEN, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { refreshToken });

        String refreshTokenId = refresh.getId();
        attributeList.setAttribute(OAuth20Constants.REFRESH_TOKEN_ID, OAuth20Constants.ATTRTYPE_RESPONSE_META, new String[] { refreshTokenId });
    }

    public static final void handleIDToken(AttributeList attributeList, OAuth20Token idtoken) {
        String strIdToken = idtoken.getTokenString();
        attributeList.setAttribute(OIDCConstants.ID_TOKEN, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { strIdToken });
    }

    public static void putAccessTokenInMap(Map<String, String[]> idTokenMap, List<OAuth20Token> tokenList) {
        for (OAuth20Token token : tokenList) {
            String strTokenType = token.getType();
            if (OAuth20Constants.ACCESS_TOKEN.equals(strTokenType)) {
                String accessTokenString = token.getTokenString();
                idTokenMap.put(OAuth20Constants.ACCESS_TOKEN, new String[] { accessTokenString });
                break;
            }
        }
    }

    public static void putIssuerIdentifierInMap(Map<String, String[]> idTokenMap, AttributeList attributeList) {
        String issuerIdentifier = attributeList.getAttributeValueByName("issuerIdentifier");
        idTokenMap.put("issuerIdentifier", new String[] { issuerIdentifier });
    }
}
