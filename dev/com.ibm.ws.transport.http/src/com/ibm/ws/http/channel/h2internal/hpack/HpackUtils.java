/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.hpack;

public class HpackUtils {

    protected static byte getBit(byte b, int position) {
        return (byte) ((b >> position) & 1);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                  + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String byteToHexString(byte b) {
        return String.format("%02X ", b);
    }

    public static String integerToBinaryString(byte b) {
        return ("0000000" + Integer.toBinaryString(0xFF & b)).replaceAll(".*(.{8})$", "$1");
    }

    public static byte getLSB(byte b, int LSBits) {
        byte compare = (byte) (ipow(2, LSBits) - 1);
        return (byte) (b & compare);

    }

    public static byte format(byte b, byte format) {

        return (byte) (b | format);
    }

    static int ipow(int base, int exp) {
        int result = 1;
        while (exp != 0) {
            if ((exp & 1) != 0)
                result *= base;
            exp >>= 1;
            base *= base;
        }

        return result;
    }

    public static boolean isAllLower(String str) {
        for (char c : str.toCharArray()) {
            if ((c >= 'A' && c <= 'Z')) {
                return false;
            }
        }
        return true;
    }

}