/*******************************************************************************
 * Copyright (c) 2019,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.utils;

import java.util.concurrent.atomic.AtomicLong;

public class SequenceNumber {
    public static String formatSequenceNumber(long date, long value) {
        return Long.toString(date) + "_" + toHex(value);
    }

    private static final String ZEROS = "0000000000000";

    private static String toHex(long value) {
        String hexValue = Long.toHexString(value);

        int hexLen = hexValue.length();
        if (hexLen < ZEROS.length()) {
            hexValue = ZEROS.substring(hexLen) + hexValue;
        }
        return hexValue;
    }

    public static void main(String[] args) {
        long[] testValues = { 0, 1, 2, 10, 16, 17, 30, 32, 33, 255, 256, 257, 1023, 1024, 1025 };

        for (long value : testValues) {
            System.out.println(" [ " + value + " ] [ " + toHex(value) + " ]");
        }
    }

    //

    private final AtomicLong seq = new AtomicLong();

    public long getRawSequenceNumber() {
        return seq.incrementAndGet();
    }

    public String next(long date) {
        return formatSequenceNumber(date, seq.incrementAndGet());
    }
}
