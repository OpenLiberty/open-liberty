/*******************************************************************************
 * Copyright (c) 2004, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.KeyMatcher;

/**
 * Class representing a single HTTP header name.
 */
public class HttpHeaderKeys extends HeaderKeys {

    /** Counter of the number of values defined so far */
    private static final AtomicInteger NEXT_ORDINAL = new AtomicInteger(0);
    /** List keeping track of all the values, used by the corresponding matcher */
    private static final List<HttpHeaderKeys> allKeys = new ArrayList<HttpHeaderKeys>(100);
    /** Matcher used for HTTP header keys */
    private static final KeyMatcher myMatcher = new KeyMatcher(false);

    /** Enumerated object for the HTTP header key ACCEPT */
    public static final HttpHeaderKeys HDR_ACCEPT = new HttpHeaderKeys("Accept");
    /** Enumerated object for the HTTP header key ACCEPT-ENCODING */
    public static final HttpHeaderKeys HDR_ACCEPT_ENCODING = new HttpHeaderKeys("Accept-Encoding");
    /** Enumerated object for the HTTP header key ACCEPT-LANGUAGE */
    public static final HttpHeaderKeys HDR_ACCEPT_LANGUAGE = new HttpHeaderKeys("Accept-Language");
    /** Enumerated object for the HTTP header key ACCEPT-CHARSET */
    public static final HttpHeaderKeys HDR_ACCEPT_CHARSET = new HttpHeaderKeys("Accept-Charset");
    /** Enumerated object for the HTTP header key ACCEPT-RANGES */
    public static final HttpHeaderKeys HDR_ACCEPT_RANGES = new HttpHeaderKeys("Accept-Ranges");
    /** Enumerated object for the HTTP header key AGE */
    public static final HttpHeaderKeys HDR_AGE = new HttpHeaderKeys("Age", true, true);
    /** Enumerated object for the HTTP header key ALLOW */
    public static final HttpHeaderKeys HDR_ALLOW = new HttpHeaderKeys("Allow");
    /** Enumerated object for the HTTP header key AUTHORIZATION */
    public static final HttpHeaderKeys HDR_AUTHORIZATION = new HttpHeaderKeys("Authorization", false, false);
    /** Enumerated object for the HTTP header key ACCEPT-FEATURES */
    public static final HttpHeaderKeys HDR_ACCEPT_FEATURES = new HttpHeaderKeys("Accept-Features");
    /** Enumerated object for the HTTP header key ALTERNATES */
    public static final HttpHeaderKeys HDR_ALTERNATES = new HttpHeaderKeys("Alternates");
    /** Enumerated object for the HTTP header key CONNECTION */
    public static final HttpHeaderKeys HDR_CONNECTION = new HttpHeaderKeys("Connection", true, true);
    /** Enumerated object for the HTTP header key CONTENT-LENGTH */
    public static final HttpHeaderKeys HDR_CONTENT_LENGTH = new HttpHeaderKeys("Content-Length", true, true);
    /** Enumerated object for the HTTP header key CONTENT-TYPE */
    public static final HttpHeaderKeys HDR_CONTENT_TYPE = new HttpHeaderKeys("Content-Type");
    /** Enumerated object for the HTTP header key CONTENT-ENCODING */
    public static final HttpHeaderKeys HDR_CONTENT_ENCODING = new HttpHeaderKeys("Content-Encoding", true, true);
    /** Enumerated object for the HTTP header key COOKIE */
    public static final HttpHeaderKeys HDR_COOKIE = new HttpHeaderKeys("Cookie");
    /** Enumerated object for the HTTP header key COOKIE2 */
    public static final HttpHeaderKeys HDR_COOKIE2 = new HttpHeaderKeys("Cookie2");
    /** Enumerated object for the HTTP header key CONTENT-LANGUAGE */
    public static final HttpHeaderKeys HDR_CONTENT_LANGUAGE = new HttpHeaderKeys("Content-Language");
    /** Enumerated object for the HTTP header key CACHE-CONTROL */
    public static final HttpHeaderKeys HDR_CACHE_CONTROL = new HttpHeaderKeys("Cache-Control");
    /** Enumerated object for the HTTP header key CONTENT-DISPOSITION */
    public static final HttpHeaderKeys HDR_CONTENT_DISPOSITION = new HttpHeaderKeys("Content-Disposition");
    /** Enumerated object for the HTTP header key CONTENT-LOCATION */
    public static final HttpHeaderKeys HDR_CONTENT_LOCATION = new HttpHeaderKeys("Content-Location");
    /** Enumerated object for the HTTP header key CONTENT-MD5 */
    public static final HttpHeaderKeys HDR_CONTENT_MD5 = new HttpHeaderKeys("Content-MD5");
    /** Enumerated object for the HTTP header key CONTENT-RANGE */
    public static final HttpHeaderKeys HDR_CONTENT_RANGE = new HttpHeaderKeys("Content-Range");
    /** Enumerated object for the HTTP header key DATE */
    public static final HttpHeaderKeys HDR_DATE = new HttpHeaderKeys("Date");
    /** Enumerated object for the HTTP header key ETAG */
    public static final HttpHeaderKeys HDR_ETAG = new HttpHeaderKeys("ETag");
    /** Enumerated object for the HTTP header key EXPECT */
    public static final HttpHeaderKeys HDR_EXPECT = new HttpHeaderKeys("Expect", true, true);
    /** Enumerated object for the HTTP header key EXPIRES */
    public static final HttpHeaderKeys HDR_EXPIRES = new HttpHeaderKeys("Expires");
    /** Enumerated object for the HTTP header key FROM */
    public static final HttpHeaderKeys HDR_FROM = new HttpHeaderKeys("From");
    /** Enumerated object for the HTTP header key HOST */
    public static final HttpHeaderKeys HDR_HOST = new HttpHeaderKeys("Host");
    /** Enumerated object for the HTTP header key HTTP2-SETTINGS */
    public static final HttpHeaderKeys HDR_HTTP2_SETTINGS = new HttpHeaderKeys("HTTP2-Settings");
    /** Enumerated object for the HTTP header key IF-MATCH */
    public static final HttpHeaderKeys HDR_IF_MATCH = new HttpHeaderKeys("If-Match");
    /** Enumerated object for the HTTP header key IF-MODIFIED-SINCE */
    public static final HttpHeaderKeys HDR_IF_MODIFIED_SINCE = new HttpHeaderKeys("If-Modified-Since");
    /** Enumerated object for the HTTP header key IF-NONE-MATCH */
    public static final HttpHeaderKeys HDR_IF_NONE_MATCH = new HttpHeaderKeys("If-None-Match");
    /** Enumerated object for the HTTP header key IF-RANGE */
    public static final HttpHeaderKeys HDR_IF_RANGE = new HttpHeaderKeys("If-Range");
    /** Enumerated object for the HTTP header key IF-UNMODIFIED-SINCE */
    public static final HttpHeaderKeys HDR_IF_UNMODIFIED_SINCE = new HttpHeaderKeys("If-Unmodified-Since");
    /** Enumerated object for the HTTP header key KEEP-ALIVE */
    public static final HttpHeaderKeys HDR_KEEP_ALIVE = new HttpHeaderKeys("Keep-Alive");
    /** Enumerated object for the HTTP header key LOCATION */
    public static final HttpHeaderKeys HDR_LOCATION = new HttpHeaderKeys("Location");
    /** Enumerated object for the HTTP header key LAST-MODIFIED */
    public static final HttpHeaderKeys HDR_LAST_MODIFIED = new HttpHeaderKeys("Last-Modified");
    /** Enumerated object for the HTTP header key MAX-FORWARDS */
    public static final HttpHeaderKeys HDR_MAX_FORWARDS = new HttpHeaderKeys("Max-Forwards", true, true);
    /** Enumerated object for the HTTP header key NEGOTIATE */
    public static final HttpHeaderKeys HDR_NEGOTIATE = new HttpHeaderKeys("Negotiate");
    /** Enumerated object for the HTTP header key PRAGMA */
    public static final HttpHeaderKeys HDR_PRAGMA = new HttpHeaderKeys("Pragma");
    /** Enumerated object for the HTTP header key P3P */
    public static final HttpHeaderKeys HDR_P3P = new HttpHeaderKeys("P3P");
    /** Enumerated object for the HTTP header key PROXY-AUTHENTICATE */
    public static final HttpHeaderKeys HDR_PROXY_AUTHENTICATE = new HttpHeaderKeys("Proxy-Authenticate");
    /** Enumerated object for the HTTP header key PROXY-AUTHORIZATION */
    public static final HttpHeaderKeys HDR_PROXY_AUTHORIZATION = new HttpHeaderKeys("Proxy-Authorization", false, false);
    /** Enumerated object for the HTTP header PROXY-CONNECTION */
    public static final HttpHeaderKeys HDR_PROXY_CONNECTION = new HttpHeaderKeys("Proxy-Connection");
    /** Enumerated object for the HTTP header key REFERER */
    public static final HttpHeaderKeys HDR_REFERER = new HttpHeaderKeys("Referer");
    /** Enumerated object for the HTTP header key RANGE */
    public static final HttpHeaderKeys HDR_RANGE = new HttpHeaderKeys("Range");
    /** Enumerated object for the HTTP header key RETRY-AFTER */
    public static final HttpHeaderKeys HDR_RETRY_AFTER = new HttpHeaderKeys("Retry-After");
    /** Enumerated object for the HTTP header key SERVER */
    public static final HttpHeaderKeys HDR_SERVER = new HttpHeaderKeys("Server");
    /** Enumerated object for the HTTP header key SET-COOKIE */
    public static final HttpHeaderKeys HDR_SET_COOKIE = new HttpHeaderKeys("Set-Cookie");
    /** Enumerated object for the HTTP header key SET-COOKIE2 */
    public static final HttpHeaderKeys HDR_SET_COOKIE2 = new HttpHeaderKeys("Set-Cookie2");
    /** Enumerated object for the HTTP header key SOAPACTION */
    public static final HttpHeaderKeys HDR_SOAPACTION = new HttpHeaderKeys("SOAPAction");
    /** Enumerated object for the HTTP header key SURROGATE-CAPABILITY */
    public static final HttpHeaderKeys HDR_SURROGATE_CAPABILITY = new HttpHeaderKeys("Surrogate-Capability");
    /** Enumerated object for the HTTP header key SURROGATE-CONTROL */
    public static final HttpHeaderKeys HDR_SURROGATE_CONTROL = new HttpHeaderKeys("Surrogate-Control");
    /** Enumerated object for the HTTP header key TRANSFER-ENCODING */
    public static final HttpHeaderKeys HDR_TRANSFER_ENCODING = new HttpHeaderKeys("Transfer-Encoding", true, true);
    /** Enumerated object for the HTTP header key TE */
    public static final HttpHeaderKeys HDR_TE = new HttpHeaderKeys("TE");
    /** Enumerated object for the HTTP header key TRAILER */
    public static final HttpHeaderKeys HDR_TRAILER = new HttpHeaderKeys("Trailer");
    /** Enumerated object for the HTTP header key TCN */
    public static final HttpHeaderKeys HDR_TCN = new HttpHeaderKeys("TCN");
    /** Enumerated object for the HTTP header key USER-AGENT */
    public static final HttpHeaderKeys HDR_USER_AGENT = new HttpHeaderKeys("User-Agent");
    /** Enumerated object for the HTTP header key UPGRADE */
    public static final HttpHeaderKeys HDR_UPGRADE = new HttpHeaderKeys("Upgrade");
    /** Enumerated object for the HTTP header key VARY */
    public static final HttpHeaderKeys HDR_VARY = new HttpHeaderKeys("Vary");
    /** Enumerated object for the HTTP header key VIA */
    public static final HttpHeaderKeys HDR_VIA = new HttpHeaderKeys("Via");
    /** Enumerated object for the HTTP header key VARIANT-VARY */
    public static final HttpHeaderKeys HDR_VARIANT_VARY = new HttpHeaderKeys("Variant-Vary");
    /** Enumerated object for the HTTP header key WARNING */
    public static final HttpHeaderKeys HDR_WARNING = new HttpHeaderKeys("Warning");
    /** Enumerated object for the HTTP header key WWW-AUTHENTICATE */
    public static final HttpHeaderKeys HDR_WWW_AUTHENTICATE = new HttpHeaderKeys("WWW-Authenticate");
    /** Private WAS header for Auth_Type */
    public static final HttpHeaderKeys HDR_$WSAT = new HttpHeaderKeys("$WSAT");
    /** Private WAS header for Client Certificate */
    public static final HttpHeaderKeys HDR_$WSCC = new HttpHeaderKeys("$WSCC");
    /** Private WAS header for Cipher Suite */
    public static final HttpHeaderKeys HDR_$WSCS = new HttpHeaderKeys("$WSCS");
    /** Private WAS header for Is_Secure */
    public static final HttpHeaderKeys HDR_$WSIS = new HttpHeaderKeys("$WSIS");
    /** Private WAS header for Scheme */
    public static final HttpHeaderKeys HDR_$WSSC = new HttpHeaderKeys("$WSSC");
    /** Private WAS header for Protocol */
    public static final HttpHeaderKeys HDR_$WSPR = new HttpHeaderKeys("$WSPR");
    /** Private WAS header for Remote_Address */
    public static final HttpHeaderKeys HDR_$WSRA = new HttpHeaderKeys("$WSRA");
    /** Private WAS header for Remote_Host */
    public static final HttpHeaderKeys HDR_$WSRH = new HttpHeaderKeys("$WSRH");
    /** Private WAS header for Remove_User */
    public static final HttpHeaderKeys HDR_$WSRU = new HttpHeaderKeys("$WSRU");
    /** Private WAS header for Server_Name */
    public static final HttpHeaderKeys HDR_$WSSN = new HttpHeaderKeys("$WSSN");
    /** Private WAS header for Server_Port */
    public static final HttpHeaderKeys HDR_$WSSP = new HttpHeaderKeys("$WSSP");
    /** Private WAS header for SSL_Session_ID */
    public static final HttpHeaderKeys HDR_$WSSI = new HttpHeaderKeys("$WSSI");
    /** Private WAS header for AutoCompression */
    public static final HttpHeaderKeys HDR_$WSZIP = new HttpHeaderKeys("$WSZIP");
    /** Private WAS header for Partition_Table */
    public static final HttpHeaderKeys HDR_$WSPT = new HttpHeaderKeys("$WSPT");
    /** Private WAS header for error page information */
    public static final HttpHeaderKeys HDR_$WSEP = new HttpHeaderKeys("$WSEP");
    /** Private WAS header for PMI correlation */
    public static final HttpHeaderKeys HDR_PMIRM_CORRELATOR = new HttpHeaderKeys("rmcorrelator");
    /** Private WAS header for Partition_Version */
    public static final HttpHeaderKeys HDR_PARTITION_VERSION = new HttpHeaderKeys("_WS_HAPRT_WLMVERSION");
    /** Performance test header */
    public static final HttpHeaderKeys HDR_UA_CPU = new HttpHeaderKeys("UA-CPU");
    /** PMI/ARM header (@319194) */
    public static final HttpHeaderKeys HDR_ARM_CORRELATOR = new HttpHeaderKeys("ARM_CORRELATOR");
    /** 314871 - Private application-timeout header */
    public static final HttpHeaderKeys HDR_$WSATO = new HttpHeaderKeys("$WSATO");
    /** PK17960 - Original Content-Length header before auto-decompression */
    public static final HttpHeaderKeys HDR_$WSORIGCL = new HttpHeaderKeys("$WSORIGCL");
    /** 333093 - enumerated MIME-Version header */
    public static final HttpHeaderKeys HDR_MIME_VERSION = new HttpHeaderKeys("MIME-Version");
    /** 333093 - enumerated Content-Transfer-Encoding header */
    public static final HttpHeaderKeys HDR_CONTENT_TRANSFER_ENCODING = new HttpHeaderKeys("Content-Transfer-Encoding");
    /** 333093 - enumerated Content-ID header */
    public static final HttpHeaderKeys HDR_CONTENT_ID = new HttpHeaderKeys("Content-ID");
    /** 333093 - enumerated Content-Description */
    public static final HttpHeaderKeys HDR_CONTENT_DESCRIPTION = new HttpHeaderKeys("Content-Description");
    /** 352668.1 - add z/OS transaction ID header */
    public static final HttpHeaderKeys HDR_ZOS_TRAN_XID = new HttpHeaderKeys("ZOS_TRAN_XID");
    /** PK37608 - add header to suppress error page header */
    public static final HttpHeaderKeys HDR_$WSPC = new HttpHeaderKeys("$WSPC");
    /** XD related ODR header */
    public static final HttpHeaderKeys HDR_$WSODRINFO = new HttpHeaderKeys("$WSODRINFO");
    /** Cache related header */
    public static final HttpHeaderKeys HDR_EDGE_CONTROL = new HttpHeaderKeys("Edge-control");

    public static final HttpHeaderKeys HDR_FORWARDED = new HttpHeaderKeys("Forwarded");

    public static final HttpHeaderKeys HDR_X_FORWARDED_BY = new HttpHeaderKeys("X-Forwarded-By");

    public static final HttpHeaderKeys HDR_X_FORWARDED_FOR = new HttpHeaderKeys("X-Forwarded-For");

    public static final HttpHeaderKeys HDR_X_FORWARDED_HOST = new HttpHeaderKeys("X-Forwarded-Host");

    public static final HttpHeaderKeys HDR_X_FORWARDED_PORT = new HttpHeaderKeys("X-Forwarded-Port");
    /** De facto standard header for original protocol (similar to $WSIS) */
    public static final HttpHeaderKeys HDR_X_FORWARDED_PROTO = new HttpHeaderKeys("X-Forwarded-Proto");
    /** Private WAS header used by the HTTP Session Manager to determine if a request is failed over */
    public static final HttpHeaderKeys HDR_$WSFO = new HttpHeaderKeys("$WSFO");

    public static final HttpHeaderKeys HDR_HSTS = new HttpHeaderKeys("Strict-Transport-Security");

    public static final HttpHeaderKeys HDR_AUTHORIZATION_ENCODING = new HttpHeaderKeys("Authorization-Encoding");

    public static final HttpHeaderKeys HDR_ORIGIN = new HttpHeaderKeys("Origin");

    /** Max value of header keys that will be kept in key storage */
    public static final int ORD_MAX = 1024;

    /**
     * Constructor to create a new HttpHeaderKey and add it to the
     * enumerated list.
     *
     * @param name
     */
    private HttpHeaderKeys(String name) {
        super(name, generateNextOrdinal());
        if (NEXT_ORDINAL.get() <= ORD_MAX) {

            allKeys.add(this);
            myMatcher.add(this);
        }
    }

    /**
     * Constructor to create a new HttpHeaderKey and add it to the
     * enumerated list.
     *
     * @param name
     * @param undefined
     */
    private HttpHeaderKeys(String name, boolean undefined) {
        super(name, generateNextOrdinal());
        setUndefined(undefined);

        if (NEXT_ORDINAL.get() <= ORD_MAX) {

            allKeys.add(this);
            myMatcher.add(this);
        }
    }

    /**
     * Constructor for a new HTTP header key that allows overriding the debug
     * logging flag and the add/remove filtering code.
     *
     * @param name
     * @param shouldLog
     * @param shouldFilter
     */
    private HttpHeaderKeys(String name, boolean shouldLog, boolean shouldFilter) {
        super(name, generateNextOrdinal());
        super.setShouldLogValue(shouldLog);
        super.setUseFilters(shouldFilter);
        if (NEXT_ORDINAL.get() <= ORD_MAX) {

            allKeys.add(this);
            myMatcher.add(this);
        }
    }

    /**
     * @see com.ibm.wsspi.genericbnf.HeaderKeys#getEnumByOrdinal(int)
     */
    @Override
    public Object getEnumByOrdinal(int i) {
        return allKeys.get(i);
    }

    /**
     * Find the enumerated object that matchs the input name using the given
     * offset and length into that name. If none exist, then a null value is
     * returned.
     *
     * @param name
     * @param offset
     *                   - starting point in that name
     * @param length
     *                   - length to use from that starting point
     * @return HttpHeaderKeys
     */
    public static HttpHeaderKeys match(String name, int offset, int length) {
        if (null == name)
            return null;
        return (HttpHeaderKeys) myMatcher.match(name, offset, length);
    }

    /**
     * Find the enumerated object that matchs the input name using the given
     * offset and length into that name. If none exist, then a null value is
     * returned.
     *
     * @param name
     * @param offset
     *                   - starting point in that name
     * @param length
     *                   - length to use from that offset
     * @return HttpHeaderKeys
     */
    public static HttpHeaderKeys match(byte[] name, int offset, int length) {
        if (null == name)
            return null;
        return (HttpHeaderKeys) myMatcher.match(name, offset, length);
    }

    /**
     * Find the enumerated object matching the input name. If this name has
     * never been seen prior, then a new object is created by this call.
     *
     * @param name
     * @param offset
     *                                     - starting point in that input name
     * @param length
     *                                     - length to use from that offset
     * @param returnNullForInvalidName
     *                                     - return null instead of throw IllegalArgumentException for header name validation
     * @return HttpHeaderKeys
     * @throws NullPointerException
     *                                      if input name is null
     * @throws IllegalArgumentException
     *                                      if the input name contains invalid chars
     */
    public static HttpHeaderKeys find(byte[] name, int offset, int length, boolean returnNullForInvalidName) {
        HttpHeaderKeys key = (HttpHeaderKeys) myMatcher.match(name, offset, length);
        if (null == key) {
            synchronized (HttpHeaderKeys.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (HttpHeaderKeys) myMatcher.match(name, offset, length);
                if (null == key) {
                    String headerName = new String(name, offset, length);
                    // make sure the name is valid
                    if (validateHeaderName(headerName, returnNullForInvalidName)) {
                        key = new HttpHeaderKeys(headerName, true);
                    }
                }
            } // end-sync

        }
        return key;
    }

    /**
     * Find the enumerated object matching the input name. If this name has
     * never been seen prior, then a new object is created by this call.
     *
     * @param name
     * @param returnNullForInvalidName
     *                                     - return null instead of throw IllegalArgumentException for header name validation
     * @return HttpHeaderKeys
     * @throws NullPointerException
     *                                      if input name is null
     * @throws IllegalArgumentException
     *                                      if the input name contains invalid chars
     */
    public static HttpHeaderKeys find(String name, boolean returnNullForInvalidName) {
        HttpHeaderKeys key = (HttpHeaderKeys) myMatcher.match(name, 0, name.length());
        if (null == key) {
            synchronized (HttpHeaderKeys.class) {
                // protect against 2 threads getting here on the new value by
                // testing again inside a sync block
                key = (HttpHeaderKeys) myMatcher.match(name, 0, name.length());
                if (null == key) {
                    // make sure the name is valid
                    if (validateHeaderName(name, returnNullForInvalidName)) {
                        key = new HttpHeaderKeys(name, true);
                    }
                }
            } // end-sync
        }
        return key;
    }

    /*
     * A valid header name is "!" / "#" / "$" / "%" / "&" / "'" /
     * "*" / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~" / DIGIT / ALPHA
     *
     * The information about valid chars in a header name comes from
     * RCF 9110 section 5.6.2 tchars
     * https://www.rfc-editor.org/rfc/rfc9110.html#section-5.6.2
     *
     * PH52074
     */
    private static boolean validateHeaderName(String name, boolean returnFalseForInvalidName) {
        for (int i = 0, size = name.length(); i < size; i++) {
            char c = name.charAt(i);
            // if we found an error, throw the exception now
            if (!isValidTchar(c)) {
                if (returnFalseForInvalidName) {
                    return false;
                }
                IllegalArgumentException iae = new IllegalArgumentException("Header name contained an invalid character " + i);
                FFDCFilter.processException(iae, HttpHeaderKeys.class.getName() + ".validateHeaderName(String)", "1", name);
                throw iae;
            }
        }
        return true;
    }

    public static boolean isValidTchar(char c) {
        boolean valid = ((c >= 'a') && (c <= 'z')) ||
                        ((c >= 'A') && (c <= 'Z')) ||
                        ((c >= '0') && (c <= '9')) ||
                        (c == '!') || (c == '#') ||
                        (c == '$') || (c == '%') ||
                        (c == '&') || (c == '\'') ||
                        (c == '*') || (c == '+') ||
                        (c == '-') || (c == '.') ||
                        (c == '^') || (c == '_') ||
                        (c == '`') || (c == '|') ||
                        (c == '~');

        return valid;
    }

    /** private headers defined as sensitive */
    private static final HashSet<String> sensitiveHeaderList = new HashSet<String>(Arrays.asList(HDR_$WSCC.getName(), HDR_$WSRA.getName(), HDR_$WSRH.getName(),
                                                                                                 HDR_$WSAT.getName(), HDR_$WSRU.getName()));

    /**
     * @param headerName
     * @return true if headerName is considered to be a sensitive WAS private header
     */
    public static boolean isSensitivePrivateHeader(String headerName) {
        if (headerName == null) {
            return false;
        }
        return headerName.length() > 0 && headerName.charAt(0) == '$' && sensitiveHeaderList.contains(headerName);
    }

    /** private headers defined as sensitive */
    private static final HashSet<String> privateHeaderList = new HashSet<String>(Arrays.asList(HDR_$WSAT.getName(), HDR_$WSCC.getName(), HDR_$WSCS.getName(),
                                                                                               HDR_$WSIS.getName(), HDR_$WSSC.getName(), HDR_$WSPR.getName(), HDR_$WSRA.getName(),
                                                                                               HDR_$WSRH.getName(), HDR_$WSRU.getName(), HDR_$WSSN.getName(), HDR_$WSSP.getName(),
                                                                                               HDR_$WSSI.getName(), HDR_$WSZIP.getName(), HDR_$WSEP.getName(), HDR_$WSPT.getName(),
                                                                                               HDR_$WSATO.getName(), HDR_$WSORIGCL.getName(), HDR_$WSPC.getName(),
                                                                                               HDR_$WSODRINFO.getName(),
                                                                                               HDR_$WSFO.getName()));

    /**
     * @param headerName
     * @return true if headerName is a WAS private header
     */
    public static boolean isWasPrivateHeader(String headerName) {
        if (headerName == null) {
            return false;
        }
        return headerName.length() > 0 && headerName.charAt(0) == '$' && privateHeaderList.contains(headerName);
    }

    private static int generateNextOrdinal() {
        synchronized (HttpHeaderKeys.class) {
            if (Integer.MAX_VALUE == NEXT_ORDINAL.get()) {
                NEXT_ORDINAL.set(ORD_MAX);

            }
            return NEXT_ORDINAL.getAndIncrement();

        }
    }
}
