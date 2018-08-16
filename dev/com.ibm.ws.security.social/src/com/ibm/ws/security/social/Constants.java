/*
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2016, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social;

import com.ibm.ws.security.SecurityService;

public class Constants {

    public final static String UTF8 = "UTF-8";

    public static final String ATTRIBUTE_SOCIALMEDIA_REQUEST = "SocialLoginRequest";
    public static final String ATTRIBUTE_TAI_REQUEST = "SocialTaiRequest";

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
