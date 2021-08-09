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
 * <p>LogInput, performs reading of log records.<\p>
 * 
 * @author IBM Corporation.
 */
public abstract class LogInput
{
    private static final Class cclass = LogInput.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_LOG);

    // The ObjectManagerState this LogInput is instantiated by.
    protected ObjectManagerState objectManagerState;

    /**
     * Constructor
     * 
     * @param objectManagerState creating the LogInput.
     * @throws ObjectManagerException
     */
    protected LogInput(ObjectManagerState objectManagerState)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        objectManagerState);

        this.objectManagerState = objectManagerState;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // LogInput().

    /**
     * Prevents further operations on the log file.
     * 
     * @throws ObjectManagerException
     */
    public abstract void close()
                    throws ObjectManagerException;

    /**
     * Gives the size of the log file in use.
     * 
     * @return long the size of the log file in bytes.
     */
    protected abstract long getLogFileSize();

    /**
     * Reads the next record from the log.
     * 
     * @return LogRecord read.
     * @throws ObjectManagerException
     * @throws LogFileExhaustedException if there are no more logRecords left to read.
     */
    public abstract LogRecord readNext()
                    throws ObjectManagerException;

} // End of class LogInput.
