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

import java.security.AccessController;
import java.security.Key;
import java.security.PrivilegedAction;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.internal.JwtTokenException;

/**
 *
 */
public class JwsSigner {

    // private static TraceComponent tc = Tr.register(JwsSigner.class,
    // TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    // set org.jose4j.jws.default-allow-none to true to behave the same as old
    // jwt
    // allow signatureAlgorithme as none
    static {
        AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.setProperty("org.jose4j.jws.default-allow-none", "true");
            }
        });
    };

    @FFDCIgnore({ Exception.class })
    public static String getSignedJwt(JwtClaims claims, JwtData jwtData) throws JwtTokenException {
        String jwt = null;
        // jwtData.getConfig();

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

        // Set the signature algorithm on the JWT/JWS that will integrity
        // protect the claims
        jws.setAlgorithmHeaderValue(jwtData.getSignatureAlgorithm());
        jws.setDoKeyValidation(false);

        // Sign the JWS and produce the compact serialization or the complete
        // JWT/JWS
        // representation, which is a string consisting of three dot ('.')
        // separated
        // base64url-encoded parts in the form Header.Payload.Signature
        // If you wanted to encrypt it, you can simply set this jwt as the
        // payload
        // of a JsonWebEncryption object and set the cty (Content Type) header
        // to "jwt".

        try {
            jwt = jws.getCompactSerialization();
        } catch (Exception e) {
            // Tr.error(tc, "JWT_CANNOT_GENERATE_JWT", objs);
            throw new JwtTokenException(e.getLocalizedMessage(), e);
            //            throw new JwtTokenException(/*
            //                                         * Tr.formatMessage(tc,
            //                                         * "JWT_CANNOT_GENERATE_JWT", objs),
            //                                         */"Can not generate JWT", e);
        }
        // if (tc.isDebugEnabled()) {
        // Tr.debug(tc, "JWT=", jwt);
        // }
        return jwt;
    }

}
