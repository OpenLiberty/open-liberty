/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social;

import com.ibm.ws.security.SecurityService;

public class Constants {

    public final static String UTF8 = "UTF-8";

    public static final String ATTRIBUTE_SOCIALMEDIA_REQUEST = "SocialLoginRequest";
    public static final String ATTRIBUTE_TAI_REQUEST = "SocialTaiRequest";
    
    public static final String ATTRIBUTE_TAI_BEFORE_SSO_REQUEST = "SocialTaiBeforeSSORequest";

    public final static String CHARSSTR = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public final static char[] CHARS = CHARSSTR.toCharArray();

    public final static String KEY_SOCIALLOGIN_SERVICE = SocialLoginService.class.getName();
    public final static String KEY_SECURITY_SERVICE = SecurityService.class.getName();

    // public final static String SOCIAL_CTX_ROOT = "/ibm/api/social-login";
    public static final String DEFAULT_ERROR_MSG_JSP = "/errorMsg.jsp";
    public static final String WELL_KNOWN = ".well-known";

    public static final String client_secret_basic = "client_secret_basic";
    public static final String client_secret_post = "client_secret_post";

}
