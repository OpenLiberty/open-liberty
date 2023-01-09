/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class AuthorizationRequestParameters {

    private static final TraceComponent tc = Tr.register(AuthorizationRequestParameters.class);

    public static final String SCOPE = "scope";
    public static final String RESPONSE_TYPE = "response_type";
    public static final String CLIEND_ID = "client_id";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String STATE = "state";
    public static final String RESPONSE_MODE = "response_mode";
    public static final String NONCE = "nonce";
    public static final String PROMPT = "prompt";
    public static final String ACR_VALUES = "acr_values";
    public static final String DISPLAY = "display";

    private final String authorizationEndpointUrl;
    private final String scope;
    private final String responseType;
    private final String clientId;
    private final String redirectUri;
    private final String state;

    private final List<NameValuePair> conditionalParameters = new ArrayList<>();

    public AuthorizationRequestParameters(String authorizationEndpointUrl, String scope, String responseType, String clientId, String redirectUri, String state) {
        this.authorizationEndpointUrl = authorizationEndpointUrl;
        this.scope = scope;
        this.responseType = responseType;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.state = state;
    }

    public void addParameter(String name, String value) {
        if (name == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cannot add parameter because name is null");
            }
            return;
        }
        conditionalParameters.add(new BasicNameValuePair(name, value));
    }

    public String buildRequestUrl() {
        String queryString = buildRequestQueryString();
        String queryMark = "?";
        if (authorizationEndpointUrl.indexOf("?") > 0) {
            queryMark = "&";
        }
        return authorizationEndpointUrl + queryMark + queryString;
    }

    String buildRequestQueryString() {
        String query = buildQueryWithRequiredParameters();
        query = appendConditionalParametersToQuery(query);
        return query;
    }

    String buildQueryWithRequiredParameters() {
        String query = "";
        query = appendParameterToQuery(query, SCOPE, scope);
        query = appendParameterToQuery(query, RESPONSE_TYPE, responseType);
        query = appendParameterToQuery(query, CLIEND_ID, clientId);
        query = appendParameterToQuery(query, REDIRECT_URI, redirectUri);
        query = appendParameterToQuery(query, STATE, state);
        return query;
    }

    String appendConditionalParametersToQuery(String query) {
        for (NameValuePair conditionalParameter : conditionalParameters) {
            query = appendParameterToQuery(query, conditionalParameter.getName(), conditionalParameter.getValue());
        }
        return query;
    }

    String appendParameterToQuery(String query, String parameterName, String parameterValue) {
        if (parameterName == null) {
            return query;
        }
        try {
            if (query == null) {
                query = "";
            }
            if (!query.isEmpty()) {
                query += "&";
            }
            query += URLEncoder.encode(parameterName, "UTF-8");
            if (parameterValue != null) {
                query += "=" + URLEncoder.encode(parameterValue, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            // Do nothing - UTF-8 encoding will be supported
        }
        return query;
    }

}
