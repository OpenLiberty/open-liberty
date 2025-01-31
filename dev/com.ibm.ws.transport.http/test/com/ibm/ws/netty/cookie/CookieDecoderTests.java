/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.openliberty.http.netty.cookie.CookieDecoder;
import io.openliberty.http.netty.cookie.CookieEncoder;

/**
 * Tests the functionality of the {@link CookieDecoder} utility.
 * These tests ensure that cookies are properly decoded from "Cookie" headers.
 */
public class CookieDecoderTests {

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
        List<HttpCookie> cookies = CookieDecoder.decodeServerCookies(cookieString);

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
        List<HttpCookie> cookies = CookieDecoder.decodeServerCookies("");
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
        List<HttpCookie> cookies = CookieDecoder.decodeServerCookies(cookieString);

        assertThat("Expected one cookie after ignoring $version attribute", cookies, hasSize(1));
        HttpCookie c = cookies.get(0);
        assertThat("Cookie name should be 'cookieFlavor'", c.getName(), is("cookieFlavor"));
        assertThat("Cookie value should be 'vanilla'", c.getValue(), is("vanilla"));
    }

}
