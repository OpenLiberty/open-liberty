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
package com.ibm.ws.security.fat.common;

import java.util.List;

public class Constants {

    public static final boolean DEBUG = true;
    public static final boolean FORCE_SERVER_RESTART = false;
    public static final boolean DO_NO_RESTART_SERVER = false;

    public static final String DEFAULT_HTTP_PORT = "8010";

    public static final String JKS_LOCATION = "./securitykeys/sslClientDefault.jks";

    public static final boolean SKIP_CHECK_FOR_SECURITY_STARTED = false;

    public static final boolean JUNIT_REPORTING = true;
    public static final boolean NO_JUNIT_REPORTING = false;

    public static final String STRING_CONTAINS = "contains";
    public static final String STRING_DOES_NOT_CONTAIN = "does not contain";
    public static final String STRING_MATCHES = "matches";
    public static final String STRING_DOES_NOT_MATCH = "does not match";
    public static final String STRING_EQUALS = "equals";
    public static final String STRING_NULL = "null";
    public static final String STRING_NOT_NULL = "not null";
    public static final String TIME_TYPE = "time type";
    public static final String LIST_MATCHES = "listmatches";

    public static final List<String> NO_EXTRA_APPS = null;
    public static final List<String> NO_EXTRA_MSGS = null;

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

    // Status code definitions
    public static final int OK_STATUS = 200;
    public static final int CREATED_STATUS = 201;
    public static final int REDIRECT_STATUS = 302;
    public static final int BAD_REQUEST_STATUS = 400;
    public static final int UNAUTHORIZED_STATUS = 401;
    public static final int FORBIDDEN_STATUS = 403;
    public static final int NOT_FOUND_STATUS = 404;
    public static final int NOT_ALLOWED_STATUS = 405;
    public static final int INTERNAL_SERVER_ERROR_STATUS = 500;
    public static final int BAD_GATEWAY = 502;

    /* ***************** Http methods ******************* */
    public static final String GETMETHOD = "GET";
    public static final String POSTMETHOD = "POST";
    public static final String DELETEMETHOD = "DELETE";
    public static final String PUTMETHOD = "PUT";
    public static final String HEADMETHOD = "HEAD";
    public static final String OPTIONSMETHOD = "OPTIONS";
    public static final String PATCHMETHOD = "PATCH";
    public static final String TRACEMETHOD = "TRACE";

    public static final String AUTHORIZATION = "Authorization";

    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    public static final String UNAUTHORIZED_EXCEPTION = "401 Unauthorized";
    public static final String HTTP_UNAUTHORIZED_EXCEPTION = "HTTP " + UNAUTHORIZED_EXCEPTION;
    public static final String CONTEXT_ROOT_NOT_FOUND = "<title [^>]+\"CONTEXT_ROOT_NOT_FOUND\".+</title>";

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

    /* Signature contants */
    public static final String SIGALG_NONE = "none";
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
    public static final String[] ALL_TEST_SIGALGS = { SIGALG_HS256, SIGALG_HS384, SIGALG_HS512,
            SIGALG_RS256, SIGALG_RS384, SIGALG_RS512,
            SIGALG_ES256, SIGALG_ES384, SIGALG_ES512 };
    public static final String[] ALL_TEST_HSSIGALGS = { SIGALG_HS256, SIGALG_HS384, SIGALG_HS512 };
    public static final String[] ALL_TEST_RSSIGALGS = { SIGALG_RS256, SIGALG_RS384, SIGALG_RS512 };
    public static final String[] ALL_TEST_ESSIGALGS = { SIGALG_ES256, SIGALG_ES384, SIGALG_ES512 };

    public static final String SUCCESSFUL_LOGOUT_MSG = "You have successfully logged out";

    public static final String SNOOP = "snoop";
    public static final String SNOOPING = "snooping";
    public static final String SNIFFING = "sniffing";
    public static final String SNORKING = "snorking";

    public static final String OPENID_APP = "formlogin";
    public static final String DEFAULT_SERVLET = "SimpleServlet";

    public static final String LTPA_TOKEN = "LtpaToken2";
    public static final String JWT_SSO_COOKIE_NAME = "JWT";

    public static final String LOGIN_PROMPT = "Enter your username and password to login";
    public static final String LOGIN_ERROR = "Error: username and password doesn't match";
    public static final String LOGIN_TITLE = "Login";

    public static final String AUTHORIZATION_ERROR = "Error 403: AuthorizationFailed";
    public static final String HTTP_ERROR_FORBIDDEN = "HTTP Error 403 - Forbidden";
    public static final String HTTP_UNAUTHORIZED = "HTTP/1.1 401 Unauthorized";
    public static final String HTTP_ERROR_UNAUTHORIZED = "HTTP Error 401";
    public static final String HTTP_ERROR_MESSAGE = "HTTP Error Message";
    public static final String FORBIDDEN = "Forbidden";
    public static final String NOT_FOUND_MSG = "Not Found";
    public static final String NOT_FOUND_ERROR = "Error 404:";
    public static final String OK_MESSAGE = "OK";
    public static final String BAD_REQUEST = "Bad Request";

    /* ********************** MESSAGES ************************ */
    public static final String MSG_INVALID_PWD = "CWWKS1100A";
    public static final String MSG_FILE_NOT_FOUND = "SRVE0190E";
    public static final String MSG_APP_READY = "CWWKT0016I";
    public static final String CWWKZ0003I_APP_UPDATED = "CWWKZ0003I";
    public static final String CWWKZ0009I_APP_STOPPED_SUCCESSFULLY = "CWWKZ0009I";

    /* ********************** HTTP RESPONSE HEADER VALUES ************************ */
    public static final String RESPONSE_HEADER_WWWAUTHENTICATE = "WWW-Authenticate: ";
    public static final String RESPONSE_HEADER_CONTENT_JSON = "application/json";
    public static final String RESPONSE_CACHE_CONTROL_NO_STORE = "no-store";
    public static final String RESPONSE_PRAGMA_NO_CACHE = "no-cache";
    public static final String RESPONSE_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String RESPONSE_HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String RESPONSE_HEADER_PRAGMA = "Pragma";

    /* ****************** test apps ************************* */
    public static final String TESTMARKER_START_APP = "testmarker";

    /* ****************** Misc ****************************** */
    public static final String TEST_CASE = "test_case";
}
