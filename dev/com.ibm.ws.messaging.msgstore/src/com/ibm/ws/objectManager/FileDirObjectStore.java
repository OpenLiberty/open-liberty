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
 * A cheap and cheerful ObjectStore that uses a file directory to store each object as a separate file. we reuse the
 * seqenceNumber from MemoryObjectStore as the filename.
 * 
 * @version @(#) 1/25/13
 * @author IBM Corporation
 */
public final class FileDirObjectStore
                extends AbstractObjectStore
{
    private static final Class cclass = FileDirObjectStore.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_STORE);
    private static final long serialVersionUID = 1767083865060772669L;

    // Store area for header information preserved in the ObjectStore.
    protected static final long headerIdentifier = 101;
    // Reserve sequence numbers 102-200 for future use.
    protected static final long initialSequenceNumber = 200;

    protected static final int version = 1;

    // The set of the storedObjectIdentifiers representing all ManagedObjects on disk.
    private transient java.util.Set managedObjectsOnDisk;
    // The ManagedObjects waiting to be written to disk, indexed by the storedObjectIdentifier.
    // We hold the ManagedObject in this map not the Token in order to force it to remain in memory until
    // we have written it.
    private transient volatile java.util.Map managedObjectsToWrite;
    // When a checkpoint becomes active this is the set of ManagedObjects that will be
    // that will be written as part of the checkpoint;
    protected transient java.util.Map checkpointManagedObjectsToWrite;
    // The objects waiting to be deleted, indexed by the storedObjectIdentifier.
    private transient volatile java.util.Map tokensToDelete;
    // When a checkpoint becomes active this is the set of Tokens that will be 
    // deleted as part of the checkpoint.
    protected transient java.util.Map checkpointTokensToDelete;
    // The actual name of the directory the files live in. 
    String storeDirectoryName;

    /**
     * Constructor
     * 
     * @param storeName identifies the ObjecStore and the file directory.
     * @param objectManager the ObjectManager that manages this store.
     * @throws ObjectManagerException
     */
    public FileDirObjectStore(String storeName,
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
    } // FileDirObjectStore().

    /**
     * Constructor
     * 
     * @param storeName identifies the ObjecStore and the file directory.
     * @param objectManager that manages this store.
     * @param storeStrategy The storeage strategy, one of STRATEGY_XXX:
     * @throws ObjectManagerException
     */
    public FileDirObjectStore(String storeName,
                              ObjectManager objectManager,
                              int storeStrategy)
        throws ObjectManagerException
    {
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
    } // FileDirObjectStore().

    // --------------------------------------------------------------------------
    // extends ObjectStore.
    // --------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#open(com.ibm.ws.objectManager.ObjectManagerState)
     */
    public synchronized void open(ObjectManagerState objectManagerState)
                    throws ObjectManagerException
    {
        final String methodName = "open";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        methodName,
                        new Object[] { objectManagerState });

        super.open(objectManagerState);

        managedObjectsOnDisk = new java.util.HashSet();
        managedObjectsToWrite = new ConcurrentHashMap(concurrency);
        tokensToDelete = new ConcurrentHashMap(concurrency);

        storeDirectoryName = (String) objectManagerState.objectStoreLocations.get(storeName);
        if (storeDirectoryName == null)
            storeDirectoryName = storeName;
        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(this,
                        cclass,
                        methodName,
                        new Object[] { "storeName:150", storeName, storeDirectoryName });

        // During restart the file directory must exist.
        // This test a voids creating unnecessary empty files in the case where the file does not already 
        // exist at a warm start. It is not foolproof though, if the file is deleted by another process
        // between here and the RandomAccesFile constructor we will create a new empty file.    
        if (objectManagerState.getObjectManagerStateState() == ObjectManagerState.stateReplayingLog
            && !(new java.io.File(storeDirectoryName).exists())) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { "File does not exist:159" });
            throw new NonExistentObjectStoreFileException(this,
                                                          storeDirectoryName);
        } // if.

        // Make the directory to write the objects to.
        java.io.File storeDirectory = new java.io.File(storeDirectoryName);
        if (!(storeDirectory.isDirectory())) {
            storeDirectory.mkdirs();
        } // if (!(storeDirectory.isDirectory())).

        // Now look at the header file and initialise the ObjectStore.
        try {
            String storedHeaderFileName = storeDirectoryName + java.io.File.separator + headerIdentifier;
            java.io.RandomAccessFile headerFile = new java.io.RandomAccessFile(storedHeaderFileName,
                                                                               "r");
            int versionRead = headerFile.readInt();
            long objectStoreIdentifierRead = headerFile.readLong();
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            "open",
                            new Object[] { new Integer(versionRead),
                                          new Long(objectStoreIdentifierRead) });
            // Use the saved value if we are a new store.
            if (objectStoreIdentifier == IDENTIFIER_NOT_SET)
                objectStoreIdentifier = (int) objectStoreIdentifierRead;

            sequenceNumber = headerFile.readLong();
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            "open",
                            new Object[] { new Long(sequenceNumber) });

        } catch (java.io.FileNotFoundException exception) {
            // The file does not exist.
            // No FFDC Code Needed, this may be first use of a store.
            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                trace.event(this,
                            cclass,
                            "java.io.FileNotFoundException caught opening header",
                            exception);

            // Set initial values.      

            // Sequence numbers 0-200 are reserved.    
            sequenceNumber = initialSequenceNumber;

            writeHeader();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "open", exception, "1:212:1.17");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "open");
            throw new PermanentIOException(this,
                                           exception);
        } //  catch (java.io.IOException exception).

        // Are we interested in what's already on the disk?
        if (storeStrategy == STRATEGY_KEEP_UNTIL_NEXT_OPEN) {
            clear();
        } // if storeStrategy...

        // Find the next store sequence number that we can use.
        // Do this by searching the store.
        String ExistingFiles[]; // Array of existing file names.
        ExistingFiles = storeDirectory.list();
        synchronized (inMemoryTokens) {
            for (int iFile = 0; // Index to existing files.
            iFile < ExistingFiles.length; iFile++) {
                sequenceNumber = Math.max((new Long(ExistingFiles[iFile])).longValue(),
                                          sequenceNumber);
                managedObjectsOnDisk.add(new Long(ExistingFiles[iFile]));
            } // for loop over existing Files.
        } // synchronized (inMemoryTokens).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "open");
    } // open().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#get(com.ibm.ws.objectManager.Token)
     */
    public ManagedObject get(Token storedToken)
                    throws ObjectManagerException {
        final String methodName = "get";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { storedToken });

        byte[] managedObjectBytes = null;

        try {
            String storedObjectFileName = storeDirectoryName + java.io.File.separator + storedToken.storedObjectIdentifier;

            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            methodName,
                            new Object[] { storedObjectFileName });

            java.io.RandomAccessFile storeFile = new java.io.RandomAccessFile(storedObjectFileName,
                                                                              "r");
            long fileLength = storeFile.length();
            managedObjectBytes = new byte[(int) fileLength];
            storeFile.readFully(managedObjectBytes);
            storeFile.close();

        } catch (java.io.FileNotFoundException exception) {
            // No FFDC Code Needed.
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           exception);
            return null;

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:286:1.17");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           exception);
            throw new TemporaryIOException(this,
                                           exception);
        } // catch IOException.

        // Recover the ManagedObject from its serialized bytes.
        ManagedObject objectFromStore = ManagedObject.restoreFromSerializedBytes(managedObjectBytes,
                                                                                 objectManagerState);
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { objectFromStore });
        return objectFromStore;
    } // get().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#reserve(int)
     */
    public void reserve(int deltaSize)
                    throws ObjectManagerException
    {
        //TODO Needs an implementation.
    } // reserve().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#add(com.ibm.ws.objectManager.ManagedObject, boolean, int)
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

        // TODO Perhaps it would be better if managedObjectsToWrite were just a collection 
        //      based on a set of circular buffers.

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
            } // for(;;)
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
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "remove",
                        new Object[] { token, new Boolean(requiresCurrentCheckpoint) });

        super.remove(token, requiresCurrentCheckpoint);

        // Cancel any writing.
        managedObjectsToWrite.remove(new Long(token.storedObjectIdentifier));

        // Now Schedule the deletion of the disk copy.

        // TODO see TODO for tokens toWrite in add() above.
        if (storeStrategy == STRATEGY_SAVE_ONLY_ON_SHUTDOWN) {
            // We dont need to add Objects that are not in the directory as they wont need to be 
            // deleted. This is important for Stores only written at shutdown as they may 
            // accumulate a lot of delete operations.
            if (managedObjectsOnDisk.contains(new Long(token.storedObjectIdentifier)))
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
            // Repeat attempts to insert the token into the tokensToDelete util we find one that 
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

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "remove");
    } // End of method remove.

    /**
     * Capture the ManagedObjects to write and delete as part of the checkpoint.
     */
    synchronized void captureCheckpointManagedObjects()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "captureCheckpointManagedObjectsremove"
                            );

        // Now that we are synchronized check that we have not captured the checkpoint sets already. 
        if (checkpointManagedObjectsToWrite == null) {
            // Take the tokens to write first, if we miss a delete we will catch it next time. 
            // The managedObjectsToWrite and tokensToDelete sets are volatile so users of the store will move to them 
            // promptly.
            checkpointManagedObjectsToWrite = managedObjectsToWrite;
            managedObjectsToWrite = new ConcurrentHashMap(concurrency);
            checkpointTokensToDelete = tokensToDelete;
            tokensToDelete = new ConcurrentHashMap(concurrency);
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "captureCheckpointManagedObjects");
    } // captureCheckpointManagedObjects().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#clear()
     */
    protected synchronized void clear()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "clear"
                            );

        super.clear();

        managedObjectsOnDisk = new java.util.HashSet();
        managedObjectsToWrite = new ConcurrentHashMap(concurrency);
        tokensToDelete = new ConcurrentHashMap(concurrency);

        deleteDirectory(storeDirectoryName);

        // Make the directory to write the objects to.
        java.io.File storeDirectory = new java.io.File(storeDirectoryName);
        if (!(storeDirectory.isDirectory())) {
            storeDirectory.mkdirs();
        } // if (!(storeDirectory.isDirectory())).

        // Sequence numbers 0-200 are reserved.    
        sequenceNumber = initialSequenceNumber;
        writeHeader();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "clear"
                            );
    } // clear(). 

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#flush()
     */
    public synchronized void flush()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "flush");

        // Capture the ManagedObjects to write and delete.
        java.util.Map ourManagedObjectsToWrite = managedObjectsToWrite;
        managedObjectsToWrite = new ConcurrentHashMap(concurrency);
        java.util.Map ourTokensToDelete = tokensToDelete;
        tokensToDelete = new ConcurrentHashMap(concurrency);

        // Write the managed objects to disk.
        // Use of ConcurrentHashMap makes this a safe copy of the set at the time we construct the iterator.
        java.util.Iterator managedObjectIterator = ourManagedObjectsToWrite.values()
                        .iterator();
        while (managedObjectIterator.hasNext()) {
            ManagedObject managedObject = (ManagedObject) managedObjectIterator.next();
            write(managedObject);
        } // While managedObjectIterator.hasNext().

        // Delete the managed objects from disk.
        // Use of ConcurrentHashMap makes this a safe copy of the set at the time we construct the iterator.
        java.util.Iterator tokenIterator = ourTokensToDelete.values()
                        .iterator();
        while (tokenIterator.hasNext()) {
            Token token = (Token) tokenIterator.next();
            if (managedObjectsOnDisk.contains(new Long(token.storedObjectIdentifier))) {
                java.io.File storeFile = new java.io.File(storeDirectoryName
                                                          + java.io.File.separator
                                                          + token.storedObjectIdentifier);
                storeFile.delete();
                managedObjectsOnDisk.remove((new Long(token.storedObjectIdentifier)));
            } // if ... contains.
        } // While tokenIterator.hasNext().

        // Write the header and force to disk.
        writeHeader();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "flush");
    } // flush().

    private transient Set tokenSet; // Initialised if used.

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ObjectStore#tokens()
     */
    public Set tokens() {
        if (tokenSet == null) {
            tokenSet = new AbstractSetView() {
                public long size() {
                    return managedObjectsOnDisk.size();
                } // size().

                public Iterator iterator() {
                    final java.util.Iterator tokenIterator = managedObjectsOnDisk.iterator();
                    return new Iterator() {

                        public boolean hasNext()
                        {
                            return tokenIterator.hasNext();
                        } // hasNext().

                        public Object next()
                        {
                            Token token = new Token(FileDirObjectStore.this, ((Long) tokenIterator.next()).longValue());
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
     * Writes an object to hardened storage, but may return before the write completes.
     * 
     * @param managedObject to be written.
     * @throws ObjectManagerException
     */
    private void write(ManagedObject managedObject)
                    throws ObjectManagerException
    {
        final String methodName = "write";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { managedObject });

        // Pick up and write the latest serialized bytes.
        ObjectManagerByteArrayOutputStream serializedBytes = null;
        if (usesSerializedForm) {
            // Pick up and write the latest serialized bytes.

            // It is possible that several threads requested a write one after the
            // other each getting the request added to a different managedObjetsToWrite table,
            // however the first of these through here will clear serializedBytes.
            // It is also possible that the transaction state of the managed object
            // was restored from a checkpoint, in which case the serialized object
            // may already be in the ObjectStore, and the serializedBytes will again be null.

            // Is the Object deleted? It may have got deleted after we release the
            // synchronize lock when we
            // captured the tokensToWrite hashtable but before we actually try to write it.
            if (managedObject.state != ManagedObject.stateDeleted)
                serializedBytes = managedObject.freeLatestSerializedBytes();

        } else {
            // Not logged so use the current serialized bytes, as long as its not part of a transaction.
            // If it is part of a transaction then the transaction will hold the ManagedObject in memory.
            // Not locked because this is only used by SAVE_ONLY_ON_SHUTDOWN stores at shutdown
            // when no appliaction threads are active.
            if (managedObject.state == ManagedObject.stateReady)
                serializedBytes = managedObject.getSerializedBytes();
        } // if ( usesSerializedForm ).

        // It is possible that several threads requested a write one after the other each getting the request added
        // to a different tokensToWrite table, however the first of these through here will clear serializedBytes.
        if (serializedBytes != null) { // Already done by another thread?
            try {
                java.io.FileOutputStream storeFileOutputStream = new java.io.FileOutputStream(storeDirectoryName
                                                                                              + java.io.File.separator
                                                                                              + managedObject.owningToken.storedObjectIdentifier);
                storeFileOutputStream.write(serializedBytes.getBuffer(), 0, serializedBytes.getCount());
                storeFileOutputStream.flush();
                storeFileOutputStream.close();

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    exception,
                                                    "1:656:1.17");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { exception });
                throw new PermanentIOException(this,
                                               exception);
            } // catch java.io.IOException.

            managedObjectsOnDisk.add(new Long(managedObject.owningToken.storedObjectIdentifier));
        } // if (serializedBytes != null ).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // write().

    /**
     * Write header information for disk in the file headerIdentifier
     * and force it to disk.
     * 
     * @throws ObjectManagerException
     */
    public void writeHeader()
                    throws ObjectManagerException
    {
        final String methodName = "writeHeader";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        try {
            java.io.FileOutputStream headerOutputStream = new java.io.FileOutputStream(storeDirectoryName
                                                                                       + java.io.File.separator
                                                                                       + headerIdentifier);
            java.io.DataOutputStream dataOutputStream = new java.io.DataOutputStream(headerOutputStream);
            java.io.FileDescriptor fileDescriptor = headerOutputStream.getFD();
            dataOutputStream.writeInt(version);
            dataOutputStream.writeLong(objectStoreIdentifier);
            dataOutputStream.writeLong(sequenceNumber);
            dataOutputStream.flush();
            headerOutputStream.flush();
            fileDescriptor.sync(); // Force buffered records to disk.
            headerOutputStream.close();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:706:1.17");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { exception });
            throw new PermanentIOException(this,
                                           exception);
        } // catch java.io.IOException.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // writeHeader().

    public void deleteDirectory(String directoryName)
    {
        java.io.File file = new java.io.File(directoryName);
        doDelete(file);
    } // deleteDirectory().

    private void doDelete(java.io.File file)
    {
        // Make the directory to write the objects to.
        if (file.isDirectory()) {

            // Find all of the files in the next level.

            // JCLRM does not have listFiles...
            // java.io.File nextFiles[] = file.listFiles();

            String[] files = file.list();
            java.io.File[] nextFiles = null;
            if (files != null) {
                nextFiles = new java.io.File[files.length];
                for (int iFile = 0; iFile < nextFiles.length; iFile++) {
                    nextFiles[iFile] = new java.io.File(file.getPath(),
                                                        files[iFile]);
                }
            }

            if (nextFiles != null) {
                for (int iFile = 0; // Index to next files.
                iFile < nextFiles.length; iFile++) {
                    java.io.File nextFile = nextFiles[iFile];
                    if (nextFile.isDirectory()) {
                        doDelete(nextFile);
                    } else {
                        nextFile.delete();
                    }
                } // for loop over existing Files.
            }
        } // if (file.isDirectory()).
        file.delete();
    } // doDelete().
} // class FileDirObjectStore.
