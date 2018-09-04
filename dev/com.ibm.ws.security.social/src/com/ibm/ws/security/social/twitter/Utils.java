/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.twitter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Calendar;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.social.TraceConstants;

public class Utils {

    private static TraceComponent tc = Tr.register(Utils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static final char[] chars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };

    public static final int NONCE_LENGTH = 40;

    /**
     * Generates a random string of length {@value #NONCE_LENGTH}
     *
     * @return
     */
    public static String generateNonce() {
        return generateNonce(NONCE_LENGTH);
    }

    /**
     * Generates a random string with the specified length.
     *
     * @param length
     * @return
     */
    public static String generateNonce(int length) {
        if (length < 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Negative length provided. Will default to nonce of length " + NONCE_LENGTH);
            }
            length = NONCE_LENGTH;
        }
        StringBuilder randomString = new StringBuilder();
        SecureRandom r = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int index = r.nextInt(chars.length);
            randomString.append(chars[index]);
        }
        return randomString.toString();
    }

    public static String getCurrentTimestamp() {
        long timeMs = Calendar.getInstance().getTimeInMillis();
        String timestamp = Long.valueOf(timeMs / 1000).toString();
        return timestamp;
    }

    /**
     * Percent encodes the specified value to be compliant with RFC 3986.
     *
     * @param value
     * @return
     */
    public static String percentEncode(String value) {
        if (value == null) {
            return "";
        }

        String encoded = value;
        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
            // Ignore - UTF-8 should be supported
        }

        // Create RFC 3986-compliant encoded string
        StringBuilder buf = new StringBuilder(encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '+') {
                buf.append("%20");
            } else if (c == '*') {
                buf.append("%2A");
            } else if (c == '%' && (i + 2) < encoded.length() && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
                buf.append('~');
                i += 2;
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
     * Percent encodes the specified value to be compliant with RFC 3986.
     *
     * @param value
     * @return
     */
    @Sensitive
    public static String percentEncodeSensitive(@Sensitive String value) {
        if (value == null) {
            return "";
        }

        String encoded = value;
        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {
            // Ignore - UTF-8 should be supported
        }

        // Create RFC 3986-compliant encoded string
        StringBuilder buf = new StringBuilder(encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '+') {
                buf.append("%20");
            } else if (c == '*') {
                buf.append("%2A");
            } else if (c == '%' && (i + 2) < encoded.length() && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
                buf.append('~');
                i += 2;
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

}
