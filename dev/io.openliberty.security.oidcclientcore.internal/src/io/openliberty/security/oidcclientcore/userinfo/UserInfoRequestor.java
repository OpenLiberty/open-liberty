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
package io.openliberty.security.oidcclientcore.userinfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.util.EntityUtils;

import com.ibm.json.java.JSONObject;

import io.openliberty.security.oidcclientcore.exceptions.UserInfoEndpointNotHttpsException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseNot200Exception;
import io.openliberty.security.oidcclientcore.http.HttpConstants;
import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;

public class UserInfoRequestor {

    private final String userInfoEndpoint;
    private final String accessToken;
    private final SSLSocketFactory sslSocketFactory;

    private final List<NameValuePair> params;
    private final boolean hostnameVerification;
    private final boolean useSystemPropertiesForHttpClientConnections;

    OidcClientHttpUtil oidcClientHttpUtil = OidcClientHttpUtil.getInstance();

    private UserInfoRequestor(Builder builder) {
        this.userInfoEndpoint = builder.userInfoEndpoint;
        this.accessToken = builder.accessToken;
        this.sslSocketFactory = builder.sslSocketFactory;

        this.hostnameVerification = builder.hostnameVerification;
        this.useSystemPropertiesForHttpClientConnections = builder.useSystemPropertiesForHttpClientConnections;

        this.params = new ArrayList<NameValuePair>();
    }

    public UserInfoResponse requestUserInfo() throws UserInfoResponseException {
        if (!userInfoEndpoint.toLowerCase().startsWith(HttpConstants.HTTPS_SCHEME)) {
            throw new UserInfoEndpointNotHttpsException(userInfoEndpoint);
        }

        int statusCode = 0;
        JSONObject claims = null;
        try {
            Map<String, Object> resultMap = getFromUserInfoEndpoint();
            HttpResponse response = (HttpResponse) resultMap.get(HttpConstants.RESPONSEMAP_CODE);
            if (response == null) {
                throw new Exception("HttpResponse from getUserinfo is null");
            }
            claims = extractClaimsFromResponse(response);
            statusCode = getStatusCodeFromResponse(response);
        } catch (Exception e) {
            throw new UserInfoResponseException(userInfoEndpoint, e);
        }

        if (statusCode != 200) {
            throw new UserInfoResponseNot200Exception(userInfoEndpoint, Integer.toString(statusCode), claims.toString());
        }

        return new UserInfoResponse(claims);
    }

    private int getStatusCodeFromResponse(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    private JSONObject extractClaimsFromResponse(HttpResponse response) throws Exception {
        HttpEntity entity = response.getEntity();
        String jresponse = null;
        if (entity != null) {
            jresponse = EntityUtils.toString(entity);
        }
        if (jresponse == null || jresponse.isEmpty()) {
            return null;
        }
        String contentType = getContentType(entity);
        if (contentType == null) {
            return null;
        }
        JSONObject claims = null;
        if (contentType.contains(HttpConstants.APPLICATION_JSON)) {
            claims = extractClaimsFromJsonResponse(jresponse);
        } else if (contentType.contains(HttpConstants.APPLICATION_JWT)) {
            // TODO: handle jwt (jws and jwe) response type
        }
        return claims;
    }

    private JSONObject extractClaimsFromJsonResponse(String jresponse) throws IOException {
        return JSONObject.parse(jresponse);
    }

    private String getContentType(HttpEntity entity) {
        Header contentTypeHeader = entity.getContentType();
        if (contentTypeHeader != null) {
            return contentTypeHeader.getValue();
        }
        return null;
    }

    private Map<String, Object> getFromUserInfoEndpoint() throws HttpException, IOException {
        return oidcClientHttpUtil.getFromEndpoint(userInfoEndpoint,
                                                  params,
                                                  null,
                                                  null,
                                                  accessToken,
                                                  sslSocketFactory,
                                                  hostnameVerification,
                                                  useSystemPropertiesForHttpClientConnections);
    }

    public static class Builder {

        private final String userInfoEndpoint;
        private final String accessToken;
        private final SSLSocketFactory sslSocketFactory;

        private boolean hostnameVerification = false;
        private boolean useSystemPropertiesForHttpClientConnections = false;

        public Builder(String userInfoEndpoint, String accessToken, SSLSocketFactory sslSocketFactory) {
            this.userInfoEndpoint = userInfoEndpoint;
            this.accessToken = accessToken;
            this.sslSocketFactory = sslSocketFactory;
        }

        public Builder hostnameVerification(boolean hostnameVerification) {
            this.hostnameVerification = hostnameVerification;
            return this;
        }

        public Builder useSystemPropertiesForHttpClientConnections(boolean useSystemPropertiesForHttpClientConnections) {
            this.useSystemPropertiesForHttpClientConnections = useSystemPropertiesForHttpClientConnections;
            return this;
        }

        public UserInfoRequestor build() {
            return new UserInfoRequestor(this);
        }
    }
}
