/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.util;

import java.util.regex.Pattern;

public enum Base64UrlEncoder implements Encoder {
    URL_AND_FILENAME_SAFE_ENCODING;

    private static final char[] BASE64URL_CHARS = (
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz"
            + "0123456789"
            + "-_"
    ).toCharArray();

    private static final Pattern ENCODED_STRING = Pattern.compile("([A-Za-z0-9\\-_]{4})*([A-Za-z0-9\\-_]{2}==|[A-Za-z0-9\\-_]{3}=)?");

    private static final char PAD_CHAR = '=';

    public String encode(byte[] bytes) {
        int len = getEncodedLength(bytes.length);
        StringBuilder sb = new StringBuilder(len);

        int leftovers = 0;
        for (int i = 0; i < bytes.length; i++) {
            int b = 0xFF & bytes[i]; // mask to avoid negative b
            switch (i%3) {
                case 0:
                    sb.append(BASE64URL_CHARS[(b>>>2)]);
                    leftovers = (b & 0x03) << 4;
                    break;
                case 1:
                    sb.append(BASE64URL_CHARS[leftovers | (b>>>4)]);
                    leftovers = (b & 0x0F) << 2;
                    break;
                case 2:
                    sb.append(BASE64URL_CHARS[leftovers | (b>>>6)]);
                    sb.append(BASE64URL_CHARS[b & 0x3F]);
                    break;
            }
        }

        switch (bytes.length%3) {
            case 0:
                break;
            case 1:
                sb.append(BASE64URL_CHARS[leftovers]).append(PAD_CHAR).append(PAD_CHAR);
                break;
            case 2:
                sb.append(BASE64URL_CHARS[leftovers]).append(PAD_CHAR);
                break;
        }

        assert sb.length() == len;

        return sb.toString();
    }

    public static int getEncodedLength(int numBytes) {
        return (numBytes + 2) / 3 * 4;
    }

    public static boolean isEncodedString(String s) {
        return ENCODED_STRING.matcher(s).matches();
    }
}
