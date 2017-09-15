/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
     *            The byte array to be parsed into cookies.
     * @param cookieHeader
     *            the header this cookie represents.
     * @return a list of <code>Cookie</code> objects parsed from the headerValue.
     * @throws IllegalArgumentException
     *             if headerValue is NULL.
     */
    public List<HttpCookie> parse(byte[] headerValue, HeaderKeys cookieHeader) throws IllegalArgumentException {
        if (null == headerValue) {
            throw new IllegalArgumentException("Null input");
        }

        // initialize the member variables
        this.name = null;
        this.value = null;
        this.bytePosition = 0;

        // initialize the local variables
        CookieData token = null;
        HttpCookie cookie = null;
        List<HttpCookie> cookiesList = new LinkedList<HttpCookie>();
        int version = 0;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "parse [" + GenericUtils.nullOutPasswords(headerValue, (byte) '&') + "] " + cookieHeader);
        }

        // keep looping through pulling individual cookies or cookie attributes
        // until we run out of input data

        while (this.bytePosition < headerValue.length) {

            // parse out the cookie name or type, then get the value
            token = matchAndParse(headerValue, cookieHeader);
            parseValue(headerValue, token);
            if (null == token) {
                // parsed name may not exist yet
                if (null != this.name && 0 != this.name.length) {
                    // Create an instance of the cookie
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Creating cookie, version " + version);
                    }
                    try {
                        cookie = new HttpCookie(GenericUtils.getEnglishString(this.name), GenericUtils.getEnglishString(this.value));
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

        } // end - while have data to parse

        return cookiesList;
    }

    /**
     * This method matches the cookie attribute header with the pre-established
     * Cookie header types. If a match is established the appropriate Cookie
     * header data type is returned.
     * 
     * @param data
     *            The header-value byte array passed down by parse
     * @param hdr
     * @return The appropriate CookieData type if a match is found for the
     *         header, otherwise it returns null
     */
    private CookieData matchAndParse(byte[] data, HeaderKeys hdr) {

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
        if (null == token) {
            // New cookie name found
            this.name = new byte[len];
            System.arraycopy(data, start, this.name, 0, len);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "name: " + GenericUtils.getEnglishString(this.name));
            }
        }

        return token;
    }

    /**
     * This method parses the cookie attribute value.
     * 
     * @param data
     *            The value byte array passed down by parse method
     * @param token
     *            The type of the CookieData attribute
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
        if ('"' == data[start] && '"' == data[stop]) {
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
                    Tr.debug(tc, "value: " + GenericUtils.nullOutPasswords(this.value, (byte) '&'));
                }
            }
        }
    }

}
