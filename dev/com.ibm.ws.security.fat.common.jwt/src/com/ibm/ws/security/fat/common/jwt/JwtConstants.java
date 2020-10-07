/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt;

import com.ibm.ws.security.fat.common.Constants;

public class JwtConstants extends Constants {

    public static final String JWT_COOKIE_NAME = "JWT";

    public static final String PARAM_BUILDER_ID = "builder_id";
    public static final String PARAM_UPN = "upn";

    public static final String X509_CERT = "x509_cert";
    public static final String JWK_CERT = "jwk_cert";

    public static final String SIGALG_HS256 = "HS256";
    public static final String SIGALG_HS384 = "HS384";
    public static final String SIGALG_HS512 = "HS512";

    public static final String SIGALG_RS256 = "RS256";
    public static final String SIGALG_RS384 = "RS384";
    public static final String SIGALG_RS512 = "RS512";

    public static final String SIGALG_ES256 = "ES256";
    public static final String SIGALG_ES384 = "ES384";
    public static final String SIGALG_ES512 = "ES512";

    public static final String SIGALG_PS256 = "PS256";
    public static final String SIGALG_PS384 = "PS384";
    public static final String SIGALG_PS512 = "PS512";

    public static final String ENCRYPT_RS256 = "RS256";
    public static final String ENCRYPT_RS384 = "RS384";
    public static final String ENCRYPT_RS512 = "RS512";

    public static final String JWT_ISSUER_CLAIM = "/ibm/api/jwt/";
    public static final String BUILT_JWT_TOKEN = "Built JWT Token: ";
    public static final String JWT_DELIMITER = ".";
    public static final String JWT_TOKEN_HEADER = "Header: ";
    public static final String JWT_TOKEN_PAYLOAD = "Payload: ";
    public static final String JWT_JSON = "JSON: ";
    public static final String JWT_CLAIM = "Claim: ";
    public static final String JWT_GETCLAIM = "getClaim: ";
    public static final String JWT_GETALLCLAIMS = "getAllClaims: ";
    public static final String JWT_CLAIM_KEY = "Key: ";
    public static final String JWT_CLAIM_VALUE = "Value: ";
    public static final String NO_JWT_CLAIMS = "No Claims";
    public static final String NO_JWT_TOKEN = "No Token";
    public static final String JWT_MALFORMED_TOKEN_HEADER = "Header malformed: ";
    public static final String JWT_TOKEN_HEADER_JSON = "JSON Header: ";
}
