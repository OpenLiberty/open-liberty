/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.filter.fat;

import java.util.List;
import java.util.Map;

public class AuthFilterConstants {

    public final static String USER0 = "user0";
    public final static String USER0_PWD = "user0pwd";

    public final static String PROP_TEST_SYSTEM_HOST_NAME = "security.auth.filter.test.system.host.name";

    // Servlet and request constants
    public final static String HEADER_AUTHORIZATION = "Authorization";
    public final static String HEADER_USER_AGENT = "User-Agent";
    public final static String HEADER_HOST = "Host";
    public final static String HEADER_REMOTE_ADDR = "";
    public final static String FIREFOX = "Firefox";
    public final static String IE = "IE";
    public final static String SIMPLE_SERVLET_NAME = "SimpleServlet";
    public final static String SIMPLE_SERVLET = "/" + SIMPLE_SERVLET_NAME;
    public final static String OWNER_STRING = "Owner:\t\t";

    //Additional Supported Header Values for AuthFilterElements
    public final static String HEADER_EMAIL = "email";
    public final static String HEADER_NAME_NO_VALUE = "nameNoValue";
    public final static String HEADER_NAME_NO_VALUE_INVALID = "nameNoValueInvalid";

    public final static String COOKIE = "Cookie";
    public final static String SSO_COOKIE_NAME = "LtpaToken2";
    // Constants to ease readability
    public final static List<String> NO_APPS = null;
    public final static List<String> NO_MSGS = null;
    public final static Map<String, String> NO_PROPS = null;

    public final static boolean START_SERVER = true;
    public final static boolean DONT_START_SERVER = false;
    public final static boolean RESTART_SERVER = true;
    public final static boolean DONT_RESTART_SERVER = false;
    public final static boolean MESSAGE_NOT_EXPECTED = false;
    public final static boolean IGNORE_ERROR_CONTENT = true;
    public final static boolean DONT_IGNORE_ERROR_CONTENT = false;
    public final static boolean DONT_HANDLE_SSO_COOKIE = false;
    public final static boolean IS_EMPLOYEE = true;
    public final static boolean IS_NOT_EMPLOYEE = false;
    public final static boolean IS_MANAGER = true;
    public final static boolean IS_NOT_MANAGER = false;

    // Other constants
    protected static final int DEFAULT_LOG_SEARCH_TIMEOUT = 120 * 1000;
    public static final int MESSAGE_NOT_EXPECTED_LOG_SEARCH_TIMEOUT = 15 * 1000;

    public final static String LOCALHOST_IP_ADDR_NOT_DEFAULT_VALUE = "localhost ip address is not a default vaule. We will not run the test";

}