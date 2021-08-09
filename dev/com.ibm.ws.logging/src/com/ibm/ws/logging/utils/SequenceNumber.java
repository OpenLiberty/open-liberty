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
package com.ibm.ws.logging.utils;

import java.util.concurrent.atomic.AtomicLong;

/*
 * thread safe sequence number creator
 */
public class SequenceNumber {
    private final AtomicLong seq = new AtomicLong();
    private final static String ZEROES = "0000000000000";

    /*
     * Creates the next sequence number
     */
    public long getRawSequenceNumber() {
        return seq.incrementAndGet();
    }

    public static String formatSequenceNumber(long date, long n) {
        return date + "_" + toPaddedHex(n);
    }

    /*
     * Creates the next sequence number formatted string
     */
    public String next(long date) {
        long n = seq.incrementAndGet();
        return formatSequenceNumber(date, n);
    }

    /*
     * Converts a long to a 13 character 0-padded lower case hex string
     */
    private static String toPaddedHex(long n) {
        String hexValue = Long.toHexString(n);
        hexValue = upperCaseHex(hexValue);
        String paddedHexValue = hexValue.length() <= 12 ? ZEROES.substring(hexValue.length()) + hexValue : hexValue;
        return paddedHexValue;
    }

    /*
     * Efficiently converts a string containing a hexadecimal number from lower case to upper case
     */
    private static String upperCaseHex(String s) {
        char chars[] = s.toCharArray();
        int length = s.length();

        for (int i = 0; i < length; i++) {

            switch (chars[i]) {
                case 'a':
                    chars[i] = 'A';
                    break;
                case 'b':
                    chars[i] = 'B';
                    break;
                case 'c':
                    chars[i] = 'C';
                    break;
                case 'd':
                    chars[i] = 'D';
                    break;
                case 'e':
                    chars[i] = 'E';
                    break;
                case 'f':
                    chars[i] = 'F';
                    break;
            }
        }

        return new String(chars);
    }

}
