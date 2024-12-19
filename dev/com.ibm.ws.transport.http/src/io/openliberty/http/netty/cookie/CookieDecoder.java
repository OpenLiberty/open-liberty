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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.http.HttpCookie;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

/**
 * A utility class that decodes HTTP cookie header strings into a list of
 * {@link HttpCookie} instances. This class supports both standard
 * servlet-style cookies and uses Netty's cookie decoding for broader
 * compatibility when not in a Servlet 6 environment.
 * 
 * When Servlet 6 cookies are in use, the cookie string is parsed manually
 * to allow for additional flexibility and handling of attributes that may
 * begin with a dollar sign ('$'). For other scenarios, cookies are decoded
 * using Netty’s {@link ServerCookieDecoder}, which operates in LAX mode
 * by default.
 * 
 * Decoding Modes: LAX vs STRICT
 * 
 * Netty’s {@link ServerCookieDecoder} provides two modes for decoding cookies:
 * 
 * STRICT Mode: Enforces a strict interpretation of the RFC 6265
 * specification. Cookies that deviate from the RFC’s formatting requirements
 * (e.g., improper character usage, missing key-value pairs) will be rejected.
 * This mode ensures maximum standards compliance but may discard cookies
 * that are slightly non-compliant.
 * 
 * LAX Mode: Allows a more permissive decoding of cookies, accepting
 * certain non-standard or legacy formatting patterns. While it may decode
 * cookies that STRICT mode would reject, it provides broader compatibility
 * with cookies encountered in real-world scenarios.
 * 
 * 
 * By default, this class uses LAX mode to maximize compatibility.
 */
public class CookieDecoder {

    private CookieDecoder(){} 

    /**
     * Decodes the provided cookie header string into a list of
     * {@link HttpCookie} objects.
     * 
     * If Servlet 6 cookies are enabled, the cookie string is parsed
     * directly to accommodate additional features or attributes required
     * for Servlet 6.
     * 
     * If Servlet 6 cookies are not enabled, this method leverages the
     * use of Netty’s LAX mode decoder, which accepts a wide range of
     * cookie formats.
     * 
     * @param cookieString the raw cookie header string, as received from an
     *                         HTTP object
     * @return a list of decoded {@link HttpCookie} instances, or an empty list
     *         if the input is null or empty
     */
    public static List<HttpCookie> decode(String cookieString) {
                   
        if (cookieString == null || cookieString.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<HttpCookie> list = new LinkedList<HttpCookie>();
        boolean isServlet6Cookie = HttpDispatcher.useEE10Cookies();
        boolean foundDollarSign = false;
        String skipDollarSignName = null;
        String name = null;
        String value = null;
        String trimmedRawValue = null;

        if (isServlet6Cookie) {
            String[] rawCookies = cookieString.split(";");
            for (String rawCookie : rawCookies) {
                trimmedRawValue = rawCookie.trim();
                if(trimmedRawValue.isEmpty()){
                    continue;
                }

                foundDollarSign = ('$' == trimmedRawValue.charAt(0));

                String[] splitCookie = rawCookie.split("=", 2);
                if (splitCookie.length == 2) {
                    name = splitCookie[0].trim();
                    value = splitCookie[1].trim();

                } else {
                    name = trimmedRawValue;
                }

                if (foundDollarSign) {
                    skipDollarSignName = name.substring(1);
                    //$version skipped per Servlet specification
                    if ("version".equalsIgnoreCase(skipDollarSignName)) {
                        continue;
                    }
                }

                list.add(new HttpCookie(name, value));
                name = null;
                value = null;
            }

        } else {
            Set<Cookie> cookies = decodeNetty(cookieString);
            for (Cookie c : cookies) {
                list.add(new HttpCookie(c.name(), c.value()));
            }
        }

        return list;
    }

    /**
     * Decodes the given cookie header string using Netty’s LAX mode cookie
     * decoding. 
     * 
     * @param cookieString the raw cookie header string, as received from an HTTP object
     * @return a set of decoded Netty {@link Cookie} instances, or an empty set
     *         if the input is null or empty
     */
    public static Set<Cookie> decodeNetty(String cookieString) {
        if (cookieString == null || cookieString.isEmpty()) {
            return Collections.emptySet();
        }
        return ServerCookieDecoder.LAX.decode(cookieString);
    }
}