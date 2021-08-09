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
package com.ibm.ws.security.openidconnect.web;

import com.ibm.ws.security.openidconnect.token.JWSHeader;
import com.ibm.ws.security.openidconnect.token.JWT;
import com.ibm.ws.security.openidconnect.token.JWTPayload;

/*
 * Genereate JWT which is a super class of IdToken which can be used for the test.
 */
public class IDTokenUtil {
    //public static final String KEY_STRING = "secret";  // need >=256bit key for jose4j
    public static final String KEY_STRING = "secretsecretsecretsecretsecretsecret";
    public static final String ALGORITHM = "HS256";
    public static final String ISSUER = "http://localhost:8010/oidc";
    public static final String AUDIENCE = "client01";
    public static final String SUBJECT = "user1";

    public static String createIdTokenString() {
        String output = null;
        try {
            JWSHeader header = new JWSHeader();
            JWTPayload payload = new JWTPayload();
            byte[] key = KEY_STRING.getBytes("UTF-8");

            header.setAlgorithm(ALGORITHM);
            payload.setIssuer(ISSUER);
            payload.setAudience(AUDIENCE);
            payload.setSubject(SUBJECT);
            JWT jwt = new JWT(header, payload, key);
            output = jwt.getSignedJWTString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

}
