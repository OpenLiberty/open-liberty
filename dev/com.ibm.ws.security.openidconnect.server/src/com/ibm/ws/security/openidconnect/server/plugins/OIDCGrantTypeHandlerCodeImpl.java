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
package com.ibm.ws.security.openidconnect.server.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InternalException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.granttype.impl.OAuth20GrantTypeHandlerCodeImpl;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.common.cl.BuildResponseTypeUtil;

public class OIDCGrantTypeHandlerCodeImpl extends OAuth20GrantTypeHandlerCodeImpl {

    private static final String CLASS = OIDCGrantTypeHandlerCodeImpl.class.getName();
    private static Logger log = Logger.getLogger(CLASS);

    @Override
    public void validateRequestGrantType(AttributeList attributeList, List<OAuth20Token> tokens) throws OAuthException {
        super.validateRequestGrantType(attributeList, tokens);
        String[] scopes = getScopesFromAuthorizationCode(tokens);
        if (hasOpenIDScope(scopes)) {
            String issuerIdentifier = attributeList.getAttributeValueByName(OAuth20Constants.ISSUER_IDENTIFIER);
            if (issuerIdentifier == null || issuerIdentifier.isEmpty()) {
                throw new OAuth20InternalException("security.oauth20.error.token.internal.missing.issuer",
                                new Throwable("Missing " + OAuth20Constants.ISSUER_IDENTIFIER));
            }
        }
    }

    private String[] getScopesFromAuthorizationCode(List<OAuth20Token> tokens) {
        String[] scopes = null;
        if (tokens.size() >= 1) {
            OAuth20Token code = (OAuth20Token) tokens.get(0);
            scopes = code.getScope();
        }
        return scopes;
    }

    private boolean hasOpenIDScope(String[] scopes) {
        boolean result = false;
        if (scopes != null) {
            for (String scope : scopes) {
                if (OIDCConstants.SCOPE_OPENID.equals(scope)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public List<OAuth20Token> buildTokensGrantType(AttributeList attributeList, OAuth20TokenFactory tokenFactory, List<OAuth20Token> tokens) {
        String methodName = "buildTokensGrantType";
        log.entering(CLASS, methodName);

        // The super.buildTokenGrantType ought to handle accessToken and refreshToken already
        List<OAuth20Token> tokenList = super.buildTokensGrantType(attributeList, tokenFactory, tokens);

        try {
            String requestType = attributeList.getAttributeValueByNameAndType(OAuth20Constants.REQUEST_FEATURE, OAuth20Constants.ATTRTYPE_REQUEST);
            // id_token is added if a request type is oidc.
            if (OAuth20Constants.REQUEST_FEATURE_OIDC.equals(requestType)) {
                // index 0 in the token list should be the authorization code
                if (tokens.size() >= 1) {
                    OAuth20Token code = (OAuth20Token) tokens.get(0);

                    if (code != null) {
                        String clientId = attributeList.getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                        String redirectUri = attributeList.getAttributeValueByNameAndType(OAuth20Constants.REDIRECT_URI, OAuth20Constants.ATTRTYPE_PARAM_BODY);
                        String[] scopes = code.getScope();
                        String username = code.getUsername();
                        String stateId = code.getStateId();
                        if (scopes != null) {
                            for (String scope : scopes) {
                                if (OIDCConstants.SCOPE_OPENID.equals(scope)) {
                                    if (tokenList == null) {
                                        tokenList = new ArrayList<OAuth20Token>();
                                    }
                                    IDTokenFactory idTokenFactory = new IDTokenFactory(tokenFactory.getOAuth20ComponentInternal());
                                    Map<String, String[]> idTokenMap =
                                                    idTokenFactory.buildTokenMap(clientId, username, redirectUri, stateId, scopes, code,
                                                                                 OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE);
                                    BuildResponseTypeUtil.putAccessTokenInMap(idTokenMap, tokenList);
                                    BuildResponseTypeUtil.putIssuerIdentifierInMap(idTokenMap, attributeList);

                                    OAuth20Token id = idTokenFactory.createIDToken(idTokenMap);

                                    if (id != null) {
                                        tokenList.add(id);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            log.exiting(CLASS, methodName);
        }

        return tokenList;
    }

    public void buildResponseGrantType(AttributeList attributeList, List<OAuth20Token> tokens) {
        String methodName = "buildResponseGrantType";
        log.entering(CLASS, methodName);
        BuildResponseTypeUtil.buildResponseGrantType(attributeList, tokens);
    }

}
