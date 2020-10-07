/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.cookies;

import java.util.Date;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * This class serves as a utility class for cookie serialization. It contains
 * methods to serialize cookies of type Cookie, Cookie2, Set-Cookie and
 * Set-Cookie2.
 */
public class CookieUtils {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(CookieUtils.class,
                                                         HttpMessages.HTTP_TRACE_NAME,
                                                         HttpMessages.HTTP_BUNDLE);

    /** from RFC 2068, token special case characters */
    private static final String TSPECIALS = "\"()<>@,;:\\/[]?={} \t";
    /** max-age: 0 should send a very old date to expire the cookie */
    private static final String longAgo = "; Expires=Thu, 01-Dec-94 16:00:00 GMT";
    private static final String longAgoRFC1123 = "; Expires=Thu, 01 Dec 1994 16:00:00 GMT";
    private static boolean skipCookiePathQuotes = false;

    /**
     * Default constructor for the utility class.
     */
    private CookieUtils() {
        // nothing to do
    }

    /**
     * Return the value of the HTTP header this cookie translates into. If the
     * given header is not supported or is unknown, then a simple name=value
     * string will be returned.
     *
     * @param cookie The cookie object that needs to be serialized
     * @param hdr
     * @param httpOnly if set
     * @param isv0CookieDateRFC1123 - Custom property needs to be set
     * @param skipCookiePathQuotes - Custom property needs to be set
     * @return String -- the HTTP header value of this cookie.
     * @throws NullPointerException if either input value is null.
     */
    public static String toString(HttpCookie cookie, HeaderKeys hdr, boolean isv0CookieDateRFC1123, boolean skipPathQuotes) {
        skipCookiePathQuotes = skipPathQuotes;
        return toString(cookie, hdr, isv0CookieDateRFC1123);
    }

    /**
     * Return the value of the HTTP header this cookie translates into. If the
     * given header is not supported or is unknown, then a simple name=value
     * string will be returned.
     *
     * @param cookie The cookie object that needs to be serialized
     * @param hdr
     * @return String -- the HTTP header value of this cookie.
     * @throws NullPointerException if either input value is null.
     */
    public static String toString(HttpCookie cookie, HeaderKeys hdr, boolean isv0CookieDateRFC1123) {

        if (null == cookie) {
            throw new NullPointerException("Null cookie input");
        }
        if (null == hdr) {
            throw new NullPointerException("Null header input");
        }

        try {
            if (0 == cookie.getVersion()) {
                // V0 cookies can be Cookie or Set-Cookie headers
                if (HttpHeaderKeys.HDR_COOKIE.equals(hdr)) {
                    return convertV0Cookie(cookie);
                } else if (HttpHeaderKeys.HDR_SET_COOKIE.equals(hdr)) {
                    return convertV0SetCookie(cookie, isv0CookieDateRFC1123);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Invalid header: " + hdr);
                }
            } else if (1 == cookie.getVersion()) {
                // V1 cookies could be any of the four
                if (HttpHeaderKeys.HDR_COOKIE.equals(hdr)) {
                    return convertV1Cookie(cookie);
                } else if (HttpHeaderKeys.HDR_COOKIE2.equals(hdr)) {
                    return convertV1Cookie2(cookie);
                } else if (HttpHeaderKeys.HDR_SET_COOKIE.equals(hdr)) {
                    return convertV1SetCookie(cookie);
                } else if (HttpHeaderKeys.HDR_SET_COOKIE2.equals(hdr)) {
                    return convertV1SetCookie2(cookie);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Invalid header: " + hdr);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Invalid Cookie version: " + cookie.getVersion());
                }
            }
            // default to the simplest format
            return convertV0Cookie(cookie);
        } catch (Exception e) {
            FFDCFilter.processException(e, CookieUtils.class.getName() + ".toString", "1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while converting the cookie; " + cookie);
            }
        }
        return null;
    }

    /**
     * Return true iff the string contains special characters that need to be
     * quoted.
     *
     * @param value
     * @return boolean
     */
    private static boolean needsQuote(String value) {
        if (null == value)
            return true;
        int len = value.length();
        if (0 == len) {
            return true;
        }
        if ('"' == value.charAt(0)) {
            if ('"' == value.charAt(len - 1)) {
                // already wrapped with quotes
                return false;
            }
            // we now know this has a quote (special char) in it, so return now
            return true;
        }

        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c >= 0x7f || TSPECIALS.indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Append the input value string to the given buffer, wrapping it with
     * quotes if need be.
     *
     * @param buff
     * @param value
     */
    private static void maybeQuote(StringBuilder buff, String value) {
        // PK48169 - handle a null value as well as an empty one
        if (null == value || 0 == value.length()) {
            buff.append("\"\"");
        } else if (needsQuote(value)) {
            buff.append('"');
            buff.append(value);
            buff.append('"');
        } else {
            buff.append(value);
        }
    }

    /**
     * Convert the V0 Cookie into a Cookie header string.
     *
     * @param cookie
     * @return the value of the header.
     */
    private static String convertV0Cookie(HttpCookie cookie) {
        StringBuilder buffer = new StringBuilder(40);

        // Append name=value
        buffer.append(cookie.getName());
        String value = cookie.getValue();
        if (null != value && 0 != value.length()) {
            buffer.append('=');
            buffer.append(value);
        } else {
            // PK48196 - send an empty value string
            buffer.append("=\"\"");
        }

        // check for optional path
        value = cookie.getPath();
        if (null != value && 0 != value.length()) {
            buffer.append("; $Path=");
            buffer.append(value);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created v0 Cookie: [" + buffer.toString() + "]");
        }
        return buffer.toString();
    }

    /**
     * Convert the V1 Cookie into a Cookie header string.
     *
     * @param cookie
     * @return the value of the header.
     */
    private static String convertV1Cookie(HttpCookie cookie) {
        StringBuilder buffer = new StringBuilder(40);

        // Append name=value
        buffer.append(cookie.getName());
        buffer.append('=');
        maybeQuote(buffer, cookie.getValue());

        // Always append the version
        buffer.append("; $Version=1");

        // check for optional path
        String value = cookie.getPath();
        if (null != value && 0 != value.length()) {
            buffer.append("; $Path=");
            if (!skipCookiePathQuotes) {
                maybeQuote(buffer, value);
            } else {
                buffer.append(value);
            }
        }

        // check for optional domain
        value = cookie.getDomain();
        if (null != value && 0 != value.length()) {
            buffer.append("; $Domain=");
            maybeQuote(buffer, value);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created v1 Cookie: [" + buffer.toString() + "]");
        }
        return buffer.toString();
    }

    /**
     * Convert the V1 Cookie into a Cookie2 header string.
     *
     * @param cookie
     * @return the value of the header.
     */
    private static String convertV1Cookie2(HttpCookie cookie) {
        // Note: J2EE Cookies do not support V1 RFC2965 Cookie2 objects yet so
        // this code isn't fully correct
        StringBuilder buffer = new StringBuilder(40);

        // Append name=value
        buffer.append(cookie.getName());
        buffer.append('=');
        maybeQuote(buffer, cookie.getValue());

        // Always append the version
        buffer.append("; $Version=1");

        // check for optional path
        String value = cookie.getPath();
        if (null != value && 0 != value.length()) {
            buffer.append("; $Path=");
            if (!skipCookiePathQuotes) {
                maybeQuote(buffer, value);
            } else {
                buffer.append(value);
            }
        }

        // check for optional domain
        value = cookie.getDomain();
        if (null != value && 0 != value.length()) {
            buffer.append("; $Domain=");
            maybeQuote(buffer, value);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created v1 Cookie2: [" + buffer.toString() + "]");
        }
        return buffer.toString();
    }

    /**
     * Convert the V0 Cookie into a Set-Cookie header string.
     *
     * @param cookie
     * @return the value of the header.
     */
    private static String convertV0SetCookie(HttpCookie cookie, boolean isRFC1123DateEnabled) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Cookie Name: " + cookie.getName());
            Tr.debug(tc, "Cookie MaxAge: " + cookie.getMaxAge());
            Tr.debug(tc, "RFC1123 Date Enabled (4 digit year): " + isRFC1123DateEnabled);
        }

        StringBuilder buffer = new StringBuilder(40);

        // Append name=value
        buffer.append(cookie.getName());
        String value = cookie.getValue();
        if (null != value && 0 != value.length()) {
            buffer.append('=');
            buffer.append(value);
        } else {
            // PK48196 - send an empty value string
            buffer.append("=\"\"");
        }

        // check for optional comment
        String comment = cookie.getComment();
        if (null != comment && 0 != comment.length()) {
            buffer.append("; Comment=");
            maybeQuote(buffer, comment);
        }

        // check for optional max-age/expires
        int expires = cookie.getMaxAge();
        if (-1 < expires) {
            if (0 == expires) {
                if (!isRFC1123DateEnabled) {
                    buffer.append(longAgo);
                } else {
                    buffer.append(longAgoRFC1123);
                }
            } else {
                buffer.append("; Expires=");
                if (!isRFC1123DateEnabled) {
                    buffer.append(HttpDispatcher.getDateFormatter().getRFC2109Time(new Date(HttpDispatcher.getApproxTime() + (expires * 1000L))));
                } else {
                    buffer.append(HttpDispatcher.getDateFormatter().getRFC1123Time(new Date(HttpDispatcher.getApproxTime() + (expires * 1000L))));
                }
            }
        } else if (cookie.isDiscard()) {
            // convert discard to immediate expiration
            if (!isRFC1123DateEnabled) {
                buffer.append(longAgo);
            } else {
                buffer.append(longAgoRFC1123);
            }
        }

        // check for optional path
        value = cookie.getPath();
        if (null != value && 0 != value.length()) {
            buffer.append("; Path=");
            buffer.append(value);
        }

        // check for optional domain
        value = cookie.getDomain();
        if (null != value && 0 != value.length()) {
            buffer.append("; Domain=");
            buffer.append(value);
        }

        if (cookie.isSecure()) {
            buffer.append("; Secure");
        }

        if (cookie.isHttpOnly()) {
            buffer.append("; HttpOnly");
        }

        //check for optional samesite
        value = cookie.getAttribute("samesite");
        if (null != value && 0 != value.length()) {
            buffer.append("; SameSite=");
            buffer.append(value);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created v0 Set-Cookie: [" + GenericUtils.nullOutPasswords(buffer.toString(), (byte) '&') + "]");
        }
        return buffer.toString();
    }

    /**
     * Convert the V1 Cookie into a Set-Cookie header string.
     *
     * @param cookie
     * @return the value of the header.
     */
    private static String convertV1SetCookie(HttpCookie cookie) {
        StringBuilder buffer = new StringBuilder(40);

        // Append name=value
        buffer.append(cookie.getName());
        buffer.append('=');
        maybeQuote(buffer, cookie.getValue());

        // insert version - required
        buffer.append("; Version=1");

        // check for optional comment
        String value = cookie.getComment();
        if (null != value && 0 != value.length()) {
            buffer.append("; Comment=");
            maybeQuote(buffer, value);
        }

        // check for optional domain
        value = cookie.getDomain();
        if (null != value && 0 != value.length()) {
            buffer.append("; Domain=");
            maybeQuote(buffer, value);
        }

        // check for optional max-age
        int maxAge = cookie.getMaxAge();
        if (-1 < maxAge) {
            buffer.append("; Max-Age=");
            buffer.append(maxAge);
        } else if (cookie.isDiscard()) {
            // convert discard to immediate max-age
            buffer.append("; Max-Age=0");
        }

        // check for optional path
        value = cookie.getPath();
        if (null != value && 0 != value.length()) {
            buffer.append("; Path=");
            if (!skipCookiePathQuotes) {
                maybeQuote(buffer, value);
            } else {
                buffer.append(value);
            }
        }

        if (cookie.isSecure()) {
            buffer.append("; Secure");
        }

        if (cookie.isHttpOnly()) {
            buffer.append("; HttpOnly");
        }

        //check for optional samesite
        value = cookie.getAttribute("samesite");
        if (null != value && 0 != value.length()) {
            buffer.append("; SameSite=");
            buffer.append(value);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created v1 Set-Cookie: [" + buffer.toString() + "]");
        }

        return buffer.toString();
    }

    /**
     * Convert the V1 Cookie into a Set-Cookie2 header string.
     *
     * @param cookie
     * @return the value of the header.
     */
    private static String convertV1SetCookie2(HttpCookie cookie) {
        // Note: J2EE cookies don't support V1 RFC2965 Set-Cookie2 because there
        // are additional pieces like Port, Discard, CommentURL, etc.
        StringBuilder buffer = new StringBuilder(40);

        // Append name=value
        buffer.append(cookie.getName());
        buffer.append('=');
        maybeQuote(buffer, cookie.getValue());

        // insert version - required
        buffer.append("; Version=1");

        // check for optional comment
        String value = cookie.getComment();
        if (null != value && 0 != value.length()) {
            buffer.append("; Comment=");
            maybeQuote(buffer, value);
        }

        // check for optional domain
        value = cookie.getDomain();
        if (null != value && 0 != value.length()) {
            buffer.append("; Domain=");
            maybeQuote(buffer, value);
        }

        // check for optional max-age
        int maxAge = cookie.getMaxAge();
        if (-1 < maxAge) {
            buffer.append("; Max-Age=");
            buffer.append(maxAge);
        }

        // check for optional path
        value = cookie.getPath();
        if (null != value && 0 != value.length()) {
            buffer.append("; Path=");
            if (!skipCookiePathQuotes) {
                maybeQuote(buffer, value);
            } else {
                buffer.append(value);
            }
        }

        value = cookie.getAttribute("commenturl");
        if (null != value && 0 != value.length()) {
            buffer.append("; CommentURL=");
            maybeQuote(buffer, value);
        }

        value = cookie.getAttribute("port");
        if (null != value && 0 != value.length()) {
            buffer.append("; Port=");
            maybeQuote(buffer, value);
        }

        if (cookie.isDiscard()) {
            buffer.append("; Discard");
        }

        if (cookie.isSecure()) {
            buffer.append("; Secure");
        }

        if (cookie.isHttpOnly()) {
            buffer.append("; HttpOnly");
        }

        //check for optional samesite
        value = cookie.getAttribute("samesite");
        if (null != value && 0 != value.length()) {
            buffer.append("; SameSite=");
            buffer.append(value);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created v1 Set-Cookie2: [" + buffer.toString() + "]");
        }

        return buffer.toString();
    }

}
