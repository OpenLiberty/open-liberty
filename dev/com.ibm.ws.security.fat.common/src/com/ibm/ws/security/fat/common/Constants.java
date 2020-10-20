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
package com.ibm.ws.security.fat.common;

public class Constants {

    public static final String STRING_CONTAINS = "contains";
    public static final String STRING_DOES_NOT_CONTAIN = "does not contain";
    public static final String STRING_MATCHES = "matches";
    public static final String STRING_DOES_NOT_MATCH = "does not match";
    public static final String STRING_EQUALS = "equals";
    public static final String STRING_NULL = "null";
    public static final String STRING_NOT_NULL = "not null";
    public static final String TIME_TYPE = "time type";

    /**
     * This interface allows us to create a simple check type inheritance structure to divide up different categories of check
     * types (e.g. string checks, object checks, JSON-type checks. etc.). New check type categories can be defined as a new enum
     * that extends this interface and defines whatever category-specific check types that make sense.
     */
    public interface CheckType {
    }

    public static enum StringCheckType implements CheckType {
        NULL, NOT_NULL, EQUALS, CONTAINS, DOES_NOT_CONTAIN, CONTAINS_REGEX, DOES_NOT_CONTAIN_REGEX
    };

    public static enum ObjectCheckType implements CheckType {
        EQUALS
    };

    public static enum JsonCheckType implements CheckType {
        KEY_EXISTS, KEY_DOES_NOT_EXIST, VALUE_TYPE
    };

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
    public static final String TOKEN_TYPE_TOKEN = "Token";
    public static final String TOKEN_TYPE_MISC = "misc";

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
    public static final String APP_TESTMARKER_PATH = "testMarker";

    public static final String COMMON_CONFIG_DIR = "configs";

    public static final String HELLOWORLD_APP = "helloworld";

    /* ***************** Http methods ******************* */
    public static final String GETMETHOD = "GET";
    public static final String POSTMETHOD = "POST";
    public static final String DELETEMETHOD = "DELETE";
    public static final String PUTMETHOD = "PUT";
    public static final String HEADMETHOD = "HEAD";

    public static final String AUTHORIZATION = "Authorization";

    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    public static final String UNAUTHORIZED_EXCEPTION = "401 Unauthorized";
    public static final String HTTP_UNAUTHORIZED_EXCEPTION = "HTTP " + UNAUTHORIZED_EXCEPTION;

    /********************** Page Values *********************/
    public static final String FORM_LOGIN_HEADING = "Form Login Page";
    public static final String FORM_LOGIN_TITLE = "login.jsp";

    /********************** Server Info *********************/
    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME = "fat.server.hostname";
    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTIP = "fat.server.hostip";

    public static final String BVT_SERVER_1_PORT_NAME_ROOT = "security_1_HTTP_default";
    public static final String BVT_SERVER_2_PORT_NAME_ROOT = "security_2_HTTP_default";
    public static final String BVT_SERVER_3_PORT_NAME_ROOT = "security_3_HTTP_default";
    public static final String BVT_SERVER_4_PORT_NAME_ROOT = "security_4_HTTP_default";

}
