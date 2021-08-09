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
package com.ibm.ws.concurrent.persistent.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.TimeZone;

import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Utility methods.
 */
@Trivial
public class Utils {
    /**
     * Prints a date. We do not use java.util.DateFormat because it is not thread safe.
     * The format is text of the form 2014/06/03-08:48:00.000-CDT
     * 
     * @param sb buffer for output.
     * @param millis milliseconds.
     * @return the buffer.
     */
    public static final StringBuilder appendDate(StringBuilder sb, Long millis) {
        if (millis == null)
            return sb.append((Long) null);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        TimeZone zone = cal.getTimeZone();
        sb.append(cal.get(Calendar.YEAR)).append('/');
        int m = cal.get(Calendar.MONTH);
        m++; // Month range is 0 - 11. Bump one for formatting
        if (m < 10)
            sb.append('0');
        sb.append(m).append('/');
        int d = cal.get(Calendar.DAY_OF_MONTH);
        if (d < 10)
            sb.append('0');
        sb.append(d).append('-');
        int h = cal.get(Calendar.HOUR_OF_DAY);
        if (h < 10)
            sb.append('0');
        sb.append(h).append(':');
        m = cal.get(Calendar.MINUTE);
        if (m < 10)
            sb.append('0');
        sb.append(m).append(':');
        int s = cal.get(Calendar.SECOND);
        if (s < 10)
            sb.append('0');
        sb.append(s).append('.');
        int ms = cal.get(Calendar.MILLISECOND);
        if (ms < 100)
            sb.append('0');
        if (ms < 10)
            sb.append('0');
        sb.append(ms).append('-');
        sb.append(zone.getDisplayName(zone.inDaylightTime(cal.getTime()), TimeZone.SHORT));
        sb.append('[').append(millis).append(']');
        return sb;
    }

    /**
     * Prints a state.
     * 
     * @param sb buffer for output.
     * @param bits state bits.
     * @return the buffer
     */
    public static final void appendState(StringBuilder sb, short bits) {
        // These 3 states are mutually exclusive.
        if ((bits & TaskState.SCHEDULED.bit) != 0)
            sb.append(TaskState.SCHEDULED.name()).append(',');
        if ((bits & TaskState.ENDED.bit) != 0)
            sb.append(TaskState.ENDED.name()).append(',');
        if ((bits & TaskState.SUSPENDED.bit) != 0)
            sb.append(TaskState.SUSPENDED.name()).append(',');

        // Other states that can be combined with the above
        if ((bits & TaskState.UNATTEMPTED.bit) != 0)
            sb.append(TaskState.UNATTEMPTED.name()).append(',');
        if ((bits & TaskState.SKIPPED.bit) != 0)
            sb.append(TaskState.SKIPPED.name()).append(',');
        if ((bits & TaskState.SKIPRUN_FAILED.bit) != 0)
            sb.append(TaskState.SKIPRUN_FAILED.name()).append(',');
        if ((bits & TaskState.FAILURE_LIMIT_REACHED.bit) != 0)
            sb.append(TaskState.FAILURE_LIMIT_REACHED.name()).append(',');
        if ((bits & TaskState.CANCELED.bit) != 0)
            sb.append(TaskState.CANCELED.name()).append(',');
        sb.deleteCharAt(sb.length() - 1);
    }

    /**
     * Returns a string representing the specified instance, which includes at least the class name and hash code.
     * 
     * @param obj instance of an object.
     * @return a string representing the specified instance, which includes at least the class name and hash code.
     */
    @Trivial
    public static final String toString(Object obj) {
        if (obj == null)
            return null;

        String result = obj.toString();
        if (result.indexOf('@') > 0 && result.contains(obj.getClass().getSimpleName()))
            return result;
        else
            return obj.getClass().getName() + '@' + Integer.toHexString(obj.hashCode()) + ": " + result;
    }

    /**
     * Normalizes an empty or a null string into a space string.
     * 
     * @param str The string to normalize.
     * @return The normalized string.
     */
    @Trivial
    public static final String normalizeString(String str) {
        return (str == null || str.length() == 0) ? " " : str;
    }

    private static final String EOLN = String.format("%n");

    /**
     * Formats an exception's stack trace as a String.
     * 
     * @param th a throwable object (Exception or Error)
     * 
     * @return String containing the exception's stack trace.
     */
    public static String stackTraceToString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (int depth = 0; depth < 10 && th != null; depth++) {
            th.printStackTrace(pw);
            Throwable cause = th.getCause();
            if (cause != null && cause != th) {
                pw.append("-------- chained exception -------").append(EOLN);
            }
            th = cause;
        }
        return sw.toString();
    }
}
