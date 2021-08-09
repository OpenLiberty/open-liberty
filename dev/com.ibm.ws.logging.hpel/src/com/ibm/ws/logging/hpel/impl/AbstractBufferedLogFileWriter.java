/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Timer;
import java.util.TimerTask;

import com.ibm.ws.logging.hpel.LogFileWriter;

/**
 * Implementation of the {@link LogFileWriter} interface using buffering to improve
 * performance of writting records into a file.
 */
public abstract class AbstractBufferedLogFileWriter implements LogFileWriter {
    // Be careful with the logger since the code in this class is used in logging logic itself
    // and may result in an indefinite loop.
    //private final static String BUNDLE_NAME = "com.ibm.ws.logging.hpel.resources.HpelMessages";
    //private final static String className = AbstractBufferedLogFileWriter.class.getName();
    //private static Logger logger; 

    private final static String OUT_BUFFER_SIZE_PROPERTY_NAME = "HPEL.BUFFER.SIZE";
    private final static int DEFAULT_BUFFER_SIZE = 8 * 1024;
    private static int bufferSize;
    private final static String FLUSH_PERIOD_PROPERTY_NAME = "HPEL.FLUSH.PERIOD.SECS";
    private final static long DEFAULT_FLUSH_PERIOD_SECS = 10;
    private static long flushPeriod;

    static {
        String bufferSizeStr = getSystemProperty(OUT_BUFFER_SIZE_PROPERTY_NAME);
        if (bufferSizeStr != null) {
            try {
                bufferSize = Integer.decode(bufferSizeStr);
            } catch (NumberFormatException e) {
                // It's OK to use logger here since adding log records should not result in reloading this class.
                //	logger.logp(Level.WARNING, className, "static", "HPEL_WrongBufferSizeValue",
                //			new Object[]{bufferSizeStr, OUT_BUFFER_SIZE_PROPERTY_NAME, DEFAULT_BUFFER_SIZE});
                bufferSize = DEFAULT_BUFFER_SIZE;
            }
        } else {
            bufferSize = DEFAULT_BUFFER_SIZE;
        }

        String flushPeriodStr = getSystemProperty(FLUSH_PERIOD_PROPERTY_NAME);
        if (flushPeriodStr != null) {
            try {
                flushPeriod = Long.decode(flushPeriodStr);
            } catch (NumberFormatException e) {
                // It's OK to use logger here since adding log records should not result in reloading this class.
                //	logger.logp(Level.WARNING, className, "static", "HPEL_WrongFlushPeriodValue",
                //			new Object[]{flushPeriodStr, FLUSH_PERIOD_PROPERTY_NAME, DEFAULT_FLUSH_PERIOD_SECS});
                flushPeriod = DEFAULT_FLUSH_PERIOD_SECS;
            }
        } else {
            flushPeriod = DEFAULT_FLUSH_PERIOD_SECS;
        }
        flushPeriod *= 1000; // convert default flush period from seconds to milliseconds
        //logger = Logger.getLogger(className, BUNDLE_NAME);
    }

    private final File file;
    /**
     * Underlying output stream. All access to this instance should be synchronized.
     */
    protected final OutputStream fileStream;

    private Timer flushTimer = null;

    private class flushTask extends TimerTask {
        @Override
        public void run() {
            try {
                synchronized (fileStream) {
                    if (flushTimer != null) {
                        fileStream.flush();
                    }
                }
            } catch (IOException e) {
                // It's OK to use logger here since new log records will not result in change of period
                // flush frequence.
                //logger.logp(Level.WARNING, flushTask.class.getName(), "run", "HPEL_ExceptionInPeriodicFlush", e);
            }
        }
    }

    AbstractBufferedLogFileWriter(File file, boolean bufferingEnabled) throws IOException {
        this.file = file;

        // Take into account possible access problem with creating flush thread
        if (bufferingEnabled) {
            flushTimer = AccessHelper.createTimer();
        }

        if (bufferingEnabled && flushTimer != null) {
            fileStream = new BufferedOutputStream(AccessHelper.createFileOutputStream(file, false), bufferSize);
            flushTimer.schedule(new flushTask(), flushPeriod, flushPeriod);
        } else {
            fileStream = AccessHelper.createFileOutputStream(file, false);
        }

    }

    /**
     * @param flushPeriodPropertyName
     * @return
     */
    private static String getSystemProperty(final String flushPeriodPropertyName) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(flushPeriodPropertyName);
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.hpel.internal.LogFileWriter#close(byte[])
     */
    @Override
    public void close(byte[] tail) throws IOException {
        synchronized (fileStream) {
            // Cancel timer thread before any method capable of throwing IOException.
            if (flushTimer != null) {
                flushTimer.cancel();
                flushTimer = null;
            }
            fileStream.flush();
            fileStream.close();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.hpel.internal.LogFileWriter#currentFile()
     */
    @Override
    public File currentFile() {
        return file;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.hpel.internal.LogFileWriter#flush()
     */
    @Override
    public void flush() throws IOException {
        // Flushing should be done only if buffering is disabled.
        if (flushTimer == null) {
            fileStream.flush();
        }
    }

}
