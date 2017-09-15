package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.objectManager.utils.Printable;
import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * <p>LogOutput, supports writing of log records.
 * 
 * @author IBM Corporation
 */
public abstract class LogOutput
                implements Printable
{
    private static final Class cclass = LogOutput.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_LOG);

    public static final boolean gatherStatistics = true; // Built for statistics if true.

    // A sequence number to uniquely identify the log record.
    // The log records do necessarily appear in the log in the same order as their
    // sequence numbers. Checkpointing depends on this because ManagedObjects are
    // forced into the ObjectStores assuming that LogRecords are forced to disk up to 
    // and including the LSN of the commit record. 
    protected long logSequenceNumber = 0; // Unique number.
    // Space reserved in the log by trasaction so that they can be certain of writing outcome and checkpoint 
    // records without hitting log full.
    protected long reserved = 0;

    /**
     * Constructor
     * 
     * @throws ObjectManagerException
     */
    protected LogOutput()
        throws ObjectManagerException
    {}

    /**
     * Prohibits further operations on the Log.
     * 
     * @throws ObjectManagerException
     */
    protected abstract void close()
                    throws ObjectManagerException;

    /**
     * Sets the size of the log file to the new value.
     * Blocks until this has completed.
     * 
     * @param logFileSize new log file size in bytes, this will be rounded down the next whole page.
     * @throws ObjectManagerException
     */
    protected abstract void setLogFileSize(long logFileSize)
                    throws ObjectManagerException;

    /**
     * Gives the size of the log file in use.
     * 
     * @return long the size of the log file in bytes.
     */
    protected abstract long getLogFileSize();

    /**
     * Gives the size of the log file requested.
     * 
     * @return long the size of the log file requested in bytes.
     */
    protected abstract long getLogFileSizeRequested();

    /**
     * Gives the size of free space in the log file requested.
     * 
     * @return long the size of free space in the log file requested in bytes.
     */
    protected abstract long getLogFileSpaceLeft();

    /**
     * @return Returns true if the ocupancy of the log file is high.
     */
    protected boolean isOcupancyHigh()
    {
        return false;
    } // isOcupancyHigh().

    /**
     * Writes buffered output to hardened storage at least up to the Mark point,
     * then ensure that the logHeader is rewritten to indicate a new start point in the log.
     * This will be at a point previously set by a call to markAndWriteNext().
     * Blocks until this has completed.
     * 
     * @throws ObjectManagerException
     */
    protected abstract void truncate()
                    throws ObjectManagerException;

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.Printable#print(java.io.PrintWriter)
     */
    public void print(java.io.PrintWriter printWriter)
    {
        printWriter.println("LogOutput"
                            + " logSequenceNumber=" + logSequenceNumber + "(long)"
                            + " reserved=" + reserved + "(long)");
    } // print().

    /**
     * Reserve space in the log.
     * 
     * @param reservedDelta the change to the number of reserved bytes if this write is successful.
     * @throws ObjectManagerException
     */
    protected abstract void reserve(long reservedDelta)
                    throws ObjectManagerException;

    /**
     * Appends a LogRecord to end of the LogFile. !! note public because ClusterSenderChannelInstance write a log record
     * directly.
     * 
     * @param logRecord to be written.
     * @param reservedDelta the change to the number of reserved bytes if this write is successful.
     * @param checkSpace true is we should check that log file space is available before filling the buffer.
     * @param flush if the logRecord must be forced to disk before we return.
     * 
     * @return long the log sequence number for the written LogRecord.
     * @throws ObjectManagerException
     */
    protected abstract long writeNext(LogRecord logRecord,
                                      long reservedDelta,
                                      boolean checkSpace,
                                      boolean flush)
                    throws ObjectManagerException;

    /**
     * Appends a LogRecord to end of the LogFile, and marks the truncate position.
     * 
     * @param logRecord to be written.
     * @param reservedDelta the change to the number of reserved bytes if this write is successful.
     * @param checkSpace true is we should check that log file space is available before filling the buffer.
     * @param flush true if the logRecord must be forced to doisk before we return.
     * 
     * @return long the log sequence number for the written LogRecord.
     * @throws ObjectManagerException
     */
    protected abstract long markAndWriteNext(LogRecord logRecord,
                                             long reservedDelta,
                                             boolean checkSpace,
                                             boolean flush)
                    throws ObjectManagerException;

    /**
     * @return long the logSequenceNumber.
     */
    protected long getLogSequenceNumber() {
        return logSequenceNumber;
    }

    /*
     * Builds a set of properties containing the current statistics.
     * 
     * @return java.util.Properties the statistics.
     */
    protected java.util.Map captureStatistics() {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "captureStatistics");

        java.util.Map statistics = new java.util.HashMap();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "captureStatistics",
                       statistics);
        return statistics;
    } // captureStatistics().
} // class LogOutput.
