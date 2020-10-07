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
package com.ibm.oauth.core.internal.oauth20.responsetype;

import java.util.List;

import com.google.gson.JsonArray;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Handler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;

/**
 * Classes that implement this interface do so to handle an OAuth 2.0 request
 * for authorization which uses a particular response_type attribute e.g. token
 * OR code.
 *
 * When an authorization request is processed the response_type request
 * parameter will be matched to registered response type handler classes. If it
 * finds one that matches, it will be instantiated and it's method will be
 * invoked to handle the request.
 *
 * The methods of this class receive an AttributeList which represents the
 * normalized version of the original OAuth 2.0 HTTP request. The HTTP request
 * is transformed into an AttributeList and attributes are added based on the
 * following schema:
 *
 * Post body parameters will have the attribute type:
 * <code>urn:ibm:names:oauth:body:param</code>
 *
 * Query string parameters will have the attribute type:
 * <code>urn:ibm:names:oauth:query:param</code>
 *
 */
public interface OAuth20ResponseTypeHandler extends OAuth20Handler {
    /**
     * This method is called after the core validates the following:
     * <ul>
     * <li>the authorization request came from an enabled OAuth 2.0 client
     * <li>the request was authorized by a resource owner
     * <li>the request contains a valid response_type parameter
     * <li>if a redirect URI was sent, it matches the registered one
     * </ul>
     * <p>
     * This method can be used to validate any other response_type specific
     * parameters that may be required
     *
     * @param attributeList
     *            the HTTP request converted into an AttributeList (described
     *            above)
     * @param redirectUris
     *            the previously registered redirect URIs of the client that made
     *            this request
     * @param  allowRegexpRedirects
     *            true if the caller allows redirect URIs to contain regular expressions
     * @throws OAuthException
     *             the OAuthException should contain the error_code relevant to
     *             the encountered error
     */
    public void validateRequestResponseType(AttributeList attributeList,
            JsonArray redirectUris, boolean allowRegexpRedirects) throws OAuthException;

    /**
     * This method is invoked after all validation takes place in order to build
     * the OAuth 2.0 grant(s)/token(s) required depending on the response_type
     * parameter.
     *
     * @param attributeList
     *            the HTTP request converted into an AttributeList (described
     *            above)
     * @param tokenFactory
     *            a helper class for building OAuth20Token objects
     * @param redirectUri
     *            the redirect uri provided in the request
     * @return a list of OAuth20Token objects to be passed to the
     *         buildResponseResponseType method
     */
    public List<OAuth20Token> buildTokensResponseType(
            AttributeList attributeList, OAuth20TokenFactory tokenFactory,
            String redirectUri);

    /**
     * Once the response type handler has built the necessary OAuth20Token
     * objects and returned them to the core to be cached, this method is called
     * to specify what information is returned to the OAuth 2.0 client.
     *
     * In order to return parameters to the OAuth 2.0 client, this method must
     * update the AttributeList that is passed as a parameter. All attributes to
     * be included in the response must have the following attribute type:
     * <code>urn:ibm:names:oauth:response:attribute</code>
     *
     * @param stsuu
     *            the HTTP request converted into an STSUU XML document
     *            (described above)
     * @param tokens
     *            a list of OAuth20Token objects that were created by the
     *            buildTokensResponseType method
     */
    public void buildResponseResponseType(AttributeList attributeList,
            List<OAuth20Token> tokens);
}
