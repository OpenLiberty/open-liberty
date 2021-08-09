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
 * <p>checkpointEndLogRecord marks the end of a checkpoint.
 */
class CheckpointEndLogRecord extends LogRecord
{
    private static final Class cclass = CheckpointEndLogRecord.class;
    private static final long serialVersionUID = -2254428502415067295L;

    private static Trace trace = ObjectManager.traceFactory.getTrace(CheckpointEndLogRecord.class,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);

    /**
     * Construct a LogRecord and prepare its buffers ready to write to the log.
     * 
     * @throws ObjectManagerException
     */
    protected CheckpointEndLogRecord()
        throws ObjectManagerException
    {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        // Get the buffers that the log record wants to write.
        buffers = getBuffers();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // CheckpointEndLogRecord(). 

    /**
     * Constructor
     * 
     * @param dataInputStream from which to construct the log record.
     * @throws ObjectManagerException
     */
    protected CheckpointEndLogRecord(java.io.DataInputStream dataInputStream)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        dataInputStream);

        // LogRecord.type already read.
        // Nothing left to do. 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , methodName
                            );
    } // CheckpointEndLogRecord().  

    /**
     * Gives back the serialized LogRecord as arrays of bytes.
     * 
     * @return ObjectManagerByteArrayOutputStream[] the buffers containing the serialized LogRecord.
     */
    public ObjectManagerByteArrayOutputStream[] getBuffers()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getBuffers"
                            );

        ObjectManagerByteArrayOutputStream[] buffers = new ObjectManagerByteArrayOutputStream[1];

        // Create the buffer to contain the header for this log record. 
        buffers[0] = new ObjectManagerByteArrayOutputStream(4);
        buffers[0].writeInt(LogRecord.TYPE_CHECKPOINT_END);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getBuffers"
                       , new Object[] { buffers }
                            );
        return buffers;
    } // getBuffers(). 

    /**
     * Called to perform recovery action during a warm start of the ObjectManager.
     * 
     * @param ObjectManagerState of the ObjectManager performing recovery.
     * 
     */
    public void performRecovery(ObjectManagerState objectManagerState)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "performRecovery"
                        , " objectManager=" + objectManagerState + "(ObjectManagerState)"
                          + ")"
                            );

        objectManagerState.checkpointEndSeen = true;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "performRecovery"
                            );
    } // Of method performRecovery. 
} // End of class CheckpointEndLogRecord.
