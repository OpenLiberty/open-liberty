/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.cookies.CookieUtils;
import com.ibm.ws.http.channel.internal.cookies.SameSiteCookieUtils;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * A utility class that encodes {@link HttpCookie} instances into header strings, 
 * applying configuration-based rules for SameSite and partitioned cookie attributes.
 *
 * If SameSite configuration is enabled, this class determines the correct
 * SameSite attribute for the cookie based on explicit mappings, wildcard
 * settings, or pattern-based matches. For cookies that must use SameSite=None,
 * it ensures they are secure and sets a partitioned attribute if required.
 */
public final class CookieEncoder {

    private CookieEncoder() {}

    private static final String EXPIRES_ATTRIBUTE = "expires";
    private static final String SAMESITE_ATTRIBUTE = "samesite";
    private static final String PARTITIONED_ATTRIBUTE = "partitioned";
    private static final SimpleDateFormat RFC1123_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    private static final int MAX_MAX_AGE_SECONDS = Integer.MAX_VALUE; // 2,147,483,647 seconds


    /**
     * Encodes a list of cookies into one or more final header lines. The
     * given {@code header} (expected to be a variation of "Cookie" or 
     * "Set-Cookie" header) determines teh output format by leveraging the use of 
     * {@link CookieUtils}.
     * 
     * This encode method will:
     *      Invoke policy logic for each cookie (SameSite, Partinioning, etc.)
     *      Handle duplicate cookies based on the {@link HttpChannelConfig}
     * @param cookies   the list of cookies to encode
     * @param header    which cookie header type is being written 
     * @param config    the {@code HttpChannelConfig} options
     * @param userAgent the user-agent string for checking SameSite compatibility
     * @return  a list of fully encoded header lines, or empty if {@code cookies} is
     * null or empty
     */
    public static List<String> encodeAllCookies(final List<HttpCookie> cookies, 
                                                final HeaderKeys header, 
                                                final HttpChannelConfig config,
                                                final String userAgent){
        if(cookies == null || cookies.isEmpty()){
            return Collections.emptyList();
        }
        boolean filterDuplicates = config.doNotAllowDuplicateSetCookies() && 
            (header == HttpHeaderKeys.HDR_SET_COOKIE || header == HttpHeaderKeys.HDR_SET_COOKIE2);

        LinkedHashMap<String, String> duplicatesMap = new LinkedHashMap<>();
        List<String> results = new ArrayList<>();

        for(HttpCookie cookie: cookies){
            String encoded = encodeCookie(cookie, header, config, userAgent);
            if(encoded == null || encoded.isEmpty()){
                continue;
            }
            if(filterDuplicates){
                duplicatesMap.put(cookie.getName(), encoded);
            }else{
                results.add(encoded);
            }
        }
        if(filterDuplicates){
            results.addAll(duplicatesMap.values());
        }
        return results;

    }


    /**
     * Encodes a single {@link HttpCookie} into a single header line. The
     * {@code header} parameter can by any supported cookie key:
     * See {@link HttpHeaderKeys}
     * HDR_COOKIE represents "Cookie" request headers
     * HDR_SET_COOKIE represents "Set-Cookie" response headers
     * HDR_COOKIE2, HDR_SET_COOKIE2 (legacy RFC 2965 support)
     * 
     * This method upgrades the cookie version if SameSite config is being used
     * and the cookie was set to Version=0. It leverages {@link CookieUtils} to
     * assemble the normalized header line.
     * 
     * @param cookie    the cookie to encode
     * @param header    which cookie header type is being written
     * @param config    the {@code HttpChannelConfig} options
     * @param userAgent the user-agent string for checking SameSite compatibility
     * @return a serialized header line, or null if invalid
     */
    public static String encodeCookie(final HttpCookie cookie, 
                                final HeaderKeys header, 
                                final HttpChannelConfig config, 
                                final String userAgent) {

        if(cookie == null || header == null){
            return null;
        }

        applyCookiePolicy(cookie, config, userAgent);
        ensureCookieVersion(cookie, config);
        

        return CookieUtils.toString(cookie, header, 
                                    config.isv0CookieDateRFC1123compat(), 
                                    config.shouldSkipCookiePathQuotes());
    }

    /**
     * Applies the main cookie policy for SameSite and Partitioned attributes.
     * @param cookie
     * @param config
     * @param userAgent
     */
    private static void applyCookiePolicy(final HttpCookie cookie, 
                                          final HttpChannelConfig config, 
                                          final String userAgent){
        if(cookie==null) return;

        applySameSiteAttribute(cookie, config, userAgent);
        applyPartitionedAttribute(cookie, config);    
    }

    /**
     * Ensures that the cookie version is valid.
     */
    private static void ensureCookieVersion(HttpCookie cookie, HttpChannelConfig config){
        if(cookie.getVersion() < 0){
            cookie.setVersion(0);
        }
        // Future enhancement: if using SameSite config and cookie is Version 0, consider upgrading to Version 1.
        //if(config.useSameSiteConfig() && cookie.getVersion() == 0){
        //    cookie.setVersion(1);
        //}
    }

    /**
     * If the cookie does not have a SameSite attribute, the {@link HttpChannelConfig} is used
     * to determine if a value should be injected. If injected, the secure attribute will be
     * verified if the SameSite attribute needs it. 
     * 
     * If the cookie already had a SameSite value, it will remain as-is.
     */
    private static void applySameSiteAttribute(final HttpCookie cookie, 
                                                  final HttpChannelConfig config, 
                                                  final String userAgent){

        String originalSameSite = cookie.getAttribute(SAMESITE_ATTRIBUTE);
        boolean serverInjectedSameSite = false;
        
        if(originalSameSite == null){
            serverInjectedSameSite = applySameSiteConfiguration(cookie, config);
            originalSameSite = cookie.getAttribute(SAMESITE_ATTRIBUTE);
        }
        if(originalSameSite == null || originalSameSite.isEmpty()){
            return;
        }

        SameSite sameSite;
        try {
            if(serverInjectedSameSite){
                sameSite = SameSite.from(originalSameSite);
                if(sameSite.requiresSecure() && !cookie.isSecure()){
                    cookie.setSecure(true);
                }
            cookie.setAttribute(SAMESITE_ATTRIBUTE, sameSite.toString());           
            }  
        }catch(IllegalArgumentException e){
            cookie.setAttribute(SAMESITE_ATTRIBUTE, null);
        }finally{
            if (originalSameSite.equalsIgnoreCase(SameSite.NONE.name()) && userAgent != null) {
                boolean isIncompatible = SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent);
                if (isIncompatible) {
                    cookie.setAttribute(SAMESITE_ATTRIBUTE, null);
                    cookie.setAttribute(PARTITIONED_ATTRIBUTE, null);
                }
            } 
        }
    }

    private static boolean applySameSiteConfiguration(final HttpCookie cookie, final HttpChannelConfig config){
        if(!config.useSameSiteConfig()){
            return false;
        }
        String name = cookie.getName();
        String defaultSameSite = config.getSameSiteCookies().get(name);
        if(defaultSameSite == null && config.onlySameSiteStar()){
            defaultSameSite = config.getSameSiteCookies().get(HttpConfigConstants.WILDCARD_CHAR);
        }
        if(defaultSameSite == null){
            defaultSameSite = matchSameSitePattern(name, config);
        }
        if(defaultSameSite != null && !defaultSameSite.isEmpty()){
            cookie.setAttribute(SAMESITE_ATTRIBUTE, defaultSameSite);
        }
        return true;
    }

    private static String matchSameSitePattern(final String name, final HttpChannelConfig config){
        for (Pattern pattern: config.getSameSitePatterns().keySet()){
            Matcher matcher = pattern.matcher(name);
            if(matcher.matches()){
                return config.getSameSitePatterns().get(pattern);
            }
        }
        return null;
    }

    /**
     * Apply the Partitioned attribute if the cookie has SameSite  set to 'None'
     * and the Partitioned attribute is not currently set.
     */
    private static void applyPartitionedAttribute(final HttpCookie cookie, final HttpChannelConfig config){
        if(config.getPartitioned()){
            String sameSiteValue = cookie.getAttribute(SAMESITE_ATTRIBUTE);
            if (sameSiteValue != null && sameSiteValue.equalsIgnoreCase(SameSite.NONE.name())) {
                String partitioned = cookie.getAttribute(PARTITIONED_ATTRIBUTE);
                if (partitioned == null) {
                    cookie.setAttribute(PARTITIONED_ATTRIBUTE, "");
                }
            }
        }
        enforcePartitionedPolicy(cookie);
    }

    /**
     * Final pass to disable the Partitioned attribute if SameSite is 
     * not 'None'.
     */
    private static void enforcePartitionedPolicy(final HttpCookie cookie) {
        String sameSiteValue = cookie.getAttribute(SAMESITE_ATTRIBUTE);
        boolean isNone = (sameSiteValue != null && sameSiteValue.equalsIgnoreCase("none"));

        String partitionedValue = cookie.getAttribute(PARTITIONED_ATTRIBUTE);
        if (!isNone && partitionedValue != null && !partitionedValue.equalsIgnoreCase("false")) {
            cookie.setAttribute(PARTITIONED_ATTRIBUTE, "false");
        }
    }

    /**
     * Utility method, would to calculate Max Age attribute based on the 
     * Expires attribute. Unused at the moment as it would change default
     * behavior. 
     * @param cookie
     * 
     * NOTE: This method is for future enhancement, seek approval to enable use 
     * of this.
     */
    @SuppressWarnings("unused")
    private static void handleExpiresToMaxAge(final HttpCookie cookie){
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
                    cookie.setAttribute(EXPIRES_ATTRIBUTE, null);
                }else {
                    cookie.setMaxAge(0);
                }
            }
    }

    /**
     * Parses the expires value as an RFC1123 date. Returns {@code null}
     * if parse fails. 
     * 
     * @param expires the raw expires String
     * @return the parsed Date or null
     */
    private static Date parseExpires(final String expires){
        try {
            return RFC1123_DATE_FORMAT.parse(expires);
        } catch(ParseException e){
            return null;
        }
    }
}
