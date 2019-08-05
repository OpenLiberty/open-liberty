/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.jose4j;

import java.security.Key;
import java.security.interfaces.RSAPrivateKey;

import org.jose4j.keys.HmacKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

/**
 *
 */
public class JWTData {

    private static final String SIGNATURE_ALG_HS256 = "HS256";
    private static final String SIGNATURE_ALG_RS256 = "RS256";
    private static TraceComponent tc = Tr.register(JWTData.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String TYPE_ID_TOKEN = "ID Token";
    public static final String TYPE_JWT_TOKEN = "Json Web Token";

    boolean bIdToken = false;
    boolean bJwtToken = false;

    private Key _signingKey = null;
    private String _keyId = null;

    OidcServerConfig oidcServerConfig = null;
    String tokenType = TYPE_ID_TOKEN;

    String signatureAlgorithm = null;
    JWTTokenException noKeyException = null;

    public JWTData(@Sensitive String sharedKey, OidcServerConfig oidcServerConfig, String tokenType) {
        this.oidcServerConfig = oidcServerConfig;
        this.tokenType = tokenType;
        this.signatureAlgorithm = oidcServerConfig.getSignatureAlgorithm();

        bIdToken = TYPE_ID_TOKEN.equals(tokenType);
        bJwtToken = TYPE_JWT_TOKEN.equals(tokenType);
        initSigningKey(sharedKey);
    }

    /*
     * Handle the signingKey here to get the same error messages
     */
    @FFDCIgnore(Exception.class)
    @Sensitive
    protected void initSigningKey(@Sensitive String sharedKey) {
        try {
            if (this.oidcServerConfig.isJwkEnabled() && SIGNATURE_ALG_RS256.equals(signatureAlgorithm)) {
                JSONWebKey jwk = this.oidcServerConfig.getJSONWebKey();
                _signingKey = jwk.getPrivateKey();
                _keyId = jwk.getKeyID();
            } else {
                if (SIGNATURE_ALG_HS256.equals(signatureAlgorithm)) {
                    _signingKey = new HmacKey(sharedKey.getBytes("UTF-8"));
                } else if (SIGNATURE_ALG_RS256.equals(signatureAlgorithm)) {
                    _signingKey = this.oidcServerConfig.getPrivateKey();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "RSAPrivateKey: " + (_signingKey instanceof RSAPrivateKey));
                    }
                    if (!(_signingKey instanceof RSAPrivateKey)) {
                        // error handling
                        String errorMsg = Tr.formatMessage(tc, "SIGNING_KEY_NOT_RSA", new Object[] { signatureAlgorithm });
                        Object[] objs = new Object[] { signatureAlgorithm, errorMsg };
                        noKeyException = JWTTokenException.newInstance(false, "JWT_BAD_SIGNING_KEY", objs);
                        _signingKey = null; // we will catch this later in jwtSigner
                    }
                }
            }
        } catch (Exception e) {
            // UnsupportedEncodingException e (won't happen)
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception obtaining the signing key: " + e);
            }
            // error messages
            // JWT_BAD_SIGNING_KEY=CWWKS1455E: A signing key was not available. The signature algorithm is [{0}]. {1}
            Object[] objs = new Object[] { signatureAlgorithm, e.getLocalizedMessage() }; // let JWTTokenException handle the exception
            noKeyException = JWTTokenException.newInstance(false, "JWT_BAD_SIGNING_KEY", objs);
        }
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    @Sensitive
    public JWTData(Key key, String id) {
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

    public JWTTokenException getNoKeyException() {
        if (noKeyException != null) {
            return noKeyException;
        } else {
            // this should not happen
            return new JWTTokenException("No signing key found");
        }
    }

    /**
     * @return
     */
    public boolean isJwt() {
        return bJwtToken;
    }
}
