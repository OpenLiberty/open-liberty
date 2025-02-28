/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants.SameSite;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.openliberty.http.netty.cookie.CookieDecoder;
import io.openliberty.http.netty.cookie.CookieEncoder;

/**
 * Tests the functionality of the {@link CookieEncoder} utility.
 * These tests ensure that cookies are properly encoded from "Set-Cookie" headers and {@link HttpCookie} 
 * objects. This considers appropriate header formats, respecting configuration settings related to SameSite, 
 * partitioning, and other cookie attributes.
 */
public class CookieEncoderTests{

    private HttpChannelConfig config;
    private HeaderKeys header;

    private static final String NAME = "testCookieName";
    private static final String VALUE = "testCookieValue";

    //Attributes
    private static final String MAX_AGE = "Max-Age";
    private static final String PARTITIONED = "Partitioned";
    private static final String SAMESITE = "SameSite";
    private static final String SECURE = "Secure";


    private static final String SAMESITE_NONE = "None";
    private static final String SAMESITE_LAX = "Lax";
    private static final String SAMESITE_STRICT = "Strict";
    private static final String INVALID_SAMESITE = "InvalidValue";
    private static final String EXPIRES_FUTURE = "Wed, 09 Jun 2025 10:18:14 GMT";
    private static final String EXPIRES_PAST = "Wed, 09 Jun 2020 10:18:14 GMT";
    private static final String EXPIRES_MALFORMED = "InvalidDate";

    private static final String UA = "DefaultAgent/1.0";


    /**
     * Sets up mock configuration and header objects before each test.
     */
    @Before
    public void setUp() {
        config = mock(HttpChannelConfig.class);

        when(config.useSameSiteConfig()).thenReturn(false);
        when(config.getSameSiteCookies()).thenReturn(Collections.emptyMap());
        when(config.onlySameSiteStar()).thenReturn(false);
        when(config.getSameSitePatterns()).thenReturn(Collections.emptyMap());
        when(config.getPartitioned()).thenReturn(false);
        when(config.isv0CookieDateRFC1123compat()).thenReturn(true);
        when(config.shouldSkipCookiePathQuotes()).thenReturn(false);
        when(config.doNotAllowDuplicateSetCookies()).thenReturn(false);

        header = HttpHeaderKeys.HDR_SET_COOKIE;
    }

    private void setupSameSiteConfig(boolean useSameSite, String sameSiteValue, boolean partitioned){
        when(config.useSameSiteConfig()).thenReturn(useSameSite);

        if (useSameSite) {
            Map<String, String> sameSiteCookies = new HashMap<>();
            if (sameSiteValue != null) {
                sameSiteCookies.put(NAME, sameSiteValue);
            }
            when(config.getSameSiteCookies()).thenReturn(sameSiteCookies);
            when(config.onlySameSiteStar()).thenReturn(false);
            when(config.getSameSitePatterns()).thenReturn(new HashMap<>());
            when(config.getPartitioned()).thenReturn(partitioned);
        } else {
            // Reset to default behaviors if not using SameSite config
            when(config.getSameSiteCookies()).thenReturn(Collections.emptyMap());
            when(config.onlySameSiteStar()).thenReturn(false);
            when(config.getSameSitePatterns()).thenReturn(Collections.emptyMap());
            when(config.getPartitioned()).thenReturn(false);
        }
    }

    // This would be for future enhacement, for not it is not supported behavior.
    //@Test 
    public void testEncodeVersion0WithMaxAgeShouldUpgrade(){
        HttpCookie cookie = new HttpCookie(NAME, VALUE);
        cookie.setVersion(0);
        cookie.setMaxAge(3600);
        cookie.setAttribute(SAMESITE, SAMESITE_NONE);
        cookie.setSecure(false);

        setupSameSiteConfig(true, SAMESITE_NONE, true);

        String encoded = CookieEncoder.encodeCookie(cookie, header, config, UA);

        assertThat(encoded, containsString("testCookieName=testCookieValue"));
        assertThat(encoded, containsString("Max-Age=3600"));
        assertThat(encoded, containsString("SameSite=None"));
        assertThat(encoded, containsString("Secure"));
        assertThat(encoded, containsString("Partitioned"));
    }

    /**
     * Tests that a cookie without an existing SameSite attribute is encoded using the
     * configuration SameSite value. This verifies that SameSite attributes are
     * properly applied when they are present in configuration mappings.
     */
    @Test
    public void testEncodeAddsSameSiteStrict() {
        HttpCookie cookie = new HttpCookie("testCookie", "value");

        when(config.useSameSiteConfig()).thenReturn(true);

        Map<String, String> sameSiteMap = new HashMap<>();
        sameSiteMap.put("testCookie", "Strict");

        setupSameSiteConfig(true, SAMESITE_STRICT, false);
        when(config.getSameSiteCookies()).thenReturn(sameSiteMap);
        

        String encoded = CookieEncoder.encodeCookie(cookie, HttpHeaderKeys.HDR_SET_COOKIE, config, UA).toLowerCase();
        assertThat("Encoded cookie should include samesite=strict", encoded, containsString("samesite=strict"));
    }

    /**
     * Tests that a cookie with a SameSite=None configuration is automatically made secure
     * and partitioned if the configuration requires it. This ensures that special rules for
     * SameSite=None are correctly enforced at encoding time.
     */
    @Test
    public void testEncodeSameSiteNoneMakesCookieSecureAndPartitioned() {
        HttpCookie cookie = new HttpCookie(NAME, VALUE);

        setupSameSiteConfig(true, SAMESITE_NONE, true);

        String encoded = CookieEncoder.encodeCookie(cookie, HttpHeaderKeys.HDR_SET_COOKIE, config, UA).toLowerCase();
        assertThat("Encoded cookie should have samesite=none", encoded, containsString("samesite=none"));
        assertThat("Encoded cookie should be secure", encoded, containsString("secure"));
        assertThat("Encoded cookie should be partitioned", encoded, containsString("partitioned"));
    }

    /**
     * Tests that a cookie name matching a configured pattern is assigned the proper SameSite
     * attribute. This ensures that pattern-based configuration rules are applied during encoding.
     */
    @Test
    public void testEncodePatternMatchSameSiteLax() {
        HttpCookie cookie = new HttpCookie("user_login", "xyz");

        setupSameSiteConfig(true, SAMESITE_LAX, false);

        Map<Pattern, String> patternMap = new HashMap<>();
        patternMap.put(Pattern.compile("user_.*"), "Lax");
        when(config.getSameSitePatterns()).thenReturn(patternMap);

        String encoded = CookieEncoder.encodeCookie(cookie, HttpHeaderKeys.HDR_SET_COOKIE, config, UA).toLowerCase();
        assertThat("Encoded cookie should include samesite=Lax for pattern match", encoded, containsString("samesite=lax"));
    }


   
}
