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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ibm.ws.http.channel.internal.cookies.CookieHeaderByteParser;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * A utility class that decodes HTTP "Cookie" and "Set-Cookie" headers. This
 * considers cookies that adhere to either legacy or current RFC specifications.
 * After parsing the header String, a list of {@link HttpCookie} objects is 
 * provided. 
 * 
 * Note: Support for "Cookie2" and "Set-Cookie2" (described by RFC 2965), is 
 * considered depracated. Most modern clients and browsers follow the newer
 * RFC 6265. This utility provides parsing of these older versions for 
 * compatibility considerations. 
 */
public class CookieDecoder {

    private CookieDecoder(){
        //Private singleton
    } 
    
    /**
     * Decodes a "Cookie" header string into a list of {@link HttpCookie} objects.
     * 
     * Use {@link #decodeSerCookies2(String)} if needing to decode RFC 2965 "Cookie2"
     * headers. 
     * 
     * @param cookieString the "Cookie" header value, e.g. "name=value; foo=bar"
     * @return a list of {@link HttpCookie} instances, or an empty list if the input
     * is {@code null} or empty.
     */
    public static List<HttpCookie> decodeServerCookies(String cookieString) {
                       
        return decode(cookieString, HttpHeaderKeys.HDR_COOKIE);    
    }

    /**
     * Decodes a "Cookie2" header string (as described by RFC 2965) into a list 
     * of {@link HttpCookie} objects.
     * 
     * Note: modern clients generally do not send Cookie2 headers, so this is 
     * considered a deprecated type of header.
     * 
     * @param cookieString the "Cookie2" header value
     * @return a list of {@link HttpCookie} instances, or an empty list if the 
     * input is {@code null} or empty.
     */
    public static List<HttpCookie> decodeServerCookies2(String cookieString){
        return decode(cookieString, HttpHeaderKeys.HDR_COOKIE2);
    }

    /**
     * Decodes a "Set-Cookie" header string into a list of {@link HttpCookie} objects.
     * 
     * Use {@link #decodeSetCookieHeader2(String)} if needing to decode RFC 2965
     * "Set-Cookie2" headers.
     * 
     * @param cookieString the "Set-Cookie" header value, e.g "name=value; Path=/"
     * @return a list of {@link HttpCookie} instances, or an empty list if the
     * input is {@code null} or empty.
     */
    public static List<HttpCookie> decodeSetCookieHeader(String cookieString){
        return decode(cookieString, HttpHeaderKeys.HDR_SET_COOKIE);
    }

    /**
     * Decodes a "Set-Cookie2" header string (as described by RFC 2965) into a 
     * list of {@link HttpCookie} objects. 
     * 
     * Note: modern clients generally do not support Set-Cookie2, so this is 
     * considered a deprecated type of header. This is present for compatibility
     * with older implementation specifications.
     * 
     * @param cookieString the "Set-Cookie2" header value
     * @return a list of {@link HttpCookie} instances, or an empty list if the
     * input is {@code null} or empty.
     */
    public static List<HttpCookie> decodeSetCookie2Header(String cookieString){
        return decode(cookieString, HttpHeaderKeys.HDR_SET_COOKIE2);
    }


    /**
     * Decodes a cookie header string into a list of {@link HttpCookie} objetcts. 
     * The header key parameter determines the type of cookie header being parsed. 
     * 
     * @param cookieString the cookie header value
     * @param header the cookie header type used to determine parsing logic
     * @return a list of {@link HttpCookie} instances, or an empty list if the 
     * input is {@code null} or empty.
     */
    public static List<HttpCookie> decode(String cookieString, HttpHeaderKeys header){
        if (cookieString == null || cookieString.isEmpty()) {
            return Collections.emptyList();
        }
        CookieHeaderByteParser parser = new CookieHeaderByteParser();
        return parser.parse(cookieString.getBytes(), header);
    }
}