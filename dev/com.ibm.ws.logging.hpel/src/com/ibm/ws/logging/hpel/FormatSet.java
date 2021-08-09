/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.logging.hpel;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

/**
 * A FormatSet object simply contains a Date, SimpleDateFormat and a FieldPosition
 * object. This "tuple" is frequently used to format a timestamp for a log entry.
 * <p>
 * It is expensive to "new" up these objects every time a log entry is made and a
 * timestamp formatted. In V5, the formatting objects such as a TraceLogger contained
 * a single set of these objects that were continuously reused. However, the downside
 * of that approach is that every entry necessarily serialized on usage of the
 * formatting objects.
 * <p>
 * For V6, we will try a slighty more adaptive approacch. We will maintain a small cache
 * of "sets" of these formatting objects. When logging activity is low, we use cached objects
 * and avoid overhead of creating these objects. When logging activity is high, we will do
 * extra object create and destroys, but we will avoid the serialization problem.
 * <p>
 * These objects can be used "as is" to format normal logs. For the activity log, the
 * timestamp format is not appropriate.
 */
public class FormatSet {

    // Static caching management
    static private final int svMaxCacheSize = 5;
    static private Vector<FormatSet> svInstances = new Vector<FormatSet>();
    static private int svNumberCached = 0;
    /**
     *
     * PK13288 Start - TimeZone sysTimeZone.
     * Static variable for system-wide usage.
     * It was discovered that a particular system's timezone can be altered and affect
     * the WAS.ras component. Namely TraceLoggers and variants such as Memory Trace Dump
     * are tools that get affected by a change in timezone. This is both a design
     * issue for WAS.ras and a limitation on the JVM since there should be security wrappers
     * for altering TimeZone.setDefault() and TimeZone.getDefault(). Other requirements
     * not currently implemented include Application Context for particular variables
     * such as timezone.
     */
    private static TimeZone sysTimeZone = TimeZone.getDefault();
    // PK13288 End.

    //PK42263 - added new static variable for date formatter
    public final static DateFormat svFormatter = getBasicDateFormatter();

    // Instance attributes
    public Date ivDate;
    //DateFormat ivFormatter; // PK42263
    public FieldPosition ivFieldPos;

    /**
     * Obtain a FormatSet object.
     * <p>
     * If one cannot be obtained from the cache, new one up.
     */
    public static synchronized FormatSet getFormatSet() {
        if (svNumberCached == 0)
            return new FormatSet();
        else {
            FormatSet fs = svInstances.remove(0);
            --svNumberCached;
            return fs;
        }
    }

    /**
     * Return a FormatSet object.
     * <p>
     * Object may be cached or discarded.
     */
    public static synchronized void returnFormatSet(FormatSet fs) {
        if (svNumberCached < svMaxCacheSize) {
            svInstances.addElement(fs);
            svNumberCached++;
        }
    }

    /**
     * Constructor is private.
     */
    private FormatSet() {
        ivDate = new Date();
        ivFieldPos = new FieldPosition(0);
        // PK13288 Start
        //ivFormatter = getBasicDateFormatter(); // PK42263 removed - replaces by static variable
        // PK13288 End.

    }

    /**
     * Return a DateFormat object that can be used to format timestamps in the
     * System.out, System.err and TraceOutput logs. It will use the default date
     * format.
     */
    public static DateFormat getBasicDateFormatter() { // PK42263 - made static
        // Retrieve a standard Java DateFormat object with desired format, using default locale
        return customizeDateFormat(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM), false);
    }

    /**
     * Modifies an existing DateFormat object so that it can be used to format timestamps in the
     * System.out, System.err and TraceOutput logs using either default date and time format or
     * ISO-8601 date and time format
     *
     * @param formatter DateFormat object to be modified
     * @param flag to use ISO-8601 date format for output.
     *
     * @return DateFormat object with adjusted pattern
     */
    public static DateFormat customizeDateFormat(DateFormat formatter, boolean isoDateFormat) {
        String pattern;
        int patternLength;
        int endOfSecsIndex;

        if (!!!isoDateFormat) {
            if (formatter instanceof SimpleDateFormat) {
                // Retrieve the pattern from the formatter, since we will need to modify it.
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
                formatter = new SimpleDateFormat("yy.MM.dd HH:mm:ss:SSS z");
            }

        } else {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        }
        // PK13288 Start -
        if (sysTimeZone != null) {
            formatter.setTimeZone(sysTimeZone);
        }
        // PK13288 End
        return formatter;
    }

    /**
     * Modifies an existing DateFormat object so that it can be used to format timestamps in the
     * System.out, System.err and TraceOutput logs using default date and time format.
     *
     * @param formatter DateFormat object to be modified.
     *
     * @return DateFormat object with adjusted pattern
     */
    public static DateFormat customizeDateFormat(DateFormat formatter) {
        return customizeDateFormat(formatter, false);
    }

}
