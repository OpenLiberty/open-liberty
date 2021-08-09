/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;

/**
 *
 */
public class SpnegoOIDCConstants extends Constants {

    public static boolean USE_SPNEGO = true;
    public static boolean DONT_USE_SPNEGO = false;
    
    public static String SEND_BAD_TOKEN="THISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKEN";

    public static final String ADD_NEGIOTIATE_HEADER = "ADD_SPNEGO_TOKEN";

    public static final String[] GOOD_OIDC_POST_LOGIN_ACTIONS_CONSENT_SPNEGO = { POST_LOGIN_PAGE, ADD_NEGIOTIATE_HEADER, LOGIN_USER, GET_RP_CONSENT };

    public static final String[] GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT = { GET_LOGIN_PAGE, LOGIN_USER };
    
    public static final String[] BAD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT = {SEND_BAD_TOKEN };
    
    public static final String[] BASIC_TOKEN = { INVOKE_AUTH_ENDPOINT, INVOKE_TOKEN_ENDPOINT };

    public static final String SPNEGO_NEGOTIATE = "Negotiate ";

    public static final String FORMLOGIN_SERVLET = "ServletName: FormLoginServlet";

    public static final String GET_SPNEGO_LOGIN_PAGE_METHOD = "getLoginPage";
    
    public static final String NTLM_TOKEN="CWWKS4307E: <html><head><title>An NTLM Token was received.</title></head>";

}
