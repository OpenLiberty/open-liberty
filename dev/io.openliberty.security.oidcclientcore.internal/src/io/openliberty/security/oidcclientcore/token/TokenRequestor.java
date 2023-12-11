/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;

public class TokenRequestor {

    public static final TraceComponent tc = Tr.register(TokenRequestor.class);

    private final String tokenEndpoint;
    private final String clientId;
    @Sensitive
    private final String clientSecret;
    private final String grantType;
    private final String redirectUri;
    private final String code;
    private final String refreshToken;
    private final SSLSocketFactory sslSocketFactory;

    private final boolean isHostnameVerification;
    private final String authMethod;
    private final String resources;
    private final List<NameValuePair> params;
    private final HashMap<String, String> customParams;
    private final boolean useSystemPropertiesForHttpClientConnections;
    private final String originHeaderValue;

    OidcClientHttpUtil oidcClientHttpUtil = OidcClientHttpUtil.getInstance();

    private TokenRequestor(Builder builder) throws TokenRequestException {
        this.tokenEndpoint = builder.tokenEndpoint;
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.grantType = builder.grantType;
        this.redirectUri = builder.redirectUri;
        this.code = builder.code;
        this.refreshToken = builder.refreshToken;
        this.sslSocketFactory = builder.sslSocketFactory;
        this.isHostnameVerification = builder.isHostnameVerification;
        this.authMethod = builder.authMethod;
        this.resources = builder.resources;
        this.customParams = builder.customParams;
        this.useSystemPropertiesForHttpClientConnections = builder.useSystemPropertiesForHttpClientConnections;
        this.originHeaderValue = builder.originHeaderValue;

        List<NameValuePair> params = getBasicParams();
        mergeCustomParams(params, customParams);
        this.params = params;
    }

    @Sensitive
    private List<NameValuePair> getBasicParams() throws TokenRequestException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(TokenConstants.GRANT_TYPE, grantType));
        if (resources != null) {
            params.add(new BasicNameValuePair(TokenConstants.RESOURCE, resources));
        }
        params.add(new BasicNameValuePair(TokenConstants.REDIRECT_URI, redirectUri));
        if (code != null) {
            params.add(new BasicNameValuePair(TokenConstants.CODE, code));
        }

        if (refreshToken != null) {
            params.add(new BasicNameValuePair(TokenConstants.REFRESH_TOKEN, refreshToken));
        }

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
        JSONObject json = JSONObject.parse(tokenEndpointEntity);
        return new TokenResponse(json);
    }

    private Map<String, Object> postToTokenEndpoint() throws Exception {
        HttpPost httpPost = oidcClientHttpUtil.setupPost(tokenEndpoint, params, clientId, clientSecret, null, authMethod);
        if (originHeaderValue != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Will add Origin HTTP header with value: [" + originHeaderValue + "]");
            }
            httpPost.addHeader("Origin", originHeaderValue);
        }
        return oidcClientHttpUtil.postToEndpoint(tokenEndpoint,
                                                 httpPost,
                                                 sslSocketFactory,
                                                 isHostnameVerification,
                                                 useSystemPropertiesForHttpClientConnections);
    }

    public static class Builder {

        private String grantType = TokenConstants.AUTHORIZATION_CODE;

        private final String tokenEndpoint;
        private final String clientId;
        @Sensitive
        private final String clientSecret;
        private final String redirectUri;
        private final String code;

        private String refreshToken = null;
        private SSLSocketFactory sslSocketFactory = null;
        private boolean isHostnameVerification = true;
        private String authMethod = TokenConstants.METHOD_POST;
        private String resources = null;
        private HashMap<String, String> customParams = null;
        private boolean useSystemPropertiesForHttpClientConnections = false;
        private String originHeaderValue;

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

        public Builder grantType(String grantType) {
            this.grantType = grantType;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder resources(String resources) {
            this.resources = resources;
            return this;
        }

        public Builder customParams(@Sensitive HashMap<String, String> customParams) {
            if (customParams != null && this.customParams != null) {
                this.customParams.putAll(customParams);
            } else {
                this.customParams = customParams;
            }
            return this;
        }

        public Builder useSystemPropertiesForHttpClientConnections(boolean useSystemPropertiesForHttpClientConnections) {
            this.useSystemPropertiesForHttpClientConnections = useSystemPropertiesForHttpClientConnections;
            return this;
        }

        public Builder originHeaderValue(String originHeaderValue) {
            this.originHeaderValue = originHeaderValue;
            return this;
        }

        public TokenRequestor build() throws TokenRequestException {
            return new TokenRequestor(this);
        };
    }
}
