/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.common.jwt.jws;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.security.common.http.HttpUtils;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.common.jwt.exceptions.SharedKeyMissingException;
import io.openliberty.security.common.jwt.exceptions.UnsupportedSignatureAlgorithmException;
import io.openliberty.security.common.jwt.jwk.RemoteJwkData;

public class JwsVerificationKeyHelper {

    public static final Set<String> SUPPORTED_SIGNATURE_ALGORITHMS;

    static {
        Set<String> supportedAlgorithms = new HashSet<>();
        supportedAlgorithms.add("none");
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

    private String configId;
    @Sensitive
    private ProtectedString sharedSecret;
    private RemoteJwkData remoteJwkData;
    private JWKSet jwkSet;

    private JwsVerificationKeyHelper(Builder builder) {
        this.configId = builder.configId;
        this.sharedSecret = builder.sharedSecret;
        this.remoteJwkData = builder.remoteJwkData;
        this.jwkSet = builder.jwkSet;
    }

    public RemoteJwkData getRemoteJwkData() {
        return remoteJwkData;
    }

    public Key getVerificationKey(JsonWebStructure jws) throws Exception {
        String signatureAlgorithmFromJws = getSignatureAlgorithmFromJws(jws);
        if (signatureAlgorithmFromJws == null) {
            // TODO
        }
        if (signatureAlgorithmFromJws.equalsIgnoreCase("none")) {
            return null;
        }
        if (signatureAlgorithmFromJws.startsWith("HS")) {
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

    Key getSharedKey() throws SharedKeyMissingException {
        if (sharedSecret == null || sharedSecret.isEmpty()) {
            throw new SharedKeyMissingException();
        }
        String sharedSecretProtectedString = new String(sharedSecret.getChars());
        return new HmacKey(sharedSecretProtectedString.getBytes(StandardCharsets.UTF_8));
    }

    Key retrievePublicKey(JsonWebStructure jws, String signatureAlgorithmFromJws) throws IOException, Exception {
        String kid = jws.getKeyIdHeaderValue();
        // Support for 'x5t' header remains for interoperability purposes.
        // If FIPS 140-3 is enabled, usage of the 'x5t' header for signature verification is disabled
        String x5t = CryptoUtils.isFips140_3Enabled() ? null : jws.getX509CertSha1ThumbprintHeaderValue();
        String x5tS256 = jws.getX509CertSha256ThumbprintHeaderValue();
        JwKRetriever jwkRetriever = createJwkRetriever(signatureAlgorithmFromJws);
        return jwkRetriever.getPublicKeyFromJwk(kid, x5t, x5tS256, "sig", false);
    }

    JwKRetriever createJwkRetriever(String signatureAlgorithmFromJws) throws Exception {
        String jwksUri = (remoteJwkData == null) ? null : remoteJwkData.getJwksUri();
        SSLSupport sslSupport = (remoteJwkData == null) ? null : remoteJwkData.getSslSupport();

        JwKRetriever jwkRetriever = new JwKRetriever(configId, null, jwksUri, jwkSet, sslSupport, false, null, null, signatureAlgorithmFromJws);
        // Override the retriever's HttpUtils member so we can set connection and read timeouts
        jwkRetriever.httpUtils = new HttpUtils() {
            @Override
            public HttpGet createHttpGetMethod(String url, final List<NameValuePair> commonHeaders) {
                HttpGet request = super.createHttpGetMethod(url, commonHeaders);
                if (remoteJwkData != null) {
                    RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(remoteJwkData.getJwksConnectTimeout()).setSocketTimeout(remoteJwkData.getJwksReadTimeout()).build();
                    request.setConfig(requestConfig);
                }
                return request;
            }
        };
        return jwkRetriever;
    }

    public static class Builder {

        private String configId;
        @Sensitive
        private ProtectedString sharedSecret;
        private RemoteJwkData remoteJwkData;
        private JWKSet jwkSet;

        public Builder configId(String configId) {
            this.configId = configId;
            return this;
        }

        public Builder sharedSecret(@Sensitive ProtectedString sharedSecret) {
            this.sharedSecret = sharedSecret;
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
