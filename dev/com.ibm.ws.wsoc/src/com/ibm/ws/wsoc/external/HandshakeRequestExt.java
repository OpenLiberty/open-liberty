/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.external;

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wsoc.AnnotatedEndpoint;

public class HandshakeRequestExt implements HandshakeRequest {

    private HttpServletRequest request = null;
    private Map<String, List<String>> headers = null;
    private Map<String, List<String>> parameterMap = null;
    private String queryString = null;
    private HttpSession httpSession = null;
    private Principal principal = null;
    private URI requestURI = null;

    public HandshakeRequestExt(HttpServletRequest _request,
                               Map<String, List<String>> _headers,
                               Map<String, List<String>> _parameterMap,
                               URI _requestURI, Endpoint _ep, EndpointConfig _epc) {

        headers = _headers;
        // if there are no headers, then _headers should be empty list, not null.  Following servlet spec (wsoc javadoc doesn't say)
        if (headers == null) {
            headers = new HashMap<String, List<String>>();
        }

        //as per WebSocket HandshakeRequest API, the headers should be read only
        this.headers = Collections.unmodifiableMap(headers);
        request = _request;

        // add the path Params to the qurey parms that were already added
        Map<String, List<String>> pathParams = determinePathParams(_ep, _epc, _requestURI);
        _parameterMap.putAll(pathParams);

        //Even though webSocket Spec/API doesn't specify this to be readonly, TCK needs this to be readonly because
        //HttpServletRequest's parameterMap is immutable.
        this.parameterMap = Collections.unmodifiableMap(_parameterMap);
        queryString = request.getQueryString();
        httpSession = request.getSession();
        principal = request.getUserPrincipal();
        requestURI = _requestURI;

    }

    private Map<String, List<String>> determinePathParams(Endpoint endpoint, EndpointConfig endpointConfig, URI _uri) {

        // automated debug should print out the returned map, so no other debug will be added.
        HashMap<String, List<String>> pathParameters = new HashMap<String, List<String>>();

        String[] endpointURIParts = null;
        //endpoint path. e.g /basic/bookings/{guest-id}
        if ((endpoint != null) && (endpoint instanceof AnnotatedEndpoint)) {
            //annotated
            endpointURIParts = ((AnnotatedEndpoint) endpoint).getEndpointPath().split("/");
        } else if (endpointConfig != null) {
            //programmatic
            endpointURIParts = ((ServerEndpointConfig) endpointConfig).getPath().split("/");
        }

        if ((endpointURIParts == null) || (_uri == null)) {
            // return empty array if there is nothing to look at
            return pathParameters;
        }

        // This code is cut-n-paste from code in SessionImpl, which does the same thing.
        //incoming request uri path. e.g /bookings/JohnDoe
        String[] requestURIParts = _uri.getPath().split("/");
        int i = endpointURIParts.length - 1;
        int j = requestURIParts.length - 1;
        //start to compare the endpoint path and request uri path, starting from the last segment because
        //request uri at this point of execution does not have context root of the webapp, which in this case is '/basic'
        //and endpoint uri on the other hand does have the context root of the webapp at this point of execution.
        while (i > 1) { //skipping the first part because first part of split("/") for /../../.. is always an empty string
            if (endpointURIParts[i].startsWith("{") && endpointURIParts[i].endsWith("}")) {
                String endpointPart = endpointURIParts[i].substring(1, endpointURIParts[i].length() - 1); //guest-id

                // Map will have pathName, pathvalue --> guest-id,JohnDoe
                List<String> list = Arrays.asList(requestURIParts[j]);
                pathParameters.put(endpointPart, list);
            }
            i--;
            j--;
        }

        return pathParameters;
    }

    @Override
    @Sensitive
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public Object getHttpSession() {
        return httpSession;
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        return parameterMap;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public URI getRequestURI() {
        return requestURI;
    }

    @Override
    @Sensitive
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return request.isUserInRole(role);
    }

}
