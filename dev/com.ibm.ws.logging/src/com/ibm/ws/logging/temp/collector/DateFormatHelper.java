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
package com.ibm.ws.logging.temp.collector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 *
 */
public class DateFormatHelper {

    /** Thread specific date format objects */
    private static ThreadLocal<BurstDateFormat> dateformats = new ThreadLocal<BurstDateFormat>();

    /** Locale date and time format pattern */
    private static final ThreadLocal<String> localeDatePattern = new ThreadLocal<String>();

    /**
     * Return the given time formatted, based on the provided format structure
     *
     * @param timestamp
     *            A timestamp as a long, e.g. what would be returned from
     *            <code>System.currentTimeMillis()</code>
     *
     * @param useIsoDateFormat
     *            A boolean, if true, the given date and time will be formatted in ISO-8601,
     *            e.g. yyyy-MM-dd'T'HH:mm:ss.SSSZ
     *            if false, the date and time will be formatted as the current locale,
     *
     * @return formated date string
     */
    public static final String formatTime(long timestamp, boolean useIsoDateFormat) {
        BurstDateFormat df = dateformats.get();
        if (df == null) {
            SimpleDateFormat formatter = (SimpleDateFormat) getDateFormat();

            df = new BurstDateFormat((SimpleDateFormat) getDateFormat());
            dateformats.set(df);
            String ddp = formatter.toPattern();
            localeDatePattern.set(ddp);
        }

        try {
            // Get and store the locale Date pattern that was retrieved from getDateTimeInstance, to be used later.
            // This is to prevent creating multiple new instances of SimpleDateFormat, which is expensive.

            if (useIsoDateFormat) {
                df.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            } else {
                df.applyPattern(localeDatePattern.get());
            }
        } catch (Exception e) {
            // Use the default pattern, instead.
        }

        return df.format(timestamp);
    }

    /**
     * Return a format string that will produce a reasonable standard way for
     * formatting time (but still using the current locale)
     *
     * @return The format string
     */
    private static DateFormat getDateFormat() {
        String pattern;
        int patternLength;
        int endOfSecsIndex;
        // Retrieve a standard Java DateFormat object with desired format.
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        if (formatter instanceof SimpleDateFormat) {
            // Retrieve the pattern from the formatter, since we will need to
            // modify it.
            SimpleDateFormat sdFormatter = (SimpleDateFormat) formatter;
            pattern = sdFormatter.toPattern();
            // Append milliseconds and timezone after seconds
            patternLength = pattern.length();
            endOfSecsIndex = pattern.lastIndexOf('s') + 1;
            String newPattern = pattern.substring(0, endOfSecsIndex) + ":SSS z";
            if (endOfSecsIndex < patternLength)
                newPattern += pattern.substring(endOfSecsIndex, patternLength);
            // 0-23 hour clock (get rid of any other clock formats and am/pm)
            newPattern = newPattern.replace('h', 'H');
            newPattern = newPattern.replace('K', 'H');
            newPattern = newPattern.replace('k', 'H');
            newPattern = newPattern.replace('a', ' ');
            newPattern = newPattern.trim();
            sdFormatter.applyPattern(newPattern);
            formatter = sdFormatter;
        } else {
            formatter = new SimpleDateFormat("dd/MMM/yyyy HH:mm:ss:SSS z");
        }
        return formatter;
    }
}
