/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.annotation.Sensitive;

import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;

public class TokenRequestor {

    private static final List<NameValuePair> commonHeaders;

    private final String tokenEndpoint;
    private final String clientId;
    @Sensitive
    private final String clientSecret;
    private final String grantType;
    private final String redirectUri;
    private final String code;
    private final SSLSocketFactory sslSocketFactory;

    private final boolean isHostnameVerification;
    private final String authMethod;
    private final String resources;
    private final List<NameValuePair> params;
    private final HashMap<String, String> customParams;
    private final boolean useSystemPropertiesForHttpClientConnections;

    OidcClientHttpUtil oidcClientHttpUtil = OidcClientHttpUtil.getInstance();

    static {
        commonHeaders = new ArrayList<NameValuePair>();
        commonHeaders.add(new BasicNameValuePair(TokenConstants.ACCEPT, TokenConstants.APPLICATION_JSON));
    }

    private TokenRequestor(Builder builder) {
        this.tokenEndpoint = builder.tokenEndpoint;
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.grantType = builder.grantType;
        this.redirectUri = builder.redirectUri;
        this.code = builder.code;
        this.sslSocketFactory = builder.sslSocketFactory;
        this.isHostnameVerification = builder.isHostnameVerification;
        this.authMethod = builder.authMethod;
        this.resources = builder.resources;
        this.customParams = builder.customParams;
        this.useSystemPropertiesForHttpClientConnections = builder.useSystemPropertiesForHttpClientConnections;

        List<NameValuePair> params = getBasicParams();
        mergeCustomParams(params, customParams);
        this.params = params;
    }

    private List<NameValuePair> getBasicParams() {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(TokenConstants.GRANT_TYPE, grantType));
        if (resources != null) {
            params.add(new BasicNameValuePair(TokenConstants.RESOURCE, resources));
        }
        params.add(new BasicNameValuePair(TokenConstants.REDIRECT_URI, redirectUri));
        params.add(new BasicNameValuePair(TokenConstants.CODE, code));
        if (authMethod.equals(TokenConstants.METHOD_POST) || authMethod.equals(TokenConstants.METHOD_CLIENT_SECRET_POST)) {
            params.add(new BasicNameValuePair(TokenConstants.CLIENT_ID, clientId));
            params.add(new BasicNameValuePair(TokenConstants.CLIENT_SECRET, clientSecret));
        }
        return params;
    }

    private void mergeCustomParams(@Sensitive List<NameValuePair> params, HashMap<String, String> customParams) {
        if (customParams == null || customParams.isEmpty()) {
            return;
        }

        for (Entry<String, String> param : customParams.entrySet()) {
            if (param.getKey() != null && param.getValue() != null) {
                params.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            }
        }
    }

    public TokenResponse requestTokens() throws Exception {
        Map<String, Object> tokenEndpointResponse = postToTokenEndpoint();
        String tokenEndpointEntity = oidcClientHttpUtil.extractEntityFromTokenResponse(tokenEndpointResponse);
        Map<String, String> tokens = getTokensFromJsonString(tokenEndpointEntity);
        return new TokenResponse(tokens.get(TokenConstants.ID_TOKEN), tokens.get(TokenConstants.ACCESS_TOKEN), tokens.get(TokenConstants.REFRESH_TOKEN));
    }

    private Map<String, Object> postToTokenEndpoint() throws Exception {
        return oidcClientHttpUtil.postToEndpoint(
                                                 tokenEndpoint,
                                                 params,
                                                 clientId,
                                                 clientSecret,
                                                 null,
                                                 sslSocketFactory,
                                                 commonHeaders,
                                                 isHostnameVerification,
                                                 authMethod,
                                                 useSystemPropertiesForHttpClientConnections);
    }

    private Map<String, String> getTokensFromJsonString(String jsonString) throws Exception {
        Map<String, String> tokens = new HashMap<>();
        List<String> tokenTypes = Arrays.asList(TokenConstants.TOKEN_TYPES);

        JSONObject json = JSONObject.parse(jsonString);

        @SuppressWarnings("unchecked")
        Iterator<String> iterator = json.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = json.get(key);
            if (value instanceof String && tokenTypes.contains(key)) {
                tokens.put(key, value.toString());
            }
        }

        return tokens;
    }

    public static class Builder {

        private final String grantType = TokenConstants.AUTHORIZATION_CODE;

        private final String tokenEndpoint;
        private final String clientId;
        @Sensitive
        private final String clientSecret;
        private final String redirectUri;
        private final String code;

        private SSLSocketFactory sslSocketFactory = null;
        private boolean isHostnameVerification = false;
        private String authMethod = TokenConstants.METHOD_BASIC;
        private String resources = null;
        private HashMap<String, String> customParams = null;
        private boolean useSystemPropertiesForHttpClientConnections = false;

        public Builder(String tokenEndpoint, String clientId, @Sensitive String clientSecret, String redirectUri, String code) {
            this.tokenEndpoint = tokenEndpoint;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.redirectUri = redirectUri;
            this.code = code;
        }

        public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        public Builder isHostnameVerification(boolean isHostnameVerification) {
            this.isHostnameVerification = isHostnameVerification;
            return this;
        }

        public Builder authMethod(String authMethod) {
            this.authMethod = authMethod;
            return this;
        }

        public Builder resources(String resources) {
            this.resources = resources;
            return this;
        }

        public Builder customParams(HashMap<String, String> customParams) {
            this.customParams = customParams;
            return this;
        }

        public Builder useSystemPropertiesForHttpClientConnections(boolean useSystemPropertiesForHttpClientConnections) {
            this.useSystemPropertiesForHttpClientConnections = useSystemPropertiesForHttpClientConnections;
            return this;
        }

        public TokenRequestor build() {
            return new TokenRequestor(this);
        };
    }
}
