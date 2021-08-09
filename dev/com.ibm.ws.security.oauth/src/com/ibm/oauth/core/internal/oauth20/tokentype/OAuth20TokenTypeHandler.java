/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20.tokentype;

import java.util.List;
import java.util.Map;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Handler;

/**
 * Classes that implement this interface do so to both create an OAuth 2.0 token
 * when the token endpoint is accessed with a valid grant, and to validate
 * tokens as part of a request for a protected resource.
 * 
 * Methods of this class related to validating a token receive an AttributeList
 * which represents the normalized OAuth 2.0 HTTP request. The attributes are
 * based on the following schema:
 * 
 * OAuth protocol parameters will have the attribute type:
 * <code>urn:ibm:names:ITFIM:oauth:param</code>
 * 
 * Post body parameters will have the attribute type:
 * <code>urn:ibm:names:ITFIM:oauth:body:param</code>
 * 
 * Query string parameters will have the attribute type:
 * <code>urn:ibm:names:ITFIM:oauth:query:param</code>
 * 
 */
public interface OAuth20TokenTypeHandler extends OAuth20Handler {

    /**
     * 
     * This method is called by a factory when an instance of this object is
     * created. The configuration object will allow the client provider to
     * initialize itself.
     * 
     * @param config
     *            - Configuration entity for the component instance
     */
    public void init(OAuthComponentConfiguration config);

    /**
     * This method must return the name of the token type that this
     * implementation class handles e.g. "Bearer" or "Mac". It must correspond
     * with the getSubType() method of the OAuth20Token class implementation.
     * 
     * @return the name of the token type this class handles
     */
    public String getTypeTokenType();

    /**
     * This method is used to create an access token, it is called by the
     * OAuth20TokenFactory class which is accessible from the
     * OAuth20ResponseTypeHandler and OAuth20GrantTypeHandler classes.
     * 
     * @param tokenMap
     *            A Map <String, String[]> containing the following parameters
     *            (if they are valid):
     *            <ul>
     *            <li>CLIENT_ID - client id from request
     *            <li>FEDERATION_ID - the federation to which this token applies
     *            <li>USERNAME - username that authorized the grant
     *            <li>REDIRECT_URI - redirect uri from request
     *            <li>STATE_ID - state id, if none present generate a new one
     *            <li>SCOPE - scope from the grant being used
     *            <li>LIFETIME - the configured lifetime of tokens
     *            <li>LENGTH - the configured length of tokens
     *            </ul>
     * @return
     */
    public OAuth20Token createToken(Map<String, String[]> tokenMap);

    /**
     * This method is called before TFIM retrieves the grant(s)/token(s)
     * associated with this request. This method should inspect the request and
     * return a List of String keys that identify the OAuth20Token objects in
     * the cache that are required to validate this request.
     * 
     * @param attributeList
     *            the HTTP request converted into an attribute list (described
     *            above)
     * @return a list of String keys for OAuth20Tokens in the token cache
     * @throws OAuthException
     */
    public List<String> getKeysTokenType(AttributeList attributeList)
            throws OAuthException;

    /**
     * This method is used to validate the grant(s)/token(s) supplied in the
     * request against the OAuth20Token objects in the cache.
     * 
     * @param attributeList
     *            the HTTP request converted into an attribute list (described
     *            above)
     * @param tokens
     *            the tokens retrieved from the token cache per the
     *            getKeysTokenType method
     * @throws OAuthException
     *             the OAuthException should contain the OAuth error_code
     *             relevant to the encountered error
     */
    public void validateRequestTokenType(AttributeList attributeList,
            List<OAuth20Token> tokens) throws OAuthException;

    /**
     * Once the token type handler has built the necessary OAuth20Token objects
     * and returned them to the component to be cached, this method is called to
     * specify what information is returned to the OAuth 2.0 client.
     * 
     * In order to return parameters to the OAuth 2.0 client, this method must
     * append attributes to the attribute list that is passed as a parameter.
     * These response attributes must have the following attribute type:
     * <code>urn:ibm:names:ITFIM:oauth:response:attribute</code>
     * 
     * @param attributeList
     *            the HTTP request converted into an attribute list (described
     *            above)
     * @param tokens
     *            a list of OAuth20Token objects that were created by the
     *            buildTokens method
     */
    public void buildResponseTokenType(AttributeList attributeList,
            List<OAuth20Token> tokens);
}
