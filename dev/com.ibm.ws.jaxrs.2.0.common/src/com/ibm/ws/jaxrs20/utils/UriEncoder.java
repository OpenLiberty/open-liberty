/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs20.utils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * URI Encoding and Decoding
 */
public final class UriEncoder {

    private static final Charset CHARSET_UTF_8 = StandardCharsets.UTF_8;

    private UriEncoder() {
        // no instances
    }

    /** Hexadecimal digits for escaping. */
    private static final char[]    hexDigits           =
                                                           {'0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'             };

    private static final byte[]    normalizedHexDigits = new byte[128];

    private static final boolean[] isHexDigit          = new boolean[128];

    /**
     * Unreserved characters according to RFC 3986. Each character below ASCII
     * 128 has single array item with true if it is unreserved and false if it
     * is reserved.
     */
    private static final boolean[]  unreservedChars     = new boolean[128];
    private static final boolean[]  userInfoChars       = new boolean[128];
    private static final boolean[]  segmentChars        = new boolean[128];
    private static final boolean[]  matrixChars         = new boolean[128];
    private static final boolean[]  pathChars           = new boolean[128];
    private static final boolean[]  queryChars          = new boolean[128];
    private static final boolean[]  queryParamChars     = new boolean[128];
    private static final boolean[]  fragmentChars       = new boolean[128];
    private static final boolean[]  uriChars            = new boolean[128];
    private static final boolean[]  uriTemplateChars    = new boolean[128];

    static {
        // unreserved - ALPHA / DIGIT / "-" / "." / "_" / "~"
        Arrays.fill(unreservedChars, false);
        Arrays.fill(unreservedChars, 'a', 'z' + 1, true);
        Arrays.fill(unreservedChars, 'A', 'Z' + 1, true);
        Arrays.fill(unreservedChars, '0', '9' + 1, true);
        unreservedChars['-'] = true;
        unreservedChars['_'] = true;
        unreservedChars['.'] = true;
        unreservedChars['~'] = true;

        // sub delimiters - "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / ","
        // / ";" / "="
        // user info chars - *( unreserved / pct-encoded / sub-delims / ":" )
        System.arraycopy(unreservedChars, 0, userInfoChars, 0, 128);
        userInfoChars['!'] = true;
        userInfoChars['$'] = true;
        userInfoChars['&'] = true;
        userInfoChars['\''] = true;
        userInfoChars['('] = true;
        userInfoChars[')'] = true;
        userInfoChars['*'] = true;
        userInfoChars['+'] = true;
        userInfoChars[','] = true;
        userInfoChars[';'] = true;
        userInfoChars['='] = true;
        userInfoChars[':'] = true;

        // segment - *(unreserved / pct-encoded / sub-delims / ":" / "@")
        System.arraycopy(userInfoChars, 0, segmentChars, 0, 128);
        segmentChars['@'] = true;

        // matrix - *(unreserved / pct-encoded / sub-delims / ":" / "@") without
        // "=" and ";"
        System.arraycopy(segmentChars, 0, matrixChars, 0, 128);
        matrixChars['='] = false;
        matrixChars[';'] = false;

        // path - *(unreserved / pct-encoded / sub-delims / ":" / "@" / "/")
        System.arraycopy(segmentChars, 0, pathChars, 0, 128);
        pathChars['/'] = true;

        // query - *(unreserved / pct-encoded / sub-delims / ":" / "@" / "/" /
        // "?")
        System.arraycopy(pathChars, 0, queryChars, 0, 128);
        queryChars['?'] = true;

        // fragment - *(unreserved / pct-encoded / sub-delims / ":" / "@" / "/"
        // / "?")
        System.arraycopy(queryChars, 0, fragmentChars, 0, 128);

        // query param - *(unreserved / pct-encoded / sub-delims / ":" / "@" /
        // "/" / "?") without
        // "&" and "="
        System.arraycopy(queryChars, 0, queryParamChars, 0, 128);
        queryParamChars['&'] = false;
        queryParamChars['='] = false;

        // uri - *(unreserved / pct-encoded / sub-delims / ":" / "@" / "/" / "?"
        // / "#" / "[" / "]" )
        System.arraycopy(queryChars, 0, uriChars, 0, 128);
        uriChars['#'] = true;
        uriChars['['] = true;
        uriChars[']'] = true;

        // uri template - *(unreserved / pct-encoded / sub-delims / ":" / "@" /
        // "/" / "?" / "#" /
        // "[" / "]" / "{" / "}" )
        System.arraycopy(uriChars, 0, uriTemplateChars, 0, 128);
        uriTemplateChars['{'] = true;
        uriTemplateChars['}'] = true;

        // fill the isHex array
        Arrays.fill(isHexDigit, false);
        Arrays.fill(isHexDigit, '0', '9' + 1, true);
        Arrays.fill(isHexDigit, 'a', 'f' + 1, true);
        Arrays.fill(isHexDigit, 'A', 'F' + 1, true);

        // fill the normalizedHexDigits array
        normalizedHexDigits['0'] = '0';
        normalizedHexDigits['1'] = '1';
        normalizedHexDigits['2'] = '2';
        normalizedHexDigits['3'] = '3';
        normalizedHexDigits['4'] = '4';
        normalizedHexDigits['5'] = '5';
        normalizedHexDigits['6'] = '6';
        normalizedHexDigits['7'] = '7';
        normalizedHexDigits['8'] = '8';
        normalizedHexDigits['9'] = '9';
        normalizedHexDigits['A'] = 'A';
        normalizedHexDigits['B'] = 'B';
        normalizedHexDigits['C'] = 'C';
        normalizedHexDigits['D'] = 'D';
        normalizedHexDigits['E'] = 'E';
        normalizedHexDigits['F'] = 'F';
        normalizedHexDigits['a'] = 'A';
        normalizedHexDigits['b'] = 'B';
        normalizedHexDigits['c'] = 'C';
        normalizedHexDigits['d'] = 'D';
        normalizedHexDigits['e'] = 'E';
        normalizedHexDigits['f'] = 'F';

    }

    private static int decodeHexDigit(char c) {

        // Decode single hexadecimal digit. On error returns 0 (ignores errors).
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        } else {
            return 0;
        }
    }

    /**
     * Encode all characters other than unreserved according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param string string to encode
     * @return encoded US-ASCII string
     */
    public static String encodeString(String string) {
        return encode(string, false, unreservedChars);
    }

    /**
     * Encode user info according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param userInfo the user info to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded user info string
     */
    public static String encodeUserInfo(String userInfo, boolean relax) {
        return encode(userInfo, relax, userInfoChars);
    }

    /**
     * Encode a path segment (without matrix parameters) according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param segment the segment (without matrix parameters) to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded segment string
     */
    public static String encodePathSegment(String segment, boolean relax) {
        return encode(segment, relax, segmentChars);
    }

    /**
     * Encode a matrix parameter (name or value) according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param matrix the matrix parameter (name or value) to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded matrix string
     */
    public static String encodeMatrix(String matrix, boolean relax) {
        return encode(matrix, relax, matrixChars);
    }

    /**
     * Encode a complete path string according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param path the path string to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded path string
     */
    public static String encodePath(String path, boolean relax) {
        return encode(path, relax, pathChars);
    }

    /**
     * Encode a query parameter (name or value) according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param queryParam the query parameter string to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded query parameter string
     */
    public static String encodeQueryParam(String queryParam, boolean relax) {
        boolean[] unreserved = queryParamChars;
        String string = queryParam;

        if (queryParam == null) {
            return null;
        }

        if (!needsEncoding(queryParam, false, unreserved)) {
            return string;
        }

        // Encode to UTF-8
        ByteBuffer buffer = CHARSET_UTF_8.encode(string);
        // Prepare string buffer
        StringBuilder sb = new StringBuilder(buffer.remaining());
        // Now encode the characters
        while (buffer.hasRemaining()) {
            int c = buffer.get();

            if ((c == '%') && relax && (buffer.remaining() >= 2)) {
                int position = buffer.position();
                if (isHex(buffer.get(position)) && isHex(buffer.get(position + 1))) {
                    sb.append((char)c);
                    continue;
                }
            }

            if ((c >= ' ' && unreserved[c])) {
                sb.append((char)c);
            } else if ((c == ' ')) {
                sb.append('+');
            } else {
                sb.append('%');
                sb.append(hexDigits[(c & 0xf0) >> 4]);
                sb.append(hexDigits[c & 0xf]);
            }
        }

        return sb.toString();
    }

    /**
     * Encode a complete query string according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param query the query string to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded query string
     */
    public static String encodeQuery(String query, boolean relax) {
        return encode(query, relax, queryChars);
    }

    /**
     * Encode a fragment string according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param fragment the fragment string to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded fragment string
     */
    public static String encodeFragment(String fragment, boolean relax) {
        return encode(fragment, relax, fragmentChars);
    }

    /**
     * Encode a uri according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>, escaping all
     * reserved characters.
     * 
     * @param uri string to encode
     * @param relax if true, then any sequence of chars in the input of the form
     *            '%XX', where XX are two HEX digits, will not be encoded.
     * @return encoded US-ASCII string
     */
    public static String encodeUri(String uri, boolean relax) {
        return encode(uri, relax, uriChars);
    }

    /**
     * Encode a uri template according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>, escaping all
     * reserved characters, except for '{' and '}'.
     * 
     * @param uriTemplate template to encode
     * @param relax if true, then any sequence of chars in the input of the form
     *            '%XX', where XX are two HEX digits, will not be encoded.
     * @return encoded US-ASCII string
     */
    public static String encodeUriTemplate(String uriTemplate, boolean relax) {
        return encode(uriTemplate, relax, uriTemplateChars);
    }

    /**
     * Encode a string according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>, escaping all
     * characters where <code>unreserved[char] == false</code>, where
     * <code>char</code> is a single character such as 'a'.
     * 
     * @param string string to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded.
     * @param unreserved an array of booleans that indicates which characters
     *            are considered unreserved. a character is considered
     *            unreserved if <code>unreserved[char] == true</code>, in which
     *            case it will not be encoded
     * @return encoded US-ASCII string
     */
    private static String encode(String string, boolean relax, boolean[] unreserved) {
        if (string == null) {
            return null;
        }

        if (!needsEncoding(string, false, unreserved)) {
            return string;
        }

        // Encode to UTF-8
        ByteBuffer buffer = CHARSET_UTF_8.encode(string);
        // Prepare string buffer
        StringBuilder sb = new StringBuilder(buffer.remaining());
        // Now encode the characters
        while (buffer.hasRemaining()) {
            int c = buffer.get();

            if ((c == '%') && relax && (buffer.remaining() >= 2)) {
                int position = buffer.position();
                if (isHex(buffer.get(position)) && isHex(buffer.get(position + 1))) {
                    sb.append((char)c);
                    continue;
                }
            }

            if ((c >= ' ' && unreserved[c])) {
                sb.append((char)c);
            } else {
                sb.append('%');
                sb.append(hexDigits[(c & 0xf0) >> 4]);
                sb.append(hexDigits[c & 0xf]);
            }
        }

        return sb.toString();
    }

    private static boolean isHex(int c) {
        return isHexDigit[c];
    }

    /**
     * Determines if the input string contains any invalid URI characters that
     * require encoding
     * 
     * @param uri the string to test
     * @return true if the the input string contains only valid URI characters
     */
    private static boolean needsEncoding(String s, boolean relax, boolean[] unreserved) {
        int len = s.length();
        for (int i = 0; i < len; ++i) {
            char c = s.charAt(i);
            if (c == '%' && relax) {
                continue;
            }
            if (c > unreserved.length) {
                return true;
            }
            if (unreserved[c] == false) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decode US-ASCII uri according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a> and replaces all
     * occurrences of the '+' sign with spaces.
     * 
     * @param string query string to decode
     * @return decoded query
     */
    public static String decodeQuery(String string) {
        return decodeString(string, true, null);
    }

    /**
     * Decode US-ASCII uri according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param string US-ASCII uri to decode
     * @return decoded uri
     */
    public static String decodeString(String string) {
        return decodeString(string, false, null);
    }

    /**
     * Decodes only the unreserved chars, according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a> section 6.2.2.2
     * 
     * @param string US-ASCII uri to decode
     * @return decoded uri
     */
    public static String normalize(String string) {
        return decodeString(string, false, unreservedChars);
    }

    private static String decodeString(String string, boolean query, boolean[] decodeChars) {
        if (string == null) {
            return null;
        }

        if (!needsDecoding(string, query)) {
            return string;
        }

        int len = string.length();
        // Prepare byte buffer
        ByteBuffer buffer = ByteBuffer.allocate(len);
        // decode string into byte buffer
        for (int i = 0; i < len; ++i) {
            char c = string.charAt(i);
            if (c == '%' && (i + 2 < len)) {
                int v = 0;
                int d1 = decodeHexDigit(string.charAt(i + 1));
                int d2 = decodeHexDigit(string.charAt(i + 2));
                if (d1 >= 0 && d2 >= 0) {
                    v = d1;
                    v = v << 4 | d2;
                    if (decodeChars != null && (v >= decodeChars.length || !decodeChars[v])) {
                        buffer.put((byte)string.charAt(i));
                        buffer.put(normalizedHexDigits[string.charAt(i + 1)]);
                        buffer.put(normalizedHexDigits[string.charAt(i + 2)]);
                    } else {
                        buffer.put((byte)v);
                    }
                    i += 2;
                } else {
                    buffer.put((byte)c);
                }
            } else {
                if (query && c == '+') {
                    c = ' ';
                }
                buffer.put((byte)c);
            }
        }
        // Decode byte buffer from UTF-8
        buffer.flip();
        return CHARSET_UTF_8.decode(buffer).toString();
    }

    private static boolean needsDecoding(String s, boolean query) {
        boolean needs = s.indexOf('%') != -1;
        if (!needs && query) {
            needs = s.indexOf('+') != -1;
        }
        return needs;
    }

}
