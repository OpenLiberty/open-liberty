/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.common.jwt.jws;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.HmacKey;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;

import io.openliberty.security.common.jwt.exceptions.SharedKeyMissingException;
import io.openliberty.security.common.jwt.exceptions.UnsupportedSignatureAlgorithmException;
import io.openliberty.security.common.jwt.jwk.RemoteJwkData;

public class JwsVerificationKeyHelper {

    public static final Set<String> SUPPORTED_SIGNATURE_ALGORITHMS;

    static {
        Set<String> supportedAlgorithms = new HashSet<>();
        supportedAlgorithms.add("RS256");
        supportedAlgorithms.add("RS384");
        supportedAlgorithms.add("RS512");
        supportedAlgorithms.add("HS256");
        supportedAlgorithms.add("HS384");
        supportedAlgorithms.add("HS512");
        supportedAlgorithms.add("ES256");
        supportedAlgorithms.add("ES384");
        supportedAlgorithms.add("ES512");
        SUPPORTED_SIGNATURE_ALGORITHMS = Collections.unmodifiableSet(supportedAlgorithms);
    }

    private String clientId;
    @Sensitive
    private ProtectedString clientSecret;
    private RemoteJwkData remoteJwkData;
    private JWKSet jwkSet;

    private JwsVerificationKeyHelper(Builder builder) {
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.remoteJwkData = builder.remoteJwkData;
        this.jwkSet = builder.jwkSet;
    }

    public Key getVerificationKey(JsonWebStructure jws) throws Exception {
        String signatureAlgorithmFromJws = getSignatureAlgorithmFromJws(jws);
        if (signatureAlgorithmFromJws != null && signatureAlgorithmFromJws.startsWith("HS")) {
            return getSharedKey();
        }
        return retrievePublicKey(jws, signatureAlgorithmFromJws);
    }

    String getSignatureAlgorithmFromJws(JsonWebStructure jws) throws UnsupportedSignatureAlgorithmException {
        String algorithm = null;
        if (jws != null && jws instanceof JsonWebSignature) {
            algorithm = ((JsonWebSignature) jws).getAlgorithmHeaderValue();
        }
        if (SUPPORTED_SIGNATURE_ALGORITHMS.contains(algorithm)) {
            return algorithm;
        }
        throw new UnsupportedSignatureAlgorithmException(algorithm);
    }

    Key getSharedKey() throws SharedKeyMissingException, UnsupportedEncodingException {
        if (clientSecret == null || clientSecret.isEmpty()) {
            throw new SharedKeyMissingException();
        }
        String clientSecretProtectedString = new String(clientSecret.getChars());
        return new HmacKey(clientSecretProtectedString.getBytes("UTF-8"));
    }

    Key retrievePublicKey(JsonWebStructure jws, String signatureAlgorithmFromJws) throws IOException, Exception {
        String kid = jws.getKeyIdHeaderValue();
        String x5t = jws.getX509CertSha1ThumbprintHeaderValue();
        JwKRetriever jwkRetriever = createJwkRetriever(signatureAlgorithmFromJws);
        return jwkRetriever.getPublicKeyFromJwk(kid, x5t, "sig", false);
    }

    JwKRetriever createJwkRetriever(String signatureAlgorithmFromJws) throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(clientId, null, remoteJwkData.getJwksUri(), jwkSet, remoteJwkData.getSslSupport(), false, null, null, signatureAlgorithmFromJws);
        // Override the retriever's HttpUtils member so we can set connection and read timeouts
        jwkRetriever.httpUtils = new HttpUtils() {
            @Override
            public HttpGet createHttpGetMethod(String url, final List<NameValuePair> commonHeaders) {
                HttpGet request = super.createHttpGetMethod(url, commonHeaders);
                RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(remoteJwkData.getJwksConnectTimeout()).setSocketTimeout(remoteJwkData.getJwksReadTimeout()).build();
                request.setConfig(requestConfig);
                return request;
            }
        };
        return jwkRetriever;
    }

    public static class Builder {

        private String clientId;
        @Sensitive
        private ProtectedString clientSecret;
        private RemoteJwkData remoteJwkData;
        private JWKSet jwkSet;

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(@Sensitive ProtectedString clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder remoteJwkData(RemoteJwkData remoteJwkData) {
            this.remoteJwkData = remoteJwkData;
            return this;
        }

        public Builder jwkSet(JWKSet jwkSet) {
            this.jwkSet = jwkSet;
            return this;
        }

        public JwsVerificationKeyHelper build() {
            return new JwsVerificationKeyHelper(this);
        }

    }

}
