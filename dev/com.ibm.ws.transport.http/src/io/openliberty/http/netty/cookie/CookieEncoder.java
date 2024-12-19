/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.cookie;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.cookies.CookieUtils;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.HttpCookie;

/**
 * A utility class that encodes {@link HttpCookie} instances into header strings, 
 * applying configuration-based rules for SameSite and partitioned cookie attributes.
 *
 * If SameSite configuration is enabled, this class determines the correct
 * SameSite attribute for the cookie based on explicit mappings, wildcard
 * settings, or pattern-based matches. For cookies that must use SameSite=None,
 * it ensures they are secure and sets a partitioned attribute if required.
 */
public class CookieEncoder {

    private CookieEncoder() {}

    private static final String SAMESITE_ATTRIBUTE = "samesite";
    private static final String PARTITIONED_ATTRIBUTE = "partitioned";

    /**
     * Encodes the given cookie into a string suitable for a Set-Cookie header,
     * applying SameSite and partitioned logic if configured.
     * 
     * @param cookie the cookie to encode 
     * @param header the associated header keys
     * @param config the channel configuration
     * @return a Set-Cookie header value
     */
    public static String encode(HttpCookie cookie, HeaderKeys header, HttpChannelConfig config) {
        if (config.useSameSiteConfig()) {

            String currentSameSite = cookie.getAttribute("samesite");

            if(currentSameSite == null){
                String cookieName = cookie.getName();
                String sameSiteValue = config.getSameSiteCookies().get(cookieName);

                if(sameSiteValue == null && config.onlySameSiteStar()){
                    sameSiteValue = config.getSameSiteCookies().get(HttpConfigConstants.WILDCARD_CHAR);
                }

                if(sameSiteValue == null){
                    for(Pattern pattern: config.getSameSitePatterns().keySet()){
                        Matcher matcher = pattern.matcher(cookieName);
                        if(matcher.matches()){
                            sameSiteValue = config.getSameSitePatterns().get(pattern);
                            break;
                        }
                    }
                }

                if(sameSiteValue != null){
                    cookie.setAttribute(SAMESITE_ATTRIBUTE, sameSiteValue);

                    if(HttpConfigConstants.SameSite.NONE.getName().equalsIgnoreCase(sameSiteValue)){
                        if(!cookie.isSecure()){
                            cookie.setSecure(true);
                        }
                        if(config.getPartitioned() == Boolean.TRUE && cookie.getAttribute(PARTITIONED_ATTRIBUTE) == null){
                            cookie.setAttribute(PARTITIONED_ATTRIBUTE, "");
                        }
                    }
                }
            } else {
                // If SameSite already set, check if it's None and ensure partitioned if needed
                if (HttpConfigConstants.SameSite.NONE.getName().equalsIgnoreCase(currentSameSite)) {
                    if (config.getPartitioned() == Boolean.TRUE && cookie.getAttribute(PARTITIONED_ATTRIBUTE) == null) {
                        cookie.setAttribute(PARTITIONED_ATTRIBUTE, "");
                    }
                }
            }
        }
        
        return CookieUtils.toString(cookie, header, config.isv0CookieDateRFC1123compat(), config.shouldSkipCookiePathQuotes());
    }
}
