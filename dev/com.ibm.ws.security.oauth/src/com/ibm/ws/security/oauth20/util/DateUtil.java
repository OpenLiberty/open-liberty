/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.SimpleTimeZone;

/**
 *
 */
public class DateUtil {

    /**
     * Date format used by internal formatting methods.  Not to be handed out.
     */
    private static final SimpleDateFormat _RFC822DateFormatInternal;

    /**
     * Date format used by internal formatting methods.  Not to be handed out.
     */
    private static final SimpleDateFormat _RFC850DateFormatInternal;

    /**
     * Date format used by internal formatting methods.  Not to be handed out.
     */
    private static final SimpleDateFormat _ANSICDateFormatInternal;

    /**
     * GMT time zone used by internal formatting methods.  Not to be handed out.
     */
    private static final SimpleTimeZone _gmtTimeZoneInternal;

    /**
     * Returns a timestamp value for date/time specified in <code>time</code>.
     * The value must be in one of the formats specified in RFC2616 Section 3.3.1.</p>
     *
     * According to RFC2616 Section 3.3.1:</p>
     * <code>
     *  HTTP applications have historically allowed three different formats for the representation of date/time stamps:
     *       Sun, 06 Nov 1994 08:49:37 GMT  ; RFC 822, updated by RFC 1123
     *       Sunday, 06-Nov-94 08:49:37 GMT ; RFC 850, obsoleted by RFC 1036
     *       Sun Nov  6 08:49:37 1994       ; ANSI C's asctime() format
     *  ... HTTP/1.1 clients and servers that parse the date value MUST accept all three formats
     * </code></p>
     *
     * The time is parsed assuming an English locale ({@link Locale#ENGLISH}).
     *
     * @param time A string representing the time value. May not be <code>null</code>.
     *
     * @return The parsed date/time value.  <code>null</code> if invalid format.
     */
    public static Timestamp parseTimeRFC2616(String time) {
        if (time == null)
            throw new IllegalArgumentException("time must not be null"); //$NON-NLS-1$

        try {
            return DateUtil.parseTimeRFC822(time);
        } catch (ParseException e) {/* ignore */
        }

        try {
            return DateUtil.parseTimeRFC850(time);
        } catch (ParseException e) {/* ignore */
        }

        try {
            return DateUtil.parseTimeANSIC(time);
        } catch (ParseException e) {/* ignore */
        }

        return null;
    }

    /**
     * Returns a timestamp value for date/time specified in <code>time</code>.
     * The value must be in RFC822, with a 4 digit year, with a trailing 'z' signifying
     * that the time is relative to UTC.
     *
     * This method is synchronized. <p/>
     *
     * The time is parsed assuming an English locale ({@link Locale#ENGLISH}).
     *
     * @param time A string representing the time value, formatted as
     * the format pattern returned by {@link #formatTimeRFC822(Date)}.
     * May not be <code>null</code>.
     *
     * @return The parsed date/time value.  Never <code>null</code>.
     *
     * @throws ParseException Thrown on date parsing errors.
     */
    public static synchronized Timestamp parseTimeRFC822(String time) throws ParseException {
        if (time == null)
            throw new IllegalArgumentException("time must not be null"); //$NON-NLS-1$

        return new Timestamp(DateUtil._RFC822DateFormatInternal.parse(time).getTime());
    }

    /**
     * Returns a timestamp value for date/time specified in <code>time</code>.
     * The value must be in RFC850.
     *
     * This method is synchronized. <p/>
     *
     * The time is parsed assuming an English locale ({@link Locale#ENGLISH}).
     *
     * @param time A string representing the time value. May not be <code>null</code>.
     *
     * @return The parsed date/time value.  Never <code>null</code>.
     *
     * @throws ParseException Thrown on date parsing errors.
     */
    public static synchronized Timestamp parseTimeRFC850(String time) throws ParseException {
        if (time == null)
            throw new IllegalArgumentException("time must not be null"); //$NON-NLS-1$

        return new Timestamp(DateUtil._RFC850DateFormatInternal.parse(time).getTime());
    }

    /**
     * Returns a timestamp value for date/time specified in <code>time</code>.
     * The value must be in ANSI C's asctime() format.
     *
     * This method is synchronized. <p/>
     *
     * The time is parsed assuming an English locale ({@link Locale#ENGLISH}).
     *
     * @param time A string representing the time value. May not be <code>null</code>.
     *
     * @return The parsed date/time value.  Never <code>null</code>.
     *
     * @throws ParseException Thrown on date parsing errors.
     */
    public static synchronized Timestamp parseTimeANSIC(String time) throws ParseException {
        if (time == null)
            throw new IllegalArgumentException("time must not be null"); //$NON-NLS-1$

        return new Timestamp(DateUtil._ANSICDateFormatInternal.parse(time).getTime());
    }

    static {
        _gmtTimeZoneInternal = new SimpleTimeZone(0, "GMT"); //$NON-NLS-1$

        _RFC822DateFormatInternal = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH); //$NON-NLS-1$
        DateUtil._RFC822DateFormatInternal.setTimeZone(DateUtil._gmtTimeZoneInternal);

        _RFC850DateFormatInternal = new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z", Locale.ENGLISH); //$NON-NLS-1$
        DateUtil._RFC850DateFormatInternal.setTimeZone(DateUtil._gmtTimeZoneInternal);

        _ANSICDateFormatInternal = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH); //$NON-NLS-1$
        DateUtil._ANSICDateFormatInternal.setTimeZone(DateUtil._gmtTimeZoneInternal);
    }

}
