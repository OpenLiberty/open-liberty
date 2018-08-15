/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social.web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.TraceConstants;

/**
 *
 */
public class KnownSocialLoginUrl {
    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(KnownSocialLoginUrl.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    //public static final String SOCIAL_LOGIN_CONTEXT_PATH = Constants.SOCIAL_CTX_ROOT;// "/ibm/social-login";
    public static final String REGEX_COMPONENT_ID = "/(([\\w-]+)|" + Constants.WELL_KNOWN + ")/";
    public static final String PATH_KNOWN_SOCIAL_LOGIN_URL = "(redirect|configuration)";
    public static final String SLASH_PATH_KNWON_SOCIAL_LOGIN_URL = "/" + PATH_KNOWN_SOCIAL_LOGIN_URL;
    private static final Pattern PATTERN_KNOWN_SOCIAL_LOGIN_URL = Pattern.compile("^" + REGEX_COMPONENT_ID + PATH_KNOWN_SOCIAL_LOGIN_URL + "$");

    // path cannot be null
    public static Matcher matchKnownSocialMediaUrl(String path) {
        synchronized (PATTERN_KNOWN_SOCIAL_LOGIN_URL) {
            return PATTERN_KNOWN_SOCIAL_LOGIN_URL.matcher(path);
        }
    }

}
