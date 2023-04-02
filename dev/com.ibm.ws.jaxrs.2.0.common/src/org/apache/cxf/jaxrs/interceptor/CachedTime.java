/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
// Liberty Change for CXF Begin
package org.apache.cxf.jaxrs.interceptor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

class CachedTime {

    /** Stored formatter */
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).withZone(ZoneId.of("GMT"));

    /** Last time we formatted a value */
    private long lastTimeCheck = 0L;
    /** The stored formatted time as a string */
    private String sTime = null;

    private static CachedTime instance = new CachedTime();

    protected static final long DEFAULT_TOLERANCE = 1000L;

    /**
     * Create a cachedTime instance with the given format.
     * <br>
     * 
     * @param format
     */

    protected CachedTime() {
    }

    public static CachedTime getCachedTime() {
        return instance;
    }

    /**
     * Utility method to determine whether to use the cached time value or
     * update to a newly formatted timestamp.
     * 
     * @param tolerance
     */
    private void updateTime(long tolerance) {
        long now = System.currentTimeMillis();
        // check for exact match
        if (now == this.lastTimeCheck) {
            return;
        }
        // check for a "range" match
        if (0L != tolerance) {
            long range = (-1 == tolerance) ? DEFAULT_TOLERANCE : tolerance;
            if ((now - this.lastTimeCheck) <= range) {
                return;
            }
        }
        // otherwise need to format the current time
        sTime = dateFormatter.format(Instant.ofEpochMilli(now));

        this.lastTimeCheck = now;
    }

    /**
     * Get a formatted version of the time as a String. The input range is
     * the allowed difference in time from the cached snapshot that the
     * caller is willing to use. If that range is exceeded, then a new
     * snapshot is taken and formatted.
     * <br>
     * 
     * @param tolerance -- milliseconds, -1 means use default 1000ms, a 0
     *            means that this must be an exact match in time
     * @return String
     */
    protected synchronized String getTimeAsString(long tolerance) {
        updateTime(tolerance);
        return this.sTime;
    }
}
//Liberty Change for CXF End
