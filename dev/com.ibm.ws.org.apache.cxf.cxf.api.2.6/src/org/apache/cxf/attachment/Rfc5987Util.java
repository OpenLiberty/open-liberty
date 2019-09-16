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
package org.apache.cxf.attachment;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for encoding and decoding values according to RFC 5987. Assumes the
 * caller already knows the encoding scheme for the value.
 */
public final class Rfc5987Util {

    public static final Charset UTF_8 = Charset.forName("UTF-8");


    private static final Pattern ENCODED_VALUE_PATTERN = Pattern.compile("%[0-9a-f]{2}|\\S",
            Pattern.CASE_INSENSITIVE);
    
    private Rfc5987Util() {

    }

    public static String encode(final String s) throws UnsupportedEncodingException {
        return encode(s, UTF_8.toString());
    }

    // http://stackoverflow.com/questions/11302361/ (continued next line)
    // handling-filename-parameters-with-spaces-via-rfc-5987-results-in-in-filenam
    public static String encode(final String s, String encoding) throws UnsupportedEncodingException {
        final byte[] rawBytes = s.getBytes(encoding);
        final int len = rawBytes.length;
        final StringBuilder sb = new StringBuilder(len << 1);
        final char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'};
        final byte[] attributeChars = {'!', '#', '$', '&', '+', '-', '.', '0',
            '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
            'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '^', '_', '`', 'a',
            'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '|',
            '~'};
        for (final byte b : rawBytes) {
            if (Arrays.binarySearch(attributeChars, b) >= 0) {
                sb.append((char) b);
            } else {
                sb.append('%');
                sb.append(digits[0x0f & (b >>> 4)]);
                sb.append(digits[b & 0x0f]);
            }
        }

        return sb.toString();
    }

    public static String decode(String s, String encoding)
        throws UnsupportedEncodingException {
        Matcher matcher = ENCODED_VALUE_PATTERN.matcher(s);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (matcher.find()) {
            String matched = matcher.group();
            if (matched.startsWith("%")) {
                Integer value = Integer.parseInt(matched.substring(1), 16);
                bos.write(value);
            } else {
                bos.write(matched.charAt(0));
            }
        }

        return new String(bos.toByteArray(), encoding);
    }
}