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
 * Uses a single direct access file to store serialized objects. The long value
 * in the token is an index into the directory. The Directory holds the disk
 * location of each Managed Object in the store.
 * 
 * The ObjectDirectory consists of a balanced BTree with each entry containing the
 * byte address of the Object and the length of the disk space allocated to it.
 * When the store is flushed the the current set of managedObjectToWrite is captured
 * and space is allocated for each object and an entry inserted into the directory.
 * The new ManagedObjects are written and the new directory areas then space for deleted
 * ManagedObjects and free directory space is released. The remaining freespace
 * addresses are saved previously allocated area on the disk. Final the file header
 * is rewritten pointing to the new root of the directory tree. Hence there is no
 * intermediate state of the store with some objects written but not others.
 * 
 * @version @(#) 1/25/13
 * @author IBM Corporation
 */
public abstract class AbstractSingleFileObjectStore
                extends AbstractObjectStore
{
    private static final Class cclass = AbstractSingleFileObjectStore.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_STORE);

    private static final long serialVersionUID = -1011499273819925485L;

    // Store area for the directory and free space, but these are saved in the log header
    // and not the directory itself.
    protected static final long directoryIdentifier = 101;
    protected static final long freeSpaceIdentifier = 102;
    // Reserve sequence numbers 102-200 for future use.
    protected static final long initialSequenceNumber = 200;

    // Used to check that the file is likely to be a SingleFileObjectStore (38 characters).
    static final String signature = "++ObjectManager.SingleFileObjectStore++";

    // The stream that handles the file we will write Objects into.
    protected transient String storeFileName;
    protected transient java.io.RandomAccessFile storeFile;

    // The ManagedObjects waiting to be written to disk, indexed by the storedObjectIdentifier.
    // We hold the ManagedObject in this map not the Token in order to force it to
    // remain in memory until we have written it. This is volatile
    // because we insert a new set when a checkpoint is started without taking 
    // lock.
    protected transient volatile java.util.Map managedObjectsToWrite;
    // The ammount of new file space needed to write these objects,
    // not including the directoryReservedSize.
    // The reservedSize cannot exceed the JVM address space to a long is sufficient on 
    // a 64 bit JVM.
    // transient java.util.concurrent.atomic.AtomicLong reservedSize;
    transient AtomicXXBitLong reservedSize;

    // On some 32bit JVM's the implementation of java.util.concurrent.atomic.AtomicLong is 
    // not very concurrent so we use an AtomicInteger instead.
    interface AtomicXXBitLong {
        long addAndGet(long longValue);

        long get();

        void set(long longValue);
    }

    class Atomic64BitLong
                    extends com.ibm.ws.objectManager.utils.concurrent.atomic.AtomicLongImpl
                    implements AtomicXXBitLong {
        private static final long serialVersionUID = 1L;

        Atomic64BitLong(long longValue) {
            super(longValue);
        }
    } // class Atomic64BitLong.

    class Atomic32BitLong
                    implements AtomicXXBitLong {
        com.ibm.ws.objectManager.utils.concurrent.atomic.AtomicInteger intValue;

        Atomic32BitLong(long longValue) {
            intValue = new com.ibm.ws.objectManager.utils.concurrent.atomic.AtomicIntegerImpl((int) longValue);
        }

        public final long addAndGet(long longValue) {
            return intValue.addAndGet((int) longValue);
        }

        public final long get() {
            return intValue.get();
        }

        public final void set(long longValue) {
            intValue.set((int) longValue);
        }
    } // class Atomic32BitLong. 

    // We use Integer in preference to Long because some implementations of integer offer better concurrency.

    // Space, included in reservedSize for a complete rewrite of the directory.
    protected transient long directoryReservedSize;
    // The reserved size that causes us to ask for more disk space.
    private transient long reservationThreshold = 0;
    // The point at which we ask for a checkpoint, reservationCheckpointThreshold <= resrvationThreshold.
    private transient long reservationCheckpointThreshold = 0;
    private transient long simulateFullReservedSize = 0;
    private transient boolean reservationPacing = false;
    private transient ReservationPacingLock reservationPacingLock;

    private class ReservationPacingLock {};

    // The maximum ammout of Transaction.add() data than can be added in one checkpoint.
    static final int reservationCheckpointMaximum = 5000000;

    // When a checkpoint becomes active this is the set of ManagedObjects that will be
    // that will be written as part of the checkpoint;  
    protected transient java.util.Map checkpointManagedObjectsToWrite;
    // A cache of ManagedObjects referenced directly so that they remain in storage.
    protected transient java.lang.ref.SoftReference[] cachedManagedObjects;
    protected transient int iCache = 0;
    static final int initialCachedManagedObjectsSize = 10000;
    protected transient int cachedManagedObjectsSize;
    // The Tokens of ManagedObjects waiting to be deleted, indexed by the
    // storedObjectIdentifier.
    protected transient volatile java.util.Map tokensToDelete;
    // When a checkpoint becomes active this is the set of Tokens that will be 
    // deleted as part of the checkpoint.
    protected transient java.util.Map checkpointTokensToDelete;
    protected transient int checkpointReleaseSize;
    // Lock held by object getters while reading the file.
    transient GetLock getLock;

    class GetLock extends com.ibm.ws.objectManager.utils.concurrent.locks.ReentrantReadWriteLockImpl {};

    // Directory of occupied space in the store.
    protected transient Directory directory;

    // Directory of free space in the store.
    // A double-linked list of FreeSpace objects sorted by address, which is unique.
    // Sorting is maintained by virtue of the freeAllocatedSpace method, which inserts items into
    // the list in manner that maintains order (relies on newFreeSpace being sorted).
    // Double-linking is required so that items can be found using the freeSpaceByLength set,
    // and then removed from the list without paying a cost to find the previous entry.
    protected transient FreeSpace freeSpaceByAddressHead;
    // The same set of FreeSpace objects sorted by length then address if the length is not unique.
    protected transient java.util.SortedSet freeSpaceByLength;
    // Where the free space map is currently saved in the file.
    // This store area is never added to the directory as it would disrupt the free space map.
    protected transient Directory.StoreArea freeSpaceStoreArea;
    // Metric to track the largest size the free space entry has grown to
    protected transient int maxFreeSpaceCount;
    // The size of a free space entry when stored on disk.
    // long byte address + long length.
    static final int freeSpaceEntryLength = 16;
    // The smallest size we bother to keep as free space, if we are left with less than this
    // amount we just give it as part of the allocation. Note that it takes
    // freeSpaceEntryLength bytes to keep track of a piece of free space.
    static final int minimumFreeSpaceEntrySize = 17;

    // The StoreAreas that will be free once this flush completes. This is space that is
    // release by replaced Objects, Deleted Objects and the updated pieces of the directory. 
    // PK79872 modifies the algorithm to use a sorted set for this free set, allowing us to
    // perform a single parse of the sorted free space list when merging free space back in.
    protected transient java.util.SortedSet newFreeSpace;
    // Number of bytes used in the store file at the end of the last flush.
    protected transient long storeFileSizeUsed;
    // The allocated size of the store file.
    protected transient long storeFileSizeAllocated;
    // The admistered limits on the ObjectStore file size.
    protected transient long minimumStoreFileSize;
    protected transient long maximumStoreFileSize;

    protected final static int pageSize = 4 * 1024; // Writes end on a 4K page.

    protected final int version = 1;
    // The minimumNodeSize of the Directory BTree when it is created.
    static final int initialDirectoryMinimumNodeSize = 10; // 500;

    // For gatherStatistics.
    // Number of times a ManagedObject is retrieved from the store.
    protected transient int numberOfGetRequests;
    protected transient int numberOfDirectoryNodesRead;
    private transient int numberOfStoreFullCheckpointsTriggered;
    private transient int numberOfReservationCheckpointsTriggered;
    private transient int pacedReservationsReleased;
    // Number of objects written to disk.
    protected transient int numberOfManagedObjectsWritten;
    private transient long lastFlushMilliseconds;
    private transient long writingMilliseconds;
    private transient long removingEntriesMilliseconds;
    private transient long allocatingEntrySpaceMilliseconds;
    private transient long releasingEntrySpaceMilliseconds;
    private transient long directoryWriteMilliseconds;
    protected transient long storeFileExtendedDuringAllocation;

    // Set to true to add guardBytes to ManagedObjects, and to detect store overwrites.
    // Requires a cold start of the store to activate or deactivate.
    static final boolean useGuardBytes = false;
    static byte guardByte = 0;
    static final int guardBytesLength = 16;

    /**
     * Constructor
     * 
     * @param storeName Identifies the ObjecStore and the file directory.
     * @param objectManager The ObjectManager that manages this store.
     * @throws ObjectManagerException
     */
    public AbstractSingleFileObjectStore(String storeName,
                                         ObjectManager objectManager)
        throws ObjectManagerException {
        super(storeName,
              objectManager,
              STRATEGY_KEEP_ALWAYS); // Invoke the SuperClass constructor.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { storeName, objectManager });
            trace.exit(this,
                       cclass,
                       "<init>");
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).
    } // AbstractSingleFileObjectStore().

    /**
     * Constructor
     * 
     * @param storeName Identifies the ObjecStore and the file directory.
     * @param objectManager that manages this store.
     * @param storeStrategy one of STRATEGY_XXX:
     * @throws ObjectManagerException
     */
    public AbstractSingleFileObjectStore(String storeName,
                                         ObjectManager objectManager,
                                         int storeStrategy)
        throws ObjectManagerException
    {
        super(storeName,
              objectManager,
              storeStrategy); // Invoke the SuperClass constructor.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { storeName, objectManager, new Integer(storeStrategy) });
            trace.exit(this,
                       cclass,
                       "<init>");
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).   
    } // AbstractSingleFileObjectStore().

    /**
     * Sets the size of the store file to the new values.
     * At least the minimum space is reserved in the file system.
     * No more than the maximum number of bytes will be used.
     * Blocks until this has completed.
     * The initial values used by the ObjecStore are 0 and Long.MAX_VAULE.
     * The store will attempt to release space as ManagedObjects are deleted to reach the
     * minimum size.
     * 
     * @param newMinimumStoreFileSize the new minimum store file size in bytes.
     * @param newMaximumStoreFileSize the new maximum store file size in bytes.
     * 
     * @throws IllegalArgumentException if minimum > maximum.
     * @throws StoreFileSizeTooSmallException if the maximum is smaller than the current usage.
     * @throws PermanentIOException if the disk space cannot be expanded to the new minimum size.
     * @throws ObjectManagerException
     */
    public synchronized void setStoreFileSize(long newMinimumStoreFileSize
                                              , long newMaximumStoreFileSize)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "setStoreFileSize",
                        new Object[] { new Long(newMinimumStoreFileSize), new Long(newMaximumStoreFileSize) }
                            );

        // Synchronized so we have locked out flush();

        if (newMinimumStoreFileSize > newMaximumStoreFileSize) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "setStoreFileSize"
                                );
            throw new IllegalArgumentException(newMinimumStoreFileSize + ">" + newMaximumStoreFileSize);
        } // if (newStoreFileSize...

        // Check that the new MaximumStoreFileSize is still bigger than the existing
        // contents if the ObjectStore.
        if (newMaximumStoreFileSize < storeFileSizeUsed) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "setStoreFileSize"
                           , new Object[] { new Long(newMaximumStoreFileSize), new Long(storeFileSizeAllocated), new Long(storeFileSizeUsed) }
                                );
            throw new StoreFileSizeTooSmallException(this
                                                     , newMaximumStoreFileSize
                                                     , storeFileSizeAllocated
                                                     , storeFileSizeUsed);
        } // if (newStoreFileSize...

        // If we are expanding the minimum file size grab the disk space now.
        // If we fail before storing the new administered values the space will be release next time
        // we open().
        if (newMinimumStoreFileSize > storeFileSizeAllocated) {

            try {
                setStoreFileSizeInternalWithException(newMinimumStoreFileSize);
            } catch (java.io.IOException exception)
            {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "setStoreFileSize", exception, "1:349:1.57");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, "setStoreFileSize");

                throw new PermanentIOException(this, exception);
            }

        } // if ( newMinimumStoreFileSize > storeFileSizeAllocated).

        minimumStoreFileSize = newMinimumStoreFileSize;
        maximumStoreFileSize = newMaximumStoreFileSize;

        writeHeader();
        force();

        setAllocationAllowed();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "setStoreFileSize");
    } // setStoreFileSize().

    /**
     * @return Returns the cachedManagedObjectsSize.
     */
    public synchronized int getCachedManagedObjectsSize()
    {
        return cachedManagedObjectsSize;
    }

    /**
     * Causes all curently cached ManagedObjects to be dropped.
     * 
     * @param cachedManagedObjectsSize The cachedManagedObjectsSize to set.
     * @throws ObjectManagerException
     */
    public synchronized void setCachedManagedObjectsSize(int cachedManagedObjectsSize)
                    throws ObjectManagerException
    {
        this.cachedManagedObjectsSize = cachedManagedObjectsSize;
        cachedManagedObjects = new java.lang.ref.SoftReference[cachedManagedObjectsSize];
        writeHeader();
        force();
    }

    /**
     * @return long the maximumStoreFileSize.
     */
    public synchronized long getMaximumStoreFileSize()
    {
        return maximumStoreFileSize;
    }

    /**
     * @return long the minimumStoreFileSize.
     */
    public synchronized long getMinimumStoreFileSize()
    {
        return minimumStoreFileSize;
    }

    /**
     * Gives the size of the store file in use.
     * 
     * @return long the size of the store file in bytes.
     */
    public long getStoreFileSize()
    {
        return storeFileSizeAllocated;
    } // gestoreFileSize().

    /**
     * Gives the size of the used space in the store file in use.
     * 
     * @return long the size of the used space in the store file in bytes.
     */
    public long getStoreFileUsed()
    {
        return storeFileSizeUsed;
    }

    /**
     * Tests to see if allocation of new Objects should be allowed or not.
     * Caller must be synchronized on this.
     * 
     * @throws ObjectManagerException
     */
    protected void setAllocationAllowed()
                    throws ObjectManagerException {
        final String methodName = "setAllocationAllowed";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { new Long(storeFileSizeAllocated),
                                                                new Long(storeFileSizeUsed) });

        // Users of the store should have reserved space in the store for Objects that are allocated before
        // they finally add them to the store. We have already reserved enough space for a complete replacement 
        // of the directory which also implies there is already enough space to delete any number of 
        // ManagedObjects in the store.

        // Calculate how much file space we need to continue storing ManagedObjects.
        // We make the pessimistic assumption that the entire contents of the log might need to be written 
        // to this ObjectStore.

        // We have to take into account any pending request to increase the size of the log file, because this
        // would allow applications to over commit the store if they used the extra log space when it did not 
        // exist in the Object Store.

        long storeFileSizeRequired = storeFileSizeAllocated;

        if (simulateFullReservedSize > 0) {
            // We are simulating a a full filesystem.
            allocationAllowed = false;
            reservationThreshold = 0;

        }
        else
        {
            // do we have enough space to accomdate the whole log in the store plus reserved space in the store
            long currentReservedSize = reservedSize.get();

            long pesimisticSpaceRequired = Math.max(objectManagerState.logOutput.getLogFileSize(),
                                                    objectManagerState.logOutput.getLogFileSizeRequested())
                                           + directoryReservedSize
                                           + currentReservedSize;

            long largestFreeSpace = 0;
            if (freeSpaceByLength.size() > 0) {
                largestFreeSpace = ((FreeSpace) freeSpaceByLength.last()).length;
            }

            if (pesimisticSpaceRequired <= largestFreeSpace) {
                // we can at least fit the whole log plus reserved space in the largest free space entry
                // so we are ok
                allocationAllowed = true;
            }
            else
            {
                storeFileSizeRequired = storeFileSizeUsed +
                                        pesimisticSpaceRequired;

                // See if we need to change the size of the store file.
                if (storeFileSizeRequired <= storeFileSizeAllocated) {
                    // we have allocated more than is required, its just not in the free space map yet
                    allocationAllowed = true;

                } else if (storeFileSizeRequired <= maximumStoreFileSize) {
                    // we need more than we have allocated, and that is less than the maximum allowed, try grow to the required...
                    allocationAllowed = setStoreFileSizeInternal(storeFileSizeRequired);

                } else {
                    // We need greater than the maximum allowed, make sure we have all the space we are
                    // allowed to have.
                    if (storeFileSizeAllocated < maximumStoreFileSize)
                        setStoreFileSizeInternal(maximumStoreFileSize);
                    // TODO We could still allow allocation until we really are full when the reservation scheme will catch it!
                    allocationAllowed = false;
                    // Request a checkpoint, see if we can clear some space.
                    numberOfStoreFullCheckpointsTriggered++;
                    objectManagerState.requestCheckpoint(persistent);

                } // if ( storeFileSizeRequired...

                reservationThreshold = storeFileSizeAllocated - storeFileSizeUsed - directoryReservedSize;
                reservationCheckpointThreshold = Math.min(reservationThreshold,
                                                          currentReservedSize + reservationCheckpointMaximum);
            }
        }
        // Release at least one blocked reservation request.
        if (reservationPacing) {
            synchronized (reservationPacingLock) {
                reservationPacing = false;
                reservationPacingLock.notify();
            } // synchronized (reservationPacingLock).
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { new Boolean(allocationAllowed), new Long(storeFileSizeRequired) });
    } // setAllocationAllowed().

    /**
     * Alter the store file size. Callers of this method need to check the size asked for is within
     * the maximum and minium. TODO: refactor the checking into this method.
     * 
     * 2 versions of this method so that callers may get at the IOException if needed.
     * 
     * @param storeFileSizeRequired The requested size of the store file.
     * @return boolean true if the store file is set to the required size.
     */
    private void setStoreFileSizeInternalWithException(long storeFileSizeRequired) throws java.io.IOException {
        final String methodName = "setStoreFileSizeInternalWithException";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { new Long(storeFileSizeRequired), new Long(storeFileSizeAllocated), new Long(storeFileSizeUsed) });

        java.io.RandomAccessFile f = null;
        try {
            f = new java.io.RandomAccessFile(storeFileName, "rw"); // rw is required to set the length in java 1.5
            f.setLength(storeFileSizeRequired);

            // check we got what we asked for
            final long length = f.length();
            if (length != storeFileSizeRequired)
            {
                // we are not using a translated exception here because it will only be FFDC'd so service
                // is essentially the only consumer, and this error really should have been an IOException.

                java.io.IOException exception = new java.io.IOException("Store file size not what was requested.  Requested " + storeFileSizeRequired +
                                                                        ", got + " + length);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { exception });

                throw exception;
            }

            storeFileSizeAllocated = storeFileSizeRequired;
        } finally {
            if (f != null)
            {
                try {
                    f.close();
                } catch (java.io.IOException e) {
                    // No FFDC code needed.                
                }
            }
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);

    }

    private boolean setStoreFileSizeInternal(long storeFileSizeRequired) {
        final String methodName = "setStoreFileSizeInternal";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { new Long(storeFileSizeRequired), new Long(storeFileSizeAllocated), new Long(storeFileSizeUsed) });

        boolean allocated = false;

        try {
            setStoreFileSizeInternalWithException(storeFileSizeRequired);
            allocated = true;

        } catch (java.io.IOException exception) {
            // No FFDC code needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:603:1.57");
            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                trace.event(this,
                            cclass,
                            methodName,
                            exception);

            trace.warning(this,
                          cclass,
                          methodName,
                          "ObjectStore_AllocateFileSpaceFailed",
                          new Object[] { new Long(storeFileSizeRequired),
                                        new Long(storeFileSizeAllocated),
                                        exception });

        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { new Boolean(allocated) });

        return allocated;
    } // setStoreFileSize().

    // --------------------------------------------------------------------------
    // extends AbstractObjectStore.
    // --------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#open(com.ibm.ws.objectManager.ObjectManagerState)
     */
    public synchronized void open(ObjectManagerState objectManagerState)
                    throws ObjectManagerException {
        final String methodName = "open";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { objectManagerState });

        super.open(objectManagerState);

        managedObjectsToWrite = new ConcurrentHashMap(concurrency);
        tokensToDelete = new ConcurrentHashMap(concurrency);
        newFreeSpace = new java.util.TreeSet(); // Natural order of store entries is address
        numberOfGetRequests = 0;
        numberOfDirectoryNodesRead = 0;
        numberOfStoreFullCheckpointsTriggered = 0;
        numberOfReservationCheckpointsTriggered = 0;
        pacedReservationsReleased = 0;
        numberOfManagedObjectsWritten = 0;
        storeFileExtendedDuringAllocation = 0;
        reservationPacingLock = new ReservationPacingLock();
        getLock = new GetLock();

        storeFileName = (String) objectManagerState.objectStoreLocations.get(storeName);
        if (storeFileName == null)
            storeFileName = storeName;
        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(this,
                        cclass,
                        methodName,
                        new Object[] { "storeName:664", storeName, storeFileName });

        // During restart the file must exist, later we will check that it has at least a valid header.
        // This test a voids creating unnecessary empty files in the case where the file does not already 
        // exist at a warm start. It is not foolproof though, if the file is deleted by another process
        // between here and the RandomAccesFile constructor we will create a new empty file.    
        if (objectManagerState.getObjectManagerStateState() == ObjectManagerState.stateReplayingLog
            && !(new java.io.File(storeFileName).exists())) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { "File does not exist:673" });
            throw new NonExistentObjectStoreFileException(this,
                                                          storeFileName);
        } // if.

        // Access the underlying file.
        try {
            storeFile = new java.io.RandomAccessFile(storeFileName,
                                                     "rw" // Read, Write
            );
        } catch (java.io.FileNotFoundException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:685:1.57");
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { "File not found exception:687" });
            throw new NonExistentObjectStoreFileException(this,
                                                          exception,
                                                          storeFileName);
        } // try.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // open().

    /**
     * Space accounting.
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#reserve(int,boolean)
     */
    public final void reserve(int deltaSize, boolean paced)
                    throws ObjectManagerException
    {
        final String methodName = "reserve";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { new Integer(deltaSize), new Boolean(paced) });

        long newReservedSize = reservedSize.addAndGet(deltaSize);

        if (newReservedSize > reservationCheckpointThreshold) {
            numberOfReservationCheckpointsTriggered++;
            objectManagerState.requestCheckpoint(persistent);

            // Pacing: slow paced requests.
            // ----------------------------   
            if (paced) {
                synchronized (reservationPacingLock) {

                    // TODO not during restart!
                    // TODO Need to release all waiters at shutdown.
                    while (newReservedSize > reservationCheckpointThreshold
                           && reservationPacing
                           && paced) {
                        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                            trace.debug(this, cclass, methodName, new Object[] { "wait:728",
                                                                                new Long(newReservedSize),
                                                                                new Long(reservationCheckpointThreshold),
                                                                                new Long(reservationThreshold),
                                                                                new Boolean(reservationPacing) });
                        try {
                            newReservedSize = reservedSize.addAndGet(-deltaSize);
                            reservationPacingLock.wait();

                        } catch (InterruptedException exception) {
                            // No FFDC Code Needed.
                            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:739:1.57");
                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this, cclass, methodName, exception);
                            throw new UnexpectedExceptionException(this, exception);

                        } finally {
                            newReservedSize = reservedSize.addAndGet(deltaSize);
                        } // try...
                    } // while (newReservedSize > reservationCheckpointThreshold...

                    // If we have a big enough backlog of reserved space in the store, 
                    // make subsequent paced requests also wait, 
                    // except during restart or shutdown.
                    if (newReservedSize > reservationCheckpointThreshold
                        && (objectManagerState.state == ObjectManagerState.stateColdStarted
                        || objectManagerState.state == ObjectManagerState.stateWarmStarted)) {
                        reservationPacing = true;
                    } else {
                        // Allow at least one more waiter to proceed.
                        reservationPacing = false;
                        reservationPacingLock.notify();
                    } // if (newReservedSize...
                } // synchronized (reservationPacingLock).

            } // if (paced)..

            // Expand the file size if necessary.
            // ----------------------------------

            if (newReservedSize > reservationThreshold) {
                synchronized (this) {
                    // Try extending the file.
                    // TODO setAllocationAllowed() will release another paced reservation.
                    setAllocationAllowed();

                    if (newReservedSize > (storeFileSizeAllocated - storeFileSizeUsed - directoryReservedSize)) {
                        // Suppress storeFull exceptions during recovery.
                        // Also suppress the exception if we are releasing space by using a negative delta size.
                        // During commit surplus space is released into the store. If the directory depth has
                        // increased meanwhile this may mean that there is still insufficient space in the store to allow
                        // further reservation requests, but we still allow negative requests.
                        if (objectManagerState.getObjectManagerStateState() == ObjectManagerState.stateReplayingLog
                            || deltaSize <= 0) {
                            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                                trace.debug(this, cclass, methodName, new Object[] { "Objectstorefull exception supressed:783",
                                                                                    new Long(newReservedSize) });
                        } else {
                            // We can't make the reservation so take it back.
                            reservedSize.addAndGet(-deltaSize);

                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this, cclass, methodName, new Object[] { new Long(newReservedSize) });
                            throw new ObjectStoreFullException(this, null); // TODO objectToStore
                        } // if (objectManagerState...
                    } // if (reservedSize > (storeFileSizeAllocated - storeFileSizeUsed)).
                } // synchronized (this).

            } // if (newReservedSize > reservationThreshold).

        } else if (reservationPacing) {
            // We are below the limit so release any paced reservations.
            synchronized (reservationPacingLock) {
                pacedReservationsReleased++;
                reservationPacing = false;
                reservationPacingLock.notify();
            } // synchronized (reservationPacingLock).

        } // if (newReservedSize > reservationCheckpointThreshold).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // reserve().

    /**
     * When enabled, reserve requests to this ObjectStore will throw ObjectStoreFullException.
     * 
     * @param isFull true subsequent reservations throw ObjectStoreFullException. if false subsequent reservations may
     *            succeed.
     * @throws ObjectManagerException
     */
    public void simulateFull(boolean isFull)
                    throws ObjectManagerException {
        final String methodName = "simulateFull";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { new Boolean(isFull) });

        if (isFull) {
            // Clear as much space as we can.
            objectManagerState.waitForCheckpoint(true);
            // Reserve all of the available space.
            synchronized (this) {
                long available = storeFileSizeAllocated - storeFileSizeUsed - directoryReservedSize - reservedSize.get();
                long newReservedSize = reservedSize.addAndGet((int) available);
                simulateFullReservedSize = simulateFullReservedSize + available;
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this, cclass, methodName, new Object[] { "isFull:834",
                                                                        new Long(available),
                                                                        new Long(newReservedSize),
                                                                        new Long(simulateFullReservedSize) });

            } // synchronized (this).

        } else {
            synchronized (this) {
                reservedSize.addAndGet((int) -simulateFullReservedSize);
                simulateFullReservedSize = 0;
            } // synchronized (this).
            objectManagerState.waitForCheckpoint(true);
        } // if (isFull).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { new Long(simulateFullReservedSize) });
    } // simulateFull().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#add(com.ibm.ws.objectManager.ManagedObject, boolean)
     */
    public void add(ManagedObject managedObject,
                    boolean requiresCurrentCheckpoint)
                    throws ObjectManagerException
    {
        final String methodName = "add";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { managedObject, new Boolean(requiresCurrentCheckpoint) });

        super.add(managedObject, requiresCurrentCheckpoint);

        // Replacements are detected at flush time,
        // we free the space occupied by any previous version after we have written
        // the new one.

        // TODO Better to change the maps to use a single identifier returned by the
        // ManagedObject.
        //      This would save newing up Long().

        // Include this to the set of ManagedObjects to write, if its not already included.
        if (requiresCurrentCheckpoint
            && storeStrategy != STRATEGY_SAVE_ONLY_ON_SHUTDOWN) {
            // The checkpoint logic in ObjectManagerState never calls flush until all of the
            // requiresCurrentCheckpoint transactions have been checkpointed. So no need to synchronize 
            // on checkpointManagedObjectsToWrite once we have made sure it exits.
            if (checkpointManagedObjectsToWrite == null)
                captureCheckpointManagedObjects();

            checkpointManagedObjectsToWrite.put(new Long(managedObject.owningToken.storedObjectIdentifier),
                                                managedObject);

        } else {
            for (;;) {
                // Repeat requests to write until we can see the same Map after we have done the insertion, 
                // this means that we will have been included in at least one flush.
                java.util.Map myManagedObjectsToWrite = managedObjectsToWrite;
                myManagedObjectsToWrite.put(new Long(managedObject.owningToken.storedObjectIdentifier),
                                            managedObject);
                // Make sure the flush() did not steal managedObjectsToWrite while we were adding to it.
                if (myManagedObjectsToWrite == managedObjectsToWrite)
                    break;
            } // for(;;).
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // add().

    /**
     * Actual removal from the file takes place when flush() is called.
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#remove(com.ibm.ws.objectManager.Token, boolean)
     */
    public void remove(Token token,
                       boolean requiresCurrentCheckpoint)
                    throws ObjectManagerException {
        final String methodName = "remove";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { token, new Boolean(requiresCurrentCheckpoint) });

        super.remove(token, requiresCurrentCheckpoint);

        // Cancel any writing.
        managedObjectsToWrite.remove(new Long(token.storedObjectIdentifier));

        // Schedule the deletion of the disk copy.
        if (storeStrategy == STRATEGY_SAVE_ONLY_ON_SHUTDOWN) {
            // We don't need to add Objects that are not in the directory as they wont need to be 
            // deleted. This is important for Stores only written at shutdown as they may 
            // accumulate a lot of delete operations.
            Directory.StoreArea storeArea = (Directory.StoreArea) directory.getEntry(new Long(token.storedObjectIdentifier));
            if (storeArea != null)
                tokensToDelete.put(new Long(token.storedObjectIdentifier),
                                   token);

        } else if (requiresCurrentCheckpoint) {
            // The checkpoint logic in ObjectManagerState never calls flush until all of the
            // requiresCurrentCheckpoint transactions have been checkpointed. So no need to synchronize 
            // on checkpointManagedObjectsToWrite.
            if (checkpointTokensToDelete == null)
                captureCheckpointManagedObjects();

            checkpointTokensToDelete.put(new Long(token.storedObjectIdentifier),
                                         token);

        } else {
            // If flush() gets ahead of our addition to this table it will be deleted
            // next time.
            // Repeat attempts to insert the token into the tokensToDelete until we find one that 
            // we can be sure was included in a flush().
            for (;;) {
                java.util.Map myTokensToDelete = tokensToDelete;
                myTokensToDelete.put(new Long(token.storedObjectIdentifier),
                                     token);
                // Make sure the flush() did not steal tokensToDelete while we were adding to it.
                if (myTokensToDelete == tokensToDelete)
                    break;
            } // for (;;).
        }

        // Cancel the ManagedObjects reference from the cache, if it is in memory. We might get the wrong
        // ManagedObject if the slot has been reused, that's OK because it's just the same as if it 
        // is being reused.
        if (token.managedObjectReference != null) {
            ManagedObject managedObject = (ManagedObject) token.managedObjectReference.get();
            if (managedObject != null) {
                java.lang.ref.SoftReference[] localCachedManagedObjects = cachedManagedObjects;
                if (managedObject.objectStoreCacheIndex < localCachedManagedObjects.length)
                    localCachedManagedObjects[managedObject.objectStoreCacheIndex] = null;
            }
        } // if (token.managedObjectReference != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // remove().

    /**
     * Capture the ManagedObjects to write and delete as part of the checkpoint.
     */
    synchronized void captureCheckpointManagedObjects()
    {
        final String methodName = "captureCheckpointManagedObjects";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        // Now that we are synchronized check that we have not captured the checkpoint sets already. 
        if (checkpointManagedObjectsToWrite == null) {
            // Take the tokens to write before TokensToDelete, if we miss a delete we will catch it next time. 
            // The managedObjectsToWrite and tokensToDelete sets are volatile so users of the store will move to them 
            // promptly.
            checkpointManagedObjectsToWrite = managedObjectsToWrite;
            managedObjectsToWrite = new ConcurrentHashMap(concurrency);
            checkpointTokensToDelete = tokensToDelete;
            tokensToDelete = new ConcurrentHashMap(concurrency);
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // captureCheckpointManagedObjects().

    /**
     * Keep recently used ManagedObjects in memory if space allows.
     * 
     * @param managedObject to be cached.
     */
    //TODO This function could be moved up into AbstractObjectStore, or made into a single cache for the
    //TODO whole ObjectManager, by putting it in ObjectManagerstate so that all stores can use it.
    void cache(ManagedObject managedObject) {
        // Make a local reference to cachedManagedObjects in case it is changing, there is a risk that 
        // the same slot in cachedMangedObjects will be used twice, in that case the Object is not 
        // cached and will be read again when referenced.
        java.lang.ref.SoftReference[] localCachedManagedObjects = cachedManagedObjects;
        int localICache = iCache++;
        if (localICache >= localCachedManagedObjects.length) {
            iCache = 0;
            localICache = 0;
        }

        localCachedManagedObjects[localICache] = new java.lang.ref.SoftReference(managedObject);
        managedObject.objectStoreCacheIndex = localICache;
    } // cache().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#clear()
     */
    protected synchronized void clear()
                    throws ObjectManagerException {
        final String methodName = "clear";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName);

        // No call to super.clear() because we use a WeakValueHashMap.
        inMemoryTokens.clear();
        cachedManagedObjects = new java.lang.ref.SoftReference[cachedManagedObjectsSize];
        managedObjectsToWrite.clear();
        tokensToDelete.clear();
        // No need to clear newFreeSpace because the existing one is cleared just before we flush anyway.

        // Allow for two headers to be written at the front of the file.
        storeFileSizeUsed = pageSize * 2;
        freeSpaceByAddressHead = null;
        freeSpaceByLength = new java.util.TreeSet(new LengthComparator());
        freeSpaceStoreArea = null;

        // Sequence numbers 0-200 are reserved.    
        sequenceNumber = initialSequenceNumber;

        directory = makeDirectory(initialDirectoryMinimumNodeSize, 0, 0);
        directoryReservedSize = directory.spaceRequired();
        reservedSize.set(0);
        setAllocationAllowed();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // clear().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#close()
     */
    public synchronized void close()
                    throws ObjectManagerException
    {
        final String methodName = "close";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        super.close();

        // Close the storeFile.
        try {
            if (storeFile != null)
                storeFile.close();
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "close", exception, "1:1082:1.57");
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "close",
                           "via PermanentIOException");
            throw new PermanentIOException(this,
                                           exception);
        } //  catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // close(). 

    /**
     * Writes buffered output to hardened storage. Blocks until this has
     * completed.
     * 
     * @throws ObjectManagerException
     */
    public synchronized void flush()
                    throws ObjectManagerException
    {
        final String methodName = "flush";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        if (storeStrategy == STRATEGY_SAVE_ONLY_ON_SHUTDOWN) {
            // Since we only flush on shutdown make sure we are shutting down, otherwise just return. 
            if (objectManagerState.getObjectManagerStateState() != ObjectManagerState.stateStopped) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName);
                return;
            }

            // If there are any active transactions it is not safe to flush.
            if (objectManagerState.getTransactionIterator().hasNext()) {
                trace.warning(this,
                              cclass,
                              methodName,
                              "ObjectStore_UnsafeToFlush",
                              this
                                );
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName);
                return;
            }
        } // if ( storeStrategy == STRATEGY_SAVE_ONLY_ON_SHUTDOWN ).

        // Debug freespace list
        // if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()) trace.debug(this, cclass, methodName, "START of Flush: newFreeSpace.size()      = "+newFreeSpace.size());
        // if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()) trace.debug(this, cclass, methodName, "START of Flush: freeSpaceByLength.size() = "+freeSpaceByLength.size());

        // We are single threaded through flush, we are now about to make
        // updates to the directory and free space map, which must be consistent.
        // Also reserve space in the file for the directory updates and free space map.
        updateDirectory();

        // Release any space into the free space pool.
        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(this,
                        cclass,
                        methodName,
                        "Release new free space");

        // Free up space that was release by directory changes, replaced or deleted Objects.
        freeAllocatedSpace(newFreeSpace);

        if (gatherStatistics) {
            long now = System.currentTimeMillis();
            releasingEntrySpaceMilliseconds += now - lastFlushMilliseconds;
            lastFlushMilliseconds = now;
        } // if (gatherStatistics).

        // Write the modified parts of the directory to disk.
        directory.write();

        if (gatherStatistics) {
            long now = System.currentTimeMillis();
            directoryWriteMilliseconds += now - lastFlushMilliseconds;
            lastFlushMilliseconds = now;
        } // if (gatherStatistics).

        // Write the free space map to disk.
        writeFreeSpace();

        // Force the data to disk then force the header, if we can make the assumption that
        // these writes go to disk first, this first force is unnecessary.
        if (storeStrategy == STRATEGY_KEEP_ALWAYS)
            force();

        writeHeader();
        if (storeStrategy == STRATEGY_KEEP_ALWAYS)
            force();

        // Debug freespace list
        // if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()) trace.debug(this, cclass, methodName, "END of Flush: newFreeSpace.size()      = "+newFreeSpace.size());
        // if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()) trace.debug(this, cclass, methodName, "END of Flush: freeSpaceByLength.size() = "+freeSpaceByLength.size());

        // Defect 573905
        // Reset the newFreeSpace after we have finished with it to release the
        // memory it uses while we are not flushing.
        newFreeSpace.clear();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // flush().

    /**
     * Capture the objects we will write and delete.
     * Update the directory and freeSpaceMap. Do not overwrite any existing data
     * because we need to have the before and after versions available until we flip
     * over to the after version when we rewrite the header.
     * 1) Update the directory.
     * 2) Allocate new space for the directory and the free space map.
     * 
     * Caller must be synchronized on this.
     * 
     * @throws ObjectStoreFullException if we can't allocate the space needed to make the update.
     * @throws ObjectManagerException
     */
    private void updateDirectory()
                    throws ObjectManagerException
    {
        final String methodName = "updateDirectory";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

// TODO See below for how to deal with out of disk space conditions.    
//    // Keep the current disk location of the directory.
//    long currentDirectoryRootAddress = ((Directory.Node)directory.root).byteAddress;
//    long currentDirectoryRootLength = ((Directory.Node)directory.root).length;
//    long currentStoreFileSizeUsed = storeFileSizeUsed;
//    
//    long newFreeSpaceLength;
//    long newFreeSpaceByteAddress; 

//  try {    
        // Capture the ManagedObjects to write and delete. The checkpoint manager has does not 
        // call flush until all transactions have completd their checkpoint activity.
        // If we have not seen any chekpoint updates truncate the write and delete sets now. 
        if (checkpointManagedObjectsToWrite == null)
            captureCheckpointManagedObjects();

        // Defect 573905
        // Free space clear moved to end of flush.

        // The reserved space we will release once the flush has written all it needs to the disk.
        checkpointReleaseSize = 0;

        if (gatherStatistics) // Start the clock.
            lastFlushMilliseconds = System.currentTimeMillis();

        // 1) Update the directory.
        // ------------------------
        // New write updates are not updating checkpointManagedObjectsToWrite, we have a safe set.
        for (java.util.Iterator managedObjectIterator = checkpointManagedObjectsToWrite.values().iterator(); managedObjectIterator.hasNext();) {
            ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
            cache(managedObject);
            write(managedObject);
        } // For ... checkpointManagedObjectsToWrite.
        checkpointManagedObjectsToWrite = null;

        if (gatherStatistics) {
            long now = System.currentTimeMillis();
            writingMilliseconds += now - lastFlushMilliseconds;
            lastFlushMilliseconds = now;
        } // if (gatherStatistics).

        // Remove Objects from the directory and make a note of any free space that we can release
        // once we have allocated all of the new space we will need.    
        for (java.util.Iterator tokenIterator = checkpointTokensToDelete.values().iterator(); tokenIterator.hasNext();) {
            Token token = (Token) tokenIterator.next();
            // Delete the object by removing it from the directory, we dont touch
            // the data on the disk.
            Directory.StoreArea storeArea = (Directory.StoreArea) directory.removeEntry(new Long(token.storedObjectIdentifier));
            // Did we ever write this?
            if (storeArea != null)
                newFreeSpace.add(storeArea);
        } // For ... checkpointTokensToDelete.
        checkpointTokensToDelete = null;

        if (gatherStatistics) {
            long now = System.currentTimeMillis();
            removingEntriesMilliseconds += now - lastFlushMilliseconds;
            lastFlushMilliseconds = now;
        } // if (gatherStatistics).

        // 2) Allocate new space for the directory and the free space map.
        // ---------------------------------------------------------------
        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(this,
                        cclass,
                        methodName,
                        "Reserve space for the direcory and new free space map");

        // All changes have been made to the directory, now reserve space in the file for any updates 
        // and release any space which will become free.
        directory.reserveSpace();

        if (gatherStatistics) {
            long now = System.currentTimeMillis();
            allocatingEntrySpaceMilliseconds += now - lastFlushMilliseconds;
            lastFlushMilliseconds = now;
        } // if (gatherStatistics).

        if (freeSpaceStoreArea != null && freeSpaceStoreArea.length != 0)
            newFreeSpace.add(freeSpaceStoreArea);
        // Make a worst case assumption that no new free space will merge with the 
        // existing free space.
        long newFreeSpaceLength = (freeSpaceByLength.size() + newFreeSpace.size()) * freeSpaceEntryLength;
        FreeSpace newFreeSpaceArea = allocateSpace(newFreeSpaceLength);

        //TODO The following code segment will restore the ObjectStore to the state ir was in before we ran out
        //TODO of disk space, however the serialized bytes will now have been released from the ManagedObjects. 
//    } catch (ObjectStoreFullException objectStoreFullException) {
//      // No FFDC code needed.
//      if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
//        trace.event(this,
//                    cclass,
//                    methodName,
//                    objectStoreFullException);
//      
//      // Not enough space was available to make the update. 
//      // Perhaps insufficient reservation() was done?
//      // Put things back the way they were.
//      managedObjectsToWrite.putAll(checkpointManagedObjectsToWrite);
//      tokensToDelete.putAll(checkpointTokensToDelete);
//      // Revert to the disk copy of the directory, if there is none
//      // the length will be zero so we will create a new empty directory.
//      directory = readDirectory(directory.getMinimumNodeSize(),
//                                currentDirectoryRootAddress,
//                                currentDirectoryRootLength);
//      
//      storeFileSizeUsed = currentStoreFileSizeUsed;
//      
//      if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//        trace.exit(this,
//                   cclass,
//                   methodName,
//                   new Object[]{objectStoreFullException});
//      throw objectStoreFullException;      
//    } // try...

        // All space allocation is now done.   
        freeSpaceStoreArea = directory.makeStoreArea(freeSpaceIdentifier,
                                                     newFreeSpaceArea.address,
                                                     newFreeSpaceArea.length);

        // Adjust the space we need to safely remove entries from the directory.
        // The worstCaseDirectorySpace is an over estimate of the space needed so we might 
        // prematurely stop allocations.
        directoryReservedSize = directory.spaceRequired();
        // Give back all space that was previously reserved, but which has now been written.
        reservedSize.addAndGet(-checkpointReleaseSize);

        // See if the store is too now full to allow further ManagedObject allocation.
        // Also set the reservation thresholds.
        setAllocationAllowed();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // updateDirectory().

    /**
     * Allocate space in the file.
     * 
     * @param lengthRequired the number of bytes needed.
     * @return FreeSpace giving the byte address that the buffer can be written at. The actual
     *         length allocated may be greater than the size requested.
     * @throws ObjectManagerException
     */
    FreeSpace allocateSpace(long lengthRequired)
                    throws ObjectManagerException
    {
        final String methodName = "allocateSpace";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { new Long(lengthRequired) });

        // Find some free space, first search the free space map for the smallest
        // entry that will accommodate the request.
        FreeSpace freeSpace;

        java.util.SortedSet tailSet = freeSpaceByLength.tailSet(new FreeSpace(0, lengthRequired));
        if (!tailSet.isEmpty()) {
            // Use free space in the body of file.
            freeSpace = (FreeSpace) tailSet.first();
            tailSet.remove(freeSpace);

            long remainingLength = freeSpace.length - lengthRequired;

            if (remainingLength < minimumFreeSpaceEntrySize) {

                // All of this slot used up, also remove it from the address map.
                if (freeSpace.prev != null) {
                    freeSpace.prev.next = freeSpace.next;
                }
                else {
                    freeSpaceByAddressHead = freeSpace.next;
                }
                if (freeSpace.next != null) {
                    freeSpace.next.prev = freeSpace.prev;
                }
                // Return without any links
                freeSpace.prev = freeSpace.next = null;

                // Debug freespace list
                // if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()) trace.debug(this, cclass, methodName, "REMOVE from freespace list");

            } else {
                // Partially used, reduce the length, but leave it in place in the address Map.
                // Allocate the end portion of the space found to a new FreeSpace area and  
                // add the remaining piece back into the length map, where its key will have changed. 
                freeSpace.length = remainingLength;
                freeSpaceByLength.add(freeSpace);

                freeSpace = new FreeSpace(freeSpace.address + remainingLength, lengthRequired);
            } // if (remainingLength == 0).

        } else {
            // Add to end of file.
            freeSpace = new FreeSpace(storeFileSizeUsed, lengthRequired);
            long newStoreFileSizeUsed = storeFileSizeUsed + lengthRequired;
            // Check to see if we need to extended the file. 
            if (newStoreFileSizeUsed > storeFileSizeAllocated) {
                storeFileExtendedDuringAllocation++;

                // This should not occur if we are using STRATEGY_KEEP_ALWAYS because we 
                // have already allocated enough space to store all the objects currently
                // in the log.
                if (storeStrategy == STRATEGY_KEEP_ALWAYS
                    && (objectManagerState.logFileType == ObjectManager.LOG_FILE_TYPE_FILE
                    || objectManagerState.logFileType == ObjectManager.LOG_FILE_TYPE_CLEAR)) {
                    ObjectManager.ffdc.processException(this,
                                                        cclass,
                                                        methodName,
                                                        new Exception("Extended allocated file"),
                                                        "1:1437:1.57",
                                                        new Object[] { new Long(newStoreFileSizeUsed), new Long(storeFileSizeAllocated) });
                    if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                        trace.debug(this, cclass, methodName, new Object[] { "STRATEGY_KEEP_ALWAYS:1440",
                                                                            new Long(newStoreFileSizeUsed),
                                                                            new Long(storeFileSizeAllocated) });
                } // if ( objectManagerState.logFileType...

                if (newStoreFileSizeUsed <= maximumStoreFileSize
                    && setStoreFileSizeInternal(newStoreFileSizeUsed)) {

                } else {
                    allocationAllowed = false;

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   methodName,
                                   new Object[] { "ObjectStoreFull" });
                    throw new ObjectStoreFullException(this,
                                                       null);
                }
            } // if (newStoreFileSizeUsed > storeFileSizeAllocated).

            storeFileSizeUsed = newStoreFileSizeUsed;
        } // if (!tailMap.isEmpty()).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { freeSpace });
        return freeSpace;
    } // allocateSpace().

    private void freeAllocatedSpace(java.util.Collection sortedFreeSpaceList)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "freeAllocatedSpace",
                        new Object[] { new Integer(sortedFreeSpaceList.size()), new Long(freeSpaceByLength.size()) });

        // Remove from the head of the sorted set until we find the first non-negative
        // address - indicating that the storage was allocated
        java.util.Iterator listIterator = sortedFreeSpaceList.iterator();
        Directory.StoreArea currentArea = null;
        while (listIterator.hasNext()) {
            currentArea = (Directory.StoreArea) listIterator.next();
            if (currentArea.byteAddress > 0)
                break;
        }

        // Did we find at least one to merge?
        if (currentArea != null) {

            // We now have a pointer to the first store area in the sorted list
            // that needs to be merged into the free space map.
            // We iterate through the free space map (which is also in order)
            // merging the entries in, and moving our pointer forwards.
            FreeSpace spaceEntry = freeSpaceByAddressHead;
            FreeSpace previousEntry = null;
            do {
                // If spaceEntry is null then we have reached the end of the list.
                // We handle this case first, because we can avoid null-checks in
                // other branches.
                // The same logic is used to handle the case where we have moved
                // past the point in the address-sorted free space list where this
                // entry would be merged, and did not find any existing entries
                // to merge it with. Merging would have been performed in branches
                // below on an earlier pass round the loop if it was possible (as
                // we would have looked at the entry that is now spaceEntry as        
                // spaceEntry.next in the below branches).
                if (spaceEntry == null || // Tail of list reached
                    spaceEntry.address > currentArea.byteAddress // Moved past insertion point without merge
                ) {
                    // Create a new entry, unless this is a zero-sized entry
                    if (currentArea.length > 0) {
                        FreeSpace newSpaceEntry =
                                        new FreeSpace(currentArea.byteAddress, currentArea.length);

                        // Link it in behind the current entry
                        newSpaceEntry.next = spaceEntry;
                        if (previousEntry != null) {
                            previousEntry.next = newSpaceEntry;
                        }
                        else {
                            // We are the new head
                            freeSpaceByAddressHead = newSpaceEntry;
                        }
                        newSpaceEntry.prev = previousEntry;
                        if (spaceEntry != null) {
                            spaceEntry.prev = newSpaceEntry;
                        }

                        // Add our extended entry into the length-sorted list
                        freeSpaceByLength.add(newSpaceEntry);

                        // Debug freespace list
                        // if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()) trace.debug(this, cclass, methodName, "ADD to freespace list");

                        // Keep track of the maximum free space count as a statistic
                        if (gatherStatistics && freeSpaceByLength.size() > maxFreeSpaceCount)
                            maxFreeSpaceCount = freeSpaceByLength.size();

                        // As we've added a new entry before the current on, we should use it next time round
                        spaceEntry = newSpaceEntry;
                        // Previous entry stayed the same - as we've inserted without moving forwards
                    }
                    // Regardless of whether we added an entry, move onto the next store area and
                    // go back round the loop.
                    if (listIterator.hasNext()) {
                        currentArea = (Directory.StoreArea) listIterator.next();
                    }
                    else
                        currentArea = null; // We've run out of entries to merge
                }
                // Can our current store entry be merged with the current free space entry.
                else if (spaceEntry.address + spaceEntry.length == currentArea.byteAddress) {
                    // We can merge this entry with the one before it.
                    // Remove from the length-sorted list and change the size
                    freeSpaceByLength.remove(spaceEntry);
                    spaceEntry.length += currentArea.length;

                    // Can we also merge it with the one after it?
                    FreeSpace nextSpaceEntry = spaceEntry.next;
                    if (nextSpaceEntry != null &&
                        currentArea.byteAddress + currentArea.length == nextSpaceEntry.address) {
                        // Remove the eliminated space entry from the length-sorted list
                        freeSpaceByLength.remove(nextSpaceEntry);

                        // Debug freespace list
                        // if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()) trace.debug(this, cclass, methodName, "REMOVE from freespace list");

                        // Make the previous one larger
                        spaceEntry.length += nextSpaceEntry.length;
                        // Remove the next one
                        spaceEntry.next = nextSpaceEntry.next;
                        if (nextSpaceEntry.next != null) {
                            nextSpaceEntry.next.prev = spaceEntry;
                        }
                    }

                    // Add our extended entry into the length-sorted list
                    freeSpaceByLength.add(spaceEntry);

                    // We've merged this store entry now, so move onto the next one
                    // in the sorted list.
                    if (listIterator.hasNext()) {
                        currentArea = (Directory.StoreArea) listIterator.next();
                    }
                    else
                        currentArea = null; // We've run out of entries to merge
                    // Note we do not advance our position in the free space, as the
                    // current entry could also be of interest to the next store item.
                }
                // Can our current store entry be merged with the next free space entry
                // (note that the case where it merges with both is already handled).
                else if (spaceEntry.next != null &&
                         currentArea.byteAddress + currentArea.length == spaceEntry.next.address) {
                    // Remove from the length-sorted list and change the size
                    FreeSpace nextSpaceEntry = spaceEntry.next;
                    freeSpaceByLength.remove(nextSpaceEntry);
                    nextSpaceEntry.address = currentArea.byteAddress;
                    nextSpaceEntry.length += currentArea.length;

                    // Add back into the length-sorted list
                    freeSpaceByLength.add(nextSpaceEntry);

                    // We've merged this store entry now, so move onto the next one
                    // in the sorted list.
                    if (listIterator.hasNext()) {
                        currentArea = (Directory.StoreArea) listIterator.next();
                    }
                    else
                        currentArea = null; // We've run out of entries to merge
                    // Note we do not advance our position in the free space, as the
                    // current entry could also be of interest to the next store item.
                }
                // Otherwise this space entry is not interesting to us, and we
                // can simply move onto the next one.
                else {
                    previousEntry = spaceEntry;
                    spaceEntry = spaceEntry.next;
                }
                // Although looping through the free space map, our condition for
                // breaking the loop is when we've run out of entries to merge.
            } while (currentArea != null);
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "freeAllocatedSpace",
                       new Object[] { new Long(freeSpaceByLength.size()) });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#captureStatistics()
     */
    public java.util.Map captureStatistics()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "captureStatistics");

        java.util.Map statistics = super.captureStatistics();

        statistics.put("managedobjectsToWrite.size()",
                       Integer.toString(managedObjectsToWrite.size()));
        statistics.put("reservedSize",
                       reservedSize.toString());
        statistics.put("directoryReservedSize",
                       Long.toString(directoryReservedSize));
        statistics.put("tokensToDelete.size()",
                       Integer.toString(tokensToDelete.size()));
        statistics.put("directory.size()",
                       Long.toString(directory.size()));
        statistics.put("storeFileSizeUsed",
                       Long.toString(storeFileSizeUsed));
        statistics.put("storeFileSizeAllocated",
                       Long.toString(storeFileSizeAllocated));
        statistics.put("freeSpaceByLength.size()",
                       Integer.toString(freeSpaceByLength.size()));
        statistics.put("maxFreeSpaceCount",
                       Integer.toString(maxFreeSpaceCount));
        statistics.put("numberOfGetRequests",
                       Integer.toString(numberOfGetRequests));
        numberOfGetRequests = 0;
        statistics.put("storeFileExtendedDuringAllocation",
                       Long.toString(storeFileExtendedDuringAllocation));
        storeFileExtendedDuringAllocation = 0;
        statistics.put("numberOfDirectoryNodesRead",
                       Integer.toString(numberOfDirectoryNodesRead));
        numberOfDirectoryNodesRead = 0;
        statistics.put("numberOfStoreFullCheckpointsTriggered",
                       Integer.toString(numberOfStoreFullCheckpointsTriggered));
        numberOfStoreFullCheckpointsTriggered = 0;
        statistics.put("numberOfReservationCheckpointsTriggered",
                       Integer.toString(numberOfReservationCheckpointsTriggered));
        numberOfReservationCheckpointsTriggered = 0;
        statistics.put("pacedReservationsReleased",
                       Integer.toString(pacedReservationsReleased));
        pacedReservationsReleased = 0;
        statistics.put("numberOfManagedObjectsWritten",
                       Integer.toString(numberOfManagedObjectsWritten));
        numberOfManagedObjectsWritten = 0;
        statistics.put("writingMilliseconds",
                       Long.toString(writingMilliseconds));
        writingMilliseconds = 0;
        statistics.put("removingEntriesMilliseconds",
                       Long.toString(removingEntriesMilliseconds));
        removingEntriesMilliseconds = 0;
        statistics.put("allocatingEntrySpaceMilliseconds",
                       Long.toString(allocatingEntrySpaceMilliseconds));
        allocatingEntrySpaceMilliseconds = 0;
        statistics.put("releasingEntrySpaceMilliseconds",
                       Long.toString(releasingEntrySpaceMilliseconds));
        releasingEntrySpaceMilliseconds = 0;
        statistics.put("directoryWriteMilliseconds",
                       Long.toString(directoryWriteMilliseconds));
        directoryWriteMilliseconds = 0;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "captureStatistics",
                       "return=" + statistics);
        return statistics;
    } // method captureStatistics().

    /**
     * Print a dump of the state.
     * 
     * @param printWriter to be written to.
     */
    public synchronized void print(java.io.PrintWriter printWriter)
    {
        super.print(printWriter);

        try {
            printWriter.println("State Dump for:" + cclass.getName()
                                + "\n storeFile=" + storeFile + "(java.io.RandomaccessFile)"
                                + "\n storeFileSizeUsed=" + storeFileSizeUsed + "(long)"
                                + " storeFileSizeAllocated=" + storeFileSizeAllocated + "(long)"
                                + " reservedSize=" + reservedSize + "(long)"
                                + " directoryReservedSize=" + directoryReservedSize + "(long)"
                                + "\n minimumStoreFileSize=" + minimumStoreFileSize
                                + " maximumStoreFileSize=" + maximumStoreFileSize
                                + "\n ((Directory.Node)directory.root).byteAddress=" + ((Directory.Node) directory.root).byteAddress
                                + "((Directory.Node)directory.root).length=" + ((Directory.Node) directory.root).length
                                + " directory.size()=" + directory.size()
                                + "\n freeSpaceStoreArea=" + freeSpaceStoreArea);
            printWriter.println();

            // directory.print(printWriter);
            // printWriter.println();

            printWriter.println("managedObjectsToWrite...");
            for (java.util.Iterator managedObjectIterator = managedObjectsToWrite.values().iterator(); managedObjectIterator.hasNext();) {
                ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
                printWriter.println(managedObject.toString());
            } // for ... managedObjectsToWrite.

            printWriter.println("tokensToDelete...");
            for (java.util.Iterator tokenIterator = tokensToDelete.values().iterator(); tokenIterator.hasNext();) {
                Token token = (Token) tokenIterator.next();
                printWriter.println(token.toString());
            } // for ... tokensToDelete.

            printWriter.println("directory...");
            printWriter.println("identifier          byteAddress         length");
            printWriter.println(("directoryRoot       ")
                                + (((Directory.Node) directory.root).byteAddress + "                    ").substring(0, 20)
                                + (((Directory.Node) directory.root).length + "                    ").substring(0, 20)
                            );
            for (Iterator storeAreaIterator = directory.entrySet().iterator(); storeAreaIterator.hasNext();) {
                Directory.StoreArea storeArea = (Directory.StoreArea) storeAreaIterator.next();
                printWriter.println((storeArea.identifier + "                    ").substring(0, 20)
                                    + (storeArea.byteAddress + "                    ").substring(0, 20)
                                    + (storeArea.length + "                    ").substring(0, 20)
                                );
                Directory.Node child = (Directory.Node) storeArea.getChild();
                if (child != null) {
                    printWriter.println(("directoryMetaData   ")
                                        + (child.byteAddress + "                    ").substring(0, 20)
                                        + (child.length + "                    ").substring(0, 20)
                                    );
                    if (storeArea.next.getKey() == null) {
                        storeArea = (Directory.StoreArea) storeArea.next;
                        child = (Directory.Node) storeArea.getChild();
                        printWriter.println(("directoryMetaData   ")
                                            + (child.byteAddress + "                    ").substring(0, 20)
                                            + (child.length + "                    ").substring(0, 20)
                                        );
                    } // if (storeArea.next.getKey() == null).                    
                } // if (child != null).
            } // for ... directory.
        } catch (ObjectManagerException objectManagerException) {
            // No FFDC code needed.
            printWriter.println("Caught objectManagerException=" + objectManagerException);
            objectManagerException.printStackTrace(printWriter);
        } // try...

        printWriter.println("cachedManagedObjects...");
        for (int i = 0; i < cachedManagedObjects.length; i++) {
            if (cachedManagedObjects[i] != null)
                printWriter.println(i + " " + cachedManagedObjects[i].toString());
        } // for cachedManagedObjects.

        printWriter.println("freeSpaceByLength.size()=" + freeSpaceByLength.size() + "\n... Address(long) Length(Long)");
        for (java.util.Iterator iterator = freeSpaceByLength.iterator(); iterator.hasNext();) {
            FreeSpace freeSpace = (FreeSpace) iterator.next();
            printWriter.println(freeSpace.address + " " + freeSpace.length);
        } // for freeSpaceAddress.

        printWriter.println("newFreeSpace.size()=" + newFreeSpace.size() + "\n... Address(long) Length(Long)");
        for (java.util.Iterator freeSpaceIterator = newFreeSpace.iterator(); freeSpaceIterator.hasNext();) {
            Directory.StoreArea storeArea = (Directory.StoreArea) freeSpaceIterator.next();
            printWriter.println(storeArea.byteAddress + " " + storeArea.length);
        } // for newFreeSpace.

    } // print().

    /*
     * Validates that the store space is not corrupt.
     * Synchronized to prevent update while we are validating.
     * 
     * @return boolean true if the store is not corrupt.
     */
    public synchronized boolean validate(java.io.PrintStream printStream)
                    throws ObjectManagerException
    {
        final String methodName = "validate";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { printStream });

        boolean valid = true; // Until proven otherwise.

        valid = directory.validate(printStream);

        // Build a map of store areas sorted on the start byteAddress.
        java.util.TreeMap storeAreas = new java.util.TreeMap();

        Directory.StoreArea storeArea;
        Directory.StoreArea existing;
        // Load the directory contents.
        for (Iterator directoryIterator = directory.entrySet().iterator(); directoryIterator.hasNext();) {
            storeArea = (Directory.StoreArea) directoryIterator.next();
            existing = (Directory.StoreArea) storeAreas.put(new Long(storeArea.byteAddress),
                                                            storeArea);
            if (existing != null) {
                printStream.println("storeArea " + storeArea + " payload reused byte Address"
                                    + " storeArea.byteAddress=" + storeArea.byteAddress
                                    + " storeArea.length=" + storeArea.length
                                    + " existing.length=" + existing.length
                                    + " existing.identifier=" + existing.identifier
                                );
                valid = false;
            } // if (existing != null).

            Directory.Node child = (Directory.Node) storeArea.getChild();
            if (child != null) {
                // Fake up another store area to mimick any space used by the BTree metadata contain any child below.
                Directory.StoreArea metaData = directory.makeStoreArea(0, child.byteAddress, child.length);
                existing = (Directory.StoreArea) storeAreas.put(new Long(metaData.byteAddress),
                                                                metaData);

                if (existing != null) {
                    printStream.println("storeArea " + storeArea + " child reused byte Address"
                                        + " child.byteAddress=" + child.byteAddress
                                        + " child.length=" + child.length
                                        + " existing.length=" + existing.length
                                        + " existing.identifier=" + existing.identifier
                                    );
                    valid = false;
                } // if (existing != null).

                // We have a child and so are not a leaf node, check for a final greater than entry.
                if (storeArea.next.getKey() == null) {

                    storeArea = (Directory.StoreArea) storeArea.next;
                    child = (Directory.Node) storeArea.getChild();
                    metaData = directory.makeStoreArea(0, child.byteAddress, child.length);
                    existing = (Directory.StoreArea) storeAreas.put(new Long(metaData.byteAddress),
                                                                    metaData);
                    if (existing != null) {
                        printStream.println("storeArea " + storeArea + " greater than child reused byte Address"
                                            + " child.byteAddress=" + child.byteAddress
                                            + " child.length=" + child.length
                                            + " existing.length=" + existing.length
                                            + " existing.identifier=" + existing.identifier
                                        );
                        valid = false;
                    } //  if (existing != null).

                } // if (storeArea.next.getKey() == null).                    
            } // if (child != null).
        } // For ... directory.

        // Add in the root of the directory.
        if (((Directory.Node) directory.root).length != 0) {
            storeArea = directory.makeStoreArea(0,
                                                ((Directory.Node) directory.root).byteAddress,
                                                ((Directory.Node) directory.root).length);
            existing = (Directory.StoreArea) storeAreas.put(new Long(storeArea.byteAddress),
                                                            storeArea);
            if (existing != null) {
                printStream.println("storeArea " + storeArea + " directory root reused byte Address"
                                    + " storeArea.byteAddress=" + storeArea.byteAddress
                                    + " storeArea.length=" + storeArea.length
                                    + " existing.length=" + existing.length
                                    + " existing.identifier=" + existing.identifier
                                );
                valid = false;
            } // if (existing != null)  
        } // (((Directory.Node)directory.root).length != 0).

        // Add the free space.
        int freeSpaceCount = 0;
        FreeSpace freeSpace = freeSpaceByAddressHead;
        FreeSpace prevFreeSpace = null;
        while (freeSpace != null) {
            storeArea = directory.makeStoreArea(0,
                                                freeSpace.address,
                                                freeSpace.length);
            existing = (Directory.StoreArea) storeAreas.put(new Long(storeArea.byteAddress),
                                                            storeArea);
            if (existing != null) {
                printStream.println("freeSpace storeArea " + storeArea + " reused byte Address"
                                    + " storeArea.byteAddress=" + storeArea.byteAddress
                                    + " storeArea.length=" + storeArea.length
                                    + " existing.length=" + existing.length
                                    + " existing.identifier=" + existing.identifier
                                );
                valid = false;
            } // if (existing != null).

            // Check the list is structurally sound
            if (freeSpace.prev != prevFreeSpace) {
                printStream.println("freeSpaceByAddress list invalid" +
                                    " prevFreeSpace=" + prevFreeSpace +
                                    " freeSpace.prev=" + freeSpace.prev);
            }

            // Check the list is ordered correctly
            if (prevFreeSpace != null && freeSpace.address <= prevFreeSpace.address) {
                printStream.println("freeSpaceByAddress list out of order" +
                                    " freeSpace.address=" + freeSpace.address +
                                    " freeSpace.length=" + freeSpace.length +
                                    " prevFreeSpace.address=" + prevFreeSpace.address +
                                    " prevFreeSpace.length=" + prevFreeSpace.length);
            }

            prevFreeSpace = freeSpace;
            freeSpace = freeSpace.next;
            freeSpaceCount++;
        } // for freeSpaceCount.

        // Check the size of the two views of the free space match
        if (freeSpaceCount != freeSpaceByLength.size()) {
            printStream.println("freeSpaceByAddress.size()=" + freeSpaceCount +
                                " freeSpaceByLength.size()=" + freeSpaceByLength.size());
            valid = false;
        }

        // Add in the freeSpaceStoreArea.
        if (freeSpaceStoreArea != null && freeSpaceStoreArea.length != 0) {
            existing = (Directory.StoreArea) storeAreas.put(new Long(freeSpaceStoreArea.byteAddress),
                                                            freeSpaceStoreArea);
            if (existing != null) {
                printStream.println("freeSpaceStoreArea " + freeSpaceStoreArea + " reused byte Address"
                                    + " freeSpaceStoreArea.byteAddress=" + freeSpaceStoreArea.byteAddress
                                    + " freeSpaceStoreArea.length=" + freeSpaceStoreArea.length
                                    + " existing.length=" + existing.length
                                    + " existing.identifier=" + existing.identifier
                                );
                valid = false;
            } // if (existing != null).  
        } // if (freeSpaceStoreArea != null && freeSpaceStoreArea.length != 0).

        // Move through the StoreAreas accounting for all of the space in the file
        // as we go. Start with the header as the previous StoreArea.
        Directory.StoreArea previous = directory.makeStoreArea(0, 0, pageSize * 2);
        for (java.util.Iterator storeAreasIterator = storeAreas.values().iterator(); storeAreasIterator.hasNext();) {
            storeArea = (Directory.StoreArea) storeAreasIterator.next();
            long gap = storeArea.byteAddress - (previous.byteAddress + previous.length);
            if (gap > 0) {
                printStream.println("previous.byteAddress=" + previous.byteAddress
                                    + " previous.length=" + previous.length
                                    + " gap=" + gap
                                    + " storeArea.byteAddress=" + storeArea.byteAddress
                                    + " storeArea.length=" + storeArea.length
                                );
                valid = false;
            } else if (gap < 0) {
                printStream.println("previous.byteAddress=" + previous.byteAddress
                                    + " previous.length=" + previous.length
                                    + " overlap=" + (-gap)
                                    + " storeArea.byteAddress=" + storeArea.byteAddress
                                    + " storeArea.length=" + storeArea.length
                                );
                valid = false;
            }
            previous = storeArea;
        } // For ... storeAreas.

        if (storeFileSizeUsed != (previous.byteAddress + previous.length)) {
            printStream.println("storeFileLength not valid"
                                + " previous.byteAddress + previous.length="
                                + (previous.byteAddress + previous.length)
                                + " storeFileSizeUsed=" + storeFileSizeUsed);
            valid = false;
        } // if (storeFileSizeUsed != (previous.byteAddress + previous.length)).  

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { new Boolean(valid) });
        return valid;
    } // validate().

    private transient Set tokenSet; // Initialised if used.

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#tokens()
     */
    public Set tokens() {
        if (tokenSet == null) {
            tokenSet = new AbstractSetView() {

                public long size() throws ObjectManagerException {
                    return directory.size();
                } // size().

                public Iterator iterator() throws ObjectManagerException {

                    final Iterator storeAreaIterator = directory.entrySet().iterator();
                    return new Iterator() {

                        public boolean hasNext() throws ObjectManagerException
                        {
                            return storeAreaIterator.hasNext();
                        } // hasNext().

                        public Object next() throws ObjectManagerException
                        {
                            Directory.StoreArea storeArea = (Directory.StoreArea) storeAreaIterator.next();
                            Token token = new Token(AbstractSingleFileObjectStore.this, storeArea.identifier);
                            return like(token);
                        } // next().   

                        public boolean hasNext(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            throw new UnsupportedOperationException();
                        } // hasNext().

                        public Object next(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            throw new UnsupportedOperationException();
                        } // next().

                        public Object remove(Transaction transaction)
                                        throws ObjectManagerException
                        {
                            throw new UnsupportedOperationException();
                        } // remove().
                    }; // new Iterator().
                } // iterator().  

            }; // new AbstractSetView().
        } // if (tokenSet == null). 
        return tokenSet;
    } // tokens().

    /**
     * Create the directory, either by reading it from disk or a new empty directory.
     * 
     * @param minimumNodeSize the minimum size of a directory Node.
     * @param directoryRootByteAddress the directory root byte address.
     * @param directoryRootLength the number of bytes in the direcory root, or zero if the root
     *            does not exist in the file.
     * @return Directory read from disk.
     * 
     * @throws ObjectManagerException
     */
    abstract Directory makeDirectory(int minimumNodeSize,
                                     long directoryRootByteAddress,
                                     long directoryRootLength)
                    throws ObjectManagerException;

    /**
     * Writes a ManagedObject to hardened storage, but may return before the write
     * completes.
     * 
     * @param managedObject to be written.
     * @throws ObjectManagerException
     */
    abstract void write(ManagedObject managedObject)
                    throws ObjectManagerException;

    /**
     * Write the freeSpaceMap to the space allocated to the freeSpaceStoreArea.
     * 
     * @throws ObjectManagerException
     */
    abstract void writeFreeSpace()
                    throws ObjectManagerException;

    /**
     * Writes the fileStore header and forces the disk if necessary.
     * 
     * @throws ObjectManagerException
     */
    abstract void writeHeader()
                    throws ObjectManagerException;

    /**
     * Force updates to disk.
     * 
     * @throws ObjectManagerException
     */
    abstract void force()
                    throws ObjectManagerException;

    // --------------------------------------------------------------------------
    // Inner classes.
    // --------------------------------------------------------------------------

    class FreeSpace {
        long length;
        long address;
        FreeSpace prev;
        FreeSpace next;

        FreeSpace(long address, long length) {
            this.length = length;
            this.address = address;
        }
    } // inner class FreeSpace.

    /**
     * Comparator to compare address parts only of a FreeSpace object.
     */
    class AddressComparator
                    implements java.util.Comparator
    {
        /*
         * (non-Javadoc)
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object arg0,
                           Object arg1) {
            FreeSpace freeSpace0 = (FreeSpace) arg0;
            FreeSpace freeSpace1 = (FreeSpace) arg1;

            if (freeSpace0.address < freeSpace1.address) {
                return -1;
            }
            else if (freeSpace0.address > freeSpace1.address) {
                return 1;
            }
            else {
                return 0;
            }
        } // compare().    
    } // inner class AddressComparator.

    /**
     * Comparator to compare Length of FreeSPace Objects, if the lengths are equal, then compare adresses.
     */
    class LengthComparator
                    implements java.util.Comparator
    {
        /*
         * (non-Javadoc)
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object arg0,
                           Object arg1) {
            FreeSpace freeSpace0 = (FreeSpace) arg0;
            FreeSpace freeSpace1 = (FreeSpace) arg1;

            if (freeSpace0.length < freeSpace1.length)
                return -1;
            else if (freeSpace0.length > freeSpace1.length)
                return 1;
            else {
                // If the length are equal, compare by address
                if (freeSpace0.address < freeSpace1.address) {
                    return -1;
                }
                else if (freeSpace0.address > freeSpace1.address) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        } // compare().    
    } // inner class LengthComparator.

    /**
     * @author Andrew_Banks
     * 
     *         Manages the in memory and on disk map of identifiers to StoreAreas as a B-Tree.
     * 
     *         Each Node in the Btree is a block of Store Areas.
     *         TODO This implementation is
     *         thread safe for readers, but only one thread is safe for one update at a time.
     */
    abstract class Directory
                    extends BTree
    {
        private final Class cclass = AbstractSingleFileObjectStore.Directory.class;

        /**
         * @param minimumNodeSize the minimum size of a directory Node.
         * @param directoryRootByteAddress the directory root byte address.
         * @param directoryRootLength the number of bytes in the direcory root, or zero if the root
         *            does not exist in the file.
         * 
         * @throws ObjectManagerException
         */
        Directory(int minimumNodeSize
                  , long directoryRootByteAddress
                  , long directoryRootLength)
            throws ObjectManagerException
        {
            super(minimumNodeSize);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "<init>",
                            new Object[] { new Integer(minimumNodeSize), new Long(directoryRootByteAddress), new Long(directoryRootLength) }
                                );

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>");
        } // Directory().

        /**
         * Allocate space in the file for update parts of the directory.
         * Release any new unused space into newFreeSpace,
         * pending its actual release.
         * 
         * Not synchronized because flush() is single threaded and this does not alter the
         * tree structure so getters from the tree are not affected.
         * 
         * @throws ObjectManagerException
         */
        void reserveSpace()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "reserveSpace");

            ((Directory.Node) root).reserveSpace();

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "reserveSpace"
                                );
        } // reserveSpace().

        /**
         * An overestimate of the space required on disk to store the directory
         * including enough space in the free space map to completely replace it.
         * Estimating the space for the directory in this way is fast but means
         * that there is a large jump in the estimate each time a new child is
         * added to the root.
         * 
         * @return long an over estimate to store and then delete the directory.
         * @throws ObjectManagerException
         */
        long spaceRequired()
                        throws ObjectManagerException {
            final String methodName = "spaceRequired";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName);

            // Assume a worst case of all nodes being full.
            final int worstCaseNodeSize = (getMinimumNodeSize() * 2) * (5 * 8) // Space for all Entries. 
                                          + 4 + 1 // Node header, Length and flag.
                                          + 2 * 8; // FreeSpace entry, needed if the node is rewritten or deleted.
            long nodesAtThisLevel = 1;
            long spaceRequired = 0;
            for (Node node = (Node) root; node != null;) {
                spaceRequired = spaceRequired + nodesAtThisLevel * worstCaseNodeSize;
                // We are at the root node use the actual number of children, otherwise
                // assume the maximum possible.
                if (nodesAtThisLevel == 1)
                    nodesAtThisLevel = node.numberOfKeys;
                else
                    nodesAtThisLevel = nodesAtThisLevel * getMinimumNodeSize() * 2;

                if (node.first == null)
                    node = null;
                else
                    node = (Node) node.first.getChild();
            } // for...

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { new Long(spaceRequired) });
            return spaceRequired;
        } // spaceRequired().

        /**
         * Write the updated parts of the directory to the file in the space previously allocated. Release any unreferenced
         * sub directories from storage to free their virtual memory.
         * 
         * Not synchronized because flush() is single threaded and this does not alter the tree structure so getterrs from
         * the tree are not affected.
         * 
         * @throws ObjectManagerException
         */
        void write()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "write");

            ((Directory.Node) root).write();

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "write"
                                );
        } // reserveSpace().

        // --------------------------------------------------------------------------
        // extends BTree.
        // --------------------------------------------------------------------------

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.BTree#setRoot(com.ibm.ws.objectManager.BTree.Node)
         */
        void setRoot(Node newRoot)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "setRoot",
                            new Object[] { newRoot });

            // The root directory node location is always written as part of the header,
            // so there is no need to remember that it has been changed.
            super.setRoot(newRoot);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "setRoot");
        } // setRoot().

        /**
         * Construct a Node for the superclass BTree.
         * 
         * @param parent The parent of this node.
         * @return Node constructed, has no storage allocated.
         * @throws ObjectManagerException
         */
        abstract BTree.Node makeNode(BTree.Node parent)
                        throws ObjectManagerException;

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.BTree#makeEntry(java.lang.Object, java.lang.Object)
         */
        abstract BTree.Entry makeEntry(Object key, Object value)
                        throws ObjectManagerException;

        /**
         * Construct a StoreArea which can be put into this directory.
         * 
         * @param identifier the identifier of the StoreArea.
         * @param byteAddress the byte offset of the object in the file.
         * @param length the number of bytes in the serialised object written on the disk.
         * @return StoreArea which can be put into this Directory.
         * @throws ObjectManagerException
         */
        abstract StoreArea makeStoreArea(long identifier,
                                         long byteAddress,
                                         long length)
                        throws ObjectManagerException;

        // --------------------------------------------------------------------------
        // Inner classes.
        // --------------------------------------------------------------------------

        abstract class Node
                        extends BTree.Node
        {
            private final Class cclass = AbstractSingleFileObjectStore.Directory.Node.class;

            // Indicator written to the file to signal isLeaf;
            static final byte flagIsLeaf = 0;
            static final byte flagIsNotLeaf = 1;

            // True if this Node has been modified since the last write to disk.
            boolean modified = false;
            // Reduced each time we flush the directory, when it reaches zero we drop the Node and will
            // reload it from the file if required.  2 is the minimum initialDormantFlushCount because 
            // decrementing and then nulling takes place before the checkpoint is complete.  If it is 1,
            // then the StoreArea is nulled out before it is written. 
            int dormantFlushCount;
            static final int initialDormantFlushCount = 2;

            // The file area where this node is store, zero length if it has not yet been written.
            long byteAddress = 0;
            long length = 0;

            /**
             * This constructor creates an empty leaf node.
             * 
             * @param parent of the new Node.
             */
            Node(Node parent)
            {
                super(parent);
            } // Node().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.BTree.Node#size()
             */
            public long size()
                            throws ObjectManagerException {
                final String methodName = "size";
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass, methodName);

                long size;
                Entry entry = first;

                if (isLeaf) {
                    size = numberOfKeys;

                } else {
                    // Allow for the final entry having no key value pair, just a child.
                    size = -1;
                    while (entry != null) {
                        boolean childWasNull = (entry.child == null);
                        size = size + entry.getChild().size();
                        // If we just set up the child then un set it we don't want to retain node storage just because we
                        // asked the size because this causes large stores to run out of storage.
                        if (childWasNull)
                            entry.child = null;
                        size = size + 1;
                        entry = entry.next;
                    } // while...
                }

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Object[] { new Long(size) });
                return size;
            }

            /**
             * Allocate space in the file for his subdirectory and any child subDirectories.
             * Release any new unused space into newFreeSpace,
             * pending its actual release.
             * 
             * Release any unreferenced sub directories from storage to free their virtual memory.
             * 
             * @throws ObjectManagerException
             * 
             */
            void reserveSpace()
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "reserveSpace",
                                new Object[] { new Boolean(modified), new Integer(dormantFlushCount) });

                // Allow child directories to reserve space.
                if (!isLeaf) {
                    for (StoreArea storeArea = (StoreArea) first; storeArea != null; storeArea = (StoreArea) storeArea.next) {
                        // If the child is not loaded it can't have been modified.
                        if (storeArea.child != null) {
                            ((Node) storeArea.child).reserveSpace();
                            if (((Node) storeArea.child).dormantFlushCount == 0) {

                                // Update the child location we will write.
//         storeArea.childByteAddress = ((Node)storeArea.child).byteAddress;
//         storeArea.childLength = ((Node)storeArea.child).length;
//       Release any unreferenced sub directories from storage to free their virtual memory.
                                storeArea.childSoftReference = new java.lang.ref.SoftReference(storeArea.child);
                                storeArea.child = null;
                            } // if (((Node)storeArea.child).dormantFlushCount == 0). 
                        } // if (storeArea.child != null).
                    } // for...
                } // if (!isLeaf).
                dormantFlushCount--;

                if (modified) {
                    // Release any space we currently hold.
                    if (length != 0) {
                        newFreeSpace.add(makeStoreArea(0,
                                                       byteAddress,
                                                       length));
                    } // if ( length != 0).

                    FreeSpace freeSpace = allocateSpace(writtenSize());
                    byteAddress = freeSpace.address;
                    length = freeSpace.length;

                    // The modified flag is cleared when we do the write.
                } // if (modified).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "reserveSpace"
                                    );
            } // reserveSpace().

            /**
             * Calculate the bytes needed in the file to store this Node.
             * 
             * @return the number of bytes written when this node is stored in a file.
             */
            long writtenSize()
            {
                long size;
                if (isLeaf)
                    size = 4 // numberOfKeys.
                           + 1 //flagIsLeaf/flagIsNotLeaf.
                           + numberOfKeys * 3 * 8; //Identifier,address,length.
                else
                    size = 4 // numberOfKeys.
                           + 1 //flagIsLeaf/flafIsNotLeaf.
                           + (numberOfKeys * 5 * 8) //Identifier,address,length,childAddress,childLength.
                           + 2 * 8; // Final childAddress,childLength.

                // For debugging add guardBytes before and after the Node metadata.
                if (useGuardBytes) {
                    size = size + 2 * guardBytesLength;
                } // if (useGuardBytes).

                return size;
            } // writtenSize().

            /**
             * Write this sub directory and any child nodes that have been updated
             * in the space previously allocated.
             * Release any unreferenced sub directories from storage to free their virtual memory.
             * 
             * @throws ObjectManagerException
             */
            abstract void write()
                            throws ObjectManagerException;

            // --------------------------------------------------------------------------
            // extends BTree.Node.
            // --------------------------------------------------------------------------

            /**
             * Splits a full node into two nodes about its middle key.
             * Any file storage reserved for the node remains with the greater than half as th
             * returned Entry's childByteAddress and childLength.
             * 
             * @return Entry in the the middle, chained to its child, the less than Node.
             * @throws ObjectManagerException
             * 
             */
            Entry split()
                            throws ObjectManagerException
            {
                modified = true;
                // In case we are the root, reset the dormant flushCount as we will be come a child of the new root
                // and no one will have set this as yet.
                dormantFlushCount = Node.initialDormantFlushCount;
                return super.split();
            } // split().

            /**
             * Insert a new Entry into this Node, according to the key in the Entry.
             * The caller ensures that this node is not full when this method is called.
             * 
             * @param newEntry to be inserted.
             * 
             * @return Entry an existing entry for the key contained in the new Entry, or null.
             * @throws ObjectManagerException
             */
            Entry insert(Entry newEntry)
                            throws ObjectManagerException
            {
                // Will set modified even if the non leaf node does not require modification. This has to be
                // done so that we rewrite the nodes all the way down to the modified Entry. When we are finished 
                // we will have marked all the nodes from the root to the inserted Entry so that they are all
                // rewritten. When we finnaly rewrite the header to point to the new root node we flip to the
                // new disk version of the tree. 
                modified = true;
                Entry replacedEntry = super.insert(newEntry);
                return replacedEntry;
            } // insert().

            /**
             * Remove a Entry from this node or its descendants.
             * 
             * This descends into the map recursively searching for and acting on
             * nodes that potentially contain the given key. We must ensure that a Node has
             * more than minimumNumberOfKeys-1 before descending into it so that the child can
             * always sustain a deletion. Similrarly the caller must ensure we have sufficient keys to
             * sustain deletion before invoking this method.
             * 
             * @param key to be deleted.
             * @return Entry matching the key or null.
             * @throws ObjectManagerException
             */
            Entry delete(Object key)
                            throws ObjectManagerException
            {
                // Will set modified even if the non leaf node does not require modification. This has to be
                // done so that we rewrite the nodes all the way down to the modified Entry. When we are finished 
                // we will have marked all the nodes from the root to the inserted Entry so that they are all
                // rewritten. When we finnaly rewrite the header to point to the new root node we flip to the
                // new disk version of the tree.
                modified = true;
                Node currentRoot = (Directory.Node) root;
                Entry deletedEntry = super.delete(key);
                if (!isLeaf && numberOfKeys == 0) {
                    // Root has been dropped and a child promoted.
                    // Free the space only if it was actually allocated.
                    if (currentRoot.length != 0)
                        newFreeSpace.add(makeStoreArea(0,
                                                       currentRoot.byteAddress,
                                                       currentRoot.length));
                } // if (!isLeaf && numberOfKeys == 0).
                return deletedEntry;
            } // delete().

            Entry removeLastEntryAndFollowingNode()
                            throws ObjectManagerException
            {
                modified = true;
                return super.removeLastEntryAndFollowingNode();
            } // removeLastEntryAndFollowingNode().

            Entry removeFirstEntryAndPrecedingNode()
            {
                modified = true;
                return super.removeFirstEntryAndPrecedingNode();
            } // removeFirstEntryAndPrecedingNode().

            /**
             * Clear this node and its descendants. Free all storage associated with nodes but not
             * the Entries themselves.
             * 
             * @throws ObjectManagerException
             */
            public void clear()
                            throws ObjectManagerException
            {
                modified = true;
                super.clear();
            } // clear().

        } // inner class Node.

        /**
         * Represents an Object in the store, or free space in the store
         * and any metadata from the child sub directory.
         * 
         * @version 1.00 05 November 2004
         * @author Andrew Banks
         */
        abstract class StoreArea
                        extends BTree.Entry
                        implements Comparable
        {
            private final Class cclass = AbstractSingleFileObjectStore.Directory.StoreArea.class;

            // Worst case assumption about how much file space is needed to store this StoreArea.  
            static final int maximumWrittenSize = 5 * 8;

            // Matches token.storedObjectIdentifier.
            protected long identifier = 0;
            // Start of the payload Object in the store file.
            protected long byteAddress = 0;
            // Number of bytes occupied by the palyload in the store file. 
            protected long length = 0;
            // Start of the child Subdirectory in the store file.
            protected long childByteAddress = 0;
            // Number of bytes in the child sub directory.
            protected long childLength = 0;
            // A SoftReference to the child node, if we decide to release the strong reference.
            java.lang.ref.SoftReference childSoftReference;

            /**
             * Creates a StoreArea representing space used in the store file. Used by
             * BTree to create empty Entries.
             * 
             * @param key (Long) storedObjectIdentifier for the new StoreArea.
             * @param value always null.
             */
            StoreArea(Object key,
                      Object value)
            {
                super(key, value);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "<init>");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "<init>");
            } // end of Constructor.

            /**
             * Creates a StoreArea representing space used in the store file .
             * 
             * @param identifier the identifier of the StoreArea.
             * @param byteAddress the byte offset of the object in the file.
             * @param length the number of bytes in the serialised object written on the
             *            disk.
             */
            StoreArea(long identifier,
                      long byteAddress,
                      long length)
            {
                super(new Long(identifier), null);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "<init>",
                                new Object[] { new Long(identifier), new Long(byteAddress), new Long(length) });

                this.identifier = identifier;
                this.byteAddress = byteAddress;
                this.length = length;

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "<init>");
            } // end of Constructor.

            long getChildByteAddress()
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "getChildByteAddress");

                long address;
                if (child == null)
                    address = childByteAddress;
                else
                    address = ((Node) child).byteAddress;

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "getChildByteAddress"
                               , new Object[] { new Long(address) }
                                    );

                return address;
            } // getChildByteAddress().

            long getChildLength()
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "getChildLength");

                long length;
                if (child == null)
                    length = childLength;
                else
                    length = ((Node) child).length;

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "getChildLength"
                               , new Object[] { new Long(length) }
                                    );

                return length;
            } // getChildByteAddress().

            // --------------------------------------------------------------------------
            // extends BTree.Entry.
            // --------------------------------------------------------------------------

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.BTree.Entry#setChild(com.ibm.ws.objectManager.BTree.Node)
             */
            void setChild(BTree.Node child)
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "setChild",
                                new Object[] { child });

                super.setChild(child);
                if (child == null) {
                    childByteAddress = 0;
                    childLength = 0;
                } else {
                    childByteAddress = ((Node) child).byteAddress;
                    childLength = ((Node) child).length;
                }

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "setChild"
                               , new Object[] { new Long(childByteAddress), new Long(childLength) }
                                    );
            } // setChild().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.BTree.Entry#mergeChildWithGreaterThanChild()
             */
            void mergeChildWithGreaterThanChild()
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "mergeChildWithGreaterThanChild");

                // Release any space the greater than child Node currently holds as it will be dropped.
                Node dropped = (Node) next.getChild();
                if (dropped.length != 0) {
                    newFreeSpace.add(makeStoreArea(0,
                                                   dropped.byteAddress,
                                                   dropped.length));
                } // if (dropped.length != 0).   
                super.mergeChildWithGreaterThanChild();

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "mergeChildWithGreaterThanChild",
                               new Object[] { dropped }
                                    );
            } // mergeChildWithGreaterThanChild().

            // --------------------------------------------------------------------------
            // implements Comparable.
            // --------------------------------------------------------------------------

            /**
             * Compare two store areas by byte address.
             * Used when store areas are placed in a sorted set.
             */
            public int compareTo(Object other) {
                long diff = byteAddress - ((StoreArea) other).byteAddress;
                if (diff < 0)
                    return -1;
                else if (diff > 0)
                    return 1;
                else
                    return 0;
            }

            /**
             * Compare two store areas by byte address.
             * Used when store areas are placed in a sorted set.
             */
            public boolean equals(Object other) {
                return ((StoreArea) other).byteAddress == byteAddress;
            }

            // --------------------------------------------------------------------------
            // extends Object.
            // --------------------------------------------------------------------------

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Object#toString()
             */
            public String toString()
            {
                return new String("Directory.StoreArea"
                                  + "(identifier=" + identifier + " byteAddress=" + byteAddress + " length=" + length + ")"
                                  + " " + super.toString());
            } // toString().
        } // inner class StoreArea.

    } // inner class Directory.

} // End of class SingleFileObjectStore.
