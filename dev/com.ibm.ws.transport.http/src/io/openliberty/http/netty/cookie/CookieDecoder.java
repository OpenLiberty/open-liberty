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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
        String trimmedRawValue = null;
        String cleanCookieString = cookieString;

        if (isServlet6Cookie) {
            String[] rawCookies = cookieString.split(";");
            StringBuilder processedCookieString = new StringBuilder();
            for (String rawCookie : rawCookies) {
                trimmedRawValue = rawCookie.trim();
                if(trimmedRawValue.isEmpty()){
                    continue;
                }

                foundDollarSign = ('$' == trimmedRawValue.charAt(0));

                if (foundDollarSign) {
                    skipDollarSignName = trimmedRawValue.substring(1).split("=")[0];
                    //$version skipped per Servlet specification
                    if ("version".equalsIgnoreCase(skipDollarSignName)) {
                        continue;
                    }
                } else {
                    if(processedCookieString.length()>0){
                        processedCookieString.append("; ");
                    }
                }
                processedCookieString.append(trimmedRawValue);
            } 
            cleanCookieString = processedCookieString.toString();

        } 

        list.addAll(decodeNetty(cleanCookieString));
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
    public static Set<HttpCookie> decodeNetty(String cookieString) {
        if (cookieString == null || cookieString.isEmpty()) {
            return Collections.emptySet();
        }
        Set<HttpCookie> cookies = new HashSet<HttpCookie>();

        try{
            for(Cookie c: ServerCookieDecoder.STRICT.decode(cookieString)){
                HttpCookie cookie = mapToCookie(c);
                cookie.setVersion(1);
                cookies.add(cookie);
            }
        } catch(Exception e){
            // Fallback to LAX decoder for Version 0
            for(Cookie c: ServerCookieDecoder.LAX.decode(cookieString)){
                HttpCookie cookie = mapToCookie(c);
                cookie.setVersion(0);
                cookies.add(cookie);
            }
        }
        return cookies;
    }

    private static HttpCookie mapToCookie(Cookie nettyCookie){
        HttpCookie cookie = new HttpCookie(nettyCookie.name(), nettyCookie.value());

        if(nettyCookie.domain() != null){
            cookie.setDomain(nettyCookie.domain());
        }
        if(nettyCookie.path() != null){
            cookie.setPath(nettyCookie.path());
        }
        if(nettyCookie.maxAge() != Long.MIN_VALUE){
            long maxAgeLong = nettyCookie.maxAge();
            if (maxAgeLong > Integer.MAX_VALUE){
                cookie.setMaxAge(Integer.MAX_VALUE);
            } else if (maxAgeLong < Integer.MIN_VALUE){
                cookie.setMaxAge(Integer.MIN_VALUE);
            } else {
                cookie.setMaxAge((int)maxAgeLong);
            }
        }
        cookie.setHttpOnly(nettyCookie.isHttpOnly());
        cookie.setSecure(nettyCookie.isSecure());

        return cookie;
    }
}