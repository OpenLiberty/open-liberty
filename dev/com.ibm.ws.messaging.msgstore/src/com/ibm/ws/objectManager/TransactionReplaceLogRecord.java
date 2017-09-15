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
 * TransactionReplaceLogRecord contains log information to redo a replace of a ManagedObject.
 * 
 * @author IBM Corporation
 */
class TransactionReplaceLogRecord
                extends LogRecord {
    private static final Class cclass = TransactionReplaceLogRecord.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);
    private static final long serialVersionUID = 5720229220972571013L;

    // The logicalUnitOfWork that the replace operation belongs to.
    protected LogicalUnitOfWork logicalUnitOfWork;
    // The state of the transaction.
    protected int transactionState;
    // The queue manager object to replace.
    protected Token token;
    // The serialized form of the ManagedObject.
    protected byte managedObjectBytes[];
    ObjectManagerByteArrayOutputStream serializedBytes;

    /**
     * Construct a LogRecord and prepare its buffers ready to write to the log.
     * 
     * @param internalTransaction performing the replace.
     * @param token of the managedObject being replaced.
     * @param serializedBytes containng the serialized for of the ManagedObject.
     * @throws ObjectManagerException
     */
    protected TransactionReplaceLogRecord(InternalTransaction internalTransaction,
                                          Token token,
                                          ObjectManagerByteArrayOutputStream serializedBytes)
        throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { internalTransaction, token, serializedBytes }
                            );

        this.logicalUnitOfWork = internalTransaction.getLogicalUnitOfWork();
        this.transactionState = internalTransaction.getState();
        this.token = token;
        this.serializedBytes = serializedBytes;

        // Get the buffers that the log record wants to write.
        buffers = getBuffers(internalTransaction.logRecordByteArrayOutputStream);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // of Constructor.

    /**
     * Constructor
     * 
     * @param dataInputStream from which to construct the log record.
     * @param objectManagerState of the objectManager reconstructing the LogRecord.
     * @throws ObjectManagerException
     */
    protected TransactionReplaceLogRecord(java.io.DataInputStream dataInputStream,
                                          ObjectManagerState objectManagerState)
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        "DataInputStream=" + dataInputStream + ", ObjectManagerState=" + objectManagerState);

        try {
            // LogRecord.type already read.
            logicalUnitOfWork = new LogicalUnitOfWork(dataInputStream);
            transactionState = dataInputStream.readInt();
            token = Token.restore(dataInputStream,
                                  objectManagerState);
            long managedObjectLength = dataInputStream.readLong();

            // Now get the serialized form of the ManagedObject.
            managedObjectBytes = new byte[(int) managedObjectLength];
            dataInputStream.read(managedObjectBytes);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "<init>", exception, "1:114:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>",
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // Constructor.

    /**
     * Gives back the serialized LogRecord as arrays of bytes.
     * 
     * @param byteArrayOutputStream used to hold the log record.
     * @return ObjectManagerByteArrayOutputStream[] the buffers containing the serialized LogRecord.
     * @throws ObjectManagerException
     */
    public ObjectManagerByteArrayOutputStream[] getBuffers(ObjectManagerByteArrayOutputStream byteArrayOutputStream)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getBuffers");

        ObjectManagerByteArrayOutputStream[] buffers = new ObjectManagerByteArrayOutputStream[2];

        // Create the buffer to contain the header for this log record.
        byteArrayOutputStream.reset();
        buffers[0] = byteArrayOutputStream;

        buffers[0].writeInt(LogRecord.TYPE_REPLACE);
        logicalUnitOfWork.writeSerializedBytes(buffers[0]);
        buffers[0].writeInt(transactionState);
        token.writeSerializedBytes(buffers[0]);
        buffers[0].writeLong(serializedBytes.getCount());

        // Now add a buffer containing the serialzed ManagedObject itself.
        buffers[1] = serializedBytes;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
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
                    throws ObjectManagerException
    {
        final String methodName = "performRecovery";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { objectManagerState,
                                      logicalUnitOfWork,
                                      new Integer(transactionState),
                                      token,
                                      new Integer(managedObjectBytes.length) });

        // Recover the ManagedObject from its serialized bytes.
        ManagedObject replacementManagedObject = ManagedObject.restoreFromSerializedBytes(managedObjectBytes,
                                                                                          objectManagerState);

        // Replace the object using its original transaction.
        Transaction transactionForRecovery = objectManagerState.getTransaction(logicalUnitOfWork);
        ManagedObject existingManagedObject = token.getManagedObject();
        if (existingManagedObject == null) {
            // The object may have already been deleted from the ObjectStore,
            // so create a dummy object to keep the transaction happy.
            // The Token will have the ObjecStore and storedObjectIdentifier so the the correct delete in the
            // ObjectStore can take place.
            DummyManagedObject dummyManagedObject = new DummyManagedObject("Created by TransactionReplaceLogRecord.performRecovery()");
            dummyManagedObject.state = ManagedObject.stateReady;
            dummyManagedObject.owningToken = token;
            existingManagedObject = token.setManagedObject(dummyManagedObject);
            existingManagedObject.state = ManagedObject.stateReady;
        } // if(existingManagedObject == null).

        transactionForRecovery.lock(existingManagedObject);
        token.setManagedObject(replacementManagedObject); // Revert to the restored managed object.

        transactionForRecovery.replace(existingManagedObject); // Redo the replace.
        // No need to reset the transaction state because Replace can only be executed before
        // the transaction is prepared.
        // transactionForRecovery.internalTransaction.resetState(transactionState);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // performRecovery().

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
               + Token.maximumSerializedSize()
               + 8 // Size of serialized ManagedObject.
        ;
    } // maximumSerializedSize().

} // class TransactionReplaceLogRecord.
