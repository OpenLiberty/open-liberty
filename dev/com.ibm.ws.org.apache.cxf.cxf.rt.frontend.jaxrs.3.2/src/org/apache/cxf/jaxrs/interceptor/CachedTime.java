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
// Liberty Change for CXF Begin
package org.apache.cxf.jaxrs.interceptor;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cxf.jaxrs.utils.HttpUtils;

class CachedTime {
    /** Stored formatter */
    private SimpleDateFormat myFormat = null;

    /** Last time we formatted a value */
    private long lastTimeCheck = 0L;
    /** The stored formatted time as a string */
    private String sTime = null;
//    /** The stored formatted time as byte[] */
//    private final byte[] baTime = new byte[0];
//    /** The stored formatted time as a char[] */
//    private final char[] caTime = new char[0];
    /** Static date object used for all conversions */
    private final Date myDate = new Date();
    /** Buffer that the formatted puts output into */
    private StringBuffer myBuffer = new StringBuffer(33);

    private static CachedTime instance = new CachedTime(HttpUtils.getHttpDateFormat());

    protected static final long DEFAULT_TOLERANCE = 1000L;

    /**
     * Create a cachedTime instance with the given format.
     * <br>
     * 
     * @param format
     */

    protected CachedTime(SimpleDateFormat format) {
        this.myFormat = format;
    }

    public static CachedTime getCachedTime() {
        return instance;
    }

    /**
     * Access the formatter for this particular cached time instance.
     * 
     * @return SimpleDateFormat
     */
    protected SimpleDateFormat getFormat() {
        return this.myFormat;
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
        this.myDate.setTime(now);
        this.myBuffer.setLength(0);
        this.myBuffer = this.myFormat.format(this.myDate, this.myBuffer, new FieldPosition(0));
//        int len = this.myBuffer.length();
//
//        // extract the char[] of the time and save the byte[] equivalent
//        if (this.caTime.length != len) {
//            // both arrays will always have the same length
//            this.caTime = new char[len];
//            this.baTime = new byte[len];
//        }
//        this.myBuffer.getChars(0, len, this.caTime, 0);
//        for (int i = 0; i < len; i++) {
//            this.baTime[i] = (byte) this.caTime[i];
//        }
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
//    protected byte[] getTimeAsBytes(long tolerance) {
//        updateTime(tolerance);
//        return this.baTime;
//    }

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
        // see if we need the delayed string creation at this point
        if (null == this.sTime) {
            this.sTime = this.myBuffer.toString();
        }
        return this.sTime;
    }
}
//Liberty Change for CXF End
