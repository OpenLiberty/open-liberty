/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.oidc.common;

import org.junit.Ignore;

import com.ibm.ws.security.oauth.test.ClientRegistrationHelper;
import com.ibm.ws.security.test.common.CommonTestClass;

/**
 * Base test for 'client registration' related test cases to extends
 *
 */
@Ignore
// Ignore this class, since it's not actually a test
public abstract class AbstractOidcRegistrationBaseTest extends CommonTestClass {

    public static String _testName = "";

    public static final String FIELD_CLIENT_SECRET = "client_secret";
    public static final String FIELD_APPLICATION_TYPE = "application_type";
    public static final String FIELD_SUBJECT_TYPE = "subject_type";

    /**
     * Expected OidcBaseClient values
     */
    public static final String COMPONENT_ID = "OP";
    public static final String CLIENT_ID = "123456789";
    public static final String CLIENT_SECRET = "secret1234";
    public static final String CLIENT_NAME = "test client";

    public static final String REDIRECT_URI_1 = "http://www.redirect1.com";
    public static final String REDIRECT_URI_2 = "http://www.redirect2.com";

    public static final boolean IS_ENABLED = true;
    public static final long CLIENT_ID_ISSUED_AT = 12345L;
    public static final String REGISTRATION_CLIENT_URI = "https://localhost:8020/oidc/endpoint/OP/registration/" + CLIENT_ID;
    public static final long CLIENT_SECRET_EXPIRES_AT = 0;
    public static final String TOKEN_ENDPOINT_AUTH_METHOD = "none";
    public static final String SCOPE = "openid general profile";

    public static final String GRANT_TYPES_1 = "authorization_code";
    public static final String GRANT_TYPES_2 = "implicit";

    public static final String RESPONSE_TYPES_1 = "code";
    public static final String RESPONSE_TYPES_2 = "token";

    public static final String APPLICATION_TYPE = "web";
    public static final String SUBJECT_TYPE = "public";

    public static final String POST_LOGOUT_REDIRECT_URI_1 = "http://www.logout1.com";
    public static final String POST_LOGOUT_REDIRECT_URI_2 = "http://www.logout2.com";

    public static final String PREAUTHORIZED_SCOPE = "profile";
    public static final boolean INTROSPECT_TOKENS = true;

    public static final String TRUSTED_URI_PREFIX_1 = "http://www.trusted1.com/";
    public static final String TRUSTED_URI_PREFIX_2 = "http://www.trusted2.com/";

    public static final String FUNCTIONAL_USER_ID = "testuser";

    public static final String SALT = "HStFDlTCZ4rJQThbSddZG5q0Ovf2ozDlrJtT1AYiUhM=";
    public static final String ALG = "PBKDF2WithHmacSHA512";

    protected ClientRegistrationHelper clientRegistrationHelper = new ClientRegistrationHelper(false);

}
