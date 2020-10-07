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
package com.ibm.oauth.core.internal.oauth20.granttype;

import java.util.List;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Handler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;

/**
 * Classes that implement this interface do so to handle an OAuth 2.0 request
 * for access token which uses a particular grant_type attribute e.g.
 * authorization_code OR refresh_token.
 * 
 * When an access token request is received, the core component will inspect the
 * grant_type attribute and look for any registered grant type handler classes.
 * If it finds one that matches, it will be instantiated and it's methods will
 * be invoked to handle the request.
 * 
 * The methods of this class receive an attribute list which represents the
 * original OAuth 2.0 HTTP request. The HTTP request is transformed into an
 * attribute list and parameters are added based on the following schema:
 * 
 * If the client authenticated with the token endpoint using a mechanism other
 * than sending the client_id and client_secret as post body parameters, the
 * client_id should be included with the attribute type:
 * <code>urn:ibm:names:oauth:param</code>
 * 
 * Post body parameters will have the attribute type:
 * <code>urn:ibm:names:oauth:body:param</code>
 * 
 * Query string parameters will have the attribute type:
 * <code>urn:ibm:names:oauth:query:param</code>
 * 
 */
public interface OAuth20GrantTypeHandler extends OAuth20Handler {
    /**
     * This method is called before the component retrieves the
     * grant(s)/token(s) associated with this request. This method should
     * inspect the request and return a List of String keys that identify the
     * OAuth20Token objects in the token cache that are required to validate
     * this request.
     * 
     * @param attributeList
     *            the HTTP request converted into an attribute list (described
     *            above)
     * @return a list of Sting keys for OAuth20Tokens in the token cache
     * @throws OAuth20Exception
     */
    public List<String> getKeysGrantType(AttributeList attributeList)
            throws OAuthException;

    /**
     * This method is called after the component validates the following:
     * <ul>
     * <li>the authorization request came from an enabled OAuth 2.0 client
     * <li>the request contains a valid grant_type parameter
     * <li>the redirect URI is matches the registered redirect uri
     * </ul>
     * <p>
     * This method can be used to validate any other grant_type specific
     * parameters that may be required.
     * 
     * @param attributeList
     *            the HTTP request converted into an attribute list (described
     *            above)
     * @param tokens
     *            a list of OAuth20Token objects identified by the token keys
     *            returned by the getTokenKeys method
     * @throws OAuthException
     *             the OAuthException should contain the error_code relevant to
     *             the encountered error
     */
    public void validateRequestGrantType(AttributeList attributeList,
            List<OAuth20Token> tokens) throws OAuthException;

    /**
     * This method is invoked after all validation takes place in order to build
     * the OAuth 2.0 grant(s)/token(s) required depending on the grant_type
     * parameter.
     * 
     * @param attributeList
     *            the HTTP request converted into an attribute list (described
     *            above)
     * @param factory
     *            a helper class for building OAuth20Token objects
     * @param tokens
     *            a list of tokens retrieved from the token cache based on the
     *            keys returned by the getKeysGrantType method
     * @return a list of OAuth20Token objects to be passed to the
     *         buildResponseGrantType method
     */
    public List<OAuth20Token> buildTokensGrantType(AttributeList attributeList,
            OAuth20TokenFactory factory, List<OAuth20Token> tokens);

    /**
     * Once the grant type handler has built the necessary OAuth20Token objects
     * and returned them to the core component to be cached, this method is
     * called to specify what information is returned to the OAuth 2.0 client.
     * 
     * In order to return parameters to the OAuth 2.0 client, this method must
     * append attributes to the attribute list that is passed as a parameter.
     * These response attributes must have the following attribute type:
     * <code>urn:ibm:names:oauth:response:attribute</code>
     * 
     * @param attributeList
     *            the HTTP request converted into an attribute list (described
     *            above)
     * @param tokens
     *            a list of OAuth20Token objects that were created by the
     *            buildTokensGrantType method
     */
    public void buildResponseGrantType(AttributeList attributeList,
            List<OAuth20Token> tokens);
}
