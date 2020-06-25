package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.objectManager.utils.Printable;
import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * <p>
 * Transaction contains the InternalTransaction.
 * 
 * State definitions.
 * int stateError A state error has occured.
 * int stateInactive No transactional work has been done.
 * int stateActiveNonPersistent Transactional work has been done but only involving nonpersistent objects.
 * int stateActivePersistent Transactional work has been done involving persistent objects.
 * int statePrePreparedInactive The transaction is prePrepared but no transactional work has been done.
 * int statePrePreparedNonPersistent The transaction is prePrepared but only involves nonpersistent objects.
 * int statePrePreparedPersistent The transaction is prePrepared and involves persistent objects.
 * int statePreparedInactive The transaction is prepared but no transactioanl work was done.
 * int statePreparedNonPersistent The transaction is prepared but only involves nonpersistent objects.
 * int statePreparedPersistent The transaction is prepared and involves persistent objects.
 * int stateCommitingInactive The transaction must commit but no transactioanl work was done.
 * int stateCommitingNonPersistent The transaction must commit but only involves nonpersistent objects.
 * int stateCommitingPersistent The transaction must commit and it involves persistent objects.
 * int stateBackingOutInactive The transaction must backout but no transactioanl work was done.
 * int stateBackingOutNonPersistent The transaction must backout but only involves nonpersistent objects.
 * int stateBackingOutPersistent The transaction must backout and it involves persistent objects.
 * int stateTerminated The transaction is commited or backed out.
 * 
 * @author IBM Corporation
 */

public class Transaction
                implements Printable
{
    private static final Class cclass = Transaction.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);

    /*---------------------- Define the state machine (begin) ----------------------*/
    // Transaction states.
    // See above for deinitions of the states.
    public static final int stateError = InternalTransaction.stateError;
    public static final int stateInactive = InternalTransaction.stateInactive;
    public static final int stateActiveNonPersistent = InternalTransaction.stateActiveNonPersistent;
    public static final int stateActivePersistent = InternalTransaction.stateActivePersistent;
    public static final int statePrePreparedInactive = InternalTransaction.statePrePreparedInactive;
    public static final int statePrePreparedNonPersistent = InternalTransaction.statePrePreparedNonPersistent;
    public static final int statePrePreparedPersistent = InternalTransaction.statePrePreparedPersistent;
    public static final int statePreparedInactive = InternalTransaction.statePreparedInactive;
    public static final int statePreparedNonPersistent = InternalTransaction.statePreparedNonPersistent;
    public static final int statePreparedPersistent = InternalTransaction.statePreparedPersistent;
    public static final int stateCommitingInactive = InternalTransaction.stateCommitingInactive;
    public static final int stateCommitingNonPersistent = InternalTransaction.stateCommitingNonPersistent;
    public static final int stateCommitingPersistent = InternalTransaction.stateCommitingPersistent;
    public static final int stateBackingOutInactive = InternalTransaction.stateBackingOutInactive;
    public static final int stateBackingOutNonPersistent = InternalTransaction.stateBackingOutNonPersistent;
    public static final int stateBackingOutPersistent = InternalTransaction.stateBackingOutPersistent;
    public static final int stateTerminated = InternalTransaction.stateTerminated;

    /*---------------------- Define the state machine (end) ------------------------*/

    // Termination reasons.
    public static final int terminatedNotTerminated = 0;
    public static final int terminatedByUser = 1;
    public static final int terminatedShutdown = 2;
    public static final int terminatedLogTooFull = 3;
    public static final int terminatedOrphanBackedOut = 4;
    public static final int terminatedDuplicate = 5;
    public static final String[] terminationReasonNames = new String[] { "NotTerminated",
                                                                        "User",
                                                                        "Shutdown",
                                                                        "LogTooFull",
                                                                        "OrphanBackedOut",
                                                                        "Duplicate" };
    private int terminationReason = terminatedByUser;

    // The underlying InternalTransaction.
    protected volatile InternalTransaction internalTransaction;

    /**
     * Constructor.
     * 
     * @param internalTransaction contained by this Transaction.
     */
    protected Transaction(InternalTransaction internalTransaction)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        internalTransaction);

        this.internalTransaction = internalTransaction;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // End of Constructor.

    /**
     * <p>
     * 
     * @return int state of the objectManagerState owning this Transaction.
     *         </p>
     */
    public int getObjectManagerStateState()
    {
        return internalTransaction.objectManagerState.getObjectManagerStateState();
    } // End of getObjectManagerStateState method.

    /**
     * <p>
     * 
     * @return int the state of the InternalTransaction.
     *         </p>
     */
    public int getState()
    {
        return internalTransaction.getState();
    } // getState().

    /**
     * @return LogicalUnitOfWork identifying the Transaction.
     */
    public LogicalUnitOfWork getLogicalUnitOfWork()
    {
        return internalTransaction.getLogicalUnitOfWork();
    } // of getLogicalUnitOfWork().

    /**
     * <p>
     * Returns the XID identifying the transaction or null if has not been set.
     * 
     * @return byte[] The Xopen identifier of the transaction, or null.
     *         </p>
     */
    public byte[] getXID()
    {
        return internalTransaction.getXID();
    } // getXID().

    /**
     * <p>
     * Sets the XID identifying the transaction if has not been set already. No
     * check is made on the uniqueness of the XID.
     * </p>
     * 
     * @param XID The Xopen identifier of the transaction, or null.
     * @throws ObjectManagerException
     */
    public void setXID(byte[] XID)
                    throws ObjectManagerException
    {
        internalTransaction.setXID(XID);

    } // setXID().

    /**
     * @return java.util.Collection the includedManagedObjects.
     */
    public java.util.Collection getIncludedManagedObjects()
    {
        return internalTransaction.getIncludedManagedObjects();
    }

    /**
     * @return long the current ObjectManagerState transactionUnlockSequence.
     */
    public long getTransactionUnlockSequence()
    {
        return internalTransaction.objectManagerState.getTransactionUnlockSequence();
    }

    /**
     * <p>
     * Note a request for the Token in the transaction to be called at prePrepare,
     * preCommit, preBackout, postCommit and postBackout.
     * </p>
     * 
     * @param tokenToCallBack who's ManagedObject is to be called back.
     * @throws ObjectManagerException
     * @throws InvalidStateException if the tansaction has started its prepare processing.
     */
    public void requestCallback(Token tokenToCallBack)
                    throws ObjectManagerException
    {
        internalTransaction.requestCallback(tokenToCallBack, this);
    } // requestCallback().

    /**
     * <p>
     * Lock a ManagedObject for use by this transaction. It is necessary to lock
     * an object if you don't want it used by another transaction while it is
     * included in this one. When an object is locked we create its before image
     * so that the default backout action can then restore it if a backout occurs.
     * You must lock a ManagedObject before replacing it so that the default
     * rollback action is enabled. Use optimisticReplace if you dont want to take
     * a lock, but if you do this, you have to compensate for any actions yourself
     * if the transaction is rolled back.
     * 
     * Add and delete operations also take this kind of lock automatically, as a
     * precaution to prevent other transactions attempting another Add, Delete or
     * Replace operation.
     * </p>
     * 
     * @param managedObject to be locked.
     * @throws ObjectManagerException
     */
    public void lock(ManagedObject managedObject)
                    throws ObjectManagerException
    {
        internalTransaction.lock(managedObject);
    } // End of lock method.

    /**
     * Add an after image of an object invloved in the transaction. Objects added
     * are protected by this unit of work. After any updates are made these are
     * written to the log if the managedObject is persistent and later written to
     * the Objectstore.
     * 
     * @param managedObject to be added under the scope of the Transaction.
     * @throws ObjectManagerException
     */
    public void add(ManagedObject managedObject)
                    throws ObjectManagerException
    {
        internalTransaction.add(managedObject,
                                this,
                                0);
    } // add().

    /**
     * Add an after image of an object invloved in the transaction. Objects added
     * are protected by this unit of work. After any updates are made these are
     * written to the log if the managedObject is persistent and later written to
     * the Objectstore.
     * 
     * @param managedObject to be added under the scope of the Transaction.
     * @param logSpaceReservedDelta extra log space the caller wants reserved until
     *            the transaction completes.
     * @throws ObjectManagerException
     */
    public void add(ManagedObject managedObject,
                    long logSpaceReservedDelta)
                    throws ObjectManagerException {
        internalTransaction.add(managedObject,
                                this,
                                logSpaceReservedDelta);
    } // add().

    /**
     * Recover an Add operation from a checkpoint.
     * 
     * @param managedObject recovered from a checkpoint LogRecord.
     * @throws ObjectManagerException
     */
    protected void addFromCheckpoint(ManagedObject managedObject)
                    throws ObjectManagerException {
        internalTransaction.addFromCheckpoint(managedObject,
                                              this);
    } // addFromCheckpoint().

    /**
     * <p>
     * Add an after image of an object invloved in the transaction. The Object
     * must be Locked before it can be replaced and the lock is held until after
     * the transaction commits of backs out.
     * </p>
     * <p>
     * This operation has the same end result as delete followed by add except
     * that the after image is stored only in memory until after the transaction
     * commits so that the before immage in the object store is not damaged in the
     * case of a backout being required. This costs more storage. It is also more
     * difficult for the object store that stores the object to make efficient use
     * of its storage if the replacement is a different size to the original. The
     * advantage of using replace is that it retains the original object store
     * identifier, so references to this need not be changed.
     * </p>
     * 
     * @param managedObject to be replaced.
     * @throws ObjectManagerException
     */
    public void replace(ManagedObject managedObject)
                    throws ObjectManagerException {
        internalTransaction.replace(managedObject,
                                    this,
                                    0);
    } // replace().

    /**
     * <p>
     * Add an after image of an object invloved in the transaction. The Object
     * must be Locked before it can be replaced and the lock is held until after
     * the transaction commits of backs out.
     * </p>
     * <p>
     * This operation has the same end result as delete followed by add except
     * that the after image is stored only in memory until after the transaction
     * commits so that the before immage in the object store is not damaged in the
     * case of a backout being required. This costs more storage. It is also more
     * difficult for the object store that stores the object to make efficient use
     * of its storage if the replacement is a different size to the original. The
     * advantage of using replace is that it retains the original object store
     * identifier, so references to this need not be changed.
     * </p>
     * 
     * @param managedObject to be replaced.
     * @param logSpaceReservedDelta extra log space the caller wants reserved
     *            until the transaction completes.
     * @throws ObjectManagerException
     */
    public void replace(ManagedObject managedObject,
                        long logSpaceReservedDelta)
                    throws ObjectManagerException {
        internalTransaction.replace(managedObject,
                                    this,
                                    logSpaceReservedDelta);
    } // replace().

    /**
     * Recover a Replace operation from a checkpoint.
     * 
     * @param managedObject recovered from a checkpoint.
     * @param serializedBytes representing the serialized form of the ManagedObject originally
     *            logged.
     * @throws ObjectManagerException
     */
    protected void replaceFromCheckpoint(ManagedObject managedObject,
                                         byte[] serializedBytes)
                    throws ObjectManagerException {
        internalTransaction.replaceFromCheckpoint(managedObject, serializedBytes, this);

    } // replaceFromCheckpoint().

    /**
     * <p>
     * Perform a number of optimistic replace updates to ManagedOjects but writes and single log record with a list of
     * objects to replace. Optimistic replace does not required the ManagedObject to be in locked state, it can be in
     * Added,Locked,Replaced,Deleted,Ready states and be used by another transaction However, it is up to the code
     * updating the object to correctly reverse the changes at the time any backout of this transaction occurs. Because
     * the transaction is not locked no before immage of the object is kept. Unlike standard replace we do not need to
     * hold the update out of the ObjectStore until commit time because the updating code is responsible for reversing the
     * effects of the update. These compensating changes can be made after the transaction has prepared.
     * </p>
     * <p>
     * The advantage of this method is that all of the updated objects are handled together, so that they all make it into
     * the log for restart. If they were added to the log separately we might fail and see some of them at restart and not
     * others This allows objects such as lists to make several associated changes without having to worry about
     * compensating for the intermediate states.
     * </p>
     * 
     * @param managedObjectsToAdd List of ManagedObjects to add or null if there are none.
     * @param managedObjectsToReplace List of ManagedObjects to be Optimistically replaced, or null if there are none.
     * @param managedObjectsToDelete List of ManagedObjects to delete, or null if there are none.
     * @param tokensToNotify List of tokens to be notified once the replace is logged, or null if there are none.
     * @throws ObjectManagerException
     */
    public void optimisticReplace(java.util.List managedObjectsToAdd,
                                  java.util.List managedObjectsToReplace,
                                  java.util.List managedObjectsToDelete,
                                  java.util.List tokensToNotify)
                    throws ObjectManagerException {
        internalTransaction.optimisticReplace(managedObjectsToAdd,
                                              managedObjectsToReplace,
                                              managedObjectsToDelete,
                                              tokensToNotify,
                                              this,
                                              0);
    } // optimisticReplace().

    /**
     * <p>
     * Perform a number of optimistic replace updates to ManagedOjects but writes
     * and single log record with a list of objects to replace. Optimistic replace
     * does not required the ManagedObject to be in locked state, it can be in
     * Added,Locked,Replaced,Deleted,Ready states and be used by another
     * transaction However, it is up to the code updating the object to correctly
     * reverse the changes at the time any backout of this transaction occurs.
     * Because the transaction is not locked no before immage of the object is
     * kept. Unlike standard replace we do not need to hold the update out of the
     * ObjectStore until commit time because the updating code is responsible for
     * reversing the effects of the update. These compensating changes can be made
     * after the transaction has prepared.
     * </p>
     * <p>
     * The advantage of this method is that all of the updated objects are handled
     * together, so that they all make it into the log for restart. If they were
     * added to the log separately we might fail and see some of them at restart
     * and not others This allows objects such as lists to make several associated
     * changes without having to worry about compensating for the intermediate
     * states.
     * </p>
     * 
     * @param managedObjectsToAdd List of ManagedObjects to add,
     *            or null if there are none.
     * @param managedObjectsToReplace List of ManagedObjects to be Optimistically replaced,
     *            or null if there are none.
     * @param managedObjectsToDelete List of ManagedObjects to delete,
     *            or null if there are none.
     * @param tokensToNotify List of tokens to be notified once the replace is logged,
     *            or null if there are none.
     * @param logSpaceReservedDelta extra log space the caller wants reserved until the transaction
     *            completes.
     * @throws ObjectManagerException
     */
    public void optimisticReplace(java.util.List managedObjectsToAdd,
                                  java.util.List managedObjectsToReplace,
                                  java.util.List managedObjectsToDelete,
                                  java.util.List tokensToNotify,
                                  long logSpaceReservedDelta)
                    throws ObjectManagerException {
        internalTransaction.optimisticReplace(managedObjectsToAdd,
                                              managedObjectsToReplace,
                                              managedObjectsToDelete,
                                              tokensToNotify,
                                              this,
                                              logSpaceReservedDelta);
    } // optimisticReplace().

    /**
     * <p>
     * Reinstate the state of an ManagedObject to be optimistaclly replaced after
     * reading a TransactionCheckpointLogRecord.
     * </p>
     * 
     * @param ManagedObject
     *            to be optimistically replaced.
     */
    protected void optimisticReplaceFromCheckpoint(ManagedObject managedObject)
                    throws ObjectManagerException
    {
        internalTransaction.optimisticReplaceFromCheckpoint(managedObject,
                                                            this);
    } // optimisticReplaceFromCheckpoint().

    /**
     * <p>
     * Reinstate the notification of a ManagedObject after recovery from a
     * checkpoint.
     * </p>
     * 
     * @param Token
     *            to be notified.
     */
    protected void notifyFromCheckpoint(Token token)
                    throws ObjectManagerException
    {
        internalTransaction.notifyFromCheckpoint(token,
                                                 this);
    } // End of notifyFromCheckpoint method.

    /**
     * <p>
     * These ManagedObjects cease to exist after the transaction commits or are
     * reinstated if it backs out. When the unit of work commits, these objects
     * are deleted from their object store.
     * </p>
     * 
     * @param ManagedObject
     *            to be deleetd.
     */
    public void delete(ManagedObject managedObject)
                    throws ObjectManagerException
    {
        internalTransaction.delete(managedObject,
                                   this,
                                   0);
    } // End of delete method.

    /**
     * <p>
     * These ManagedObjects cease to exist after the transaction commits or are
     * reinstated if it backs out. When the unit of work commits, these objects
     * are deleted from their object store.
     * </p>
     * 
     * @param managedObject to be deleetd.
     * @param logSpaceReservedDelta extra log space the caller wants reserved until the transaction
     *            completes.
     * @throws ObjectManagerException
     */
    public void delete(ManagedObject managedObject,
                       long logSpaceReservedDelta)
                    throws ObjectManagerException {
        internalTransaction.delete(managedObject, this, logSpaceReservedDelta);
    } // delete().

    /**
     * <p>
     * Recreate a delete operation from a checkpoint read from the transaction log.
     * </p>
     * 
     * @param managedObject to be deleted.
     */
    protected void deleteFromCheckpoint(ManagedObject managedObject)
                    throws ObjectManagerException {
        internalTransaction.deleteFromCheckpoint(managedObject, this);
    } // deleteFromCheckpoint().

    /**
     * Prepare the transaction. After execution of this method the users of this transaction shall not perform any further
     * work as part of the logical unit of work, or modify any of the objects that are referenced by the transaction.
     * 
     * We have already spooled the add and delete log records for persistent objects. Since we are performing a two phase
     * commit we force a prepare record. The Objects already have space reserved in the object store.
     * 
     * @throws ObjectManagerException
     */
    public void prepare()
                    throws ObjectManagerException {
        internalTransaction.prepare(this);
    } // prepare().

    /**
     * Commit the transaction. Fore write a commit record to the log. Unlock the
     * objects that are part of this logical unit of work.
     * 
     * @param reUse true indicates that the transaction is terminated and then reused
     *            with another logical unit of work.
     * @throws ObjectManagerException
     */
    public void commit(boolean reUse)
                    throws ObjectManagerException {
        internalTransaction.commit(reUse, this);
    } // commit().

    /**
     * Rollback the transacion. Force a backout record to the log.
     * 
     * @param reUse Indicates whether the transaction is terminated or can be reused for another logical unit of work.
     * @throws ObjectManagerException
     */
    public void backout(boolean reUse)
                    throws ObjectManagerException {
        internalTransaction.backout(reUse, this);
    } // backout().

    /**
     * Called by the ObjectManager when it has finished replaying the log but before it backs out any inflight
     * transactions and starts making forward progress. Any ManagedObjects that have registered for callbacks during
     * recovery wuill now be informed that recovery is completed.
     * 
     * @throws ObjectManagerException
     */
    protected void recoveryCompleted()
                    throws ObjectManagerException
    {
        internalTransaction.recoveryCompleted(this);
    } // recoveryCompleted().

    /**
     * Print a dump of the state.
     * 
     * @param printWriter to be written to.
     */
    public void print(java.io.PrintWriter printWriter)
    {
        internalTransaction.print(printWriter);
    } // print().

    /**
     * Gives the reason a transaction entered Transaction.stateTerminated.
     * <p>
     * Valid reasons are:
     * <ul>
     * <li>terminatedNotTerminated The transaction is not in Transaction.stateTerminated;
     * <li>terminatedByUser The user commited or backed out the tansaction and has not reused it.
     * <li>terminatedShutdown The ObjectManager has shut down.
     * <li>terminatedLogTooFull The ObjectManager backed out the transaction because the log was too full.
     * <li>terminatedOrphanBackedOut The transaction was discovered to be unreferencable by the application and was backed out.
     * </ul>
     * 
     * @return int the terminationReason.
     */
    public int getTerminationReason() {
        if (internalTransaction.getState() != InternalTransaction.stateTerminated)
            return terminatedNotTerminated;
        return terminationReason;
    }

    /**
     * @param terminationReason The terminationReason to set.
     */
    void setTerminationReason(int terminationReason) {
        this.terminationReason = terminationReason;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        if (internalTransaction == null)
            return new String("Transaction"
                              + "(null)"
                              + "/" + Integer.toHexString(hashCode()));
        else
            return new String("Transaction"
                              + "(" + internalTransaction.toString() + ")"
                              + "/" + Integer.toHexString(hashCode()));
    } // toString().

    // for diagnostics only
    public void finalize()
    {
      if (null!=internalTransaction) internalTransaction.finaliseTick = System.currentTimeMillis();
    }
} // class Transaction.
