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
package com.ibm.ws.security.fat.common;

public class Constants {

    public static final String STRING_CONTAINS = "contains";
    public static final String STRING_DOES_NOT_CONTAIN = "does not contain";
    public static final String STRING_MATCHES = "matches";
    public static final String STRING_DOES_NOT_MATCH = "does not match";
    public static final String STRING_EQUALS = "equals";
    public static final String STRING_NULL = "null";
    public static final String STRING_NOT_NULL = "not null";

    public static final String CONSOLE_LOG = "console.log";
    public static final String MESSAGES_LOG = "messages.log";
    public static final String TRACE_LOG = "trace.log";

    public static final String RESPONSE_TITLE = "title";
    public static final String RESPONSE_FULL = "response";
    public static final String RESPONSE_MESSAGE = "message";
    public static final String RESPONSE_STATUS = "status";
    public static final String RESPONSE_HEADER = "header";
    public static final String RESPONSE_URL = "url";
    public static final String JSON_OBJECT = "jsonObject";
    public static final String EXCEPTION_MESSAGE = "exceptionMsg";

    public static final String HEADER_SET_COOKIE = "Set-Cookie";
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    public static final String J_SECURITY_CHECK = "j_security_check";
    public static final String J_USERNAME = "j_username";
    public static final String J_PASSWORD = "j_password";

    public static final String LTPA_COOKIE_NAME = "LtpaToken2";

    public static final String BASIC_REALM = "BasicRealm";
    public static final String TESTUSER = "testuser";
    public static final String TESTUSERPWD = "testuserpwd";

    public static final String BASE_64_REGEX = "[a-zA-Z0-9_=+/-]";

    public static final String APP_FORMLOGIN = "formlogin";
    public static final String APP_TESTMARKER = "testmarker";

    public static final String COMMON_CONFIG_DIR = "configs";

    /* ***************** Http methods ******************* */
    public static final String GETMETHOD = "GET";
    public static final String POSTMETHOD = "POST";
    public static final String DELETEMETHOD = "DELETE";
    public static final String PUTMETHOD = "PUT";
    public static final String HEADMETHOD = "HEAD";

    public static final String UNAUTHORIZED_EXCEPTION = "401 Unauthorized";
    public static final String HTTP_UNAUTHORIZED_EXCEPTION = "HTTP " + UNAUTHORIZED_EXCEPTION;

    /********************** Page Values *********************/
    public static final String FORM_LOGIN_HEADING = "Form Login Page";
    public static final String FORM_LOGIN_TITLE = "login.jsp";

}