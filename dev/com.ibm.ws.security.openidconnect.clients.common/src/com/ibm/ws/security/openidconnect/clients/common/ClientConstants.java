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
package com.ibm.ws.security.openidconnect.clients.common;

import java.util.Arrays;
import java.util.List;

import com.ibm.ws.security.openidconnect.common.Constants;

/**
 * This is used by the openidconnect client code.
 */
public class ClientConstants {
    public static final String OIDC_CLIENT = "oidc_client";
    public static final String OIDC_AUTHN_HINT_HEADER = "oidcAuthnHint";

    public final static String JTI = "jti";
    public final static String SUB = "sub";
    public final static String SCOPE = Constants.SCOPE; // "scope";
    public final static String CLIENT_ID = Constants.CLIENT_ID; // "client_id";
    public final static String CLIENT_SECRET = Constants.CLIENT_SECRET; // "client_secret";
    public final static String CID = "cid";
    public final static String GRANT_TYPE = Constants.GRANT_TYPE; // "grant_type";
    public final static String ID_TOKEN = Constants.ID_TOKEN; // "id_token";
    public static final String TYPE_ID_TOKEN = "ID Token";
    public static final String JWT_TOKEN = "jwt"; // jwt token issued by the provider
    public static final String ISSUED_JWT_TOKEN = "issuedJwt"; // new jwt token
    public final static String TOKEN = "token";
    public final static String USER_ID = "user_id";
    public final static String USER_NAME = "user_name";
    public final static String EMAIL = "email"; // TODO: should it read from the config object
    public final static String IAT = "iat";
    public final static String EXP = "exp";
    public final static String ISS = "iss";
    public final static String AUD = "aud";

    public final static String REDIRECT_URI = Constants.REDIRECT_URI; // "redirect_uri";

    public final static String GROUPS_ID = "groupIds";
    public final static String REALM_NAME = "realmName";
    public final static String UNIQUE_SECURITY_NAME = "uniqueSecurityName";

    public static final String CHARSET = Constants.UTF_8; //"UTF-8";
    public static final String CODE = Constants.CODE; //"code";
    public static final String ID_TOKEN_TOKEN = "id_token token";
    public static final String IMPLICIT = Constants.IMPLICIT; // "implicit"
    public final static String AUTHORIZATION_CODE = "authorization_code";
    public static final String STATE = Constants.STATE; // "state";
    public static final String WAS_OIDC_STATE_KEY = "WASOidcState";
    public static final String WAS_REQ_URL_OIDC = "WASReqURLOidc";
    public static final String WAS_OIDC_CODE = "WASOidcCode";
    public static final String WAS_OIDC_NONCE = "WASOidcNonce";

    public final static String RESPONSEMAP_CODE = "RESPONSEMAP_CODE";
    public final static String RESPONSEMAP_METHOD = "RESPONSEMAP_METHOD";
    public final static String AUTHORIZATION = "Authorization";
    public final static String BEARER = "Bearer ";
    public final static List<String> primaryKeys = Arrays.asList(new String[] { JTI, SUB, SCOPE, CLIENT_ID, CID, GRANT_TYPE,
            USER_ID, USER_NAME, EMAIL, IAT, EXP, ISS, AUD });
    public final static String METHOD_BASIC = "basic";
    public final static String METHOD_POST = "post"; // used by oidc-c
    public final static String METHOD_CLIENT_SECRET_POST = "client_secret_post"; // used by social
    public final static String METHOD_JWT = "jwt";
    public final static String METHOD_SAML = "saml";

    public final static String REQ_METHOD_POST = "POST";
    public final static String REQ_CONTENT_TYPE_NAME = "Content-Type";
    public final static String REQ_CONTENT_TYPE_APP_FORM_URLENCODED = "application/x-www-form-urlencoded";

    public final static String PROPAGATION_SUPPORTED = "supported";
    public final static String PROPAGATION_REQUIRED = "required";
    public final static String PROPAGATION_NONE = "none";

    public final static String ALGORITHM_NONE = "none";

    public final static String VALIDATION_INTROSPECT = "introspect";
    public final static String VALIDATION_USERINFO = "userinfo";
    public final static String VALIDATION_LOCAL = "local";

    public final static String JWT_ACCESS_TOKEN_REMOTE_VALIDATION_NONE = "none";
    public final static String JWT_ACCESS_TOKEN_REMOTE_VALIDATION_ALLOW = "allow";
    public final static String JWT_ACCESS_TOKEN_REMOTE_VALIDATION_REQUIRE = "require";

    public final static String ATTRIB_OIDC_CLIENT_REQUEST = "com.ibm.wsspi.security.oidc.client.request";
    public static final String WLP_USER_DIR = "${wlp.user.dir}";
    // expired date is 1970 April
    public static final String STR_COOKIE_EXPIRED = " expires=Fri, 13-Apr-1970 00:00:00 GMT";
    public static final String STR_COOKIE_DO_NOT_EXPIRED = " expires=0;";
    public static final String COOKIE_NAME_OIDC_CLIENT_PREFIX = "WASOidcClient_";

    //"com.ibm.wssi.security.oidc.client.credential.storing.gmt.time";
    public static final String CREDENTIAL_STORING_TIME_MILLISECONDS = Constants.CREDENTIAL_STORING_TIME_MILLISECONDS;
    public static final String RSA = "RSA";
    public static final String EC = "EC";

}
