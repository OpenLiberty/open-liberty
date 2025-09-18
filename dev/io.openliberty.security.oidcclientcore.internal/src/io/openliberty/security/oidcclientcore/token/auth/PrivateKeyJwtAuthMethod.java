/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token.auth;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.X509Util;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.security.common.ssl.SecuritySSLUtils;
import com.ibm.ws.ssl.KeyStoreService;

import io.openliberty.security.oidcclientcore.exceptions.PrivateKeyJwtAuthException;
import io.openliberty.security.oidcclientcore.exceptions.PrivateKeyJwtAuthMissingKeyException;
import io.openliberty.security.oidcclientcore.exceptions.TokenEndpointAuthMethodSettingsException;
import io.openliberty.security.oidcclientcore.token.TokenConstants;
import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;

@Component(service = PrivateKeyJwtAuthMethod.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class PrivateKeyJwtAuthMethod extends TokenEndpointAuthMethod {

    public static final TraceComponent tc = Tr.register(PrivateKeyJwtAuthMethod.class);

    private static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
    protected static volatile KeyStoreService keyStoreService;

    @Reference(name = KEY_KEYSTORE_SERVICE, policy = ReferencePolicy.DYNAMIC)
    public void setKeyStoreService(KeyStoreService keyStoreServiceSvc) {
        keyStoreService = keyStoreServiceSvc;
    }

    public void unsetKeyStoreService(KeyStoreService keyStoreServiceSvc) {
        keyStoreService = null;
    }

    public static final String AUTH_METHOD = "private_key_jwt";

    private static final float EXP_TIME_IN_MINUTES = 5;

    private String configurationId;
    private String clientId;
    private String tokenEndpoint;
    private String clientAssertionSigningAlgorithm;
    private String trustStoreRef;
    private String sslRef;
    private String keyAliasName;

    private final SecuritySSLUtils sslUtils = new SecuritySSLUtils();

    @Deprecated
    public PrivateKeyJwtAuthMethod() {
    }

    public PrivateKeyJwtAuthMethod(String configurationId, String clientId, String tokenEndpoint, String clientAssertionSigningAlgorithm,
                                   String trustStoreRef, String sslRef, String keyAliasName) throws TokenEndpointAuthMethodSettingsException {
        this.configurationId = configurationId;
        this.clientId = clientId;
        this.tokenEndpoint = tokenEndpoint;
        this.clientAssertionSigningAlgorithm = clientAssertionSigningAlgorithm;
        this.trustStoreRef = trustStoreRef;
        this.sslRef = sslRef;
        this.keyAliasName = keyAliasName;
        if (keyAliasName == null || keyAliasName.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "PRIVATE_KEY_JWT_MISSING_KEY_ALIAS_NAME", configurationId);
            throw new TokenEndpointAuthMethodSettingsException(configurationId, AUTH_METHOD, errorMsg);
        }
    }

    @Override
    @FFDCIgnore(PrivateKeyJwtAuthException.class)
    public void setAuthMethodSpecificSettings(Builder tokenRequestBuilder) throws TokenEndpointAuthMethodSettingsException {
        try {
            HashMap<String, String> customParams = getPrivateKeyJwtParameters();
            tokenRequestBuilder.customParams(customParams);
        } catch (PrivateKeyJwtAuthException e) {
            throw new TokenEndpointAuthMethodSettingsException(configurationId, AUTH_METHOD, e.getMessage());
        }
    }

    HashMap<String, String> getPrivateKeyJwtParameters() throws PrivateKeyJwtAuthException {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(TokenConstants.CLIENT_ASSERTION_TYPE, TokenConstants.CLIENT_ASSERTION_TYPE_JWT_BEARER);
        String clientAssertionJwt = createPrivateKeyJwt();
        parameters.put(TokenConstants.CLIENT_ASSERTION, clientAssertionJwt);
        return parameters;
    }

    @FFDCIgnore(Exception.class)
    public String createPrivateKeyJwt() throws PrivateKeyJwtAuthException {
        String jwt = null;
        try {
            JwtClaims claims = populateJwtClaims();
            jwt = getSignedJwt(claims);
        } catch (Exception e) {
            throw new PrivateKeyJwtAuthException(configurationId, e.getMessage());
        }
        return jwt;
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
        PrivateKey clientAssertionSigningKey = getPrivateKeyForClientAuthentication();
        if (clientAssertionSigningKey == null) {
            throw new PrivateKeyJwtAuthMissingKeyException(configurationId);
        }

        JsonWebSignature jws = new JsonWebSignature();
        setHeaderValues(jws);

        jws.setPayload(claims.toJson());

        jws.setKey(clientAssertionSigningKey);
        jws.setDoKeyValidation(false);

        Object token = ThreadIdentityManager.runAsServer();
        try {
            return jws.getCompactSerialization();
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    @Sensitive
    PrivateKey getPrivateKeyForClientAuthentication() throws Exception {
        String keyStoreRef = sslUtils.getKeyStoreRef(sslRef);
        if (keyStoreRef == null || keyStoreRef.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "PRIVATE_KEY_JWT_MISSING_KEYSTORE_REF", configurationId);
            throw new Exception(errorMsg);
        }
        try {
            return keyStoreService.getPrivateKeyFromKeyStore(keyStoreRef, keyAliasName, null);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "PRIVATE_KEY_JWT_ERROR_GETTING_PRIVATE_KEY", keyAliasName, keyStoreRef, e.getMessage());
            throw new Exception(errorMsg, e);
        }
    }

    private void setHeaderValues(JsonWebSignature jws) throws Exception {
        jws.setAlgorithmHeaderValue(clientAssertionSigningAlgorithm);
        jws.setHeader("typ", "JWT");
        jws.setHeader(CryptoUtils.isFips140_3EnabledWithBetaGuard() ? "x5t#S256" : "x5t", getX5tForPublicKey());
    }

    String getX5tForPublicKey() throws Exception {
        // trustStoreRef takes precedence over the sslRef
        X509Certificate x509Cert = getX509CertificateFromTrustStoreRef();
        if (x509Cert == null) {
            x509Cert = getX509CertificateFromSslRef();
        }
        return CryptoUtils.isFips140_3EnabledWithBetaGuard() ? X509Util.x5tS256(x509Cert) : X509Util.x5t(x509Cert);
    }

    @FFDCIgnore(Exception.class)
    X509Certificate getX509CertificateFromTrustStoreRef() throws Exception {
        if (trustStoreRef == null || trustStoreRef.isEmpty()) {
            return null;
        }
        try {
            return keyStoreService.getX509CertificateFromKeyStore(trustStoreRef, keyAliasName);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Encountered an error loading the [{0}] key from the [{1}] trustStoreRef: {2}", keyAliasName, trustStoreRef, e.getMessage());
            }
            return null;
        }
    }

    X509Certificate getX509CertificateFromSslRef() throws Exception {
        String storeRef = sslUtils.getTrustStoreRef(sslRef);
        if (storeRef == null || storeRef.isEmpty()) {
            storeRef = sslUtils.getKeyStoreRef(sslRef);
        }
        try {
            return keyStoreService.getX509CertificateFromKeyStore(storeRef, keyAliasName);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "PRIVATE_KEY_JWT_ERROR_GETTING_PUBLIC_KEY", keyAliasName, storeRef, e.getMessage());
            throw new Exception(errorMsg, e);
        }
    }

}
