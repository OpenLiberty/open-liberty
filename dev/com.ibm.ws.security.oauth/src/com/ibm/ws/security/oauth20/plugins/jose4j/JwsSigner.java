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

import java.security.AccessController;
import java.security.Key;
import java.security.PrivilegedAction;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

/**
 *
 */
public class JwsSigner {

    private static TraceComponent tc = Tr.register(JwsSigner.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    // set org.jose4j.jws.default-allow-none to true to behave the same as old jwt
    // allow signatureAlgorithme as none
    static {
        AccessController
                .doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.setProperty("org.jose4j.jws.default-allow-none", "true");
                    }
                });
    };

    @FFDCIgnore({ Exception.class })
    public static String getSignedJwt(JwtClaims claims, OidcServerConfig oidcServerConfig, JWTData jwtData) throws JWTTokenException {
        String jwt = null;
        // A JWT is a JWS and/or a JWE with JSON claims as the payload.
        // In this example it is a JWS so we create a JsonWebSignature object.
        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(claims.toJson());

        Key key = jwtData.getSigningKey();
        if (key == null && !"none".equals(jwtData.getSignatureAlgorithm())) {
            throw jwtData.getNoKeyException();
        }
        // The JWT is signed using the private key
        jws.setKey(key);

        // Set the Key ID (kid) header because it's just the polite thing to do.
        // We only have one key in this example but a using a Key ID helps
        // facilitate a smooth key rollover process
        String keyId = jwtData.getKeyID();
        if (keyId != null) {
            jws.setKeyIdHeaderValue(keyId);
        }

        // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
        jws.setAlgorithmHeaderValue(oidcServerConfig.getSignatureAlgorithm());
        jws.setDoKeyValidation(false);

        // Sign the JWS and produce the compact serialization or the complete JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        // If you wanted to encrypt it, you can simply set this jwt as the payload
        // of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
        try {
            jwt = jws.getCompactSerialization();
        } catch (Exception e) {
            Object[] objs = new Object[] { oidcServerConfig.getProviderId(), e.getLocalizedMessage() };
            Tr.error(tc, "JWT_CANNOT_GENERATE_JWT", objs);
            throw new JWTTokenException(Tr.formatMessage(tc, "JWT_CANNOT_GENERATE_JWT", objs), e);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JWT=", jwt);
        }
        return jwt;
    }

}
