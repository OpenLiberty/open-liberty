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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
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

    private static final String EXPIRES_ATTRIBUTE = "expires";
    private static final String SAMESITE_ATTRIBUTE = "samesite";
    private static final String PARTITIONED_ATTRIBUTE = "partitioned";
    private static final SimpleDateFormat RFC1123_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    private static final int MAX_MAX_AGE_SECONDS = Integer.MAX_VALUE; // 2,147,483,647 seconds


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
        if(cookie == null){
            throw new IllegalArgumentException("Cookie cannot be null");
        }
        if(header == null){
            throw new IllegalArgumentException("HeaderKeys cannot be null");
        }

        determineAndSetVersion(cookie, config);
        applyVersionAttributes(cookie, config);

        return CookieUtils.toString(cookie, header, config.isv0CookieDateRFC1123compat(), config.shouldSkipCookiePathQuotes());
    }

    private static void determineAndSetVersion(HttpCookie cookie, HttpChannelConfig config){
        if(config.useSameSiteConfig()){
            cookie.setVersion(1);
        } else {

            int version = 0;

            boolean hasSameSite = cookie.getAttribute(SAMESITE_ATTRIBUTE) != null;
            boolean hasMaxAge = cookie.getMaxAge() > -1;

            if(hasSameSite || hasMaxAge){
                version = 1;
            }
            cookie.setVersion(version);
        }
    }

    private static void applyVersionAttributes(HttpCookie cookie, HttpChannelConfig config){
        if(cookie.getVersion() == 1){
            String sameSite = cookie.getAttribute(SAMESITE_ATTRIBUTE);

            if(config.useSameSiteConfig()){
                if(sameSite == null){
                    String name = cookie.getName();
                    String configSameSiteValue = config.getSameSiteCookies().get(name);

                    if(configSameSiteValue == null && config.onlySameSiteStar()){
                        configSameSiteValue = config.getSameSiteCookies().get(HttpConfigConstants.WILDCARD_CHAR);
                    }

                    if(configSameSiteValue == null){
                        for(Pattern pattern: config.getSameSitePatterns().keySet()){
                            Matcher matcher = pattern.matcher(name);
                            if(matcher.matches()){
                                configSameSiteValue = config.getSameSitePatterns().get(pattern);
                                break;
                            }
                        }
                    }

                    if(configSameSiteValue != null){
                        sameSite = configSameSiteValue;
                        cookie.setAttribute(SAMESITE_ATTRIBUTE, configSameSiteValue);
                    }
                }
            }

            if("None".equalsIgnoreCase(sameSite)){
                if(!cookie.isSecure()){
                    cookie.setSecure(true);
                }
            }
            if(config.getPartitioned() && cookie.getAttribute(PARTITIONED_ATTRIBUTE) == null) {
                cookie.setAttribute(PARTITIONED_ATTRIBUTE, "");
            }

            handleExpiresToMaxAge(cookie);
        }
    }

    private static void handleExpiresToMaxAge(HttpCookie cookie){
            boolean hasExpires = cookie.getAttribute(EXPIRES_ATTRIBUTE) != null;
            boolean hasMaxAge = cookie.getMaxAge() > -1;

            if(hasExpires && !hasMaxAge){
                String expires = cookie.getAttribute(EXPIRES_ATTRIBUTE);
                Date expiresDate = parseExpires(expires);
                if(expiresDate != null){
                    long currentTime = System.currentTimeMillis();
                    long maxAgeSeconds = TimeUnit.MILLISECONDS.toSeconds(expiresDate.getTime() - currentTime);

                    //Prevent overflow since setMaxAge uses int
                    if(maxAgeSeconds > MAX_MAX_AGE_SECONDS){
                        maxAgeSeconds = MAX_MAX_AGE_SECONDS;
                    }else if(maxAgeSeconds < 0){
                        maxAgeSeconds = 0;
                    }
                    
                    cookie.setMaxAge((int)maxAgeSeconds);
                    //Should we remove attribute?
                }else {
                    cookie.setMaxAge(0);
                }
            }
    }

    private static Date parseExpires(String expires){
        try {
            return RFC1123_DATE_FORMAT.parse(expires);
        } catch(ParseException e){
            return null;
        }
    }
}
