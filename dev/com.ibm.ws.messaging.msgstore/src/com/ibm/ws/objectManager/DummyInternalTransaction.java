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
 * A place holder for completed InternalTransactions which always throws
 * InvalidStateException if used.
 * 
 * @author IBM Corporation
 */
class DummyInternalTransaction
                extends InternalTransaction
{
    private static final Class cclass = DummyInternalTransaction.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);

    /**
     * Constructor. This transaction is not registered with the ObjectManager.
     * 
     * @param objectManagerState of the ObjectManager which will own the InternalTransaction.
     * @param logicalUnitOfWork that identifies the transaction.
     * @throws ObjectManagerException
     */
    protected DummyInternalTransaction(ObjectManagerState objectManagerState,
                                       LogicalUnitOfWork logicalUnitOfWork)
        throws ObjectManagerException {
        super(objectManagerState,
              logicalUnitOfWork);
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { objectManagerState,
                                      logicalUnitOfWork });

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // DummyInternalTransaction().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#add(com.ibm.ws.objectManager.ManagedObject, com.ibm.ws.objectManager.Transaction, long)
     */
    protected synchronized void add(ManagedObject managedObject,
                                    Transaction transaction,
                                    long logSpaceReservedDelta)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#addFromCheckpoint(com.ibm.ws.objectManager.ManagedObject, com.ibm.ws.objectManager.Transaction)
     */
    protected synchronized void addFromCheckpoint(ManagedObject managedObject,
                                                  Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#backout(boolean, com.ibm.ws.objectManager.Transaction)
     */
    public synchronized void backout(boolean reUse,
                                     Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#checkpoint(long)
     */
    protected synchronized void checkpoint(long forcedLogSequenceNumber)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#commit(boolean, com.ibm.ws.objectManager.Transaction)
     */
    public synchronized void commit(boolean reUse,
                                    Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#delete(com.ibm.ws.objectManager.ManagedObject, com.ibm.ws.objectManager.Transaction, long)
     */
    public synchronized void delete(ManagedObject managedObject,
                                    Transaction transaction,
                                    long logSpaceReservedDelta)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#deleteFromCheckpoint(com.ibm.ws.objectManager.ManagedObject, com.ibm.ws.objectManager.Transaction)
     */
    protected synchronized void deleteFromCheckpoint(ManagedObject managedObject,
                                                     Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#getIncludedManagedObjects()
     */
    protected java.util.Collection getIncludedManagedObjects()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#getState()
     */
    protected int getState()
    {
        return Transaction.stateTerminated;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#getXID()
     */
    protected byte[] getXID()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#lock(com.ibm.ws.objectManager.ManagedObject)
     */
    protected synchronized void lock(ManagedObject managedObject)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#notifyFromCheckpoint(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    protected synchronized void notifyFromCheckpoint(Token token,
                                                     Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#optimisticReplace(java.util.List, java.util.List, java.util.List, java.util.List, com.ibm.ws.objectManager.Transaction,
     * long)
     */
    protected synchronized void optimisticReplace(java.util.List managedObjectsToAddReplace,
                                                  java.util.List managedObjectsToReplace,
                                                  java.util.List managedObjectsToDelete,
                                                  java.util.List tokensToNotify,
                                                  Transaction transaction,
                                                  long logSpaceReservedDelta)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#optimisticReplaceFromCheckpoint(com.ibm.ws.objectManager.ManagedObject, com.ibm.ws.objectManager.Transaction)
     */
    protected synchronized void optimisticReplaceFromCheckpoint(ManagedObject managedObject,
                                                                Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#postBackout(com.ibm.ws.objectManager.Transaction)
     */
    void postBackout(Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#postCommit(com.ibm.ws.objectManager.Transaction)
     */
    void postCommit(Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#preBackout(com.ibm.ws.objectManager.Transaction)
     */
    void preBackout(Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#preCommit(com.ibm.ws.objectManager.Transaction)
     */
    void preCommit(Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#prepare(com.ibm.ws.objectManager.Transaction)
     */
    public synchronized void prepare(Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#prePrepare(com.ibm.ws.objectManager.Transaction)
     */
    void prePrepare(Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#recoveryCompleted(com.ibm.ws.objectManager.Transaction)
     */
    protected void recoveryCompleted(Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#replace(com.ibm.ws.objectManager.ManagedObject, com.ibm.ws.objectManager.Transaction, long)
     */
    protected synchronized void replace(ManagedObject managedObject,
                                        Transaction transaction,
                                        long logSpaceReservedDelta)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#replaceFromCheckpoint(com.ibm.ws.objectManager.ManagedObject, byte[], com.ibm.ws.objectManager.Transaction)
     */
    public synchronized void replaceFromCheckpoint(ManagedObject managedObject,
                                                   byte[] serializedBytes,
                                                   Transaction transaction)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#requestCallback(com.ibm.ws.objectManager.Token)
     */
    protected synchronized void requestCallback(Token tokenToPrePrepare)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.InternalTransaction#setXID(byte[])
     */
    protected synchronized void setXID(byte[] XID)
                    throws ObjectManagerException
    {
        throw new InvalidStateException(this, InternalTransaction.stateTerminated, InternalTransaction.stateNames[InternalTransaction.stateTerminated]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "DummyInternalTransaction";
    }
} // end of class DummyInternalTransaction.    
