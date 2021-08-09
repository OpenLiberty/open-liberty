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
 * <p>checkpointStartLogRecord marks the start of a checkpoint.
 * <ol>
 * <li>Establish the ObjectManagers persistent state.
 * <li>Establish the Transactions active during the checkpoint.
 * </ol>
 * 
 * @author IBM Corporation
 */
class CheckpointStartLogRecord extends LogRecord
{
    private static final Class cclass = CheckpointStartLogRecord.class;
    private static final long serialVersionUID = 971589238651066798L;

    private static Trace trace = ObjectManager.traceFactory.getTrace(CheckpointStartLogRecord.class,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);

    // The OueueManagerState token.
    protected Token objectManagerStateToken;
    protected byte objectManagerStateBytes[];
    ObjectManagerByteArrayOutputStream serializedBytes;

    /**
     * Construct a LogRecord and prepare its buffers ready to write to the log.
     * 
     * @param objectManagerState of the ObjectManager writing the checkpoint.
     * @throws ObjectManagerException
     */
    protected CheckpointStartLogRecord(ObjectManagerState objectManagerState)
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { objectManagerState });

        objectManagerStateToken = objectManagerState.getToken();
        // Capture the ObjectManagerState.
        serializedBytes = objectManagerState.getSerializedBytes();
        // Get the buffers that the log record wants to write.
        buffers = getBuffers();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // CheckpointStartLogRecord. 

    /**
     * Constructor
     * 
     * @param dataInputStream from which to construct the log record.
     * @param objectManagerState reading the log record.
     * @throws ObjectManagerException
     */
    protected CheckpointStartLogRecord(java.io.DataInputStream dataInputStream,
                                       ObjectManagerState objectManagerState)
        throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { dataInputStream,
                                      objectManagerState });

        try {
            // LogRecord.type already read.
            objectManagerStateToken = Token.restore(dataInputStream,
                                                    objectManagerState);
            long objectManagerStateBytesLength = dataInputStream.readLong();

            // Now get the serialized form of the ObjectManagerState.
            objectManagerStateBytes = new byte[(int) objectManagerStateBytesLength];
            dataInputStream.read(objectManagerStateBytes);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "<init>", exception, "1:104:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "<init> via PermanentIOException"
                             + "exception=" + exception + "(java.io.IOException)"
                                );
            throw new PermanentIOException(this
                                           , exception);
        } // catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // CheckpointStartLogRecord. 

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogRecord#getBuffers()
     */
    public ObjectManagerByteArrayOutputStream[] getBuffers()
                    throws ObjectManagerException
    {
        final String methodName = "getBuffers";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        ObjectManagerByteArrayOutputStream[] buffers = new ObjectManagerByteArrayOutputStream[2];

        // Create the buffer to contain the header for this log record.
        buffers[0] = new ObjectManagerByteArrayOutputStream((int) (4 + Token.maximumSerializedSize() + 8));

        buffers[0].writeInt(LogRecord.TYPE_CHECKPOINT_START);
        objectManagerStateToken.writeSerializedBytes(buffers[0]);
        buffers[0].writeLong(serializedBytes.getCount());

        // Now add buffers for the ObjectManagerState. 
        buffers[1] = objectManagerStateToken.getManagedObject().getSerializedBytes();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { buffers });
        return buffers;
    } // getBuffers(). 

    /**
     * Called to perform any recovery action during a warm start of the ObjectManager.
     * 
     * @param objectManagerState of the ObjectManager performing recovery.
     * @throws ObjectManagerException
     */
    public void performRecovery(ObjectManagerState objectManagerState)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "performRecovery",
                        objectManagerState);

        // Recover the ObjectManagerState from its serialized bytes.
        ObjectManagerState recoveredObjectManagerState = (ObjectManagerState) ManagedObject.restoreFromSerializedBytes(objectManagerStateBytes,
                                                                                                                       objectManagerState);
        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(this, cclass
                        , "recoveredObjectManagerState=" + recoveredObjectManagerState + "(ObjectManagerState)"
                            );

        // Make ObjectManagerState the recovered version.
        objectManagerStateToken.setManagedObject(recoveredObjectManagerState);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "performRecovery");
    } // Of method performRecovery. 
} // End of class CheckpointStartLogRecord.
