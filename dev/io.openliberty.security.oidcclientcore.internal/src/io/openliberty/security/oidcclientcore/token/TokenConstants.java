/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

public class TokenConstants {

    public static final String CODE = "code";
    public static final String AUTHORIZATION_CODE = "authorization_code";

    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String GRANT_TYPE = "grant_type";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    public static final String CLIENT_ASSERTION = "client_assertion";
    public static final String CLIENT_ASSERTION_TYPE_JWT_BEARER = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    public static final String RESOURCE = "resource";
    public static final String METHOD_BASIC = "basic";
    public static final String METHOD_POST = "post";
    public static final String METHOD_CLIENT_SECRET_POST = "client_secret_post";
    public static final String METHOD_PRIVATE_KEY_JWT = "private_key_jwt";

    public static final String SCOPE = "scope";
    public static final String EXPIRES_IN = "expires_in";
    public static final String TOKEN_TYPE = "token_type";

    public static final String ID_TOKEN = "id_token";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    public static final String[] TOKEN_TYPES = new String[] { ID_TOKEN, ACCESS_TOKEN, REFRESH_TOKEN };

}
