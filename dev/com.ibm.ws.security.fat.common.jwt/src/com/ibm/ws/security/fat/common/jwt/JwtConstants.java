/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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

    public static final String SIGALG_RS256 = "RS256";
    public static final String SIGALG_HS256 = "HS256";

    /************************* misc JWT *****************************/
    public static final String JWT_ISSUER_CLAIM = "/ibm/api/jwt/";
    public static final String JWT_DEFAULT_EXPIRY = "2h";
    public static final String BUILT_JWT_TOKEN = "Built JWT Token: ";
    public static final String BUILD_JWT_TOKEN = "build_jwt_token";
    public static final String VALIDATE_JWT_CLAIMS = "validate_jwt_claims";
    public static final String VALIDATE_A_JWT_CLAIM = "validate_a_jwt_claim";
    public static final String REMOVE_JWT_CLAIMS = "remove_jwt_claims";
    public static final String CORRECTLY_CAUGHT = "Correctly Caught: ";
    public static final String JWT_DELIMITER = ".";

    /********************************* API Client Servlet ********************************/
    public static final String JWT_BUILDER_ACTION_BUILD = "build_token";
    public static final String JWT_BUILDER_ACTION_DEFAULT = "build_default_token";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM = "claim_from";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN = "claim_from_JwtToken";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING = "claim_from_JwtString";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN_NULL = "claim_from_JwtToken_null";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING_NULL = "claim_from_JwtString_null";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_ENCODED_PAYLOAD = "claim_from_EncodedPayload";
    public static final String JWT_BUILDER_ACTION_CLAIM_FROM_DECODED_PAYLOAD = "claim_from_DecodedPayload";
    public static final String JWT_BUILDER_FINAL_TOKEN = "FinalJWTToken: ";
    public static final String JWT_BUILDER_TOKEN_1 = "JWTToken1: ";
    public static final String JWT_BUILDER_BEFORE_REMOVE_TOKEN = "BeforeRemoveJWTToken: ";
    public static final String JWT_BUILDER_HEADER = "Header: ";
    public static final String JWT_BUILDER_PAYLOAD = "Payload: ";
    public static final String JWT_BUILDER_KEYID = "KeyId: ";
    public static final String JWT_BUILDER_ALGORITHM = "Algorithm: ";
    public static final String JWT_BUILDER_ISSUER = "Issuer: ";
    public static final String JWT_BUILDER_SUBJECT = "Subject: ";
    public static final String JWT_BUILDER_AUDIENCE = "Audience: ";
    public static final String JWT_BUILDER_EXPIRATION = "Expiration: ";
    public static final String JWT_BUILDER_NOTBEFORE = "NotBefore: ";
    public static final String JWT_BUILDER_ISSUED_AT = "IssuedAt: ";
    public static final String JWT_BUILDER_JWTID = "JwtId: ";
    public static final String JWT_BUILDER_AUTHORIZEDPARTY = "AuthorizedParty: ";
    public static final String JWT_BUILDER_JSON = "JSON: ";
    public static final String JWT_BUILDER_CLAIM = "Claim: ";
    public static final String JWT_BUILDER_GETCLAIM = "getClaim: ";
    public static final String JWT_BUILDER_GETALLCLAIMS = "getAllClaims: ";
    public static final String JWT_BUILDER_KEY = "Key: ";
    public static final String JWT_BUILDER_VALUE = "Value: ";
    public static final String JWT_BUILDER_NO_CLAIMS = "No Claims";
    public static final String JWT_BUILDER_NO_TOKEN = "No Token";
    public static final String JWT_BUILDER_NOT_SET = "Not Set";
    public static final String JWT_BUILDER_ADD_AUD = "Adding audiences";
    public static final String JWT_BUILDER_ADD_CLAIMS = "Adding claims";
    public static final String JWT_BUILDER_SET_EXP = "Setting Expiration time:";
    public static final String JWT_BUILDER_SET_NBF = "Setting NotBefore time:";
    public static final String JWT_BUILDER_FETCH = "Fetching: ";
    public static final String JWT_BUILDER_SIGN_WITH = "Setting signWith: ";
    public static final String JWT_BUILDER_LOAD_CLAIMS = "Load JWT Token Claims";
    public static final String JWT_BUILDER_SET_JTI = "Setting JTI:";
    public static final String JWT_BUILDER_SET_SUB = "Setting Subject:";
    public static final String JWT_BUILDER_SET_ISS = "Setting Issuer:";
    public static final String JWT_BUILDER_REMOVE = "Removing:";
    public static final String JWT_BUILDER_DEFAULT_ID = "defaultJWT";
    public static final String JWT_BUILDER_NAME_ATTR = "Name";
    public static final String JWT_CONTEXT_NULL = "JsonWebToken from SecurityContext was null";

    /********************************* JWT Consumer API Servlet ********************************/
    public static final String JWT_CONSUMER_SERVLET = "jwtconsumerclient";
    public static final String JWT_CONSUMER_ENDPOINT = JWT_CONSUMER_SERVLET + "/JwtConsumerClient";
    public static final String JWT_CONSUMER_PARAM_CLIENT_ID = "clientId";
    public static final String JWT_CONSUMER_PARAM_JWT = "jwtString";
    public static final String JWT_CONSUMER_START_SERVLET_OUTPUT = "Start of JwtConsumerClient output";
    public static final String JWT_CONSUMER_SUCCESSFUL_CONSUMER_CREATION = "Successfully created consumer for id [";
    public static final String JWT_CONSUMER_TOKEN_LINE = BUILT_JWT_TOKEN;
    public static final String JWT_CONSUMER_CLAIM = "JWT Consumer Claim: ";
    public static final String JWT_CONSUMER_TOKEN_HEADER_MALFORMED = "Header malformed: ";
    public static final String JWT_CONSUMER_TOKEN_HEADER_JSON = "JSON Header: ";
    public static final String JWT_REALM_NAME = "realmName";
}