/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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
    public static final String PARAM_KEY_MGMT_ALG = "keyManagementAlg";
    public static final String PARAM_CONTENT_ENCRYPT_ALG = "contentEncryptionAlg";
    public static final String PARAM_ENCRYPT_KEY = "encrypt_key";

    public static final String X509_CERT = "x509_cert";
    public static final String JWK_CERT = "jwk_cert";

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
    public static final String JWE_TOKEN_HEADER_JSON = "JWE JSON Header: ";
    public static final String DEFAULT_KEY_MGMT_KEY_ALG = "RSA-OAEP";
    public static final String DEFAULT_CONTENT_ENCRYPT_ALG = "A256GCM";
    public static final String KEY_MGMT_KEY_ALG_256 = "RSA-OAEP-256";
    public static final String CONTENT_ENCRYPT_ALG_192 = "A192GCM";

    public static final String BOOTSTRAP_PROP_ENCRYPTION_SETTING = "fat.server.encryption.setting";

}
