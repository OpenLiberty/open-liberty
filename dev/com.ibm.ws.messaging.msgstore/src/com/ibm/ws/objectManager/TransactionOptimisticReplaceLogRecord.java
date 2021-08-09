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
 * TransactionOptimisticReplaceLogRecord contains log information to redo multiple add
 * and optimistic replace actions of ManagedObjects.
 * 
 * @author IBM Corporation
 */
class TransactionOptimisticReplaceLogRecord extends LogRecord
{
    private static final Class cclass = TransactionOptimisticReplaceLogRecord.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);
    private static final long serialVersionUID = -4939541309211419352L;

    // The logicalUnitOfWork that the replace operation belongs to.
    protected LogicalUnitOfWork logicalUnitOfWork;
    // The state of the transaction.
    protected int transactionState;
    // The list ManagedObject tokens add.
    private java.util.List tokensToAdd;
    // The list ManagedObject tokens Optimistic replace.
    private java.util.List tokensToOptimisticReplace;
    // The list of serialized forms of the ManagedObjects.
    private java.util.List serializedManagedObjectBytes;
    //The list ManagedObject tokens delete.
    private java.util.List tokensToDelete;
    // The list of Tokens to notify that the replace has been logged.
    private java.util.List tokensToNotify;

    /**
     * Construct a LogRecord and prepare its buffers ready to write to the log.
     * 
     * @param internalTransaction performing the deletion.
     * @param tokensToAdd the list of Tokens represnting persistent ManagedObjects to be added, may not be null.
     * @param tokensToOptimisticReplace the list of Tokens represnting persistent ManagedObjects to be replaced,
     *            may not be null.
     * @param serializedManagedObjectBytes the list serialized forms of the ManagedObjects, added first followed by replacements.
     * @param tokensToDelete the list of Tokens represnting persistent ManagedObjects to be deleted,
     *            may not be null.
     * @param tokensToNotify the list of Tokens whose ManagedObjects are to be notified after the replacement
     *            is logged, may not be null.
     * @throws ObjectManagerException
     */
    protected TransactionOptimisticReplaceLogRecord(InternalTransaction internalTransaction,
                                                    java.util.List tokensToAdd,
                                                    java.util.List tokensToOptimisticReplace,
                                                    java.util.List serializedManagedObjectBytes,
                                                    java.util.List tokensToDelete,
                                                    java.util.List tokensToNotify)
        throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { internalTransaction, tokensToAdd, tokensToOptimisticReplace,
                                      serializedManagedObjectBytes, tokensToDelete, tokensToNotify });

        this.logicalUnitOfWork = internalTransaction.getLogicalUnitOfWork();
        this.transactionState = internalTransaction.getState();
        this.tokensToAdd = tokensToAdd;
        this.tokensToOptimisticReplace = tokensToOptimisticReplace;
        this.serializedManagedObjectBytes = serializedManagedObjectBytes;
        this.tokensToDelete = tokensToDelete;
        this.tokensToNotify = tokensToNotify;

        // Get the buffers that the log record wants to write.
        buffers = getBuffers(internalTransaction.logRecordByteArrayOutputStream);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // TransactionOptimisticReplaceLogRecord().

    /**
     * Constructor
     * 
     * @param dataInputStream from which to construct the log record.
     * @param objectManagerState of the objectManager reconstructing the LogRecord.
     * @throws ObjectManagerException
     */
    protected TransactionOptimisticReplaceLogRecord(java.io.DataInputStream dataInputStream,
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
            int numberOfTokensToAdd = dataInputStream.readInt();
            int numberOfTokensToOptimisticReplace = dataInputStream.readInt();
            int numberOfTokensToDelete = dataInputStream.readInt();
            int numberOfTokensToNotify = dataInputStream.readInt();
            logicalUnitOfWork = new LogicalUnitOfWork(dataInputStream);
            transactionState = dataInputStream.readInt();
            serializedManagedObjectBytes = new java.util.ArrayList(numberOfTokensToAdd + numberOfTokensToOptimisticReplace);

            // Unpack the buffers containing the serialized ManagedObjects to add.
            tokensToAdd = new java.util.ArrayList(numberOfTokensToAdd);
            for (int i = 0; i < numberOfTokensToAdd; i++) {
                Token token = Token.restore(dataInputStream,
                                            objectManagerState);
                tokensToAdd.add(token);

                int managedObjectLength = dataInputStream.readInt();
                // Now get the serialized form of the ManagedObject.
                byte[] serializedBuffer = new byte[managedObjectLength];
                dataInputStream.read(serializedBuffer);
                serializedManagedObjectBytes.add(serializedBuffer);
            } // for tokensToAdd.size().

            // Unpack the buffers containing the serialized ManagedObjects to Optimistic replace.      
            tokensToOptimisticReplace = new java.util.ArrayList(numberOfTokensToOptimisticReplace);
            for (int i = 0; i < numberOfTokensToOptimisticReplace; i++) {
                Token token = Token.restore(dataInputStream,
                                            objectManagerState);
                tokensToOptimisticReplace.add(token);

                int managedObjectLength = dataInputStream.readInt();
                // Now get the serialized form of the ManagedObject.
                byte[] serializedBuffer = new byte[managedObjectLength];
                dataInputStream.read(serializedBuffer);
                serializedManagedObjectBytes.add(serializedBuffer);
            } // for tokensToOptimisticReplace.size().

            // Unpack the buffers containing the tokensToDelete.
            tokensToDelete = new java.util.ArrayList(numberOfTokensToDelete);
            for (int i = 0; i < numberOfTokensToDelete; i++) {
                Token token = Token.restore(dataInputStream,
                                            objectManagerState);
                tokensToDelete.add(token);
            } // for tokensToDelete.size().

            // Unpack the buffers containing the tokensToNotify.
            tokensToNotify = new java.util.ArrayList(numberOfTokensToNotify);
            for (int i = 0; i < numberOfTokensToNotify; i++) {
                Token token = Token.restore(dataInputStream,
                                            objectManagerState);
                tokensToNotify.add(token);
            } // for tokensToNotify.size().

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "<init>", exception, "1:174:1.11");

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
    } // TransactionOptimisticReplaceLogRecord().

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

        ObjectManagerByteArrayOutputStream[] buffers = new ObjectManagerByteArrayOutputStream[1
                                                                                              + (tokensToAdd.size()) * 2
                                                                                              + (tokensToOptimisticReplace.size()) * 2
                                                                                              + (tokensToDelete.size())
                                                                                              + (tokensToNotify.size())];

        int bufferIndex = 0; // The next buffer to use.

        // Create the buffer to contain the header for this log record.
        byteArrayOutputStream.reset();
        buffers[bufferIndex++] = byteArrayOutputStream;
        java.io.DataOutputStream dataOutputStream = new java.io.DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeInt(LogRecord.TYPE_OPTIMISTIC_REPLACE);
            dataOutputStream.writeInt(tokensToAdd.size());
            dataOutputStream.writeInt(tokensToOptimisticReplace.size());
            dataOutputStream.writeInt(tokensToDelete.size());
            dataOutputStream.writeInt(tokensToNotify.size());
            logicalUnitOfWork.writeObject(dataOutputStream);
            dataOutputStream.writeInt(transactionState);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "getBuffers", exception, "1:228:1.11");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "getBuffers"
                           , exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch.

        java.util.Iterator serializedObjectIterator = serializedManagedObjectBytes.iterator();

        //  Create the buffers containing the serialized ManagedObjects to add.
        for (int i = 0; i < tokensToAdd.size(); i++) {
            byteArrayOutputStream = new ObjectManagerByteArrayOutputStream((int) Token.maximumSerializedSize() + 4);
            dataOutputStream = new java.io.DataOutputStream(byteArrayOutputStream);
            ((Token) tokensToAdd.get(i)).writeObject(dataOutputStream);

            ObjectManagerByteArrayOutputStream managedObjectBytes = (ObjectManagerByteArrayOutputStream) serializedObjectIterator.next();
            try {
                dataOutputStream.writeInt(managedObjectBytes.getCount());

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "getBuffers", exception, "1:253:1.11");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "getBuffers",
                               exception);
                throw new PermanentIOException(this,
                                               exception);
            } // catch.
            buffers[bufferIndex++] = byteArrayOutputStream;

            // Now add a buffer containing the serialzed ManagedObject itself.
            buffers[bufferIndex++] = managedObjectBytes;
        } // for tokensToOptimisticAdd.size().

        // Create the buffers containing the serialized ManagedObjects to optimistic replace.
        for (int i = 0; i < tokensToOptimisticReplace.size(); i++) {
            byteArrayOutputStream = new ObjectManagerByteArrayOutputStream((int) Token.maximumSerializedSize() + 4);
            dataOutputStream = new java.io.DataOutputStream(byteArrayOutputStream);

            ((Token) tokensToOptimisticReplace.get(i)).writeObject(dataOutputStream);

            ObjectManagerByteArrayOutputStream managedObjectBytes = (ObjectManagerByteArrayOutputStream) serializedObjectIterator.next();
            try {
                dataOutputStream.writeInt(managedObjectBytes.getCount());

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "getBuffers", exception, "1:282:1.11");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "getBuffers",
                               exception);
                throw new PermanentIOException(this,
                                               exception);
            } // catch.
            buffers[bufferIndex++] = byteArrayOutputStream;

            // Now add a buffer containing the serialzed ManagedObject itself.
            buffers[bufferIndex++] = managedObjectBytes;
        } // for tokensToOptimisticReplace.size().

        // Create the buffers containing the Tokens to delete.
        for (int i = 0; i < tokensToDelete.size(); i++) {
            byteArrayOutputStream = new ObjectManagerByteArrayOutputStream((int) Token.maximumSerializedSize());
            dataOutputStream = new java.io.DataOutputStream(byteArrayOutputStream);
            ((Token) tokensToDelete.get(i)).writeObject(dataOutputStream);
            buffers[bufferIndex++] = byteArrayOutputStream;
        } // for tokensToDelete.size().

        // Create the buffers containing the Tokens to notify.
        for (int i = 0; i < tokensToNotify.size(); i++) {
            byteArrayOutputStream = new ObjectManagerByteArrayOutputStream((int) Token.maximumSerializedSize());
            dataOutputStream = new java.io.DataOutputStream(byteArrayOutputStream);
            ((Token) tokensToNotify.get(i)).writeObject(dataOutputStream);
            buffers[bufferIndex++] = byteArrayOutputStream;
        } // for tokensToNotify.size().

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getBuffers",
                       "return=" + buffers);
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
                        new Object[] { objectManagerState, logicalUnitOfWork, new Integer(transactionState)
                                      , tokensToOptimisticReplace, serializedManagedObjectBytes, tokensToDelete, tokensToNotify }
                            );

        // Replace the objects using their original transaction.
        Transaction transactionForRecovery = objectManagerState.getTransaction(logicalUnitOfWork);

        java.util.Iterator serializedObjectIterator = serializedManagedObjectBytes.iterator();

        // A list of ManagedObjects to add, this lists forces the ManagedObjects to
        // remain in storage while we give them to the Transaction.
        java.util.List managedObjectsToAdd = new java.util.ArrayList(tokensToAdd.size());
        for (java.util.Iterator tokenIterator = tokensToAdd.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();

            // Recover the ManagedObject from the serialized bytes.
            byte managedObjectBytes[] = (byte[]) serializedObjectIterator.next();
            ManagedObject managedObject = ManagedObject.restoreFromSerializedBytes(managedObjectBytes,
                                                                                   objectManagerState);

            ManagedObject managedObjectToAdd = token.setManagedObject(managedObject);
            managedObjectToAdd.state = ManagedObject.stateConstructed;
            managedObjectsToAdd.add(managedObjectToAdd);
        } // for ...tokensToAdd.

        // A list of ManagedObjects to optimistic replace, this lists forces the ManagedObjects to
        // remain in storage while we give them to the Transaction.
        java.util.List managedObjectsToOptimisticReplace = new java.util.ArrayList(tokensToOptimisticReplace.size());

        for (java.util.Iterator tokenIterator = tokensToOptimisticReplace.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();

            // Recover the ManagedObject from the serialized bytes.
            byte managedObjectBytes[] = (byte[]) serializedObjectIterator.next();
            ManagedObject managedObject = ManagedObject.restoreFromSerializedBytes(managedObjectBytes,
                                                                                   objectManagerState);

            ManagedObject managedObjectToReplace = token.setManagedObject(managedObject);
            managedObjectsToOptimisticReplace.add(managedObjectToReplace);
        } // for ...tokensToOptimisticReplace.

        // A list of ManagedObjects to delete, this lists forces the ManagedObjects to
        // remain in storage while we give them to the Transaction.
        java.util.List managedObjectsToDelete = new java.util.ArrayList(tokensToDelete.size());
        // Check that the tokensToDelete all have an underlying managedObject because they may have been deleted
        // from the ObjectStore already.

        for (java.util.Iterator tokenIterator = tokensToDelete.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();

            ManagedObject existingManagedObject = token.getManagedObject();
            if (existingManagedObject == null) {
                // The object may have already been deleted from the ObjectStore,
                // so create a dummy object to keep the transaction happy.
                // The Token will have the ObjecStore and storedObjectIdentifier so the the correct delete in the
                // ObjectStore can take place.
                DummyManagedObject dummyManagedObject = new DummyManagedObject("Created by TransactionOptimisticReplaceLogRecord.performRecovery() (Delete)");
                existingManagedObject = token.setManagedObject(dummyManagedObject);
                existingManagedObject.state = ManagedObject.stateReady;
            } // if(existingManagedObject == null).
            managedObjectsToDelete.add(existingManagedObject);
        } // for tokensToDelete...

        // Check that the tokensToNotify all have an underlying managedObject because they may have been deleted
        // by a subsequent transaction. If we create any dummy objects we must hold a strong reference to them
        // until after the transactionForRecovery.optimisticReplace method has run, otherwise the garbage
        // collector might remove them from the weak reference in Token, and it wont be in. the ObjectStore.
        java.util.List dummyManagedObjectsToNotify = new java.util.ArrayList();

        for (java.util.Iterator tokenIterator = tokensToNotify.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();

            ManagedObject existingManagedObject = token.getManagedObject();
            if (existingManagedObject == null) {
                // The object may have already been deleted from the ObjectStore,
                // so create a dummy object to keep the transaction happy.
                // The Token will have the ObjecStore and storedObjectIdentifier so the the correct delete in the
                // ObjectStore can take place.
                DummyManagedObject dummyManagedObject = new DummyManagedObject("Created by TransactionOptimisticReplaceLogRecord.performRecovery() (Notify)");
                dummyManagedObject.state = ManagedObject.stateReady;
                dummyManagedObjectsToNotify.add(dummyManagedObject);
                token.setManagedObject(dummyManagedObject);
            } // if(existingManagedObject == null).
        } // for tokensToNotify...

        transactionForRecovery.optimisticReplace(managedObjectsToAdd,
                                                 managedObjectsToOptimisticReplace,
                                                 managedObjectsToDelete,
                                                 tokensToNotify);
        transactionForRecovery.internalTransaction.resetState(transactionState);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "performRecovery");
    } // performRecovery().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogRecord#maximumSerializedSize()
     */
    protected static long maximumSerializedSize()
    {
        return 4 // Log Record Type.
               + 4 // Number Of Tokens to add.
               + 4 // Number of Tokens to optimisticReplace.
               + 4 // Number of tokens to delete.
               + 4 // Number of Tokens to notify.
               + LogicalUnitOfWork.maximumSerializedSize()
               + 4 // Transaction State.
        ;
    } // maximumSerializedSize().

}// End of class TransactionOptimisticReplaceLogRecord.
