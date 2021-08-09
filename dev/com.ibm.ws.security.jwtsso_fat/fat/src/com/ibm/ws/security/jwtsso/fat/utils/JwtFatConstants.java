/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat.utils;

import com.ibm.ws.security.fat.common.jwt.JwtConstants;

public class JwtFatConstants extends JwtConstants {

    public static final String JWT_COOKIE_NAME = "JWT";
    public static final String JWT_COOKIE_NAME_MSG = "JWT cookie name: ";
    public static final String JWT_PRINCIPAL_MSG = "JWT principal: ";
    public static final String EXPECTED_COOKIE_NAME = "cookie: " + JWT_COOKIE_NAME;
    public static final String EXPECTED_COOKIE_2_NAME = "cookie: " + JWT_COOKIE_NAME + "02";

    public static final String LTPA_COOKIE_NAME = "LtpaToken2";

    public static final String JWT_REGEX = BASE_64_REGEX + "+\\." + BASE_64_REGEX + "+\\." + BASE_64_REGEX + "+";

    public static final String DEFAULT_ISS_CONTEXT = "/jwt/defaultJwtSso";
    public static final String DEFAULT_ISS_REGEX = "https?://" + "[^/]+" + JwtFatConstants.DEFAULT_ISS_CONTEXT;

    public static final String FORMLOGIN_CONTEXT_ROOT = "/formlogin";
    public static final String JWT_BUILDER_CONTEXT_ROOT = "/jwtbuilder";

    public static final String SIMPLE_SERVLET_PATH = FORMLOGIN_CONTEXT_ROOT + "/SimpleServlet";

    public static final String USER_1 = "user1";
    public static final String USER_1_PWD = "user1pwd";

    public static final boolean SECURE = true;
    public static final boolean NOT_SECURE = false;
    public static final boolean HTTPONLY = true;
    public static final boolean NOT_HTTPONLY = false;

    public static final String RAW_TOKEN_KEY = "raw_token";

    public static final String MPJWT_VERSION_11 = "mpJwt11";
    public static final String MPJWT_VERSION_12 = "mpJwt12";
    public static final String MPJWT_VERSION_20 = "mpJwt20";
    public static final String NO_MPJWT = "noMpJwt";
}
