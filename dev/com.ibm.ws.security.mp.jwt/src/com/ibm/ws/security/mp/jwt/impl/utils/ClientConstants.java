/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.impl.utils;

/**
 *
 */
public class ClientConstants {

    public final static String TOKEN = "token";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String JWT_TOKEN = "jwt";
    public static final String MP_JWT_TOKEN = "mpjwtPrincipal";
    public static final String ISSUED_JWT_TOKEN = "issuedJwt";
    public static final String EXPIRES_IN = "expires_in";

    public final static String USER_ID = "user_id";
    public final static String USER_NAME = "user_name";
    public final static String EMAIL = "email";

    public static final String CHARSET = "UTF-8";

    public final static String AUTHORIZATION = "Authorization";
    public final static String BEARER = "bearer ";
    public final static String METHOD_client_secret_basic = "client_secret_basic";
    public final static String METHOD_client_secret_post = "client_secret_post";
    public final static String LOGIN_HINT = "mpjwt_login_hint";

}
