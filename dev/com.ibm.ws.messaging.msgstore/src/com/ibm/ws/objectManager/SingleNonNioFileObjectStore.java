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
 * Implements a SingleFileObjectStore not using java.nio.
 * You can create this type of store on a system that supports java.nio, but,
 * if you restart an ObjectManager using this kind of store on a system that does
 * support java.nio, it will be migrated to a SingleFileObjectStore.
 * 
 * @version @(#) 1/25/13
 * @author IBM Corporation
 */
public final class SingleNonNioFileObjectStore
                extends AbstractSingleFileObjectStore
{
    private static final Class cclass = SingleNonNioFileObjectStore.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_STORE);

    private static final long serialVersionUID = 4753669053373190961L;

    // To avoid conflicts between seek/read and seek/write these operations are carried out
    // under a synchronize block. This is not necessary with java.nio because it is synchronized
    // internally. Using a separate file handle to read for the read operations does not work
    // as sometimes incorrect data is read, even though it has already been written.

    // Create a buffer for writing data other than the ManagedObjects themselves.
    private transient ObjectManagerByteArrayOutputStream byteArrayOutputStream;

    /**
     * Constructor
     * 
     * @param storeName Identifies the ObjecStore and the file directory.
     * @param objectManager that manages this store.
     * @throws ObjectManagerException
     */
    public SingleNonNioFileObjectStore(String storeName,
                                       ObjectManager objectManager)
        throws ObjectManagerException
    {
        super(storeName,
              objectManager,
              STRATEGY_KEEP_ALWAYS); // Invoke the SuperClass constructor.

        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { storeName, objectManager });
            trace.exit(this,
                       cclass,
                       methodName);
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).
    } // End of constructor.

    /**
     * Constructor
     * 
     * @param storeName that identifies the ObjecStore and the file directory.
     * @param objectManager that manages this store.
     * @param storeStrategy one of STRATEGY_XXX:
     * @throws ObjectManagerException
     */
    public SingleNonNioFileObjectStore(String storeName,
                                       ObjectManager objectManager,
                                       int storeStrategy)
        throws ObjectManagerException {
        super(storeName,
              objectManager,
              storeStrategy); // Invoke the SuperClass constructor.

        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { storeName, objectManager, new Integer(storeStrategy) });
            trace.exit(this,
                       cclass,
                       methodName);
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).
    } // End of constructor.

    // --------------------------------------------------------------------------
    // extends AbstractSingleFileObjectStore.
    // --------------------------------------------------------------------------

    /**
     * Establish the transient state of this ObjectStore.
     * 
     * @param objectManagerState with which the ObjectStore is registered.
     * @throws ObjectManagerException
     */
    public synchronized void open(ObjectManagerState objectManagerState)
                    throws ObjectManagerException {
        final String methodName = "open";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, objectManagerState);

        super.open(objectManagerState);

        byteArrayOutputStream = new ObjectManagerByteArrayOutputStream(pageSize);

        // Now look at the file and initialise the ObjectStore.
        // Read the header, to find the directory and free space map.
        try {
            int versionRead = storeFile.readInt();

            // Read a known number of signature charaters.
            char[] signatureRead = new char[signature.length()];
            for (int i = 0; i < signature.length(); i++)
                signatureRead[i] = storeFile.readChar();
            if (!(new String(signatureRead).equals(signature))) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "open"
                               , new Object[] { signatureRead });
                throw new InvalidStoreSignatureException(this,
                                                         new String(signatureRead), signature);
            } // if(!(new String(signatureRead).equals(signature))).

            long objectStoreIdentifierRead = storeFile.readLong();
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            methodName,
                            new Object[] { new Integer(versionRead),
                                          new String(signatureRead),
                                          new Long(objectStoreIdentifierRead)
                            }
                                );
            // Use the saved value if we are a new store.
            if (objectStoreIdentifier == IDENTIFIER_NOT_SET)
                objectStoreIdentifier = (int) objectStoreIdentifierRead;

            // The byte address of the directory root.
            long directoryRootByteAddress = storeFile.readLong();
            // The current number of bytes in the directory root.
            long directoryRootLength = storeFile.readLong();
            // The minimumNodeSize of the directory BTree.
            int directoryMinimumNodeSize = storeFile.readInt();
            long freeSpaceStoreAreaByteAddress = storeFile.readLong();
            long freeSpaceStoreAreaLength = storeFile.readLong();
            // The actual number of free space entries since we have to overestimate the space we need to store them.
            long freeSpaceCount = storeFile.readLong();

            sequenceNumber = storeFile.readLong();
            // This is the length we assume we have written.
            storeFileSizeUsed = storeFile.readLong();
            // Administered limits on store file size.
            minimumStoreFileSize = storeFile.readLong();
            maximumStoreFileSize = storeFile.readLong();
            cachedManagedObjectsSize = storeFile.readInt();

            storeFileSizeAllocated = storeFile.length();

            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            methodName,
                            new Object[] { new Long(directoryRootByteAddress)
                                          , new Long(directoryRootLength)
                                          , new Integer(directoryMinimumNodeSize)
                                          , new Long(freeSpaceStoreAreaByteAddress)
                                          , new Long(freeSpaceStoreAreaLength)
                                          , new Long(freeSpaceCount)
                                          , new Long(sequenceNumber)
                                          , new Long(storeFileSizeUsed)
                                          , new Long(minimumStoreFileSize)
                                          , new Long(maximumStoreFileSize)
                                          , new Integer(cachedManagedObjectsSize)
                                          , new Long(storeFileSizeAllocated)
                            }
                                );

            // Are we interested in what's already on the disk?
            if (storeStrategy == STRATEGY_KEEP_ALWAYS
                || storeStrategy == STRATEGY_SAVE_ONLY_ON_SHUTDOWN) {
                // Create the array that locks recently used ManagedObjects into memory.
                cachedManagedObjects = new java.lang.ref.SoftReference[cachedManagedObjectsSize];
                directory = new Directory(directoryMinimumNodeSize,
                                          directoryRootByteAddress,
                                          directoryRootLength);
                if (freeSpaceStoreAreaByteAddress != 0)
                    freeSpaceStoreArea = directory.makeStoreArea(freeSpaceIdentifier,
                                                                 freeSpaceStoreAreaByteAddress,
                                                                 freeSpaceStoreAreaLength);
                readFreeSpaceMap(freeSpaceCount);

            } else if (storeStrategy == STRATEGY_KEEP_UNTIL_NEXT_OPEN) {
                if (Runtime.getRuntime().totalMemory() < Integer.MAX_VALUE)
                    reservedSize = new Atomic32BitLong(0);
                else
                    reservedSize = new Atomic64BitLong(0);
                clear();
            } // if (storeStrategy...

        } catch (java.io.EOFException exception) {
            // The file is empty or did not exist.
            // No FFDC Code Needed, this may be first use of a store.
            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                trace.event(this,
                            cclass,
                            methodName,
                            exception);

            if (objectManagerState.getObjectManagerStateState() == ObjectManagerState.stateReplayingLog) {
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:241:1.35");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Object[] { "EndOfFile:244" });
                throw new PermanentIOException(this,
                                               exception);
            } // if.

            // Set initial values.
            minimumStoreFileSize = 0;
            maximumStoreFileSize = Long.MAX_VALUE;
            cachedManagedObjectsSize = initialCachedManagedObjectsSize;
            cachedManagedObjects = new java.lang.ref.SoftReference[cachedManagedObjectsSize];

            directory = new Directory(initialDirectoryMinimumNodeSize, 0, 0);
            freeSpaceByAddressHead = null;
            freeSpaceByLength = new java.util.TreeSet(new AbstractSingleFileObjectStore.LengthComparator());
            freeSpaceStoreArea = null;

            // Sequence numbers 0-200 are reserved.
            sequenceNumber = initialSequenceNumber;

            // Allow for two headers to be written at the front of the file.
            storeFileSizeUsed = pageSize * 2;
            storeFileSizeAllocated = storeFileSizeUsed;
            // allocateSpace() is now initialized,

            // Make sure there is a valid header on the disk, in case we fail after this.
            // It is fatal for the ObjectManager to fail after we extend the file system
            // in setAllocationAllowed() without writing a header as the header will contain zeros.
            writeHeader();
            force();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:276:1.35");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName);
            throw new PermanentIOException(this,
                                           exception);
        } //  catch (java.io.IOException exception).

        directoryReservedSize = directory.spaceRequired();
        if (Runtime.getRuntime().totalMemory() < Integer.MAX_VALUE)
            reservedSize = new Atomic32BitLong(0);
        else
            reservedSize = new Atomic64BitLong(0);

        // Assume we have already reserved enough space to completely rewrite the directory so any number of
        // node splits or deletions are already accounted for. We now need to say how much space is needed
        // each time an object is added or replaced in the store. This will include enough space to also 
        // complete its removal.
        addSpaceOverhead = 4 + 1 // Space for a new Node, numberOfKeys + flagIsLeaf/flagIsNotLeaf.
                           + 5 * 8 // Space for a new Entry, Identifier,address,length,childAddress,childLength.
                           + 2 * 8 // An entry in the free space map for the node when it is rewritten.
                           + 2 * 8; // An entry in the free space map when the ManagedObject is removed.  
        // For debugging add guardBytes before and after the Node metadata and  before and after the payload.
        if (useGuardBytes)
            addSpaceOverhead = addSpaceOverhead + 2 * 2 * guardBytesLength;

        setAllocationAllowed();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // open().

    /**
     * Load the free space map from disk.
     * 
     * @param freeSpaceCount number of FreeSpace areas to be read.
     * @throws ObjectManagerException
     */
    private void readFreeSpaceMap(long freeSpaceCount)
                    throws ObjectManagerException
    {
        final String methodName = "readFreeSpaceMap";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { new Long(freeSpaceCount) });

        freeSpaceByAddressHead = null;
        freeSpaceByLength = new java.util.TreeSet(new AbstractSingleFileObjectStore.LengthComparator());
        // Load the free space map.
        if (freeSpaceStoreArea != null) {
            try {
                storeFile.seek(freeSpaceStoreArea.byteAddress);
                FreeSpace freeSpaceByAddressTail = null;
                for (int i = 0; i < freeSpaceCount; i++) {
                    AbstractSingleFileObjectStore.FreeSpace freeSpace =
                                    new AbstractSingleFileObjectStore.FreeSpace(storeFile.readLong(), storeFile.readLong());

                    // If we are switching to using a sorted free space map, then
                    // we may find the map we load from the store is unsorted.
                    // In this case we take a hit on sorting it during the first
                    // startup after it is enabled.
                    if (freeSpaceByAddressTail != null && freeSpace.address < freeSpaceByAddressTail.address) {
                        FreeSpace sortEntry = freeSpaceByAddressHead;
                        while (sortEntry != null) {
                            // Have we just gone past where this entry should be inserted?
                            if (sortEntry.address > freeSpace.address) {
                                // Add it before the current entry.
                                freeSpace.next = sortEntry;
                                freeSpace.prev = sortEntry.prev;
                                if (sortEntry.prev != null) {
                                    sortEntry.prev.next = freeSpace;
                                }
                                else {
                                    freeSpaceByAddressHead = freeSpace;
                                }
                                sortEntry.prev = freeSpace;
                                break;
                            }
                            sortEntry = sortEntry.next;
                        }
                        // Check we found a place to insert it
                        if (sortEntry == null) {
                            // No FFDC Code Needed.
                            Exception exception = new RuntimeException();
                            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:365:1.35");
                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this,
                                           cclass,
                                           "readFreeSpaceMap");
                            throw new UnexpectedExceptionException(this, exception);
                        }
                        // The tail remains unchanged in this case, as we can't get to this
                        // block unless there is already a tail, and our entry is before it.
                    }
                    // Otherwise we simply add this entry to the tail
                    else {
                        // Are we the first entry?
                        if (freeSpaceByAddressTail == null) {
                            freeSpaceByAddressHead = freeSpace;
                        }
                        else {
                            freeSpace.prev = freeSpaceByAddressTail;
                            freeSpaceByAddressTail.next = freeSpace;
                        }
                        // The tail is now our new entry
                        freeSpaceByAddressTail = freeSpace;
                    }

                    // Add the entry to the length-sorted list 
                    freeSpaceByLength.add(freeSpace);
                } // for directoryCount.

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:395:1.35");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName);

                // cannot continue
                objectManagerState.requestShutdown();
                throw new PermanentIOException(this,
                                               exception);
            } // catch (java.io.IOException exception).
        } // if (freeSpaceStoreArea != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // readFreeSpaceMap().

    /**
     * Retrieve and reinstantiate an object in the store.
     * 
     * @param token representing the object to be retrieved.
     * @return ManagedObject the object from the store or null if there is none.
     * @throws ObjectManagerException
     */
    public ManagedObject get(Token token)
                    throws ObjectManagerException {
        final String methodName = "get";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { token });

        if (gatherStatistics)
            numberOfGetRequests++;

        ManagedObject objectFromStore = null;
        synchronized (this) { // Avoid collisions with write operations.

            Directory.StoreArea storeArea = (Directory.StoreArea) directory.getEntry(new Long(token.storedObjectIdentifier));
            if (storeArea != null) { // Was the object written?
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this,
                                cclass,
                                methodName,
                                new Object[] { storeArea });

                byte[] managedObjectBytes = new byte[(int) storeArea.length];

                try {
                    // Now read the serialized form of the ManagedObject.
                    storeFile.seek(storeArea.byteAddress);
                    storeFile.readFully(managedObjectBytes);

                } catch (java.io.IOException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this,
                                                        cclass,
                                                        methodName,
                                                        exception,
                                                        "1:455:1.35");

                    // check if the file handle is still good
                    try {
                        if (storeFile.getFD().valid())
                        {
                            TemporaryIOException tioe = new TemporaryIOException(this, exception);
                            if (trace.isEntryEnabled())
                                trace.exit(this,
                                           cclass,
                                           "get",
                                           tioe);
                            throw tioe;
                        }
                        else
                        {
                            // there is nothing more we can do really, so request a shutdown
                            // and throw a permanent ioexception.
                            objectManagerState.requestShutdown();

                            PermanentIOException pioe = new PermanentIOException(this, exception);
                            if (trace.isEntryEnabled())
                                trace.exit(this,
                                           cclass,
                                           "get",
                                           pioe);

                            throw pioe;
                        }
                    } catch (java.io.IOException e)
                    {
                        // there is nothing more we can do really, so request a shutdown
                        // and throw a permanent ioexception.
                        objectManagerState.requestShutdown();

                        PermanentIOException pioe = new PermanentIOException(this, exception);
                        if (trace.isEntryEnabled())
                            trace.exit(this,
                                       cclass,
                                       "get",
                                       pioe);

                        throw pioe;
                    }

                } // catch (java.io.IOException exception).

                // For debugging check guardBytes added before and after the serialized ManagedObject.
                if (useGuardBytes) {
                    byte[] unGuardedBytes = new byte[-guardBytesLength + managedObjectBytes.length - guardBytesLength];
                    for (int i = 0; i < guardBytesLength; i++) {
                        if (managedObjectBytes[i] != managedObjectBytes[guardBytesLength + unGuardedBytes.length + i]) {
                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this,
                                           cclass,
                                           methodName);
                            throw new GuardBytesException(this,
                                                          token);
                        }
                    } // for guardBytes.

                    System.arraycopy(managedObjectBytes, // Source.
                                     guardBytesLength, // Source position.
                                     unGuardedBytes, // Destination.
                                     0, // Destination position.
                                     unGuardedBytes.length); // Length.

                    managedObjectBytes = unGuardedBytes;
                } // If (useGuardBytes).

                // Recover the ManagedObject from its serialized bytes.
                objectFromStore = ManagedObject.restoreFromSerializedBytes(managedObjectBytes,
                                                                           objectManagerState);
                // Cache the ManagedObject in memory, possibly push out a previously
                // cached ManagedObject.
                cache(objectFromStore);
            } // if (storeArea != null).
        } // synchronized(this).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { objectFromStore });
        return objectFromStore;
    } // get().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#close()
     */
    public synchronized void close()
                    throws ObjectManagerException {
        final String methodName = "close";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName);

        super.close();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // close().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.AbstractSingleFileObjectStore#readDirectory(int, long, long)
     */
    final AbstractSingleFileObjectStore.Directory makeDirectory(int minimumNodeSize,
                                                                long directoryRootByteAddress,
                                                                long directoryRootLength)
                    throws ObjectManagerException {
        return new Directory(minimumNodeSize,
                             directoryRootByteAddress,
                             directoryRootLength);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.AbstractSingleFileObjectStore#write(com.ibm.ws.objectManager.ManagedObject)
     */
    final void write(ManagedObject managedObject)
                    throws ObjectManagerException
    {
        final String methodName = "write";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { managedObject });

        ObjectManagerByteArrayOutputStream byteArrayOutputStream = null;
        byte[] serializedBytes = null;
        int serializedBytesLength = 0;
        if (usesSerializedForm) {
            // Pick up and write the latest serialized bytes.

            // It is possible that several threads requested a write one after the
            // other each getting the request added to a different managedObjetsToWrite table,
            // however the first of these through here will clear serializedBytes.
            // It is also possible that the transaction state of the managed object
            // was restored from a checkpoint, in which case the serialized object
            // may already be in the ObjectStore, and the serializedBytes will again be null.

            // Is the Object deleted? It may have got deleted after we release the
            // synchronize lock when we captured the tokensToWrite hashtable but before we
            // actually try to write it. We dont strictly need the test for stateDeleted
            // since once in this state the byteArrayOutputStream will be null.
            if (managedObject.state != ManagedObject.stateDeleted)
                byteArrayOutputStream = managedObject.freeLatestSerializedBytes();

        } else {
            // Not logged so use the current serialized bytes, as long as its not part of a transaction.
            // If it is part of a transaction then the transaction will hold the ManagedObject in memory.
            // Not locked because this is only used by SAVE_ONLY_ON_SHUTDOWN stores at shutdown
            // when no appliaction threads are active.
            if (managedObject.state == ManagedObject.stateReady)
                byteArrayOutputStream = managedObject.getSerializedBytes();
            // The byteArrayOutputStream will have getRleaseSize() == 0;
        } // if ( usesSerializedForm ).

        if (byteArrayOutputStream != null) {
            serializedBytes = byteArrayOutputStream.getBuffer();
            serializedBytesLength = byteArrayOutputStream.getCount();
            // For debugging add guardBytes before and after the serialized
            // ManagedObject.
            if (useGuardBytes) {
                guardByte++;
                byte[] guardedBytes = new byte[guardBytesLength + serializedBytesLength + guardBytesLength];
                java.util.Arrays.fill(guardedBytes // Target
                                      , 0 // From index.
                                      , guardBytesLength - 1 // To index.
                                      , guardByte //Fill byte.
                );
                System.arraycopy(serializedBytes // Source.
                                 , 0 // Source position.
                                 , guardedBytes // Destination.
                                 , guardBytesLength // Destination position.
                                 , serializedBytesLength// Length.
                );
                java.util.Arrays.fill(guardedBytes,
                                      guardBytesLength + serializedBytesLength,
                                      guardBytesLength + serializedBytesLength + guardBytesLength - 1,
                                      guardByte);
                serializedBytes = guardedBytes;
                serializedBytesLength = guardedBytes.length;
            } // if (useGuardBytes).

            //TODO Consider Rounding to a 4K boundary.
            //!! int lengthRequired = ((serializedBytes.length/4096)+1)*4096;

            // Allocate some space for the ManagedObject, if we are recovering, we can reuse already 
            // allocated space and avoid the need to have extra space available in the store. This is 
            // important if we were to crash after flushing the store but before we write checkpoint 
            // end. It that event we would try and write the objects to the store for a second time
            // during recovery and require that it had enough spare space to do this. 
            Directory.StoreArea storeArea;
            boolean addToDirectory = false;
            if (objectManagerState.getObjectManagerStateState() == ObjectManagerState.stateReplayingLog) {
                storeArea = (Directory.StoreArea) directory.getEntry(new Long(managedObject.owningToken.storedObjectIdentifier));
                if (storeArea == null
                    || storeArea.length < serializedBytesLength
                    || storeArea.length + minimumFreeSpaceEntrySize > serializedBytesLength) {
                    FreeSpace freeSpace = allocateSpace(serializedBytesLength);
                    storeArea = (Directory.StoreArea) directory.makeStoreArea(managedObject.owningToken.storedObjectIdentifier,
                                                                              freeSpace.address,
                                                                              freeSpace.length);
                    addToDirectory = true;
                }

            } else {
                FreeSpace freeSpace = allocateSpace(serializedBytesLength);
                storeArea = (Directory.StoreArea) directory.makeStoreArea(managedObject.owningToken.storedObjectIdentifier,
                                                                          freeSpace.address,
                                                                          freeSpace.length);
                addToDirectory = true;
            } // if (objectManagerState.

            if (gatherStatistics)
                numberOfManagedObjectsWritten++;
            writeBuffer(serializedBytes,
                        0,
                        serializedBytesLength,
                        storeArea.byteAddress);

            if (addToDirectory) {
                // We assume that even though the buffer has not been forced to disk, the file system will
                // allow a read of the disk to look into the buffer, hence we go ahead and
                // update the live directory.
                Directory.StoreArea existingStoreArea = (Directory.StoreArea) directory.putEntry(storeArea);

                // If this is a replacement we will eventually recover the space the previous object occupies.
                // Note the free space for now but don't release it until the new Object
                // has been forced to disk.
                if (existingStoreArea != null) {
                    newFreeSpace.add(existingStoreArea);
                } // if (existingStoreArea == null).
            } // if (addToDirectory).

            // Keep count of the space we will eventually release now that we have allocated the space.
            checkpointReleaseSize = checkpointReleaseSize + byteArrayOutputStream.getReleaseSize();
            objectManagerState.returnByteArrayOutputStreamToPool(byteArrayOutputStream);
        } // if (byteArrayOutputStream != null ).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // write().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.AbstractSingleFileObjectStore#writeFreeSpace()
     */
    final void writeFreeSpace()
                    throws ObjectManagerException {
        final String methodName = "writeFreeSpace";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        // Write the free space map to disk.
        byteArrayOutputStream.reset();
        java.io.DataOutputStream dataOutputStream = new java.io.DataOutputStream(byteArrayOutputStream);
        long freeSpaceByteAddress = freeSpaceStoreArea.byteAddress;
        try {
            FreeSpace freeSpace = freeSpaceByAddressHead;
            while (freeSpace != null) {
                dataOutputStream.writeLong(freeSpace.address);
                dataOutputStream.writeLong(freeSpace.length);
                freeSpace = freeSpace.next;
            } // for freeSpaceByAddress...
            dataOutputStream.flush();
            byteArrayOutputStream.flush();
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "flush", exception, "1:724:1.35");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "flush");
            throw new PermanentIOException(this,
                                           exception);
        } // catch.

        writeBuffer(byteArrayOutputStream.getBuffer(),
                    0,
                    byteArrayOutputStream.getCount(),
                    freeSpaceByteAddress);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // writeFreeSpace().

    /**
     * Write a byteBuffer. May return before the write completes.
     * 
     * @param byteBuffer to be written.
     * @param offset within the buffer of the first byte to write.
     * @param length of the buffer to write.
     * @param byteAddress that the buffer is to be was written at.
     * @return int the number of bytes written.
     * @throws ObjectManagerException
     */
    private int writeBuffer(byte[] byteBuffer,
                            int offset,
                            int length,
                            long byteAddress)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "writeBuffer",
                        new Object[] { new Integer(byteBuffer.hashCode()), new Integer(offset), new Integer(length), new Long(byteAddress) }
                            );

        // Write the bytes.
        try {
            storeFile.seek(byteAddress);
            storeFile.write(byteBuffer, offset, length);
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "writeBuffer", exception, "1:774:1.35");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "writeBuffer");

            // cannot continue
            objectManagerState.requestShutdown();
            throw new PermanentIOException(this,
                                           exception);
        } // catch.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "writeBuffer",
                       new Object[] { new Integer(length) });
        return length;
    } // writeBuffer().

    /**
     * Writes the fileStore header.
     * 
     * @throws ObjectManagerException
     */
    final void writeHeader()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "writeHeader");

        byteArrayOutputStream.reset();
        java.io.DataOutputStream dataOutputStream = new java.io.DataOutputStream(byteArrayOutputStream);

        // Write the header.
        try {
            dataOutputStream.writeInt(version);
            dataOutputStream.writeChars(signature);
            dataOutputStream.writeLong(objectStoreIdentifier);
            dataOutputStream.writeLong(((Directory.Node) directory.root).byteAddress);
            dataOutputStream.writeLong(((Directory.Node) directory.root).length);
            dataOutputStream.writeInt(directory.getMinimumNodeSize());
            if (freeSpaceStoreArea == null) {
                dataOutputStream.writeLong(0);
                dataOutputStream.writeLong(0);
            } else {
                dataOutputStream.writeLong(freeSpaceStoreArea.byteAddress);
                dataOutputStream.writeLong(freeSpaceStoreArea.length);
            }
            dataOutputStream.writeLong((long) freeSpaceByLength.size());
            dataOutputStream.writeLong(sequenceNumber);
            dataOutputStream.writeLong(storeFileSizeUsed);
            dataOutputStream.writeLong(minimumStoreFileSize);
            dataOutputStream.writeLong(maximumStoreFileSize);
            dataOutputStream.writeInt(cachedManagedObjectsSize);

            dataOutputStream.flush();
            byteArrayOutputStream.flush();
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "writeHeader", exception, "1:836:1.35");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "flush");
            throw new PermanentIOException(this,
                                           exception);
        } // catch.

        byte[] buffer = byteArrayOutputStream.getBuffer();
        int length = byteArrayOutputStream.getCount();
        writeBuffer(buffer,
                    0,
                    length,
                    0);
        // Write the header twice.
        writeBuffer(buffer,
                    0,
                    length,
                    pageSize);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "writeHeader"
                            );
    } // writeHeader().

    /**
     * Force updates to disk.
     * 
     * @throws ObjectManagerException
     */
    void force()
                    throws ObjectManagerException
    {
        final String methodName = "force";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        // SYNCing the disk seems to cause get to sometimes read corrupt data! Instead open the file "rwd"
        try {
            storeFile.getFD().sync(); // Force buffered records and not metadata to disk.

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:884:1.35");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName);

            // cannot continue
            objectManagerState.requestShutdown();
            throw new PermanentIOException(this,
                                           exception);
        } // catch.
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // force().

    //---------------------------------------------------------------------------
    // Inner classes.
    // --------------------------------------------------------------------------

    final class Directory
                    extends AbstractSingleFileObjectStore.Directory
    {
        private final Class cclass = SingleNonNioFileObjectStore.Directory.class;

        /**
         * @param minimumNodeSize the minimum size of a directory Node.
         * @param directoryRootByteAddress the directory root byte address.
         * @param directoryRootLength the number of bytes in the direcory root.
         * 
         * @throws ObjectManagerException
         */
        Directory(int minimumNodeSize,
                  long directoryRootByteAddress,
                  long directoryRootLength)
            throws ObjectManagerException {
            super(minimumNodeSize
                  , directoryRootByteAddress
                  , directoryRootLength);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "<init>",
                            new Object[] { new Integer(minimumNodeSize), new Long(directoryRootByteAddress), new Long(directoryRootLength) }
                                );

            // If length = 0 we end up with Directory.Node because makeNode is invoked.
            if (directoryRootLength > 0) {
                root = new Node(directoryRootByteAddress, directoryRootLength);
            } // if (directoryRootLength > 0).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>");
        } // Directory().

        /**
         * Construct a Node for the superclass BTree.
         * 
         * @param parent of this node.
         * @return BTree.Node constructed, has no storage allocated.
         * 
         * @exception ObjectManagerException
         */
        BTree.Node makeNode(BTree.Node parent)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            SingleNonNioFileObjectStore.Directory.class, // !! This must be used as makeNode is called in
                            "makeNode", // superclass constructor before cclass is initialized
                            new Object[] { parent });

            Directory.Node newNode = new Directory.Node((Directory.Node) parent);
            newNode.modified = true;
            newNode.dormantFlushCount = Node.initialDormantFlushCount;

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           SingleNonNioFileObjectStore.Directory.class, // !! This must be used as makeNode is called in
                           "makeNode", // superclass constructor before cclass is initialized
                           new Object[] { newNode }
                                );
            return newNode;
        } // makeNode().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.BTree#makeEntry(java.lang.Object, java.lang.Object)
         */
        BTree.Entry makeEntry(Object key, Object value)
                        throws ObjectManagerException
        {
            Directory.Entry newEntry = new Directory.StoreArea(key, value);
            return newEntry;
        } // makeEntry().

        /**
         * Construct a StoreArea which can be put into this directory.
         * 
         * @param identifier of the StoreArea.
         * @param byteAddress offset of the object in the file.
         * @param length number of bytes in the serialised object written on the disk.
         * @return StoreArea which can be put into this Directory.
         * 
         * @exception ObjectManagerException
         */
        AbstractSingleFileObjectStore.Directory.StoreArea makeStoreArea(long identifier,
                                                                        long byteAddress,
                                                                        long length)
                        throws ObjectManagerException
        {
            return new StoreArea(identifier,
                                 byteAddress,
                                 length);
        } // makeStoreArea().

        // --------------------------------------------------------------------------
        // Inner classes.
        // --------------------------------------------------------------------------

        class Node
                        extends AbstractSingleFileObjectStore.Directory.Node
        {
            private final Class cclass = SingleNonNioFileObjectStore.Directory.Node.class;

            /**
             * This constructor creates an empty leaf node.
             * 
             * @param parent of this Node or null if the root.
             */
            Node(Node parent)
            {
                super(parent);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "<init>",
                                new Object[] { parent });
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "<init>"
                                    );
            } // Node().

            /**
             * Load a Node from the file.
             * 
             * @param byteAddress of the Node on disk.
             * @param length number of bytes allocated on the disk for this Node.
             * 
             * @exception ObjectManagerException
             */
            Node(long byteAddress, long length)
                throws ObjectManagerException
            {
                // Construct an empty leaf Node with no parent.
                super(null); //TODO What about the parent?

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "<init>",
                                new Object[] { new Long(byteAddress), new Long(length) });

                this.byteAddress = byteAddress;
                this.length = length;
                if (gatherStatistics)
                    numberOfDirectoryNodesRead++;

                // Read the Node from disk.
                try {
                    storeFile.seek(byteAddress);

                    // For debugging check guardBytes added before and after the Node metadata.
                    byte firstGuardByte;
                    if (useGuardBytes) {
                        firstGuardByte = storeFile.readByte();
                        for (int i = 1; i < guardBytesLength; i++) {
                            byte nextGuardByte = storeFile.readByte();
                            if (nextGuardByte != firstGuardByte) {
                                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                    trace.exit(this,
                                               cclass,
                                               "<init>");
                                throw new GuardBytesException(this,
                                                              this);
                            } // if (nextGuardByte != firstGuardByte).
                        } // for guardBytes.
                    } // if (useGuardBytes).

                    numberOfKeys = storeFile.readInt();
                    byte flag = storeFile.readByte();
                    if (flag == flagIsNotLeaf)
                        isLeaf = false;

                    // Load the StoreAreas.
                    StoreArea previous = null;
                    for (int i = 0; i < numberOfKeys; i++) {
                        StoreArea storeArea = (StoreArea) makeStoreArea(storeFile.readLong(),
                                                                        storeFile.readLong(),
                                                                        storeFile.readLong());

                        if (previous == null) {
                            first = storeArea;
                        } else {
                            previous.next = storeArea;
                        }
                        previous = storeArea;

                        if (!isLeaf) {
                            storeArea.childByteAddress = storeFile.readLong();
                            storeArea.childLength = storeFile.readLong();
                        } // if(!isLeaf).
                    } // for numberOfKeys...

                    // Load any final StoreArea.
                    if (!isLeaf) {
                        StoreArea storeArea = new StoreArea(null, null);
                        storeArea.childByteAddress = storeFile.readLong();
                        storeArea.childLength = storeFile.readLong();
                        previous.next = storeArea;
                    } // if (!isLeaf).

                    // For debugging check guardBytes added before and after the Node metadata.
                    if (useGuardBytes) {
                        for (int i = 0; i < guardBytesLength; i++) {
                            byte nextGuardByte = storeFile.readByte();
                            if (nextGuardByte != firstGuardByte) {
                                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                    trace.exit(this,
                                               cclass,
                                               "<init>");
                                throw new GuardBytesException(this,
                                                              this);
                            } // if (nextGuardByte != firstGuardByte).
                        } // for guardBytes.
                    } // if (useGuardBytes).

                } catch (java.io.IOException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this, cclass, "<init>", exception, "1:1125:1.35");

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "<init>");

                    // cannot continue
                    objectManagerState.requestShutdown();
                    throw new PermanentIOException(this,
                                                   exception);
                } // catch.

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "<init>"
                                    );
            } // Node().

            /**
             * Write this sub directory and any child nodes that have been updated
             * in the space previously allocated.
             * 
             * @throws ObjectManagerException
             */
            void write()
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "write",
                                new Object[] { new Long(byteAddress), new Long(length) });

                if (modified) {
                    // Recurse into child sub directories first, let them use the sharedBuffer.
                    if (!isLeaf) {
                        for (StoreArea storeArea = (StoreArea) first; storeArea != null; storeArea = (StoreArea) storeArea.next) {
                            // Recurse into child, if it has been loaded.
                            if (storeArea.child != null) {
                                ((Node) storeArea.child).write();
                                // Update the child location we will write.
                                storeArea.childByteAddress = ((Node) storeArea.child).byteAddress;
                                storeArea.childLength = ((Node) storeArea.child).length;
                            } // if (storeArea.child != null).
                        } // for StoreAreas...
                    } // if (!isLeaf).

                    // Write this sub directory to the file.
                    byteArrayOutputStream.reset();
                    java.io.DataOutputStream dataOutputStream = new java.io.DataOutputStream(byteArrayOutputStream);
                    // The address of the directory in the file.
                    long directoryByteAddress = byteAddress;

                    try {
                        // For debugging add guardBytes before and after the Node metadata.
                        if (useGuardBytes) {
                            guardByte++;
                            for (int i = 0; i < guardBytesLength; i++)
                                dataOutputStream.writeByte(guardByte);
                        } // if (useGuardBytes).

                        dataOutputStream.writeInt(numberOfKeys);
                        if (isLeaf)
                            dataOutputStream.writeByte(flagIsLeaf);
                        else
                            dataOutputStream.writeByte(flagIsNotLeaf);

                        for (StoreArea storeArea = (StoreArea) first; storeArea != null; storeArea = (StoreArea) storeArea.next) {

                            // The final Entry in a BTree Node has a child but no key.
                            if (storeArea.getKey() != null) {
                                dataOutputStream.writeLong(storeArea.identifier);
                                dataOutputStream.writeLong(storeArea.byteAddress);
                                dataOutputStream.writeLong(storeArea.length);
                            }

                            if (!isLeaf) {
                                dataOutputStream.writeLong(storeArea.getChildByteAddress());
                                dataOutputStream.writeLong(storeArea.getChildLength());
                            } // if (!isLeaf).

                        } // for StoreAreas...

                        // For debugging add guardBytes before and after the Node metadata.
                        if (useGuardBytes) {
                            for (int i = 0; i < guardBytesLength; i++)
                                dataOutputStream.writeByte(guardByte);
                        } // if (useGuardBytes).

                        dataOutputStream.flush();
                        byteArrayOutputStream.flush();
                    } catch (java.io.IOException exception) {
                        // No FFDC Code Needed.
                        ObjectManager.ffdc.processException(this, cclass, "write", exception, "1:1222:1.35");

                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this,
                                       cclass,
                                       "write");
                        throw new PermanentIOException(this,
                                                       exception);
                    } // catch.

                    writeBuffer(byteArrayOutputStream.getBuffer(),
                                0,
                                byteArrayOutputStream.getCount(),
                                directoryByteAddress);
                    modified = false;
                } // if (modified).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "write");
            } // write().

        } // inner class Node.

        /**
         * Represents the disk location of an Object in the store
         * and metadata from the child sub directory.
         * 
         * @version 1.00 05 November 2004
         * @author Andrew Banks
         */
        class StoreArea
                        extends AbstractSingleFileObjectStore.Directory.StoreArea
        {
            private final Class cclass = SingleNonNioFileObjectStore.Directory.StoreArea.class;

            // --------------------------------------------------------------------------
            // extends AbstractSingleFileObjectStore.Directory.StoreArea.
            // --------------------------------------------------------------------------

            /**
             * Creates a StoreArea representing space used in the store file .
             * 
             * @param key representing the storedObjectIdentifier as a Long.
             * @param value representing the byte address on the area as a Long.
             */
            StoreArea(Object key,
                      Object value) {
                super(key, value);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "<init>",
                                new Object[] { key, value });

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "<init>");
            } // end of Constructor.

            /**
             * Creates a StoreArea representing space used in the store file .
             * 
             * @param long the identifier of the StoreArea.
             * @param long the byte offset of the object in the file.
             * @param long the number of bytes in the serialised object written on the
             *        disk.
             * @exception ObjectManagerException.
             */
            StoreArea(long identifier,
                      long byteAddress,
                      long length)
            {
                super(identifier, byteAddress, length);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "<init>",
                                new Object[] { new Long(identifier), new Long(byteAddress), new Long(length) });

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "<init>");
            } // end of Constructor.

            /**
             * Give up the child sub tree of an internal node, if the child is not already loaded
             * it is loaded from the file.
             * 
             * @return Node the child.
             * @exception ObjectManagerException
             */
            BTree.Node getChild()
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "getChild",
                                new Object[] { new Long(identifier), new Long(childByteAddress), new Long(childLength) });

                // take a reference to child in case a checkpoint nulls child out before we return
                BTree.Node childRef = child;

                if (childRef == null && childLength != 0) {
                    java.lang.ref.SoftReference softReference = childSoftReference;
                    if (softReference != null)
                        child = childRef = (BTree.Node) softReference.get();

                    if (childRef == null)
                        child = childRef = new Node(childByteAddress, childLength); //TODO What about the parent?
                } // if (child == null && childLength != 0).

                // set dormantFlushCount using the reference in case a checkpoint nulls child out
                if (childRef != null)
                    ((Node) childRef).dormantFlushCount = Node.initialDormantFlushCount;

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "getChild"
                               , new Object[] { childRef }
                                    );

                return childRef;
            } // getChild().

        } // inner class StoreArea.
    } // inner class Directory.
} // class SingleNonNioFileObjectStore.
