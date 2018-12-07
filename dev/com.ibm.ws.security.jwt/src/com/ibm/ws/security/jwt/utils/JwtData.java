/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import java.security.Key;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;

import org.jose4j.keys.HmacKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
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

    // public static final String TYPE_ID_TOKEN = "ID Token";
    public static final String TYPE_JWT_TOKEN = "Json Web Token";

    boolean bIdToken = false;
    boolean bJwtToken = false;

    private Key _signingKey = null;
    private String _keyId = null;

    JwtConfig jwtConfig = null;
    String tokenType = TYPE_JWT_TOKEN;
    JWKProvider jwkProvider = null;

    String signatureAlgorithm = null;
    JwtTokenException noKeyException = null;

    public JwtData(BuilderImpl jwtBuilder, JwtConfig jwtConfig, String tokenType) throws JwtTokenException {
        this.jwtConfig = jwtConfig;
        this.tokenType = tokenType;
        signatureAlgorithm = jwtBuilder.getAlgorithm();

        bJwtToken = TYPE_JWT_TOKEN.equals(tokenType);
        initSigningKey(jwtBuilder, jwtConfig);
    }

    public JwtConfig getConfig() {
        return jwtConfig;

    }

    /*
     * Handle the signingKey here to get the same error messages
     */
    @FFDCIgnore(Exception.class)
    protected void initSigningKey(BuilderImpl jwtBuilder, JwtConfig jwtConfig) throws JwtTokenException {
        String keyType = Constants.SIGNING_KEY_X509;
        try {
            if (jwtConfig.isJwkEnabled() && SIGNATURE_ALG_RS256.equals(signatureAlgorithm)) {
                keyType = Constants.SIGNING_KEY_JWK;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Signing key type is " + keyType);
                }
                JSONWebKey jwk = jwtConfig.getJSONWebKey();
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
            } else {
                if (SIGNATURE_ALG_HS256.equals(signatureAlgorithm)) {
                    keyType = Constants.SIGNING_KEY_SECRET;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Signing key type is " + keyType);
                    }
                    String sharedKey = jwtBuilder.getSharedKey();
                    if (JwtUtils.isNullEmpty(sharedKey)) {
                        sharedKey = jwtConfig.getSharedKey();
                    }
                    if (!JwtUtils.isNullEmpty(sharedKey)) {
                        _signingKey = new HmacKey(sharedKey.getBytes("UTF-8"));
                    } else {
                        _signingKey = null;
                    }

                } else if (SIGNATURE_ALG_RS256.equals(signatureAlgorithm)) {
                    _signingKey = jwtBuilder.getKey();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Signing key type is " + keyType);
                    }
                    if (_signingKey == null) {
                        String keyAlias = null;
                        String keyStoreRef = null;
                        keyAlias = jwtConfig.getKeyAlias();
                        keyStoreRef = jwtConfig.getKeyStoreRef();
                        _signingKey = JwtUtils.getPrivateKey(keyAlias, keyStoreRef);
                        _keyId = buildKidFromPublicKey(JwtUtils.getPublicKey(keyAlias, keyStoreRef));
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Key alias: " + keyAlias + ", Keystore: " + keyStoreRef+ ", kid: " + _keyId);
                        }
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "RSAPrivateKey: " + (_signingKey instanceof RSAPrivateKey));
                    }
                    if (_signingKey != null && !(_signingKey instanceof RSAPrivateKey)) {
                        // error handling
                        // String errorMsg = Tr.formatMessage(tc, "SIGNING_KEY_NOT_RSA", new Object[] { signatureAlgorithm });
                        // Object[] objs = new Object[] { signatureAlgorithm, errorMsg };
                        // noKeyException = JWTTokenException.newInstance(false, "JWT_BAD_SIGNING_KEY", objs);
                        _signingKey = null; // we will catch this later in jwtSigner
                        _keyId = null;
                    }
                }
            }
        } catch (Exception e) {
            // UnsupportedEncodingException e (won't happen)
            // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            // Tr.debug(tc, "Exception obtaining the signing key: " + e);
            // }
            // error messages
            // JWT_BAD_SIGNING_KEY=CWWKS1455E: A signing key was not available. The signature algorithm is [{0}]. {1}

            Object[] objs = new Object[] { signatureAlgorithm, jwtConfig.isJwkEnabled(), e.getLocalizedMessage() }; // let JWTTokenException handle the exception
            JwtTokenException jte = JwtTokenException.newInstance(false, "JWT_NO_SIGNING_KEY_WITH_ERROR", objs);
            jte.initCause(e);
            throw jte;
        }
        if (_signingKey == null) {
            Object[] objs = new Object[] { signatureAlgorithm, jwtConfig.isJwkEnabled(), "" }; // let JWTTokenException handle the exception
            throw JwtTokenException.newInstance(true, "JWT_NO_SIGNING_KEY_WITH_ERROR", objs);
        }
    }
    
    private String buildKidFromPublicKey(PublicKey cert)  {
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
    Key getSigningKey() {
        return _signingKey;
    }

    String getKeyID() {
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
