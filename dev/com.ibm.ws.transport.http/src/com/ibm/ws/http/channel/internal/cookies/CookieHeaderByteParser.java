/*******************************************************************************
 * Copyright (c) 2004, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.cookies;

import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.values.CookieData;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.HttpCookie;

/**
 * <code>CookieHeaderByteParser</code> serves as a centralized location for
 * parsing HTTP Cookies.
 *
 */
public class CookieHeaderByteParser {

    /** RAS debugging variable */
    private static final TraceComponent tc = Tr.register(CookieHeaderByteParser.class,
                                                         HttpMessages.HTTP_TRACE_NAME,
                                                         HttpMessages.HTTP_BUNDLE);

    /** The current position in the header value */
    private int bytePosition = 0;
    /** The parsed name of a cookie name/value pair */
    private byte[] name;
    /** The parsed value of a cookie name/value pair */
    private byte[] value;

    //Servlet 6.0
    private boolean useEE10Cookies;

    /*
     * Servlet 6.1 (EE11)
     * Response Set-Cookie behaviors (no change in request Cookie)
     * 1. response addHeader/setHeader will not split the Set-Cookie header for arbitrary attributes
     * 2. setAttribute with empty value - only show attribute name itself; example : setAttribute("JustName", "") or setAttribute("JustName", "=") > JustName;
     * 3. setAttribute with null value - will remove that attribute
     * 4. surrounding quotes are part of cookie's value
     */
    private boolean isEE11;
    private boolean hasDollarSign = false;

    /**
     * Constructor for this class.
     */
    public CookieHeaderByteParser() {
        // nothing extra to do
    }

    /**
     * Parses the specified cookie header value byte array into
     * <code>Cookie</code> objects.
     *
     * @param headerValue
     *                         The byte array to be parsed into cookies.
     * @param cookieHeader
     *                         the header this cookie represents.
     * @return a list of <code>Cookie</code> objects parsed from the headerValue.
     * @throws IllegalArgumentException
     *                                      if headerValue is NULL.
     */
    public List<HttpCookie> parse(byte[] headerValue, HeaderKeys cookieHeader) throws IllegalArgumentException {
        if (null == headerValue) {
            throw new IllegalArgumentException("Null input");
        }
        // initialize the member variables
        this.name = null;
        this.value = null;
        this.bytePosition = 0;
        this.useEE10Cookies = HttpDispatcher.useEE10Cookies();

        // initialize the local variables
        CookieData token = null;
        HttpCookie cookie = null;
        List<HttpCookie> cookiesList = new LinkedList<HttpCookie>();
        int version = 0;

        //Servlet 6.1
        this.isEE11 = HttpDispatcher.isEE11();
        String cName = null;
        String cValue = null;
        hasDollarSign = false;
        boolean isRequestCookie = cookieHeader.getName().equalsIgnoreCase("Cookie") ? true : false; //Cookie: request; Set-Cookie: response

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "parse ENTRY [" + GenericUtils.nullOutPasswords(headerValue, (byte) '&') + "] " + cookieHeader);
            Tr.debug(tc, "Request Cookie [" + isRequestCookie + "] , EE11 [" + isEE11 + "]");
        }

        // keep looping through pulling individual cookies or cookie attributes
        // until we run out of input data
        while (this.bytePosition < headerValue.length) {
            // parse out the cookie name or type, then get the value
            token = matchAndParse(headerValue, cookieHeader);
            parseValue(headerValue, token);

            cName = GenericUtils.getEnglishString(this.name);
            cValue = GenericUtils.getEnglishString(this.value);

            /*
             * matchAndParse() determines that the token is null - means this is not a pre-established cookie header types (i.e HttOnly, Secure, SameSite ...)
             *
             * Example: Consider this response cookieHeader [Cookie_viaAddHeader=CookieValue_viaAddHeader; Secure; SameSite=None; randomAttributeB=myAttValueB]
             * (token is determined using the name part of each pair i.e Cookie_viaAddHeader, Secure, SameSite, randomAttributeB)
             *
             * The parsed output trace below will show something similar to:
             * parsed token [null] , name [Cookie_viaAddHeader] , value [CookieValue_viaAddHeader] // -> new response Set-Cookie
             * parsed token [Key: secure Ordinal: 4] , name [null] , value []
             * parsed token [Key: samesite Ordinal: 11] , name [null] , value [None]
             *
             * parsed token [null] , name [randomAttributeB] , value [myAttValueB] // -> new response Set-Cookie (in 6.0)
             *
             * Response (i.e Set-Cookie header):
             * - In 6.0, every unrecognized token (i.e token == null) will result in a new response Set-Cookie header (this caused the split header behavior in 6.0 when using
             * addHeader or setHeader)
             *
             * - In 6.1, unrecognized token (i.e token == null) will NOT result in new Set-Cookie header for THIS SET. Instead, it is treated as an attribute of this Set-Cookie.
             *
             * Request (i.e Cookie header) - No attribute is accepted as per new RFC in 6.0; no $ is allowed Except for Version;
             * attribute will be treated as a new Cookie header ($ is part of the header value)
             *
             * Example: Request Cookie header : [$Version=1; name1=value1; $Path=/Dollar_Path; $Domain=localhost; $NAME2=DollarNameValue; Domain=DomainValue] Key: Cookie
             * Results in multiple request Cookie headers:
             * [name1=value1]
             * [$Path=/Dollar_Path]
             * [$Domain=localhost]
             * [$NAME2=DollarNameValue]
             * [Domain=DomainValue]
             *
             */
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "parsed token [" + token + "] , name [" + cName + "] , value [" + cValue + "] , hasDollarSign ["
                             + this.hasDollarSign + "] , cookiesList [" + cookiesList.size() + "]");
            }

            if (null == token) {
                // parsed name may not exist yet
                if (null != this.name && 0 != this.name.length) {
                    /*
                     * Request Cookie is processed the same in all versions 6.0 and above
                     * Output Response Set-Cookie is processed differently
                     */
                    if (isRequestCookie || !isEE11) { // All Incoming requests or Servlet 6.0 responses
                        // Create an instance of the cookie
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Creating cookie, version " + version);
                        }
                        try {
                            cookie = new HttpCookie(cName, cValue);
                        } catch (IllegalArgumentException iae) {
                            // no FFDC required
                            // Broken cookie name due to invalid characters
                            this.name = null;
                            this.value = null;
                            continue;
                        }
                        cookie.setVersion(version);
                        cookiesList.add(cookie);
                    }
                    //Servlet 6.1 response outgoing
                    else {
                        if (cookiesList.size() == 0) { //only the first time to create an instance of the cookie for EACH Set-Cookie
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, " 6.1 - Creating cookie, version " + version);
                            }
                            try {
                                cookie = new HttpCookie(cName, cValue);
                            } catch (IllegalArgumentException iae) {
                                // no FFDC required
                                // Broken cookie name due to invalid characters
                                this.name = null;
                                this.value = null;
                                continue;
                            }
                            cookie.setVersion(version);
                            cookiesList.add(cookie);
                        } else {
                            /*
                             * arbitrary attribute .i.e not the well known HttpOnly, SameSite, Partitioned ....
                             */
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "parse ; arbitrary setAttribute , name [" + cName + "] , value [" + cValue + "]");
                            }

                            cookie.setAttribute(cName, cValue);
                        }
                    }
                }
            } else if (null != this.value) {
                // version is a special cookie value in that it might
                // be at the front of the line and does not apply to just
                // one cookie instance
                if (CookieData.cookieVersion.equals(token)) {
                    try {
                        version = GenericUtils.asIntValue(this.value);
                        if (null != cookie) {
                            cookie.setVersion(version);
                        }
                    } catch (NumberFormatException ne) {
                        FFDCFilter.processException(ne, getClass().getName() + ".parse", "166");
                        version = 0; // set back to default
                    }
                } else if (null != cookie) {
                    token.set(cookie, this.value);
                }
            }
            // reset for next parsing pass
            token = null;
            this.name = null;
            this.value = null;
            this.hasDollarSign = false;

        } // end - while have data to parse

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "parse EXIT , cookiesList [" + cookiesList + "]");
        }

        return cookiesList;
    }

    /**
     * This method matches the cookie attribute header with the pre-established
     * Cookie header types. If a match is established the appropriate Cookie
     * header data type is returned.
     *
     * @param data
     *                 The header-value byte array passed down by parse
     * @param hdr
     * @return The appropriate CookieData type if a match is found for the
     *         header, otherwise it returns null
     */
    private CookieData matchAndParse(byte[] data, HeaderKeys hdr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "matchAndParse ENTRY" + " HeaderKeys [" + hdr + "]");
        }

        int pos = this.bytePosition;
        int start = -1;
        int stop = -1;

        for (; pos < data.length; pos++) {
            byte b = data[pos];

            // found the delimiter for the name */
            if ('=' == b) {
                break;
            }

            // In case of headers like MyNullCookie;
            // Set-Cookie is comma separated
            if (';' == b || ',' == b) {
                if (-1 == start) {
                    // just ignore this empty bit (ie. ";;version=1")
                    continue;
                }
                // decrement the position so that the parse cookie value code
                // will notice the missing value (by seeing semi-colon first)
                pos--;
                break;
            }

            // ignore white space
            if (' ' != b && '\t' != b) {
                if (-1 == start) {
                    start = pos;
                }
                stop = pos;
            }
        }

        // save our stopping point (past the delimiter)
        this.bytePosition = pos + 1;

        if (-1 == start) {
            // nothing was found
            return null;
        }
        if (-1 == stop) {
            // shouldn't be possible
            stop = pos;
        } else if (data.length == stop) {
            stop--;
        }

        boolean foundDollar = ('$' == data[start]);

        if (foundDollar) {
            hasDollarSign = true;
            // skip past the leading $ symbol
            start++;
        } else if ('"' == data[start] && '"' == data[stop]) {
            // quotes around the values, strip them off
            start++;
            stop--;
        }
        int len = stop - start + 1;
        if (0 >= len) {
            // invalid data
            return null;
        }
        CookieData token = CookieData.match(data, start, len);
        if (null != token && null != hdr) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "matchAndParse , token name [" + token.getName() + "] , foundDollar [" + foundDollar + "]");
            }

            /*
             * Since Servlet 6.0 (EE10):
             * Follows RFC 6265.
             * Attributes are no longer accepted from the request Cookie header (section 4.2.2 of RFC)
             * $ is used only for $Versions in the request Cookie;
             * $ prefix any other will be treated as new cookie ($ is part of a cookie name)
             */
            if (this.useEE10Cookies) {
                if (foundDollar) {
                    String cName = token.getName();
                    if (cName.equalsIgnoreCase("version")) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "matchAndParse", " dollar version ");
                        }

                        if (!token.validForHeader(hdr, foundDollar)) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Token not valid for header, " + hdr + " " + token);
                            }
                            token = null;
                        }
                    } else { // $ANY is a new cookie
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "matchAndParse", " dollar " + cName + " , token [" + token + "]");
                        }
                        token = null;
                    }
                } else { // not foundDollar
                         // test whether what we believe to be a token is a valid attribute
                         // for this header instance. If not, then treat it as a new cookie
                         // name
                    if (!token.validForHeader(hdr, foundDollar)) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Token not valid for header, " + hdr + " " + token);
                        }
                        token = null;
                    }
                }
            } else { // prior to Servlet 6.0 path
                // test whether what we believe to be a token is a valid attribute
                // for this header instance. If not, then treat it as a new cookie
                // name
                if (!token.validForHeader(hdr, foundDollar)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Token not valid for header, " + hdr + " " + token);
                    }
                    token = null;
                }
            }
        }

        if (null == token) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "matchAndParse , token is null ; foundDollar [" + foundDollar + "]");
            }

            // New cookie name found
            if (foundDollar && this.useEE10Cookies) { //Servlet 6.0 : $ is part of the name, so put it back and adjust the len
                start--;
                len++;
            }
            this.name = new byte[len];
            System.arraycopy(data, start, this.name, 0, len);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "name: " + GenericUtils.getEnglishString(this.name));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "matchAndParse EXIT" + " token [" + token + "]");
        }

        return token;
    }

    /**
     * This method parses the cookie attribute value.
     *
     * @param data
     *                  The value byte array passed down by parse method
     * @param token
     *                  The type of the CookieData attribute
     */
    private void parseValue(byte[] data, CookieData token) {
        int start = -1;
        int stop = -1;
        int pos = this.bytePosition;
        int num_quotes = 0;

        // cycle through each byte until we hit a delimiter or end of data
        for (; pos < data.length; pos++) {
            byte b = data[pos];

            // check for delimiter
            if (';' == b) {
                break;
            }

            // check for quotes
            if ('"' == b) {
                num_quotes++;
            }

            // Commas should not be treated as delimiters when they are
            // part of the Expires attribute
            if (',' == b) {
                // Port="80,8080" is valid
                if (CookieData.cookiePort.equals(token)) {
                    if (2 <= num_quotes) {
                        // this comma is after the quoted port string
                        break;
                    }
                } else if (!CookieData.cookieExpires.equals(token)) {
                    break;
                }
            }

            // ignore white space
            if (' ' != b && '\t' != b) {
                if (-1 == start) {
                    start = pos;
                }
                stop = pos;
            }
        }

        // save where we stopped
        this.bytePosition = pos + 1;

        // check the output parameters
        if (-1 == start) {
            this.value = new byte[0];
            return;
        }
        if (-1 == stop) {
            this.value = new byte[0];
            return;
        }

        // filter out any surrounding quotes
        // Servlet 6.1 - surrounding quotes are part of cookie value
        if (!isEE11 && '"' == data[start] && '"' == data[stop]) {
            start++;
            stop--;
        }

        // Retrieve the cookie attribute value
        int len = stop - start + 1;
        if (0 <= len) {
            this.value = new byte[len];
            if (0 < len) {
                System.arraycopy(data, start, this.value, 0, len);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, " parseValue, value: [" + GenericUtils.nullOutPasswords(this.value, (byte) '&') + "]");
                }
            }
        }
    }
}
