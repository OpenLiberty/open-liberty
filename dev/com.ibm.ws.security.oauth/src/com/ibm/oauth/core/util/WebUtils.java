/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  Class for common functions used when handling web requests
 */
public class WebUtils {

    private static TraceComponent tc = Tr.register(WebUtils.class);

    /*
     * Note: This elaborate comment moved from OAuth20ComponentImpl
     * 
     * This regular expression is designed to match the Authorization header for
     * the bearer token spec defined in section 2.1 of:
     * 
     * http://tools.ietf.org/html/draft-ietf-oauth-v2-bearer
     * 
     * In plain English, this string matches (case insensitive) a string
     * beginning with "Bearer" without quotes, followed by compulsory
     * whitespace, followed by one or more characters which must be an
     * alpha-numeric or one of _"!#$%&'()+-./:<=>?@[]^`{|}~\,;
     * 
     * Within the regular expression, \w is used to represent all word
     * characters (all alpha-numerics plus the underscore). Of the other
     * characters, some are quoted with a backslash because they are
     * metacharacters according to Java regular expression syntax - see
     * http://download.oracle.com/javase/tutorial/essential/regex/literals.html
     * 
     * Additionally a Group Capture is done on the characters following the
     * whitespace, and this group (1) represents the bearer token. The regular
     * expression also matches the beginning and end of the search string, so
     * only one call to the matcher's "find" method is needed.
     */
    private static final String BEARER_AZN_HEADER_PATTERN = "(?i)^bearer\\s+([\\w\"!#\\$%&'\\(\\)\\*\\+\\-\\./:<=>\\?@\\[\\]\\^`\\{\\|\\}~\\\\,;]+)$";
    static Pattern bearerHeaderPattern = Pattern.compile(BEARER_AZN_HEADER_PATTERN);

    /**
     * Extract the bearer token from the HTTP authorization header. 
     * 
     * @param authzHeader The authorization header string
     * @return token The bearer token or null
     */
    public static String getBearerTokenFromAuthzHeader(String authzHeader) {
        String token = null;
        if (authzHeader != null && authzHeader.length() > 0) {
            Matcher matcher = bearerHeaderPattern.matcher(authzHeader.trim());
            if (matcher.find()) {
                token = matcher.group(1);
            }
        }
        return token;
    }
}
