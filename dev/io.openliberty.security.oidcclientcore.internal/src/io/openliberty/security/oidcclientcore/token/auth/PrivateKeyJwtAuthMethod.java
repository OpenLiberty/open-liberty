/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token.auth;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.security.oidcclientcore.exceptions.PrivateKeyJwtAuthException;
import io.openliberty.security.oidcclientcore.exceptions.PrivateKeyJwtAuthMissingKeyException;
import io.openliberty.security.oidcclientcore.exceptions.TokenEndpointAuthMethodSettingsException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.token.TokenConstants;
import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;

public class PrivateKeyJwtAuthMethod {

    public static final TraceComponent tc = Tr.register(PrivateKeyJwtAuthMethod.class);

    public static final String AUTH_METHOD = "private_key_jwt";

    private static final float EXP_TIME_IN_MINUTES = 5;

    private static boolean issuedBetaMessage = false;

    private final String clientId;
    private final String tokenEndpoint;
    private final String clientAssertionSigningAlgorithm;
    @Sensitive
    private final Key clientAssertionSigningKey;

    public PrivateKeyJwtAuthMethod(String clientId, String tokenEndpoint, String clientAssertionSigningAlgorithm, @Sensitive Key clientAssertionSigningKey) {
        this.clientId = clientId;
        this.tokenEndpoint = tokenEndpoint;
        this.clientAssertionSigningAlgorithm = clientAssertionSigningAlgorithm;
        this.clientAssertionSigningKey = clientAssertionSigningKey;
    }

    public String createPrivateKeyJwt() throws PrivateKeyJwtAuthException {
        String jwt = null;
        try {
            if (clientAssertionSigningKey == null) {
                throw new PrivateKeyJwtAuthMissingKeyException(clientId);
            }
            JwtClaims claims = populateJwtClaims();
            jwt = getSignedJwt(claims);
        } catch (Exception e) {
            throw new PrivateKeyJwtAuthException(clientId, e.getMessage());
        }
        return jwt;
    }

    public void setAuthMethodSpecificSettings(Builder tokenRequestBuilder) throws TokenEndpointAuthMethodSettingsException {
        try {
            tokenRequestBuilder.clientAssertionSigningAlgorithm(clientAssertionSigningAlgorithm);
            tokenRequestBuilder.clientAssertionSigningKey(clientAssertionSigningKey);
        } catch (Exception e) {
            throw new TokenEndpointAuthMethodSettingsException(clientId, AUTH_METHOD, e.getMessage());
        }
    }

    void addPrivateKeyJwtParameters(List<NameValuePair> params) throws TokenRequestException {
        if (!isRunningBetaMode()) {
            return;
        }
        params.add(new BasicNameValuePair(TokenConstants.CLIENT_ASSERTION_TYPE, TokenConstants.CLIENT_ASSERTION_TYPE_JWT_BEARER));
        String clientAssertionJwt = buildClientAssertionJwt();
        params.add(new BasicNameValuePair(TokenConstants.CLIENT_ASSERTION, clientAssertionJwt));
    }

    private boolean isRunningBetaMode() {
        if (!ProductInfo.getBetaEdition()) {
            return false;
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
            return true;
        }
    }

    private String buildClientAssertionJwt() throws TokenRequestException {
        PrivateKeyJwtAuthMethod pkjAuthMethod = new PrivateKeyJwtAuthMethod(clientId, tokenEndpoint, clientAssertionSigningAlgorithm, clientAssertionSigningKey);
        try {
            return pkjAuthMethod.createPrivateKeyJwt();
        } catch (PrivateKeyJwtAuthException e) {
            throw new TokenRequestException(clientId, e.getMessage(), e);
        }
    }

    private JwtClaims populateJwtClaims() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(clientId);
        claims.setSubject(clientId);
        claims.setAudience(tokenEndpoint);
        claims.setExpirationTimeMinutesInTheFuture(EXP_TIME_IN_MINUTES);
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        return claims;
    }

    private String getSignedJwt(JwtClaims claims) throws Exception {
        JsonWebSignature jws = new JsonWebSignature();
        setHeaderValues(jws);

        jws.setPayload(claims.toJson());

        jws.setKey(clientAssertionSigningKey);
        jws.setDoKeyValidation(false);

        return jws.getCompactSerialization();
    }

    @FFDCIgnore(NoSuchAlgorithmException.class)
    private void setHeaderValues(JsonWebSignature jws) {
        jws.setAlgorithmHeaderValue(clientAssertionSigningAlgorithm);
        jws.setHeader("typ", "JWT");
        try {
            String x5t = new String(Base64.getUrlEncoder().encode(MessageDigest.getInstance("SHA-1").digest(clientAssertionSigningKey.getEncoded())));
            jws.setHeader("x5t", x5t);
        } catch (NoSuchAlgorithmException e) {
            // SHA-1 should be supported; should be able to ignore
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting x5t for signing key: " + e);
            }
        }
    }
}
