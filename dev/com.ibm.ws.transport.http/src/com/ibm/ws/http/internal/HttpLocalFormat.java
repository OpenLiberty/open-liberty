/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;

/**
 * Class that encapsulates an instance of the date formatter for each of the
 * supported HTTP date formats. This object is intended to be stored at the
 * thread layer for quick, thread-safe usage of the parsing code.
 */
public class HttpLocalFormat {

    /** Default tolerance range is within one second */
    static final long DEFAULT_TOLERANCE = 1000L;
    /** Ref to the GMT timezone */
    static final TimeZone gmt = TimeZone.getTimeZone("GMT");

    /** Cached RFC 1123 format timer */
    private CachedTime c1123Time = null;
    /** Cached RFC 1036 format timer */
    private CachedTime c1036Time = null;
    /** Cached ASCII format timer */
    private CachedTime cAsciiTime = null;
    /** Cached NCSA format timer */
    private CachedTime cNCSATime = null;
    /** Cached RFC 2109 format timer */
    private CachedTime c2109Time = null;

    /**
     * Create an instance of this class.
     * Create the CachedTimes lazily only if they are needed in order to reduce memory.
     */
    public HttpLocalFormat() {}

    private CachedTime getC1123Time() {
        if (this.c1123Time == null) {
            this.c1123Time = new CachedTime("EEE, dd MMM yyyy HH:mm:ss z", true);
        }
        return this.c1123Time;
    }

    private CachedTime getC1036Time() {
        if (this.c1036Time == null) {
            this.c1036Time = new CachedTime("EEEEEEEEE, dd-MMM-yy HH:mm:ss z", true);
        }
        return this.c1036Time;
    }

    private CachedTime getCAsciiTime() {
        if (this.cAsciiTime == null) {
            this.cAsciiTime = new CachedTime("EEE MMM  d HH:mm:ss yyyy", true);
        }
        return this.cAsciiTime;
    }

    private CachedTime getCNCSATime() {
        if (this.cNCSATime == null) {
            this.cNCSATime = new CachedTime("dd/MMM/yyyy:HH:mm:ss Z", false);
        }
        return this.cNCSATime;
    }

    private CachedTime getC2109Time() {
        if (this.c2109Time == null) {
            this.c2109Time = new CachedTime("EEE, dd-MMM-yy HH:mm:ss z", true);
        }
        return this.c2109Time;
    }

    /**
     * Get the current time formatted for RFC 1123.
     * <br>
     * EEE, dd MMM yyyy HH:mm:ss z
     * <br>
     *
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return byte[]
     */
    public byte[] get1123TimeAsBytes(long range) {
        return this.getC1123Time().getTimeAsBytes(range);
    }

    /**
     * Get the current time formatted for RFC 1123.
     * <br>
     * EEE, dd MMM yyyy HH:mm:ss z
     * <br>
     *
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return String
     */
    public String get1123TimeAsString(long range) {
        return this.getC1123Time().getTimeAsString(range);
    }

    /**
     * Get the current time formatted for RFC 1036.
     * <br>
     * EEEEEEEEE, dd-MMM-yy HH:mm:ss z
     * <br>
     *
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return byte[]
     */
    public byte[] get1036TimeAsBytes(long range) {
        return this.getC1036Time().getTimeAsBytes(range);
    }

    /**
     * Get the current time formatted for RFC 1036.
     * <br>
     * EEEEEEEEE, dd-MMM-yy HH:mm:ss z
     * <br>
     *
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return String
     */
    public String get1036TimeAsString(long range) {
        return this.getC1036Time().getTimeAsString(range);
    }

    /**
     * Get the current time formatted for standard ASCII.
     * <br>
     * EEE MMM d HH:mm:ss yyyy
     * <br>
     *
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return byte[]
     */
    public byte[] getAsciiTimeAsBytes(long range) {
        return this.getCAsciiTime().getTimeAsBytes(range);
    }

    /**
     * Get the current time formatted for standard ASCII.
     * <br>
     * EEE MMM d HH:mm:ss yyyy
     * <br>
     *
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return String
     */
    public String getAsciiTimeAsString(long range) {
        return this.getCAsciiTime().getTimeAsString(range);
    }

    /**
     * Get the current time formatted for NCSA.
     * <br>
     * dd/MMM/yyyy:HH:mm:ss Z
     * <br>
     *
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return byte[]
     */
    public byte[] getNCSATimeAsBytes(long range) {
        return this.getCNCSATime().getTimeAsBytes(range);
    }

    /**
     * Get the current time formatted for NCSA.
     * <br>
     * dd/MMM/yyyy:HH:mm:ss Z
     * <br>
     *
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return String
     */
    public String getNCSATimeAsString(long range) {
        return this.getCNCSATime().getTimeAsString(range);
    }

    /**
     * Get the current time formatted for RFC 2109 (Cookie).
     * <br>
     * EEE, dd-MMM-YY HH:mm:ss GMT
     * <br>
     *
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return byte[]
     */
    public byte[] get2109TimeAsBytes(long range) {
        return this.getC2109Time().getTimeAsBytes(range);
    }

    /**
     * Get the current time formatted for RFC 2109 (Cookie).
     * <br>
     * EEE, dd-MMM-YY HH:mm:ss GMT
     * <br>
     *
     * @param range (milliseconds, -1 means use default, 0 means right now)
     * @return String
     */
    public String get2109TimeAsString(long range) {
        return this.getC2109Time().getTimeAsString(range);
    }

    /**
     * Get access to the RFC 1123 formatter/parser.
     * <br>
     *
     * @return SimpleDateFormat
     */
    public SimpleDateFormat get1123Format() {
        return this.getC1123Time().getFormat();
    }

    /**
     * Get access to the RFC 1036 formatter/parser.
     * <br>
     *
     * @return SimpleDateFormat
     */
    public SimpleDateFormat get1036Format() {
        return this.getC1036Time().getFormat();
    }

    /**
     * Get access to the ASCII formatter/parser.
     * <br>
     *
     * @return SimpleDateFormat
     */
    public SimpleDateFormat getAsciiFormat() {
        return this.getCAsciiTime().getFormat();
    }

    /**
     * Get access to the NCSA style formatter/parser.
     * <br>
     *
     * @return SimpleDateFormat
     */
    public SimpleDateFormat getNCSAFormat() {
        return this.getCNCSATime().getFormat();
    }

    /**
     * Get access to the RFC 2109 formatter/parser.
     *
     * @return SimpleDateFormat
     */
    public SimpleDateFormat get2109Format() {
        return this.getC2109Time().getFormat();
    }

    /**
     * Get access to the RFC 1123 parser.
     * <br>
     *
     * @return SimpleDateFormat
     */
    public SimpleDateFormat get1123Parse() {
        return this.getC1123Time().getParse();
    }

    /**
     * Get access to the RFC 1036 parser.
     * <br>
     *
     * @return SimpleDateFormat
     */
    public SimpleDateFormat get1036Parse() {
        return this.getC1036Time().getParse();
    }

    /**
     * Get access to the ASCII parser.
     * <br>
     *
     * @return SimpleDateFormat
     */
    public SimpleDateFormat getAsciiParse() {
        return this.getCAsciiTime().getParse();
    }

    /**
     * Get access to the NCSA style parser.
     * <br>
     *
     * @return SimpleDateFormat
     */
    public SimpleDateFormat getNCSAParse() {
        return this.getCNCSATime().getParse();
    }

    /**
     * Get access to the RFC 2109 parser.
     *
     * @return SimpleDateFormat
     */
    public SimpleDateFormat get2109Parse() {
        return this.getC2109Time().getParse();
    }

    /**
     * Private class that wraps handling a specific date formatter and keeping
     * a stored formatted byte[]. If the current time is within the target
     * tolerance in milliseconds of the stored time, then the previous formatted
     * byte[] is returned, otherwise the current time is formatted and used for
     * the next tolerance range of time. This class is used at the threadlocal
     * level so no synchronization is required.
     *
     */
    private static class CachedTime {
        private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
        private static final char[] EMPTY_CHAR_ARRAY = new char[0];
        /** Stored formatter */
        private final SimpleDateFormat myFormat;
        private final String pattern;
        private final boolean gmtTimeZone;
        /** Stored Parser */
        private SimpleDateFormat myParse = null;
        /** Last time we formatted a value */
        private long lastTimeCheck = 0L;
        /** The stored formatted time as a string */
        private String sTime = null;
        /** The stored formatted time as byte[] */
        private byte[] baTime = EMPTY_BYTE_ARRAY;
        /** The stored formatted time as a char[] */
        private char[] caTime = EMPTY_CHAR_ARRAY;
        /** Static date object used for all conversions */
        private final Date myDate = new Date();
        /** Buffer that the formatted puts output into */
        private StringBuffer myBuffer = new StringBuffer(33);

        /**
         * Create a cachedTime instance with the given format.
         * <br>
         *
         * @param format
         */
        CachedTime(String format, boolean gmtTimeZone) {
            this.pattern = format;
            this.gmtTimeZone = gmtTimeZone;
            this.myFormat = new SimpleDateFormat(format, Locale.US);
            if (gmtTimeZone) {
                this.myFormat.setTimeZone(gmt);
            }
        }

        /**
         * Access the formatter for this particular cached time instance.
         *
         * @return SimpleDateFormat
         */
        SimpleDateFormat getFormat() {
            return this.myFormat;
        }

        /**
         * Access the Parser for this particular cached time instance.
         *
         * @return SimpleDateFormat
         */
        SimpleDateFormat getParse() {
            if (this.myParse == null) {
                this.myParse = new SimpleDateFormat(pattern);
                if (gmtTimeZone) {
                    this.myParse.setTimeZone(gmt);
                }
            }
            return this.myParse;
        }

        /**
         * Utility method to determine whether to use the cached time value or
         * update to a newly formatted timestamp.
         *
         * @param tolerance
         */
        private void updateTime(long tolerance) {
            long now = HttpDispatcher.getApproxTime();
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
            this.myDate.setTime(now);
            this.myBuffer.setLength(0);
            this.myBuffer = this.myFormat.format(this.myDate, this.myBuffer, new FieldPosition(0));
            int len = this.myBuffer.length();

            // extract the char[] of the time and save the byte[] equivalent
            if (this.caTime.length != len) {
                // both arrays will always have the same length
                this.caTime = new char[len];
                this.baTime = new byte[len];
            }
            this.myBuffer.getChars(0, len, this.caTime, 0);
            for (int i = 0; i < len; i++) {
                this.baTime[i] = (byte) this.caTime[i];
            }
            // delay the string creation until it's actually needed
            this.sTime = null;

            this.lastTimeCheck = now;
        }

        /**
         * Get a formatted version of the time as a byte[]. The input range is
         * the allowed difference in time from the cached snapshot that the
         * caller is willing to use. If that range is exceeded, then a new
         * snapshot is taken and formatted.
         * <br>
         *
         * @param tolerance -- milliseconds, -1 means use default 1000ms, a 0
         *            means that this must be an exact match in time
         * @return byte[]
         */
        byte[] getTimeAsBytes(long tolerance) {
            updateTime(tolerance);
            return this.baTime;
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
        String getTimeAsString(long tolerance) {
            updateTime(tolerance);
            // see if we need the delayed string creation at this point
            if (null == this.sTime) {
                this.sTime = new String(this.caTime, 0, this.caTime.length);
            }
            return this.sTime;
        }
    }
}