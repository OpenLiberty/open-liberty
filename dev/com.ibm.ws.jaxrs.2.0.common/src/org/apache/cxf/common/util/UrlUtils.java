/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Utility class for decoding and encoding URLs
 * 
 */
@Trivial
public final class UrlUtils {

    private static final int RADIX = 16;
    private static final byte ESCAPE_CHAR = '%';
    private static final byte PLUS_CHAR = '+';

    private UrlUtils() {

    }

    public static String urlEncode(String value) {

        return urlEncode(value, StandardCharsets.UTF_8.name());
    }

    @FFDCIgnore(UnsupportedEncodingException.class)
    public static String urlEncode(String value, String enc) {

        try {
            value = URLEncoder.encode(value, enc);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        return value;
    }

    /**
     * Decodes using URLDecoder - use when queries or form post values are decoded
     * 
     * @param value value to decode
     * @param enc encoding
     */
    public static String urlDecode(String value, String enc) {
        return urlDecode(value, enc, false);
    }

    @FFDCIgnore(ArrayIndexOutOfBoundsException.class)
    private static String urlDecode(String value, String enc, boolean isPath) {

        boolean needDecode = false;
        int escapesCount = 0;
        int i = 0;
        final int length = value.length();
        while (i < length) {
            //Liberty change END
            char ch = value.charAt(i++);
            if (ch == ESCAPE_CHAR) {
                escapesCount += 1;
                i += 2;
                needDecode = true;
            } else if (!isPath && ch == PLUS_CHAR) {
                needDecode = true;
            }
        }
        if (needDecode) {
            final byte[] valueBytes = StringUtils.toBytes(value, enc);
            ByteBuffer in = ByteBuffer.wrap(valueBytes);
            ByteBuffer out = ByteBuffer.allocate(in.capacity() - (2 * escapesCount) + 1);
            while (in.hasRemaining()) {
                final int b = in.get();
                if (!isPath && b == PLUS_CHAR) {
                    out.put((byte) ' ');
                } else if (b == ESCAPE_CHAR) {
                    try {
                        final int u = digit16((byte) in.get());
                        final int l = digit16((byte) in.get());
                        out.put((byte) ((u << 4) + l));
                    } catch (final BufferUnderflowException e) {
                        throw new IllegalArgumentException(
                                "Invalid URL encoding: Incomplete trailing escape (%) pattern");
                    }
                } else {
                    out.put((byte) b);
                }
            }
            out.flip();
            return Charset.forName(enc).decode(out).toString();
        } else {
            return value;
        }
    }

    private static int digit16(final byte b) {
        final int i = Character.digit((char) b, RADIX);
        if (i == -1) {
            throw new IllegalArgumentException("Invalid URL encoding: not a valid digit (radix " + RADIX + "): " + b);
        }
        return i;
    }

    public static String urlDecode(String value) {
        return urlDecode(value, StandardCharsets.UTF_8.name());
    }

    /**
     * URL path segments may contain '+' symbols which should not be decoded into ' '
     * This method replaces '+' with %2B and delegates to URLDecoder
     * 
     * @param value value to decode
     */
    public static String pathDecode(String value) {
        return urlDecode(value, StandardCharsets.UTF_8.name(), true);
    }

    /**
     * Create a map from String to String that represents the contents of the query
     * portion of a URL. For each x=y, x is the key and y is the value.
     * 
     * @param s the query part of the URI.
     * @return the map.
     */
    public static Map<String, String> parseQueryString(String s) {
        Map<String, String> ht = new HashMap<String, String>();
        StringTokenizer st = new StringTokenizer(s, "&");
        while (st.hasMoreTokens()) {
            String pair = st.nextToken();
            int pos = pair.indexOf('=');
            if (pos == -1) {
                ht.put(pair.toLowerCase(), "");
            } else {
                ht.put(pair.substring(0, pos).toLowerCase(),
                       pair.substring(pos + 1));
            }
        }
        return ht;
    }

    /**
     * Return everything in the path up to the last slash in a URI.
     * 
     * @param baseURI
     * @return the trailing
     */
    public static String getStem(String baseURI) {
        int idx = baseURI.lastIndexOf('/');
        String result = baseURI;
        if (idx != -1) {
            result = baseURI.substring(0, idx);
        }
        return result;
    }

}
