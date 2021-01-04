/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tx.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Utils {
    private static final String pattern = "dd/mm/yyyy, HH:MM.ss:SSS z";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(pattern);
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    public static final String traceTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZONE_ID).format(DATE_TIME_FORMATTER);
    }

    /**
     * Converts a byte array to a string.
     */
    public static String toString(byte[] b) {
        StringBuffer result = new StringBuffer(b.length);
        for (int i = 0; i < b.length; i++)
            result.append((char) b[i]);
        return (result.toString());
    }

    public static byte[] byteArray(String s) {
        return byteArray(s, false);
    }

    public static byte[] byteArray(String s, boolean keepBothBytes) {
        byte[] result = new byte[s.length() * (keepBothBytes ? 2 : 1)];
        for (int i = 0; i < result.length; i++)
            result[i] = keepBothBytes ? (byte) (s.charAt(i / 2) >> (i & 1) * 8) : (byte) (s.charAt(i));
        return result;
    }
}
