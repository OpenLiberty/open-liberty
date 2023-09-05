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
import java.util.concurrent.atomic.AtomicReference;

class CachedTime {

    /** Stored formatter */
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM uuuu HH:mm:ss zzz", Locale.US).withZone(ZoneId.of("GMT"));

    private static final CachedTime instance = new CachedTime();

    private static class CachedFormattedTime {
        final long timeInMilliseconds;
        final String formattedTimeString;

        CachedFormattedTime(long time, String formattedString) {
            this.timeInMilliseconds = time;
            this.formattedTimeString = formattedString;
        }
    }

    private final AtomicReference<CachedFormattedTime> cachedTime = new AtomicReference<>();

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
     * Get a formatted version of the time as a String.
     * <br>
     * @param  current time in milliseconds from System.currentTimeMillis()
     * @return String
     */
    protected String getTimeAsString(long now) {
        // We only care about seconds, so remove the milliseconds from the time.
        now -= (now % 1000);

        CachedFormattedTime cachedFormattedTime = cachedTime.get();
        if (cachedFormattedTime != null && now == cachedFormattedTime.timeInMilliseconds) {
            return cachedFormattedTime.formattedTimeString;
        }

        // otherwise need to format the current time
        String sTime = dateFormatter.format(Instant.ofEpochMilli(now));

        // Only update it if another thread hasn't already updated it.
        cachedTime.compareAndSet(cachedFormattedTime, new CachedFormattedTime(now, sTime));
        return sTime;
    }
}
//Liberty Change for CXF End
