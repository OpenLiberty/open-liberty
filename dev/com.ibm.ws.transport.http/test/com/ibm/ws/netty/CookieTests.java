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
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.openliberty.http.netty.cookie.CookieDecoder;
import io.openliberty.http.netty.cookie.CookieEncoder;

/**
 * Tests the functionality of the {@link CookieEncoder} and {@link CookieDecoder} utilities.
 * These tests ensure that cookies are properly decoded from header strings and encoded into
 * appropriate header formats, respecting configuration settings related to SameSite, partitioning,
 * and other cookie attributes.
 */
public class CookieTests{

    private HttpChannelConfig config;
    private HeaderKeys header;

    /**
     * Sets up mock configuration and header objects before each test.
     */
    @Before
    public void setUp() {
        config = mock(HttpChannelConfig.class);
        header = mock(HeaderKeys.class);
    }

    /**
     * Tests that a simple cookie header is decoded correctly into a list
     * of {@link HttpCookie} instances. The input should represent a valid Cookie header
     * (e.g., "Cookie: myCookie=myValue") without attributes such as Path or HttpOnly,
     * which are not part of the Cookie header format.
     */
    @Test
    public void testDecodeCookie() {
        // Using a simple cookie pair without Path and HttpOnly attributes
        String cookieString = "myCookie=myValue";
        List<HttpCookie> cookies = CookieDecoder.decode(cookieString);

        assertThat("Expected one cookie to be decoded", cookies, hasSize(1));
        HttpCookie c = cookies.get(0);
        assertThat("Cookie name should match input", c.getName(), is("myCookie"));
        assertThat("Cookie value should match input", c.getValue(), is("myValue"));
    }

    /**
     * Tests that an empty cookie string returns an empty list of {@link HttpCookie} objects.
     * This verifies behavior when no cookies are present.
     */
    @Test
    public void testDecodeEmptyString() {
        List<HttpCookie> cookies = CookieDecoder.decode("");
        assertThat("Expected no cookies to be decoded from empty string", cookies, is(empty()));
    }

    /**
     * Tests that a cookie string containing attributes prefixed with a dollar sign ('$')
     * is decoded correctly. This checks that special attributes are handled as expected.
     * The cookie string here should still represent a valid cookie header line.
     */
    @Test
    public void testDecodeServlet6WithDollarSign() {
        String cookieString = "$version=1; cookieFlavor=vanilla";
        List<HttpCookie> cookies = CookieDecoder.decode(cookieString);

        assertThat("Expected one cookie after ignoring $version attribute", cookies, hasSize(1));
        HttpCookie c = cookies.get(0);
        assertThat("Cookie name should be 'cookieFlavor'", c.getName(), is("cookieFlavor"));
        assertThat("Cookie value should be 'vanilla'", c.getValue(), is("vanilla"));
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
        when(config.getSameSiteCookies()).thenReturn(sameSiteMap);
        when(config.onlySameSiteStar()).thenReturn(false);
        when(config.getSameSitePatterns()).thenReturn(Collections.emptyMap());
        when(config.getPartitioned()).thenReturn(false);
        when(config.isv0CookieDateRFC1123compat()).thenReturn(false);
        when(config.shouldSkipCookiePathQuotes()).thenReturn(false);

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
        HttpCookie cookie = new HttpCookie("testCookie", "value");

        when(config.useSameSiteConfig()).thenReturn(true);

        Map<String, String> sameSiteMap = new HashMap<>();
        sameSiteMap.put("testCookie", "None");
        when(config.getSameSiteCookies()).thenReturn(sameSiteMap);
        when(config.onlySameSiteStar()).thenReturn(false);
        when(config.getSameSitePatterns()).thenReturn(Collections.emptyMap());
        when(config.getPartitioned()).thenReturn(true);
        when(config.isv0CookieDateRFC1123compat()).thenReturn(false);
        when(config.shouldSkipCookiePathQuotes()).thenReturn(false);

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

        when(config.useSameSiteConfig()).thenReturn(true);
        when(config.onlySameSiteStar()).thenReturn(false);
        when(config.getSameSiteCookies()).thenReturn(Collections.emptyMap());

        Map<Pattern, String> patternMap = new HashMap<>();
        patternMap.put(Pattern.compile("user_.*"), "Lax");
        when(config.getSameSitePatterns()).thenReturn(patternMap);

        when(config.getPartitioned()).thenReturn(false);
        when(config.isv0CookieDateRFC1123compat()).thenReturn(false);
        when(config.shouldSkipCookiePathQuotes()).thenReturn(false);

        String encoded = CookieEncoder.encode(cookie, HttpHeaderKeys.HDR_SET_COOKIE, config).toLowerCase();
        assertThat("Encoded cookie should include samesite=Lax for pattern match", encoded, containsString("samesite=lax"));
    }

}
