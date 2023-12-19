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

import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Locale.Category;

/**
 *
 */
public class DateFormatHelper {

    /**
     * A format that will produce a reasonable standard way for
     * formatting time (but still using the current locale)
     */
    private static final BurstDateFormat localeDateFormat;

    private static final BurstDateFormat isoDateFormat;

    static {
        // Retrieve a standard Java DateFormat object with desired format.
        Locale locale = Locale.getDefault(Category.FORMAT);
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, FormatStyle.MEDIUM, Chronology.ofLocale(locale), locale);
        // Append milliseconds and timezone after seconds
        int patternLength = pattern.length();
        int endOfSecsIndex = pattern.lastIndexOf('s') + 1;
        StringBuilder newPattern = new StringBuilder(pattern.substring(0, endOfSecsIndex) + ":SSS z");
        if (endOfSecsIndex < patternLength)
            newPattern.append(pattern.substring(endOfSecsIndex, patternLength));
        // 0-23 hour clock (get rid of any other clock formats and am/pm)
        String localeDatePattern = removeClockFormat(newPattern);

        localeDateFormat = new BurstDateFormat(localeDatePattern, ':', locale);

        isoDateFormat = new BurstDateFormat("uuuu-MM-dd'T'HH:mm:ss.SSSZ", '.');
    }

    /**
     * Return the localeDatePattern without clock formats and am/pm as a trimmed String
     *
     * @param sb
     *               A StringBuilder holding the pre-formatted localeDatePattern
     * @return
     *         The formatted localeDatePattern
     */
    private static final String removeClockFormat(StringBuilder sb) {
        int start = 0;
        int end = sb.length() - 1;
        char letter;
        // trim left side
        while (start <= end && ((letter = sb.charAt(start)) == ' ' || letter == 'a'))
            start++;
        // trim right side
        // Java 20 added a narrow no-break space character into the format (Unicode 202F character)
        while (end > start && ((letter = sb.charAt(end)) == ' ' || letter == 'a' || letter == '\u202f'))
            end--;
        // replace characters 'h', 'k', and 'K' to 'H' and 'a' to ' '
        for (int i = start; i <= end; i++) {
            letter = sb.charAt(i);
            if (letter == 'h' || letter == 'k' || letter == 'K')
                sb.setCharAt(i, 'H');
            else if (letter == 'a')
                sb.setCharAt(i, ' ');
        }
        return sb.substring(start, end + 1);
    }

    /**
     * Return the given time formatted, based on the provided format structure
     *
     * @param timestamp
     *                             A timestamp as a long, e.g. what would be returned from
     *                             <code>System.currentTimeMillis()</code>
     *
     * @param useIsoDateFormat
     *                             A boolean, if true, the given date and time will be formatted in ISO-8601,
     *                             e.g. yyyy-MM-dd'T'HH:mm:ss.SSSZ
     *                             if false, the date and time will be formatted as the current locale,
     *
     * @return formated date string
     */
    public static final String formatTime(long timestamp, boolean useIsoDateFormat) {
        return (useIsoDateFormat ? isoDateFormat : localeDateFormat).format(timestamp);
    }
}
