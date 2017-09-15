/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public class TimestampUtils {
    protected final static long startTime;
    protected final static long startTimeNano;

    static {
        long start = OsgiPropertyUtils.getLong("kernel.launch.time", 0);
        if (start == 0)
            start = System.currentTimeMillis();

        startTime = start;
        startTimeNano = System.nanoTime();
    }

    public static void writeTimeToFile(File file, long timestamp) {
        if (file == null)
            return;

        // Update last modified timestamp
        String stamp = Long.toString(timestamp);

        BufferedWriter out = null;
        FileWriter fstream = null;

        try {
            // if parent doesn't exist and can't be created, return early
            if (!FileUtils.ensureDirExists(file.getParentFile()))
                return;

            fstream = new FileWriter(file);
            out = new BufferedWriter(fstream);
            out.write(stamp, 0, stamp.length());
        } catch (IOException e) {
        } finally {
            if (!tryToClose(out))
                tryToClose(fstream);
        }
    }

    @FFDCIgnore({ FileNotFoundException.class, NumberFormatException.class })
    public static long readTimeFromFile(File file) {
        long timestamp = 0;

        // Read in server start timestamp
        FileReader fstream = null;
        BufferedReader in = null;

        try {
            fstream = new FileReader(file);
            in = new BufferedReader(fstream);
            timestamp = new Long(in.readLine()).longValue();
        } catch (FileNotFoundException e) {
            // Not an error: stamp might not have been created (return 0)
        } catch (NumberFormatException e) {
        } catch (IOException e) {
        } finally {
            if (!tryToClose(in))
                tryToClose(fstream);
        }

        return timestamp;
    }

    /**
     * @param nlsClass
     *            Class from the calling bundle
     * @param msgKey
     *            Translated message key
     */
    public static void auditElapsedTime(TraceComponent callingTc, String msgKey) {
        if (callingTc.isAuditEnabled())
            Tr.audit(callingTc, msgKey, getElapsedTime());
    }

    public static String getElapsedTime() {
        return getElapsedTime(startTime);
    }

    /**
     * @param startTime The start time (Obtained from System.currentTimeMillis)
     * @return a string version of the time since the startTime
     * @deprecated Replaced by @link {@link #getElapsedTimeNanos(long)}.
     *             Note that System.currentTimeMillis can go backwards and hence you may get a string that starts with '-' character
     */
    @Deprecated
    public static String getElapsedTime(long startTime) {
        // Grab current time
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        return getElapsedTimeAsStringFromMilliInterval(elapsedTime);
    }

    /**
     * Return elapsed time in the locale format
     *
     * @param elapsedTime A time interval in milliseconds
     * @return A string version of the time interval to 3 decimal places
     */
    static String getElapsedTimeAsStringFromMilliInterval(long elapsedTime) {
        return String.format("%.3f", elapsedTime / 1000.0);
    }

    /**
     * @param startTime The start time (Obtained from System.nanoTime)
     * @return a string version of the time since the startTime
     */
    public static String getElapsedTimeNanos(long startTime) {
        // Grab current time
        long endTime = System.nanoTime();
        long elapsedTime = (endTime - startTime) / 1000000L;
        return getElapsedTimeAsStringFromMilliInterval(elapsedTime);
    }

    @FFDCIgnore(IOException.class)
    private static boolean tryToClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
                return true;
            } catch (IOException e) {
                // At least attempt to close stream
            }
        }
        return false;
    }

    /**
     * Returns the nanosecond tick count when the server started, which may be negative.
     */
    public static final long getStartTimeNano() {
        return startTimeNano;
    }
}