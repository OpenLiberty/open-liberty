/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;

import org.jose4j.keys.HmacKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.crypto.KeyAlgorithmChecker;
import com.ibm.ws.security.common.jwk.impl.JWKProvider;
import com.ibm.ws.security.common.jwk.impl.JwkKidBuilder;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.internal.BuilderImpl;
import com.ibm.ws.security.jwt.internal.JwtTokenException;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;

/**
 *
 */
public class JwtData {

    private static final TraceComponent tc = Tr.register(JwtData.class);

    private static final String SIGNATURE_ALG_HS256 = "HS256";
    private static final String SIGNATURE_ALG_RS256 = "RS256";
    private static final String SIGNATURE_ALG_NONE = "none";

    // public static final String TYPE_ID_TOKEN = "ID Token";
    public static final String TYPE_JWT_TOKEN = "Json Web Token";

    boolean bIdToken = false;
    boolean bJwtToken = false;

    private Key _signingKey = null;
    private String _keyId = null;

    JwtConfig jwtConfig = null;
    JwtDataConfig jwtDataConfig = null;
    String tokenType = TYPE_JWT_TOKEN;
    JWKProvider jwkProvider = null;

    String signatureAlgorithm = null;
    JwtTokenException noKeyException = null;

    private final KeyAlgorithmChecker keyAlgChecker = new KeyAlgorithmChecker();

    public JwtData(BuilderImpl jwtBuilder, JwtConfig jwtConfig, String tokenType) throws JwtTokenException {
        this.jwtConfig = jwtConfig;
        this.tokenType = tokenType;
        signatureAlgorithm = jwtBuilder.getAlgorithm();
        bJwtToken = TYPE_JWT_TOKEN.equals(tokenType);
        String sharedKey = jwtBuilder.getSharedKey();
        sharedKey = (JwtUtils.isNullEmpty(sharedKey) ? jwtConfig.getSharedKey() : sharedKey);
        jwtDataConfig = new JwtDataConfig(jwtBuilder.getAlgorithm(), jwtConfig.getJSONWebKey(), sharedKey,
                jwtBuilder.getKey(), jwtConfig.getKeyAlias(), jwtConfig.getKeyStoreRef(), tokenType,
                jwtConfig.isJwkEnabled());
        initSigningKey(jwtDataConfig);
    }

    public JwtData(JwtDataConfig config) throws JwtTokenException {
        tokenType = config.tokenType;
        jwtDataConfig = config;
        signatureAlgorithm = config.signatureAlgorithm;
        bJwtToken = TYPE_JWT_TOKEN.equals(tokenType);
        initSigningKey(jwtDataConfig);
    }

    public JwtConfig getConfig() {
        return jwtConfig;
    }

    /*
     * Handle the signingKey here to get the same error messages
     */
    @FFDCIgnore(Exception.class)
    // protected void initSigningKey(BuilderImpl jwtBuilder, JwtConfig jwtConfig)
    // throws JwtTokenException {
    protected void initSigningKey(JwtDataConfig config) throws JwtTokenException {
        String keyType = Constants.SIGNING_KEY_X509;
        try {
            if (isJwkSignatureAlgorithmType(config)) {
                initSigningKeyUsingJwk(config);
            } else {
                if (keyAlgChecker.isHSAlgorithm(signatureAlgorithm)) {
                    initSigningKeyUsingHSAlgorithm(config);
                } else if (isSignatureAlgorithmUsingKeyStore()) {
                    initSigningKeyUsingKeyStore(config, keyType);
                }
            }
        } catch (Exception e) {
            Object[] objs = new Object[] { signatureAlgorithm, jwtDataConfig.isJwkEnabled, e.getLocalizedMessage() }; // let JWTTokenException handle the exception
            JwtTokenException jte = JwtTokenException.newInstance(false, "JWT_NO_SIGNING_KEY_WITH_ERROR", objs);
            jte.initCause(e);
            throw jte;
        }
        if (_signingKey == null && !signatureAlgorithm.equals(SIGNATURE_ALG_NONE)) {
            Object[] objs = new Object[] { signatureAlgorithm, jwtDataConfig.isJwkEnabled, "" }; // let JWTTokenException handle the exception
            throw JwtTokenException.newInstance(true, "JWT_NO_SIGNING_KEY_WITH_ERROR", objs);
        }
    }

    boolean isJwkSignatureAlgorithmType(JwtDataConfig config) {
        // RSxxx or ESxxx signature algorithms
        return config.isJwkEnabled && (keyAlgChecker.isRSAlgorithm(config.signatureAlgorithm)
                || keyAlgChecker.isESAlgorithm(config.signatureAlgorithm));
    }

    boolean isSignatureAlgorithmUsingKeyStore() {
        // RSxxx or ESxxx signature algorithms
        return (keyAlgChecker.isRSAlgorithm(signatureAlgorithm) || keyAlgChecker.isESAlgorithm(signatureAlgorithm));
    }

    void initSigningKeyUsingJwk(JwtDataConfig config) {
        String keyType = Constants.SIGNING_KEY_JWK;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JWK + RS256 Signing key type is " + keyType);
        }
        JSONWebKey jwk = config.jwk;
        if (jwk == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not succcessfully build a JWK");
            }
            _signingKey = null;
            _keyId = null;
        } else {
            _signingKey = jwk.getPrivateKey();
            _keyId = jwk.getKeyID();
        }
    }

    void initSigningKeyUsingHSAlgorithm(JwtDataConfig config) throws UnsupportedEncodingException {
        String keyType = Constants.SIGNING_KEY_SECRET;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "hs256 Signing key type is " + keyType);
        }
        String sharedKey = config.sharedKey;
        if (!(sharedKey == null || sharedKey.isEmpty())) {
            _signingKey = new HmacKey(sharedKey.getBytes("UTF-8"));
        } else {
            _signingKey = null;
        }
    }

    void initSigningKeyUsingKeyStore(JwtDataConfig config, String keyType) throws KeyStoreException, CertificateException, InvalidTokenException {
        _signingKey = config.signingKey;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Signing key type is " + keyType);
        }
        if (_signingKey == null) {
            String keyAlias = null;
            String keyStoreRef = null;
            keyAlias = config.keyAlias;
            keyStoreRef = config.keyStoreRef;
            _signingKey = JwtUtils.getPrivateKey(keyAlias, keyStoreRef);
            _keyId = buildKidFromPublicKey(JwtUtils.getPublicKey(keyAlias, keyStoreRef));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Key alias: " + keyAlias + ", Keystore: " + keyStoreRef + ", kid: " + _keyId);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "RSAPrivateKey: " + (_signingKey instanceof RSAPrivateKey));
        }
        if (!keyAlgChecker.isPrivateKeyValidType(_signingKey, signatureAlgorithm)) {
            // error handling
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "clear _signingKey and _keyId");
            }
            _signingKey = null; // we will catch this later in jwtSigner
            _keyId = null;
        }
    }

    private String buildKidFromPublicKey(PublicKey cert) {
        JwkKidBuilder kidbuilder = new JwkKidBuilder();
        return kidbuilder.buildKeyId(cert);
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    @Sensitive
    public JwtData(Key key, String id) {
        _signingKey = key;
        _keyId = id;
    }

    @Sensitive
    public Key getSigningKey() {
        return _signingKey;
    }

    public String getKeyID() {
        return _keyId;
    }

    /**
     * @return
     */
    public String getTokenType() {
        return tokenType;
    }

    public JwtTokenException getNoKeyException() {
        if (noKeyException != null) {
            return noKeyException;
        } else {
            // this should not happen
            return new JwtTokenException("No signing key found");
        }
    }

    /**
     * @return
     */
    public boolean isJwt() {
        return bJwtToken;
    }
}
