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
package com.ibm.oauth.core.api.oauth20;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.OAuthComponent;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;

/**
 * Interface representing OAuth 2.0 service provider component.
 */
public interface OAuth20Component extends OAuthComponent {

    /**
     * Called by a authorization request end point to perform user authorization in OAuth 2.0 processing.
     * @param username The user name of the resource owner as authenticated by the authorization endpoint.
     * @param clientId The client_id parameter from the request to the authorization endpoint
     * @param redirectUri The redirect_uri parameter from the request to the authorization endpoint. This may be null.
     * @param responseType The response_type parameter from the request to the authorization endpoint
     * @param state - The state parameter from the request to the authorization endpoint. This may be null.
     * @param authorizedScopes an array of strings representing scope authorized by the resource owner. This may be null if no explicit scope is associated with the authorization.
     * @param response The response object that will be populated by the component during OAuth processing
     * @return OAuthResult representing user authorization result.
     *
     */
    public OAuthResult processAuthorization(String username, String clientId,
            String redirectUri, String responseType, String state,
            String[] authorizedScopes, HttpServletResponse response);

    /**
     * Called by a authorization request end point to perform user authorization in OAuth 2.0 processing.
     * @param request The request object that will be populated by the component during OpenID processing
     * @param response The response object that will be populated by the component during OAuth processing
         * @param options an list of OpenID Connect unique optional parameters.
     * @return OAuthResult representing user authorization result.
     *
     */
    public OAuthResult processAuthorization(HttpServletRequest request, HttpServletResponse response, AttributeList options);

    /**
     * Called by a token request end point to perform OAuth 2.0 token request.
     *
     * @param authenticatedClient An authenticated client.
     * The authenticatedClient represents the client identity authenticated by
     * the token endpoint itself if the token endpoint performs its own
     * authentication of client requests.
     * If authenticatedClient is null, the component will look for and validate
     * the client_secret if present. If authenticatedClient is null and no
     * client_secret parameter is present, the client will be considered
     * a public client.
     * @param request a HttpServletRequest instance
     * @param response a HttpServletRequest instance
     * @return the result of OAuth token request.
     */
    public OAuthResult processTokenRequest(String authenticatedClient,
            HttpServletRequest request, HttpServletResponse response);

    /**
     * Called by app-password or app-token endpoint to request a long lived
     * app-password or app-token.
     * The token is written directly to the response as json.
     *
     * @param isAppPasswordRequest - if true, produce app-password, else produce app-token
     * @param authenticatedClient - The client id of a client that has already been
     *   validated to exist, be enabled, and be configured to allow app-passwords and/or app-tokens
     *
     * @param request  a HttpServletRequest instance
     *   The following attributes are expected to be set on the request:
     *     user  - the user that was previously authenticated from the access_token parameter.
     *   The following parameters are expected to be set on the request:
     *     app_name - user friendly name to identify the client applications using the app-password or app-token
     *   The following parameters might be set on the request:
     *     app_id -   an id associated with an app
     *     access_token - the short-lived token of the user making this request.
     * @param response a HttpServletRequest instance
     * @return the result of the request
     */
    public OAuthResult processAppTokenRequest(boolean isAppPasswordRequest, String authenticatedClient,
            HttpServletRequest request, HttpServletResponse response);

    /**
     * Called by an OAuth enforcement point to validate an OAuth-enabled request
     * when the enforcement point is not doing any pre-processing of the
     * attributes within the HttpServletRequest and has direct access to the
     * original client request. This method is most likely to be used when the
     * enforcement point is co-located with the rest of the OAuth server
     * application utilizing the component.
     *
     * @param request an HttpServletRequest instance.
     * @return the result of OAuth protected resource request processing.
     */
    public OAuthResult processResourceRequest(HttpServletRequest request);

    /**
     * This is an alternate API to be used by an OAuth enforcement point to
     * validate an OAuth-enabled request when the enforcement point has
     * pre-processed the HTTP request and marshalled attributes into an
     * AttributeList as described below. This method is most likely to be used
     * if the enforcement point is remote from the rest of the OAuth server
     * hosting the component. The remote enforcement point could use a custom
     * communications method back to the OAuth server which could assemble the
     * client request parameters into the AttributeList, call this API and
     * return the result back to the remote enforcement point.
     *
     *
     * The attribute list should contain:
     *
     * The access token, regardless of the method by which it is transmitted,
     * should be added to the AttributeList with name =
     * <code>access_token</code> and type =
     * <code>urn:ibm:names:oauth:param</code>.
     *
     * All POST body parameters should be added as attributes with their
     * original attribute name and values and the type =
     * <code>urn:ibm:names:oauth:body:param</code>.
     *
     * All query string parameters should be added as attributes with their
     * original name and values and the type =
     * <code>urn:ibm:names:oauth:query:param</code>.
     *
     * Other metadata about the client request should be included in the
     * attribute list with the type
     * <code>urn:ibm:names:oauth:request</code> and names and values as
     * follows:
     * <ul>
     * <li>The original client request URL hostname should be added with
     * attribute name = host</li>
     * <li>The port number (if non-standard) should be added with attribute name
     * = port</li>
     * <li>The HTTP method (GET/POST) should be added with attribute name =
     * method</li>
     * <li>The request path should be added with attribute name = path</li>
     * <li>The request scheme (http/https) should be added with attribute name =
     * scheme</li>
     * </ul>
     *
     * @param attributeList
     * @return the result of OAuth protected resource request processing.
     */
    public OAuthResult processResourceRequest(AttributeList attributeList);
}
