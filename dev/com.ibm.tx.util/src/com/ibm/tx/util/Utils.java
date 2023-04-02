/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tx.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.ws.kernel.service.util.CpuInfo;

public class Utils {
    private static final String pattern = "dd/MM/yyyy, HH:mm.ss:SSS z";

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

    public static <K, V> Map<K, V> createConcurrentMap() {
        return new ConcurrentHashMap<>(256, 0.75f, getNumCHBuckets());
    }

    public static <K> Set<K> createConcurrentSet() {
        return Collections.newSetFromMap(createConcurrentMap());
    }

    // Calculate number of concurrent hash buckets as a factor of
    // the number of available processors.
    private static int getNumCHBuckets() {
        // determine number of processors
        final int baseVal = CpuInfo.getAvailableProcessors().get() * 20;

        // determine next power of two
        int pow = 2;
        while (pow < baseVal)
            pow *= 2;
        return pow;
    }
}
