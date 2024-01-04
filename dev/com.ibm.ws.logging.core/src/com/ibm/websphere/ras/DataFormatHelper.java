/*******************************************************************************
 * Copyright (c) 2010, 2023 IBM Corporation and others.
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
package com.ibm.websphere.ras;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Locale.Category;

/**
 * Utilities which provide formatting for date strings (NCSA compliant dates
 * used in trace records), and padded thread ids, etc.
 */
public class DataFormatHelper {
    /** Formatted thread ids */
    private static ThreadLocal<String> threadids = new ThreadLocal<String>();

    private static final DateTimeFormatter localeDateFormatter;

    private static final DateTimeFormatter isoDateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ").withZone(ZoneId.systemDefault());

    /**
     * Computes a format string that will produce a reasonable standard way for
     * formatting time (but still using the current locale)
     */
    static {
        // Retrieve a standard Java DateFormat object with desired format.
        Locale locale = Locale.getDefault(Category.FORMAT);
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, FormatStyle.MEDIUM, Chronology.ofLocale(locale), locale);
        // Append milliseconds and timezone after seconds
        int patternLength = pattern.length();
        int endOfSecsIndex = pattern.lastIndexOf('s') + 1;
        String newPattern = pattern.substring(0, endOfSecsIndex) + ":SSS z";
        if (endOfSecsIndex < patternLength)
            newPattern += pattern.substring(endOfSecsIndex, patternLength);
        // 0-23 hour clock (get rid of any other clock formats and am/pm)
        newPattern = newPattern.replace('h', 'H');
        newPattern = newPattern.replace('K', 'H');
        newPattern = newPattern.replace('k', 'H');
        newPattern = newPattern.replace('a', ' ');
        // Java 20 added a narrow no-break space character into the format (Unicode 202F character)
        newPattern = newPattern.replace('\u202f', ' ');
        newPattern = newPattern.trim();
        pattern = newPattern;
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().appendPattern(pattern);
        localeDateFormatter = builder.toFormatter().withZone(ZoneId.systemDefault()).withLocale(locale).withChronology(Chronology.ofLocale(locale)).withDecimalStyle(DecimalStyle.of(locale));
    }

    /**
     * Returns a string containing a concise, human-readable description of the
     * object. The string is the same as the one that would be returned by
     * Object.toString even if the object's class has overriden the toString
     * or hashCode methods. The return value for a null object is null.
     *
     * @param o the object
     * @return the string representation
     */
    public static String identityToString(Object o) {
        return o == null ? null : o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o));
    }

    /**
     * Returns a string containing a concise, human-readable description of the
     * object. The result is similar to {@link #identityToString} but indicates
     * that the contents of the object are masked because they are sensitive.
     *
     * @param o the object
     * @return a string representation
     */
    public static String sensitiveToString(Object o) {
        return o == null ? null : "<sensitive " + o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o)) + '>';
    }

    /**
     * Return the current time formatted as dd/MMM/yyyy HH:mm:ss:SSS Z
     *
     * @return formated date string
     */
    public static final String formatCurrentTime() {
        return '[' + localeDateFormatter.format(ZonedDateTime.now()) + ']';
    }

    /**
     * Return the given time formatted as dd/MMM/yyyy HH:mm:ss:SSS Z
     *
     * @param timestamp
     *            A timestamp as a long, e.g. what would be returned from
     *            <code>System.currentTimeMillis()</code>
     * @return formated date string
     */
    public static final String formatTime(long timestamp) {
        return localeDateFormatter.format(Instant.ofEpochMilli(timestamp));
    }
    
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
        Instant instant = Instant.ofEpochMilli(timestamp);
        return useIsoDateFormat ? isoDateFormatter.format(instant) : localeDateFormatter.format(instant);
    }

    /**
     * Get and return the thread id, padded to 8 characters.
     *
     * @return 8 character string representation of thread id
     */
    public static final String getThreadId() {
        String id = threadids.get();
        if (null == id) {
            // Pad the tid out to 8 characters
            id = getThreadId(Thread.currentThread());
            threadids.set(id);
        }
        return id;
    }

    /**
     * Get and return the thread id, padded to 8 characters.
     *
     * @param thread the specified thread
     * @return the string representation of the thread id
     */
    public static final String getThreadId(Thread thread) {
        return DataFormatHelper.padHexString((int) thread.getId(), 8);
    }

    /**
     * Returns the provided integer, padded to the specified number of characters with zeros.
     *
     * @param num
     *            Input number as an integer
     * @param width
     *            Number of characters to return, including padding
     * @return input number as zero-padded string
     */
    public static final String padHexString(int num, int width) {
        final String zeroPad = "0000000000000000";

        String str = Integer.toHexString(num);
        final int length = str.length();

        if (length >= width)
            return str;

        StringBuilder buffer = new StringBuilder(zeroPad.substring(0, width));
        buffer.replace(width - length, width, str);
        return buffer.toString();
    }

    /**
     * Returns a string containing the formatted exception stack
     *
     * @param t throwable
     *
     * @return formatted exception stack as a string
     */
    public static final String throwableToString(Throwable t) {
        final StringWriter s = new StringWriter();
        final PrintWriter p = new PrintWriter(s);

        if (t == null) {
            p.println("none");
        } else {
            printStackTrace(p, t);
        }

        return DataFormatHelper.escape(s.toString());
    }

    private static final void printStackTrace(PrintWriter p, Throwable t) {
        t.printStackTrace(p);

        if (printFieldStackTrace(p, t, "org.omg.CORBA.portable.UnknownException", "originalEx")) {
            return;
        }

        // Other candidate that have custom linked exceptions not currently
        // returned by getCause():
        // - SAXException.getException()
        // - SQLException.getNextException()
        // - JMSException.getLinkedException()
    }

    /**
     * Find a field value in the class hierarchy of an exception, and if the
     * field contains another Throwable, then print its stack trace.
     *
     * @param p the writer to print to
     * @param t the outer throwable
     * @param className the name of the class to look for
     * @param fieldName the field in the class to look for
     * @return true if the field was found
     */
    private static final boolean printFieldStackTrace(PrintWriter p, Throwable t, String className, String fieldName) {
        for (Class<?> c = t.getClass(); c != Object.class; c = c.getSuperclass()) {
            if (c.getName().equals(className)) {
                try {
                    Object value = c.getField(fieldName).get(t);
                    if (value instanceof Throwable && value != t.getCause()) {
                        p.append(fieldName).append(": ");
                        printStackTrace(p, (Throwable) value);
                    }
                    return true;
                } catch (NoSuchFieldException e) {
                } catch (IllegalAccessException e) {
                }
            }
        }

        return false;
    }

    /**
     * Escapes characters in the input string that would interfere with formatting
     *
     * @param src input string to be escaped
     *
     * @return escaped string
     */
    public final static String escape(String src) {
        if (src == null)
            return "";

        StringBuilder result = null;
        for (int i = 0, max = src.length(), delta = 0; i < max; i++) {
            char c = src.charAt(i);
            if (!Character.isWhitespace(c) && Character.isISOControl(c) || Character.getType(c) == Character.UNASSIGNED) {
                String hexVal = Integer.toHexString(c);
                String replacement = "\\u" + ("0000" + hexVal).substring(hexVal.length());
                if (result == null) {
                    result = new StringBuilder(src);
                }
                result.replace(i + delta, i + delta + 1, replacement);
                delta += replacement.length() - 1;
            }
        }

        if (result == null)
            return src;
        else
            return result.toString();
    }

}
