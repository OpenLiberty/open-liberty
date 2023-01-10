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
package io.openliberty.security.oidcclientcore.userinfo;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.util.EntityUtils;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.common.jwt.jws.JwsSignatureVerifier;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.config.OidcMetadataService;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoEndpointNotHttpsException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseNot200Exception;
import io.openliberty.security.oidcclientcore.http.HttpConstants;
import io.openliberty.security.oidcclientcore.http.OidcClientHttpUtil;
import io.openliberty.security.oidcclientcore.jwt.JwtUtils;

public class UserInfoRequestor {

    public static final TraceComponent tc = Tr.register(UserInfoRequestor.class);

    private final OidcClientConfig oidcClientConfig;
    private final String userInfoEndpoint;
    private final String accessToken;

    private final List<NameValuePair> params;
    private final boolean hostnameVerification;
    private final boolean useSystemPropertiesForHttpClientConnections;

    OidcClientHttpUtil oidcClientHttpUtil = OidcClientHttpUtil.getInstance();

    private UserInfoRequestor(Builder builder) {
        this.oidcClientConfig = builder.oidcClientConfig;
        this.userInfoEndpoint = builder.userInfoEndpoint;
        this.accessToken = builder.accessToken;

        this.hostnameVerification = builder.hostnameVerification;
        this.useSystemPropertiesForHttpClientConnections = builder.useSystemPropertiesForHttpClientConnections;

        this.params = new ArrayList<NameValuePair>();
    }

    @FFDCIgnore(Exception.class)
    public UserInfoResponse requestUserInfo() throws UserInfoResponseException {
        if (!userInfoEndpoint.toLowerCase().startsWith(HttpConstants.HTTPS_SCHEME)) {
            throw new UserInfoEndpointNotHttpsException(userInfoEndpoint, oidcClientConfig.getClientId());
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
            claims = extractClaimsFromJwtResponse(jresponse);
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

    public JSONObject extractClaimsFromJwtResponse(String responseString) throws Exception {
        JwtContext jwtContext = JwtParsingUtils.parseJwtWithoutValidation(responseString);
        if (jwtContext != null) {
            JwtClaims claims = validateJwsSignatureAndGetClaims(jwtContext);
            if (claims != null) {
                return JSONObject.parse(claims.toJson());
            }
        }
        return null;
    }

    /**
     * Validates the JWS signature only and extracts the claims so they can be verified elsewhere.
     */
    JwtClaims validateJwsSignatureAndGetClaims(JwtContext jwtContext) throws Exception {
        io.openliberty.security.common.jwt.jws.JwsSignatureVerifier.Builder verifierBuilder = verifyJwsAlgHeaderOnly(jwtContext);

        JsonWebStructure jws = JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtContext);
        Key jwtVerificationKey = JwtUtils.getJwsVerificationKey(jws, oidcClientConfig);

        JwsSignatureVerifier signatureVerifier = verifierBuilder.key(jwtVerificationKey).build();
        return signatureVerifier.validateJwsSignature(jwtContext);
    }

    /**
     * Validates the "alg" header in the JWT to ensure the token is signed with one of the allowed algorithms. This allows us to
     * avoid doing the work to fetch the signing key for the token if the algorithm isn't supported.
     */
    io.openliberty.security.common.jwt.jws.JwsSignatureVerifier.Builder verifyJwsAlgHeaderOnly(JwtContext jwtContext) throws Exception {
        String[] signingAlgsAllowed = getSigningAlgorithmsAllowed();

        io.openliberty.security.common.jwt.jws.JwsSignatureVerifier.Builder verifierBuilder = new JwsSignatureVerifier.Builder();
        verifierBuilder = verifierBuilder.signatureAlgorithmsSupported(signingAlgsAllowed);
        verifierBuilder.build().verifyAlgHeaderOnly(jwtContext);;
        return verifierBuilder;
    }

    @FFDCIgnore(OidcClientConfigurationException.class)
    String[] getSigningAlgorithmsAllowed() {
        String[] signingAlgsAllowed = null;
        try {
            signingAlgsAllowed = MetadataUtils.getUserInfoSigningAlgorithmsSupported(oidcClientConfig);
        } catch (OidcDiscoveryException | OidcClientConfigurationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting user info signing algorithm supported. Defaulting to RS256. Exception was: " + e.getMessage());
            }
            signingAlgsAllowed = new String[] { "RS256" };
        }
        return signingAlgsAllowed;
    }

    private Map<String, Object> getFromUserInfoEndpoint() throws HttpException, IOException {
        return oidcClientHttpUtil.getFromEndpoint(userInfoEndpoint,
                                                  params,
                                                  null,
                                                  null,
                                                  accessToken,
                                                  OidcMetadataService.getSSLSocketFactory(),
                                                  hostnameVerification,
                                                  useSystemPropertiesForHttpClientConnections);
    }

    public static class Builder {

        private final OidcClientConfig oidcClientConfig;
        private final String userInfoEndpoint;
        private final String accessToken;

        private boolean hostnameVerification = false;
        private boolean useSystemPropertiesForHttpClientConnections = false;

        public Builder(OidcClientConfig oidcClientConfig, String userInfoEndpoint, String accessToken) {
            this.oidcClientConfig = oidcClientConfig;
            this.userInfoEndpoint = userInfoEndpoint;
            this.accessToken = accessToken;
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
