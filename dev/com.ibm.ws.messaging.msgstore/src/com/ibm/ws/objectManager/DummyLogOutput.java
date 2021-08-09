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

import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * <p>
 * Dummy log used during recovery in place of a real log.
 * Also used in place of a real log where no recovery is needed.
 * <\p>
 */
public class DummyLogOutput extends LogOutput
{
    private static final Class cclass = DummyLogOutput.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_LOG);

    private Object logSequenceNumberLock = new Object(); // Lock.
    private long logFileSize = 0;

    /**
     * Constructor
     * 
     * @throws ObjectManagerException
     */
    protected DummyLogOutput()
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this, cclass,
                        "<init>");
            trace.exit(this, cclass,
                       "<init>");
        }
    } // Constructor.

    /**
     * Prohibits further operations on the LogFile.
     * 
     * @throws ObjectManagerException
     */
    protected void close()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this, cclass,
                        "close");
            trace.exit(this, cclass,
                       "close");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#setLogFileSize(long)
     */
    protected synchronized void setLogFileSize(long logFileSize)
                    throws ObjectManagerException
    {
        this.logFileSize = logFileSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#getLogFileSize()
     */
    protected synchronized long getLogFileSize()
    {
        return logFileSize;
    } // getLogFileSize().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#getLogFileSizeRequested()
     */
    protected synchronized long getLogFileSizeRequested()
    {
        return logFileSize;
    } // getLogFileSizeRequested().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#getLogFileSpaceLeft()
     */
    protected long getLogFileSpaceLeft()
    {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#truncate()
     */
    protected void truncate()
                    throws ObjectManagerException
    {
        final String methodName = "truncate";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // truncate().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#reserve(long)
     */
    protected void reserve(long reservedDelta)
                    throws ObjectManagerException {
        // Nothing to do.
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#writeNext(com.ibm.ws.objectManager.LogRecord, long, boolean)
     */
    protected long writeNext(LogRecord logRecord,
                             long reservedDelta,
                             boolean checkSpace,
                             boolean flush)
                    throws ObjectManagerException {
        final String methodName = "writeNext";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { logRecord, new Long(reservedDelta), new Boolean(checkSpace), new Boolean(flush) });

        long usableLogSequenceNumber;

        // Take a lock on the log and increment the log sequence number.
        synchronized (logSequenceNumberLock) {
            logSequenceNumber++; // Set the current Sequence number.
            usableLogSequenceNumber = logSequenceNumber;
        } // synchronized (LogSequeunceNumberLock).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { new Long(usableLogSequenceNumber) });
        return usableLogSequenceNumber;
    } // writeNext().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#markAndWriteNext(com.ibm.ws.objectManager.LogRecord, long, boolean)
     */
    protected long markAndWriteNext(LogRecord logRecord,
                                    long reservedDelta,
                                    boolean checkSpace,
                                    boolean flush)
                    throws ObjectManagerException {
        final String methodName = "markAndWriteNext";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { logRecord, new Long(reservedDelta), new Boolean(checkSpace), new Boolean(flush) });

        long usableLogSequenceNumber;

        // Take a lock on the log and increment the log sequence number.
        synchronized (logSequenceNumberLock) {
            logSequenceNumber++; // Set the current Sequence number.
            usableLogSequenceNumber = logSequenceNumber;
        } // synchronized (LogSequeunceNumberLock).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       Long.toString(usableLogSequenceNumber));
        return usableLogSequenceNumber;
    } // markAndWriteNext().
} // class DummyLogOutput.
