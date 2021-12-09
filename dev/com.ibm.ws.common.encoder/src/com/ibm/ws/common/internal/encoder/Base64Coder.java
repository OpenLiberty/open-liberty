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
package com.ibm.ws.common.internal.encoder;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Base64 Encoding and Decoding utility class
 */
public final class Base64Coder {
    private static final byte Base64EncMap[];
    private static final byte Base64DecMap[];

    // Tokens used in Base64-encoded data, indexed by their corresponding byte value.
    private static final char[] TOKENS = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a',
                                           'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1',
                                           '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };

    // Character used to pad encoding groups at the end of Base64-encoded content.
    private static final char PAD_TOKEN = '=';

    // The minimum token character value considered for decoding.
    private static final int TOKEN_MIN = 0;

    // The maximum token character value considered for decoding.
    private static final int TOKEN_MAX = Byte.MAX_VALUE;

    // Value associated with characters that are not valid Base64 token.
    private static final byte INVALID_TOKEN_VALUE = -1;

    // Array indexed by token character and containing decoded values.
    // INVALID_TOKEN_VALUE is used for characters that are not valid Base64
    // tokens.
    private static final byte[] TOKEN_VALUES = new byte[TOKEN_MAX - TOKEN_MIN + 1];

    static {
        byte map[] = { 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107,
                       108,
                       109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47 };
        Base64EncMap = map;
        Base64DecMap = new byte[128];
        for (int idx = 0; idx < Base64EncMap.length; idx++)
            Base64DecMap[Base64EncMap[idx]] = (byte) idx;

    }

    /** Prevent instantiation of this static-only class */
    private Base64Coder() {
    }

    /**
     * Converts a String to a base64 encoded String.
     *
     * @param str String, may be {@code null}.
     * @return base64 encoded String {@code null} if the str was null.
     */
    public static final String base64Encode(String str) {
        if (str == null) {
            return null;
        } else {
            byte data[] = getBytes(str);
            return toString(base64Encode(data));
        }
    }

    /**
     * Converts a raw byte array to a base64 encoded byte array.
     *
     * @param data raw byte array, may be {@code null}.
     * @return base64 encoded String or {@code null} if the data was null.
     */
    public static final String base64EncodeToString(byte data[]) {
        return toString(base64Encode(data));
    }

    /**
     * Converts a raw byte array to a base64 encoded byte array.
     *
     * @param data raw byte array, may be {@code null}.
     * @return base64 encoded byte array or {@code null} if the data was null.
     */
    public static final byte[] base64Encode(byte data[]) {
        if (data == null)
            return null;
        byte dest[] = new byte[((data.length + 2) / 3) * 4];
        int sidx = 0;
        int didx = 0;
        for (; sidx < data.length - 2; sidx += 3) {
            dest[didx++] = Base64EncMap[data[sidx] >>> 2 & 0x3f];
            dest[didx++] = Base64EncMap[data[sidx + 1] >>> 4 & 0xf | data[sidx] << 4 & 0x3f];
            dest[didx++] = Base64EncMap[data[sidx + 2] >>> 6 & 0x3 | data[sidx + 1] << 2 & 0x3f];
            dest[didx++] = Base64EncMap[data[sidx + 2] & 0x3f];
        }

        if (sidx < data.length) {
            dest[didx++] = Base64EncMap[data[sidx] >>> 2 & 0x3f];
            if (sidx < data.length - 1) {
                dest[didx++] = Base64EncMap[data[sidx + 1] >>> 4 & 0xf | data[sidx] << 4 & 0x3f];
                dest[didx++] = Base64EncMap[data[sidx + 1] << 2 & 0x3f];
            } else {
                dest[didx++] = Base64EncMap[data[sidx] << 4 & 0x3f];
            }
        }
        for (; didx < dest.length; didx++) {
            dest[didx] = 61;
        }

        return dest;
    }

    /**
     * Converts a base64 encoded byte array to a raw byte array.
     *
     * @param data base64 encoded byte array, may be {@code null}.
     * @return raw byte array or {@code null} if the data was null or
     *         not the expected size.
     */
    public static final byte[] base64Decode(byte data[]) {
        if (data == null) {
            return null;
        }
        if (data.length == 0) {
            return null;
        }
        if (!((data.length % 4) == 0)) {
            return null;
        }

        int tail;
        for (tail = data.length; data[tail - 1] == 61; tail--);
        byte dest[] = new byte[tail - data.length / 4];
        for (int idx = 0; idx < data.length; idx++) {
            data[idx] = Base64DecMap[data[idx]];
        }

        int sidx = 0;
        int didx;
        for (didx = 0; didx < dest.length - 2; didx += 3) {
            dest[didx] = (byte) (data[sidx] << 2 & 0xff | data[sidx + 1] >>> 4 & 0x3);
            dest[didx + 1] = (byte) (data[sidx + 1] << 4 & 0xff | data[sidx + 2] >>> 2 & 0xf);
            dest[didx + 2] = (byte) (data[sidx + 2] << 6 & 0xff | data[sidx + 3] & 0x3f);
            sidx += 4;
        }

        if (didx < dest.length) {
            dest[didx] = (byte) (data[sidx] << 2 & 0xff | data[sidx + 1] >>> 4 & 0x3);
        }
        if (++didx < dest.length) {
            dest[didx] = (byte) (data[sidx + 1] << 4 & 0xff | data[sidx + 2] >>> 2 & 0xf);
        }
        return dest;
    }

    /**
     * Encode a byte array into a Base64-encoded string. The string is not
     * broken into 72 character lines.
     *
     * @param data Byte data to be encoded.
     * @return Base64-encoded string.
     */
    public static String encode(byte[] data) {
        return encode(data, 0, data.length);
    }

    /**
     * Encode a byte array into a Base64-encoded String. The String is not
     * broken into 72 character lines.
     *
     * @param data Byte data to be encoded
     * @param offset Starting index within the byte data to be encoded
     * @param length Number of bytes to encode
     * @return Base64-encoded string.
     */
    public static String encode(byte[] data, int offset, int length) {

        // This method used to call the stream version of the encode() method but
        // the stream wrapper classes are significantly slower than manipulating the
        // byte array and writing to the output directly so the encoding logic is
        // duplicated here instead.
        final int remainder = length % 3;
        final int resultSize = ((remainder == 0 ? length : length + 3 - remainder) / 3 * 4);

        final StringBuffer resultBuff = new StringBuffer(resultSize);

        // Indicates the current position within the three byte encoding input
        // group.
        int groupIndex = 0;

        byte previousByte = 0;
        byte currentByte;

        // Iterate through and encode all the input data.
        for (int i = offset; i < length; i++) {
            currentByte = data[i];
            switch (groupIndex) {
                case 0: {
                    // Encode the first 6-bit output token using the first byte of
                    // the input group.
                    resultBuff.append(TOKENS[(currentByte & 0xfc) >> 2]);
                    groupIndex = 1;
                    break;
                }
                case 1: {
                    // Encode the second 6-bit output token using the first and
                    // second byte of the input group.
                    resultBuff.append(TOKENS[((previousByte & 0x3) << 4) | ((currentByte & 0xf0) >> 4)]);
                    groupIndex = 2;
                    break;
                }
                case 2: {
                    // Encode the third and fourth 6-bit output token using the
                    // second and third byte of the input group.
                    resultBuff.append(TOKENS[((previousByte & 0xf) << 2) | ((currentByte & 0xc0) >> 6)]);
                    resultBuff.append(TOKENS[currentByte & 0x3f]);
                    groupIndex = 0;
                    break;
                }
                default:
                    break;
            }

            // Keep hold of the current byte as it may contain data for the next
            // 6-bit output.
            previousByte = currentByte;
        }

        // Encode any remaining data using zero as the next byte, and pad to the
        // end
        // of the four token output group.
        switch (groupIndex) {
            case 1: {
                resultBuff.append(TOKENS[(previousByte & 0x3) << 4]);
                resultBuff.append(PAD_TOKEN);
                resultBuff.append(PAD_TOKEN);
                break;
            }
            case 2: {
                resultBuff.append(TOKENS[(previousByte & 0xf) << 2]);
                resultBuff.append(PAD_TOKEN);
                break;
            }
            default:
                break;
        }

        final String result = resultBuff.toString();

        return result;
    }

    /**
     * Converts a String with the given encoding to a base64 encoded String.
     *
     * @param str
     * @param enc
     * @return
     * @throws UnsupportedEncodingException
     */
    public static final String base64Decode(String str, String enc) throws UnsupportedEncodingException {
        if (str == null) {
            return null;
        } else {
            byte data[] = getBytes(str);
            return base64Decode(new String(data, enc));
        }

    }

    /**
     * Converts a base64 encoded String to a decoded String.
     *
     * @param str String, may be {@code null}.
     * @return base64 encoded String {@code null} if the str was null.
     */
    public static final String base64Decode(String str) {
        if (str == null) {
            return null;
        } else {
            byte data[] = getBytes(str);
            return toString(base64Decode(data));
        }
    }

    /**
     * Converts a base64 encoded String to a decoded byte array.
     *
     * @param str String, may be {@code null}.
     * @return base64 encoded byte array {@code null} if the str was null.
     */
    public static final byte[] base64DecodeString(String str) {
        return base64Decode(getBytes(str));
    }

    /**
     * Converts a byte array to a String. Only ASCII characters are supported.
     *
     * @param b byte array, may be {@code null}.
     * @return ASCII String representing the byte array or {@code null} if the array was null.
     */
    public static final String toString(byte[] b) {
        if (b == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0, len = b.length; i < len; i++) {
            sb.append((char) (b[i] & 0xff));
        }
        return sb.toString();
    }

    /**
     * Converts a String to a byte array by using UTF-8 character sets.
     * This code is needed even converting US-ASCII characters since there are some character sets which are not compatible with US-ASCII code area.
     * For example, CP-1399, which is z/OS Japanese EBCDIC character sets, is one.
     * 
     * @param input String, may be {@code null}.
     * @return byte array representing the String or {@code null} if the String was null or contained non ASCII character.
     */
    public static final byte[] getBytes(String input) {
        return input == null ? null : input.getBytes(StandardCharsets.UTF_8);
    }
}
