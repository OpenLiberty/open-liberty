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
 * Implements a SingleFileObjectStore using java.nio.
 * If you restart an ObjectManager using this kind of store on a system that does not
 * support java.nio, it will be migrated to a SingleNonNoiFileObjectStore.
 * 
 * @version @(#) 1/25/13
 * @author IBM Corporation
 */
public final class SingleFileObjectStore
                extends AbstractSingleFileObjectStore
{
    private static final Class cclass = SingleFileObjectStore.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_STORE);

    private static final long serialVersionUID = 5641115480865092449L;

    // The FileChannel that handles writing to the storeFile.
    private transient java.nio.channels.FileChannel storeChannel;
    private transient java.nio.ByteBuffer sharedBuffer;
    private transient java.nio.channels.FileLock storeFileLock;

    /**
     * Constructor
     * 
     * @param storeName Identifies the ObjecStore and the file directory.
     * @param objectManager that manages this store.
     * @throws ObjectManagerException
     */
    public SingleFileObjectStore(String storeName,
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
    public SingleFileObjectStore(String storeName,
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

        sharedBuffer = java.nio.ByteBuffer.allocateDirect(1024 * 1024);
        storeChannel = storeFile.getChannel();

        // Make sure no one else can write to the file.
        // Obtain an exclusive lock on the storeFileChannel.
        try {
            storeFileLock = storeChannel.tryLock();
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "<init>", exception, "1:137:1.39");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { exception });
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).
        if (storeFileLock == null) { // Did we get the lock?
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           "via StoreFileInUseException, storeName=" + storeName + "(String)");
            throw new StoreFileInUseException(this,
                                              storeName);
        } // (fileLock == null).

        // Now look at the file and initialise the ObjectStore.
        // Read the header, to find the directory and free space map.
        try {
            java.nio.ByteBuffer storeHeader = java.nio.ByteBuffer.allocate(pageSize);
            storeChannel.read(storeHeader);
            storeHeader.flip();

            int versionRead = storeHeader.getInt();

            // Read a known number of signature charaters.
            char[] signatureRead = new char[signature.length()];
            for (int i = 0; i < signature.length(); i++)
                signatureRead[i] = storeHeader.getChar();
            if (!(new String(signatureRead).equals(signature))) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "open"
                               , new Object[] { signatureRead });
                throw new InvalidStoreSignatureException(this,
                                                         new String(signatureRead), signature);
            } // if(!(new String(signatureRead).equals(signature))).

            long objectStoreIdentifierRead = storeHeader.getLong();
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
            long directoryRootByteAddress = storeHeader.getLong();
            // The current number of bytes in the directory root.
            long directoryRootLength = storeHeader.getLong();
            // The minimumNodeSize of the directory BTree.
            int directoryMinimumNodeSize = storeHeader.getInt();
            long freeSpaceStoreAreaByteAddress = storeHeader.getLong();
            long freeSpaceStoreAreaLength = storeHeader.getLong();
            // The actual number of free space entries since we have to overestimate the space we need to store them.
            long freeSpaceCount = storeHeader.getLong();

            sequenceNumber = storeHeader.getLong();
            // This is the length we assume we have written.
            storeFileSizeUsed = storeHeader.getLong();
            // Administered limits on store file size.
            minimumStoreFileSize = storeHeader.getLong();
            maximumStoreFileSize = storeHeader.getLong();
            cachedManagedObjectsSize = storeHeader.getInt();

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

        } catch (java.nio.BufferUnderflowException exception) {
            // The file is empty or did not exist.
            // No FFDC Code Needed, this may be first use of a store.
            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                trace.event(this,
                            cclass,
                            methodName,
                            exception);

            if (objectManagerState.getObjectManagerStateState() == ObjectManagerState.stateReplayingLog) {
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:267:1.39");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Object[] { "Buffer underflow:270" });
                throw new PermanentNIOException(this,
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
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:302:1.39");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName);
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

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
                java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocate((int) freeSpaceCount * freeSpaceEntryLength);
                storeChannel.read(byteBuffer,
                                  freeSpaceStoreArea.byteAddress);
                byteBuffer.flip();

                FreeSpace freeSpaceByAddressTail = null;
                for (int i = 0; i < freeSpaceCount; i++) {
                    AbstractSingleFileObjectStore.FreeSpace freeSpace =
                                    new AbstractSingleFileObjectStore.FreeSpace(byteBuffer.getLong(), byteBuffer.getLong());

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
                            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:395:1.39");
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
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:425:1.39");

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
        Directory.StoreArea storeArea = (Directory.StoreArea) directory.getEntry(new Long(token.storedObjectIdentifier));

        if (storeArea != null) { // Was the object written?
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            methodName,
                            new Object[] { storeArea });

            java.nio.ByteBuffer serializedBuffer = java.nio.ByteBuffer.allocate((int) storeArea.length);
            getLock.readLock().lock();
            try {
                // Now read the serialized form of the ManagedObject.
                storeChannel.read(serializedBuffer,
                                  storeArea.byteAddress);

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:480:1.39");

                // check if the file handle is still good
                if (storeChannel.isOpen())
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

            } finally {
                getLock.readLock().unlock();
            } // try... 

            byte[] managedObjectBytes = serializedBuffer.array();

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

        // Close the storeChannel.
        try {
            if (storeFileLock != null)
                storeFileLock.release();
            if (storeChannel != null)
                storeChannel.close();
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:570:1.39");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, exception);
            throw new PermanentIOException(this,
                                           exception);
        } //  catch (java.io.IOException exception).

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

//      if (serializedBytesLength < sharedBuffer.capacity()) {
//        sharedBuffer.clear();
//        try {
//          sharedBuffer.put(serializedBytes,0,serializedBytesLength);
//        } catch (java.nio.BufferOverflowException exception) {
//          // No FFDC Code Needed.
//          ObjectManager.ffdc.processException(this,cclass,methodName,exception,"1:702:1.39");
//
//          // Did not fit, should not occur because we checked first.
//          if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//            trace.exit(this, cclass, methodName);
//          throw new PermanentNIOException(this,
//                                          exception);
//        } // catch (java.nio.BufferOverflowException exception).
//        // Write the buffer.
//        sharedBuffer.flip();
//        writeBuffer(sharedBuffer,
//                    storeArea.byteAddress);
//
//      } else {
//        java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.wrap(serializedBytes,0,serializedBytesLength);
//        writeBuffer(byteBuffer,
//                    storeArea.byteAddress);
//      } // if (serializedBytesLength...

            try {
                for (int iStart = 0; iStart < serializedBytesLength;) {
                    sharedBuffer.clear();
                    int iLength = Math.min(sharedBuffer.remaining(), serializedBytesLength - iStart);

                    sharedBuffer.put(serializedBytes, iStart, iLength);
                    sharedBuffer.flip();
                    writeBuffer(sharedBuffer,
                                storeArea.byteAddress + iStart);
                    iStart = iStart + iLength;
                } // for...  
            } catch (java.nio.BufferOverflowException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:734:1.39");

                // Did not fit, should not occur because we checked first.
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName);
                throw new PermanentNIOException(this,
                                                exception);
            } // catch (java.nio.BufferOverflowException exception).

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
        sharedBuffer.clear();
        long freeSpaceByteAddress = freeSpaceStoreArea.byteAddress;
        FreeSpace freeSpace = freeSpaceByAddressHead;
        while (freeSpace != null) {
            sharedBuffer.putLong(freeSpace.address);
            sharedBuffer.putLong(freeSpace.length);
            if (sharedBuffer.remaining() < freeSpaceEntryLength) {
                sharedBuffer.flip();
                int bytesWritten = writeBuffer(sharedBuffer,
                                               freeSpaceByteAddress);
                freeSpaceByteAddress = freeSpaceByteAddress + bytesWritten;
                sharedBuffer.clear();
            } // if( sharedBuffer.remaining() < freeSpaceEntryLength).
            freeSpace = freeSpace.next;
        } // for freeSpaceByAddress...
        sharedBuffer.flip();
        writeBuffer(sharedBuffer,
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
     * @param byteAddress that the buffer is to be was written at.
     * @return int the number of bytes written.
     * @throws ObjectManagerException
     */
    private int writeBuffer(java.nio.ByteBuffer byteBuffer,
                            long byteAddress)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "writeBuffer",
                        new Object[] { new Integer(byteBuffer.hashCode()), new Long(byteAddress) }
                            );

        // Write the bytes.
        int bytesWritten = 0;
        try {
            while (byteBuffer.hasRemaining()) {
                bytesWritten = bytesWritten + storeChannel.write(byteBuffer,
                                                                 byteAddress + bytesWritten);
            } // while (byteBuffer.hasRemaining()).
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "writeBuffer", exception, "1:833:1.39");

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
                       new Object[] { new Integer(bytesWritten) });
        return bytesWritten;
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

        // Write the header.
        sharedBuffer.clear();
        sharedBuffer.putInt(version);
        for (int i = 0; i < signature.length(); i++)
            sharedBuffer.putChar(signature.charAt(i));
        sharedBuffer.putLong(objectStoreIdentifier);
        sharedBuffer.putLong(((Directory.Node) directory.root).byteAddress);
        sharedBuffer.putLong(((Directory.Node) directory.root).length);
        sharedBuffer.putInt(directory.getMinimumNodeSize());
        if (freeSpaceStoreArea == null) {
            sharedBuffer.putLong(0);
            sharedBuffer.putLong(0);
        } else {
            sharedBuffer.putLong(freeSpaceStoreArea.byteAddress);
            sharedBuffer.putLong(freeSpaceStoreArea.length);
        }
        sharedBuffer.putLong((long) freeSpaceByLength.size());
        sharedBuffer.putLong(sequenceNumber);
        sharedBuffer.putLong(storeFileSizeUsed);
        sharedBuffer.putLong(minimumStoreFileSize);
        sharedBuffer.putLong(maximumStoreFileSize);
        sharedBuffer.putInt(cachedManagedObjectsSize);

        sharedBuffer.flip();
        writeBuffer(sharedBuffer,
                    0);

        // Write the header twice.
        sharedBuffer.flip();
        writeBuffer(sharedBuffer,
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

        try {
            storeChannel.force(false); // Force buffered records and not metadata to disk.

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:923:1.39");

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
        private final Class cclass = SingleFileObjectStore.Directory.class;

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
                            SingleFileObjectStore.Directory.class, // !! This must be used as makeNode is called in
                            "makeNode", // superclass constructor before cclass is initialized
                            new Object[] { parent });

            Directory.Node newNode = new Directory.Node((Directory.Node) parent);
            newNode.modified = true;
            newNode.dormantFlushCount = Node.initialDormantFlushCount;

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           SingleFileObjectStore.Directory.class, // !! This must be used as makeNode is called in
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
            private final Class cclass = SingleFileObjectStore.Directory.Node.class;

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
                throws ObjectManagerException {
                // Construct an empty leaf Node with no parent.
                super(null); //TODO What about the parent?

                final String methodName = "<init>";
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                methodName,
                                new Object[] { new Long(byteAddress), new Long(length) });

                this.byteAddress = byteAddress;
                this.length = length;
                if (gatherStatistics)
                    numberOfDirectoryNodesRead++;

                // Read the Node from disk.
                getLock.readLock().lock();
                try {
                    java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocate((int) length);
                    storeChannel.read(byteBuffer,
                                      byteAddress);
                    byteBuffer.flip();

                    // For debugging check guardBytes added before and after the Node metadata.
                    byte firstGuardByte;
                    if (useGuardBytes) {
                        firstGuardByte = byteBuffer.get();
                        for (int i = 1; i < guardBytesLength; i++) {
                            byte nextGuardByte = byteBuffer.get();
                            if (nextGuardByte != firstGuardByte) {
                                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                    trace.exit(this, cclass, methodName);
                                throw new GuardBytesException(this,
                                                              this);
                            } // if (nextGuardByte != firstGuardByte).
                        } // for guardBytes.
                    } // if (useGuardBytes).

                    numberOfKeys = byteBuffer.getInt();
                    byte flag = byteBuffer.get();
                    if (flag == flagIsNotLeaf)
                        isLeaf = false;

                    // Load the StoreAreas.
                    StoreArea previous = null;
                    for (int i = 0; i < numberOfKeys; i++) {
                        StoreArea storeArea = (StoreArea) makeStoreArea(byteBuffer.getLong(),
                                                                        byteBuffer.getLong(),
                                                                        byteBuffer.getLong());

                        if (previous == null) {
                            first = storeArea;
                        } else {
                            previous.next = storeArea;
                        }
                        previous = storeArea;

                        if (!isLeaf) {
                            storeArea.childByteAddress = byteBuffer.getLong();
                            storeArea.childLength = byteBuffer.getLong();
                        } // if(!isLeaf).
                    } // for numberOfKeys...

                    // Load any final StoreArea.
                    if (!isLeaf) {
                        StoreArea storeArea = new StoreArea(null, null);
                        storeArea.childByteAddress = byteBuffer.getLong();
                        storeArea.childLength = byteBuffer.getLong();
                        previous.next = storeArea;
                    } // if (!isLeaf).

                    // For debugging check guardBytes added before and after the Node metadata.
                    if (useGuardBytes) {
                        for (int i = 0; i < guardBytesLength; i++) {
                            byte nextGuardByte = byteBuffer.get();
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
                    ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:1166:1.39");

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName);

                    // cannot continue
                    objectManagerState.requestShutdown();
                    throw new PermanentIOException(this,
                                                   exception);

                } finally {
                    getLock.readLock().unlock();
                } // try...

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName);
            } // Node().

            /**
             * Write this sub directory and any child nodes that have been updated in the space previously allocated.
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
                    sharedBuffer.clear();
                    // The address of the directory in the file.
                    long directoryByteAddress = byteAddress;

                    // For debugging add guardBytes before and after the Node metadata.
                    if (useGuardBytes) {
                        guardByte++;
                        for (int i = 0; i < guardBytesLength; i++)
                            sharedBuffer.put(guardByte);
                    } // if (useGuardBytes).

                    sharedBuffer.putInt(numberOfKeys);
                    if (isLeaf)
                        sharedBuffer.put(flagIsLeaf);
                    else
                        sharedBuffer.put(flagIsNotLeaf);

                    for (StoreArea storeArea = (StoreArea) first; storeArea != null; storeArea = (StoreArea) storeArea.next) {

                        // The final Entry in a BTree Node has a child but no key.
                        if (storeArea.getKey() != null) {
                            sharedBuffer.putLong(storeArea.identifier);
                            sharedBuffer.putLong(storeArea.byteAddress);
                            sharedBuffer.putLong(storeArea.length);
                        }

                        if (!isLeaf) {
                            sharedBuffer.putLong(storeArea.getChildByteAddress());
                            sharedBuffer.putLong(storeArea.getChildLength());
                        } // if (!isLeaf).

                        if (sharedBuffer.remaining() < StoreArea.maximumWrittenSize) {
                            sharedBuffer.flip();
                            int bytesWritten = writeBuffer(sharedBuffer,
                                                           directoryByteAddress);
                            directoryByteAddress = directoryByteAddress + bytesWritten;
                            sharedBuffer.clear();
                        } // if( sharedBuffer.remaining() < StoreArea.writtenSize).
                    } // for StoreAreas...

                    // For debugging add guardBytes before and after the Node metadata.
                    if (useGuardBytes) {
                        for (int i = 0; i < guardBytesLength; i++)
                            sharedBuffer.put(guardByte);
                    } // if (useGuardBytes).

                    sharedBuffer.flip();
                    writeBuffer(sharedBuffer,
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
            private final Class cclass = SingleFileObjectStore.Directory.StoreArea.class;

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
             * @param identifier of the StoreArea.
             * @param byteAddress offset of the object in the file.
             * @param length the number of bytes in the serialised object written on the
             *            disk.
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
} // class SingleFileObjectStore.
