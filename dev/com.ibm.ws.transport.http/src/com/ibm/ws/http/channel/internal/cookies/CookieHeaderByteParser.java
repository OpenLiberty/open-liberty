/*******************************************************************************
 * Copyright (c) 2004, 2026 IBM Corporation and others.
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
    private boolean isRequestCookie = false;

    /*
     * Servlet 6.1 (EE11)
     * Request Cookie:
     * 1. semicolon (;) is the only delimiter. cookie-pair following comma (, commaName=commaValue) is rejected
     *    cookie-string = cookie-pair *( ";" SP cookie-pair )
     * 2. Cookie value: Double and Sing Quotes are part of value
     * 3. Cookie name cannot have any double quotes (reject); Single quote is part of name
     * 
     * Response Set-Cookie: 
     * 1. response addHeader/setHeader will not split the Set-Cookie header for arbitrary attributes (part after ; )
     * 2. setAttribute with empty value - only show attribute name itself; example : setAttribute("JustName", "") or setAttribute("JustName", "=") > JustName;
     * 3. setAttribute with null value - will remove that attribute
     * 4. Cookie Vale: Double and Single Quotes are part of value
     * 5. Cookie name : No Double quotes anywhere (reject); Single quote is part of name
     * 6. Comma in Set-Cookie will discard the entire cookie; unless comma is after semicolon ( likely ; Expires)
     */
    private boolean isEE11;
    private boolean hasDollarSign = false;

    public CookieHeaderByteParser() {
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
        this.isRequestCookie = "Cookie".equalsIgnoreCase(cookieHeader.getName()) ? true : false; //Cookie: request; Set-Cookie: response

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "parse ENTRY [" + GenericUtils.nullOutPasswords(headerValue, (byte) '&') + "] " + cookieHeader);
            Tr.debug(tc, (isRequestCookie ? "Request Cookie" : "Response Set-Cookie") + ", EE11 [" + isEE11 + "] , cookie length [" + headerValue.length + "]");
        }
       
        /*
         * Early validation for Response Set-Cookie: One per cookie
         * Invalid:     
         * Set-Cookie: name1=value1, name2=value2"
         * Set-Cookie: name1=value1, name2=value2; Path=/
         *    
         * Valid: 
         * Set-Cookie: name1=value1; Expires=Thu, 01 Jan 2026 00:00:00 GMT
         */
        if (this.isEE11 && !this.isRequestCookie) {
            // Any comma before semicolon will discard this Set-Cookie.
            boolean foundComma = false;
            for (int i = 0; i < headerValue.length; i++) {
                if (';' == headerValue[i]) {
                    break;
                }
                if (',' == headerValue[i]) {
                    foundComma = true;
                    break;
                }
            }
            
            if (foundComma) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "parse, response Set-Cookie contains invalid comma. Ignore entire cookie");
                }

                return cookiesList; //empty list
            }
        }

        // keep looping through pulling individual cookies or cookie attributes
        // until we run out of input data
        while (this.bytePosition < headerValue.length) {
            // parse out the cookie name or type, then get the value
            token = matchAndParse(headerValue, cookieHeader);
            
            //Skip if this.name is null (when the cookie name has invalid double quotes character (EE11).)
            if (token != null || this.name != null) {
                parseValue(headerValue, token);
            }

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
                     * Request Cookie is processed the same in all versions up to 6.0
                     * Request Cookie 6.1 ignores comma delimiter in cookie-pair
                     * 
                     * Response Set-Cookie is processed differently
                     */
                    if (this.isRequestCookie || !isEE11) { // All Incoming requests <= 6.0 or Servlet 6.0 responses
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
     *         header, otherwise it returns null.
     *         
     *         The parsed name is up to, but excluding, the '=' before calling parseValue().
     *         In EE11, double quote name will skip the parseValue() until the 
     *         next semicolon or end of data; then matchAndParse() resuming for next pair
     */
    private CookieData matchAndParse(byte[] data, HeaderKeys hdr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, " matchAndParse ENTRY" + " HeaderKeys [" + hdr + "] , start index: " + this.bytePosition);
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
            // Set-Cookie is comma separated (pre EE11)
            if (';' == b) {
                if (-1 == start) {
                    // just ignore this empty bit (ie. ";;version=1")
                    continue;
                }
                // decrement the position so that the parse cookie value code
                // will notice the missing value (by seeing semi-colon first)
                pos--;
                break;
            } 
            
            if (',' == b) {
                if (this.isEE11 && this.isRequestCookie) {
                    // EE11 request Cookie: comma is invalid, skip this cookie-pair until the next ; or end of data
                    // Example: "name1=value1; bad1=val1, bad2=val2; name3=value3"
                    //   -> Skip "bad1=val1, bad2=val2", continue with "name3=value3"
                    
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Request cookie has invalid comma at index: " + pos + " . Ignore cookie name [" + GenericUtils.nullOutPasswords(this.name, (byte) '&') +"]");
                    }

                    for (pos++; pos < data.length; pos++) {
                        if (';' == data[pos]) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, " matchAndParse: request cookie skipping to the next ; at index: " + pos);
                            }
                            // Found semicolon, position after it and return null to skip this cookie-pair
                            this.bytePosition = pos + 1;
                            return null;
                        }
                    }
                    
                    this.bytePosition = data.length;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, " matchAndParse: request cookie reaches end of data");
                    }

                    return null;
                }
                
                // Pre-EE11 comma acts as delimiter. EE11 response Set-Cookie already validated earlier in parse()
                if (-1 == start) {
                    // just ignore this empty bit
                    continue;
                }
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
            /*
             * case =value; delimiter = is found but start is still -1;
             * skip to next semicolon or end of data
             */
            if (pos < data.length && '=' == data[pos]) {
                int skip = this.bytePosition; // already past '='
                while (skip < data.length && ';' != data[skip]) {
                    skip++;
                }
                this.bytePosition = (skip < data.length) ? skip + 1 : data.length;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, " matchAndParse: empty cookie name before '=' ; Ignore this pair");
                }
            }
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
        }  
        else if (this.isEE11) {
            // EE11: cookie names must not contain any double-quote characters - Both Cookie and Set-Cookie
            for (int i = start; i <= stop; i++) {
                if ('"' == data[i]) {
                    //skip pass the "=value" to the next ; or end of data.
                    int skip = this.bytePosition;
                    while (skip < data.length && ';' != data[skip]) {
                        skip++;
                    }
                    // Position past the semicolon so the main loop starts at the next cookie pair
                    this.bytePosition = (skip < data.length) ? skip + 1 : data.length;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.exit(tc, " matchAndParse EXIT cookie name has double-quote. Skipping parseValue to the next ; at [" + bytePosition + "]");
                    }
                    return null;
                }
            }
        } 
        else if ('"' == data[start] && '"' == data[stop]) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.exit(tc, " matchAndParse removing of surrounding DQuotes in name");
            }
            // pre-EE11: quotes around the name, strip them off
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
                Tr.debug(tc, " matchAndParse , token name [" + token.getName() + "] , foundDollar [" + foundDollar + "]");
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
                            Tr.debug(tc, " matchAndParse", " dollar version ");
                        }

                        if (!token.validForHeader(hdr, foundDollar)) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, " Token not valid for header, " + hdr + " " + token);
                            }
                            token = null;
                        }
                    } else { // $ANY is a new cookie
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, " matchAndParse", " dollar " + cName + " , token [" + token + "]");
                        }
                        token = null;
                    }
                } else { // not foundDollar
                         // test whether what we believe to be a token is a valid attribute
                         // for this header instance. If not, then treat it as a new cookie
                         // name
                    if (!token.validForHeader(hdr, foundDollar)) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, " Token not valid for header, " + hdr + " " + token);
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
                        Tr.debug(tc, " Token not valid for header, " + hdr + " " + token);
                    }
                    token = null;
                }
            }
        }

        if (null == token) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " matchAndParse, token is null ; foundDollar [" + foundDollar + "]");
            }

            // New cookie name found
            if (foundDollar && this.useEE10Cookies) { //Servlet 6.0 : $ is part of the name, so put it back and adjust the len
                start--;
                len++;
            }
            this.name = new byte[len];
            System.arraycopy(data, start, this.name, 0, len);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " matchAndParse, cookie name: [" + GenericUtils.getEnglishString(this.name) + "]");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, " matchAndParse EXIT" + " token [" + token + "]");
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
     *                  
     * parseValue starts position is after '=' of the cookie=pair
     */
    private void parseValue(byte[] data, CookieData token) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, " parseValue ENTRY , start index: " + this.bytePosition);
        }

        int start = -1;
        int stop = -1;
        int pos = this.bytePosition; 
        int num_quotes = 0;

        // cycle through each byte until we hit a delimiter or end of data
        for (; pos < data.length; pos++) {
            byte b = data[pos];

            if (';' == b) {
                break;
            }

            if ('"' == b) {
                num_quotes++;
            }

            /*
             * EE11 request Cookie - Skip invalid cookie-pair with comma ended to the next semicolon (or end of data), break out to parse() then matchAndParse() for next pair
             * Response Set-Cookie - already validate early at the start of parse(). If it gets here, it is part of the Expires 
             *  
             * Commas should not be treated as delimiters when they are part of the Set-Cookie Expires attribute
             */
            if (',' == b) {
                if (this.isEE11 && this.isRequestCookie) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Request cookie value has invalid comma at index: " + pos + ". Ignore cookie name [" + GenericUtils.nullOutPasswords(this.name, (byte) '&') + "]");
                    }

                    this.name = null;
                    this.value = null;
                    for (pos++; pos < data.length; pos++) {
                        if (';' == data[pos]) {
                            this.bytePosition = pos + 1;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, " parseValue: request cookie - skips and resumes parsing after next ; at index: " + pos);
                            }
                            break;
                        }
                    }
                    if (pos >= data.length) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, " parseValue, request cookie reached end of data.");
                        }
                        this.bytePosition = data.length;
                    }
                    return;
                } else if (CookieData.cookieExpires.equals(token)) {
                    // Comma is part of the Expires date format (e.g., "Thu, 01 Jan 2026 00:00:00 GMT")
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, " parseValue: comma is part of Expires attribute, pos: " + pos);
                    }
                } else if (CookieData.cookiePort.equals(token)) {
                    // Port="80,8080" is valid
                    if (2 <= num_quotes) {
                        // this comma is after the quoted port string
                        break;
                    }
                } else {
                    // For all other cases (Pre-EE11 only, since EE11 is already validated)
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
        // Servlet 6.1 - keeps all quotes 
        if (!isEE11 && '"' == data[start] && '"' == data[stop]) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " parseValue: removing of surrounding DQuotes in value ");
            }
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
                    Tr.exit(tc, " parseValue EXIT, cookie value: [" + GenericUtils.nullOutPasswords(this.value, (byte) '&') + "]");
                }
            }
        }
    }
}
