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
package com.ibm.ws.security.oauth20.plugins.jose4j;

import java.security.Key;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.jwt.utils.JwtDataConfig;
import com.ibm.ws.security.oauth20.TraceConstants;
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

    // use common class for openidconnect and jwt features, for consistent behavior
    com.ibm.ws.security.jwt.utils.JwtData wrappedJwtData;

    public JWTData(@Sensitive String sharedKey, OidcServerConfig oidcServerConfig, String tokenType) {
        this.oidcServerConfig = oidcServerConfig;
        this.tokenType = tokenType;
        this.signatureAlgorithm = oidcServerConfig.getSignatureAlgorithm();
        bIdToken = TYPE_ID_TOKEN.equals(tokenType);
        bJwtToken = TYPE_JWT_TOKEN.equals(tokenType);
        try {
            // pass null private key so key gets looked up and kid generated
            JwtDataConfig config = new JwtDataConfig(signatureAlgorithm, oidcServerConfig.getJSONWebKey(), sharedKey,
                    null, oidcServerConfig.getKeyAliasName(), oidcServerConfig.getKeyStoreRef(), tokenType,
                    oidcServerConfig.isJwkEnabled());
            wrappedJwtData = new com.ibm.ws.security.jwt.utils.JwtData(config);
            _signingKey = wrappedJwtData.getSigningKey();
            _keyId = wrappedJwtData.getKeyID();
        } catch (Exception e) {
            recordException(e);
        }
    }

    private void recordException(Exception e) {
        // UnsupportedEncodingException e (won't happen)
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Exception obtaining the signing key: " + e);
        }
        // if it's a keystore exception, tell them the likely fix.
        String extraMsg = "";
        if (e instanceof java.security.KeyStoreException) {
            extraMsg = " " + Tr.formatMessage(tc, "CHECK_KEYSTORE_REF", new Object[] {});
        }
        // error messages
        // JWT_BAD_SIGNING_KEY=CWWKS1455E: A signing key was not available. The signature algorithm is [{0}]. {1}
        Object[] objs = new Object[] { signatureAlgorithm, e.getLocalizedMessage() + extraMsg }; // let JWTTokenException handle the exception
        noKeyException = JWTTokenException.newInstance(false, "JWT_BAD_SIGNING_KEY", objs);
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
