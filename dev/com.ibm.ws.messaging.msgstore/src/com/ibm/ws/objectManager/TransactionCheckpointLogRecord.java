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
 * TransactionCheckpointLogRecord contains log information to reinstate a transaction after a
 * checkpoint. Note that this only logs the action on a ManagedObject unlike the initial log records
 * which also contain added or replaced images of the ManagedObjects. After this record is written
 * we can truncate the log and loose the original log records The new images for add and Optimistivc
 * replaceare in the ObjectStores, for replace they are rewritten to the log as part of this log
 * record.
 */
class TransactionCheckpointLogRecord extends LogRecord {
    private static final Class cclass = TransactionCheckpointLogRecord.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);
    private static final long serialVersionUID = 1869839597051819424L;

    // The logicalUnitOfWork that the add operation belongs to.
    protected LogicalUnitOfWork logicalUnitOfWork;
    // The state of the transaction.
    protected int transactionState;
    // The list ManagedObject tokens replace.
    private java.util.Collection tokensToAdd;
    // The list ManagedObject tokens replace.
    private java.util.Collection tokensToReplace;
    // The list ManagedObject serialized bytes replace.
    private java.util.Collection serializedBytesToReplace;
    // The list ManagedObject tokens optimistic replace.
    private java.util.Collection tokensToOptimisticReplace;
    // The list ManagedObject tokens replace.
    private java.util.Collection tokensToDelete;
    // The list ManagedObject tokens notify.
    private java.util.Collection tokensToNotify;

    /**
     * Construct a LogRecord and prepare its buffers ready to write to the log.
     * 
     * @param internalTransaction performing the checkpoint.
     * @param tokensToAdd the Tokens representing persistent ManagedObjects to be added.
     * @param tokensToReplace the Tokens representing persistent ManagedObjects to be replaced.
     * @param serializedBytesToReplace the serialized bytes representing persistent ManagedObjects
     *            to be replaced.
     * @param tokensToOptimisticReplace the Tokens representing persistent ManagedObjects to be
     *            optimistacally replaced.
     * @param tokensToDelete the Tokens representing persistent ManagedObjects to be Deleted.
     * @param tokensToNotify the Tokens representing persistent ManagedObjects to be notified.
     * @throws ObjectManagerException
     */
    protected TransactionCheckpointLogRecord(InternalTransaction internalTransaction,
                                             java.util.Collection tokensToAdd,
                                             java.util.Collection tokensToReplace,
                                             java.util.Collection serializedBytesToReplace,
                                             java.util.Collection tokensToOptimisticReplace,
                                             java.util.Collection tokensToDelete,
                                             java.util.Collection tokensToNotify)
        throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { internalTransaction,
                                      tokensToAdd,
                                      tokensToReplace,
                                      serializedBytesToReplace,
                                      tokensToOptimisticReplace,
                                      tokensToDelete,
                                      tokensToNotify });

        this.logicalUnitOfWork = internalTransaction.getLogicalUnitOfWork();
        this.transactionState = internalTransaction.getState();
        this.tokensToAdd = tokensToAdd;
        this.tokensToReplace = tokensToReplace;
        this.serializedBytesToReplace = serializedBytesToReplace;
        this.tokensToOptimisticReplace = tokensToOptimisticReplace;
        this.tokensToDelete = tokensToDelete;
        this.tokensToNotify = tokensToNotify;

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
     * @param objectManagerState of the objectManager reconstructing the LogRecord.
     * @throws ObjectManagerException
     */
    protected TransactionCheckpointLogRecord(java.io.DataInputStream dataInputStream,
                                             ObjectManagerState objectManagerState)
        throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        "DataInputStream=" + dataInputStream + ", ObjectManagerState=" + objectManagerState);

        try {
            // LogRecord.type already read.
            logicalUnitOfWork = new LogicalUnitOfWork(dataInputStream);
            transactionState = dataInputStream.readInt();
            int numberOfTokensToAdd = dataInputStream.readInt();
            tokensToAdd = new java.util.ArrayList(numberOfTokensToAdd);
            int numberOfTokensToReplace = dataInputStream.readInt();
            tokensToReplace = new java.util.ArrayList(numberOfTokensToReplace);
            serializedBytesToReplace = new java.util.ArrayList(numberOfTokensToReplace);
            int numberOfTokensToOptimisticReplace = dataInputStream.readInt();
            tokensToOptimisticReplace = new java.util.ArrayList(numberOfTokensToOptimisticReplace);
            int numberOfTokensToDelete = dataInputStream.readInt();
            tokensToDelete = new java.util.ArrayList(numberOfTokensToDelete);
            int numberOfTokensToNotify = dataInputStream.readInt();
            tokensToNotify = new java.util.ArrayList(numberOfTokensToNotify);

            // Load the tokens to add.
            for (int i = 0; i < numberOfTokensToAdd; i++) {
                Token token = Token.restore(dataInputStream,
                                            objectManagerState);
                tokensToAdd.add(token);
            } // for tokensToAdd.size().

            // Load the tokens to replace.
            int[] serializedByteLength = new int[numberOfTokensToReplace];
            for (int i = 0; i < numberOfTokensToReplace; i++) {
                Token token = Token.restore(dataInputStream,
                                            objectManagerState);
                tokensToReplace.add(token);
                serializedByteLength[i] = dataInputStream.readInt();
            } // for tokensToReplace.size().

            // Load the tokens to optimistic replace.
            for (int i = 0; i < numberOfTokensToOptimisticReplace; i++) {
                Token token = Token.restore(dataInputStream,
                                            objectManagerState);
                tokensToOptimisticReplace.add(token);
            } // for tokensToOptimisticReplace.size().

            // Load the tokens to delete.
            for (int i = 0; i < numberOfTokensToDelete; i++) {
                Token token = Token.restore(dataInputStream,
                                            objectManagerState);
                tokensToDelete.add(token);
            } // for tokensToDelete.size().

            // Load the tokens to notify.
            for (int i = 0; i < numberOfTokensToNotify; i++) {
                Token token = Token.restore(dataInputStream,
                                            objectManagerState);
                tokensToNotify.add(token);
            } // for tokensToNotify.size().

            // Load the serialized managedObjects to replace.
            for (int i = 0; i < numberOfTokensToReplace; i++) {
                byte[] serializedBytes = new byte[serializedByteLength[i]];
                dataInputStream.read(serializedBytes);
                serializedBytesToReplace.add(serializedBytes);
            } // for tokensToReplace.size().

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(cclass, "<init>", exception, "1:190:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>",
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } //  catch (java.io.IOException exception).

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
    protected ObjectManagerByteArrayOutputStream[] getBuffers(ObjectManagerByteArrayOutputStream byteArrayOutputStream)
                    throws ObjectManagerException {
        final String methodName = "getBuffers";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { byteArrayOutputStream });

        ObjectManagerByteArrayOutputStream[] buffers = new ObjectManagerByteArrayOutputStream[1 + tokensToReplace.size()];

        // Create the buffer to contain the header for this log record.
        byteArrayOutputStream.reset();
        buffers[0] = byteArrayOutputStream;
        java.io.DataOutputStream dataOutputStream = new java.io.DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeInt(LogRecord.TYPE_CHECKPOINT_TRANSACTION);
            logicalUnitOfWork.writeObject(dataOutputStream);
            dataOutputStream.writeInt(transactionState);
            dataOutputStream.writeInt(tokensToAdd.size());
            dataOutputStream.writeInt(tokensToReplace.size());
            dataOutputStream.writeInt(tokensToOptimisticReplace.size());
            dataOutputStream.writeInt(tokensToDelete.size());
            dataOutputStream.writeInt(tokensToNotify.size());

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:240:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName, new Object[] { exception });
            throw new PermanentIOException(this,
                                           exception);
        } // catch.

        // Write the Add Tokens.
        for (java.util.Iterator tokenIterator = tokensToAdd.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            token.writeObject(dataOutputStream);
        } // for ... tokensToAdd.

        // Write the Replace Tokens.
        java.util.Iterator serializedBytesIterator = serializedBytesToReplace.iterator();
        int ibuffer = 1;
        for (java.util.Iterator tokenIterator = tokensToReplace.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            token.writeObject(dataOutputStream);
            ObjectManagerByteArrayOutputStream managedObjectBytes = (ObjectManagerByteArrayOutputStream) serializedBytesIterator.next();
            try {
                dataOutputStream.writeInt(managedObjectBytes.getCount());
            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:267:1.8");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               exception);
                throw new PermanentIOException(this,
                                               exception);
            } // catch.
              // Add the the serialized image of the ManagedObject after the header containing all of the
              // tokens.
            buffers[ibuffer++] = managedObjectBytes;
        } // for ... tokensToReplace.

        // Write the Optimistic Replace Tokens.
        for (java.util.Iterator tokenIterator = tokensToOptimisticReplace.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            token.writeObject(dataOutputStream);
        } // for ... tokensToOptimisticReplace.

        // Write the Delete Tokens.
        for (java.util.Iterator tokenIterator = tokensToDelete.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            token.writeObject(dataOutputStream);
        } // for ... tokensToDelete.

        // Write the Notify Tokens.
        for (java.util.Iterator tokenIterator = tokensToNotify.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            token.writeObject(dataOutputStream);
        } // for ... tokensToNotify.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
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
                        objectManagerState);

        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(this,
                        cclass,
                        "logicalUnitOfWork=" + logicalUnitOfWork + "(LogicalUnitOfWork)"
                                        + "\n transactionState=" + transactionState + "(int)"
                                        + "\n tokensToAdd=" + tokensToAdd + "(java.util.Collection)"
                                        + "\n tokensToReplace=" + tokensToReplace + "(java.util.Collecton)"
                                        + "\n serializedBytesToReplace=" + serializedBytesToReplace + "(java.util.Collection)"
                                        + "\n tokensToOptimisticReplace=" + tokensToOptimisticReplace + "(java.util.Collection)"
                                        + "\n tokensToDelete=" + tokensToDelete + "(java.util.Collection)"
                                        + "\n tokensToNotify=" + tokensToNotify + "(java.util.Collection)");

        // In principle we should test to see if objectManagerState.checkpointEndSee is true, 
        // because if it is we dont need to do this processing because the transaction is 
        // already in its correct state, however there is also no harm in processing this again. 
        // We have started an new checkpoint but not completed it.

        // Redo the operations of the ManagedObject using its original Transaction.
        Transaction transactionForRecovery = objectManagerState.getTransaction(logicalUnitOfWork);

        // Recover the Added ManagedObjects.
        for (java.util.Iterator tokenIterator = tokensToAdd.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            ManagedObject existingManagedObject = token.getManagedObject();
            // If a subsequent transaction has deleted the ManagedObject we dont need to recover it.
            if (existingManagedObject != null) {
                // Revert to constructed state.
                existingManagedObject.state = ManagedObject.stateConstructed;
                transactionForRecovery.addFromCheckpoint(existingManagedObject);
            } // if(existingManagedObject != null).
        } // for ... tokensToAdd.

        // Recover the Replace ManagedObjects.
        java.util.Iterator serializedBytesIterator = serializedBytesToReplace.iterator();
        for (java.util.Iterator tokenIterator = tokensToReplace.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            ManagedObject existingManagedObject = token.getManagedObject();
            byte[] managedObjectBytes = (byte[]) serializedBytesIterator.next();
            ManagedObject replacementManagedObject = ManagedObject.restoreFromSerializedBytes(managedObjectBytes,
                                                                                              objectManagerState);
            // If a subsequent transaction has deleted the ManagedObject we dont need to recover it.
            if (existingManagedObject != null) {
                // Replace what we already have with this version.
                transactionForRecovery.lock(existingManagedObject);
                // Make the underlying object the way it was when we made the original transaction.replace()
                // call.
                token.setManagedObject(replacementManagedObject);
                transactionForRecovery.replaceFromCheckpoint(existingManagedObject,
                                                             managedObjectBytes);
            } // if(existingManagedObject != null).
        } // for ... tokensToReplace.

        // Recover the Optimistic Replace ManagedObjects.
        for (java.util.Iterator tokenIterator = tokensToOptimisticReplace.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            ManagedObject existingManagedObject = token.getManagedObject();
            // If a subsequent transaction has deleted the ManagedObject we dont need to recover it.
            if (existingManagedObject != null) {
                transactionForRecovery.optimisticReplaceFromCheckpoint(existingManagedObject);
            } // if(existingManagedObject != null).
        } // for ... tokensToOptimisticReplace.

        // Recover the Delete ManagedObjects.
        for (java.util.Iterator tokenIterator = tokensToDelete.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            ManagedObject existingManagedObject = token.getManagedObject();
            // If the object has already been deleted we need not do anything.
            if (existingManagedObject != null) {
                transactionForRecovery.deleteFromCheckpoint(existingManagedObject);
            } // if(existingManagedObject != null).
        } // for ... tokensToDelete.

        // Recover the Notify ManagedObjects.
        for (java.util.Iterator tokenIterator = tokensToNotify.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            ManagedObject existingManagedObject = token.getManagedObject();
            // If the object has already been deleted we need not do anything.
            if (existingManagedObject != null) {
                transactionForRecovery.notifyFromCheckpoint(token);
            } // if(existingManagedObject != null).
        } // for ... tokensToNotify.

        transactionForRecovery.internalTransaction.resetState(transactionState);

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
               + LogicalUnitOfWork.maximumSerializedSize()
               + 4 // Transaction State.
               + 4 // Number of Tokens to Add. 
               + 4 // Number of Tokens to Replace.
               + 4 // Number Of Tokens to OptimisticReplace.
               + 4 // Number of Tokens to Delete.
               + 4 // Number of Tokens to Notify.
        ;
    } // Of maximumSerializedSize().

} // End of class TransactionCheckpointLogRecord.
