/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import java.util.List;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerRefreshImpl;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.ws.security.openidconnect.common.cl.BuildResponseTypeUtil;

public class OIDCGrantTypeHandlerRefreshImpl extends OAuth20GrantTypeHandlerRefreshImpl {

    private static final String CLASS = OIDCGrantTypeHandlerRefreshImpl.class.getName();
    private static Logger log = Logger.getLogger(CLASS);

    public OIDCGrantTypeHandlerRefreshImpl(OAuth20ConfigProvider config) {
        super(config);
    }

    @Override
    public List<OAuth20Token> buildTokensGrantType(AttributeList attributeList, OAuth20TokenFactory tokenFactory, List<OAuth20Token> tokens) {
        String methodName = "buildTokensGrantType";
        log.entering(CLASS, methodName);
        List<OAuth20Token> tokenList = super.buildTokensGrantType(attributeList, tokenFactory, tokens);

        // Do not delete this piece of commented-out of code. 
        // We may want to come back this part of code later.
        // See the description in task 118913
        //try {
        //    // index 0 in the token list should be the refresh token
        //    if (tokens.size() >= 1) {
        //        OAuth20Token refresh = (OAuth20Token) tokens.get(0);
        //
        //        if (refresh != null) {
        //            String clientId = attributeList.getAttributeValueByName(OAuth20Constants.CLIENT_ID);
        //            String username = refresh.getUsername();
        //            String redirectUri = refresh.getRedirectUri();
        //            String[] origScope = refresh.getScope();
        //            String[] requestScope = attributeList.getAttributeValuesByNameAndType(OAuth20Constants.SCOPE, OAuth20Constants.ATTRTYPE_PARAM_BODY);
        //
        //            /*
        //             * if scope isn't requested, grant previous scope per
        //             * http://tools.ietf.org/html/draft-ietf-oauth-v2#section-6
        //             */
        //            if (requestScope == null || requestScope.length == 0) {
        //                requestScope = origScope;
        //            }
        //            for (String scope : requestScope) {
        //                if (OIDCConstants.SCOPE_OPENID.equals(scope)) {
        //                    IDTokenFactory idTokenFactory = new IDTokenFactory(tokenFactory.getOAuth20ComponentInternal());
        //
        //                    String stateId = refresh.getStateId();
        // 
        //                    if (tokenList == null) {
        //                        tokenList = new ArrayList<OAuth20Token>();
        //                    }
        //
        //                    Map<String, String[]> idTokenMap = idTokenFactory.buildTokenMap(clientId, username, redirectUri, stateId, requestScope, refresh);
        //                    for (OAuth20Token token : tokenList) {
        //                        String strTokenType = token.getType();
        //                        if (OAuth20Constants.ACCESS_TOKEN.equals(strTokenType)) {
        //                            String accessTokenString = token.getTokenString();
        //                            idTokenMap.put(OAuth20Constants.ACCESS_TOKEN, new String[] { accessTokenString });
        //                            break;
        //                        }
        //                    }
        //                    
        //                    String issuerIdentifier = attributeList.getAttributeValueByName("issuerIdentifier");
        //                    idTokenMap.put("issuerIdentifier", new String[] { issuerIdentifier });
        //                    
        //                    OAuth20Token idtoken = idTokenFactory.createIDToken(idTokenMap);
        //                    if (idtoken != null) {
        //                        tokenList.add(idtoken);
        //                    }
        //                    break;
        //                }
        //
        //            }
        // 
        //        }
        //    }
        //} finally {
        //    log.exiting(CLASS, methodName);
        //}

        return tokenList;
    }

    public void buildResponseGrantType(AttributeList attributeList, List<OAuth20Token> tokens) {
        String methodName = "buildResponseGrantType";
        log.entering(CLASS, methodName);

        BuildResponseTypeUtil.buildResponseGrantType(attributeList, tokens);
    }
}
