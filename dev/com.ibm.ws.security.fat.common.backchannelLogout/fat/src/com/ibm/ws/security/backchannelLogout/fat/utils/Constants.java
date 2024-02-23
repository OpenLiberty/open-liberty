/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.backchannelLogout.fat.utils;

/**
 * This class supplies support methods to the back channel logout tests.
 */

public class Constants extends com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants {

    protected static Class<?> thisClass = Constants.class;
    public static final String sharedHSSharedKey = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger";
    public static final String logoutEventKey = "http://schemas.openid.net/event/backchannel-logout";
    public static final String defaultLogoutPage = "/oidc/end_session_logout.html";
    public static final String samlLogoutPage = "/idp/profile/SAML2/POST/SLO";
    public static final String postLogoutJSessionIdApp = "/backchannelLogoutTestApp/backChannelLogoutJSessionId";
    public static final String simpleLogoutApp = "simpleLogoutTestApp";
    public static final String backchannelLogoutApp = "backchannelLogoutTestApp";
    public static final String opJSessionIdName = "OPJSESSIONID";
    public static final String clientJSessionIdName = "clientJSESSIONID";
    public static final String client2JSessionIdName = "client2JSESSIONID";
    public static final String ibm_security_logout_default_page = "Default Logout Exit Page";

    public static final boolean sidIsRequired = true;
    public static final boolean sidIsNotRequired = false;
    public static final boolean doNotNeedToLogin = true;
    public static final boolean needToLogin = false;
    public static final boolean successfulOPLogout = true;
    public static final boolean unsuccessfulOPLogout = false;
    public static final boolean successfulRPLogout = true;
    public static final boolean unsuccessfulRPLogout = false;
    public static final boolean refreshTokenValid = true;
    public static final boolean refreshTokenInvalid = false;

    public static String opCookieName = "testOPCookie";
    public static String opCookieNameWithSaml = "testOtherOPCookie";
    public static String clientCookieName = "clientCookie";
    public static String client2CookieName = "client2Cookie";
    public static String genericServerCookieName = "testRSCookie";
    public static String spCookieName = "testSPCookie";
    public static String idpCookieName = "tbd";

    public static final boolean usesValidBCLEndpoint = true;
    public static final boolean usesFakeBCLEndpoint = false;
    public static final boolean usesInvalidBCLEndpoint = false;

    public static final String SOCIAL = "SOCIAL";

}
