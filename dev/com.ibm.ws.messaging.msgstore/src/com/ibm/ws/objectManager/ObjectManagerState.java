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

import com.ibm.ws.objectManager.utils.FileLock;
import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * <p>
 * ObjectManagerState contains the persistent and transient state of the objectManager.
 * It is constructed as a ManagedObject and allocated to the defalutObjectStore.
 * The default ObjectStore is a dummy ObjectStore that behaves like a recoverable
 * ObjectStore. The persistent state is recovered from the log during restart of the ObjectManager.
 * and replaces the constructed ObjectManagerState by calling its becomeCloneOf() method
 * as if it had been recovered. The ObjectManagerState is recovered from a
 * a CheckpointStartLogRecord, which is the first thing to be read during restart. This also contains
 * the serialised forms of the other ObjectStores so that they can be opened and recovery of all other
 * objects can be performed.
 * 
 * Changes to ObjectStores and ObjectManagerState made during and after recovery are made as part
 * of normal transactions. Transient state is derived as a result of recovering the persistent state.
 * 
 * @version @(#) 1/25/13
 * @author IBM Corporation
 */
// Public for channelInstanceLogRecord on jqm.
public class ObjectManagerState
                extends ManagedObject
                implements SimplifiedSerialization
{
    private static final Class cclass = ObjectManagerState.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(ObjectManagerState.class,
                                                                     ObjectManagerConstants.MSG_GROUP);

    private static final long serialVersionUID = 1238245075497061436L;

    public static final boolean gatherStatistics = ObjectManager.gatherStatistics && true; // Built for statistics if true.

    // PK73292.1
    // This flag by default disables the migration of object stores 
    // at readObject time. If set to true it will migrate NIO stores
    // to non-NIO stores in the abscence of NIO support and vice versa.
    private static final boolean migrateObjectStores = false;

    static int numberOfProcessors = 1;
    static {
        // We cannot always invoke availableProcessors, because OSGImin does not support this.
        // We should re-discover the number of processors occasionally as it can change.
        try {
            java.lang.reflect.Method availableProcessorsMethod = Runtime.class.getMethod("availableProcessors",
                                                                                         new Class[] {});
            Integer integer = (Integer) availableProcessorsMethod.invoke(Runtime.getRuntime(),
                                                                         new Object[] {});
            numberOfProcessors = integer.intValue();
        } catch (Exception exception) {
            // No FFDC Code Needed.
            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                trace.event(cclass
                            , "<init>"
                            , exception
                                );
        }
    } // static initialiser.

    // --------------------------Persistent state.--------------------------------
    // No ManagedObjects may form part of the persistent ObjectManagerState. The
    // checkpointStartLogRecord holds this state and must be restored without
    // reference to any ObjectStores, because at that time they will not be Open.
    // ---------------------------------------------------------------------------

    // The ObjectStores managed by this ObjectManager, indexed by objectStoreIdentifier.
    // these are written in their serialized form to the checkpointStartLogRecord.
    protected java.util.Map objectStores = new java.util.HashMap();
    // The ObjectStoreIdentifier uniquely identifies an ObjectStore within this ObjectManager,
    // the maximumObjectStoreIdentifier is the highest number used so far.
    // Values 0-100 reserved.
    protected long maximumObjectStoreIdentifier = 100;
    // Limits on the number of transactions between checkpoints.
    protected long persistentTransactionsPerCheckpoint = 5000;
    protected long nonPersistentTransactionsPerCheckpoint = 5000;
    // When the log finds itself this full a checkpoint is triggered.
    protected float logFullTriggerCheckpointThreshold = (float) 0.4;
    // When the log is this full after a checkpoint start backing out big transactions.
    // At any other time we delay them until a checkpoint has completed.
    protected float logFullPostCheckpointThreshold = (float) 0.5;
    // The maximum number of active transactions this ObjectManager will allow.
    protected long maximumActiveTransactions = 1000;
    // The number of milliseconds we pause for during checkpoint to allow transactions to complete.
    protected int checkpointDelay = 100;
    // The folowing two fields are invariant and can be used to identify the ObjectManager.
    // The first 100 characters of the name of the log file used at cold start.
    protected String coldStartLogFileName;
    // The data when this ObjectManagerState was first created.
    // The number of milliseconds since 00:00:00 UTC on January 1, 1970.
    protected long coldStartTime = 0;

    /*---------------------- Define the state machine (begin) ----------------------*/
    // Tracks the life cycle of the ObjectManager.
    static final int stateError = 0; // A state error has occurred.
    static final int stateOpeningLog = 1; // Opening the Log file.
    static final int stateReplayingLog = 2; // Replaying the log.
    static final int stateWarmStarted = 3; // Warm Started.
    static final int stateColdStarted = 4; // Cold Started.
    static final int stateShutdownStarted = 5; // Shut down.
    static final int stateStopped = 6; // Shut down.

    // The names of the states for diagnostic purposes.
    static final String stateNames[] = { "Error",
                                        "OpeningLog",
                                        "ReplayingLog",
                                        "WarmStarted",
                                        "ColdStarted",
                                        "ShutdownStarted",
                                        "Stopped" };

    // What happens when we cannot open the log.
    static final int nextStateForLogFailedToOpen[] = { stateError,
                                                      stateColdStarted,
                                                      stateError,
                                                      stateError,
                                                      stateError,
                                                      stateError,
                                                      stateError };

    // What happens when we have opened the log.
    static final int nextStateForLogOpened[] = { stateError,
                                                stateReplayingLog,
                                                stateError,
                                                stateError,
                                                stateError,
                                                stateError,
                                                stateError };

    // What happens when we have opened the log.
    static final int nextStateForLogReplayed[] = { stateError,
                                                  stateError,
                                                  stateWarmStarted,
                                                  stateError,
                                                  stateError,
                                                  stateError,
                                                  stateError };

    // What happens when we have stopped user transactions.
    static final int nextStateForStartShutdown[] = { stateError,
                                                    stateShutdownStarted,
                                                    stateShutdownStarted,
                                                    stateShutdownStarted,
                                                    stateShutdownStarted,
                                                    stateError,
                                                    stateError };

    // What happens when we have stopped user transactions.
    static final int nextStateForShutdown[] = { stateError,
                                               stateError,
                                               stateError,
                                               stateError,
                                               stateError,
                                               stateStopped,
                                               stateError };
    // The current state of the ObjectManager.
    protected transient int state = stateOpeningLog;
    // The previous state, not refereneced but will appear in a dump.
    private transient int previousState = -1;
    /*---------------------- Define the state machine (end) ------------------------*/

    // --------------------------------------------------------------------------
    // Transient state.
    // --------------------------------------------------------------------------
    // We do not store logFileName in the log as part of checkpoint start or
    // check that we have the correct one as this would prevent a user from renaming
    // the log file.
    transient String logFileName; // Name of the log file.
    protected transient java.io.RandomAccessFile logFile; // Persistent storage, accessed read write.
    private transient FileLock logLock; // Lock on the log file.
    protected transient LogOutput logOutput; // Transaction log output.
    private transient String logFileMode; // The mode "rw" or "rwd" that was usd to open the log file.
    // One of ObjectManager.LOG_FILE_TYPE_XXX.
    protected transient int logFileType;
    // An ObjectStore that is always present.
    protected transient ObjectStore defaultObjectStore;
    protected static final int defaultObjectStoreIdentifier = 0;
    // True if the main copy false if just a clone.
    private boolean mainObjectManagerState;

    // ManagedObjects keyed by their name are stored in a transactional binary tree.
    // This may be stored in multiple ObjectStores, we make the first one we find the definitive one.
    protected transient Token namedObjects;

    // True if nio is available in this JVM.
    protected transient boolean nioAvailable = true;

    // The ObjectManager that originally instantiated this ObjectManagerState.
    protected transient ObjectManager objectManager;

    // The last LogicalUnitOfWork.identifier used, or zero .
    // Protected by a lock on registeredInternalTransactions.
    private transient long maximumLogicalUnitOfWorkIdentifier = 0;
    // The hashtable of InternalTransactions recognised by this objectManager.
    // Keyed off their logicalUnitOfWork.identifier.
    private transient java.util.Map registeredInternalTransactions = new ConcurrentHashMap(numberOfProcessors * 4);
    // The set of transactions made ready for reuse, also keyed of their LogicalUnitOfWork.identifier.
    private transient ConcurrentHashMap freeTransactions = new ConcurrentHashMap(numberOfProcessors * 4);
    // The value of acive Transactions we actually use, this may be less that the defined number
    // of maximumActiveTransactions if the log is too full.
    // Synchronized on registeredInternalTransactions.
    protected transient long currentMaximumActiveTransactions = maximumActiveTransactions;
    // The sum of registeredInternalTransactions.size() + freeTransactions.size()
    // Synchronized on registeredInternalTransactions.
    private transient long totalTransactions = 0;

    // Queue of transactions that are no longer referenced and are potentially reusable.
    protected transient java.lang.ref.ReferenceQueue orphanTransactionsQueue = new java.lang.ref.ReferenceQueue();
    protected transient DummyInternalTransaction dummyInternalTransaction;

    // logSequenceNumber for non persistent objects.
    private transient long dummyLogSequenceNumber = 0;
    private transient Object dummyLogSequenceNumberLock = new Object();
    protected transient boolean checkpointEndSeen;

    //Un-synced (dirty) counters used to give a rough indication of the number of transactions since
    // the last checkpoint.
    // Count of completed persistent transactions since checkpoint.
    private transient int persistentTransactionsSinceLastCheckpoint = 0;
    //Count of completed non-persistent transactions since checkpoint.
    private transient int nonPersistentTransactionsSinceLastCheckpoint = 0;

    // Global flag to indicate checkpoint has started.
    protected static final int CHECKPOINT_STARTING_NO = 0;
    protected static final int CHECKPOINT_STARTING_NONPERSISTENT = 1;
    protected static final int CHECKPOINT_STARTING_PERSISTENT = 2;
    protected transient int checkpointStarting = CHECKPOINT_STARTING_NO;

    // Worker thread for checkpointing.
    protected transient CheckpointHelper checkpointHelper = null;
    private transient long checkpointStartLogRecordSize;
    private transient long serializedSize;

    // A logical clock that determines the sequence (time) at which transactionLocks are released.
    private transient long transactionUnlockSequence = 0;
    // Users of transactionUnlockSequence must synchronise on the following.
    protected transient Object transactionUnlockSequenceLock = new Object();
    // Maps ObjectStore names to their actual disk locations.
    protected transient java.util.Map objectStoreLocations;

    // A pool of ObjectManagerByteArrayOutputStreams keyed by array size.
    static int byteArrayOutputStreamPoolsSize = 8;
    static int byteArrayOutputStreamPoolSize = 200;
    static int maximumByteArrayOutputStreamSize = 4 * 1024;
    private transient java.util.List[] byteArrayOutputStreamPools = new java.util.List[byteArrayOutputStreamPoolsSize];

    // For gatherStatistics.
    private transient long totalCheckpointsTaken = 0; // Number of checkpoints taken so far.
    private transient long waitingBetweenCheckpointsMilliseconds = 0;
    private transient long flushingCheckpointStartMilliseconds = 0;
    private transient long pausedDuringCheckpointMilliseconds = 0;
    private transient long flushingObjectStoresForCheckpointMilliseconds = 0;
    private transient long flushingEndCheckpointMilliseconds = 0;
    private transient long lastCheckpointMilliseconds;
    private transient long reusedTransactions = 0;
    private transient long totalTransactionsRequiringCheckpoint = 0;
    private transient long maximumTransactionsInAnyCheckpoint = 0;
    private transient long transactionsDelayedForLogFull = 0;

    private transient java.util.ArrayList callbacks = new java.util.ArrayList();

    /**
     * ObjectManagerState. Initialise the ObjectManagerState. Default Constructor method for this class.
     * 
     * @param logFileName the name of the transaction log File.
     * @param objectManager creating this ObjectManagerState.
     * @param logFileType one of ObjectManager.LOG_FILE_TYPE_XXX.
     * @param objectStoreLocations maps ObjectStoreName to its disk location.
     *            May be null.
     * @param callbacks an array of ObjectManagerEventCallback to be registered with the ObjectManager
     *            before it starts, may be null.
     * @throws ObjectManagerException
     */
    protected ObjectManagerState(String logFileName,
                                 ObjectManager objectManager,
                                 int logFileType,
                                 java.util.Map objectStoreLocations,
                                 ObjectManagerEventCallback[] callbacks)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { logFileName,
                                                                objectManager,
                                                                Integer.toString(logFileType),
                                                                objectStoreLocations,
                                                                callbacks });

        this.logFileName = logFileName;
        this.objectManager = objectManager;
        this.logFileType = logFileType;
        this.objectStoreLocations = objectStoreLocations;
        if (callbacks != null)
            for (int i = 0; i < callbacks.length; i++)
                this.callbacks.add(callbacks[i]);

        // Create ArrayLists for the ObjectManagerByteArray pools.
        for (int i = 0; i < byteArrayOutputStreamPools.length; i++)
            byteArrayOutputStreamPools[i] = new java.util.ArrayList(byteArrayOutputStreamPoolSize);

        // Create a defaultObjectStore to hold the ObjectManagerState, this
        // does not really store the state just acts as if it does.
        // The constructor does not drive registerObjectStore as the ObjectManagerState
        // is not initialised yet. Also we don't want applications to find it or for it to be save during
        // checkpoint processing.
        defaultObjectStore = new DummyFileObjectStore(this);
        defaultObjectStore.setIdentifier(defaultObjectStoreIdentifier);

        // Open the store now.
        defaultObjectStore.open(this);

        // Since the initial ObjectManager state is not initially recovered, set
        // ourselves to ready rather than constructed, all subsequent operations
        // on ObjectManagerState can be replace() rather than add().
        super.state = ManagedObject.stateReady;
        // We are the real state.
        mainObjectManagerState = true;
        // Create a token for the ObjectManager state, in the defaultObjectStore.
        Token objectManagerStateToken = new Token(this,
                                                  defaultObjectStore,
                                                  ObjectStore.objectManagerStateIdentifier.longValue());
        // Make sure the defaultObjectStore knows about the objectManagerStateToken.
        objectManagerStateToken = objectManagerStateToken.current();

        // See if nio is available.
        try {
            Class.forName("java.nio.channels.FileChannel");
        } catch (java.lang.ClassNotFoundException exception) {
            // No FFDC Code Needed.
            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                trace.event(this,
                            cclass,
                            methodName,
                            exception);
            nioAvailable = false;
        }

        // Access the log File.
        // This creates a zero length file except if LOG_FILE_TYPE_NONE is used.
        try {
            logFileMode = "rwd"; // Read, Write, Disk force without meta data.
            if (!nioAvailable)
                logFileMode = "rw"; // Read, Write.
            logFile = new java.io.RandomAccessFile(logFileName,
                                                   logFileMode);
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this, cclass, methodName, new Object[] { "Opened log file:384",
                                                                    logFileName,
                                                                    logFileMode,
                                                                    logFile });

        } catch (java.io.FileNotFoundException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:395:1.62");

            shutdown();
            // We opened the log file rwd, so this does not mean that the log file does not exist,
            // it might mean that we cannot open it for writing.
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { exception, logFileName });
            throw new NonExistentLogFileException(this,
                                                  exception,
                                                  logFileName);
        } // catch (java.io.FileNotFoundException exception).

        // Make sure no one else can write to the log.
        // Obtain an exclusive lock on the log File.
        try {
            logLock = FileLock.getFileLock(logFile,
                                           logFileName);
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:421:1.62");

            shutdown();
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch (ObjectManagerException).

        if (!logLock.tryLock()) { // Did we get the lock?
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>",
                           "via LogFileInUseException, logFileName=" + logFileName + "(String)");
            throw new LogFileInUseException(this,
                                            logFileName);
        } // (!logLock.tryLock()).

        // Does the log file exist yet or should we create it.
        // TODO do we need a mode where we throw an exception rather than creating the file?
        try {
            if (logFile.length() == 0) {
                // Either the log file did not exist or it is a completely empty file.
                setState(nextStateForLogFailedToOpen); // Make the state change.
            } else {
                setState(nextStateForLogOpened);
            }
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:458:1.62");

            shutdown();
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        // Set up the DummyInternalTransaction.
        dummyInternalTransaction = new DummyInternalTransaction(this,
                                                                new LogicalUnitOfWork(maximumLogicalUnitOfWorkIdentifier++));
        registeredInternalTransactions.remove(new Long(dummyInternalTransaction.getLogicalUnitOfWork().identifier));

        // Now we know what happened when we tried to find and open the log we can perform
        // the appropriate kind of startup.
        if (state == stateReplayingLog) {
            performWarmStart();
        } else { // Could not find the log.
            performColdStart();
        } // if (state == stateReplayingLog).

        // Establish the real limit on transactions.
        currentMaximumActiveTransactions = maximumActiveTransactions;
        totalTransactions = registeredInternalTransactions.size() + freeTransactions.size();

        // Reserve space in the log for another set of checkpoint records. we need to be able to write
        // another set in addition to the correct ones so that we can truncate the log.
        // The check on the log space is suppressed when we write the checkpoint log records.
        // Notice that we round up by two pages when we write checkpoint end because the truncate
        // method uses one page to ensure that checkpoint end is written and another one to
        // force the truncate of the log.
        checkpointStartLogRecordSize = ((new CheckpointStartLogRecord(this)).getBytesLeft() / FileLogOutput.pageSize + 1) * FileLogOutput.pageSize;
        logOutput.reserve(((new CheckpointEndLogRecord()).getBytesLeft() / FileLogOutput.pageSize + 2) * FileLogOutput.pageSize
                          + checkpointStartLogRecordSize);

        // Start the thread that executes a checkpoint.
        checkpointHelper = new CheckpointHelper();
        if (gatherStatistics) // Start the clock.
            lastCheckpointMilliseconds = System.currentTimeMillis();
        // Make the objectManagerState persistent.
        checkpointHelper.waitForCheckpoint(true);

        // Let any waiters know we are ready for business.
        synchronized (this) {
            notifyAll();
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // ObjectManagerState();

    /**
     * Cold start the ObjectManager.
     * 
     * @throws ObjectManagerException
     */
    protected void performColdStart()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "performColdStart");

        // This is presumably the first startup of this ObjectManager.
        // Record the name of the log file.
        coldStartLogFileName = logFileName;
        // Record the time.
        coldStartTime = System.currentTimeMillis();

        // Access the log files for forward processing.
        switch (logFileType) {
            case (ObjectManager.LOG_FILE_TYPE_FILE):
            case (ObjectManager.LOG_FILE_TYPE_CLEAR):
                logOutput = new FileLogOutput(logFile,
                                              this);
                break;

            case (ObjectManager.LOG_FILE_TYPE_NONE):
                // Note that by default this Dummy log has a filesize of zero, when used with
                // a save on shutdown store this means that the store file is increneted as each
                // object is added rather than in chunks according to the log file size.
                logOutput = new DummyLogOutput();
                break;

            default:
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "performColdStart",
                               new Integer(logFileType));
                throw new InvalidLogFileTypeException(this,
                                                      logFileType);
        } // switch (logFileType).

        trace.debug(cclass, "performColdStart", "ObjectManager using logFile " + logFileName + " was cold started");
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "performColdStart",
                       new Object[] { coldStartLogFileName, new java.util.Date(coldStartTime) });
    } // performColdStart().

    /**
     * Warm start the ObjectManager. Roll back any un prepared transactions. Transactions remaining will not yet be know
     * outside this ObjectManager.
     * 
     * @throws ObjectManagerException
     */
    protected void performWarmStart()
                    throws ObjectManagerException
    {
        final String methodName = "performWarmStart";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        FileLogInput logInput = new FileLogInput(logFile,
                                                 this);
        // Access a dummy log for output written during recovery.
        logOutput = new DummyLogOutput();
        // Transfer the log file size to logOutput so we can assume the same size file
        // during recovery. This also allows the ObjectStores to calculate their file
        // space requirements based on the ultimate size of the log.
        logOutput.setLogFileSize(logInput.getLogFileSize());

        // TODO  For emergency restart where the Log is lost.
        // Re-establish the anchor for the permanent state of the object manager,
        // which is Token number 1 in the default object store.

        // During restart we set currentMaximumActiveTransactions large in case it has been reduced below what we need.
        currentMaximumActiveTransactions = Integer.MAX_VALUE;

        performRecovery(logInput); // Read the log records.
        // Can't close logInput because it closes the logFile.
        // logInput.close(); // Finished with the log input.
        logOutput.close(); // Finished with the dummy log output.

        // Switch to a real logOutput.
        // Make the log files available for forward processing.
        // The LogInput position is now pointed at the page after the valid part of what
        // we have just read, this is the first invalid page after the last completely
        // written page. Make this the first page to be written by LogOutput.
        logOutput = new FileLogOutput(logFile,
                                      logOutput.getLogSequenceNumber(),
                                      logInput.getCurrentPage(),
                                      this);

        // Should not be needed as we are single threaded during recovery.
        synchronized (registeredInternalTransactions) {
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this
                            , cclass
                            , methodName
                            , "Inform registered transactions that recovery is completed:620");
            // Reserve the space the active transactions need in the log.
            // Inform the transactions that we have completed recovery.
            for (java.util.Iterator registeredTransactionIterator = registeredInternalTransactions.values()
                            .iterator(); registeredTransactionIterator.hasNext();) {
                InternalTransaction registeredTransaction = (InternalTransaction) registeredTransactionIterator.next();
                // Do not use getExternalTransaction because this would make a weak reference, which would cause
                // the transaction to be backed out as soon as we drop our reference to it.
                Transaction transaction = new Transaction(registeredTransaction);
                logOutput.reserve(registeredTransaction.getLogSpaceReserved());
                transaction.recoveryCompleted();
            } // for registeredInternalTransactions.

            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this, cclass, methodName, "Complete transactions:634");

            for (java.util.Iterator registeredTransactionIterator = registeredInternalTransactions.values()
                            .iterator(); registeredTransactionIterator.hasNext();) {
                InternalTransaction registeredTransaction = (InternalTransaction) registeredTransactionIterator.next();
                Transaction transaction;

                switch (registeredTransaction.state) {
                    case (Transaction.statePrePreparedPersistent):
                        transaction = new Transaction(registeredTransaction);
                        transaction.prepare();
                        if (logFileType == ObjectManager.LOG_FILE_TYPE_CLEAR)
                            transaction.backout(false);
                        break;

                    case (Transaction.stateCommitingPersistent):
                        transaction = new Transaction(registeredTransaction);
                        transaction.commit(false); // Do not reuse this transaction.
                        break;

                    case (Transaction.stateActivePersistent):
                        // Back out all transactions that are not in prepared state.
                    case (Transaction.stateBackingOutPersistent):
                        transaction = new Transaction(registeredTransaction);
                        transaction.backout(false); // Do not reuse this transaction.
                        break;

                } // switch (registeredTransaction.state).
            } // for registeredInternalTransactions.
        } // synchronized (registeredInternalTransactions).

        Transaction transaction = getTransaction();
        if (logFileType == ObjectManager.LOG_FILE_TYPE_CLEAR) {
            // We will ake new namedObjects Trees, later.
            namedObjects = null;

            for (java.util.Iterator objectStoreIterator = objectStores.values().iterator(); objectStoreIterator.hasNext();) {
                ObjectStore objectStore = (ObjectStore) objectStoreIterator.next();
                objectStore.clear();
            } // for objectStores...
        } // if (logFileType == ObjectManager.LOG_FILE_TYPE_CLEAR).

        // If an objectStore is SAVE_ONLY_ON_SHUTDOWN and we fail before shutdown then the
        // restart data will not be saved, this may cause problems because the named Object tree is
        // not there after restart because the store is empty but is assumed to contain the tree.
        // It is good practice to shutdown and restart after creating a SAVE__ONLY_ON_SHUTOWN ObjectStore.
        // Force the namedObjectTree to exist in all ObjectStores that contain restart data.
        for (java.util.Iterator objectStoreIterator = objectStores.values().iterator(); objectStoreIterator.hasNext();) {
            ObjectStore objectStore = (ObjectStore) objectStoreIterator.next();

            // Make new namedObjects Trees, but don't bother with
            // ObjectStores that can't be used for recovery.
            if (objectStore.getContainsRestartData()) {
                TreeMap namedObjectsTree = getNamedObjects(objectStore, transaction);
                if (namedObjects == null)
                    namedObjects = namedObjectsTree.getToken();
            } // if (objectStore.getContainsRestartData()).

        } // for objectstores...

        transaction.commit(false);

        trace.debug(cclass, methodName, "ObjectManager using logFile " + logFileName + " was warm started logFileType=" + ObjectManager.logFileTypeNames[logFileType]);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { coldStartLogFileName, new java.util.Date(coldStartTime) });
    } // performWarmStart().

    /**
     * Recover the state of the objectManager from the Log. Re apply changes to object stores that were lost.
     * 
     * @param logInput the logger that LogRecords are read from.
     * @throws ObjectManagerException
     */
    private final void performRecovery(LogInput logInput)
                    throws ObjectManagerException {
        final String methodName = "performRecovery";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { logInput });

        // Set no checkpoint end yest seen.
        checkpointEndSeen = false;
        // We start reading the log at the start of the last complete checkpoint.
        // The checkpointed and in flight transactions are recovered until we find checkpoint end.

        try {
            for (;;) { // Loop over input log records.
                LogRecord logRecordRead = logInput.readNext();
                logRecordRead.performRecovery(this);
            } // for log records to read.

        } catch (LogFileExhaustedException exception) {
            // No FFDC Code Needed, condition expected when end of log seen.
            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                trace.event(this,
                            cclass,
                            methodName,
                            exception);

        } catch (ObjectManagerException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:751:1.62");

            shutdown();
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           "ObjectManagerException:758");
            throw exception;
        } // try.

        // Have wee seen a completed checkpoint?
        if (checkpointEndSeen == false) {
            // TODO At cold start, we need to mark the log header so that it indicates that we have not yet
            //      completed the first ever checkpoint. If we fail before we do this we then know
            //      that we crashed before it is rewritten, we can safely have another go at writing it
            //      another first checkpoint. We could just assume that because we have not seen
            //      a checkpoint end then we did not successfully cold start and have another go.
            shutdown();
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { "CheckpointEndNotFoundException:774", logFileName });
            throw new CheckpointEndNotFoundException(this,
                                                     logFileName);
        }

        synchronized (this) {
            // Make the state change.
            setState(nextStateForLogReplayed);
        } // synchronized (this).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // performRecovery().

    /**
     * Create a checkpoint of this ObjectManagerState and all active transactions in the Log, sufficient to restart from.
     * 
     * @param persistentTransactions true if we checkpoint both persistent (logged) and non persistent objects
     *            otherwise we only checkpoint the non persistent unlogged objects.
     * @throws ObjectManagerException
     */
    private void performCheckpoint(boolean persistentTransactions)
                    throws ObjectManagerException
    {
        final String methodName = "performCheckpoint";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Boolean(persistentTransactions));

        if (gatherStatistics) {
            long now = System.currentTimeMillis();
            waitingBetweenCheckpointsMilliseconds += now - lastCheckpointMilliseconds;
            lastCheckpointMilliseconds = now;
        } // if (gatherStatistics).

        // Recover and complete Orphan transactions.
        completeOrphanTransactions();

        long logSequenceNumber = 0;
        if (persistentTransactions) {
            // Mark the point in the log that we can truncate to once checkpoint is complete,
            // then write start of checkpoint to the log. This makes sure that the CheckpointStart is recovered first,
            // which ensures that all of the ObjectStores are known at startup.
            CheckpointStartLogRecord checkpointStartLogRecord = new CheckpointStartLogRecord(this);
            // We don't release the reserved space for the Checkpoint log records as
            // we will need to re-reserve it after the checkpoint has completed. Instead we just
            // suppress the check on the log space because we reserved the space for the
            // checkpoint start and end records earlier.
            // Updates to QueueManagerState must be recorded in the log before we
            // flush the ObjectStores, otherwise we risk having named objects in the stores
            // which are not known in the ObjectManager.
            logSequenceNumber = logOutput.markAndWriteNext(checkpointStartLogRecord,
                                                           0,
                                                           false, // Do not check the log space, we reserved it earlier.
                                                           true);
            if (gatherStatistics) {
                long now = System.currentTimeMillis();
                flushingCheckpointStartMilliseconds += now - lastCheckpointMilliseconds;
                lastCheckpointMilliseconds = now;
            } // if (gatherStatistics).

            checkpointStarting = CHECKPOINT_STARTING_PERSISTENT;
            // Mark all active transactions as requiring a checkpoint.
            for (java.util.Iterator registeredTransactionIterator = registeredInternalTransactions.values().iterator(); registeredTransactionIterator.hasNext();) {
                InternalTransaction registeredTransaction = (InternalTransaction) (registeredTransactionIterator.next());
                // The registered transaction might be null if it was removed from the map after
                // we built the iterator. See ConcurrentHashMap and java.util.Map.Entry.getValue().
                if (registeredTransaction != null)
                    registeredTransaction.setRequiresCheckpoint();
            } // for registeredInternalTransactions.
        } else {
            checkpointStarting = CHECKPOINT_STARTING_NONPERSISTENT;
        } // if (persistentTransactions).

        // From now on, new transactions write to the following checkpoint in the file store.
        checkpointStarting = CHECKPOINT_STARTING_NO;

        // --------------------------------------------------------------------------------------------
        // Pause so that:
        // 1) In flight persistent transactions might complete, avoiding the need to write a checkpoint
        //    record for them.
        // 2) Objects in the checkpoint set which will be written to the Object Store might get deleted
        //    avoiding the need to write them.
        // --------------------------------------------------------------------------------------------
        Object lock = new Object();
        synchronized (lock) {
            try {
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this, cclass, methodName, new Object[] { "wait:867",
                                                                        new Integer(checkpointDelay) });

                lock.wait(checkpointDelay); // Let some transactions complete.
            } catch (InterruptedException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    exception,
                                                    "1:877:1.62");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               exception);
                throw new UnexpectedExceptionException(this,
                                                       exception);
            } // catch (InterruptedException exception).
        } // synchronized (lock).

        if (gatherStatistics) {
            long now = System.currentTimeMillis();
            pausedDuringCheckpointMilliseconds += now - lastCheckpointMilliseconds;
            lastCheckpointMilliseconds = now;
        } // if (gatherStatistics).

        // Now that the log is forced tell the active transactions they can safely update the ObjectStores
        // on the assumption that their logRecords prior to logSequenceNumber have been forced to disk.
        // The Transactions take their checkpoint on the assumption that the ObjectStores contain their
        // ManagedObjects. The Object Stores have not been forced yet but the log has,
        // so this is a safe assumption.
        long transactionsRequiringCheckpoint = 0;
        for (java.util.Iterator registeredTransactionIterator = registeredInternalTransactions.values()
                        .iterator(); registeredTransactionIterator.hasNext();) {
            InternalTransaction registeredTransaction = (InternalTransaction) (registeredTransactionIterator.next());
            // The registered transaction might be null if it was removed from the map after
            // we built the iterator. See ConcurrentHashMap and java.util.Map.Entry.getValue().
            // Checkpoint only transactions that have not cleared their reqiuirement for a checkpoint.

            if (registeredTransaction != null && registeredTransaction.requiresPersistentCheckpoint) {
                registeredTransaction.checkpoint(logSequenceNumber);
                transactionsRequiringCheckpoint++;
            } // if ... registeredTransaction.requiresCheckpoint.
        } // for registeredInternalTransactions.

        if (gatherStatistics) {
            totalTransactionsRequiringCheckpoint = totalTransactionsRequiringCheckpoint + transactionsRequiringCheckpoint;
            maximumTransactionsInAnyCheckpoint = Math.max(transactionsRequiringCheckpoint, maximumTransactionsInAnyCheckpoint);
        } // if (gatherStatistics).

        // Flush ObjectStores to disk.
        // Loop over a copy of the store collection, in case a new one is registered while we we do this.
        ObjectStore[] stores;
        synchronized (objectStores) {
            stores = (ObjectStore[]) objectStores.values().toArray(new ObjectStore[objectStores.size()]);
        }
        for (int i = 0; i < stores.length; i++) {
            // Let the store decide whether to flush according to its storage strategy.
            if (persistentTransactions)
                stores[i].flush();
            else if (!stores[i].getPersistence())
                stores[i].flush();
        } // for.

        if (gatherStatistics) {
            long now = System.currentTimeMillis();
            flushingObjectStoresForCheckpointMilliseconds += now - lastCheckpointMilliseconds;
            lastCheckpointMilliseconds = now;
            totalCheckpointsTaken++;
        } // if (gatherStatistics).

        if (persistentTransactions) {
            // Write end of checkpoint to the log.
            CheckpointEndLogRecord checkpointEndLogRecord = new CheckpointEndLogRecord();
            logOutput.writeNext(checkpointEndLogRecord,
                                0,
                                false, // Do not ckeck the log space, we reserved it earlier.
                                false);
            // Force the log and truncate to the mark point set when we wrote checkpointStartLogRecord.
            logOutput.truncate();

            if (gatherStatistics) {
                long now = System.currentTimeMillis();
                flushingEndCheckpointMilliseconds += now - lastCheckpointMilliseconds;
                lastCheckpointMilliseconds = now;
            } // if (gatherStatistics).

            // --------------------------------------------------------------------------------------------
            // If the log is still too full start backing out transactions.
            // --------------------------------------------------------------------------------------------

            synchronized (registeredInternalTransactions) {
                if (logOutput.isOcupancyHigh()) {
                    // Reduce other work in the ObjectManager so that checkpointing can catch up.
                    currentMaximumActiveTransactions = Math.max(registeredInternalTransactions.size() / 2,
                                                                2);

                    // Find the biggest transaction in terms of its reserved space in the log.
                    InternalTransaction biggestTransaction = null;
                    long biggestTransactionSize = 0;
                    for (java.util.Iterator transactionIterator = registeredInternalTransactions.values()
                                    .iterator(); transactionIterator.hasNext();) {
                        InternalTransaction internalTransaction = (InternalTransaction) transactionIterator.next();
                        long transactionSize = internalTransaction.getLogSpaceReserved();
                        if (transactionSize > biggestTransactionSize
                            && internalTransaction.state == Transaction.stateActivePersistent) {
                            biggestTransaction = internalTransaction;
                            biggestTransactionSize = transactionSize;
                        } // if (transactionSize...
                    } // for... registeredInternalTransactions.

                    if (biggestTransaction != null) {
                        Transaction transaction = biggestTransaction.getExternalTransaction();
                        // We cannot synchronize on the internal transaction because we might deadlock an application thread.
                        // Application threads may synchronize on their ManagedObjects and then invoke synchronized transaction
                        // methods, so we must not hold a transaction lock when calling backout. For example LinkedLists
                        // synchronize on the list when the preBackout callback is made then they invoke synchronized transaction
                        // methods. If we hold the transaction lock first we would deadlock another thread making transaction
                        // calls while synchronized on the list.
                        // We just catch the state exception and discard it.
                        try {
                            if (biggestTransaction.state == Transaction.stateActivePersistent) {
                                // It is possible that the biggest transaction has now moved on to some other work,
                                // but we need to backout something so that will ahve to do.
                                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                                    trace.debug(this, cclass, methodName,
                                                new Object[] { "Log too full, backing out biggestTransaction:995",
                                                              biggestTransaction });

                                trace.warning(this,
                                              cclass,
                                              methodName,
                                              "ObjectManagerState_LogTooFull",
                                              biggestTransaction);

                                transaction.setTerminationReason(Transaction.terminatedLogTooFull);
                                transaction.backout(false);

                            } // if ( biggestTransaction.state...
                        } catch (InvalidStateException exception) {
                            // No FFDC Code Needed, condition expected when we race with the application thread.
                            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                                trace.event(this,
                                            cclass,
                                            methodName,
                                            exception);
                        } // catch (StateErrorException exception).

                        requestCheckpoint(true); // Trigger another checkpoint to see if things have got better.
                    }
                } else { // log file not particularly full.
                    // We had previously reduced the number of active transactions,
                    // this will allw the number to recover. As new transactions come along they
                    // will run and the backlog will be cleared as transactions finish.
                    currentMaximumActiveTransactions = maximumActiveTransactions;
                } // if (((FileLogOutput)logOutput).getOcupancy()...

                // If we have more transactions than we need release some freeTrasactions.
                if (totalTransactions > currentMaximumActiveTransactions)
                    totalTransactions = totalTransactions - freeTransactions.clear(totalTransactions - currentMaximumActiveTransactions);
            } // synchronized (registeredInternalTransactions).
        } // if (persistentTransactions).

        // finally reset transactions since last checkpoint
        if (persistentTransactions)
            persistentTransactionsSinceLastCheckpoint = 0;
        nonPersistentTransactionsSinceLastCheckpoint = 0;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // performCheckpoint().

    /**
     * @return Returns the logFileName.
     */
    protected String getLogFileName()
    {
        return logFileName;
    }

    /**
     * @return Returns the logFileMode.
     */
    protected String getLogFileMode()
    {
        return logFileMode;
    }

    /**
     * Create or update a copy of the ObjectManagerState in each ObjectStore.
     * The caller must be synchronised on objectStores.
     * 
     * @param transaction controlling the update.
     * @throws ObjectManagerException
     */
    protected void saveClonedState(Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "saveClonedState";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, transaction);

        // Loop over all of the ObjecStores.
        java.util.Iterator objectStoreIterator = objectStores.values()
                        .iterator();
        while (objectStoreIterator.hasNext()) {
            ObjectStore objectStore = (ObjectStore) objectStoreIterator.next();

            // Don't bother with ObjectStores that can't be used for recovery.
            if (objectStore.getContainsRestartData()) {

                // Locate any existing copy.
                Token objectManagerStateCloneToken = new Token(objectStore,
                                                               ObjectStore.objectManagerStateIdentifier.longValue());
                // Swap for the definitive Token, if there is one.
                objectManagerStateCloneToken = objectStore.like(objectManagerStateCloneToken);

                ObjectManagerState objectManagerStateClone = (ObjectManagerState) objectManagerStateCloneToken.getManagedObject();

                // Does the cloned state already exist?
                if (objectManagerStateClone == null) {
                    objectManagerStateClone = new ObjectManagerState();
                    objectManagerStateClone.objectStores = objectStores;
                    objectManagerStateClone.becomeCloneOf(this);
                    objectManagerStateCloneToken.setManagedObject(objectManagerStateClone);
                    transaction.add(objectManagerStateClone);

                } else {
                    transaction.lock(objectManagerStateClone);
                    objectManagerStateClone.objectStores = objectStores;
                    objectManagerStateClone.becomeCloneOf(this);
                    transaction.replace(objectManagerStateClone);
                } //  if (objectManagerStateClone == null).
            } // if (objectStore.getContainsRestartData()).
        } // While objectStoreIterator.hasNext().

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // saveClonedState().

    /**
     * Blocking request for a checkpoint, returns once a checkpoint has been completed.
     * 
     * @param persistent true if persistent work is to be checkpointed.
     * @throws ObjectManagerException
     */
    protected final void waitForCheckpoint(boolean persistent)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "waitForCheckpoint");

        checkpointHelper.waitForCheckpoint(persistent);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "waitForCheckpoint",
                       "persistent=" + persistent + "(boolean)");
    } // waitForCheckpoint().

    /**
     * Non blocking request for a checkpoint, returns immediately. If multiple requests are made this may result in fewer
     * checkpoints than requests.
     * 
     * @param persistent true if persistent work is to be checkpointed.
     * @throws ObjectManagerException
     */
    protected final void requestCheckpoint(boolean persistent)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "requestCheckpoint",
                        new Object[] { new Boolean(persistent) });

        // During restart checkpointHelper is not running so don't request a checkpoint.
        // A checkpoint is forced at startup once it is safe to do so.
        if (checkpointHelper != null)
            checkpointHelper.requestCheckpoint(persistent);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "requestCheckpoint");
    } // requestCheckpoint().

    /**
     * Non blocking suggestion that a checkpoint should occur, returns immediately. See
     * checkpointHelper.suggestCheckPoint(boolean) for details.
     * 
     * @param persistent true if persistent work is to be checkpointed.
     * @throws ObjectManagerException
     */
    protected final void suggestCheckpoint(boolean persistent)
                    throws ObjectManagerException
    {
        if (trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "suggestCheckpoint",
                        new Object[] { new Boolean(persistent) });

        // During restart checkpointHelper is not running so don't suggest a checkpoint.
        // A checkpoint is forced at startup once it is safe to do so.
        if (checkpointHelper != null)
            checkpointHelper.suggestCheckpoint(persistent);

        if (trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "suggestCheckpoint");
    } // requestCheckpoint().

    /**
     * Terminates the ObjectManagerState and hence the ObjectManager.
     * 
     * @throws ObjectManagerException
     */
    protected final void shutdown()
                    throws ObjectManagerException
    {
        final String methodName = "shutdown";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName);

        Exception exceptionCaughtDuringShutdown = null;

        synchronized (this) {
            // Make the state change.
            setState(ObjectManagerState.nextStateForStartShutdown);
        } // synchronized (this).

        // Block the creation of new transactions.
        currentMaximumActiveTransactions = 0; // Don't start any new transactions.

        // Defect 496893
        // Call our event callbacks at this point as we are no 
        // longer open for business.
        synchronized (this)
        {
            if (callbacks != null)
            {
                java.util.Iterator it = callbacks.iterator();
                while (it.hasNext())
                {
                    ObjectManagerEventCallback callback = (ObjectManagerEventCallback) it.next();

                    callback.notification(ObjectManagerEventCallback.objectManagerStopped, null);
                }
            }
        }

        // Back out all transactions that are not in prepared state.
        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(this,
                        cclass,
                        methodName,
                        "Backout remaining unprepared transactions:1231");

        // Count the number of transactions in each state.
        int[] transactionsPerState = new int[InternalTransaction.stateNames.length];
        try {
            for (java.util.Iterator registeredTransactionIterator = registeredInternalTransactions.values().iterator(); registeredTransactionIterator.hasNext();) {
                InternalTransaction registeredTransaction = (InternalTransaction) registeredTransactionIterator.next();
                synchronized (registeredTransaction) {
                    if (registeredTransaction.state == Transaction.stateInactive
                        || registeredTransaction.state == Transaction.stateActiveNonPersistent
                        || registeredTransaction.state == Transaction.stateActivePersistent) {

                        // Check that the transaction did not commit or backout before we synchronized on it.
                        if (registeredInternalTransactions.get(new Long(registeredTransaction.getLogicalUnitOfWork().identifier)) == registeredTransaction) {
                            // Use getExternalTransaction because we need to ensure that any application will have its external
                            // Transaction terminated.
                            Transaction transaction = registeredTransaction.getExternalTransaction();
                            transaction.setTerminationReason(Transaction.terminatedShutdown);
                            transaction.backout(false); // Do not reuse this transaction.
                        } //  if ( registeredInternalTransactions...
                    } // if (   registeredTransaction.state...

                    transactionsPerState[registeredTransaction.state]++;
                } // synchronized (registeredTransaction).
            } // for registeredInternalTransactions.
        } catch (Exception exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:1263:1.62");
            exceptionCaughtDuringShutdown = exception;
        } // try.

        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()) {
            Object[] objects = new Object[transactionsPerState.length];
            for (int i = 0; i < transactionsPerState.length; i++)
                objects[i] = InternalTransaction.stateNames[i] + "=" + Integer.toString(transactionsPerState[i]);
            trace.debug(this, cclass, methodName, "Transactions in each state:1271");
            trace.debug(this, cclass, methodName, objects);
        } // if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()).

        synchronized (this) {
            // Make the state change.
            setState(ObjectManagerState.nextStateForShutdown);
        } // synchronized (this).

        try {
            // Take a checkpoint. ObjectStores with STRATEGY_SAVE_ONLY_ON_SHUTDOWN can now safely
            // update their data.
            if (checkpointHelper != null) {
                checkpointHelper.waitForCheckpoint(true);
                checkpointHelper.shutdown(); // Terminate the checkpoint thread.
                checkpointHelper = null;
            } // if (checkpointHelper != null).
        } catch (Exception exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:1294:1.62");
            exceptionCaughtDuringShutdown = exception;
        } // try.

        // Set all active transactions to a state that wont allow any more actions.
        // We cannot do this before the checkpoint as this sets the InternalTransactions
        // into stateTerminated and excludes them from the checkpoint. As a consequence
        // applications might get data into the log after the last checkpoint has started.
        try {
            for (java.util.Iterator transactionIterator = registeredInternalTransactions.values().iterator(); transactionIterator.hasNext();) {
                InternalTransaction internalTransaction = (InternalTransaction) transactionIterator.next();
                internalTransaction.shutdown();
            } // for... registeredInternalTransactions.
        } catch (Exception exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:1315:1.62");
            exceptionCaughtDuringShutdown = exception;
        } // try.

        //    if (reportStatistics) { // Stop monitoring statistics.
        //      if (statisticsHelper != null)
        //        statisticsHelper.shutdown();
        //      statisticsHelper = null;
        //    } // if (reportStatistics)

        // Close all of the ObjecStores.
        synchronized (objectStores) {
            java.util.Iterator objectStoreIterator = objectStores.values().iterator();
            while (objectStoreIterator.hasNext()) {
                try {
                    ObjectStore objectStore = (ObjectStore) objectStoreIterator.next();
                    objectStore.close();
                } catch (Exception exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:1334:1.62");
                    exceptionCaughtDuringShutdown = exception;
                } // try.
            } // While objectStoreIterator.hasNext().
        } // synchronized (objectStores).

        // Close the logger.
        try {
            if (logOutput != null) {
                logOutput.close();
                logOutput = null;
            } // if (logOutput != null).
        } catch (Exception exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:1352:1.62");
            exceptionCaughtDuringShutdown = exception;
        } // try.

        // Close the log file and realease the lock on it.
        try {
            if (logLock != null)
                logLock.release();
            if (logFile != null)
                logFile.close();
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:1368:1.62");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           "via PermanentIOException");
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        synchronized (ObjectManager.objectManagerStates) {
            ObjectManager.objectManagerStates.remove(logFileName);
        } // synchronized (ObjectManager.objectManagerStates).

        // Help garbage collection.
        registeredInternalTransactions.clear();
        registeredInternalTransactions = null;

        // Let any waiters know we are closed for business.
        synchronized (this) {
            notifyAll();
        }

        trace.debug(cclass, methodName, "ObjectManager using logFile " + logFileName + " has shut down.");

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName
                       , new Object[] { exceptionCaughtDuringShutdown });
        if (exceptionCaughtDuringShutdown != null)
            throw new UnexpectedExceptionException(this,
                                                   exceptionCaughtDuringShutdown);
    } // shutdown().

    /**
     * A non blocking request to shutdown the ObjectManager.
     * Used by helper threads when they encounter a fatal error.
     */
    protected void requestShutdown()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "shutdown");

        new ShutdownHelper();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "requestShutdown");
    } // requestShutdown().

    /**
     * Terminates the ObjectManagerState, without taking a checkpoint.
     * The allows the ObjectManager to be restarted as if it had crashed.
     * Used for testing emergency restart and also for performing an emergency shutdown
     * if a fatal error has been detected.
     * This form of shutdown continues even if it encounters errors.
     * 
     * @throws ObjectManagerException
     */
    protected final void shutdownFast()
                    throws ObjectManagerException
    {
        final String methodName = "shutdownFast";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        Exception exceptionCaughtDuringShutdown = null;

        if (!inShutdown())
        {
            synchronized (this) {
                // Make the state change.
                setState(ObjectManagerState.nextStateForStartShutdown);
            } // synchronized (this).

            // Defect 496893
            // Call our event callbacks at this point as we are no 
            // longer open for business.
            notifyCallbacks(ObjectManagerEventCallback.objectManagerStopped, null);

            // Terminate the checkpoint thread.
            try {
                if (checkpointHelper != null) {
                    checkpointHelper.shutdown();
                    checkpointHelper = null;
                } // if (checkpointHelper != null).
            } catch (Exception exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    exception,
                                                    "1:1470:1.62");
                exceptionCaughtDuringShutdown = exception;
            } // try.

            // Now that the CheckpointHelper has stopped we can move to stopped state
            // knowing that the ObjectStores will not be flushed while we are in stopped state. 

            synchronized (this) {
                // Make the state change.
                setState(ObjectManagerState.nextStateForShutdown);
            } // synchronized (this).

            // Close all of the ObjecStores.
            synchronized (objectStores) {
                java.util.Iterator objectStoreIterator = objectStores.values().iterator();
                while (objectStoreIterator.hasNext()) {
                    try {
                        ObjectStore objectStore = (ObjectStore) objectStoreIterator.next();
                        objectStore.close();
                    } catch (Exception exception) {
                        // No FFDC Code Needed.
                        ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:1491:1.62");
                        exceptionCaughtDuringShutdown = exception;
                    } // try.
                } // while objectStoreIterator.hasNext().
            } // synchronized (objectStores).

            // Close the logger.
            try {
                if (logOutput != null) {
                    logOutput.close();
                    logOutput = null;
                } // if (logOutput != null).
            } catch (Exception exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    exception,
                                                    "1:1509:1.62");
                exceptionCaughtDuringShutdown = exception;
            } // try.

            try {
                if (logLock != null)
                    logLock.release();
                if (logFile != null)
                    logFile.close();
            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    exception,
                                                    "1:1524:1.62");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               "PermanentIOException:1530");
                throw new PermanentIOException(this,
                                               exception);
            } // catch (java.io.IOException exception).

            synchronized (ObjectManager.objectManagerStates) {
                ObjectManager.objectManagerStates.remove(logFileName);
            } // synchronized (ObjectManager.objectManagerStates).

            // Let any waiters know we are ready for business.
            synchronized (this) {
                notifyAll();
            }
        }// end of if(!inshutdown)

        trace.debug(cclass, methodName, "ObjectManager using logFile " + logFileName + " has shut down without performing a final checkpoint");

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { exceptionCaughtDuringShutdown });
        if (exceptionCaughtDuringShutdown != null)
            throw new UnexpectedExceptionException(this,
                                                   exceptionCaughtDuringShutdown);
    } // shutdownFast().

    /**
     * Recognise the existence of an ObjectStore used by this ObjectManager.
     * It is possible to move an objectStore to another ObjectManager and to change its
     * name. To move or rename an ObjectStore deRegister it then Create a new ObjectStore
     * with the same file moved to its new location, or with a different ObjectManager.
     * This will work so long as the ObjectStore identifier is not in use in the
     * new ObjectManager. Be aware that if the store is added to another ObjectManager
     * which had previously used the ObjectStore identifier there may be phantom
     * references to the Objects in the moved store. Also there may be objects in
     * the moved store which are unreachable because they are referenced by objects
     * in the old ObjectManager.
     * 
     * @param objectStore to be registered with this ObjectManager. The name must be currently unknown in the
     *            ObjectManager.
     * @throws ObjectManagerException, with no change made.
     */
    protected void registerObjectStore(ObjectStore objectStore)
                    throws ObjectManagerException {
        final String methodName = "registerObjectStore";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { objectStore });

        synchronized (objectStores) {

            ObjectStore existingObjectStore = getObjectStoreByName(objectStore.getName());
            if (existingObjectStore != null) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { "DuplicateObjectStoreNameException:1594",
                                             objectStore.getName() });
                throw new DuplicateObjectStoreNameException(this,
                                                            objectStore.getName(),
                                                            existingObjectStore);
            } // if (existingObjectStore != null).

            objectStore.open(this); // Now ready for use.

            // If this a new ObjectStore, as opposed to one that has been recovered from a serialized version
            // in a checkpoint or from a file, then set its identifier.
            if (objectStore.getIdentifier() == ObjectStore.IDENTIFIER_NOT_SET) {
                // Allocate a new Identifier for the Object Store, which we know is unique.
                maximumObjectStoreIdentifier++;
                objectStore.setIdentifier((int) maximumObjectStoreIdentifier);
            } else {

                // The identifier is already set. Presumably because this is an old store being added to the
                // ObjectManager again. Check that its identifier is not already in use and move the
                // maximumObjectStoreIdentifier past this one.
                existingObjectStore = (ObjectStore) objectStores.get(new Integer(objectStore.getIdentifier()));
                if (existingObjectStore != null) {
                    objectStore.close();
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   methodName,
                                   new Object[] { "DuplicateObjectStoreNameException:1621",
                                                 objectStore.getName() });
                    throw new DuplicateObjectStoreIdentifierException(this,
                                                                      objectStore.getName(),
                                                                      objectStore.getIdentifier(),
                                                                      existingObjectStore);

                } // if duplicate identifier.
                maximumObjectStoreIdentifier = Math.max(maximumObjectStoreIdentifier, objectStore.getIdentifier());
            } // if (objectStore.getIdentifier() == ObjectStore.IDENTIFIER_NOT_SET).

            Transaction transaction = getTransaction();
            transaction.lock(this);

            // We are about to change the ObjectManagerState so we will need to compensate for any failure
            // if we fail before we commit the transaction. We don't worry abut the mamximumObjectStoreIdentfier
            // because it has been allocated to the store and could legitimately be used by it if the registration
            // is retried later but before the ObjectManager shuts down.
            try {
                objectStores.put(new Integer(objectStore.getIdentifier()),
                                 objectStore);
                // We may have previously added ObjectManagerState and we set ourselves to state ready at construction time
                // so we replace, rather than add.
                transaction.replace(this);

                // We can now use the ObjectStore, we can also use it during recovery because the replace operation
                // will have made it available again.

                // It the ObjectStore holds restart data,
                // create or reset the namedObjects map.
                Token namedObjectsToken = null;
                if (objectStore.getContainsRestartData()) {
                    TreeMap namedObjectsTree = getNamedObjects(objectStore, transaction);
                    namedObjectsToken = namedObjectsTree.getToken();
                    if (namedObjects != null) {
                        // Sync up what we found with what we have.
                        namedObjectsTree.clear(transaction);
                        namedObjectsTree.putAll((TreeMap) namedObjects.getManagedObject(), transaction);
                    } // if (namedObjects != null).

                } // if (objectStore.getContainsRestartData()).

                // Also save backup copies of ObjectManagerstate.
                saveClonedState(transaction);
                // Adjust the size of the space reserved for a CheckpointStartLogRecord. If this fails
                // the reference to the transaction will be found by the garbage collector and backed out.
                long newCheckpointStartLogRecordSize = ((new CheckpointStartLogRecord(this)).getBytesLeft() / FileLogOutput.pageSize + 1) * FileLogOutput.pageSize;
                logOutput.reserve(newCheckpointStartLogRecordSize - checkpointStartLogRecordSize);
                checkpointStartLogRecordSize = newCheckpointStartLogRecordSize;

                transaction.commit(false);

                // Now all the updates have been done make any new namedObjects visible.
                if (namedObjects == null)
                    namedObjects = namedObjectsToken;

            } catch (ObjectManagerException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    exception,
                                                    "1:1683:1.62");
                objectStores.remove(new Integer(objectStore.getIdentifier()));
                transaction.backout(false);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Object[] { exception });
                throw exception;
            } // try...

        } // synchronized (objectStores).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // registerObjectStore().

    /**
     * Recognise an ObjectStore by a new logical name at the next start of the ObjectManager.
     * 
     * @param objectStore to be renamed.
     * @param newName of the store.
     * @throws ObjectManagerException
     */
    protected void renameObjectStore(ObjectStore objectStore, String newName)
                    throws ObjectManagerException {
        final String methodName = "renameObjectStore";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { objectStore, newName });

        synchronized (objectStores) {
            // Is this store registered, if not there is nothing to do?
            ObjectStore existingObjectStore = getObjectStoreByName(objectStore.getName());
            if (existingObjectStore == null) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { "Store not registered:1724" });
                return;
            }

            // Is the newName currently in use?
            existingObjectStore = getObjectStoreByName(newName);
            if (existingObjectStore != null) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { "DuplicateObjectStoreNameException:1735",
                                             newName,
                                             objectStore.getName() });
                throw new DuplicateObjectStoreNameException(this,
                                                            newName,
                                                            existingObjectStore);
            } // if (existingObjectStore != null).

            Transaction transaction = getTransaction();
            transaction.lock(this);

            String oldName = objectStore.getName();
            // We are about to change the ObjectManagerState so we will need to compensate for any failure
            // if we fail before we commit the transaction. We don't worry abut the mamximumObjectStoreIdentfier
            // because it has been allocated to the store and could legitimately be used by it if the registration
            // is retried later but before the ObjectManager shuts down.
            try {
                objectStore.setLogicalName(newName);
                // We may have previously added ObjectManagerState and we set ourselves to state ready at construction time
                // so we replace, rather than add.
                transaction.replace(this);

                // Also save backup copies of ObjectManagerstate.
                saveClonedState(transaction);
                // Adjust the size of the space reserved for a CheckpointStartLogRecord. If this fails
                // the reference to the transaction will be found by the garbage collector and backed out.
                long newCheckpointStartLogRecordSize = ((new CheckpointStartLogRecord(this)).getBytesLeft() / FileLogOutput.pageSize + 1) * FileLogOutput.pageSize;
                logOutput.reserve(newCheckpointStartLogRecordSize - checkpointStartLogRecordSize);
                checkpointStartLogRecordSize = newCheckpointStartLogRecordSize;

                transaction.commit(false);

            } catch (ObjectManagerException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    exception,
                                                    "1:1773:1.62");
                if (objectStore instanceof AbstractObjectStore)
                    ((AbstractObjectStore) objectStore).storeName = oldName;
                transaction.backout(false);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Object[] { exception });
                throw exception;
            } // try...
        } // synchronized (objectStores).

        // Force a full checkpoint so that the caller can be sure that the log will start from
        // a checkpoint containing the new name.
        waitForCheckpoint(true);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // renameObjectStore().

    /**
     * Locate the namedObjects tree in an Object Store and create it if necessary.
     * 
     * @param objectStore which should contain the namedObjects tree.
     * @param transaction controlling any creation.
     * @return TreeMap the NamedObjects.
     * @throws ObjectManagerException
     */
    private final TreeMap getNamedObjects(ObjectStore objectStore,
                                          Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getNamedObjects",
                        new Object[] { objectStore, transaction });

        //  Locate any existing copy.
        Token namedObjectsToken = new Token(objectStore,
                                            ObjectStore.namedObjectTreeIdentifier.longValue());
        // Swap for the definitive Token, if there is one.
        namedObjectsToken = objectStore.like(namedObjectsToken);
        TreeMap namedObjectsTree = (TreeMap) namedObjectsToken.getManagedObject();

        // First time through make a TreeMap.
        if (namedObjectsTree == null) {
            namedObjectsTree = new TreeMap();
            namedObjectsToken.setManagedObject(namedObjectsTree);
            namedObjectsTree.reserveAndAdd(transaction, objectStore);
        } // if (namedObjectsTree == null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getNamedObjects",
                       new Object[] { namedObjectsTree });
        return namedObjectsTree;
    } // getNamedObjects().

    /**
     * Cease to recognise the existence of an ObjectStore used by this ObjectManager.
     * 
     * @param objectStore to be deRegistered from this ObjectManager.
     *            The name must be currently known in the ObjectManager.
     * @throws ObjectManagerException
     */
    protected final void deRegisterObjectStore(ObjectStore objectStore)
                    throws ObjectManagerException {
        final String methodName = "deRegisterObjectStore";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { objectStore });

        // Flush out as much data from the store as we can. If there are any in flight
        // transactions their updates will not get applied to the store after now.
        waitForCheckpoint(objectStore.getPersistence());

        synchronized (objectStores) {
            Transaction transaction = getTransaction();
            transaction.lock(this);

            // See if the ObjectStore contains the nameObjects Tree before we close it
            // and destroy the namedObjects ManagedObject.
            if (namedObjects != null) {
                if (namedObjects.getObjectStore() == objectStore) {
                    namedObjects = null;
                } // if (namedObjects.getObjectStore()...
            } // if (namedObjects != null).

            objectStore.close();
            objectStores.remove(new Integer(objectStore.getIdentifier()));

            transaction.replace(this);

            saveClonedState(transaction);

            // Adjust the size of the space reserved for a CheckpointStartLogRecord. If this fails
            // the reference to the transaction will be found by the garbage collector and backed out.
            long newCheckpointStartLogRecordSize = ((new CheckpointStartLogRecord(this)).getBytesLeft() / FileLogOutput.pageSize + 1) * FileLogOutput.pageSize;
            logOutput.reserve(newCheckpointStartLogRecordSize - checkpointStartLogRecordSize);
            checkpointStartLogRecordSize = newCheckpointStartLogRecordSize;
            transaction.commit(false);

            // Look for a new definitive copy of the namedObjects Tree, if necessary.
            setDefinitiveNamedObjects();
        } // synchronized (objectStores).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // deRegisterObjectStore().

    /**
     * Establish the definitive namedObjects tree in namedObjects, if not already set.
     * Caller must be synchronised on objectStores.
     */
    private void setDefinitiveNamedObjects()
    {
        for (java.util.Iterator objectStoreIterator = objectStores.values().iterator(); objectStoreIterator.hasNext() && namedObjects == null;) {
            ObjectStore objectStore = (ObjectStore) objectStoreIterator.next();

            if (objectStore.getContainsRestartData()) {
                // Locate any existing copy.
                Token namedObjectsToken = new Token(objectStore,
                                                    ObjectStore.namedObjectTreeIdentifier.longValue());
                // Swap for the definitive Token, if there is one.
                namedObjects = objectStore.like(namedObjectsToken);
            } // if (objectStore.getContainsRestartData()).
        } // for ( ObjectStores...
    } // setDefinitiveNamedObjects().

    /**
     * Locate an ObjectStore used by this objectManager.
     * 
     * @param objectStoreIdentifier The unique identifier for the ObjectStore.
     * @return ObjectStore found matching the identifier.
     * @throws ObjectManagerException
     */
    protected ObjectStore getObjectStore(int objectStoreIdentifier)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getObjectStore",
                        new Integer(objectStoreIdentifier));

        ObjectStore objectStore = null; // ObjectStore to return.
        if (objectStoreIdentifier == defaultObjectStoreIdentifier)
            objectStore = defaultObjectStore;
        else {
            synchronized (objectStores) {
                objectStore = (ObjectStore) objectStores.get(new Integer(objectStoreIdentifier));
                if (objectStore == null) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "getOjectStore",
                                   "via NonEexistantObjectStoreException"
                                                   + " objectStoreIdentifier="
                                                   + objectStoreIdentifier
                                                   + "(int)");
                    throw new NonExistentObjectStoreException(this,
                                                              objectStoreIdentifier);
                } // if(objectStore == null).
            } // synchronized (objectStores).
        } // if (objectStoreIdentifier == defaultObjectStoreIdentifier).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getObjectStore",
                       objectStore);
        return objectStore;
    } // getObjectStore().

    /**
     * Locate an ObjectStore used by this objectManager.
     * 
     * @param objectStoreName which is the name of the ObjectStore.
     * @return ObjectStore found matching the identifier.
     * @throws ObjectManagerException
     */
    protected final ObjectStore getObjectStore(String objectStoreName)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getObjectStore",
                        "objectStoreName=" + objectStoreName + "(String)");

        ObjectStore objectStore = null; // ObjectStore to return.
        synchronized (objectStores) {
            objectStore = getObjectStoreByName(objectStoreName);
        } // synchronized (objectStores).

        if (objectStore == null) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "getObjectStore",
                           "via UnknownObjectStoreException" + " objectStoreName=" + objectStoreName + "(String)");
            throw new UnknownObjectStoreException(this,
                                                  objectStoreName);
        } // if(objectStore == null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getObjectStore",
                       "returns objectStore=" + objectStore + "(ObjectStore)");
        return objectStore;
    } // End of method getObjectStore.

    /**
     * Locate an ObjectStore used by this objectManager by comparing its name.
     * The caller must already be synchronized on objectManagerstate.objectStores.
     * 
     * @param objectStoreName which is the name of the ObjectStore.
     * @return ObjectStore found matching the identifier or null if there is none.
     * @throws ObjectManagerException
     */
    private final ObjectStore getObjectStoreByName(String objectStoreName)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getObjectStoreByName",
                        new String(objectStoreName));

        ObjectStore objectStore = null; // ObjectStore to return.
        // Look for an existing ObjectStore of the same name.
        // We dont keep an index by name as this is only used infrequently and there
        // usually are only a few ObjectStores.
        java.util.Iterator objectStoreIterator = objectStores.values()
                        .iterator();
        while (objectStoreIterator.hasNext() && objectStore == null) {
            ObjectStore existingObjectStore = (ObjectStore) objectStoreIterator.next();
            if (existingObjectStore.getName()
                            .equals(objectStoreName)) {
                objectStore = existingObjectStore;
            } // if (existingObjectStore.getName...
        } // While objectStoreIterator.hasNext().

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getObjectStoreByName",
                       "returns objectStore=" + objectStore + "(ObjectStore)");
        return objectStore;
    } // End of method getObjectStoreByName.

    /**
     * Create an iterator over all ObjectStores known to this ObjectManager.
     * 
     * @return java.util.Iterator over ObjectStores.
     * @throws ObjectManagerException
     */
    final java.util.Iterator getObjectStoreIterator()
                    throws ObjectManagerException {
        final String methodName = "getObjectStoreIterator";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName);

        // make a copy of the objecStores map because the caller cannot synchronise on it.
        java.util.ArrayList storesList = new java.util.ArrayList(objectStores.values());
        java.util.Iterator objectStoreIterator = storesList.iterator();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, objectStoreIterator);
        return objectStoreIterator;
    } // getObjectStoreIterator().

    /**
     * Factory method to create or reuse a transaction for use with the ObjectManager.
     * 
     * To detect orphaned transactions, ie. transactions not referenced elsewhere in the JVM. we will keep a reference to
     * the internalTransaction and make a weak reference to the container we pass to the application. When the container
     * Transaction appears on the orphanTransactionsQueue we know the calling application has lost all references to it.
     * At this point we can attempt to reuse it.
     * 
     * @return Transaction the new transaction.
     * @throws ObjectManagerException
     */
    protected final Transaction getTransaction()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getTransaction");

        InternalTransaction internalTransaction = (InternalTransaction) freeTransactions.removeOne();

        if (internalTransaction != null) {
            registerTransaction(internalTransaction);
            if (gatherStatistics)
                reusedTransactions++;

        } else {
            // We need to make a new InternalTransaction.
            synchronized (registeredInternalTransactions) {
                if (totalTransactions >= maximumActiveTransactions) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "getTransaction",
                                   new Object[] { new Long(totalTransactions),
                                                 new Long(maximumActiveTransactions) });
                    throw new TooManyTransactionsException(this,
                                                           maximumActiveTransactions);
                } // if (totalTransactions >= maximumActiveTransactions)

                if (totalTransactions >= currentMaximumActiveTransactions) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "getTransaction",
                                   new Object[] { new Long(totalTransactions),
                                                 new Long(currentMaximumActiveTransactions) });
                    throw new TransactionCapacityExceededException(this,
                                                                   totalTransactions,
                                                                   currentMaximumActiveTransactions);
                } // if (totalTransactions >= currentMaximumActiveTransactions)

                // Make a new transaction.
                internalTransaction = new InternalTransaction(this,
                                                              new LogicalUnitOfWork(++maximumLogicalUnitOfWorkIdentifier));
                totalTransactions++;
            } // synchronized (registeredInternalTransactions).
        } // if (internalTransaction != null).

        Transaction transaction = internalTransaction.getExternalTransaction();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getTransaction",
                       transaction);
        return transaction;
    } // getTransaction().

    /**
     * Recognise the existance of a transaction involving this objectManager.
     * 
     * @param internalTransaction to be registered.
     * @throws ObjectManagerException
     */
    protected final void registerTransaction(InternalTransaction internalTransaction)
                    throws ObjectManagerException {
        final String methodName = "registerTransaction";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, internalTransaction);

        LogicalUnitOfWork logicalUnitOfWork = internalTransaction.getLogicalUnitOfWork();
        internalTransaction.registeredTick = System.currentTimeMillis();  // for orphan diagnostics
        internalTransaction.registeredTid = Thread.currentThread().getId();  // for orphan diagnostics
        internalTransaction.registeredName = Thread.currentThread().getName();  // for orphan diagnostics
        InternalTransaction registeredTransaction = (InternalTransaction) registeredInternalTransactions.put(new Long(logicalUnitOfWork.identifier),
                                                                                                             internalTransaction);
        if (registeredTransaction != null) {
            // Make the duplicate transaction useless.
            registeredTransaction.terminate(Transaction.terminatedDuplicate);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { internalTransaction,
                                                                   registeredTransaction });
            throw new DuplicateTransactionException(this, internalTransaction, registeredTransaction);
        } // if(registeredTransaction != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // registerTransaction().

    /**
     * Cease to recognise the existance of an InternalTransaction involving this objectManager. Hold it ready for reuse.
     * 
     * @param internalTransaction to be deregistered.
     * @throws ObjectManagerException
     */
    protected final void deRegisterTransaction(InternalTransaction internalTransaction)
                    throws ObjectManagerException
    {
        final String methodName = "deRegisterTransaction";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { internalTransaction });

        LogicalUnitOfWork logicalUnitOfWork = internalTransaction.getLogicalUnitOfWork();
        InternalTransaction registeredTransaction = (InternalTransaction) registeredInternalTransactions.remove(new Long(logicalUnitOfWork.identifier));

        if (registeredTransaction == null) {
            // Make the duplicate transaction useless.
            internalTransaction.terminate(Transaction.terminatedDuplicate);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { "via NonExistantTransactionException", logicalUnitOfWork });
            throw new NonExistentTransactionException(this,
                                                      internalTransaction);
        } // if(registeredTransaction == null).

        // Make the InternalTransaction available for reuse.
        internalTransaction.freeTick = System.currentTimeMillis();  // for orphan diagnostics
        internalTransaction.freeTid = Thread.currentThread().getId();  // for orphan diagnostics
        internalTransaction.freeName = Thread.currentThread().getName();  // for orphan diagnostics
        InternalTransaction freeTransaction = (InternalTransaction) freeTransactions.put(new Long(logicalUnitOfWork.identifier),
                                                                                         internalTransaction);
        if (freeTransaction != null) {
            // Make the duplicate transaction useless.
            internalTransaction.terminate(Transaction.terminatedDuplicate);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { internalTransaction, freeTransaction }
                                );
            throw new DuplicateTransactionException(this,
                                                    internalTransaction,
                                                    freeTransaction);
        } // if(freeTransaction != null).
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // deRegisterTransaction().

    /**
     * Notifies the ObjectManagerState that a transaction has completed all its activity
     * for one LogicalUnitOfWork. Here we decide whether or not to trigger a checkpoint.
     * 
     * @param internalTransaction which completed its work.
     * @param persistentWorkDone true is any persistent work was done.
     * @throws ObjectManagerException
     */
    protected final void transactionCompleted(InternalTransaction internalTransaction,
                                              boolean persistentWorkDone)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "transactionCompleted",
                        new Object[] { internalTransaction, new Boolean(persistentWorkDone) });

        // Bump count of transactions.
        if (persistentWorkDone) {
            persistentTransactionsSinceLastCheckpoint++;
            // has the threshold been breached
            if (persistentTransactionsSinceLastCheckpoint > persistentTransactionsPerCheckpoint) {
                suggestCheckpoint(true);
            }
        }
        else {
            nonPersistentTransactionsSinceLastCheckpoint++;
            // has the threshold been breached 
            if (nonPersistentTransactionsSinceLastCheckpoint > nonPersistentTransactionsPerCheckpoint) {
                suggestCheckpoint(false);
            }
        } // if (persistentWorkDone)

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "transactionCompleted");
    } // transactionCompleted().

    /**
     * Delays return if we need to delay the allocation of new transactions while the checkpoint
     * helper catches up.
     * 
     * @throws ObjectManagerException
     */
    protected final void transactionPacing()
                    throws ObjectManagerException
    {
        final String methodName = "transactionPacing";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        // Make sure logOutput exists because during shutdown we set it to null.
        LogOutput localLogOutput = logOutput;
        if (localLogOutput == null) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { new Integer(state),
                                                                   stateNames[state] });
            throw new InvalidStateException(this, state, stateNames[state]);
        } // if (localLogOutput == null).

        // If the log is full, introduce a delay to allow checkpoint to run.
        // We assume that if we let the application thread run immediately it would do
        // more persistent work. This approach is better than penalising all threads, including
        // those that do non persistent work when transactions are reused or getTransaction
        // is called, however it does also penalise threads that do no more persistent work.
        // Only block transactions that can wait, ones invoked by the ckeckpointThread would block if we
        // waited for a checkpoint to complete.
        if (localLogOutput.isOcupancyHigh()) {
            if (gatherStatistics)
                transactionsDelayedForLogFull++;

            // Behave like persistent work was done.
            waitForCheckpoint(true);

//      Object lock = new Object();
//      synchronized (lock) {
//        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
//          trace.debug(this,
//                     cclass,
//                     "transactionCompleted",
//                     "About to wait for "+ checkpointDelay + " milliseconds."
//                    );
//        try {
//          // Let some transactions complete.
//          // We need to wait for longer than the time it takes for a checkpoint to complete.
//          lock.wait(checkpointDelay*2);
//        } catch (InterruptedException exception) {
//          // No FFDC Code Needed.
//          ObjectManager.ffdc.processException(this,
//                                cclass,
//                                "transactionCompleted",
//                                exception,
//                                "1:2295:1.62");
//
//          if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//            trace.exit(this,
//                       cclass,
//                       "transactionCompleted",
//                       exception);
//          throw new UnexpectedExceptionException(this,
//                                                 exception);
//        } // catch (InterruptedException exception).
//      } // synchronized (lock).
        } // if (locaLogOutput.isOcupancyHigh()).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // transactionPacing().

    /**
     * Locate a transaction registered with this objectManager. If the transaction does not already exists then create it.
     * If there is a new XID in the LogicalUnitOfWork assign it to the transaction. Only called during recovery so the
     * external Transaction returned is not registered.
     * 
     * @param logicalUnitOfWork identifying the transaction.
     * @return Transaction identified by the LogicalUnitOfWork.
     * @throws ObjectManagerException
     */
    protected final Transaction getTransaction(LogicalUnitOfWork logicalUnitOfWork)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getTransaction",
                        new Object[] { logicalUnitOfWork });

        InternalTransaction internalTransaction = null;
        // Should not be neccesary to synchronize as we are only called during recovery, which is single threaded.
        internalTransaction = (InternalTransaction) registeredInternalTransactions.get(new Long(logicalUnitOfWork.identifier));
        // If we found an InternalTransaction, keep its LogicalUnitOfWork and abandon the one we were given.

        if (internalTransaction == null) {
            // Have a look in the freeTransactions.
            internalTransaction = (InternalTransaction) freeTransactions.remove(new Long(logicalUnitOfWork.identifier));
            if (internalTransaction != null)
                registerTransaction(internalTransaction);
        } // if(internalTransaction == null).

        if (internalTransaction == null) {
            // Make sure we not reuse this logical unit of work identifier again.
            maximumLogicalUnitOfWorkIdentifier = Math.max(logicalUnitOfWork.identifier,
                                                          maximumLogicalUnitOfWorkIdentifier);
            // Create an InternalTransaction to use the LogicaUnitOfWork we were given.
            internalTransaction = new InternalTransaction(this,
                                                          logicalUnitOfWork);

        } else { // pre existing transaction found.
            // See if we have seen the transactions XID for the first time?
            if (logicalUnitOfWork.XID != null && internalTransaction.getXID() == null)
                // No check to see if the XID's are different.
                internalTransaction.setXID(logicalUnitOfWork.XID);
        } // if(internalTransaction == null).

        // Transaction to return.
        Transaction transaction = new Transaction(internalTransaction);
        // Not transaction = getExternalTransaction(), because this is only used internally
        // at restart by logRecords. If we used getExternalTransaction() it would make weak
        // references that would be found by the garbage collector causing it to BackOut
        // the transaction befpore recovery is completed.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getTransaction",
                       transaction);
        return transaction;
    } // getTransaction().

    /**
     * Locate a transaction registered with this objectManager. with the same XID as the one passed. If a null XID is
     * passed this will return any registered transaction with a null XID.
     * 
     * @param XID Xopen identifier.
     * @return Transaction identified by the XID.
     * @throws ObjectManagerException
     */
    protected final Transaction getTransactionByXID(byte[] XID)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "getTransactionByXID");
            trace.bytes(this,
                        cclass,
                        XID);
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).

        Transaction externalTransaction = null; // Transaction to return.

        // Look at all registeredInternalTransactions.
        for (java.util.Iterator transactionIterator = registeredInternalTransactions.values()
                        .iterator(); transactionIterator.hasNext();) {
            InternalTransaction internalTransaction = (InternalTransaction) transactionIterator.next();
            byte[] transactionXID = internalTransaction.getXID();
            if (java.util.Arrays.equals(transactionXID,
                                        XID)) {
                externalTransaction = internalTransaction.getExternalTransaction();
                break;
            } // if (Arrays.equals(transactionsXID,XID)
        } // for... registeredInternalTransactions.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getTransactionByXID",
                       "returns externalTransaction=" + externalTransaction + "(Transaction)");
        return externalTransaction;
    } // End of method getTransactionByXID.

    /**
     * Create an iterator over all transactions known to this ObjectManager. The iterator returned is safe against
     * concurrent modification of the set of transactions, new transactions created after the iterator is created may not
     * be covered by the iterator.
     * 
     * @return java.util.Iterator over Transactions.
     * @throws ObjectManagerException
     */
    protected final java.util.Iterator getTransactionIterator()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getTransactionIterator");

        java.util.Iterator transactionIterator = new TransactionIterator();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getTransactionIterator",
                       transactionIterator);
        return transactionIterator;
    } // getTransactionIterator().

    /**
     * Find and complete transactions that are no longer addressable. Top up the freeTransactions pool.
     * 
     * @throws ObjectManagerException
     */
    private void completeOrphanTransactions()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "completeOrphanTransactions");

        // Find a reference that has become unreachable,
        // because the external Transaction that owns it is itself unreachable.
        InternalTransaction.TransactionReference transactionReference =
                                                      (InternalTransaction.TransactionReference) orphanTransactionsQueue.poll();
        while (transactionReference != null)
        {
            // Try determine if this is still a registered transaction so we can dump diagnostics if not
            boolean skipComplete = false;
            Long ID = new Long(transactionReference.getLogicalUnitOfWork().identifier);
            if (!registeredInternalTransactions.containsKey(ID))
            {
                // If it is not registered, dump some diagnostics in the form of a warning message (so that it is always produced,
                // even on customer systems where tracing is unlikely to be enabled).  The transaction string contains the primary
                // diagnostics of relative timestamps (tick counts) for when the transaction was last completed, registered, moved
                // to the free list and (roughly) enqueued, along with "now" (for ready reference).  Also a warning and not just
                // something in the FFDC so that we can know about problem orphans that we don't complete too.
                trace.warning(this
                             ,cclass
                             ,"completeOrphanTransactions"
                             ,"InternalTransaction_OrphanDiagnostics"
                             ,new Object[]{transactionReference.getTransactionString()
                                           ,""+System.currentTimeMillis()
                                           ,(freeTransactions.containsKey(ID)?"free":"unknown")
                                          }
                             );
                // skip attempting to complete if it holds no resources, otherwise complete to clean-up and produce
                // an exception / FFDC
                skipComplete = transactionReference.isUnused();
            }
            if (!skipComplete)
            {
                transactionReference.complete();
            }
            transactionReference = (InternalTransaction.TransactionReference) orphanTransactionsQueue.poll();
        } // while( transactionReference != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "completeOrphanTransactions");
    } // completeOrphanTransactions().

    /**
     * Genrates logSequenceNumber for non persistent ManagedObjects that dont write log records.
     * 
     * @return long a logSequenceNumber that is bigger than any returned so far for this run of the ObjectManager.
     * @throws ObjectManagerException
     */
    protected long getDummyLogSequenceNumber()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getDummyLogSequenceNumber");

        long usableLogSequenceNumber;
        // Take a lock on the log and increment the log sequence number.
        synchronized (dummyLogSequenceNumberLock) {
            dummyLogSequenceNumber++; // Set the current Sequence number.
            usableLogSequenceNumber = dummyLogSequenceNumber;
        } // synchronized (dummyLogSequenceNumberLock).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getDummyLogSequenceNumber",
                       new Long(usableLogSequenceNumber));
        return usableLogSequenceNumber;
    } // End of method getDummyLogSequenceNumber.

    /**
     * @return Returns the current transactionUnlockSequence.
     *         Note that this hides the getTransactionUnlockSequence() method in managedObject
     *         to give the global unlock sequence.
     */
    protected final long getGlobalTransactionUnlockSequence()
    {
        synchronized (transactionUnlockSequenceLock) {
            return transactionUnlockSequence;
        } // synchronized (transactionUnlockSequenceLock).
    }

    /**
     * Caller must be synchronized on transactionUnlockSequenceLock.
     * 
     * @return Returns the next transactionUnlockSequence.
     */
    protected final long getNewGlobalTransactionUnlockSequence()
    {
        return ++transactionUnlockSequence;
    }

    /**
     * @param idealSize the ideal Size of the ObjectManagerByteArrayOutputStream requested.
     *            The actual stream returned may have less than this bytes capacity but it can be expanded,
     *            it may have more than this capacity byt it may not be all used.
     * @return ObjectManagerByteArrayOutputStream ready for use.
     */
    final ObjectManagerByteArrayOutputStream getbyteArrayOutputStreamFromPool(int idealSize) {
        int index = (Thread.currentThread().hashCode());
        index = (index >>> 5) % byteArrayOutputStreamPools.length;
        java.util.List byteArrayOutputStreamPool = byteArrayOutputStreamPools[index];

        ObjectManagerByteArrayOutputStream byteArrayOutputStream = null;
        synchronized (byteArrayOutputStreamPool) {
            if (byteArrayOutputStreamPool.isEmpty() || idealSize > maximumByteArrayOutputStreamSize) {
                byteArrayOutputStream = new ObjectManagerByteArrayOutputStream(idealSize);
            } else {
                byteArrayOutputStream = (ObjectManagerByteArrayOutputStream) byteArrayOutputStreamPool.remove(byteArrayOutputStreamPool.size() - 1);
            } // if (byteArrayOutputStreamPool.isEmpty()).
        } // synchronized(byteArrayOutputStreamPool).
        return byteArrayOutputStream;
    } // getbyteArrayOutputStreamFromPool().

    /**
     * @param byteArrayOutputStream to be reset and reused.
     */
    final void returnByteArrayOutputStreamToPool(ObjectManagerByteArrayOutputStream byteArrayOutputStream) {
        int index = (Thread.currentThread().hashCode());
        index = (index >>> 5) % byteArrayOutputStreamPools.length;
        java.util.List byteArrayOutputStreamPool = byteArrayOutputStreamPools[index];

        // Keep small ones back for reuse, discard the large ones.
        if (byteArrayOutputStream.size() <= maximumByteArrayOutputStreamSize) {
            synchronized (byteArrayOutputStreamPool) {
                if (byteArrayOutputStreamPool.size() < byteArrayOutputStreamPoolSize) {
                    byteArrayOutputStream.reset();
                    byteArrayOutputStreamPool.add(byteArrayOutputStream);
                } // if (byteArrayOutputStreamPool.size() < byteArrayOutputStreamPoolSize).
            } // synchronized (byteArrayOutputStreamPool).
        } // if (byteArrayOutputStream.size() < maximumByteArrayOutputStreamSize).
    } // returnByteArrayOutputStreamToPool.

    /*
     * Builds a set of properties containing the current statistics. @return java.util.Map the statistics.
     */
    protected java.util.Map captureStatistics()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "captureStatistics");

        java.util.Map statistics = new java.util.HashMap();

        statistics.put("totalCheckpointsTaken",
                       Long.toString(totalCheckpointsTaken));
        statistics.put("waitingBetweenCheckpointsMilliseconds",
                       Long.toString(waitingBetweenCheckpointsMilliseconds));
        statistics.put("flushingCheckpointStartMilliseconds",
                       Long.toString(flushingCheckpointStartMilliseconds));
        statistics.put("pausedDuringCheckpointMilliseconds",
                       Long.toString(pausedDuringCheckpointMilliseconds));
        statistics.put("flushingObjectStoresForCheckpointMilliseconds",
                       Long.toString(flushingObjectStoresForCheckpointMilliseconds));
        statistics.put("flushingEndCheckpointMilliseconds",
                       Long.toString(flushingEndCheckpointMilliseconds));
        statistics.put("threadsWaitingForCheckpoint",
                       Integer.toString(checkpointHelper.threadsWaitingForCheckpoint));
        statistics.put("currentMaximumActiveTransactions",
                       Long.toString(currentMaximumActiveTransactions));
        statistics.put("registeredInternalTransactions.size()",
                       Integer.toString(registeredInternalTransactions.size()));
        statistics.put("freeTransactions.size()",
                       Integer.toString(freeTransactions.size()));
        statistics.put("totalTransactions",
                       Long.toString(totalTransactions));
        statistics.put("reusedTransactions",
                       Long.toString(reusedTransactions));
        statistics.put("totalTransactionsRequiringCheckpoint",
                       Long.toString(totalTransactionsRequiringCheckpoint));
        statistics.put("maximumTransactionsInAnyCheckpoint",
                       Long.toString(maximumTransactionsInAnyCheckpoint));
        statistics.put("transactionsDelaydForLogFull",
                       Long.toString(transactionsDelayedForLogFull));
        int byteArrayOutputStreamPoolsSize = 0;
        for (int i = 0; i < byteArrayOutputStreamPools.length; i++)
            byteArrayOutputStreamPoolsSize = byteArrayOutputStreamPoolsSize + byteArrayOutputStreamPools[i].size();
        statistics.put("byteArrayOutputStreamPoolsSize",
                       Integer.toString(byteArrayOutputStreamPoolsSize));

        totalCheckpointsTaken = 0;
        waitingBetweenCheckpointsMilliseconds = 0;
        flushingCheckpointStartMilliseconds = 0;
        pausedDuringCheckpointMilliseconds = 0;
        flushingObjectStoresForCheckpointMilliseconds = 0;
        flushingEndCheckpointMilliseconds = 0;
        reusedTransactions = 0;
        totalTransactionsRequiringCheckpoint = 0;
        maximumTransactionsInAnyCheckpoint = 0;
        transactionsDelayedForLogFull = 0;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "captureStatistics",
                       statistics);
        return statistics;
    } // method captureStatistics().

    /*
     * @returns int the current state for the ObjectManagerState.
     */
    protected int getObjectManagerStateState()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getState");

        int stateToReturn = state;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getState",
                       stateToReturn + " " + ObjectManagerState.stateNames[stateToReturn]);
        return stateToReturn;
    } // method getObjectManagerStateState().

    /**
     * Makes a state transition. Call must be single threaded through this.
     * 
     * @param nextState the map of the next state.
     * @throws ObjectManagerException
     */
    void setState(int[] nextState)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "setState",
                        new Object[] { nextState,
                                      new Integer(state),
                                      stateNames[state] });

        previousState = state; // Capture the previous state for dump.
        state = nextState[state]; // Make the state change.

        if (state == stateError) {
            StateErrorException stateErrorException = new StateErrorException(this,
                                                                              previousState,
                                                                              stateNames[previousState]);
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                "setState",
                                                stateErrorException,
                                                "1:2674:1.62");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "setState",
                           new Object[] { stateErrorException,
                                         new Integer(state),
                                         stateNames[state] });
            throw stateErrorException;
        } // if (state == stateError).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "setState",
                       new Object[] { new Integer(state),
                                     stateNames[state] });
    } // setState().

    // --------------------------------------------------------------------------
    // extends ManagedObject.
    // --------------------------------------------------------------------------

    /**
     * Replace the state of this object with the same object in some other state. Used for to restore the before image if
     * a transaction rolls back or is read from the log during restart.
     * 
     * @param other is the object this object is to become a clone of.
     * @throws ObjectManagerException
     */
    public void becomeCloneOf(ManagedObject other)
                    throws ObjectManagerException {
        final String methodName = "becomeCloneOf";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, other);

        ObjectManagerState otherObjectManagerState = (ObjectManagerState) other;

        maximumObjectStoreIdentifier = otherObjectManagerState.maximumObjectStoreIdentifier;
        persistentTransactionsPerCheckpoint = otherObjectManagerState.persistentTransactionsPerCheckpoint;
        nonPersistentTransactionsPerCheckpoint = otherObjectManagerState.nonPersistentTransactionsPerCheckpoint;
        logFullTriggerCheckpointThreshold = otherObjectManagerState.logFullTriggerCheckpointThreshold;
        logFullPostCheckpointThreshold = otherObjectManagerState.logFullPostCheckpointThreshold;
        maximumActiveTransactions = otherObjectManagerState.maximumActiveTransactions;
        checkpointDelay = otherObjectManagerState.checkpointDelay;
        coldStartLogFileName = otherObjectManagerState.coldStartLogFileName;
        coldStartTime = otherObjectManagerState.coldStartTime;

        if (mainObjectManagerState) {
            synchronized (objectStores) {
                mergeObjectStores(otherObjectManagerState);
                setDefinitiveNamedObjects();
            }
        } else
            objectStores = otherObjectManagerState.objectStores;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // becomeCloneOf().

    /**
     * Merge the otherObjectManagerState.objectStores with our own.
     * Caller must be synchronized on objecStores.
     * 
     * 1) New unknown object stores are opend and added to the known objectStores set. 2) Existing oobjectStores are
     * retained, and not replaced with the new one, this allows apps to continue using the ones they may already have
     * reference to. 3) ObjectStores not present in the new set are closed and removed from the objectStores set.
     * 
     * @param otherObjectManagerState
     * @throws ObjectManagerException
     */
    private void mergeObjectStores(ObjectManagerState otherObjectManagerState)
                    throws ObjectManagerException
    {
        // Merge the other ObjectStores, with those already active in this ObjectManagerState.
        // Make sure we have all of the new ObjectStores.
        for (java.util.Iterator objectStoreIterator = otherObjectManagerState.objectStores.values().iterator(); objectStoreIterator.hasNext();) {
            ObjectStore objectStore = (ObjectStore) objectStoreIterator.next();
            // We cannot use objectManagerState.getObjectStore,
            // because it throws NonExistantObjectStoreException.
            Object existingStore = objectStores.get(new Integer(objectStore.getIdentifier()));
            if (existingStore == null) {
                objectStore.open(this);
                objectStores.put(new Integer(objectStore.getIdentifier()),
                                 objectStore);

                // Does the store contain a the definitive copy of the namedObjects Tree?
                if (objectStore.getContainsRestartData()
                    && namedObjects == null) {
                    // We do not need to create the tree if it does not exist because the original transaction
                    // will have taken care of that.
                    Token namedObjectsToken = new Token(objectStore,
                                                        ObjectStore.namedObjectTreeIdentifier.longValue());
                    // Swap for the definitive Token, if there is one.
                    namedObjectsToken = objectStore.like(namedObjectsToken);

                } // if (objectStore.getContainsRestartData())...
            } // if (existingStore == null).
        } // for otherObjectManagerstate.objectStores...

        // Remove any ObjectStores in this objectStores but not in the otherObjectManagerState.objectStores.
        for (java.util.Iterator objectStoreIterator = objectStores.values().iterator(); objectStoreIterator.hasNext();) {
            ObjectStore objectStore = (ObjectStore) objectStoreIterator.next();
            // We cannot use objectManagerState.getObjectStore,
            // because it throws NonExistantObjectStoreException.
            Object newObjectStore = otherObjectManagerState.objectStores.get(new Integer(objectStore.getIdentifier()));
            if (newObjectStore == null) {
                objectStore.close();
                objectStores.remove(new Integer(objectStore.getIdentifier()));
                // No need to modify the namedObjects Tree as that is taken care of by the rest of the transaction.

                // See if the ObjectStore contains the nameObjects Tree?
                if (namedObjects != null) {
                    if (namedObjects.getObjectStore() == objectStore) {
                        namedObjects = null;
                    } // if (namedObjects.getObjectStore()...
                } // if (namedObjects != null).

            } // if (existingStore == null).
        } // for otherObjectManagerstate.objectStores...

        // It is now safe to restore tokens which make reference to their ObjectStores.

    } // mergeObjectStores().

    // --------------------------------------------------------------------------
    // Simplified serialization.
    // --------------------------------------------------------------------------

    /**
     * No argument constructor.
     * 
     * @exception ObjectManagerException
     */
    ObjectManagerState()
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>");

        // We are the not the real state, just a copy as yet.
        mainObjectManagerState = false;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // ObjectManagerState().

    private static final byte simpleSerialVersion = 0;

    /*
     * (non-Javadoc)
     * 
     * @see int SimplifiedSerialization.getSignature()
     */
    protected int getSignature()
    {
        return signature_ObjectManagerState;
    } // getSignature().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.SimplifiedSerialization#estimatedLength()
     */
    public long estimatedLength() {
        return serializedSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see SimplifiedSerialization.writeObject(java.io.DataInputStream)
     */
    public void writeObject(java.io.DataOutputStream dataOutputStream)
                    throws ObjectManagerException {
        final String methodName = "writeObject";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, dataOutputStream);

        try {
            dataOutputStream.writeByte(simpleSerialVersion);
            super.writeObject(dataOutputStream);

            // ObjectStores are serialized first because any references to ManagedObjects will
            // indirectly refer to them.
            //TODO Make ObjecStores managedObjects, store them individually in the restartObjectStores!
            //TODO This lets them use simpleSerialization and any admin changes are transactional.
            java.io.ObjectOutputStream objectOutputStream = new java.io.ObjectOutputStream(dataOutputStream);
            synchronized (objectStores) {
                objectOutputStream.writeObject(objectStores);
            }
            objectOutputStream.close();

            dataOutputStream.writeLong(maximumObjectStoreIdentifier);
            dataOutputStream.writeLong(persistentTransactionsPerCheckpoint);
            dataOutputStream.writeLong(nonPersistentTransactionsPerCheckpoint);
            dataOutputStream.writeFloat(logFullTriggerCheckpointThreshold);
            dataOutputStream.writeFloat(logFullPostCheckpointThreshold);
            dataOutputStream.writeLong(maximumActiveTransactions);
            dataOutputStream.writeInt(checkpointDelay);
            dataOutputStream.writeUTF(coldStartLogFileName);
            dataOutputStream.writeLong(coldStartTime);
            // Note the serialized size ready for the next time we serialize.
            serializedSize = dataOutputStream.size();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:2887:1.62");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // writeObject().

    /*
     * (non-Javadoc)
     * 
     * @see SimplifiedSerialization.readObject(java.io.DataInputStream,ObjectManagerState)
     */
    public void readObject(java.io.DataInputStream dataInputStream,
                           ObjectManagerState objectManagerState)
                    throws ObjectManagerException {
        final String methodName = "readObject";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { dataInputStream,
                                      objectManagerState });

        try {
            byte version = dataInputStream.readByte();
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            methodName,
                            new Byte(version));
            super.readObject(dataInputStream,
                             objectManagerState);

            // There is no transient data in either SingleFileObjectStore or SingleNonNioFileObjectStore, 
            // and the two disk formats are identical. Hence we can flip between the two types of store
            // according to whether we are running on an Nio capable JVM or not.
            // PK73292.1
            // Disable this migration code by default. Allow it to be toggled via a compile
            // time flag.
            java.io.ObjectInputStream objectInputStream;

            if (migrateObjectStores)
            {
                objectInputStream = new java.io.ObjectInputStream(dataInputStream)
                {
                    /*
                     * (non-Javadoc)
                     * 
                     * @see java.io.ObjectInputStream#readClassDescriptor()
                     */
                    protected java.io.ObjectStreamClass readClassDescriptor() throws java.io.IOException, java.lang.ClassNotFoundException
                    {
                        final String methodName = "readClassDescriptor";

                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.entry(this, cclass, methodName);

                        java.io.ObjectStreamClass objectStreamClass = super.readClassDescriptor();
                        if (objectStreamClass.getName().equals("com.ibm.ws.objectManager.SingleFileObjectStore")
                            && !nioAvailable)
                        {
                            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                                trace.debug(this, cclass, methodName, new Object[] { "Migrating:2950", objectStreamClass });

                            // PK73292
                            // use Class.forName() to delay class loading of object store 
                            // implementation until this code path is taken.
                            objectStreamClass = java.io.ObjectStreamClass.lookup(Class.forName("com.ibm.ws.objectManager.SingleNonNioFileObjectStore"));
                        }
                        else if (objectStreamClass.getName().equals("com.ibm.ws.objectManager.SingleNonNioFileObjectStore")
                                 && nioAvailable)
                        {
                            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                                trace.debug(this, cclass, methodName, new Object[] { "Migrating:2960", objectStreamClass });

                            // PK73292
                            // use Class.forName() to delay class loading of object store 
                            // implementation until this code path is taken.
                            objectStreamClass = java.io.ObjectStreamClass.lookup(Class.forName("com.ibm.ws.objectManager.SingleFileObjectStore"));
                        } // if (   objectStreamClass.getName().

                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this, cclass, methodName, new Object[] { objectStreamClass });
                        return objectStreamClass;
                    } // readClassDescriptor().
                };
            }
            else
            {
                objectInputStream = new java.io.ObjectInputStream(dataInputStream);
            }

            objectStores = (java.util.Map) objectInputStream.readObject();
            objectInputStream.close();

            // ObjectStores are merged in becomeCloneOf().

            maximumObjectStoreIdentifier = dataInputStream.readLong();
            persistentTransactionsPerCheckpoint = dataInputStream.readLong();
            nonPersistentTransactionsPerCheckpoint = dataInputStream.readLong();
            logFullTriggerCheckpointThreshold = dataInputStream.readFloat();
            logFullPostCheckpointThreshold = dataInputStream.readFloat();
            maximumActiveTransactions = dataInputStream.readLong();
            checkpointDelay = dataInputStream.readInt();
            coldStartLogFileName = dataInputStream.readUTF();
            dataInputStream.readLong();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:2999:1.62");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { "PermanentIOException:%C", exception });
            throw new PermanentIOException(this,
                                           exception);

        } catch (java.lang.ClassNotFoundException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:3015:1.62");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { "ClassNotFoundException:3021", exception });
            throw new com.ibm.ws.objectManager.ClassNotFoundException(this, exception);

        } // catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // readObject().

    void registerEventCallback(ObjectManagerEventCallback callback) {
        final String methodName = "registerEventCallback";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, callback);

        synchronized (callbacks) {
            callbacks.add(callback);
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // registerEventCallback().

    void notifyCallbacks(int event, Object[] args) {
        final String methodName = "notifyCallbacks";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { new Integer(event), args });

        synchronized (callbacks) {
            for (java.util.Iterator iterator = callbacks.iterator(); iterator.hasNext();) {
                ObjectManagerEventCallback callback = (ObjectManagerEventCallback) iterator.next();
                callback.notification(event, args);
            } // for.        
        } // synchronized (callbacks).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // notifyCallbacks.

    /*
     * Check to see if we are in shutdown mode. This will true if we are
     * either in 'shutdown started' or 'stopped'.
     */
    protected boolean inShutdown() throws ObjectManagerException
    {
        if (getObjectManagerStateState() == stateShutdownStarted ||
            getObjectManagerStateState() == stateStopped)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    //--------------------------------------------------------------------------
    // extends Object.
    // --------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return new String("ObjectManagerState"
                          + "(" + logFileName + ")"
                          + "/" + stateNames[state]
                          + " " + super.toString());
    } // toString().

    // ----------------------------------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------------------------------

    /*
     * 
     * @author Andrew_Banks
     * 
     * Gives up an ExternalTransaction covering the next InternalTransaction.
     */
    private class TransactionIterator
                    implements java.util.Iterator
    {
        java.util.Iterator internalTransactionIterator;

        TransactionIterator()
        {
            internalTransactionIterator = registeredInternalTransactions.values()
                            .iterator();
        }

        public boolean hasNext()
        {
            return internalTransactionIterator.hasNext();
        }

        public Object next()
        {
            InternalTransaction internalTransaction = (InternalTransaction) internalTransactionIterator.next();
            Transaction transaction = internalTransaction.getExternalTransaction();
            return transaction;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    } // inner class TransactionIterator .

    /**
     * The worker thread that executes a checkpoint.
     */
    protected class CheckpointHelper
                    implements Runnable
    {
        private boolean running = true; // True if we are not finished.
        private boolean waiting = false; // True if waiting.
        private boolean checkpointRequested = false; // True if a checkpoint is requested.
        private boolean persistentTransactions = false; // True is persitent work is t be checkpointed.
        private Exception abnormalTerminationException = null;
        private long checkpointCycle = 0; // Identifies the checkpoint.
        protected Thread checkpointThread = null;
        private int threadsWaitingForCheckpoint = 0;

        /**
         * Constructor
         * 
         * @throws ObjectManagerException
         */
        CheckpointHelper()
            throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "<init>");

            checkpointThread = new Thread(this);

            checkpointThread.setName("CheckpointHelper");
            // Run the CheckpointHelper in preference to other threads.
            checkpointThread.setPriority(Thread.NORM_PRIORITY + 1);
            // checkpointThread.setPriority(Thread.MAX_PRIORITY);

            // synchronize to make sure the CheckpointHelper starts before
            // any requests are admited.
            synchronized (this) {
                checkpointThread.start();
                try {
                    wait();
                } catch (InterruptedException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this,
                                                        cclass,
                                                        "waitForCheckpoint",
                                                        exception,
                                                        "1:3180:1.62");

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "waitForCheckpoint",
                                   exception);
                    throw new UnexpectedExceptionException(this,
                                                           exception);

                } // catch (InterruptedException exception).
            } // synchronized (this).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>");
        } // CheckpointHelper().

        // ----------------------------------------------------------------------------------------------
        // implements runnable
        // ----------------------------------------------------------------------------------------------

        public void run()
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "run");

            // Outer try/catch block treats all unexpected errors as terminal
            // and shuts down the Objectmanager if any occur.
            try {
                for (;;) {
                    boolean nextCheckpointPersistentence;
                    synchronized (this) {
                        checkpointCycle++; // Move on to the next cycle.
                        notifyAll(); // Release threads waiting for the checkpoint.
                        threadsWaitingForCheckpoint = 0;

                        if (!running)
                            break;

                        if (!checkpointRequested) { // Should we wait?
                            waiting = true;
                            // InterruptedException is caught by the outer try/catch block.
                            wait();
                            waiting = false;
                        } // if (!checkpointRequested).
                        checkpointRequested = false;
                        nextCheckpointPersistentence = persistentTransactions;
                        persistentTransactions = false;
                    } // synchronized (this).

                    // Perform the checkpoint while not synchronized, this allows new requests
                    // to be made without blocking.
                    // ObjectmanagerdException is caught by the outer try/catch block.
                    if (running) // We might have just shutdown.
                        performCheckpoint(nextCheckpointPersistentence); // Perform the checkpoint.

                } // for (;;).
            } catch (Exception exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    "run",
                                                    exception,
                                                    "1:3247:1.62");

                running = false;
                abnormalTerminationException = exception;
                // Make one asynchronous request to shutdown the ObjectManager.
                requestShutdown();
                synchronized (this) {
                    // We are shutting down now so notify all. The abnormalexception
                    // has been set so this should indicate to the caller that the
                    // checkpoint did fail.
                    notifyAll();
                } // synchronized (this).
            } // catch (ObjectManagerException exception).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "run");
        } // run().

        /**
         * A non blocking request that a checkpoint should be performed.
         * Will return before the checkpoint is complete.
         * 
         * @param persistent true if persistent transactions are to be checkpointed.
         * @throws ObjectManagerException
         */
        synchronized void requestCheckpoint(boolean persistent)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "requestCheckpoint",
                            new Object[] { new Boolean(persistent) });

            // Has something previously gone wrong?
            if (abnormalTerminationException != null) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "requestCheckpoint",
                               abnormalTerminationException);
                throw new UnexpectedExceptionException(this,
                                                       abnormalTerminationException);
            } // if (abnormalTerminationException != null).
              // Any request arriving after we have stopped running, at shutdown will not be honoured.
            checkpointRequested = true;
            persistentTransactions = persistentTransactions || persistent;
            if (waiting) {
                notify();
            } // if (waiting).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "requestCheckpoint");
        } // requestCheckpoint().

        /**
         * A non blocking suggestion that a checkpoint should be performed.
         * Will return before the checkpoint is complete.
         * This is different to requestCheckpoint in that it will only trigger a checkpoint
         * if the checkpointer helper is not already running (if it is sleeping). This is
         * useful to avoid a double checkpoint when the checkpoint is triggered by occupancy
         * or number of transactions.
         * 
         * A small window exists in which callers could really mean that a checkpoint should
         * run based on up-to-date numbers. So callers of this method should not rely on the
         * fact that a checkpoint will happen as a result of this call, in addition to any
         * that is currently in progress. Callers requiring this should use requestCheckpoint
         * or waitForCheckpoint.
         * 
         * It always updates persistentTransactions if appropriate.
         * 
         * @param persistent true if persistent transactions are to be checkpointed.
         * @throws ObjectManagerException
         */
        synchronized void suggestCheckpoint(boolean persistent)
                        throws ObjectManagerException
        {
            if (trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "suggestCheckpoint",
                            new Object[] { new Boolean(persistent) });

            // Has something previously gone wrong?
            if (abnormalTerminationException != null) {
                if (trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "requestCheckpoint",
                               abnormalTerminationException);
                throw new UnexpectedExceptionException(this,
                                                       abnormalTerminationException);
            } // if (abnormalTerminationException != null).

            // Any request arriving after we have stopped running, at shutdown will not be honoured.
            persistentTransactions = persistentTransactions || persistent;
            if (waiting) {
                checkpointRequested = true;
                notify();
            } // if (waiting).

            if (trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "suggestCheckpoint");
        } // suggestCheckpoint().

        /**
         * A blocking call to checkpoint, returns when checkpoint is complete.
         * 
         * @param persistent true if persisent state is to be checkpointed.
         * @throws ObjectManagerException
         */
        private synchronized void waitForCheckpoint(boolean persistent)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "waitForCheckpoint",
                            new Boolean(persistent));

            long endCycle;
            if (waiting) {
                // Currently inactive so wait for then next checkpoint.
                endCycle = checkpointCycle;
            } else {
                // Currently checkpointing, wait for another checkpoint after this one finishes.
                endCycle = checkpointCycle + 1;
            } // if (waiting).

            while (endCycle >= checkpointCycle
                   && abnormalTerminationException == null) {
                requestCheckpoint(persistent);
                threadsWaitingForCheckpoint++;
                try {
                    wait();
                } catch (InterruptedException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this,
                                                        cclass,
                                                        "waitForCheckpoint",
                                                        exception,
                                                        "1:3393:1.62");

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "waitForCheckpoint",
                                   exception);
                    throw new UnexpectedExceptionException(this,
                                                           exception);

                } // catch (InterruptedException exception).
            } // while (startCycle == checkpointCycle).

            // Was there a happy ending?
            if (abnormalTerminationException != null) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "waitForCheckpoint",
                               abnormalTerminationException);
                throw new UnexpectedExceptionException(this,
                                                       abnormalTerminationException);
            } // if (abnormalTerminationException != null).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "waitForCheckpoint");
        } // waitForCheckpoint().

        /*
         * Terminate the CheckpointHelper.
         */
        void shutdown()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "shutdown");

            synchronized (this) {
                running = false;

                if (waiting)
                    notify();
            } // synchronize (this).

            // Wait for the worker thread to complete.
            try {
                checkpointThread.join();
            } catch (InterruptedException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    "shutdown",
                                                    exception,
                                                    "1:3450:1.62");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "shutdown",
                               exception);
                throw new UnexpectedExceptionException(this,
                                                       exception);
            } // catch (InterruptedException exception).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "shutdown");
        } // shutdown().
    } // checkpointHelper().

    /**
     * Handles asynchronous requests to shut down the ObjectManager.
     */
    private class ShutdownHelper
                    implements Runnable
    {
        /**
         * Constructor
         */
        ShutdownHelper()
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "<init>");

            Thread shutdownThread = new Thread(this);
            shutdownThread.setName("ShutdownHelper");
            shutdownThread.start();

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>");
        } // ShutdownHelper().

        // ---------------------------------------------------------------------------------
        // implements Runnable.
        // ---------------------------------------------------------------------------------
        public void run() {
            final String methodName = "run";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            methodName);
            // Make one attempt to shutdown, if anything goes wrong just report the problem
            // but dont attempt any recovery. If we are here it will be because something
            // unexpected has happened and we don't want to make things any worse.
            try {
                // Use ShutdownFast because any attempt to backout Transactions or take a
                // Checkpoint is likely to fail.
                shutdownFast();
            } catch (Exception exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    exception,
                                                    "1:3516:1.62");

            } // catch (InterruptedException exception).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName);
        } // run().
    } // inner class shutdownHelper().
} // ObjectManagerState class.
