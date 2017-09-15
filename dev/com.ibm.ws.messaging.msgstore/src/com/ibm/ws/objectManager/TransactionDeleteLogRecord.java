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
 * <p>TransactionDeleteLogRecord contains log information to redo a
 * delete of a ObjectManager oject.
 * 
 * @author IBM Corporation
 */
class TransactionDeleteLogRecord
                extends LogRecord {
    private static final Class cclass = TransactionDeleteLogRecord.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);
    private static final long serialVersionUID = 7265650043809552318L;

    // The logicalUnitOfWork that the delete operation belongs to.
    private LogicalUnitOfWork logicalUnitOfWork;
    // The state of the transaction.
    private int transactionState;
    // The queue manager object to delete. 
    private Token token;

    /**
     * Construct a LogRecord and prepare its buffers ready to write to the log.
     * 
     * @param internalTransaction performing the deletion.
     * @param token of the managedObject being deleted.
     * @throws ObjectManagerException
     */
    protected TransactionDeleteLogRecord(InternalTransaction internalTransaction,
                                         Token token)
        throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { internalTransaction,
                                      token });

        this.logicalUnitOfWork = internalTransaction.getLogicalUnitOfWork();
        this.transactionState = internalTransaction.getState();
        this.token = token;

        // Get the buffers that the log record wants to write.
        buffers = getBuffers(internalTransaction.logRecordByteArrayOutputStream);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, "<init>");
    } // TransactionDeleteLogRecord().

    /**
     * Constructor
     * 
     * @param dataInputStream from which to construct the log record.
     * @param objectManagerState of the objectManager reconstructing the LogRecord.
     * @throws ObjectManagerException
     */
    protected TransactionDeleteLogRecord(java.io.DataInputStream dataInputStream,
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
            logicalUnitOfWork = new LogicalUnitOfWork(dataInputStream);
            transactionState = dataInputStream.readInt();
            token = Token.restore(dataInputStream
                                  , objectManagerState
                            );
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "<init>", exception, "1:100:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>"
                           , exception);
            throw new PermanentIOException(this,
                                           exception);
        } //  catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, "<init>");
    } // TransactionDeleteLogRecord().

    /**
     * Gives back the serialized LogRecord as arrays of bytes.
     * 
     * @param byteArrayOutputStream used to hold the log record.
     * @return ObjectManagerByteArrayOutputStream[] the buffers containing the serialized LogRecord.
     * @throws ObjectManagerException
     */
    private ObjectManagerByteArrayOutputStream[] getBuffers(ObjectManagerByteArrayOutputStream byteArrayOutputStream)
                    throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, "getBuffers", new Object[] { byteArrayOutputStream });

        ObjectManagerByteArrayOutputStream[] buffers = new ObjectManagerByteArrayOutputStream[1];

        // Create the buffer to contain the header for this log record.
        byteArrayOutputStream.reset();
        buffers[0] = byteArrayOutputStream;

        buffers[0].writeInt(LogRecord.TYPE_DELETE);
        logicalUnitOfWork.writeSerializedBytes(buffers[0]);
        buffers[0].writeInt(transactionState);
        token.writeSerializedBytes(buffers[0]);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "getBuffers",
                       new Object[] { buffers });
        return buffers;
    } // getBuffers(). 

    /**
     * Called to perform recovery action during a warm start of the ObjectManager.
     * 
     * @param objectManagerState of the ObjectManager performing recovery.
     * @throws ObjectManagerException
     */
    public void performRecovery(ObjectManagerState objectManagerState)
                    throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "performRecovery",
                        new Object[] { objectManagerState, logicalUnitOfWork, new Integer(transactionState), token });

        // Delete the ManagedObject again using its original Transaction.
        Transaction transactionForRecovery = objectManagerState.getTransaction(logicalUnitOfWork);

        ManagedObject existingManagedObject = token.getManagedObject();
        if (existingManagedObject == null)
        {
            // The object may have already been deleted from the ObjectStore,
            // so create a dummy object to keep the transaction happy. 
            // The Token will have the ObjecStore and storedObjectIdentifier so the the correct delete in the
            // ObjectStore can take place. 
            DummyManagedObject dummyManagedObject = new DummyManagedObject("Created by TransactionDeleteLogRecord.performRecovery()");
            existingManagedObject = token.setManagedObject(dummyManagedObject);
            existingManagedObject.state = ManagedObject.stateReady;
        } // if (existingManagedObject == null).   

        // Redo the delete. 
        transactionForRecovery.delete(existingManagedObject);
        // No need to reset the transaction state because Delete can only be executed before
        // the transaction is prepared.
        // transactionForRecovery.internalTransaction.resetState(transactionState);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, "performRecovery");
    } // Of method performRecovery. 

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogRecord#maximumSerializedSize()
     */
    protected static long maximumSerializedSize()
    {
        return 4 // Log Record Type.
               + LogicalUnitOfWork.maximumSerializedSize()
               + 4 // Transaction State.
               + Token.maximumSerializedSize();
    } // Of maximumSerializedSize().

} // End of class TransactionDeleteLogRecord.
