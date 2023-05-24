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
package com.ibm.ws.logging.collector;

import java.time.Instant;
import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Formats dates that are relatively close to one another.
 * Leverages the fact that the majority of the Date does not change when formats are milliseconds apart.
 * Calls format again when the seconds change. (To avoid tracking DST).
 *
 * Not thread-safe, use ThreadLocal
 */
public class BurstDateFormat {

    private final DateTimeFormatter formatter;

    private final char millisecondSeparator;

    private final AtomicReference<CachedFormattedTime> cachedTime = new AtomicReference<>();

    /**
     * Tracks whether the format is not valid. If true, the underlying SimpleDateFormat is used
     */
    volatile boolean invalidFormat = false;

    private static class CachedFormattedTime {
        /**
         * Reference timestamp
         */
        final long refTimestampWithoutMillis;

        /**
         * Reference beginning of the date
         */
        final String refBeginning;

        /**
         * Reference ending of the date
         */
        final String refEnding;

        CachedFormattedTime(long refTimestampWithoutMillis, String refBeginning, String refEnding) {
            this.refTimestampWithoutMillis = refTimestampWithoutMillis;
            this.refBeginning = refBeginning;
            this.refEnding = refEnding;
        }
    }

    /**
     * Constructs a BurstDateFormat
     *
     * @param formatter
     */
    public BurstDateFormat(String formatPattern, char millisecondSeparator) {
        this(formatPattern, millisecondSeparator, Locale.getDefault(Category.FORMAT));
    }

    public BurstDateFormat(String formatPattern, char millisecondSeparator, Locale locale) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().appendPattern(formatPattern);
        this.formatter = builder.toFormatter().withZone(ZoneId.systemDefault()).withLocale(locale).withChronology(Chronology.ofLocale(locale)).withDecimalStyle(DecimalStyle.of(locale));
        this.millisecondSeparator = millisecondSeparator;
        try {
            // Get the positions of the millisecond digits
            invalidFormat = (formatPattern.lastIndexOf('S') - formatPattern.indexOf('S') != 2);
        } catch (Exception e) {
            invalidFormat = true;
        }
    }

    /**
     * Formats a timestamp
     *
     * @param timestamp
     * @return
     */
    public String format(long timestamp) {

        // If the format is unknown, use the default formatter.
        if (invalidFormat) {
            return formatter.format(Instant.ofEpochMilli(timestamp));
        }

        try {

            long milliseconds = timestamp % 1000L;

            long timestampWithoutMillis = timestamp - milliseconds;

            CachedFormattedTime cachedFormattedTime = cachedTime.get();

            // If we need to reformat
            if (cachedFormattedTime == null || timestampWithoutMillis != cachedFormattedTime.refTimestampWithoutMillis) {
                String formattedDate = formatter.format(Instant.ofEpochMilli(timestamp));

                StringBuilder sb = new StringBuilder(4);

                sb.append(millisecondSeparator);
                if (milliseconds < 100) {
                    if (milliseconds < 10) {
                        sb.append("00");
                    } else {
                        sb.append('0');
                    }
                }
                sb.append(milliseconds);

                String millisecondString = sb.toString();

                int index = formattedDate.indexOf(millisecondString);

                if (index == -1) {
                    invalidFormat = true;
                    return formattedDate;
                }

                String refBeginning = formattedDate.substring(0, index + 1);
                String refEnding = formattedDate.substring(index + 4);

                cachedTime.compareAndSet(cachedFormattedTime, new CachedFormattedTime(timestampWithoutMillis, refBeginning, refEnding));
                return formattedDate;
            }

            StringBuilder sb = new StringBuilder(cachedFormattedTime.refBeginning);

            if (milliseconds < 100) {
                if (milliseconds < 10) {
                    sb.append("00");
                } else {
                    sb.append('0');
                }
            }

            sb.append(milliseconds).append(cachedFormattedTime.refEnding);
            return sb.toString();

        } catch (Exception e) {
            // Throw FFDC in case anything goes wrong
            // Still generate the date via the SimpleDateFormat
            invalidFormat = true;
            return formatter.format(Instant.ofEpochMilli(timestamp));
        }
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return formatter;
    }
}
