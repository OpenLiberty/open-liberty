/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
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
package com.ibm.ws.fat.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Helps you track the amount of time that passes between significant events.
 *
 * @author Tim Burns
 *
 */
public class StopWatch {

    /**
     * The default DateFormat used to format Dates as Strings
     */
    public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss:SSS z]");

    protected static final String SPACE = " ";
    protected static final String S = "s";
    protected static final String DAY = "day";
    protected static final String HOUR = "hour";
    protected static final String MINUTE = "minute";
    protected static final String SECOND = "second";
    protected static final String MILLISECOND = "millisecond";
    protected static final String TINY = "Less than 1 millisecond";

    protected long start;
    protected long stop;
    protected boolean started;
    protected boolean stopped;

    /**
     * Primary constructor.
     */
    public StopWatch() {
        this.start = 0;
        this.stop = 0;
        this.started = false;
        this.stopped = false;
    }

    /**
     * Capture the current system time, and store it as the internal start time.
     */
    public void start() {
        this.start = getCurrentTimeInMilliseconds();
        this.started = true;
        this.stopped = false;
    }

    /**
     * Determines if this instance has been started, but not stopped
     * 
     * @return true if this instance has been started, but not stopped
     */
    public boolean hasStarted() {
        return this.started;
    }

    /**
     * Determines the time that this instance was started, in milliseconds
     * 
     * @return the difference, measured in milliseconds, between the time that
     *         this instance was started and midnight, January 1, 1970 UTC.
     */
    public long getStartTimeInMilliseconds() {
        return this.start;
    }

    /**
     * Determines the time that this instance was started
     * 
     * @return A GregorianCalendar that represents the time that this instance
     *         was started.
     */
    public GregorianCalendar getStartTimeAsCalendar() {
        return getTimeAsCalendar(this.start);
    }

    /**
     * Capture the current system time, and store it as the internal stop time.
     */
    public void stop() {
        this.stop = getCurrentTimeInMilliseconds();
        this.started = false;
        this.stopped = true;
    }

    /**
     * Determines if this instance has been stopped, after being started at
     * least once
     * 
     * @return true if this instance has been stopped, after being started at
     *         least once
     */
    public boolean hasStopped() {
        return this.stopped;
    }

    /**
     * Determines the time that this instance was stopped, in milliseconds
     * 
     * @return the difference, measured in milliseconds, between the time that
     *         this instance was stopped and midnight, January 1, 1970 UTC.
     */
    public long getStopTimeInMilliseconds() {
        return this.stop;
    }

    /**
     * Determines the time that this instance was stopped
     * 
     * @return A GregorianCalendar that represents the time that this instance
     *         was stopped.
     */
    public GregorianCalendar getStopTimeAsCalendar() {
        return getTimeAsCalendar(this.stop);
    }

    /**
     * Calculate the time elapsed between starting and stopping this instance,
     * as a human-readable String
     * 
     * @return the number of milliseconds that elapsed between the start time
     *         and the stop time
     */
    public String getTimeElapsedAsString() {
        return getTimeElapsedAsString(this.start, this.stop);
    }

    /**
     * Calculate the time elapsed between starting and stopping this instance.
     * 
     * @return the number of milliseconds that elapsed between the start time
     *         and the stop time
     */
    public long getTimeElapsedAsLong() {
        return getTimeElapsedAsLong(this.start, this.stop);
    }

    /**
     * Causes the current Thread to sleep for the specified number of minutes.
     * If the current Thread is interrupted while it's sleeping, this method
     * will return immediately.
     * 
     * @param minutes The number of minutes you want the current Thread to sleep
     */
    public static void sleepMinutes(long minutes) {
        sleepMilliseconds(minutes * 60 * 1000);
    }

    /**
     * Causes the current Thread to sleep for the specified number of seconds.
     * If the current Thread is interrupted while it's sleeping, this method
     * will return immediately.
     * 
     * @param seconds The number of seconds you want the current Thread to sleep
     */
    public static void sleepSeconds(long seconds) {
        sleepMilliseconds(seconds * 1000);
    }

    /**
     * Causes the current Thread to sleep for the specified number of
     * milliseconds. If the current Thread is interrupted while it's sleeping,
     * this method will return immediately.
     * 
     * @param milliseconds
     *                         The number of milliseconds you want the current Thread to
     *                         sleep
     */
    public static void sleepMilliseconds(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            return;
        }
    }

    /**
     * Calculate the time elapsed between the input start and stop times,
     * as a human-readable String
     * 
     * @param  start
     *                   the start time
     * @param  stop
     *                   the stop time
     * @return       the number of milliseconds that elapsed between the start time
     *               and the stop time
     */
    public static String getTimeElapsedAsString(long start, long stop) {
        return convertMillisecondsToString(stop - start);
    }

    /**
     * Calculate the time elapsed between the input start and stop times.
     * 
     * @param  start
     *                   the start time
     * @param  stop
     *                   the stop time
     * @return       the number of milliseconds that elapsed between the start time
     *               and the stop time
     */
    public static long getTimeElapsedAsLong(long start, long stop) {
        return stop - start;
    }

    protected static long getCurrentTimeInMilliseconds() {
        return System.currentTimeMillis();
    }

    /**
     * Determines the current time
     * 
     * @return A GregorianCalendar that represents the current time
     */
    public static GregorianCalendar getCurrentTimeAsCalendar() {
        return getTimeAsCalendar(getCurrentTimeInMilliseconds());
    }

    /**
     * Converts the specified time in milliseconds to the same time as a
     * GregorianCalendar
     * 
     * @param  millisecondsSinceEpoch
     *                                    the time you want to convert
     * @return                        A GregorianCalendar that represents the input time
     */
    public static GregorianCalendar getTimeAsCalendar(long millisecondsSinceEpoch) {
        GregorianCalendar timestamp = new GregorianCalendar();
        timestamp.setTimeInMillis(millisecondsSinceEpoch);
        return timestamp;
    }

    /**
     * Produces a human-readable String representation of elapsed time, in
     * milliseconds
     * 
     * @param  milliseconds
     *                          the amount of time elapsed, in milliseconds
     * @return              A human-readable representation of the elapsed time
     */
    public static String convertMillisecondsToString(long milliseconds) {
        return UnitConverter.millisecondsAsString(milliseconds);
    }

    /**
     * Format the input Date as a String using the default date format.
     * 
     * @param  date
     *                  The date you want to convert
     * @return      A String description of the input date, or null if the input Date
     *              is null
     */
    public static String formatTime(Date date) {
        return formatTime(null, date);
    }

    /**
     * Format the input Date as a String using the input date format.
     * 
     * @param  format
     *                    the format you want to use to format the Date, or null if you
     *                    want to use the default date format
     * @param  date
     *                    The date you want to convert
     * @return        A String description of the input date, or null if the input Date
     *                is null
     */
    public static String formatTime(SimpleDateFormat format, Date date) {
        if (date == null) {
            return null;
        }
        return formatTime(format, date.getTime());
    }

    /**
     * Format the input Date as a String using the default date format.
     * 
     * @param  millisecondsSinceEpoch
     *                                    The date you want to convert, in milliseconds since midnight,
     *                                    January 1, 1970 UTC
     * @return                        A String description of the input date
     */
    public static String formatTime(long millisecondsSinceEpoch) {
        return formatTime(null, millisecondsSinceEpoch);
    }

    /**
     * Format the input Date as a String using the input date format.
     * 
     * @param  format
     *                                    the format you want to use to format the Date, or null if you
     *                                    want to use the default date format
     * @param  millisecondsSinceEpoch
     *                                    The date you want to convert, in milliseconds since midnight,
     *                                    January 1, 1970 UTC
     * @return                        A String description of the input date, or null if the input Date
     *                                is null
     */
    public static String formatTime(SimpleDateFormat format, long millisecondsSinceEpoch) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(millisecondsSinceEpoch);
        if (format == null) {
            return DEFAULT_DATE_FORMAT.format(calendar.getTime());
        }
        return format.format(calendar.getTime());
    }

    /**
     * Convert the input String to a Date using the default date format.
     * 
     * @param  time
     *                  The time you want to convert to a Date
     * @return      A Date representation of the input time, or null if the input
     *              String is invalid
     */
    public static Date parseTime(String time) {
        return parseTime(null, time);
    }

    /**
     * Convert the input String to a Date using the input date format.
     * 
     * @param  format
     *                    the format you want to use to parse the Date, or null if you
     *                    want to use the default date format
     * @param  time
     *                    The time you want to convert to a Date
     * @return        A Date representation of the input time, or null if the input
     *                String is invalid
     */
    public static Date parseTime(SimpleDateFormat format, String time) {
        try {
            if (format == null) {
                return DEFAULT_DATE_FORMAT.parse(time);
            }
            return format.parse(time);
        } catch (ParseException e) {
            return null;
        }
    }

}
