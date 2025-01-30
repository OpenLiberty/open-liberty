/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty;

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
import org.omg.CORBA.DynAnyPackage.Invalid;

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


    /**
     * Sets up mock configuration and header objects before each test.
     */
    @Before
    public void setUp() {
        config = mock(HttpChannelConfig.class);

        when(config.useSameSiteConfig()).thenReturn(false);
        when(config.getSameSiteCookies()).thenReturn(new HashMap<>());
        when(config.onlySameSiteStar()).thenReturn(false);
        when(config.getSameSitePatterns()).thenReturn(new HashMap<>());
        when(config.getPartitioned()).thenReturn(false);
        when(config.isv0CookieDateRFC1123compat()).thenReturn(true);
        when(config.shouldSkipCookiePathQuotes()).thenReturn(false);

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

    @Test 
    public void testEncodeVersion0WithMaxAgeShouldUpgrade(){
        HttpCookie cookie = new HttpCookie(NAME, VALUE);
        cookie.setVersion(0);
        cookie.setMaxAge(3600);
        cookie.setAttribute(SAMESITE, SAMESITE_NONE);
        cookie.setSecure(false);

        setupSameSiteConfig(true, SAMESITE_NONE, true);

        String encoded = CookieEncoder.encode(cookie, header, config);

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
        

        String encoded = CookieEncoder.encode(cookie, HttpHeaderKeys.HDR_SET_COOKIE, config).toLowerCase();
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

        String encoded = CookieEncoder.encode(cookie, HttpHeaderKeys.HDR_SET_COOKIE, config).toLowerCase();
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

        String encoded = CookieEncoder.encode(cookie, HttpHeaderKeys.HDR_SET_COOKIE, config).toLowerCase();
        assertThat("Encoded cookie should include samesite=Lax for pattern match", encoded, containsString("samesite=lax"));
    }

//     A. Version Determination and Upgrades
// Version 0 Cookie without SameSite and Max-Age:

// Expectation: Remain Version=0.
// Attributes: No SameSite, no Max-Age, no Secure.
// Version 0 Cookie with Max-Age:

// Expectation: Upgrade to Version=1, retain Max-Age.
// Version 0 Cookie with Expires:

// Expectation: Upgrade to Version=1, convert Expires to Max-Age.
// Version 0 Cookie with Both Max-Age and Expires:

// Expectation: Upgrade to Version=1, retain Max-Age, optionally remove Expires.
// Version 0 Cookie with SameSite and Max-Age:

// Expectation: Upgrade to Version=1, retain Max-Age, apply SameSite.
// Version 0 Cookie with SameSite but No Max-Age:

// Expectation: Upgrade to Version=1, apply SameSite.
// Version 0 Cookie with Invalid SameSite Value:

// Expectation: Upgrade to Version=1, default SameSite to Lax.
// Version 0 Cookie with SameSite=None and Secure=false:

// Expectation: Upgrade to Version=1, enforce Secure=true.
// Version 0 Cookie with SameSite=None and Secure=true:

// Expectation: Upgrade to Version=1, retain Secure=true.


// B. Version 1 Cookie Handling
// Version 1 Cookie with SameSite=None and Valid Max-Age:

// Expectation: Retain Version=1, retain Max-Age, enforce Secure=true.
// Version 1 Cookie with SameSite=Strict and Max-Age:

// Expectation: Retain Version=1, retain Max-Age, Secure=false.
// Version 1 Cookie with SameSite=Lax and Max-Age:

// Expectation: Retain Version=1, retain Max-Age, Secure=false.
// Version 1 Cookie without SameSite but with Max-Age:

// Expectation: Retain Version=1, retain Max-Age, no SameSite.
// Version 1 Cookie with SameSite and Secure=true:

// Expectation: Retain Version=1, retain SameSite, retain Secure=true.
// Version 1 Cookie with Invalid SameSite Value:

// Expectation: Retain Version=1, default SameSite to Lax, retain Max-Age.
// Version 1 Cookie with Max-Age Exceeding Integer.MAX_VALUE:

// Expectation: Retain Version=1, cap Max-Age at Integer.MAX_VALUE.
// Version 1 Cookie with Expires Attribute (Non-standard):

// Expectation: Retain Version=1, handle Expires appropriately (convert to Max-Age).



// C. Partitioned Attribute Handling
// Version 1 Cookie with Partitioned Attribute Set via Configuration:

// Expectation: Apply Partitioned=.
// Version 1 Cookie with Partitioned Already Set:

// Expectation: Do not override or duplicate Partitioned=.



// D. Configuration-Based SameSite Application
// Configuration Mandates SameSite but Cookie Lacks It:

// Expectation: Apply SameSite as per configuration.
// Configuration Mandates SameSite with Specific Patterns:

// Expectation: Apply SameSite based on cookie name patterns.


// E. Edge Cases and Miscellaneous Scenarios
// Malformed Expires Attribute:

// Expectation: Set Max-Age=0 to expire the cookie immediately.
// Cookie with Unsupported Version (e.g., Version=2):

// Expectation: Handle gracefully, possibly default to Version=0 or 1.
// Cookie with Reserved Characters in Attributes:

// Expectation: Handle quoting or validation as necessary.
// Cookie with SameSite in Various Cases (None, none, NONE):

// Expectation: Treat all case variations equivalently.
// Cookie with Custom Attributes:

// Expectation: Preserve custom attributes without interference.
// Version 1 Cookie without Max-Age or Expires:

// Expectation: Retain Version=1, no Max-Age, apply SameSite as per configuration.
// Cookies with Only Path and Domain Set:

// Expectation: Handle as per version and configuration.
// Multiple Cookies in a Single Header (if applicable):

// Expectation: Correctly encode each cookie individually.
// Cookie Attributes Order (if order matters):

// Expectation: Attributes are serialized in a standard or expected order.

   
}
