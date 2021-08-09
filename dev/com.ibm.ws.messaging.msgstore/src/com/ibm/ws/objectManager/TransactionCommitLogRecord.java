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
 * TransactionCommitLogRecord contains log information to commit a Transaction.
 * 
 * @author IBM Corporation
 */
class TransactionCommitLogRecord extends LogRecord {
    private static final Class cclass = TransactionCommitLogRecord.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);
    private static final long serialVersionUID = -665135963617958508L;

    // The logicalUnitOfWork that the commit operation belongs to.
    protected LogicalUnitOfWork logicalUnitOfWork;

    /**
     * Construct a LogRecord and prepare its buffers ready to write to the log.
     * 
     * @param internalTransaction performing the commit.
     * @throws ObjectManagerException
     */
    protected TransactionCommitLogRecord(InternalTransaction internalTransaction)
        throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { internalTransaction });

        this.logicalUnitOfWork = internalTransaction.getLogicalUnitOfWork();

        // Get the buffers that the log record wants to write.
        buffers = getBuffers(internalTransaction.logRecordByteArrayOutputStream);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // Constructor.

    /**
     * Constructor
     * 
     * @param dataInputStream from which to construct the log record.
     * @throws ObjectManagerException
     */
    protected TransactionCommitLogRecord(java.io.DataInputStream dataInputStream)
        throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        "DataInputStream=" + dataInputStream);

        // LogRecord.type already read.
        this.logicalUnitOfWork = new LogicalUnitOfWork(dataInputStream);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // Constructor.

    /**
     * Gives back the serialized LogRecord as arrays of bytes.
     * 
     * @param ObjectManagerByteArrayOutputStream used to hold the log record.
     * @return ObjectManagerByteArrayOutputStream[] the buffers containing the serialized LogRecord.
     */
    public ObjectManagerByteArrayOutputStream[] getBuffers(ObjectManagerByteArrayOutputStream byteArrayOutputStream)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getBuffers",
                        new Object[] { byteArrayOutputStream });

        ObjectManagerByteArrayOutputStream[] buffers = new ObjectManagerByteArrayOutputStream[1];

        // Create the buffer to contain the header for this log record.
        byteArrayOutputStream.reset();
        buffers[0] = byteArrayOutputStream;

        buffers[0].writeInt(LogRecord.TYPE_COMMIT);
        logicalUnitOfWork.writeSerializedBytes(buffers[0]);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getBuffers",
                       buffers);
        return buffers;
    } // Of method getBuffers.

    /**
     * Called to perform recovery action during a warm start of the objectManager.
     * 
     * @param ObjectManagerState
     *            of the ObjectManager performing recovery.
     */
    public void performRecovery(ObjectManagerState objectManagerState)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "performRecovery",
                        "ObjectManagerState=" + objectManagerState);

        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(this, cclass,
                        "logicalUnitOfWork.identifier=" + logicalUnitOfWork.identifier + "(long)");

        Transaction transactionForRecovery = objectManagerState.getTransaction(logicalUnitOfWork);
        transactionForRecovery.commit(false); // Do not re use this Transaction.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "performRecovery");
    } // Of method performRecovery.

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogRecord#maximumSerializedSize()
     */
    protected static long maximumSerializedSize()
    {
        return 4 // Log Record Type.
        + LogicalUnitOfWork.maximumSerializedSize();
    } // maximumSerializedSize().

} // class TransactionCommitLogRecord.
