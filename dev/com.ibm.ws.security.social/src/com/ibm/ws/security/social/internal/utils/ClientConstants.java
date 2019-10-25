/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

public class ClientConstants {
    public static final String OIDC_CLIENT = "oidc_client";
    public static final String OIDC_AUTHN_HINT_HEADER = "oidcAuthnHint";

    public final static String SCOPE = "scope";
    public final static String CLIENT_ID = "client_id";
    public final static String CLIENT_SECRET = "client_secret";
    public final static String GRANT_TYPE = "grant_type";
    public final static String ID_TOKEN = "id_token";
    public final static String TOKEN = "token";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String JWT_TOKEN = "jwt";
    public static final String ISSUED_JWT_TOKEN = "issuedJwt";
    public static final String EXPIRES_IN = "expires_in";
    public static final String SOCIAL_MEDIA = "social_media";
    public static final String ENCRYPTED_TOKEN = "encrypted_token";
    public static final String ACCESS_TOKEN_ALIAS = "accessTokenAlias";
    public static final String RESPONSE_TYPE = "response_type";
    public static final String RESPONSE_MODE = "response_mode";
    public static final String FORM_POST = "form_post";
    
    public final static String REQ_METHOD_POST = "POST";
    public final static String REQ_CONTENT_TYPE_NAME = "Content-Type";
    public final static String REQ_CONTENT_TYPE_APP_FORM_URLENCODED = "application/x-www-form-urlencoded";

    public final static String USER_ID = "user_id";
    public final static String USER_NAME = "user_name";
    public final static String EMAIL = "email";

    public final static String REDIRECT_URI = "redirect_uri";

    public static final String CHARSET = "UTF-8";
    public static final String CODE = "code";
    public final static String AUTHORIZATION_CODE = "authorization_code";
    public final static String IMPLICIT = "implicit";
    public static final String STATE = "state";

    public final static String RESPONSEMAP_CODE = "RESPONSEMAP_CODE";
    public final static String RESPONSEMAP_METHOD = "RESPONSEMAP_METHOD";
    public final static String AUTHORIZATION = "Authorization";
    public final static String BEARER = "bearer ";
    public final static String METHOD_client_secret_basic = "client_secret_basic";
    public final static String METHOD_client_secret_post = "client_secret_post";
    public final static String LOGIN_HINT = "social_login_hint";

    public final static String ATTRIB_OIDC_CLIENT_REQUEST = "com.ibm.wsspi.security.oidc.client.request";
    // public static final String CONTEXT_ROOT = "/ibm/api/social-login";

    // expired date is 1970 April
    public static final String STR_COOKIE_EXPIRED = " expires=Fri, 13-Apr-1970 00:00:00 GMT";
    public static final String COOKIE_NAME_REQ_URL_PREFIX = "WASReqUrlSocial_";
    public static final String COOKIE_NAME_STATE_KEY = "WASSocialState";
    public static final String COOKIE_NAME_MATCHING_CONFIG_IDS = "WASSocialMatchingConfigIds";
    public static final String COOKIE_NAME_SIGN_IN_REQ_URL = "WASSocialSignInReqUrl";
    public static final String MATCHING_CONFIG_IDS_COOKIE_DELIM = ",";
    public static final int SIGN_IN_COOKIES_EXPIRY_SEC = 5 * 60;

    // added for github error case, not sure if these are part of any spec
    public static final String ERROR = "error";
    public static final String ERROR_DESC = "error_description";
    public static final String ERROR_URI = "error_uri";

}
