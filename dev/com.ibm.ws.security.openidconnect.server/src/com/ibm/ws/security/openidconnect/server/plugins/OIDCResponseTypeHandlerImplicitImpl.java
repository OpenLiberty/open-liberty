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

import com.google.gson.JsonArray;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InternalException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandler;
import com.ibm.oauth.core.internal.oauth20.responsetype.impl.OAuth20ResponseTypeHandlerTokenImpl;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.common.cl.BuildResponseTypeUtil;

public class OIDCResponseTypeHandlerImplicitImpl implements
                OAuth20ResponseTypeHandler {

    private static final TraceComponent tc = Tr.register(OIDCResponseTypeHandlerImplicitImpl.class, TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);
    OAuth20ResponseTypeHandlerTokenImpl oa20rthti = new OAuth20ResponseTypeHandlerTokenImpl();

    public void validateRequestResponseType(AttributeList attributeList, JsonArray redirectUris, boolean allowRegexpRedirects) throws OAuthException {

        String responseType = attributeList.getAttributeValueByNameAndType(
                                                                           OAuth20Constants.RESPONSE_TYPE,
                                                                           OAuth20Constants.ATTRTYPE_PARAM_QUERY);
        String[] responseTypes = responseType.split(" ");
        boolean bResponseTypeToken = false;
        boolean bResponseTypeIdToken = false;
        for (String rType : responseTypes) {
            if (rType.equals(OAuth20Constants.RESPONSE_TYPE_TOKEN)) {
                oa20rthti.validateRequestResponseType(attributeList, redirectUris, allowRegexpRedirects);
                bResponseTypeToken = true;
            } else if (rType.equals(OIDCConstants.RESPONSE_TYPE_ID_TOKEN)) {
                // when openid is specified in scopes than make sure issuerIdentifier is not null

                String issuerIdentifier = attributeList.getAttributeValueByName(OAuth20Constants.ISSUER_IDENTIFIER);
                if (issuerIdentifier == null || issuerIdentifier.length() == 0) {
                    // This should not happen. But it did happen when runtime code is not mature 
                    throw new OAuth20InternalException("security.oauth20.error.authorization.internal.missing.issuer",
                                    new Throwable("Missing " + OAuth20Constants.ISSUER_IDENTIFIER));
                }

                String[] scopes = attributeList.getAttributeValuesByName(OAuth20Constants.SCOPE);
                boolean bOpenIdScope = false;
                //TODO:
                for (String scope : scopes) {
                    if (OIDCConstants.SCOPE_OPENID.equals(scope)) {
                        //String issuerIdentifier = attributeList.getAttributeValueByName(OAuth20Constants.ISSUER_IDENTIFIER);
                        bOpenIdScope = true;
                        //if (issuerIdentifier == null || issuerIdentifier.length() == 0) {
                        ////TODO: this should have been caught already?
                        //// This should not happen. But it did happen when runtime code is not mature 
                        // throw new OAuth20InternalException(new Throwable( "Missing " + OAuth20Constants.ISSUER_IDENTIFIER));
                        //}
                    }
                }
                if (!bOpenIdScope) {
                    Tr.error(tc, "OIDC_SERVER_MISSING_OPENID_SCOPE_ERR");
                    // According to the OAuth2.0 spec, the error_description should not be translated (RFC 6749 OAuth 2.0 4.1.2.1)
                    throw new OIDCMissingScopeException(OAuth20Exception.INVALID_SCOPE, "'openid' should be specified as scope if the response_type is id_token", null);
                }
                // add nonce for oidc10. It's required in implicit
                String nonce = attributeList.getAttributeValueByNameAndType(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, OAuth20Constants.ATTRTYPE_REQUEST);
                if (nonce == null || nonce.length() == 0) {
                    //TODO: determine how can this be missed??
                    Tr.error(tc, "OIDC_SERVER_MISSING_NONCE_ATTR_ERR");
                    throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, null);
                }
                bResponseTypeIdToken = true;
            } else {
                Tr.error(tc, "OIDC_SERVER_INVALID_RESPONSE_TYPE_ERR", new Object[] { rType, "{'code', 'token', 'id_token token'}" });
                // According to the OAuth2.0 spec, the error_description should not be translated (RFC 6749 OAuth 2.0 4.1.2.1)
                throw new OIDCInvalidResponseTypeException(OAuth20Exception.UNSUPPORTED_RESPONSE_TPE, "response_type '" + responseType +
                                                                                                      "' is not supported", null);
            }
        }
        if (bResponseTypeIdToken && !bResponseTypeToken) { // This is not supported in phase 1
            // According to the OAuth2.0 spec, the error_description should not be translated (RFC 6749 OAuth 2.0 4.1.2.1)
            throw new OIDCUnsupportedResponseTypeException(OAuth20Exception.UNSUPPORTED_RESPONSE_TPE, "response_type id_token without response_type token is not supported for now", null);
        }
    }

    public List<OAuth20Token> buildTokensResponseType(AttributeList attributeList, OAuth20TokenFactory tokenFactory, String redirectUri) {
        String responseType = attributeList.getAttributeValueByNameAndType(
                                                                           OAuth20Constants.RESPONSE_TYPE,
                                                                           OAuth20Constants.ATTRTYPE_PARAM_QUERY);

        List<OAuth20Token> tokenList = null;
        tokenList = oa20rthti.buildTokensResponseType(attributeList, tokenFactory, redirectUri);
        if (responseType.contains("id_token")) {
            if (tokenList == null) {
                tokenList = new ArrayList<OAuth20Token>();
            }
            String clientId = attributeList.getAttributeValueByNameAndType(OAuth20Constants.CLIENT_ID, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            String username = attributeList.getAttributeValueByNameAndType(OAuth20Constants.USERNAME, OAuth20Constants.ATTRTYPE_REQUEST);
            String redirect = attributeList.getAttributeValueByNameAndType(OAuth20Constants.REDIRECT_URI, OAuth20Constants.ATTRTYPE_PARAM_QUERY);
            String[] scopes = attributeList.getAttributeValuesByName(OAuth20Constants.SCOPE);
            // if a redirect wasn't provided in the request, use the previously
            // registered one, everything else has been previously validated
            if (redirect == null) {
                redirect = redirectUri;
            }

            // Add TokenString of accessToken for IDToken to handle hash
            // It's supposed to be only one access_token in the tokenList
            OAuth20Token accessToken = null;
            for (OAuth20Token token : tokenList) {
                String strTokenType = token.getType();
                if (OAuth20Constants.ACCESS_TOKEN.equals(strTokenType)) {
                    accessToken = token;
                    break;
                }
            }
            if (accessToken != null) {
                String stateId = accessToken.getStateId();// id_token is using the same stateId as the access token (?)

                for (String scope : scopes) {
                    // We only generate the id_token when scopes defines an "openid" scope
                    if (OIDCConstants.SCOPE_OPENID.equals(scope)) {
                        IDTokenFactory oidc10TokenFactory = new IDTokenFactory(tokenFactory.getOAuth20ComponentInternal());
                        Map<String, String[]> idTokenMap =
                                        oidc10TokenFactory.buildTokenMap(clientId, username, redirect, stateId, scopes, null, OAuth20Constants.GRANT_TYPE_IMPLICIT);
                        OAuth20TokenHelper.addExternalClaims(idTokenMap, accessToken);
                        String accessTokenString = accessToken.getTokenString();
                        idTokenMap.put(OAuth20Constants.ACCESS_TOKEN, new String[] { accessTokenString });

                        String issuerIdentifier = attributeList.getAttributeValueByName(OAuth20Constants.ISSUER_IDENTIFIER);
                        idTokenMap.put(OAuth20Constants.ISSUER_IDENTIFIER, new String[] { issuerIdentifier });
                        // add nonce for oidc10
                        String nonce = attributeList.getAttributeValueByNameAndType(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, OAuth20Constants.ATTRTYPE_REQUEST);
                        if (nonce != null && nonce.length() > 0) {
                            idTokenMap.put(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, new String[] { nonce });
                        }
                        OAuth20Token id = oidc10TokenFactory.createIDToken(idTokenMap);

                        if (id != null) {
                            tokenList.add(id);
                        }
                        // only generate one id_token, so break here
                        break;
                    }
                }
            }

        }
        return tokenList;
    }

    public void buildResponseResponseType(AttributeList attributeList, List<OAuth20Token> tokens) {

        // Ask the super class to handle access_token
        oa20rthti.buildResponseResponseType(attributeList, tokens);

        // oidc10
        // replace old code and add IDToken  
        for (OAuth20Token token : tokens) {
            String strTokenType = token.getType();
            if (OIDCConstants.ID_TOKEN.equals(strTokenType)) {
                BuildResponseTypeUtil.handleIDToken(attributeList, token);
            }
        }
    }
}
