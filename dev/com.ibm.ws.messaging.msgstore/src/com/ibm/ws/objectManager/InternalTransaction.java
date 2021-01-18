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
 * <p>Transaction manages the state of work is done on ObjectManager
 * managed objects.
 * 
 * @author IBM Corporation
 */
class InternalTransaction
                implements Printable
{
    private static final Class cclass = InternalTransaction.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);

    /*---------------------- Define the state machine (begin) ----------------------*/
    // Transaction states.
    static final int stateError = 0; // A state error has occured.
    static final int stateInactive = 1; // No transactional work has been done.
    static final int stateActiveNonPersistent = 2; // Transactional work has been done but only involving nonpersistent objects.
    static final int stateActivePersistent = 3; // Transactional work has been done involving persistent objects.
    static final int statePrePreparedInactive = 4; // The transaction is prePrepared but no transactional work has been done.
    static final int statePrePreparedNonPersistent = 5; // The transaction is prePrepared but only involves nonpersistent objects.
    static final int statePrePreparedPersistent = 6; // The transaction is prePrepared and involves persistent objects.
    static final int statePreparedInactive = 7; // The transaction is prepared but no transactional work was done.
    static final int statePreparedNonPersistent = 8; // The transaction is prepared but only involves nonpersistent objects.
    static final int statePreparedPersistent = 9; // The transaction is prepared and involves persistent objects.
    static final int stateCommitingInactive = 10; // The transaction must commit but no transactional work was done.
    static final int stateCommitingNonPersistent = 11; // The transaction must commit but only involves nonpersistent objects.
    static final int stateCommitingPersistent = 12; // The transaction must commit and it involves persistent objects.
    static final int stateBackingOutInactive = 13; // The transaction must backout but no transactional work was done.
    static final int stateBackingOutNonPersistent = 14; // The transaction must backout but only involves nonpersistent objects.
    static final int stateBackingOutPersistent = 15; // The transaction must backout and it involves persistent objects.
    static final int stateTerminated = 16; // The transaction is commited or backed out.

    // Also update the table in setRequiresCheckpoint() if updating the state machine.

    // The names of the states for diagnostic purposes.
    static final String stateNames[] = { "Error"
                                        , "Inactive"
                                        , "ActiveNonPersistent"
                                        , "ActivePersistent"
                                        , "PrePreparedInactive"
                                        , "PrePreparedNonPersistent"
                                        , "PrePreparedPersistent"
                                        , "PreparedInactive"
                                        , "PreparedNonPersistent"
                                        , "PreparedPersistent"
                                        , "CommitingInactive"
                                        , "CommitingNonPersistent"
                                        , "CommitingPersistent"
                                        , "BackingOutInactive"
                                        , "BackingOutNonPersistent"
                                        , "BackingOutPersistent"
                                        , "Terminated"
    };

    // What happens when a nonpersistent object becomes involved with the transaction.
    static final int nextStateForInvolveNonPersistentObject[] = { stateError
                                                                 , stateActiveNonPersistent
                                                                 , stateActiveNonPersistent
                                                                 , stateActivePersistent
                                                                 , statePrePreparedNonPersistent
                                                                 , statePrePreparedNonPersistent
                                                                 , statePrePreparedPersistent
                                                                 , stateError
                                                                 , stateError
                                                                 , stateError
                                                                 , stateError
                                                                 , stateError
                                                                 , stateError
                                                                 , stateError
                                                                 , stateError
                                                                 , stateError
                                                                 , stateError
    };
    // What happens when a persistent object becomes involved with the transaction.
    static final int nextStateForInvolvePersistentObject[] = { stateError
                                                              , stateActivePersistent
                                                              , stateActivePersistent
                                                              , stateActivePersistent
                                                              , statePrePreparedPersistent
                                                              , statePrePreparedPersistent
                                                              , statePrePreparedPersistent
                                                              , stateError
                                                              , stateError
                                                              , stateError
                                                              , stateError
                                                              , stateError
                                                              , stateError
                                                              , stateError
                                                              , stateError
                                                              , stateError
                                                              , stateError
    };
    // What happens when a persistent object becomes involved with the transaction
    // when we recover from a checkpoint log record.
    static final int nextStateForInvolvePersistentObjectFromCheckpoint[] = { stateError
                                                                            , stateActivePersistent
                                                                            , stateError // activeNonPersitent cannot be recovered.
                                                                            , stateActivePersistent
                                                                            , stateError // Checkpoint is locked out while prePrepared.
                                                                            , stateError
                                                                            , stateError // Checkpoint is locked out while prePrepared.
                                                                            , statePreparedPersistent
                                                                            , stateError
                                                                            , statePreparedPersistent
                                                                            , stateError // Checkpoint is locked out while commiting.
                                                                            , stateError
                                                                            , stateError
                                                                            , stateError // Checkpoint is locked out while backing out.
                                                                            , stateError
                                                                            , stateError
                                                                            , stateError
    };
    // What happens when a nonpersistent object becomes optimistically involved with the transaction.
    static final int nextStateForInvolveOptimisticNonPersistentObject[] = { stateError
                                                                           , stateActiveNonPersistent
                                                                           , stateActiveNonPersistent
                                                                           , stateActivePersistent
                                                                           , statePrePreparedNonPersistent
                                                                           , statePrePreparedNonPersistent
                                                                           , statePrePreparedPersistent
                                                                           , statePreparedNonPersistent
                                                                           , statePreparedNonPersistent
                                                                           , statePreparedPersistent
                                                                           , stateCommitingNonPersistent
                                                                           , stateCommitingNonPersistent
                                                                           , stateCommitingPersistent
                                                                           , stateBackingOutNonPersistent
                                                                           , stateBackingOutNonPersistent
                                                                           , stateBackingOutPersistent
                                                                           , stateError
    };
    // What happens when a persistent object becomes optimistically involved with the transaction.
    static final int nextStateForInvolveOptimisticPersistentObject[] = { stateError
                                                                        , stateActivePersistent
                                                                        , stateActivePersistent
                                                                        , stateActivePersistent
                                                                        , statePrePreparedPersistent
                                                                        , statePrePreparedPersistent
                                                                        , statePrePreparedPersistent
                                                                        , statePreparedPersistent // In case we do this again during recovery.
                                                                        , statePreparedPersistent // In case we do this again during recovery.
                                                                        , statePreparedPersistent
                                                                        , stateError
                                                                        , stateError
                                                                        , stateCommitingPersistent
                                                                        , stateError
                                                                        , stateError
                                                                        , stateBackingOutPersistent
                                                                        , stateError
    };

    // What happens when request for a callback is made.
    static final int nextStateForRequestCallback[] = { stateError
                                                      , stateError
                                                      , stateActiveNonPersistent
                                                      , stateActivePersistent
                                                      , stateError
                                                      , stateError
                                                      , stateError
                                                      , stateError
                                                      , statePreparedNonPersistent // restoring a prepared from a checkpoint
                                                      , statePreparedPersistent // restoring a prepared from a checkpoint
                                                      , stateError
                                                      , stateError
                                                      , stateError
                                                      , stateError
                                                      , stateError
                                                      , stateError
                                                      , stateError
    };
    // What happens when the transaction is prePrepared.
    static final int nextStateForPrePrepare[] = { stateError
                                                 , statePrePreparedInactive
                                                 , statePrePreparedNonPersistent
                                                 , statePrePreparedPersistent
                                                 , stateError
                                                 , stateError
                                                 , statePrePreparedPersistent // In case we do this again during recovery.
                                                 , stateError
                                                 , stateError
                                                 , stateError
                                                 , stateError
                                                 , stateError
                                                 , stateError
                                                 , stateError
                                                 , stateError
                                                 , stateError
                                                 , stateError
    };
    // What happens when the transaction is prepared.
    static final int nextStateForPrepare[] = { stateError
                                              , statePreparedInactive
                                              , statePreparedNonPersistent
                                              , statePreparedPersistent
                                              , statePreparedInactive
                                              , statePreparedNonPersistent
                                              , statePreparedPersistent
                                              , statePreparedInactive
                                              , statePreparedNonPersistent
                                              , statePreparedPersistent
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
    };
    //What happens when the transaction starts to commit.
    static final int nextStateForStartCommit[] = { stateError
                                                  , stateError
                                                  , stateError
                                                  , stateCommitingPersistent // In case we do this again during recovery.
                                                  , stateCommitingInactive
                                                  , stateCommitingNonPersistent
                                                  , stateCommitingPersistent
                                                  , stateCommitingInactive
                                                  , stateCommitingNonPersistent
                                                  , stateCommitingPersistent
                                                  , stateError
                                                  , stateError
                                                  , stateCommitingPersistent // In case we do this again during recovery.
                                                  , stateError
                                                  , stateError
                                                  , stateError
                                                  , stateError
    };
    // What happens when the transaction is commited.
    static final int nextStateForCommit[] = { stateError
                                             , stateError
                                             , stateError
                                             , stateError
                                             , stateError
                                             , stateError
                                             , stateError
                                             , stateError
                                             , stateError
                                             , stateError
                                             , stateInactive
                                             , stateInactive
                                             , stateInactive
                                             , stateError
                                             , stateError
                                             , stateError
                                             , stateError
    };
    // What happens when the transaction starts to back out.
    // This is the same set of transitions as for commit.
    static final int nextStateForStartBackout[] = { stateError
                                                   , stateError
                                                   , stateError
                                                   , stateBackingOutPersistent // In case we do this again during recovery.
                                                   , stateBackingOutInactive
                                                   , stateBackingOutNonPersistent
                                                   , stateBackingOutPersistent
                                                   , stateBackingOutInactive
                                                   , stateBackingOutNonPersistent
                                                   , stateBackingOutPersistent
                                                   , stateError
                                                   , stateError
                                                   , stateError
                                                   , stateError
                                                   , stateError
                                                   , stateBackingOutPersistent // In case we do this again during recovery.
                                                   , stateError
    };
    // What happens when the transaction is backed out.
    static final int nextStateForBackout[] = { stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateInactive
                                              , stateInactive
                                              , stateInactive
                                              , stateError
    };
    // What happens when the transaction is terminated.
    static final int nextStateForTerminate[] = { stateError
                                                , stateTerminated
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
                                                , stateError
    };
    // What happens when the transaction is terminated.
    static final int nextStateForShutdown[] = { stateError
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
                                               , stateTerminated
    };

    int state; // The current state of the transaction.
    // The previous state, not refereneced but will appear in a dump.
    private int previousState;

    /*---------------------- Define the state machine (end) ------------------------*/

    // The ObjectManagerState that is going to manage this transaction.
    // Protected so that linkedList and TreeMap can see what state the ObjectManagerState is in.
    protected ObjectManagerState objectManagerState;

    // Within this ObjectManager uniquely identify this unit of work for syncpoint control.
    private LogicalUnitOfWork logicalUnitOfWork;
    // Bumped each time the transaction completes, used for displaying in trace for debugging only.
    private int useCount = 0;
    protected long completeTick = 0;
    protected long completeTid = 0;
    protected String completeName = "";
    protected long freeTick = 0;
    protected long freeTid = 0;
    protected String freeName = "";
    protected long registeredTick = 0;
    protected long registeredTid = 0;
    protected String registeredName = "";
    protected long finaliseTick = 0;

    // The lock that any ManagedObjects use to indicate their locked state.
    private TransactionLock transactionLock;

    // The following Sets and Maps elimiate duplicate entries for ManagedObject, by
    // referencing the Token. This relies on Token.hashCode() being unique within the JVM
    // hence Token is declred final so this assumption cannot be invalidated.
    // ----------------------------------------------------------------------------------

    // ManagedObjects that are protected by this unit of work. Note that we hold a reference to
    // the ManagedObject rather than the Token so as to keep it resident in storage while the
    // Transaction is active. This map is keyed off the Token.
    // Note that we cannot simly make a set of ManagedObjects because ManagedObject could override
    // hashCode() and make it non unique. Map,Entry does this for example.
    private java.util.Map includedManagedObjects = new java.util.HashMap();
    // Tokens that require to be called back at prepare, commit and backout time.
    // This set relies on Token.hashCode() being unique within the JVM.
    private java.util.Set callbackTokens = new java.util.HashSet();
    // Tokens that require to be notified of completed optimistic replace operations after
    // the OptimisticReplace has been logged. This is the set we keep in case we are required
    // to reWrite it during a checkpoint.
    private java.util.Set allPersistentTokensToNotify = new java.util.HashSet();
    // The following Maps elimiate duplicate entries for ManagedObject, by
    // mapping the Token.
    // Map of ObjectManagerByteArrayOutputStreams containing serialized Bytes last
    // written to the log for each included Token.
    private java.util.Map loggedSerializedBytes = new java.util.HashMap();
    // Map of logSequenceNumbers for each ManagedObject when it was last logged as part
    // this transaction, mapped by Token.
    private java.util.Map logSequenceNumbers = new java.util.HashMap();
    // Map of ManagedObject.sequenceNumber when the ManagedObject last saved as
    // part of this transaction. keyed by the Token.
    private java.util.Map managedObjectSequenceNumbers = new java.util.HashMap();

    // Number of bytes reserved in the log for writing transaction outcome records.
    // Rounded up to whole nubers of pages to allow for the possibility of PaddingLogRecords
    // being added to each one.
    // The partHeaderLength is reserved for each logRecord by LogOutput itself but we reserve some extra
    // space here ahead of LogOutput actually needing it.
    // No need to account made for sector bytes as these are added into the FileLogOutput overhead.
    private static long logCommitOverhead = ((Math.max(TransactionCommitLogRecord.maximumSerializedSize(), TransactionBackoutLogRecord.maximumSerializedSize())
                                             + FileLogOutput.partHeaderLength
                                             )
                                             / FileLogOutput.pageSize + 1
                                            ) * FileLogOutput.pageSize;
    // We don't know how long the chekpoint record will be so add one extra page as a worst case assumption.
    private static long logCheckpointOverhead = ((TransactionCheckpointLogRecord.maximumSerializedSize()
                                                 + FileLogOutput.partHeaderLength)
                                                 / FileLogOutput.pageSize + 1)
                                                * FileLogOutput.pageSize;
    private static long logSpaceReservedOverhead = logCommitOverhead + logCheckpointOverhead;

    private long logSpaceReserved = 0;
    // A byte array output stream used by logrecords to construct their log buffer.
    // Each log record is constructed and written serially by this transaction so they
    // are free to use this without further synchronisation.
    protected ObjectManagerByteArrayOutputStream logRecordByteArrayOutputStream = new ObjectManagerByteArrayOutputStream(1024);
    // Marked as requiring checkpoint at the start of checkpoint processing.
    protected boolean requiresPersistentCheckpoint = false;
    // Hold a weak reference to the external transaction, if it exists.
    protected TransactionReference transactionReference;

    /**
     * Constructor.
     * Register the InternalTransaction with the ObjectManager.
     * 
     * @param objectManagerState of the ObjectManager which will own the InternalTransaction.
     * @param logicalUnitOfWork that identifies the transaction.
     * @throws ObjectManagerException
     */
    protected InternalTransaction(ObjectManagerState objectManagerState,
                                  LogicalUnitOfWork logicalUnitOfWork)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { objectManagerState,
                                      logicalUnitOfWork });

        this.objectManagerState = objectManagerState;
        this.logicalUnitOfWork = logicalUnitOfWork;
        transactionLock = new TransactionLock(this);
        state = stateInactive; // Initial state.
        previousState = -1; // No previous state.
        objectManagerState.registerTransaction(this);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // InternalTransaction().

    /**
     * Create the extrnal transaction for use by the public interface
     * from this InternalTransaction.
     * 
     * To detect orphaned Transactions, ie. Transactions not referenced elsewhere in the JVM,
     * we will keep a reference to the internalTransaction and make a weak reference to the
     * container we pass to the application. When the container Transaction appears on the
     * orphanTransactionsQueue we know the calling application has lost all references to it.
     * At this point we can attempt to reuse it.
     * 
     * @return transaction the new transaction.
     */
    protected synchronized final Transaction getExternalTransaction()
    {
        final String methodName = "getExternalTransaction";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);
        Transaction transaction = null; // For return.
        if (transactionReference != null)
            transaction = (Transaction) transactionReference.get();

        if (transaction == null) {
            transaction = new Transaction(this);
            // Make a WeakReference that becomes Enqueued as a result of the external Transaction becoming unreferenced.
            transactionReference = new TransactionReference(this,
                                                            transaction);
        } // if (transaction == null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { transaction });
        return transaction;
    } // getExternalTransaction().

    /**
     * <p>
     * Returns the state of the transaction.
     * </p>
     * 
     * @return int the state.
     */
    protected int getState() {
        return state;
    } // getState();

    /**
     * @return LogicalUnitOfWork.
     */
    protected LogicalUnitOfWork getLogicalUnitOfWork()
    {
        return logicalUnitOfWork;
    } // getLogicalUnitOfWork().

    /**
     * <p>
     * Returns the XID identifying the transaction or null if has not been set.
     * 
     * @return byte[] The Xopen identifier of the transaction, or null.
     *         </p>
     */
    protected byte[] getXID()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this, cclass
                        , "getXID"
                            );
            trace.exit(this, cclass
                       , "getXID"
                       , "returns logicalUnitOfWork.XID=" + logicalUnitOfWork.XID + "(byte[])"
                            );
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
        return logicalUnitOfWork.XID;
    } // End of getXID method.

    /**
     * <p>
     * Sets the XID identifying the transaction if has not been set already.
     * No check is made on the uniqueness of the XID.
     * 
     * @param XID the Xopen identifier of the transaction, or null.
     *            </p>
     * @throws ObjectManagerException
     */
    protected synchronized void setXID(byte[] XID)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "setXID",
                        XID);
            trace.bytes(this,
                        cclass,
                        XID);
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).

        if (logicalUnitOfWork.XID != null) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "setXID"
                           , "via XIDModificationException"
                             + " logicalUnitOfWork.XID=" + logicalUnitOfWork.XID + "(byte[])"
                                );
            throw new XIDModificationException(this
                                               , logicalUnitOfWork.XID
                                               , XID);
        } // if (logicalUnitOfWork.XID != null).

        if (XID.length > LogicalUnitOfWork.maximumXIDSize) {
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this, cclass
                            , "setXID"
                            , "via XIDTooLongException"
                              + " XID.length=" + XID.length + "(byte[])"
                                );
            throw new XIDTooLongException(this
                                          , XID.length);
        } // if (logicalUnitOfWork.XID != null).

        // No need to reserve extra space in the log for the XID in Commit/Backout
        // and Checkpoint logRecords because we already assumed the maximumSerializedSize
        // when the transaction wrote its fitrst LogRecord.

        logicalUnitOfWork.XID = new byte[XID.length];
        System.arraycopy(XID, 0, logicalUnitOfWork.XID, 0, XID.length);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "setXID"
                            );
    } // End of setXID method.

    /**
     * @return TransactionLock for this transaction.
     */
    protected TransactionLock getTransactionLock()
    {
        return transactionLock;
    }

    /**
     * Note that we return Collection rather than Set although all of the managedObjects
     * are unique some of them (for example TreeMap.Entry) may satisfy equals().
     * 
     * @return java.util.Collection the includedManagedObjects.
     */
    protected java.util.Collection getIncludedManagedObjects()
    {
        return new java.util.ArrayList(includedManagedObjects.values());
    }

    /**
     * Note a request for the Token in the transaction to be called at prePrepare,
     * preCommit, preBackout, postCommit and postBackout.
     * 
     * @param token who's ManagedObject is to be called back.
     * @param transaction the external transaction.
     * @throws ObjectManagerException
     * @throws InvalidStateException if the tansaction has started its prepare processing.
     */
    protected synchronized void requestCallback(Token token,
                                                Transaction transaction)
                    throws ObjectManagerException
    {
        final String methodName = "requestCallback";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { token,
                                      transaction });

        // To defend against two application threads completing the same transaction and trying to
        // continue with it at the same time we check that the Transaction still refers to this one,
        // now that we are synchronized on the InternalTransaction.
        if (transaction.internalTransaction != this) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { "via InvalidTransactionException",
                                         transaction.internalTransaction });
            // Same behaviour as if the transaction was completed and replaced by
            // objectManagerState.dummyInternalTransaction.
            throw new InvalidStateException(this,
                                            InternalTransaction.stateTerminated,
                                            InternalTransaction.stateNames[InternalTransaction.stateTerminated]);

        } // if (transaction.internalTransaction != this)
          // Make the state change, to see if we can accept requestCallBack requests.
          // We only accept the request if we will make a callback.
        setState(nextStateForRequestCallback);
        // The object is now listed for prePrepare etc. callback.
        callbackTokens.add(token);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // requestCallback().

    /**
     * <p>
     * Lock a ManagedObject for use by this transaction.
     * It is necessary to lock an object if you don't want
     * it used by another transaction while it is included in this one.
     * When an object is locked we create its before image so that the default backout
     * action can then restore it if a backout occurs.
     * You must lock a ManagedObject before replacing it so that the default rollback
     * action is enabled. Use optimisticReplace if you dont want to take a lock, but
     * if you do this, you have to compensate for any actions yourself if the
     * transaction is rolled back.
     * 
     * Add and delete operations also take this kind of lock automatically, as a precaution
     * to prevent other transactions attempting another Add, Delete or Replace operation.
     * 
     * @param managedObject to be locked.
     *            </p>
     * @throws ObjectManagerException
     */
    protected void lock(ManagedObject managedObject)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "lock"
                        , "managedObject=" + managedObject + "(ManagedObject)");

        // TODO Should take transaction as an argument and check it like add(),replace() etc do.

        // Drive the lock method for the ManagedObject, without holding the synchronized lock
        // on the InternalTransaction. Do this because the lock might block in wait(),
        // stalling this transaction and hence blocking the checkpoint thread.
        managedObject.lock(transactionLock);

        synchronized (this) {
            // Now include the ManagedObject in the transaction.
            includedManagedObjects.put(managedObject.owningToken,
                                       managedObject);
            // Set the logSequenceNumber to keep commit(), backout() and checkpoint() happy.
            // Any subsequent update will superceed the zero log sequence number.
            if (!logSequenceNumbers.containsKey(managedObject.owningToken)) {
                logSequenceNumbers.put(managedObject.owningToken,
                                       new Long(0));
                managedObjectSequenceNumbers.put(managedObject.owningToken,
                                                 new Long(0));
            } // if (!logSequenceNumbers.containsKey)...
        } // synchronized (this).

        // Now check that the transactionLock is still held because a bad application might
        // have another thread that just commited the transaction , or the ObjectManager might have
        // backed it out bacause it was too busy. If the lock is no longer good then notify
        // the ManagedObject in case any other threads are now waiting for the lock.
        // This strategy avoids holding a lock on ManagedObject and Transaction at the same time.
        if (!transactionLock.isLocked()) {
            synchronized (managedObject) {
                managedObject.notify();
            } // synchronized (managedObject).
        } // if( !transactionLock.isLocked()).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "lock");
    } // lock().

    /**
     * Add an after image of an object invloved in the transaction.
     * Objects added are protected by this unit of work. After any updates
     * are made these are written to the log if the managedObject is persistent
     * and later written to the Objectstore.
     * 
     * @param managedObject to be added under the scope of the Transaction.
     * @param transaction the external Transaction.
     * @param logSpaceReservedDelta extra log space the caller wants reserved until the transaction completes.
     * @throws ObjectManagerException
     */
    protected void add(ManagedObject managedObject,
                       Transaction transaction,
                       long logSpaceReservedDelta)
                    throws ObjectManagerException {
        final String methodName = "add";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { managedObject,
                                                                transaction,
                                                                new Long(logSpaceReservedDelta) });

        // We should not call ManagedObject add/delete/replace etc methods with the transaction lock held
        // because some of these call transaction.lock() which might wait(), thus stalling the checkpoint
        // as it tries to get the transaction lock.
        // Make the ManagedObject ready for an addition,
        // give it a chance to blow up in anything is wrong. If it turns out that this is the wrong transaction 
        // we will call postAdd after we have taken the lock on this transaction.
        managedObject.preAdd(transaction);

        // Changes may be made to these objects after this point, however they will not be restored,
        // we record the object at this point in time.
        // We have to allow for some other transaction to modify the object between the end of this transaction and
        // when we actually write it to the ObjectStore, using optimisticReplace.
        // We address this by keeping a copy of the serialised immage of the object when we create the
        // log record along with the logSequenceNumber when we write it.
        // A higher level lock, such as synchronizing on LinkedList must protect against our capturing the wrong
        // serialized version of the ManagedObject.
        // We do not add the ManagedObject to the ObjectStore now because the ObjectStore might force it to disk
        // before we force the log. This would leave us with no way of recovering if we crashed in that state.
        // We get the serializedBytes before taking the lock on the transaction bacause any stall in the 
        // ObjectStore.reserve() call would block the checkpoint.
        ObjectManagerByteArrayOutputStream serializedBytes = null;
        try {
            if (managedObject.owningToken.getObjectStore().getUsesSerializedForm())
                serializedBytes = managedObject.getSerializedBytes();
        } catch (ObjectManagerException exception) {
            // No FFDC Code Needed.
            // Drive the postAdd method for the object.
            managedObject.postAdd(transaction, false);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { exception });
            throw exception;
        } // try...

        synchronized (this) {
            // To defend against two application threads completing the same transaction and trying to
            // continue with it at the same time we check that the Transaction still refers to this one,
            // now that we are synchronized on the InternalTransaction.
            if (transaction.internalTransaction != this) {
                managedObject.postAdd(transaction, false);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Object[] { "via InvalidTransactionException",
                                                                       transaction.internalTransaction });
                // Same behaviour as if the transaction was completed and replaced by
                // objectManagerState.dummyInternalTransaction.
                throw new InvalidStateException(this,
                                                InternalTransaction.stateTerminated,
                                                InternalTransaction.stateNames[InternalTransaction.stateTerminated]);

            } // if (transaction.internalTransaction != this).

            // Is data logging required for this object in order too recover it?
            // Non persistent objects that are within the scope of the transaction do not
            // require logging. This is because we reconstruct their state from other
            // objects at restart or because their state is restablished at restart in some other way,
            // for example, we might delete them on restart.
            // We write the log record now but dont actually write to the ObjectStore until
            // the transaction has commited. The actual ObjectStore write to disk happens even
            // later when the object store is flushed.
            long logSequenceNumber;

            // This try block catches any fatal error that causes us to fail to write the transaction
            // log or reserve space in the Objectstores.
            try {
                if (managedObject.owningToken.getObjectStore().getPersistence()) {
                    testState(nextStateForInvolvePersistentObject);

                    logSpaceReservedDelta = logSpaceReservedDelta + Token.maximumSerializedSize();
                    if (logSpaceReserved == 0)
                        logSpaceReservedDelta = logSpaceReservedDelta + logSpaceReservedOverhead;

                    TransactionAddLogRecord transactionAddLogRecord = new TransactionAddLogRecord(this,
                                                                                                  managedObject.owningToken,
                                                                                                  serializedBytes);

                    // If we throw an exception in here no state change has been done.
                    logSequenceNumber = objectManagerState.logOutput.writeNext(transactionAddLogRecord, logSpaceReservedDelta,
                                                                               true, false);
                    setState(nextStateForInvolvePersistentObject); // Make the state change.

                    logSpaceReserved = logSpaceReserved + logSpaceReservedDelta;
                    // Remember what we logged in case we commit this version of the ManagedObject.
                    loggedSerializedBytes.put(managedObject.owningToken, serializedBytes);

                } else { // if (tokenToAdd.getObjectstore().getPersistence()).
                    setState(nextStateForInvolveNonPersistentObject); // Make the state change.
                    logSequenceNumber = objectManagerState.getDummyLogSequenceNumber();

                    // Do we need to capture the serialized form of the ManagedObject for the ObjectStore?
                    if (managedObject.owningToken.getObjectStore().getUsesSerializedForm()) {
                        // Remember what we would have logged in case we commit this version of the ManagedObject.
                        loggedSerializedBytes.put(managedObject.owningToken,
                                                  serializedBytes);
                    } // if (managedObject.owningToken.getObjectStore().getUsesSerializedForm()).
                } // If Non Persistent.
            } catch (ObjectManagerException exception) {
                // The write was not done.
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:860:1.41");

                // Drive the postadd method for the object.
                managedObject.postAdd(transaction, false);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Object[] { exception });
                throw exception;
            } // try...

            // The ManagedObject is now included in the transaction.
            includedManagedObjects.put(managedObject.owningToken, managedObject);
            // Remember which logSequenceNumber was used when we logged the ManagedObject.
            logSequenceNumbers.put(managedObject.owningToken,
                                   new Long(logSequenceNumber));
            managedObjectSequenceNumbers.put(managedObject.owningToken,
                                             new Long(managedObject.getUpdateSequence()));
        } // synchronized (this).

        // Drive the postAdd method for the object.
        managedObject.postAdd(transaction, true);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // add().

    /**
     * Recover an Add operation from a checkpoint.
     * 
     * @param managedObject being added.
     * @param transaction the external Transaction.
     * @throws ObjectManagerException
     */
    protected synchronized void addFromCheckpoint(ManagedObject managedObject,
                                                  Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "addFromCheckpoint";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { managedObject,
                                                                transaction });

        // Make the ManagedObject ready for an addition.
        // Nothing went wrong during forward processing!
        managedObject.preAdd(transaction);

        // Only persistent objects are recovered from a checkpoint.
        setState(nextStateForInvolvePersistentObjectFromCheckpoint);
        // The object is now included in the transaction.
        includedManagedObjects.put(managedObject.owningToken, managedObject);

        // The ManagedObject was read from the ObjectStore, give it a low log
        // and ManagedObject sequence number so that any later operation will supercede this one.
        // The loggedSerialisedBytes will be unchanged, possibly null for this Token.
        // If there already a logSequenceNumber known for this token it must have been put
        // there after the checkpoint start and has already been superceded.
        if (!logSequenceNumbers.containsKey(managedObject.owningToken)) {
            logSequenceNumbers.put(managedObject.owningToken,
                                   new Long(0));
            managedObjectSequenceNumbers.put(managedObject.owningToken,
                                             new Long(0));
        } // if (!logSequenceNumbers.containsKey(managedObject.owningToken)).

        // Redrive the postAdd method for the object.
        managedObject.postAdd(transaction, true);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // addFromCheckpoint().

    /**
     * <p>
     * Add an after image of an object invloved in the transaction. The Object must be Locked
     * before it can be replaced and the lock is held until after the transaction commits
     * of backs out.
     * </p>
     * <p>
     * This operation has the same end result as delete followed by add except that
     * the after image is stored only in memory until after the transaction commits
     * so that the before immage in the object store is not damaged in the case of
     * a backout being required. This costs more storage. It is also more difficult
     * for the object store that stores the object to make efficient use of its
     * storage if the replacement is a different size to the original. The advantage
     * of using replace is that it retains the original object store identifier, so
     * references to this need not be changed.
     * </p>
     * 
     * @param managedObject to be replaced.
     * @param transaction the external Transaction.
     * @param logSpaceReservedDelta extra log space the caller wants reserved until the transaction completes.
     * @throws ObjectManagerException
     */
    protected void replace(ManagedObject managedObject,
                           Transaction transaction,
                           long logSpaceReservedDelta)
                    throws ObjectManagerException {
        final String methodName = "replace";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { managedObject,
                                                                transaction,
                                                                new Long(logSpaceReservedDelta) });

        // Make the ManagedObject ready for a replace operation.
        // give it a chance to blow up in anything is wrong.
        managedObject.preReplace(transaction);

        // Changes may be made to these objects up until the point when the transaction starts 
        // its prepare processing, however they will not be restored, we record the object at
        // this point in time. When we write the log record we capture the serialised form of 
        // the object and use the same serialized form to write to the objects store. Further
        // changes to the object will have no effect on what is written. We have to allow for
        // some other transaction to modify the object between the end of this transaction and
        // when we actually write it to the ObjectStore. We address this by keeping a copy of
        // the serialised immage of the object when we create the log record along with the
        // logSequenceNumber when we write it. A higher level lock, such as synchronizing on
        // LinkedList must protect against our capturing the wrong serialized version of the
        // ManagedObject.
        ObjectManagerByteArrayOutputStream serializedBytes = null;
        try {
            if (managedObject.owningToken.getObjectStore().getUsesSerializedForm())
                serializedBytes = managedObject.getSerializedBytes();
        } catch (ObjectManagerException exception) {
            // No FFDC Code Needed.
            // Drive the postAdd method for the object.
            managedObject.postReplace(transaction, false);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { exception });
            throw exception;
        } // try...

        synchronized (this) {
            // To defend against two application threads completing the same transaction and trying to
            // continue with it at the same time we check that the Transaction still refers to this one,
            // now that we are synchronized on the InternalTransaction.
            if (transaction.internalTransaction != this) {
                managedObject.postReplace(transaction, false);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { "via InvalidTransactionException",
                                             transaction.internalTransaction });
                // Same behaviour as if the transaction was completed and replaced by
                // objectManagerState.dummyInternalTransaction.
                throw new InvalidStateException(this,
                                                InternalTransaction.stateTerminated,
                                                InternalTransaction.stateNames[InternalTransaction.stateTerminated]);

            } // if (transaction.internalTransaction != this).

            // To replace an object it must first be locked to prevent other transactions from
            // attempting updates.
            // Is data logging required for this object in order to recover it?
            // Non persistent objects that are within the scope of the transaction but do not
            // require logging. This is because we reconstruct their state from other
            // objects at restart or because their state is restablished at restart in some other way.
            // For instance we might delete them at restart.
            long logSequenceNumber;

            // This try block catches any fatal error that causes us to fail to write the transaction
            // log or reserve space in the Objectstores.    
            try {
                if (managedObject.owningToken.getObjectStore().getPersistence()) {
                    testState(nextStateForInvolvePersistentObject);

                    // Over estimate of reserved space. If we replase a ManagedObject previously involved in this
                    // Transaction we will have already reserved some space.
                    logSpaceReservedDelta = logSpaceReservedDelta + Token.maximumSerializedSize() + serializedBytes.getCount();
                    if (logSpaceReserved == 0)
                        logSpaceReservedDelta = logSpaceReservedDelta + logSpaceReservedOverhead;

                    TransactionReplaceLogRecord transactionReplaceLogRecord = new TransactionReplaceLogRecord(this,
                                                                                                              managedObject.owningToken,
                                                                                                              serializedBytes
                                    );
                    // If we throw an exception in here only the state change has been done.
                    logSequenceNumber = objectManagerState.logOutput.writeNext(transactionReplaceLogRecord, logSpaceReservedDelta,
                                                                               true, false);

                    // The previous testState() call means we should not fail in here.
                    setState(nextStateForInvolvePersistentObject); // Make the state change.

                    logSpaceReserved = logSpaceReserved + logSpaceReservedDelta;
                    // Remember what we logged in case we commit this version of the ManagedObject.
                    loggedSerializedBytes.put(managedObject.owningToken,
                                              serializedBytes);

                } else { // if (tokenToReplace.getObjectStore().persistent).
                    setState(nextStateForInvolveNonPersistentObject);
                    logSequenceNumber = objectManagerState.getDummyLogSequenceNumber();

                    // Do we need to capture the serialized form of the ManagedObject for the ObjectStore?
                    if (managedObject.owningToken.getObjectStore().getUsesSerializedForm()) {
                        loggedSerializedBytes.put(managedObject.owningToken,
                                                  serializedBytes);
                    } // if (managedObject.owningToken.getObjectStore().getUsesSerializedForm()).
                } // If Non Persistent.
            } catch (ObjectManagerException exception) {
                // The write was not done.
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:1060:1.41");

                // Drive the postReplace method for the object.
                managedObject.postReplace(transaction, false);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Object[] { exception });
                throw exception;
            } // catch (ObjectManagerException...

            // The object is now included in the transaction.
            includedManagedObjects.put(managedObject.owningToken, managedObject);
            // Remember which version it was when we logged it.
            logSequenceNumbers.put(managedObject.owningToken,
                                   new Long(logSequenceNumber));
            managedObjectSequenceNumbers.put(managedObject.owningToken,
                                             new Long(managedObject.getUpdateSequence()));
        } // synchronized (this).

        // Drive the postReplace method for the ManagedObject, now that logging has been done.
        managedObject.postReplace(transaction, true);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // replace().

    /**
     * Recover a Replace operation from a checkpoint.
     * 
     * @param managedObject recovered from a checkpoint.
     * @param serializedBytes representing the serialized form of the ManagedObject originally logged.
     * @param transaction the external Transaction.
     * @throws ObjectManagerException
     */
    protected synchronized void replaceFromCheckpoint(ManagedObject managedObject,
                                                      byte[] serializedBytes,
                                                      Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "replaceFromCheckpoint";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { managedObject,
                                                                new Integer(serializedBytes.length),
                                                                transaction });

        // Make the ManagedObject ready for a replace operation.
        // Nothing went wrong during forward processing!
        managedObject.preReplace(transaction);

        // Only persistent objects are recovered from a checkpoint.
        setState(nextStateForInvolvePersistentObjectFromCheckpoint);
        // The object is now included in the transaction.
        includedManagedObjects.put(managedObject.owningToken, managedObject);

        // The ManagedObject was read from the log, give it a low log
        // and ManagedObject sequence number so that any later operation will supercede this one.
        // The loggedSerialisedBytes will be unchanged, possibly null for this Token.
        // If there already a logSequenceNumber known for this token it must have been put
        // there after the checkpoint start and has already been superceded.
        if (!logSequenceNumbers.containsKey(managedObject.owningToken)) {
            logSequenceNumbers.put(managedObject.owningToken,
                                   new Long(0));
            managedObjectSequenceNumbers.put(managedObject.owningToken,
                                             new Long(0));
            // Remember what we originally logged in case we commit this version of the ManagedObject.
            // Replacements are not written to the ObjectStore for a checkpoint becauise that would
            // remove any before image which we would need if the object backed out.
            loggedSerializedBytes.put(managedObject.owningToken
                                      , serializedBytes
                            );
        } // if (!logSequenceNumbers.containsKey(token)).

        // Redrive the postReplace method for the object.
        managedObject.postReplace(transaction, true);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // replaceFromCheckpoint().

    /**
     * <p>
     * Perform a number of additions ,optimistic replace updates and deletes of
     * ManagedOjects but writes and single log record with a list of objects to replace.
     * Optimistic replace does not required the ManagedObject to be in locked state,
     * it can be in Added,Locked,Replaced,Deleted,Ready states and be used by another
     * transaction However, it is up to the code updating the object to correctly
     * reverse the changes at the time any backout of this transaction occurs. Because
     * the transaction is not locked no before immage of the object is kept. Unlike
     * standard replace we do not need to hold the update out of the ObjectStore
     * until commit time because the updating code is responsible for reversing the
     * effects of the update. These compensating changes can be made after the transaction has
     * prepared.
     * </p>
     * <p>
     * The advantage of this method is that all of the updated objects are handled
     * together, so that they all make it into the log for restart. If they were added
     * to the log separately we might fail and see some of them at restart and not others
     * This allows objects such as lists to make several associated changes without
     * having to worry about compensating for the intermediate states.
     * </p>
     * <p>
     * If any exception occurs in preAdd,preOptimisticReplace or preDelete then the following
     * preAdd,preOptimisticReplace or preDelete callbacks are not made nor are any
     * postAdd,postOptimisticReplace or postDelete or optimisticReplaceLogged callbacks.
     * </p>
     * 
     * @param managedObjectsToAdd list of ManagedObjects to add,
     *            or null if there are none.
     * @param managedObjectsToReplace list of ManagedObjects to be optimistically replaced.
     *            or null if there are none.
     * @param managedObjectsToDelete list of ManagedObjects to delete,
     *            or null if there are none.
     * @param tokensToNotify list of tokens to be notified once the replace is logged,
     *            or null if there are none.
     * @param transaction the external Transaction.
     * @param logSpaceReservedDelta extra log space the caller wants reserved until the transaction completes.
     * @throws ObjectManagerException
     */
    protected void optimisticReplace(java.util.List managedObjectsToAdd,
                                     java.util.List managedObjectsToReplace,
                                     java.util.List managedObjectsToDelete,
                                     java.util.List tokensToNotify,
                                     Transaction transaction,
                                     long logSpaceReservedDelta)
                    throws ObjectManagerException {
        final String methodName = "optimisticReplace";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { managedObjectsToAdd,
                                                                managedObjectsToReplace,
                                                                managedObjectsToDelete,
                                                                tokensToNotify,
                                                                transaction,
                                                                new Long(logSpaceReservedDelta) });

        // Keep a subset list of Tokens for persistent ManagedObjects to add.
        java.util.List persistentTokensToAdd = new java.util.ArrayList();
        // Keep a subset list of Tokens for persistent ManagedObjects to optimistically replace.
        java.util.List persistentTokensToReplace = new java.util.ArrayList();
        // Also update the list of the serializedBytes.
        java.util.List persistentSerializedBytes = new java.util.ArrayList();
        // A local Map of serializedBytes, so that we can add it to the set known to be logged,
        // once we have written the transactionOptimisticReplaceLogRecord.
        java.util.Map newLoggedSerializedBytes = new java.util.HashMap();
        // Keep a subset list of persistent tokens to delete.
        java.util.List persistentTokensToDelete = new java.util.ArrayList();
        // Keep a subset list of persistent tokens to notify.
        java.util.List persistentTokensToNotify = new java.util.ArrayList();
        long logSequenceNumber; // Sequence number of the log record written.

        // This try block catches any fatal error that causes us to fail to write the transaction log or reserve space
        // in the Objectstores or make the preAdd/Replace/OptimisticReplace/Delete calls to the ManagedObjects.
        try {
            if (managedObjectsToAdd != null) {
                for (java.util.Iterator managedObjectIterator = managedObjectsToAdd.iterator(); managedObjectIterator.hasNext();) {
                    ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();

                    // Make the ManagedObjects ready for an optimisticReplace,
                    // give it a chance to blow up in anything is wrong.
                    managedObject.preAdd(transaction);

                    // Build the subset of persistent objects.
                    if (managedObject.owningToken.getObjectStore().getPersistence()) {
                        persistentTokensToAdd.add(managedObject.owningToken);
                        // We have to allow for some other transaction to modify the object between the end of this transaction and
                        // when we actually write it to the ObjectStore.
                        // We address this by keeping a copy of the serialised immage of the object when we create the
                        // log record along with the logSequenceNumber when we write it.
                        // A higher level lock, such as synchronizing on LinkedList must protect against our capturing the wrong
                        // serialized version of the ManagedObject.
                        ObjectManagerByteArrayOutputStream serializedBytes = managedObject.getSerializedBytes();
                        persistentSerializedBytes.add(serializedBytes);
                        // Remember what we loggged in case we commit this version of the ManagedObject.
                        newLoggedSerializedBytes.put(managedObject.owningToken,
                                                     serializedBytes);

                    } else if (managedObject.owningToken.getObjectStore().getUsesSerializedForm()) {
                        // We need to capture the serialized form of the ManagedObject for the ObjectStore?
                        ObjectManagerByteArrayOutputStream serializedBytes = managedObject.getSerializedBytes();
                        newLoggedSerializedBytes.put(managedObject.owningToken,
                                                     serializedBytes);
                    } // if ... persistent.
                } // for ... managedObjectsToAdd.
            } // if (managedObjectsToAdd != null).

            if (managedObjectsToReplace != null) {
                for (java.util.Iterator managedObjectIterator = managedObjectsToReplace.iterator(); managedObjectIterator.hasNext();) {
                    ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();

                    // Make the ManagedObjects ready for an optimisticReplace,
                    // give it a chance to blow up in anything is wrong.
                    managedObject.preOptimisticReplace(transaction);

                    // Build the subset of persistent objects.
                    if (managedObject.owningToken.getObjectStore().getPersistence()) {
                        persistentTokensToReplace.add(managedObject.owningToken);
                        // We have to allow for some other transaction to modify the object between the end of this transaction and
                        // when we actually write it to the ObjectStore.
                        // We address this by keeping a copy of the serialised immage of the object when we create the
                        // log record along with the logSequenceNumber when we write it.
                        // A higher level lock, such as synchronizing on LinkedList must protect against our capturing the wrong
                        // serialized version of the ManagedObject.
                        ObjectManagerByteArrayOutputStream serializedBytes = managedObject.getSerializedBytes();
                        persistentSerializedBytes.add(serializedBytes);
                        // Remember what we loggged in case we commit this version of the ManagedObject.
                        newLoggedSerializedBytes.put(managedObject.owningToken,
                                                     serializedBytes);

                    } else if (managedObject.owningToken.getObjectStore().getUsesSerializedForm()) {
                        // We need to capture the serialized form of the ManagedObject for the ObjectStore?
                        ObjectManagerByteArrayOutputStream serializedBytes = managedObject.getSerializedBytes();
                        newLoggedSerializedBytes.put(managedObject.owningToken,
                                                     serializedBytes);
                    } // if ... persistent.
                } // for ... managedObjectsToReplace.
            } // if (managedObjectsToReplace != null).

            if (managedObjectsToDelete != null) {
                for (java.util.Iterator managedObjectIterator = managedObjectsToDelete.iterator(); managedObjectIterator.hasNext();) {
                    ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();

                    // Make the ManagedObject ready for an deletion,
                    // give it a chance to blow up in anything is wrong.
                    managedObject.preDelete(transaction);
                    // Build the subset of persistent objects.
                    if (managedObject.owningToken.getObjectStore().getPersistence()) {
                        persistentTokensToDelete.add(managedObject.owningToken);
                    } // if ... persistent.
                } // for ... managedObjectsToDelete.
            } // if (managedObjectsToDelete != null).

            if (tokensToNotify != null) {
                for (java.util.Iterator tokenIterator = tokensToNotify.iterator(); tokenIterator.hasNext();) {
                    Token token = (Token) tokenIterator.next();
                    // Build the subset of persistent objects.
                    if (token.getObjectStore().getPersistence()) {
                        persistentTokensToNotify.add(token);
                    } // if (token.getObjectStore().persistent).
                } // for ... tokenToNotify.
            } // if (tokensToNotify != null).
        } catch (ObjectManagerException exception) {
            // The write was not done.
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:1305:1.41");

            postOptmisticReplace(managedObjectsToAdd, managedObjectsToReplace, managedObjectsToDelete, transaction);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { exception });
            throw exception;
        } // catch (ObjectManagerException... 

        synchronized (this) {
            // To defend against two application threads completing the same transaction and trying to
            // continue with it at the same time we check that the Transaction still refers to this one,
            // now that we are synchronized on the InternalTransaction.
            if (transaction.internalTransaction != this) {
                postOptmisticReplace(managedObjectsToAdd, managedObjectsToReplace, managedObjectsToDelete, transaction);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               methodName,
                               new Object[] { "via InvalidTransactionException", transaction.internalTransaction }
                                    );
                // Same behaviour as if the transaction was completed and replaced by
                // objectManagerState.dummyInternalTransaction.
                throw new InvalidStateException(this,
                                                InternalTransaction.stateTerminated,
                                                InternalTransaction.stateNames[InternalTransaction.stateTerminated]);

            } // if (transaction.internalTransaction != this).
            try {
                // Is data logging required for this object in order to recover it?
                if (!persistentTokensToAdd.isEmpty()
                    || !persistentTokensToReplace.isEmpty()
                    || !persistentTokensToDelete.isEmpty()
                    || !persistentTokensToNotify.isEmpty()) {
                    testState(nextStateForInvolveOptimisticPersistentObject);

                    // Over estimate of reserved space. If we replace a ManagedObject previously involved in this
                    // Transaction we will have already reserved some space.
                    logSpaceReservedDelta = logSpaceReservedDelta
                                            + (+persistentTokensToAdd.size() + persistentTokensToReplace.size() + persistentTokensToDelete.size() + persistentTokensToNotify.size())
                                            * Token.maximumSerializedSize();
                    if (logSpaceReserved == 0)
                        logSpaceReservedDelta = logSpaceReservedDelta + logSpaceReservedOverhead;
                    TransactionOptimisticReplaceLogRecord transactionOptimisticReplaceLogRecord = new TransactionOptimisticReplaceLogRecord(this,
                                                                                                                                            persistentTokensToAdd,
                                                                                                                                            persistentTokensToReplace,
                                                                                                                                            persistentSerializedBytes,
                                                                                                                                            persistentTokensToDelete,
                                                                                                                                            persistentTokensToNotify);
                    // If we throw an exception in here no state change has been done.
                    logSequenceNumber = objectManagerState.logOutput.writeNext(transactionOptimisticReplaceLogRecord,
                                                                               logSpaceReservedDelta, true, false);

                    // The previous testState() call means we should not fail in here.
                    setState(nextStateForInvolveOptimisticPersistentObject); // Make the state change.

                    logSpaceReserved = logSpaceReserved + logSpaceReservedDelta;

                } else { // No persistent tokens mentioned then.
                    testState(nextStateForInvolveOptimisticNonPersistentObject);
                    setState(nextStateForInvolveOptimisticNonPersistentObject);
                    logSequenceNumber = objectManagerState.getDummyLogSequenceNumber();
                } // else non Persistent.

            } catch (ObjectManagerException exception) {
                // The write was not done.
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "optimisticReplace", exception, "1:1371:1.41");

                postOptmisticReplace(managedObjectsToAdd, managedObjectsToReplace, managedObjectsToDelete, transaction);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Object[] { exception });
                throw exception;
            } // catch (ObjectManagerException... 

            // Any logging must be done before the following operations on the ManagedObjects as we will
            // redrive these after recovery from the log on a restart.

            // We have now sucessfully logged the serialized bytes.
            loggedSerializedBytes.putAll(newLoggedSerializedBytes);

            // Drive the postAdd method for the ManagedObjects.
            if (managedObjectsToAdd != null) {
                for (java.util.Iterator managedObjectIterator = managedObjectsToAdd.iterator(); managedObjectIterator.hasNext();) {
                    ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
                    // The ManagedObject is now included in the transaction.
                    includedManagedObjects.put(managedObject.owningToken,
                                               managedObject);
                    // Remember which logSequenceNumber when we last logged it.
                    logSequenceNumbers.put(managedObject.owningToken,
                                           new Long(logSequenceNumber));
                    managedObjectSequenceNumbers.put(managedObject.owningToken,
                                                     new Long(managedObject.getUpdateSequence()));
                    managedObject.postAdd(transaction,
                                          true);
                } // for ... managedObjectsToAdd.
            } // if (managedObjectsToAdd != null).

            if (managedObjectsToReplace != null) {
                for (java.util.Iterator managedObjectIterator = managedObjectsToReplace.iterator(); managedObjectIterator.hasNext();) {
                    ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
                    // The ManagedObject is now included in the transaction.
                    includedManagedObjects.put(managedObject.owningToken,
                                               managedObject);
                    // Remember which logSequenceNumber when we last logged it.
                    logSequenceNumbers.put(managedObject.owningToken,
                                           new Long(logSequenceNumber));
                    managedObjectSequenceNumbers.put(managedObject.owningToken,
                                                     new Long(managedObject.getUpdateSequence()));
                    // See comment in ManagedObject.optimisticReplaceCommit().
                    // In principle the managed object here could
                    // also have been deleted by a subsequent transaction. But so far I have not seen this happen.
                    managedObject.postOptimisticReplace(transaction,
                                                        true);
                } // for ... managedObjectsToReplace.
            } // if (managedObjectsToReplace != null).

            // Drive the postDelete method for the ManagedObjects.
            if (managedObjectsToDelete != null) {
                for (java.util.Iterator managedObjectIterator = managedObjectsToDelete.iterator(); managedObjectIterator.hasNext();) {
                    ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
                    // The ManagedObject is now included in the transaction.
                    includedManagedObjects.put(managedObject.owningToken,
                                               managedObject);
                    // Remember which logSequenceNumber when we last logged it.
                    logSequenceNumbers.put(managedObject.owningToken,
                                           new Long(logSequenceNumber));
                    managedObjectSequenceNumbers.put(managedObject.owningToken,
                                                     new Long(managedObject.getUpdateSequence()));
                    managedObject.postDelete(transaction,
                                             true);
                } // for ... managedObjectsToDelete.
            } // if (managedObjectsToDelete != null).

            // Drive the optimisticReplaceLogged method for any ManagedObjects to Notify.
            if (tokensToNotify != null) {
                for (java.util.Iterator tokenIterator = tokensToNotify.iterator(); tokenIterator.hasNext();) {
                    Token token = (Token) tokenIterator.next();
                    if (token.getObjectStore()
                                    .getPersistence())
                        allPersistentTokensToNotify.add(token);
                    token.getManagedObject()
                                    .optimisticReplaceLogged(transaction);
                } // for ... tokensToNotify.
            } // if (tokensToReplace != null).
        } // synchronized (this).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // optimisticReplace().

    private final void postOptmisticReplace(java.util.List managedObjectsToAdd,
                                            java.util.List managedObjectsToReplace,
                                            java.util.List managedObjectsToDelete,
                                            Transaction transaction)
                    throws ObjectManagerException {

        // Drive the postOptimisticReplace method for the ManagedObjects to add.
        if (managedObjectsToAdd != null) {
            for (java.util.Iterator managedObjectIterator = managedObjectsToAdd.iterator(); managedObjectIterator.hasNext();) {
                ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
                managedObject.postAdd(transaction, false);
            } // for ... managedObjectsToAdd.
        } // if (managedObjectsToAdd != null).

        // Drive the postOptimisticReplace method for the ManagedObjects to optimistic replace.
        if (managedObjectsToReplace != null) {
            for (java.util.Iterator managedObjectIterator = managedObjectsToReplace.iterator(); managedObjectIterator.hasNext();) {
                ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
                managedObject.postOptimisticReplace(transaction, false);
            } // for ... managedObjectsToReplace.
        } // if (managedObjectsToReplace != null).

        // Drive the postOptimisticReplace method for the ManagedObjects to delete.
        if (managedObjectsToDelete != null) {
            for (java.util.Iterator managedObjectIterator = managedObjectsToDelete.iterator(); managedObjectIterator.hasNext();) {
                ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
                managedObject.postDelete(transaction, false);
            } // for ... managedObjectsToDelete.
        } // if (managedObjectsToDelete != null).
    } // postOptimisticReplace().

    /**
     * <p>
     * Reinstate the state of an ManagedObject to be optimistaclly replaced after reading
     * a TransactionCheckpointLogRecord.
     * </p>
     * 
     * @param managedObject to be optimistically replaced.
     * @param transaction the external Transaction.
     * @throws ObjectManagerException
     */
    protected synchronized void optimisticReplaceFromCheckpoint(ManagedObject managedObject,
                                                                Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "optimisticReplaceFromCheckpoint";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { managedObject,
                                                                transaction });

        // Make the ManagedObject ready for an optimisticReplace operation.
        // Nothing went wrong during forward processing!
        managedObject.preOptimisticReplace(transaction);

        // Only persistent objects are recovered from a checkpoint in the log!
        setState(nextStateForInvolveOptimisticPersistentObject);
        // The objects are now included in the transaction.
        includedManagedObjects.put(managedObject.owningToken, managedObject);

        // The ManagedObject was previously forced to the the ObjectStore, give it a low log
        // and managedObject sequence number so that any later operation will supercede this one.
        // The loggedSerialisedBytes will be unchanged, possibly null for this Token.
        // If there already a logSequenceNumber known for this token it must have been put
        // there after the checkpoint start and has already been superceded.
        if (!logSequenceNumbers.containsKey(managedObject.owningToken)) {
            logSequenceNumbers.put(managedObject.owningToken,
                                   new Long(0));
            managedObjectSequenceNumbers.put(managedObject.owningToken,
                                             new Long(0));
        }

        // Redrive the postObtimisticReplace method for the ManagedObject.
        managedObject.postOptimisticReplace(transaction, true);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // optimisticReplaceFromCheckpoint().

    /**
     * <p>
     * Reinstate the notification of a ManagedObject after recovery from a checkpoint.
     * </p>
     * 
     * @param token to be notified.
     * @param transaction the external Transaction.
     * @throws ObjectManagerException
     */
    protected synchronized void notifyFromCheckpoint(Token token,
                                                     Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "notifyFromCheckpoint";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { token,
                                                                transaction });

        allPersistentTokensToNotify.add(token);
        token.getManagedObject().optimisticReplaceLogged(transaction);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // notifyFromCheckpoint().

    /**
     * <p>
     * These ManagedObjects cease to exist after the transaction commits
     * or are reinstated if it backs out.
     * When the unit of work commits, these objects are deleted from their
     * object store.
     * </p>
     * 
     * @param managedObject to be deletd.
     * @param transaction the external Transaction
     * @param logSpaceReservedDelta the extra log space the caller wants reserved until the transaction completes.
     * @throws ObjectManagerException
     */
    protected void delete(ManagedObject managedObject,
                          Transaction transaction,
                          long logSpaceReservedDelta)
                    throws ObjectManagerException {
        final String methodName = "delete";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { managedObject,
                                      transaction,
                                      new Long(logSpaceReservedDelta) });

        // Make the ManagedObject ready for an deletion,
        // give it a chance to blow up in anything is wrong.
        managedObject.preDelete(transaction);

        synchronized (this) {
            // To defend against two application threads completing the same transaction and trying to
            // continue with it at the same time we check that the Transaction still refers to this one,
            // now that we are synchronized on the InternalTransaction.
            if (transaction.internalTransaction != this) {
                managedObject.postDelete(transaction, false);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { "via InvalidTransactionException",
                                             transaction.internalTransaction });
                // Same behaviour as if the transaction was completed and replaced by
                // objectManagerState.dummyInternalTransaction.
                throw new InvalidStateException(this,
                                                InternalTransaction.stateTerminated,
                                                InternalTransaction.stateNames[InternalTransaction.stateTerminated]);

            } // if (transaction.internalTransaction != this).

            // This object will be removed from the object store once the transaction
            // is commted.

            // Is data logging is required for this object in order to recover its
            // removal?
            long logSequenceNumber;

            // The following try block catces failures to write the log record.
            try {
                if (managedObject.owningToken.getObjectStore().getPersistence()) {
                    testState(nextStateForInvolvePersistentObject);

                    logSpaceReservedDelta = logSpaceReservedDelta + Token.maximumSerializedSize();
                    if (logSpaceReserved == 0)
                        logSpaceReservedDelta = logSpaceReservedDelta + logSpaceReservedOverhead;

                    TransactionDeleteLogRecord transactionDeleteLogRecord = new TransactionDeleteLogRecord(this,
                                                                                                           managedObject.owningToken);

                    // If we throw an exception in here no state change has been done.
                    logSequenceNumber = objectManagerState.logOutput.writeNext(transactionDeleteLogRecord, logSpaceReservedDelta,
                                                                               true, false);

                    // The previous testState() call means we should not fail in here.
                    setState(nextStateForInvolvePersistentObject);

                    logSpaceReserved = logSpaceReserved + logSpaceReservedDelta;

                } else { // if (token.getObjectStore().persistent).
                    setState(nextStateForInvolveNonPersistentObject);
                    logSequenceNumber = objectManagerState.getDummyLogSequenceNumber();
                } // If Non Persistent.
            } catch (ObjectManagerException exception) {
                // The write was not done.
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:1644:1.41");

                // Drive the postDelete method for the object.
                managedObject.postDelete(transaction, false);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { exception });
                throw exception;
            } //  catch (ObjectManagerException...

            loggedSerializedBytes.remove(managedObject.owningToken);

            // The ManagedObject is now included in the transaction.
            includedManagedObjects.put(managedObject.owningToken, managedObject);
            // Remember which version it was when we logged it.
            logSequenceNumbers.put(managedObject.owningToken,
                                   new Long(logSequenceNumber));
            managedObjectSequenceNumbers.put(managedObject.owningToken,
                                             new Long(managedObject.getUpdateSequence()));
        } // synchronized (this);

        // Drive the postDelete method for the object.
        managedObject.postDelete(transaction, true);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // delete().

    /**
     * <p>
     * Recreate a delete operation from a checkpoint read from the transaction log.
     * </p>
     * 
     * @param managedObject to be deleted.
     * @param transaction the external Transaction.
     * @throws ObjectManagerException
     */
    protected synchronized void deleteFromCheckpoint(ManagedObject managedObject,
                                                     Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "deleteFromCheckpoint";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { managedObject,
                                                                transaction });

        // Make the ManagedObject ready for an deletion,
        // Nothing went wrong during forward processing!
        managedObject.preDelete(transaction);

        // Only persistent ManagedObjects are recovered after a checkpoint.
        setState(nextStateForInvolvePersistentObjectFromCheckpoint);
        // The ManagedObject is now included in the transaction.
        includedManagedObjects.put(managedObject.owningToken, managedObject);

        // The ManagedObject was previously forced to the the ObjectStore, give it a low log
        // and managedObject sequence number so that any later operation will supercede this one.
        // The loggedSerialisedBytes will be unchanged, possibly null for this Token.
        // If there already a logSequenceNumber known for this token it must have been put
        // there after the checkpoint start by the delete method.
        if (!logSequenceNumbers.containsKey(managedObject.owningToken)) {
            logSequenceNumbers.put(managedObject.owningToken,
                                   new Long(0));
            managedObjectSequenceNumbers.put(managedObject.owningToken,
                                             new Long(0));
        } // if (!logSequenceNumbers.containsKey(managedObject.owningToken)).
          // Redrive the postDelete method for the ManagedObject.
        managedObject.postDelete(transaction, true);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // deleteFromCheckpoint().

    /**
     * <p> Reset the state of a transaction. Used During recovery processing of
     * TransactionOptimisticReplaceLogRecord to indicate that a transaction must commit or must backout.
     * Also use by TransactionCheckpointLogRecord to reinstate the transaction state.
     * 
     * @param recoveredState the transaction state that was reciorded in the logRecord.
     *            </p>
     */
    protected void resetState(int recoveredState)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "resetState"
                        , "recoveredState=" + recoveredState + "(int) " + stateNames[recoveredState] + "(String)"
                            );
        switch (recoveredState) {

            case (stateActivePersistent):
                setState(nextStateForInvolvePersistentObject);
                break;

            case (statePrePreparedPersistent):
                setState(nextStateForPrePrepare);
                break;

            case (statePreparedPersistent):
                setState(nextStateForPrepare);
                break;

            case (stateCommitingPersistent):
                setState(nextStateForStartCommit);
                break;

            case (stateBackingOutPersistent):
                setState(nextStateForStartBackout);
                break;

            default:

        } // switch (recoveredState)

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "resetState"
                            );
    } // resetState().

    /**
     * Prepare the transaction.
     * After execution of this method the
     * users of this transaction shall not perform any further work as part
     * of the logical unit of work, or modify any of the objects that are
     * referenced by the transaction.
     * 
     * We have already spooled the add and delete log records for persistent objects.
     * Since we are performing a two phase commit we force a prepare record.
     * The Objects already have space reserved in the object store.
     * 
     * @param transaction the external Transaction.
     */
    protected synchronized void prepare(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "prepare"
                        , "transaction=" + transaction + "(Trasnaction)"
                            );

        // To defend against two application threads completing the same transaction and trying to
        // continue with it at the same time we check that the Transaction still refers to this one,
        // now that we are synchronized on the InternalTransaction.
        if (transaction.internalTransaction != this) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "prepare",
                           new Object[] { "via InvalidTransactionException", transaction.internalTransaction }
                                );
            // Same behaviour as if the transaction was completed and replaced by
            // objectManagerState.dummyInternalTransaction.
            throw new InvalidStateException(this,
                                            InternalTransaction.stateTerminated,
                                            InternalTransaction.stateNames[InternalTransaction.stateTerminated]);

        } // if (transaction.internalTransaction != this).

        prePrepare(transaction); // Give ManagedObjects a chance to get ready.

        // Is there any logging to do?
        if (state == statePrePreparedPersistent) { // Logging work to do.

            TransactionPrepareLogRecord transactionPrepareLogRecord = new TransactionPrepareLogRecord(this);
            objectManagerState.logOutput.writeNext(transactionPrepareLogRecord
                                                   , 0
                                                   , true
                                                   , true);
        } // If logging work to do.

        // ManagedObjects do nothing at prepare time.
//    // Drive prepare method of objects included in this transaction.
//    for (java.util.Iterator managedObjectIterator = includedManagedObjects.iterator();
//         managedObjectIterator.hasNext();
//         ) {
//      ManagedObject managedObject = (ManagedObject)managedObjectIterator.next();
//      managedObject.prepare(transaction);
//    }                                            // for... includedManagedObjects.

        setState(nextStateForPrepare);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "prepare"
                            );
    } // prepare().

    /**
     * Before the transaction is prepared, give the objects a final chance to
     * complete their processing and lock them all.
     * 
     * @param transaction the external Transaction.
     */
    void prePrepare(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "prePrepare"
                        , new Object[] { transaction }
                            );

        // Make the state change, to show that we cannot accept more prePrepare requests.
        setState(nextStateForPrePrepare);

        // Allow any last minute changes before we prepare.
        for (java.util.Iterator tokenIterator = callbackTokens.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            ManagedObject managedObject = token.getManagedObject();

            // Drive the prePrepare method for the object.
            managedObject.prePrepare(transaction);
        } // for... callbackTokens.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "prePrepare"
                            );
    } // prePrepare().

    /**
     * Commit the transaction.
     * Fore write a commit record to the log.
     * Unlock the objects that are part of this logical unit of work.
     * 
     * @param reUse true indicates that the transaction is terminated and then reused
     *            with another logical unit of work.
     * @param transaction the external Transaction.
     * @throws ObjectManagerException
     */
    protected void commit(boolean reUse,
                          Transaction transaction)
                    throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "commit"
                        , new Object[] { new Boolean(reUse), transaction }
                            );

        boolean persistentWorkDone = false;
        ManagedObject[] lockedManagedObjects;
        int numberOfLockedManagedObjects = 0;

        synchronized (this) {
            // To defend against two application threads completing the same transaction and trying to
            // continue with it at the same time we check that the Transaction still refers to this one,
            // now that we are synchronized on the InternalTransaction.
            if (transaction.internalTransaction != this) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "commit",
                               new Object[] { "via InvalidTransactionException", transaction.internalTransaction }
                                    );
                // Same behaviour as if the transaction was completed and replaced by
                // objectManagerState.dummyInternalTransaction.
                throw new InvalidStateException(this,
                                                InternalTransaction.stateTerminated,
                                                InternalTransaction.stateNames[InternalTransaction.stateTerminated]);

            } // if (transaction.internalTransaction != this).

            if (state == stateInactive
                || state == stateActiveNonPersistent
                || state == stateActivePersistent) {
                // Only call prePrepare if we have not already prepared the transaction.
                prePrepare(transaction);
            } // If already prepared.

            testState(nextStateForStartCommit);
            setState(nextStateForStartCommit);

            preCommit(transaction); // Tell ManagedObjects the outcome.

            // Is there any logging to do? We only need to write log records if the
            // transaction involves persistent objects.
            if (state == stateCommitingPersistent) {
                persistentWorkDone = true;

                TransactionCommitLogRecord transactionCommitLogRecord = new TransactionCommitLogRecord(this);
                objectManagerState.logOutput.writeNext(transactionCommitLogRecord,
                                                       -logSpaceReserved,
                                                       true,
                                                       true);
                logSpaceReserved = 0;

            } // If logging work to do.

            // Drive the commit method for the included objects.
            // The synchronized block prevents us from taking a checkpoint until all of the
            // ManagedObjects have had their opportunity to update the ObjecStore. If a
            // checkpoint is currently active we will update the current checkpoint set in
            // the ObjecStore, if not we will update the next set of updates.
            lockedManagedObjects = new ManagedObject[includedManagedObjects.size()];

            boolean requiresCurrentPersistentCheckpoint = requiresPersistentCheckpoint
                                                          || (objectManagerState.checkpointStarting == ObjectManagerState.CHECKPOINT_STARTING_PERSISTENT);

            for (java.util.Iterator managedObjectIterator = includedManagedObjects.values()
                            .iterator(); managedObjectIterator.hasNext();) {
                ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
                // The logged serializedBytes will be null if the ManagedObject was deleted by this transaction
                // or if it was added from a transactionCheckpointLogRecord at restart, because the
                // Object Store will already have copy of this ManagedObject.
                ObjectManagerByteArrayOutputStream serializedBytes = (ObjectManagerByteArrayOutputStream) loggedSerializedBytes.get(managedObject.owningToken);
                long managedObjectSequenceNumber = ((Long) managedObjectSequenceNumbers.get(managedObject.owningToken)).longValue();

                // If the Object was not locked by this transaction it must have been an optimistic update.
                if (managedObject.lockedBy(transaction)) {
                    managedObject.commit(transaction,
                                         serializedBytes,
                                         managedObjectSequenceNumber,
                                         requiresCurrentPersistentCheckpoint);
                    lockedManagedObjects[numberOfLockedManagedObjects++] = managedObject;
                } else {
                    managedObject.optimisticReplaceCommit(transaction,
                                                          serializedBytes,
                                                          managedObjectSequenceNumber,
                                                          requiresCurrentPersistentCheckpoint);
                }
            } // for... includedManagedObjects.

            setState(nextStateForCommit);
            transactionLock.unLock(objectManagerState);
            postCommit(transaction); // Tell ManagedObjects the outcome is complete.
            // Tidy up the transaction.
            complete(reUse,
                     transaction);
        } // synchronized (this).

        // We don't want to clear the transaction lock held by the managedObject otherwise
        // ManagedObject.wasLocked() will not be able to give the past locked state. The
        // Unlock point wa noted above so now notify the ManagedObject and give a new
        // waiter a chance to acquire the lock. If a new transaction acquires the lock then
        // ManagedObject.wasLocked will return its result for the new transaction and
        // wasLocked() will then be true for an even later time.  Do this after we have release the
        // synchronize lock on InternalTransaction so that we avoid deadlock with ManagedObjects
        // that invoke synchronized InternalTransaction methods.

        for (int i = 0; i < numberOfLockedManagedObjects; i++) {
            synchronized (lockedManagedObjects[i]) {
                lockedManagedObjects[i].notify();
            } // synchronized (lockedManagedObjects[i]).
        } // for... lockedManagedObjects.

        // Tell the ObjectManager that we are done, once the transaction is unlocked
        // in case it is needed for checkpoint.
        objectManagerState.transactionCompleted(this, persistentWorkDone);
        // See if we need to delay while a checkpoint completes. Applications amy ask to reUSe
        // the same transaction, if so we introduce the delay here. Internal transactions are never
        // reUsed so we don't need to wory about blocking them. This call must be made when we
        // are not synchronized on the transaction because it might block waiting for a checkpoint
        // to complete if the log is full.
        if (reUse)
            objectManagerState.transactionPacing();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "commit"
                            );
    } // commit().

    /**
     * Before the transaction is commited, give the ManagedObjects a chance to
     * adjust their transient state to reflect the final outcome.
     * It is too late for them to adjust persistent state as that is already written to the log
     * assuming an outcome of Commit.
     * 
     * @param transaction the external Transaction.
     */
    void preCommit(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "preCommit"
                        , "transaction=" + transaction + "(Transaction)"
                            );

        // Allow any last minute changes before we commit.
        for (java.util.Iterator tokenIterator = callbackTokens.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            ManagedObject managedObject = token.getManagedObject();

            // Drive the preCommit method for the object.
            managedObject.preCommit(transaction);
        } // for... callbackTokens.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "preCommit"
                            );
    } // End of preCommit() method.

    /**
     * After the transaction is commited, give the ManagedObjects a final chance to
     * adjust their transient state to reflect the final outcome.
     * It is too late for them to adjust persistent state as that is already written to the log
     * assuming an outcome of Commit.
     * 
     * @param transaction the external Transaction.
     */
    void postCommit(Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "postCommit";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, transaction);

        // Allow any last minute changes now that we have commited.
        for (java.util.Iterator tokenIterator = callbackTokens.iterator(); tokenIterator.hasNext();)
        {
            Token token = (Token) tokenIterator.next();
            ManagedObject managedObject = token.getManagedObject();

            // Drive the postCommit method for the object.
            managedObject.postCommit(transaction);
        } // for... callbackTokens.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // postCommit().

    /**
     * Rollback the transacion. Force a backout record to the log.
     * 
     * @param reUse Indicates whether the transaction is terminated or can be reused for another logical unit of work.
     * @param transaction the external Transaction.
     * @throws ObjectManagerException
     */
    protected void backout(boolean reUse,
                           Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "backout";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { new Boolean(reUse),
                                                                transaction });

        boolean persistentWorkDone = false;
        ManagedObject[] lockedManagedObjects;
        int numberOfLockedManagedObjects = 0;

        synchronized (this) {
            // To defend against two application threads completing the same transaction and trying to
            // continue with it at the same time we check that the Transaction still refers to this one,
            // now that we are synchronized on the InternalTransaction.
            if (transaction.internalTransaction != this) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               methodName,
                               new Object[] { "via InvalidTransactionException", transaction.internalTransaction }
                                    );
                // Same behaviour as if the transaction was completed and replaced by
                // objectManagerState.dummyInternalTransaction.
                throw new InvalidStateException(this,
                                                InternalTransaction.stateTerminated,
                                                InternalTransaction.stateNames[InternalTransaction.stateTerminated]);

            } // if (transaction.internalTransaction != this).

            // Only call prePrepare if we have not already prepared the transaction.
            if (state == stateInactive
                || state == stateActiveNonPersistent
                || state == stateActivePersistent) {
                prePrepare(transaction);
            } // If already prepared.

            testState(nextStateForStartBackout);
            setState(nextStateForStartBackout);

            preBackout(transaction); // Tell ManagedObjects the outcome.

            // Is there any logging to do?
            if (state == stateBackingOutPersistent) {
                persistentWorkDone = true;

                TransactionBackoutLogRecord transactionBackoutLogRecord = new TransactionBackoutLogRecord(this);
                objectManagerState.logOutput.writeNext(transactionBackoutLogRecord,
                                                       -logSpaceReserved,
                                                       true,
                                                       true);
                logSpaceReserved = 0;
            } // If logging work to do.

            // Drive the backout method for the included objects.
            // The synchronized block prevents us from taking a checkpoint until all of the
            // ManagedObjects have had their opportunity to update the ObjectStore. If a
            // checkpoint is currently active we will update the current checkpoint set in
            // the ObjectStore, if not we will update the next set of updates.
            lockedManagedObjects = new ManagedObject[includedManagedObjects.size()];

            boolean requiresCurrentPersistentCheckpoint = requiresPersistentCheckpoint
                                                          || (objectManagerState.checkpointStarting == ObjectManagerState.CHECKPOINT_STARTING_PERSISTENT);

            for (java.util.Iterator managedObjectIterator = includedManagedObjects.values().iterator(); managedObjectIterator.hasNext();) {
                ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
                long managedObjectSequenceNumber = ((Long) managedObjectSequenceNumbers.get(managedObject.owningToken)).longValue();

                if (managedObject.lockedBy(transaction)) {
                    managedObject.backout(transaction,
                                          managedObjectSequenceNumber,
                                          requiresCurrentPersistentCheckpoint);
                    lockedManagedObjects[numberOfLockedManagedObjects++] = managedObject;
                } else {
                    ObjectManagerByteArrayOutputStream serializedBytes = (ObjectManagerByteArrayOutputStream) loggedSerializedBytes.get(managedObject.owningToken);
                    managedObject.optimisticReplaceBackout(transaction,
                                                           serializedBytes,
                                                           managedObjectSequenceNumber,
                                                           requiresCurrentPersistentCheckpoint);
                } // if(managedObject.lockedBy(transaction)).

            } // for... includedManagedObjects.

            setState(nextStateForBackout);
            transactionLock.unLock(objectManagerState);
            postBackout(transaction); // Tell ManagedObjects the outcome is complete.
            // Tidy up the transaction.
            complete(reUse,
                     transaction);
        } // synchronized (this).

        // We don't want to clear the transaction lock held by the managedObject otherwise
        // ManagedObject.wasLocked() will not be able to give the past locked state. The
        // Unlock point wa noted above so now notify the ManagedObject and give a new
        // waiter a chance to acquire the lock. If a new transaction acquires the lock then
        // ManagedObject.wasLocked will return its result for the new transaction and
        // wasLocked() will then be true for an even later time.  Do this after we have release the
        // synchronize lock on InternalTransaction so that we avoid deadlock with ManagedObjects
        // that invoke synchronized InternalTransaction methods.
        for (int i = 0; i < numberOfLockedManagedObjects; i++) {
            synchronized (lockedManagedObjects[i]) {
                lockedManagedObjects[i].notify();
            } // synchronized (lockedManagedObjects[i]).
        } // for... lockedManagedObjects.

        // Tell the ObjectManager that we are done, once the transaction is unlocked
        // in case it is needed for checkpoint.
        objectManagerState.transactionCompleted(this,
                                                persistentWorkDone);
        // See if we need to delay while a checkpoint completes. Applications amy ask to reUSe
        // the same transaction, if so we introduce the delay here. Internal transactions are never
        // reUsed so we don't need to wory about blocking them. This call must be made when we
        // are not synchronized on the transaction because it might block waiting for a checkpoint
        // to complete if the log is full.
        if (reUse)
            objectManagerState.transactionPacing();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // backout().

    /**
     * Before the transaction is backed out, give the ManagedObjects a chance to adjust their transient state to reflect
     * the final outcome. It is too late for them to adjust persistent state as that is already written to the log
     * assuming an outcome of Commit.
     * 
     * @param transaction the external Transaction.
     * @throws ObjectManagerException
     */
    void preBackout(Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "preBackout";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { transaction });

        // Allow any last minute changes before we backout.
        for (java.util.Iterator tokenIterator = callbackTokens.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            ManagedObject managedObject = token.getManagedObject();

            // Drive the preBackout method for the object.
            managedObject.preBackout(transaction);
        } // for... callbackTokens.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // preBackout().

    /**
     * After the transaction is backed out, give the ManagedObjects a final chance to adjust their transient state to
     * reflect the final outcome. It is too late for them to adjust persistent state as that is already written to the log
     * assuming an outcome of Commit.
     * 
     * @param transaction the external Transaction.
     * @throws ObjectManagerException
     */
    void postBackout(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "postBackout"
                          + "transaction=" + transaction + "(Transaction)"
                            );

        // Allow any last minute changes now that we have backed out.
        for (java.util.Iterator tokenIterator = callbackTokens.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            ManagedObject managedObject = token.getManagedObject();

            // Drive the postBackout method for the object.
            managedObject.postBackout(transaction);
        } // for... callbackTokens.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "postBackout");
    } // postBackout().

    /**
     * Tidy up after Commit or Backout have finished all work for this InternalTransaction.
     * 
     * @param reUse true if the trandsaction is to be reused.
     * @param transaction the external Transaction completing.
     * @throws ObjectManagerException
     */
    private synchronized final void complete(boolean reUse,
                                Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "complete";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { new Boolean(reUse),
                                                                transaction });

        // No longer any need to include this transaction in a Checkpoint.
        requiresPersistentCheckpoint = false;
        // Clear any remaining state.
        logicalUnitOfWork.XID = null;
        transactionLock = new TransactionLock(this);
        includedManagedObjects.clear();
        callbackTokens.clear();
        allPersistentTokensToNotify.clear();
        loggedSerializedBytes.clear();
        logSequenceNumbers.clear();
        managedObjectSequenceNumbers.clear();
        useCount++;
        completeTick = System.currentTimeMillis();      // for orphan diagnostics
        completeTid = Thread.currentThread().getId();
        completeName = Thread.currentThread().getName();

        if (reUse) { // Reset the transaction for further use.
            // Note that we do not clear the transactionReference because the caller is still holding it.
            // If the caller releases his reference then the reference will be found by ObjectManagerState
            // and this InternalTransaction may be reused for another external Transaction.

        } else { // Do not chain.
            if (transactionReference != null) transactionReference.clear(); // Inhibt enqueuing of the transactionReference.
            transactionReference = null; // This will prevent processing (and formation of a new link) if already enqueued.
            // Make sure the external Transaction cannot reach this internal one.
            // This is done after clearing the weak reference to prevent loss of this strong reference before it is cleared.
            transaction.internalTransaction = objectManagerState.dummyInternalTransaction;

            // Tell the ObjectManager that we no longer exist as an active transaction.
            objectManagerState.deRegisterTransaction(this);

        } // if (reUse).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // complete().

    /**
     * Permanently disable use of this transaction. reason the Transaction.terininatedXXX reason
     * 
     * @throws ObjectManagerException
     */
    protected synchronized void terminate(int reason)
                    throws ObjectManagerException {
        final String methodName = "terminate";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { new Integer(reason) });

        if (transactionReference != null) {
            Transaction transaction = (Transaction) transactionReference.get();
            /**
             * PM00131 - transaction is coming from a WeakReference, check it isn't null.
             * If we've lost the reference, there isn't any point making a new one
             * just to set a reason code on it. Nobody will have a reference to the
             * new object with which to retrieve the code.
             **/
            if (transaction != null)
                transaction.setTerminationReason(reason);
        }
        setState(nextStateForTerminate);
        // Any attempt by any therad to do anything with this Transaction
        // from now on will result in a StateErrorException.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // terminate().

    /**
     * Run at shutdown of the ObjectManager to close activity.
     * 
     * @throws ObjectManagerException
     */
    protected synchronized void shutdown()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "shutDown"
                            );

        setState(nextStateForShutdown);
        // Any attempt by any therad to do anything with this Transaction
        // from now on will result in a InvalidStateException.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "shutdown"
                            );
    } // shutdown().

    /**
     * Mark this InternalTransaction as requiring a Checkpoint in the current checkpoint cycle.
     */
    protected final void setRequiresCheckpoint()
    {
        final String methodName = "setRequiresCheckpoint";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { new Boolean(requiresPersistentCheckpoint) });

        // The states for which a checkpoint log Record must be written to the log unless this transactions ends first.
        // Has any logging been done? If The transaction enters one of thse states after this
        // call then all of its state will be in the log after CheckpointStart.
        final boolean checkpointRequired[] = { false
                                              , false
                                              , false
                                              , true // ActivePersistent.
                                              , false
                                              , false
                                              , true // PrePreparedPersistent.
                                              , false
                                              , false
                                              , true // PreparedPersistent.
                                              , false
                                              , false
                                              , true // CommitingPersistent. Not needed because of synchronize in commit.
                                              , false
                                              , false
                                              , true // BackingOutPersistent.  Not needed because of synchronize in commit.
                                              , false
        };

        requiresPersistentCheckpoint = checkpointRequired[state];

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { new Boolean(requiresPersistentCheckpoint) });
    } // setRequiresCheckpoint().

    /**
     * Called by the ObjectManager when it has written a checkpointStartLogRecord.
     * The Transaction can assume that all log records up to and including logSequenceNumber
     * are now safely hardened to disk. On this assumption it calls includedObjects
     * so that they can write after images to the ObjectStores.
     * 
     * @param forcedLogSequenceNumber the logSequenceNumber known to be forced to disk.
     * @throws ObjectManagerException
     */
    protected synchronized void checkpoint(long forcedLogSequenceNumber)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "checkpoint"
                        , new Object[] { new Long(forcedLogSequenceNumber), new Boolean(requiresPersistentCheckpoint) });

        // TODO If we have a STRATEGY_SAVE_ON_CHECKPOINT store then these objects need to be included here.
        // TODO currently STRATEGY_KEEP_UNTIL_NEXT_OPEN are not saved during a checkpoint.
        // TODO We will also need to track, nonPersistent,Serialized,and Persistent state in order to figure
        // TODO out whether or not to include the transaction in the checkpoint.

        // Was data logging required when we started the checkpoint
        // and is it still needed for recovery?
        if (requiresPersistentCheckpoint) {
            // Build subset lists of persistent tokens to recover.
            java.util.List persistentTokensToAdd = new java.util.ArrayList();
            java.util.List persistentTokensToReplace = new java.util.ArrayList();
            java.util.List persistentSerializedBytesToReplace = new java.util.ArrayList();
            java.util.List persistentTokensToOptimisticReplace = new java.util.ArrayList();
            java.util.List persistentTokensToDelete = new java.util.ArrayList();

            // Drive checkpoint for each included MangedObject.
            for (java.util.Iterator managedObjectIterator = includedManagedObjects.values().iterator(); managedObjectIterator.hasNext();) {
                ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
                if (managedObject.owningToken.getObjectStore()
                                .getPersistence()) {

                    // Has the last logged update been Forced to disk?
                    // If not the normal log record will be after the start of the chekpoint and it will
                    // cause normal recovery for that ManagedObject.
                    long logSequenceNumber = ((Long) logSequenceNumbers.get(managedObject.owningToken)).longValue();
                    if (forcedLogSequenceNumber >= logSequenceNumber) {
                        // The loggedSerializedBytes we currently have will have had any corrections made at preBackoutTime
                        // incorporated into them, so they are now the correct ones to write to the ObjectStore.
                        ObjectManagerByteArrayOutputStream serializedBytes = (ObjectManagerByteArrayOutputStream) loggedSerializedBytes.get(managedObject.owningToken);
                        long managedObjectSequenceNumber = ((Long) managedObjectSequenceNumbers.get(managedObject.owningToken)).longValue();
                        managedObject.checkpoint(this,
                                                 serializedBytes,
                                                 managedObjectSequenceNumber);

                        // Build the lists of objects to log.

                        if (managedObject.lockedBy(this)) { // Locking update?
                            switch (managedObject.getState()) {
                                case ManagedObject.stateAdded:
                                    persistentTokensToAdd.add(managedObject.owningToken);
                                    break;

                                case ManagedObject.stateReplaced:
                                    persistentTokensToReplace.add(managedObject.owningToken);
                                    // We have to rewrite the replaced serialized bytes in the log as part of the checkpoint.
                                    persistentSerializedBytesToReplace.add(serializedBytes);
                                    break;

                                case ManagedObject.stateToBeDeleted:
                                    persistentTokensToDelete.add(managedObject.owningToken);
                                    break;
                            } // switch.

                        } else { // OptimisticReplace update.
                            // A bit pointless as we dont do anything at recovery time.
                            persistentTokensToOptimisticReplace.add(managedObject.owningToken);
                        } // if (lockedBy(transaction)).

                    } // if (forcedLogSequenceNumber >= logSequenceNumber).
                } // if (managedObject.owningToken.getObjectStore().getPersistence()).
            } // for ... includedMansagedObjects.

            // The state indicates if the transaction is prepared, commiting or backing out.
            TransactionCheckpointLogRecord transactionCheckpointLogRecord = new TransactionCheckpointLogRecord(this,
                                                                                                               persistentTokensToAdd,
                                                                                                               persistentTokensToReplace,
                                                                                                               persistentSerializedBytesToReplace,
                                                                                                               persistentTokensToOptimisticReplace,
                                                                                                               persistentTokensToDelete,
                                                                                                               allPersistentTokensToNotify);

            // TODO Could correct any overestimate of the reserved log file space here.
            // We previously reserved some log space for this logRecord to be sure it will fit in the log.
            // We do not release it here because w reserved an extra page which we might not be able to get back after the
            // checkpoint has completed. Instead we just supress the check on the space used in the log.
            objectManagerState.logOutput.writeNext(transactionCheckpointLogRecord,
                                                   0,
                                                   false
                                                   , false);

            requiresPersistentCheckpoint = false;
        } // if (requiresPersistentCheckpoint).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "checkpoint"
                            );
    } // checkpoint().

    /**
     * Called by the ObjectManager when it has finished replaying the log but before it backs out
     * any inflight transactions and starts making forward progress.
     * Any ManagedObjects that have registered for callbacks during recovery wuill now be
     * informed that recovery is completed.
     * 
     * @param transaction the external Transaction.
     */
    protected void recoveryCompleted(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "recoveryCompleted"
                        , "transaction=" + transaction + "(Transaction)"
                            );

        // Inform any tokens that registered that we have now completed recovery.
        for (java.util.Iterator tokenIterator = callbackTokens.iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            ManagedObject managedObject = token.getManagedObject();

            // Drive the recoveryCompleted method for the object.
            managedObject.recoveryCompleted(transaction);
        } // for... callbackTokens.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "recoveryCompleted"
                            );
    } // End of recoveryCompleted() method.

    /*
     * Gives the ammount of space this transaction is reserving in the log.
     * 
     * @return long the number of bytes reserved in the log for this transaction.
     */
    protected long getLogSpaceReserved() {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getLogSpaceReserved"
                          + ")");

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getLogSpaceReserved"
                       , "returns logSpaceReserved=" + logSpaceReserved + "(long)"
                            );
        return logSpaceReserved;
    } // getLogSpaceReserved().

    /**
     * Print a dump of the state.
     * 
     * @param printWriter to be written to.
     */
    public void print(java.io.PrintWriter printWriter)
    {
        printWriter.println("State Dump for:" + getClass().getName()
                            + " state=" + state + "(int) " + stateNames[state] + "(String)"
                            + " transactionLock=" + transactionLock + "(TransactionLock)"
                        );
        if (logicalUnitOfWork == null)
            printWriter.println("logicalUnitOfWork=null");
        else {
            printWriter.print("logialUnitOfWork.identifier=" + logicalUnitOfWork.identifier + "(long)");
            if (logicalUnitOfWork.XID != null) {
                printWriter.print(" XID=");
                for (int i = 0; i < logicalUnitOfWork.XID.length; i++) {
                    printWriter.print(Integer.toHexString(logicalUnitOfWork.XID[i]));
                }
            }
            printWriter.println();
        }

        printWriter.println("logSpaceReserved=" + logSpaceReserved + "(long)"
                            + " requiresPersistentCheckpoint=" + requiresPersistentCheckpoint + "(boolean)"
                            + " transactionReference=" + transactionReference + "(TransactionReference)"
                        );

        printWriter.println("Included Objects...");
        for (java.util.Iterator managedObjectIterator = includedManagedObjects.values().iterator(); managedObjectIterator.hasNext();) {
            ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
            Token token = managedObject.getToken();
            printWriter.print(managedObject.toString());

            Long logSequenceNumber = (Long) logSequenceNumbers.get(token);
            Long managedObjectSequenceNumber = (Long) managedObjectSequenceNumbers.get(token);
            printWriter.print(" LSN=" + logSequenceNumber + " MSN=" + managedObjectSequenceNumber);

            ObjectManagerByteArrayOutputStream serializedBytes = (ObjectManagerByteArrayOutputStream) loggedSerializedBytes.get(token);
            if (serializedBytes == null)
                printWriter.print(" serializedBytes=null");
            else
                printWriter.print(serializedBytes);

            if (callbackTokens.contains(token))
                printWriter.println(" Callback");
            else
                printWriter.println();
        } // for ... includedMansagedObjects.

    } // print().

    /**
     * Test that a state transition is valid, but does not make the change.
     * Call must be synchronized on this.
     * 
     * @param nextState the new states.
     * @throws InvalidStateException if the transition would be invalid.
     */
    private void testState(int[] nextState)
                    throws InvalidStateException {
        final String methodName = "testState";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { nextState,
                                                                new Integer(state),
                                                                stateNames[state] });

        int newState = nextState[state]; // Test the state change.

        if (newState == stateError) {
            InvalidStateException invalidStateException = new InvalidStateException(this, state, stateNames[state]);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { invalidStateException,
                                                                   new Integer(newState),
                                                                   stateNames[newState] });
            throw invalidStateException;
        } // if (state == stateError).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // testState().

    /**
     * Makes a state transition. Call must be synchronized on this.
     * 
     * @param nextState the new states map.
     * @throws ObjectManagerException
     * @throws StateErrorException if the transition is invalid
     */
    private void setState(int[] nextState)
                    throws ObjectManagerException {
        final String methodName = "setState";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { nextState,
                                      new Integer(state),
                                      stateNames[state] });

        previousState = state; // Capture the previous state for dump.
        state = nextState[state]; // Make the state change.

        if (state == stateError) {
            StateErrorException stateErrorException = new StateErrorException(this, previousState, stateNames[previousState]);
            ObjectManager.ffdc.processException(this, cclass, "setState", stateErrorException, "1:2672:1.41");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "setState"
                           , new Object[] { stateErrorException, new Integer(state), stateNames[state] }
                                );
            throw stateErrorException;
        } // if (state == stateError).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { new Integer(state),
                                     stateNames[state] });
    } // setState().

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        String id = (logicalUnitOfWork==null?"(null)":""+logicalUnitOfWork.identifier);
        return new String("InternalTransaction"
                         + "(" + id + "." + useCount + ")"
                         + "/" + stateNames[state]
                         + "/" + Integer.toHexString(hashCode())
                         + "/" + logSpaceReserved
                         );
    } // toString().

    // orphan transaction diagnostics
    public String diagString()
    {
        return new String(toString()
                         // last complete() call
                         + ",co:" + (0==completeTick?"<not set>":""+completeTick)
                         + ":" + completeTid + ":\"" + completeName + "\""

                         // these are relative to completeTick as we're really going to be interested in relative order of events

                         // last addition to registeredInternalTransactions list
                         + ",re:" + (0==registeredTick?"<not set>":""+(registeredTick-completeTick))
                         + ":" + registeredTid + ":\"" + registeredName + "\""
                         // last addition to freeInternalTransactions list
                         + ",fr:" + (0==freeTick?"<not set>":""+(freeTick-completeTick))
                         + ":" + freeTid + ":\"" + freeName + "\""
                         // last linked external transaction finalize() call (enqueue)
                         + ",fi:" + (0==finaliseTick?"<not set>":""+(finaliseTick-completeTick))
                         );
    } // diagString().

    // ----------------------------------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------------------------------

    /**
     * This holds a WeakReference to the external Transaction so that we can
     * detect it becoming unreferenced by the application. It holds a strong reference to the
     * internal transaction so that we can find it again if this ever turns up on the
     * ObjectManagerState.orhpahTransactionsQueue.
     */
    protected class TransactionReference
                    extends java.lang.ref.WeakReference
    {
        protected InternalTransaction internalTransaction;

        TransactionReference(InternalTransaction internalTransaction
                             , Transaction externalTransaction)
        {
            super(externalTransaction
                  , objectManagerState.orphanTransactionsQueue);
            this.internalTransaction = internalTransaction;
        } // Constructor.

        /**
         * Handle completion of orphan transaction references found by the garbage collector.
         * Called by ObjectManagerState when a transaction has been found on the orphanedTransactionsQueue.
         * 
         * @throws ObjectManagerException
         */
        protected void complete()
                        throws ObjectManagerException {
            final String methodName = "complete";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName);

            synchronized (internalTransaction) {
                // We insist on this being the latest transactionReference before we will use it.
                if (this == internalTransaction.transactionReference) {

                    switch (internalTransaction.state) {
                    // We back out any transaction that is inactive but still registered,
                    // so as to get it deregistered.
                        case (stateInactive):
                        case (stateActiveNonPersistent):
                        case (stateActivePersistent):

                            Transaction transaction = new Transaction(internalTransaction);
                            transaction.setTerminationReason(Transaction.terminatedOrphanBackedOut);
                            trace.warning(this,
                                          cclass,
                                          methodName,
                                          "InternalTransaction_BackoutOrphan",
                                          transaction);

                            internalTransaction.backout(false
                                                        , transaction);
                            break;
                    } // switch (internalTransaction.state).
                } // if (this == internalTransaction.transactionReference)
            } // synchronized (internalTransaction).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // complete().

        // these are to help with diagnostics only
        protected LogicalUnitOfWork getLogicalUnitOfWork()
        {
            synchronized (internalTransaction)
            {
                return internalTransaction.getLogicalUnitOfWork();
            }
        }
        protected String getTransactionString()
        {
            synchronized (internalTransaction)
            {
                return internalTransaction.diagString();
            }
        }
        protected boolean isUnused()
        {
            synchronized (internalTransaction)
            {
                return (stateError==internalTransaction.state
                       ||stateTerminated==internalTransaction.state
                       ||(stateInactive==internalTransaction.state&&0==logSpaceReserved)
                      );
            }
        }
    } // inner class TransactionReference.
} // class internalTransaction.
