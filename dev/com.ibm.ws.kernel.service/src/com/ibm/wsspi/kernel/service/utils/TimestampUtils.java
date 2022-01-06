/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
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
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

import io.openliberty.checkpoint.spi.CheckpointHook;

/**
 *
 */
public class TimestampUtils {
    @Deprecated
    protected final static long startTime;
    @Deprecated
    protected final static long startTimeNano;
    private volatile static long internalStartTimeNano;

    static {
        long start = OsgiPropertyUtils.getLong("kernel.launch.time", 0);
        if (start == 0)
            start = System.nanoTime();

        startTime = System.currentTimeMillis();
        startTimeNano = start;
        internalStartTimeNano = start;
        Bundle thisBundle = FrameworkUtil.getBundle(TimestampUtils.class);
        // must be unit tests if null
        if (thisBundle != null) {
            BundleContext bc = thisBundle.getBundleContext();
            if (bc != null) {
                // taking shortcuts because this bundle registers the WsLocationAdmin service
                final WsLocationAdmin locServiceImpl = bc.getService(bc.getServiceReference(WsLocationAdmin.class));
                // Look for time file created during CRIU restore. Use it to calculate more accurate server start time.
                bc.registerService(CheckpointHook.class, new CheckpointHook() {
                    @Override
                    @FFDCIgnore({ NumberFormatException.class, IOException.class, IllegalArgumentException.class })
                    public void restore() {
                        long restoreTime = 0;
                        try {
                            WsResource restoreTimeResource = locServiceImpl.getServerWorkareaResource("checkpoint/restoreTime");
                            List<String> restoreStartTimeEnv = Files.readAllLines(restoreTimeResource.asFile().toPath());
                            System.out.println("restore time: " + restoreStartTimeEnv);
                            if (!restoreStartTimeEnv.isEmpty()) {
                                long startTimeInMillis = Long.parseLong(restoreStartTimeEnv.get(0));
                                long currentTime = System.currentTimeMillis();
                                System.out.println("current time: " + currentTime);
                                restoreTime = currentTime - startTimeInMillis;
                            }
                        } catch (NumberFormatException e) {
                        } catch (IOException e) {
                        } catch (IllegalArgumentException e) {
                        }
                        TimestampUtils.internalStartTimeNano = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(restoreTime);
                    }
                }, FrameworkUtil.asDictionary(Collections.singletonMap(Constants.SERVICE_RANKING, Integer.MIN_VALUE + 1)));
            }
        }
    }

    public static void writeTimeToFile(File file, long timestamp) {
        if (file == null)
            return;

        // Update last modified timestamp
        String stamp = Long.toString(timestamp);

        FileWriter fstream = null;

        try {
            // if parent doesn't exist and can't be created, return early
            if (!FileUtils.ensureDirExists(file.getParentFile()))
                return;

            fstream = new FileWriter(file);
            fstream.write(stamp, 0, stamp.length());
        } catch (IOException e) {
        } finally {
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
            in = new BufferedReader(fstream, 32);
            timestamp = Long.parseLong(in.readLine());
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
     *                     Class from the calling bundle
     * @param msgKey
     *                     Translated message key
     */
    public static void auditElapsedTime(TraceComponent callingTc, String msgKey) {
        if (callingTc.isAuditEnabled())
            Tr.audit(callingTc, msgKey, getElapsedTime());
    }

    public static String getElapsedTime() {
        return getElapsedTimeNanos(internalStartTimeNano);
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
        return internalStartTimeNano;
    }
}