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

import com.ibm.ws.objectManager.utils.Printable;
import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

public class TreeMap
                extends AbstractTreeMap
                implements SortedMap, SimplifiedSerialization, Printable
{
    private static final Class cclass = TreeMap.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_MAPS);
    private static final long serialVersionUID = -9183890410133257833L;

    // The number of bytes of logSpace to reserve for an addition to or removal from the map so that the 
    // later stages can complete.
    // They assume a maximum tree depth of 64 based on assumption of there being less than Long.MAX_VALUE entries 
    // in the tree and the tree needing to be rebalanced at every level after an insert or delete.
    private static final long logSpaceForAdd = TransactionOptimisticReplaceLogRecord.maximumSerializedSize()
                                               + FileLogOutput.partHeaderLength
                                               + maximumSerializedSize()
                                               + 64 * TreeMap.Entry.maximumSerializedSize()

                                               + TransactionOptimisticReplaceLogRecord.maximumSerializedSize()
                                               + FileLogOutput.partHeaderLength
                                               + Token.maximumSerializedSize() * (64 + 1);
    private static final long logSpaceForDelete = TransactionOptimisticReplaceLogRecord.maximumSerializedSize()
                                                  + FileLogOutput.partHeaderLength
                                                  + Token.maximumSerializedSize() * 1 // For notify.
                                                  + maximumSerializedSize()
                                                  + 64 * TreeMap.Entry.maximumSerializedSize()

                                                  + TransactionOptimisticReplaceLogRecord.maximumSerializedSize()
                                                  + FileLogOutput.partHeaderLength
                                                  + Token.maximumSerializedSize() * (64 + 1 + 1);
    // Users of size must be synchronized on TreeMap.this.
    // The size once commited. 
    // protected long size;
    // The number of entries in the tree, visible to all transactions.
    protected transient long availableSize;

    // All replacements are done optimistically and written as a single log record
    // so that we can be sure to get all of them or none of them and keep the integrity
    // of the tree. Use java.util.Set to eliminate duplicate entries.
    private transient java.util.Set managedObjectsToAdd;
    private transient java.util.Set managedObjectsToReplace;
    private transient java.util.Set tokensToNotify;

    // Space reserved for transaction operations being performed under the synchronize lock 
    // on TreeMap.this.
    transient long reservedSpaceInStore;

    /**
     * Make a new TreeMap in the ObjectStore.
     * 
     * @param transaction which must commit for the Map to exist.
     * @param objectStore that holds the tree and all the entries
     *            in the tree, but not necessarily the values referenced by the tree.
     * @exception ObjectManagerException
     * @see Comparable
     */
    public TreeMap(Transaction transaction,
                   ObjectStore objectStore)
        throws ObjectManagerException {
        super();
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { transaction,
                                                                objectStore });

        objectStore.allocate(this);
        reserveAndAdd(transaction, objectStore);

        managedObjectsToAdd = new java.util.HashSet(1);
        managedObjectsToReplace = new java.util.HashSet(); // Holds list of replacements.
        tokensToNotify = new java.util.HashSet(1); // Holds this list of tokens to notify about replacements.     

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // TreeMap().

    void reserveAndAdd(Transaction transaction,
                       ObjectStore objectStore)
                    throws ObjectManagerException {
        reservedSpaceInStore = maximumSerializedSize() + owningToken.objectStore.getAddSpaceOverhead();
        owningToken.objectStore.reserve((int) reservedSpaceInStore, true);
        transaction.add(this);
        owningToken.objectStore.reserve(-(int) reservedSpaceInStore, false);
    } // reserveAndAdd().

    /**
     * Constructs a TreeMap using the given comparator.
     * 
     * @param comparator used to sort this map.
     * @param transaction that must commit for the Map to exist.
     * @param objectStore that holds the tree and all the entries in the tree, but not necessarily the values referenced
     *            by the tree.
     * @exception ObjectManagerException
     */
    public TreeMap(java.util.Comparator comparator,
                   Transaction transaction,
                   ObjectStore objectStore)
        throws ObjectManagerException {
        super(comparator);
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { comparator,
                                                                transaction,
                                                                objectStore });

        objectStore.allocate(this);
        reservedSpaceInStore = maximumSerializedSize() + owningToken.objectStore.getAddSpaceOverhead();
        owningToken.objectStore.reserve((int) reservedSpaceInStore, true);
        transaction.add(this);
        owningToken.objectStore.reserve(-(int) reservedSpaceInStore, false);

        managedObjectsToAdd = new java.util.HashSet(1);
        managedObjectsToReplace = new java.util.HashSet(); // Holds list of replacements.
        tokensToNotify = new java.util.HashSet(1); // Holds this list of tokens to notify about replacements.   

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // TreeMap().

    /**
     * Helper method to deal with reservation and unreservation during add
     * 
     * @param transaction
     * @param objectStore
     * @throws ObjectManagerException
     */
    public TreeMap(SortedMap map, Transaction transaction, ObjectStore objectStore)
        throws ObjectManagerException {
        this(transaction, objectStore);
        putAll(map, transaction);
    } // TreeMap(). 

    AbstractTreeMap.Entry makeEntry(Object key)
                    throws ObjectManagerException
    {
        throw new UnsupportedOperationException("makeEntry not supported");
    }

    AbstractTreeMap.Entry makeEntry(Object key,
                                    Token value)
                    throws ObjectManagerException
    {
        throw new UnsupportedOperationException("makeEntry not supported");
    }

    Iterator makeTreeMapIterator(AbstractMapEntry.Type value)
                    throws ObjectManagerException
    {
        return new TreeMapIterator(value);
    }

    Iterator makeTreeMapIterator(AbstractMapEntry.Type value,
                                 AbstractTreeMap.Entry startNode,
                                 boolean checkEnd,
                                 Object end)
                    throws ObjectManagerException
    {
        return new TreeMapIterator(value,
                                   startNode,
                                   checkEnd,
                                   end);
    }

    AbstractTreeMap.SubMap makeSubMap(Object start,
                                      Object end)
    {
        return (SubMap) new SubMap(start, this, end);
    }

    /**
     * The space we reserve in the ObjectStore before we begin a remove operation.
     * 
     * @return long a worst case estimate of the ObjectStore space needed to remove an entry
     *         from this list.
     */
    long storeSpaceForRemove() {
        return TreeMap.maximumSerializedSize() // Map header.
               + 64 * TreeMap.Entry.maximumSerializedSize() // Balanced path.  
               + 65 * owningToken.objectStore.getAddSpaceOverhead(); // Store overhead for all of the above.
    } // storeSpaceForRemove()  

    /**
     * The space we reserve in the ObjectStore before we begin an add operation.
     * 
     * @return long a worst case estimate of the ObjectStore space needed to add an entry
     *         to this list.
     */
    long storeSpaceForAdd() {
        return TreeMap.maximumSerializedSize() // Map header.
               + 64 * TreeMap.Entry.maximumSerializedSize() // Balanced path.  
               + 65 * owningToken.objectStore.getAddSpaceOverhead() // Store overhead for all of the above.
               + storeSpaceForRemove(); // In case we backoutTheAdd.
    } // storeSpaceForAdd()

    /**
     * Returns the number of key-value mappings in this map , which ara available to
     * the transaction.
     * 
     * @param transaction which sees the tree as this size.
     * @return the number of key-value mappings in this map.
     * @exception ObjectManagerException
     * 
     * @see com.ibm.ws.objectManager.Collection#size(com.ibm.ws.objectManager.Transaction)
     */
    public long size(Transaction transaction)
                    throws ObjectManagerException
    {
        // No trace because this is used by toString(), and hence by trace itself;
        long sizeFound; // For return;

        synchronized (this) {
            sizeFound = availableSize;
            // Move through the map adding in any extra available entries.
            if (transaction != null) {
                Entry entry = firstEntry(transaction);
                while (entry != null) {
                    if (entry.state == Entry.stateToBeAdded && entry.lockedBy(transaction))
                        sizeFound++;
                    entry = successor(entry,
                                      transaction);
                } // while (entry != null).
            } // if (transaction != null). 
        } // synchronized (this). 

        return sizeFound;
    } // size().

    /**
     * Returns the number of key-value mappings in this map including those currently being
     * added or deleted by transactions.
     * 
     * @return long the number of key-value mappings in this map.
     */
    public long size()
    {
        //TODO Needs to include toBeDeleted elements.
        return size;
    } // size().

    /**
     * Determines if the tree is empty as viewed by the transaction.
     * Returns true if there are no entries visible to the transaction and false
     * if there are entries visible.
     * 
     * @param transaction the transaction which sees the tree as empty.
     * @return true if no entries are visible, false if there are entries visible.
     * @exception ObjectManagerException
     */
    public synchronized boolean isEmpty(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "isEmpty"
                        , new Object[] { transaction });
        boolean returnValue;
        if (firstEntry(transaction) == null) {
            returnValue = true;
        } else {
            returnValue = false;
        }
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "isEmpty"
                       , new Object[] { new Boolean(returnValue) }
                            );
        return returnValue;
    } // isEmpty().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#containsKey(java.lang.Object, com.ibm.ws.objectManager.Transaction)
     */
    public synchronized boolean containsKey(Object key
                                            , Transaction transaction
                    )
                                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "get"
                        , new Object[] { key, transaction });

        boolean returnValue = false;
        if (getEntry(key, transaction) != null)
            returnValue = true;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "containsKey"
                       , "returns " + returnValue + "(boolean)"
                            );
        return returnValue;
    } // containsKey().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#get(java.lang.Object, com.ibm.ws.objectManager.Transaction)
     */
    public synchronized Token get(Object key,
                                  Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "get",
                        new Object[] { key,
                                      transaction });

        Entry entry = getEntry(key, transaction);
        Token value = null;
        if (entry != null)
            value = entry.getValue();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "get",
                       new Object[] { entry, value });
        return value;
    } // get().

    /**
     * Find the first Entry in the Tree that Maps the given key.
     * 
     * @param key who's Entry is to be found.
     * @param transaction controling visibility of Entries.
     * @return Entry first matching the key, or null if there is none.
     * @throws ObjectManagerException
     */
    private synchronized Entry getEntry(Object key,
                                        Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getEntry",
                        new Object[] { key,
                                      transaction });

        // Find the first Entry for this key, commited or not.
        Entry entry = getEntry(key);

        // Capture the ObjectManagers unlock sequence number. Anything unlocked after
        // this will not count. The next lime delimits the pont before which additions to
        // list must committed and unlocked.
        long transactionUnlockPoint = Long.MAX_VALUE; // All transactions end before this.
        if (transaction != null)
            transactionUnlockPoint = transaction.getTransactionUnlockSequence();

        // Look forward through any duplicates to find an entry in a valid state.
        duplicateSearch: while (entry != null) {
            if (entry.state == Entry.stateAdded && !entry.wasLocked(transactionUnlockPoint)) {
                break duplicateSearch;

                // Transaction may be null, indicaiting sateToBeAdded links are not eligible.
            } else if ((entry.state == Entry.stateToBeAdded) && transaction != null && entry.lockedBy(transaction)) {
                break duplicateSearch;
            }

            entry = (Entry) successor(entry);
            if (entry != null
                && compare(key, entry.key) != 0)
                entry = null;
        } // duplicateSearch: (entry != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getEntry",
                       new Object[] { entry });
        return entry;
    } // getEntry().

    /**
     * Returns the first (lowest) key currently in this sorted map
     * which is visible to the transaction.
     * 
     * @param transaction the transaction which determines the first visible key.
     * @return the first (lowest) key currently in this sorted map.
     * @exception ObjectManagerException
     */
    public synchronized Object firstKey(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "firstKey",
                        new Object[] { transaction });
        Entry entry = firstEntry(transaction);
        Object returnKey = entry.getKey();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "firstKey",
                       new Object[] { returnKey });
        return returnKey;
    } // firstKey().

    /**
     * Returns the last (highest) key currently in this sorted map.
     * 
     * @param transaction which controls visibilty of the Entry mapped by the key.
     * @return the last (highest) key currently in this sorted map.
     * @throws ObjectManagerException
     */
    public synchronized Object lastKey(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "lastKey",
                        new Object[] { transaction });

        Entry entry = lastEntry(transaction);
        Object returnKey = entry.getKey();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "lastKey",
                       new Object[] { returnKey });
        return returnKey;
    } // lastKey().

    public void putAll(Map otherMap,
                       Transaction transaction)
                    throws ObjectManagerException
    {
        // Capture the ObjectManagers unlock sequence number. Anything unlocked after 
        // this will not count. The next lime delimits the pont before which additions to 
        // list must committed and unlocked.
        long transactionUnlockPoint = Long.MAX_VALUE; // All transactions end before this.
        if (transaction != null)
            transactionUnlockPoint = transaction.getTransactionUnlockSequence();

        // Make a dirty scan of the map.
        for (Iterator iterator = otherMap.entrySet().iterator(); iterator.hasNext();) {
            Entry entry = (Entry) iterator.next();
            if (((entry.state == Entry.stateAdded) && !entry.wasLocked(transactionUnlockPoint))
                || ((entry.state == Entry.stateToBeAdded)
                && entry.lockedBy(transaction))) {
                // Found one added by the same transaction or was unlocked before we started the scan.
                //TODO If we fail in here because of lack of log or store sace some of the
                //     entries may have already been added.
                put(entry.getKey(),
                    entry.getValue(),
                    transaction);
            }
        } // for...
    } // putAll().

    /**
     * Returns the first Entry matching the key or null if the map
     * does not contain an entry for the key. The entry returned may be uncommited.
     * 
     * @param key whose Entry is to be returned.
     * @return Entry matching the key or null if there is none.
     * @exception ObjectManagerException
     */
    private Entry getEntry(Object key)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getEntry"
                        , new Object[] { key });

        Entry entry = (Entry) super.find(key);
        if (entry != null) {
            // Look for the youngest duplicate.  
            duplicateSearch: for (;;) {
                Entry predecessor = (Entry) predecessor(entry);
                if (predecessor == null)
                    break duplicateSearch;
                if (compare(key, predecessor.key) != 0)
                    break duplicateSearch;
                entry = predecessor;
            } // for (;;).
        } // if (entry != null)  

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getEntry",
                       new Object[] { entry });
        return entry;

    } // getEntry().

    /**
     * Associates the specified value with the specified key in this map. If the map previously contained a mapping for
     * this key, the old value is replaced.
     * 
     * @param key with which the specified value is to be associated.
     * @param value to be associated with the specified key.
     * @param transaction the put will be globally visible once this transaction commits.
     * 
     * @return Token of the previous value associated with specified key, or <tt>null</tt> if there was no mapping for key. A
     *         <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with the
     *         specified key.
     * @throws ClassCastException key cannot be compared with the keys currently in the map.
     * @throws NullPointerException key is <tt>null</tt> and this map uses natural order, or its comparator does not
     *             tolerate <tt>null</tt> keys.
     * @throws ObjectManagerException
     */
    public synchronized Token put(Object key,
                                  Token value,
                                  Transaction transaction)
                    throws ObjectManagerException
    {
        return put(key,
                   value,
                   transaction,
                   false);
    } // put().

    /**
     * Associates the specified value with the specified key in this map. If the map previously contained a mapping for
     * this key, the old value is left alone and a new one added.
     * 
     * @param key with which the specified value is to be associated.
     * @param value to be associated with the specified key.
     * @param transaction the put will be globally visible once this transaction commits.
     * 
     * @throws ClassCastException key cannot be compared with the keys currently in the map.
     * @throws NullPointerException key is <tt>null</tt> and this map uses natural order, or its comparator does not
     *             tolerate <tt>null</tt> keys.
     * @throws ObjectManagerException
     */
    public synchronized void putDuplicate(Object key,
                                          Token value,
                                          Transaction transaction)
                    throws ObjectManagerException {
        put(key,
            value,
            transaction,
            true);

    } // putDuplicate().

    /**
     * Associates the specified value with the specified key in this map. If the map previously contained a mapping for
     * this key, the old value is replaced.
     * 
     * @param key with which the specified value is to be associated.
     * @param value to be associated with the specified key.
     * @param transaction the put will be globally visible once this transaction commits.
     * @param allowDuplicates true if duplicates are allowed, false if they are not.
     * 
     * @return Token of the previous value associated with specified key, or <tt>null</tt> if there was no mapping for key. A
     *         <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with the
     *         specified key.
     * @throws ClassCastException key cannot be compared with the keys currently in the map.
     * @throws NullPointerException key is <tt>null</tt> and this map uses natural order, or its comparator does not
     *             tolerate <tt>null</tt> keys.
     * @throws ObjectManagerException
     */
    private Token put(Object key,
                      Token value,
                      Transaction transaction,
                      boolean allowDuplicates)
                    throws ObjectManagerException
    {
        final String methodName = "put";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { key,
                                                                value,
                                                                transaction,
                                                                new Boolean(allowDuplicates) });

        Token returnValue = null; // The returned value.
        // The following set to true if the existing entry is replaced but was added by the same transaction.
        // (state of replaced entry is mustBeDeleted)
        boolean mustBeDeleted = false;

        // We could to follow a scheme like the one in LinkedList.addEntry where we do a composite add
        // with no window for another thread ( eg the checkpoint thread) to backout the transaction between add
        // optimistic replace. We don't do this because removing a node from a binary tree will not always restore
        // it to exactly the same state as before it was added. Instead we synchronize on InternalTransaction
        // through this code. A backout thread would lock transaction then managed object in the 
        // preBackout callback.
        synchronized (transaction.internalTransaction) {
            synchronized (this) {
                // Prevent other transactions using the tree until its creation is committed.
                if (!(state == stateReady)
                    && !((state == stateAdded) && lockedBy(transaction))) {

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, new Object[] { new Integer(state),
                                                                           stateNames[state] });
                    throw new InvalidStateException(this, state, stateNames[state]);
                }

                managedObjectsToReplace.clear(); // Reset from last time.
                // Assume we will delete a duplicate entry as well as adding a new one.
                reservedSpaceInStore = storeSpaceForAdd() + storeSpaceForRemove();
                owningToken.objectStore.reserve((int) reservedSpaceInStore, false);

                try {
                    // Search for the insertion point starting at the root.
                    if (root == null) { // Is the tree empty?
                        Entry newEntry = new Entry(this,
                                                   key,
                                                   value,
                                                   null,
                                                   transaction);

                        // If the add fails, because the log is full, no harm done because we will not have affected the
                        // structure of the tree. We now also need to reserve log space for the optimistic replace
                        // log records needed needed to reflect the modified structure of the tree and also for another
                        // set in case we need to roll back the changes if backout is executed.
                        transaction.add(newEntry,
                                        logSpaceForAdd + logSpaceForDelete);
                        setRoot(newEntry);
                        size++;

                    } else { // if (root != null).

                        Entry currentEntry = (Entry) getRoot();

                        // Search the tree looking for a free leaf.
                        keySearch: while (true) {

                            int comparison = compare(key,
                                                     currentEntry.key);
                            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                                trace.debug(this, cclass, methodName, new Object[] { currentEntry });
                            if (comparison == 0) { // Matching entry found?
                                if (allowDuplicates) {
                                    // We allow duplicate keys so just add the new entry after the equal key.
                                    add(key,
                                        value,
                                        currentEntry,
                                        transaction,
                                        logSpaceForAdd + logSpaceForDelete);
                                    break keySearch; // Now included in the tree.

                                } else if (currentEntry.state == Entry.stateAdded
                                           || (currentEntry.state == Entry.stateToBeAdded && currentEntry.lockedBy(transaction))) {

                                    // We want to replace the value at an existing key with a new one.
                                    // Mark the existing entry for deletion and insert a new entry. Until the transaction
                                    // reaches commit both of these will exist in the tree, but will only be visible to
                                    // this transaction.
                                    transaction.delete(currentEntry,
                                                       logSpaceForAdd
                                                                       + 2 * logSpaceForDelete
                                                                       + TransactionAddLogRecord.maximumSerializedSize()); // Log what we intend to do.
                                    currentEntry.requestDelete(transaction);

                                    if (currentEntry.state == Entry.stateToBeDeleted) {
                                        // In the case where we delete an entry added by the same transaction we have not incremented
                                        // the available size yet because we have not yet committed so we would not decrement it.
                                        availableSize--;
                                    } else {
                                        // We don't need to reserve space in the store for removal of the entry if the
                                        // transaction backs out because this was done when the entry was added by
                                        // the same transaction.
                                        mustBeDeleted = true;
                                    }

                                    returnValue = currentEntry.getValue();
                                    // Add the new entry after the current one.
                                    add(key,
                                        value,
                                        currentEntry,
                                        transaction,
                                        -TransactionAddLogRecord.maximumSerializedSize());
                                    break keySearch; // Now included in the tree.

                                } else if (currentEntry.willBeDeleted(transaction)) {
                                    // Skip over a deleted Entry with the same key if it is followed by another duplicate.
                                    Entry nextEntry = (Entry) successor(currentEntry);
                                    if (nextEntry != null && (compare(key, nextEntry.key) == 0)) {
                                        currentEntry = nextEntry;
                                    } else {
                                        // Add the new value.
                                        add(key,
                                            value,
                                            currentEntry,
                                            transaction,
                                            logSpaceForAdd + logSpaceForDelete);
                                        break keySearch;
                                    } // if (nextEntry...

                                } else { // Put cannot replace this value.
                                    InternalTransaction lockingTransaction = null;
                                    TransactionLock transactionLock = currentEntry.getTransactionLock();
                                    if (transactionLock != null)
                                        if (transactionLock.isLocked())
                                            lockingTransaction = transactionLock.getLockingTransaction();
                                    undoPut();
                                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                        trace.exit(this,
                                                   cclass,
                                                   methodName,
                                                   "via DuplicateKeyException"
                                                                   + " currentEntry.state ="
                                                                   + currentEntry.state
                                                                   + "(int) "
                                                                   + stateNames[currentEntry.state]
                                                                   + "(String)");
                                    throw new DuplicateKeyException(this,
                                                                    key,
                                                                    currentEntry,
                                                                    lockingTransaction);
                                } // if OK to replace.

                            } else if (comparison < 0) { // Are we in the less than branch?
                                if (currentEntry.left == null) {// We have reached a less than leaf.
                                    Entry newEntry = new Entry(this,
                                                               key,
                                                               value,
                                                               currentEntry,
                                                               transaction);
                                    transaction.add(newEntry,
                                                    logSpaceForAdd + logSpaceForDelete);
                                    size++;
                                    currentEntry.setLeft(newEntry);
                                    balance(newEntry);
                                    break keySearch; // Now included in the tree.
                                } // if (currentEntry.left == null).
                                  // Look further into the less than tree.
                                currentEntry = (Entry) currentEntry.getLeft();

                            } else { // So its the greater than branch.
                                if (currentEntry.right == null) { // We have reached a greater than than leaf.
                                    Entry newEntry = new Entry(this,
                                                               key,
                                                               value,
                                                               currentEntry,
                                                               transaction);
                                    transaction.add(newEntry,
                                                    logSpaceForAdd + logSpaceForDelete);
                                    size++;
                                    currentEntry.setRight(newEntry);
                                    balance(newEntry);
                                    break keySearch; // Now included in the tree.
                                } // if (currentEntry.right == null).
                                  // Look further into the greater than tree.
                                currentEntry = (Entry) currentEntry.getRight();

                            } // if (comparator...
                        } // keySearch: while...
                    } // else (current == null).

                    managedObjectsToReplace.add(this); // The anchor has changed.
                    // Harden the updates. This will not fail due to lack of log space because we reserved
                    // the space for the logRecord earlier.
                    transaction.optimisticReplace(null,
                                                  new java.util.ArrayList(managedObjectsToReplace),
                                                  null, // No tokens to delete.
                                                  null, // No tokens to notify.
                                                  -logSpaceForAdd);
                    // Give up reserved space, but keep back enough to remove the added entry if we have to.
                    // Also give back the space needed to delete the duplicate entry if there is no duplicate.
                    if (returnValue == null || mustBeDeleted)
                    {
                        // nothing replaced or if there was it was added in the same transaction.
                        // keep back enough to backout the add
                        owningToken.objectStore.reserve((int) (storeSpaceForRemove() - reservedSpaceInStore), false);
                    }
                    else
                    {
                        // something was replaced, keep back 2 * space for remove, 1 for backing out the add,
                        // 1 for actually doing the remove. Either commit or backout direction will give this back
                        // for each of the added and deleted entries.
                        owningToken.objectStore.reserve((int) (storeSpaceForRemove() * 2 - reservedSpaceInStore), false);
                    }

                } catch (InvalidStateException exception) {
                    // No FFDC Code Needed, user error.
                    // Remove the link we just added.
                    undoPut();

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, exception);
                    throw exception;

                } catch (LogFileFullException exception) {
                    // No FFDC Code Needed, InternalTransaction has already done this.
                    // Remove the link we just added.
                    undoPut();

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, exception);
                    throw exception;

                    // We should not see ObjectStoreFullException because we have preReserved 
                    // the ObjectStore space.
//        } catch (ObjectStoreFullException exception) {
//          // No FFDC Code Needed, InternalTransaction has already done this.
//          // Remove the link we just added.
//          undoPut();
//          
//          if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//            trace.exit(this, cclass, methodName, exception);
//          throw exception;
                } // try.
            } // synchronized (this).
        } // synchronized (transaction.internalTransaction).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { returnValue });
        return returnValue;
    } // put().

    /**
     * Add a new value to the tree at a point where they key is after all less than or equal keys and below the
     * currentEntry.
     * 
     * @param key for the value.
     * @param value to be added.
     * @param currentEntry which starts the search.
     * @param transaction controlling the addition.
     * @param logSpaceDelta the change in log space to be reserved.
     * 
     * @throws ObjectManagerException
     */
    private void add(Object key,
                     Token value,
                     Entry currentEntry,
                     Transaction transaction,
                     long logSpaceDelta)
                    throws ObjectManagerException {
        final String methodName = "add";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { key,
                                                                value,
                                                                currentEntry,
                                                                transaction,
                                                                new Long(logSpaceDelta) });

        Entry newEntry;

        // Place the new Entry at a point greater than or equal to the currentEntry.  
        keySearch: while (true) {
            if (compare(key, currentEntry.key) < 0) { // Less than.
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this, cclass, methodName, new Object[] { "Less than branch.",
                                                                        currentEntry.key });

                if (currentEntry.left == null) { // We have reached a less than leaf.
                    newEntry = new Entry(this
                                         , key
                                         , value
                                         , currentEntry
                                         , transaction
                                    );
                    transaction.add(newEntry
                                    , logSpaceDelta
                                    );
                    currentEntry.setLeft(newEntry);
                    break keySearch; // Now included in the tree.
                } // if (currentEntry.left == null).
                  // Look further into the less than tree.
                currentEntry = (Entry) currentEntry.getLeft();

            } else { // Greater than or equal.
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this, cclass, methodName, new Object[] { "Greater than branch.",
                                                                        currentEntry.key });

                if (currentEntry.right == null) { // Place to the right.
                    // Add into the greater than side.
                    newEntry = new Entry(this
                                         , key
                                         , value
                                         , currentEntry
                                         , transaction
                                    );
                    transaction.add(newEntry
                                    , logSpaceDelta
                                    );
                    currentEntry.setRight(newEntry);
                    break keySearch; // Now included in the tree.
                } // if (currentEntry.right == null).
                // Look further into the greater than tree.
                currentEntry = (Entry) currentEntry.getRight();

            } // if (comparator...
        } // keySearch: while (....

        size++;
        balance(newEntry);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // add().

    /**
     * Reverse the action of addition to the map,
     * used after an add has failed to log anything.
     * 
     * @throws ObjectManagerException
     */
    private void undoPut()
                    throws ObjectManagerException {
        final String methodName = "undoPut";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName);

        // Give back all of the remaining space.
        owningToken.objectStore.reserve((int) -reservedSpaceInStore, false);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // undoPut().

    /**
     * Removes the mapping for this key from this TreeMap if present.
     * 
     * @param key key which is associated with the value to be removed.
     * @param transaction the put will be globally visible once this transaction commits.
     * 
     * @return Token previous value associated with specified key, or <tt>null</tt> if there was no mapping for key. A
     *         <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with the
     *         specified key.
     * 
     * @throws ClassCastException key cannot be compared with the keys currently in the map.
     * @throws NullPointerException key is <tt>null</tt> and this map uses natural order, or its comparator does not
     *             tolerate <tt>null</tt> keys.
     * @exception ObjectManagerException
     */
    public synchronized Token remove(Object key,
                                     Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "remove";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { key,
                                                                transaction });

        Token returnValue = null; // The returned value.

        // Find the entry in to be removed. Actual removal from the structure of
        // the tree takes place when we know the outcome of the transaction is 
        // commit. 
        Entry entry = getEntry(key);

        // Defect 562227
        // We only need to do more (especially reserve space in the store) 
        // if we have found the supplied key in our map.
        if (entry != null) {
            boolean spaceReserved = false;

            // Capture the ObjectManagers unlock sequence number. Anything unlocked after 
            // this will not count. The next line delimits the pont before which additions to 
            // list must committed and unlocked.
            long transactionUnlockPoint = Long.MAX_VALUE; // All transactions end before this.
            if (transaction != null)
                transactionUnlockPoint = transaction.getTransactionUnlockSequence();

            try {
                // Note the different behaviour compared to LinkedLists. Here we hold the
                // lock on the tree while the delete log record is written and then mark the Entry to be 
                // deleted. This means we don't have to worry about the log filling because if it does, we
                // won't reach this point and the state will not change.
                synchronized (transaction.internalTransaction) {
                    synchronized (this) {
                        duplicateSearch: while (entry != null) { // Matching entry?
                            if (compare(key, entry.key) != 0)
                                break duplicateSearch;

                            if ((entry.state == Entry.stateToBeAdded)
                                && entry.lockedBy(transaction)) {
                                // Log what we intend to do. Reserve enough spare log space so that the eventual 
                                // optimistic replace that removes the entry from the tree is certain to succeed 
                                // in being written as well.
                                transaction.delete(entry, logSpaceForDelete);
                                returnValue = entry.getValue();
                                // Mark for deletion. 
                                entry.requestDelete(transaction);
                                // In the case where we delete an entry added by the same transaction we have not incremented 
                                // the available size yet because we have not yet committed so we would not decrement it.

                                break duplicateSearch;

                            } else if ((entry.state == Entry.stateAdded)
                                       && !entry.wasLocked(transactionUnlockPoint)) {
                                // Reserve space in the store, if not available, we fail here.
                                owningToken.objectStore.reserve((int) storeSpaceForRemove(), false);
                                spaceReserved = true;

                                transaction.delete(entry, logSpaceForDelete);
                                returnValue = entry.getValue();
                                // Mark for deletion.
                                entry.requestDelete(transaction);
                                // If the entry was available to all adjust the visible length available.
                                availableSize--;

                                break duplicateSearch;

                            } else { // Put cannot get this one.
                            } // if OK to get.

                            entry = (Entry) successor(entry);
                        } // duplicateSearch while (entry != null).
                    } // synchronized (this).
                } // synchronized (transaction.internalTransaction).
            } catch (LogFileFullException exception) {
                // No FFDC Code Needed, transaction.delete() has already done this.
                // Release space we reserved in the store.
                if (spaceReserved)
                    owningToken.objectStore.reserve(-(int) storeSpaceForRemove(), false);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, exception);
                throw exception;

                // We should not see ObjectStoreFullException because the ObjectStore already 
                // holds sufficient space to guarantee deletes work.
                //    } catch (ObjectStoreFullException exception) {
                //    // No FFDC Code Needed, transaction.delete() has already done this.
                //    unRemove();
                //    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                //    trace.exit(this, cclass, methodName, exception);
                //    throw exception;

            } catch (InvalidStateException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:1057:1.44");
                // Release space we reserved in the store.
                if (spaceReserved)
                    owningToken.objectStore.reserve(-(int) storeSpaceForRemove(), false);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, exception);
                throw exception;
            } // catch (LogFileFullException exception).
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { returnValue });
        return returnValue;
    } // remove().

    /**
     * Removes all mappings from this TreeMap, which are visible to the transaction. Actual deletion
     * of the entries takes place when the transaction commits.
     * 
     * @param transaction to control clearing the map.
     * @exception ObjectManagerException If thrown the map may be partially cleared.
     */
    public synchronized void clear(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "clear"
                          + "transaction=" + transaction + "(Transaction)"
                            );

        // Move through the map deleting each entry as we go.
        Entry entry = firstEntry(transaction);
        while (entry != null) {
            entry.remove(transaction);
            entry = successor(entry
                              , transaction
                            );
        } // while (entry != null).
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "clear"
                            );
    } // clear().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#iterator()
     */
    public Iterator iterator()
                    throws ObjectManagerException
    {
        return values().iterator();
    } // iterator().

    // These views do not create new TreeMaps, just a view of the existing one.

    private class SubMap
                    extends AbstractTreeMap.SubMap
    {
        SubMap(Object start,
               TreeMap treeMap,
               Object end)
        {
            super(start,
                  TreeMap.this,
                  end);
        } // SubMap().

        public boolean containsKey(Object key,
                                   Transaction transaction)
                        throws ObjectManagerException
        {
            if (checkRange(key,
                           hasStart,
                           hasEnd))
                return backingMap.containsKey(key,
                                              transaction);
            return false;
        }

        public Token get(Object key,
                         Transaction transaction)
                        throws ObjectManagerException
        {
            if (checkRange(key,
                           hasStart,
                           hasEnd))
                return backingMap.get(key,
                                      transaction);
            return null;
        }

        public Token put(Object key,
                         Token value,
                         Transaction transaction)
                        throws ObjectManagerException
        {
            if (checkRange(key,
                           hasStart,
                           hasEnd))
                throw new IllegalArgumentException("key not in SubMap range");
            return backingMap.put(key,
                                  value,
                                  transaction);
        }

        public Object firstKey(Transaction transaction)
                        throws ObjectManagerException
        {
            if (!hasStart)
                return TreeMap.this.firstKey(transaction);
            Entry node = TreeMap.this.findAfter(startKey,
                                                transaction);
            if (node != null && checkRange(node.getKey(),
                                           false,
                                           hasEnd))
                return node.getKey();
            throw new java.util.NoSuchElementException();
        }

        public Object lastKey(Transaction transaction)
                        throws ObjectManagerException
        {
            if (!hasEnd)
                return TreeMap.this.lastKey(transaction);
            AbstractTreeMap.Entry node = TreeMap.this.findBefore(endKey,
                                                                 transaction);
            if (node != null && checkRange(node.key,
                                           hasStart,
                                           false))
                return node.key;
            throw new java.util.NoSuchElementException();
        }

        public Iterator iterator()
                        throws ObjectManagerException
        {
            return values().iterator();
        } // iterator().
    } // class SubMap.

    private class TreeMapIterator
                    implements Iterator
    //extends AbstractTreeMap.TreeMapIterator
    {
        // TreeMapIterator is locked first if necessary, to protect the cursor. 
        // Any transaction is locked second, if necessary.
        // If the tree must be traversed or altered it is locked third.

        private Entry currentEntry = null;
        private Entry nextEntry = null;
        private Entry firstExcludedEntry = null;
        boolean beyondEndOfTree = false;
        AbstractMapEntry.Type type;

        TreeMapIterator(AbstractMapEntry.Type type)
            throws ObjectManagerException
        {
            // super(TreeMap.this,type);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "<init>",
                            new Object[] { type });
            this.type = type;
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>");
        } // TreeMapIterator().

        TreeMapIterator(AbstractMapEntry.Type type,
                        AbstractTreeMap.Entry startNode,
                        boolean checkEnd,
                        Object endKey)
            throws ObjectManagerException
        {
            // super(TreeMap.this,type,startNode,checkEnd,endKey);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "<init>",
                            new Object[] { type, startNode, new Boolean(checkEnd), endKey });
            if (startNode != null)
                currentEntry = (Entry) predecessor(startNode);
            this.type = type;
            if (endKey != null)
                this.firstExcludedEntry = (Entry) findAfter(endKey);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>");
        } // TreeMapIterator().

//    /**
//     * @param Entry which is the start of the iterator, 
//     *              null implies the start of the tree. 
//     * @param Entry which is after the end of the iteration, 
//     *              null implies the end of the tree.
//     */
//    TreeMapIterator(Entry first,
//                    Entry firstExcludedEntry)
//     {
//       currentEntry = first;
//       this.firstExcludedEntry = firstExcludedEntry;
//       type = new AbstractMapEntry.Type() {public Object get(AbstractMapEntry entry) {return entry;}}; 
//     } // TreeIterator().

        public synchronized boolean hasNext(Transaction transaction)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "hasNext",
                            new Object[] { transaction });

            boolean returnValue = false;
            synchronized (TreeMap.this) {
                if (!beyondEndOfTree) {
                    Entry nextAvailableEntry = nextAvailable(transaction);
                    if (nextAvailableEntry != null && nextAvailableEntry != firstExcludedEntry)
                        returnValue = true;
                } // if (!beyondEndOfTree).
            } // synchronized (TreeMap.this).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "hasNext",
                           new Object[] { new Boolean(returnValue) });
            return returnValue;
        } // hasNext().

        public synchronized boolean hasNext()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "hasNext");

            boolean returnValue = false;
            synchronized (TreeMap.this) {
                if (!beyondEndOfTree) {
                    Entry nextAvailableEntry = nextAvailable();
                    if (nextAvailableEntry != null && nextAvailableEntry != firstExcludedEntry)
                        returnValue = true;
                } // if (!beyondEndOfTree).
            } // synchronized (TreeMap.this).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "hasNext",
                           new Object[] { new Boolean(returnValue) });
            return returnValue;
        } // hasnext().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.Iterator#next(com.ibm.ws.objectManager.Transaction)
         */
        public synchronized Object next(Transaction transaction)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "next",
                            new Object[] { transaction });

            Object returnObject;
            synchronized (TreeMap.this) {
                if (!beyondEndOfTree) {
                    currentEntry = nextAvailable(transaction);
                    nextEntry = null;

                    if (currentEntry == null || currentEntry == firstExcludedEntry)
                        beyondEndOfTree = true;
                } // if (!beyondEndOfTree).

                if (beyondEndOfTree) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "next",
                                   "via java.util.NoSuchElementException");
                    throw new java.util.NoSuchElementException();
                } // if (beyondEndOfTree).

                returnObject = type.get(currentEntry);
            } // synchronized (TreMap.this).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "next",
                           new Object[] { returnObject });
            return returnObject;
        } // next().

        /**
         * Determine the next entry available to the transaction after the current cursor.
         * Caller must be synchronized on TreeMap.this and beyondEndOfTree must be false.
         * 
         * @param transaction controling visibility.
         * @return Entry found, or null if there is none.
         * 
         * @throws ObjectManagerException
         * @throws java.util.ConcurrentModificationException if the cursor Entry has been deleted
         *             other than by invoking remove() from this iterator.
         */
        private Entry nextAvailable(Transaction transaction)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "nextAvailable",
                            new Object[] { currentEntry, nextEntry, transaction }
                                );

            Entry nextAvailableEntry;
            if (nextEntry != null) {
                if ((nextEntry.state == Entry.stateAdded)
                    || ((nextEntry.state == Entry.stateToBeAdded) && nextEntry.lockedBy(transaction))) {
                    nextAvailableEntry = nextEntry;
                } else if (nextEntry.state == Entry.stateRemoved || nextEntry.state == Entry.stateDeleted) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "nextAvailable",
                                   "via java.util.ConcurrentModificationException");
                    throw new java.util.ConcurrentModificationException();

                } else {
                    nextAvailableEntry = successor(nextEntry,
                                                   transaction
                                    );
                } // if ( (nextEntry.state..

            } else if (currentEntry == null) {
                // At start of Tree?
                nextAvailableEntry = firstEntry(transaction);

            } else {
                if (currentEntry.state == Entry.stateRemoved
                    || currentEntry.state == Entry.stateDeleted) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "nextAvailable",
                                   "via java.util.ConcurrentModificationException");
                    throw new java.util.ConcurrentModificationException();

                } else {
                    nextAvailableEntry = successor(currentEntry,
                                                   transaction);
                } // if (currentEntry.state...

            } // (nextEntry...

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "nextAvailable"
                           , new Object[] { nextAvailableEntry }
                                );
            return nextAvailableEntry;
        } // nextAvailable().

        public synchronized Object next()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "next");

            Object returnObject;
            synchronized (TreeMap.this) {
                if (!beyondEndOfTree) {
                    currentEntry = nextAvailable();
                    nextEntry = null;

                    if (currentEntry == null || currentEntry == firstExcludedEntry)
                        beyondEndOfTree = true;
                } // if (!beyondEndOfTree).

                if (beyondEndOfTree) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "next",
                                   "via java.util.NoSuchElementException");
                    throw new java.util.NoSuchElementException();
                } // if (beyondEndOfTree).

                returnObject = type.get(currentEntry);
            } // synchronized (TreeMap.this).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "next",
                           new Object[] { returnObject });
            return returnObject;
        } // next().

        /**
         * Determine the next Entry available after the current cursor.
         * The caller must be synchronised on TreeMap.this and beyondEndOfTree must be false.
         * 
         * @return Entry found, or null if there is none.
         * 
         * @throws ObjectManagerException
         * @throws java.util.ConcurrentModificationException if the cursor Entry has been deleted
         *             other than by invoking remove() from this iterator.
         */
        private Entry nextAvailable()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "nextAvailable",
                            new Object[] { currentEntry,
                                          nextEntry });
            Entry nextAvailableEntry = null;
            if (nextEntry != null) {
                if (nextEntry.state == Entry.stateRemoved
                    || nextEntry.state == Entry.stateDeleted) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "nextAvailable",
                                   "via java.util.ConcurrentModificationException");
                    throw new java.util.ConcurrentModificationException();

                } else {
                    nextAvailableEntry = nextEntry;
                } // if ( (nextEntry.state...

            } else if (currentEntry == null) {
                if (root != null)
                    nextAvailableEntry = (Entry) minimum(getRoot());

            } else {
                if (currentEntry.state == Entry.stateRemoved
                    || currentEntry.state == Entry.stateDeleted) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "nextAvailable",
                                   "via java.util.ConcurrentModificationException");
                    throw new java.util.ConcurrentModificationException();

                } else {
                    nextAvailableEntry = (Entry) successor(currentEntry);
                } // if (currentEntry.state...
            } // if (nextEntry...

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "nextAvailable",
                           new Object[] { nextAvailableEntry });
            return nextAvailableEntry;
        } // nextAvailable().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.Iterator#remove(com.ibm.ws.objectManager.Transaction)
         */
        public synchronized Object remove(Transaction transaction)
                        throws ObjectManagerException {
            final String methodName = "remove";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction });

            // Before the start or already after the end of the tree?
            if (currentEntry == null || beyondEndOfTree) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, new Object[] { "via IllegalStateException" });
                throw new IllegalStateException();
            } // if (currentEntry == null)...

            synchronized (transaction.internalTransaction) {
                synchronized (TreeMap.this) {

                    currentEntry.remove(transaction);

                    // Find the next dirty entry, as we will delete the currentEntry
                    // if the transaction commits.
                    nextEntry = (Entry) successor(currentEntry);
                    if (nextEntry == null)
                        beyondEndOfTree = true;
                } // synchronized (TreeMap.this).
            } // synchronized (transaction.internalTransaction).

            Object returnObject = type.get(currentEntry);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { returnObject });
            return returnObject;
        } // remove().
    } // inner class TreeIterator.

    /**
     * Use the comparator to compare the two keys.
     * 
     * @param key1 first key to compare.
     * @param key2 second key to compare.
     * @return int -1 if key1 < key2 0 if key1 = key2 1 if key1 > key2
     */
    private int compare(Object key1, Object key2) {
        if (comparator == null)
            return ((Comparable) key1).compareTo(key2);
        else
            return comparator.compare(key1, key2);
    } // compare().

    private Entry findAfter(Object key, Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "findAfter",
                        new Object[] { key, transaction }
                            );

        // Find the first entry in the tree, commited or not.
        Entry entry = (Entry) findAfter(key);

        // Capture the ObjectManagers unlock sequence number. Anything unlocked after 
        // this will not count. The next lime delimits the point before which additions to 
        // list must committed and unlocked.
        long transactionUnlockPoint = Long.MAX_VALUE; // All transactions end before this.
        if (transaction != null)
            transactionUnlockPoint = transaction.getTransactionUnlockSequence();

        // Ascend the tree looking for one that is visible to the transaction.
        while (entry != null) {
            if (entry.state == Entry.stateAdded)
                if (!entry.wasLocked(transactionUnlockPoint))
                    break;// Found one.
            if ((entry.state == Entry.stateToBeAdded)
                && entry.lockedBy(transaction))
                break;
            entry = (Entry) successor(entry);
        } // while (entry != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "findAfter"
                       , new Object[] { entry }
                            );
        return entry;
    } // findAfter().

    private Entry findBefore(Object key,
                             Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "findBefore",
                        new Object[] { key,
                                      transaction });

        // Find the first entry in the tree, commited or not.
        Entry entry = (Entry) findBefore(key);

        // Capture the ObjectManagers unlock sequence number. Anything unlocked after 
        // this will not count. The next lime delimits the point before which additions to 
        // list must committed and unlocked.
        long transactionUnlockPoint = Long.MAX_VALUE; // All transactions end before this.
        if (transaction != null)
            transactionUnlockPoint = transaction.getTransactionUnlockSequence();

        // Ascend the tree looking for one that is visible to the transaction.
        while (entry != null) {
            if (entry.state == Entry.stateAdded)
                if (!entry.wasLocked(transactionUnlockPoint))
                    break;// Found one.
            if ((entry.state == Entry.stateToBeAdded)
                && entry.lockedBy(transaction))
                break;
            entry = (Entry) predecessor(entry);
        } // while (entry != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "firstBefore",
                       new Object[] { entry });
        return entry;
    } // findBefore().

    /**
     * Returns the first Entry in the TreeMap which is visible to the transaction
     * in the uncommited tree. Caller must be synchronized on this treeMap.
     * 
     * @param transaction controling visibility of Entries.
     * @return Entry which is first in the tree. Returns null if the TreeMap is empty.
     * @exception ObjectManagerException
     */
    private Entry firstEntry(Transaction transaction
                    )
                                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "firstEntry"
                        , new Object[] { transaction }
                            );

        // Find the first entry in the tree, commited or not.
        Entry entry = null;
        if (root != null)
            entry = (Entry) minimum(getRoot());

        // Capture the ObjectManagers unlock sequence number. Anything unlocked after 
        // this will not count. The next lime delimits the point before which additions to 
        // list must committed and unlocked.
        long transactionUnlockPoint = Long.MAX_VALUE; // All transactions end before this.
        if (transaction != null)
            transactionUnlockPoint = transaction.getTransactionUnlockSequence();

        // Ascend the tree looking for one that is visible to the transaction.
        while (entry != null) {
            if (entry.state == Entry.stateAdded)
                if (!entry.wasLocked(transactionUnlockPoint))
                    break;// Found one.
            if ((entry.state == Entry.stateToBeAdded)
                && entry.lockedBy(transaction))
                break;
            entry = (Entry) successor(entry);
        } // while (entry != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "firstEntry"
                       , "returns entry=" + entry + "(Entry)"
                            );
        return entry;
    } // firstEntry().

    /**
     * Returns the last Entry in the TreeMap which is visble to the Transaction
     * in the uncommited tree. Caller must be synchronized on this treeMap.
     * 
     * @param transaction the transaction controling visibility of Entries.
     * @return the first entry in the tree. Returns null if the TreeMap is empty.
     * @exception ObjectManagerException
     */
    private Entry lastEntry(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "lastEntry",
                        new Object[] { transaction });

        // Find the first entry in the tree, commited or not.
        Entry entry = null;
        if (root != null)
            entry = (Entry) maximum(getRoot());

        // Capture the ObjectManagers unlock sequence number. Anything unlocked after 
        // this will not count. The next lime delimits the point before which additions to 
        // list must committed and unlocked.
        long transactionUnlockPoint = Long.MAX_VALUE; // All transactions end before this.
        if (transaction != null)
            transactionUnlockPoint = transaction.getTransactionUnlockSequence();

        // Ascend the tree looking for one that is visible to the transaction.
        while (entry != null) {
            if (entry.state == Entry.stateAdded)
                if (!entry.wasLocked(transactionUnlockPoint))
                    break;// Found one.
            if ((entry.state == Entry.stateToBeAdded)
                && entry.lockedBy(transaction))
                break;
            entry = (Entry) predecessor(entry);
        } // while (entry != null).
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "lastEntry",
                       new Object[] { entry });
        return entry;
    } // lastEntry().

    /**
     * Returns the successor of the specified Entry in the uncommited tree
     * visible to the transaction or null if no such entry exists.
     * Caller must be synchronized on this treeMap.
     * 
     * @param entry the Entry who's successor to be returned.
     * @param transaction the transaction controling visibility of Entrys.
     * @return Entry which is the successor.
     * @exception ObjectManagerException
     */
    private Entry successor(Entry entry
                            , Transaction transaction
                    )
                                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "successor",
                        new Object[] { entry,
                                      transaction });

        // Capture the ObjectManagers unlock sequence number. Anything unlocked after 
        // this will not count. The next lime delimits the pont before which additions to 
        // list must committed and unlocked.
        long transactionUnlockPoint = Long.MAX_VALUE; // All transactions end before this.
        if (transaction != null)
            transactionUnlockPoint = transaction.getTransactionUnlockSequence();

        // Ascend the tree looking for the next greater entry.
        while (entry != null) {
            entry = (Entry) successor(entry);
            if (entry == null)
                break; // No successor entry.
            if ((entry.state == Entry.stateAdded)
                && !entry.wasLocked(transactionUnlockPoint))
                break; // Found one.
            // Transaction may be null, indicaiting sateToBeAdded links are not eligible.
            if ((entry.state == Entry.stateToBeAdded)
                && transaction != null
                && entry.lockedBy(transaction))
                break; // Found one added by the same transaction.
        } // while (entry != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "successor"
                       , new Object[] { entry });
        return entry;
    } //succesor().

    /**
     * Returns the predecessor of the specified Entry in the uncommited tree
     * visible to the transaction or null if no such entry exists. Caller must
     * be synchronized on this treeMap.
     * 
     * @param entry the Entry who's successor to be returned.
     * @param transaction the transaction controling visibility of Entrys.
     * @exception ObjectManagerException
     */
    private Entry predecessor(Entry entry,
                              Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "predecessor",
                        new Object[] { entry, transaction });

        // Capture the ObjectManagers unlock sequence number. Anything unlocked after 
        // this will not count. The next lime delimits the pont before which additions to 
        // list must committed and unlocked.
        long transactionUnlockPoint = Long.MAX_VALUE; // All transactions end before this.
        if (transaction != null)
            transactionUnlockPoint = transaction.getTransactionUnlockSequence();

        // Ascend the tree looking for the next greater entry.
        while (entry != null) {
            entry = (Entry) predecessor(entry);
            if (entry == null)
                break; // No predecessor entry.
            if ((entry.state == Entry.stateAdded)
                && !entry.wasLocked(transactionUnlockPoint))
                break; // Found one.
            // Transaction may be null, indicaiting sateToBeAdded links are not eligible.
            if ((entry.state == Entry.stateToBeAdded)
                && transaction != null
                && entry.lockedBy(transaction))
                break; // Fould one added by the same transaction.
        } // while (entry != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "predecessor"
                       , "returns entry=" + entry + "(Entry)"
                            );
        return entry;
    } // predecessor().  

    /**
     * Finalise removal of an Entry from the tree.
     * 
     * We have already logged the deletion of the Entry so we now need to rechain those Entries
     * currently refering to it and rebalance the tree. This may be redriven at recovery so we
     * need to detect if the entry has already been removed from the tree so that we don't
     * disrupt other entries in error.
     * 
     * @param entry to be removed from the structure of the tree.
     * @param transaction controling the update.
     * @exception ObjectManagerException
     */
    private synchronized void deleteEntry(Entry entry,
                                          Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "deleteEntry";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { entry,
                                                                transaction });

        // managedObjectsToAdd.clear(); // Not used.
        managedObjectsToReplace.clear(); // Reset from last time.
        tokensToNotify.clear(); // Reset from last time.
        reservedSpaceInStore = (int) storeSpaceForRemove();

        rbDelete(entry);

        // We have changed the size at least.
        managedObjectsToReplace.add(this);
        // Harden the updates. Release some of the space we reserved earlier,
        // when we deleted the entry, or, if we are backing out, when we added it.
        // Releasing the reserved space ensures that the replace will succeed.
        tokensToNotify.add(entry.getToken());
        transaction.optimisticReplace(null,
                                      new java.util.ArrayList(managedObjectsToReplace),
                                      null, // No tokens to delete.
                                      new java.util.ArrayList(tokensToNotify),
                                      -logSpaceForDelete);
        // Release any surplus space we reserved in the store.
        // During recovery we don't do this because we will not have reserved the space anyway.
        if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog)
            owningToken.objectStore.reserve(-(int) reservedSpaceInStore, false);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // deleteEntry().

    void rbDelete(Entry z)
                    throws ObjectManagerException {
        AbstractTreeMap.Entry y = z.getLeft() == null || z.getRight() == null ? z : successor(z);
        AbstractTreeMap.Entry x = y.getLeft() != null ? y.getLeft() : y.getRight();
        // Splice out y. 
        boolean yWasLeftChild = false;
        boolean yWasRightChild = false;
        if (x != null)
            x.setParent(y.getParent());
        if (y.getParent() == null)
            setRoot(x);
        else if (y == y.getParent().getLeft()) {
            y.getParent().setLeft(x);
            yWasLeftChild = true;
        } else {
            y.getParent().setRight(x);
            yWasRightChild = true;
        }
        modCount++;

        boolean yOldColor = y.getColor();
        Entry yOldParent = (Entry) y.getParent();
        // Put y where z is.
        if (y != z) {
            move(y, z);
        }

        if (!yOldColor && root != null) {
            if (x == null) {
                // Z has been replaced with y we can make use of it as a temporary placeholder
                // for the deleted node and fixup based on that.
                if (yOldParent == z)
                    z.setParent(y);
                else
                    z.setParent(yOldParent);
                z.setColor(false); /* BLACK */
                if (yWasLeftChild)
                    yOldParent.setLeft(z);
                if (yWasRightChild)
                    yOldParent.setRight(z);
                fixup(z);
                if (yWasLeftChild)
                    yOldParent.setLeft(null);
                if (yWasRightChild)
                    yOldParent.setRight(null);
            } else
                fixup(x);
        }
        size--;
    } // rbDelete().

    /**
     * Move x to occupy the position currently held by y.
     * 
     * @param x to be moved.
     * @param y location to be moved to.
     * @throws ObjectManagerException
     */
    void move(AbstractTreeMap.Entry x, AbstractTreeMap.Entry y)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "move",
                       new Object[] { x, y });

        Entry yParent = (Entry) y.getParent();

        // Set x into the position occupied by y.
        x.setParent(yParent);

        if (yParent == null)
            setRoot(x);
        else if (yParent.getRight() == y)
            yParent.setRight(x);
        else
            yParent.setLeft(x);
        x.setLeft(y.getLeft());
        x.setRight(y.getRight());

        // Set the x to be the parent of y's children.
        if (y.getLeft() != null)
            y.getLeft().setParent(x);
        if (y.getRight() != null)
            y.getRight().setParent(x);

        x.setColor(y.getColor());
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "move",
                       new Object[] { x });
    } // move().

    /**
     * Make a shallow copy of this Map, the copy contains all entries visible to the transaction.
     * 
     * @param transaction under which the new cloned tree is created.
     * @param objectStore where the cloned tree is stored.
     * @return TreeMap a shallow copy of this Map.
     * @throws ObjectManagerException
     */
    public synchronized java.lang.Object clone(Transaction transaction,
                                               ObjectStore objectStore)
                    throws ObjectManagerException

    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "clone"
                        , new Object[] { transaction, objectStore }
                            );

        TreeMap clonedMap = new TreeMap(comparator
                                        , transaction
                                        , objectStore
                        );

        // Initialize clone with our mappings
        // Move through the map recreating each entry as we go.
        Entry entry = firstEntry(transaction
                        );
        while (entry != null) {
            clonedMap.put(entry.getKey()
                          , entry.getValue()
                          , transaction
                            );
            entry = successor(entry
                              , transaction
                            );
        } // while (entry != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "clone"
                       , new Object[] { clonedMap }
                            );
        return clonedMap;
    } // clone().

    /**
     * Print a dump of the Map.
     * 
     * @param printWriter to be written to.
     */
    public synchronized void print(java.io.PrintWriter printWriter)
    {
        printWriter.println("Dump of TreeMap size=" + size + "(long)");
        try {
            for (Iterator iterator = entrySet().iterator(); iterator.hasNext();) {
                Entry entry = (Entry) iterator.next();
                printWriter.println((indexLabel(entry) + "                    ").substring(0, 20)
                                    + (entry.getColor() ? " RED  " : " BLACK")
                                    + " Entry=" + entry);
            } // for(iterator... 
        } catch (ObjectManagerException objectManagerException) {
            // No FFDC code needed.
            printWriter.println("Caught objectManagerException=" + objectManagerException);
            objectManagerException.printStackTrace(printWriter);
        } // try...   
    } // print().

    /**
     * Validates that the Tree is:
     * 1) In the correct sort order.
     * 2) RED nodes have Black/null children.
     * 3) Has all of the leaf nodes less than the maximum height from the root.
     * 4) That all leaf nodes contain the same number of BLACK nodes to the root.
     * 
     * @param printStream to print invalid reports to.
     * 
     * @return boolean true if the tree is not corrupt.
     * @throws ObjectManagerException
     */
    public synchronized boolean validate(java.io.PrintStream printStream)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "validate",
                        new Object[] { printStream });

        boolean valid = true; // Until proven otherwise.

        int logSizePlus1 = 0;
        for (long sizePlus1 = size + 1; sizePlus1 != 0; sizePlus1 = sizePlus1 >>> 1) {
            logSizePlus1++;
        } // for...
        int maximumLeafDepth = (int) (2 * logSizePlus1);
        int firstBlackDepth = 0;
        long numberFound = 0;

        Iterator iterator = entrySet().iterator();
        Entry previous = null;
        if (iterator.hasNext()) {
            numberFound++;
            previous = (Entry) iterator.next();
        } // if (iterator.hasNext()). 
        while (iterator.hasNext()) {
            numberFound++;
            Entry entry = (Entry) iterator.next();

            // Check keys are returned in the sort order.
            if (compare(entry.getKey(), previous.getKey()) < 0) {
                valid = false;
                printStream.println("key=" + previous.getKey() + " < following key=" + entry.getKey());
            } // if (compare... 
            previous = entry;

            // Check that a RED node has two BLACK/Null children.
            if (entry.getColor()) {
                if ((entry.getLeft() != null && entry.getLeft().getColor())
                    || (entry.getRight() != null && entry.getRight().getColor())) {
                    printStream.println((indexLabel(entry) + "                    ").substring(0, 20)
                                        + " Key=" + entry.getKey() + " Value=" + entry.getValue()
                                        + " Red node without two BLACK/null chideren left=" + entry.getLeft() + " right=" + entry.getRight());
                }
            } // if (entry.getColor())

            // Check tree is balanced, all leaf nodes must have the same number 
            // of BLACK nodes between themselves and the root. No leaf may be more
            // than 2*Log2(size+1) nodes from the root. 
            if (entry.getLeft() == null || entry.getRight() == null) {
                int blackDepth = 0;
                int depth = 0;
                for (Entry parent = entry; parent != getRoot(); parent = (Entry) parent.getParent()) {
                    depth++;
                    if (!parent.getColor())
                        blackDepth++;
                } // for... 
                if (firstBlackDepth == 0)
                    firstBlackDepth = blackDepth;

                if (depth > maximumLeafDepth) {
                    valid = false;
                    printStream.println((indexLabel(entry) + "                    ").substring(0, 20)
                                        + " Key=" + entry.getKey() + " Value=" + entry.getValue()
                                        + " Leaf depth=" + depth + ">" + maximumLeafDepth);
                } // if if (depth > maximumLeafDepth ).

                if (blackDepth != firstBlackDepth) {
                    valid = false;
                    printStream.println((indexLabel(entry) + "                    ").substring(0, 20)
                                        + " Key=" + entry.getKey() + " Value=" + entry.getValue()
                                        + " blackDepth=" + blackDepth + " != " + firstBlackDepth);
                } // if if (depth > maximumLeafDepth ).
            }
        } // while... 

        if (numberFound != size())
            printStream.println(" Number of Entries found=" + numberFound + " != size()" + size());

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "validate",
                       new Object[] { new Boolean(valid) });
        return valid;
    } // method validate().

    private String indexLabel(Entry child)
                    throws ObjectManagerException
    {
        // Step up the tree until we reach the root.
        String indexLabel = "";
        for (Entry parent = (Entry) child.getParent(); parent != null; parent = (Entry) child.getParent()) {
            if (parent.getLeft() == child)
                indexLabel = "L," + indexLabel;
            else if (parent.getRight() == child)
                indexLabel = "R," + indexLabel;
            else
                indexLabel = "Invalid," + indexLabel;
            child = parent;
        } // for...
        return indexLabel;
    } // indexLabel().

    // --------------------------------------------------------------------------
    // extends ManagedObject.
    // --------------------------------------------------------------------------

    /**
     * Modifiy the behaviour of the ManagedObject ObjectStore space reservation by taking
     * storage from the previously allocated reservedSpaceInStore rather than the store itself
     * this avoids the possibility of seeing an ObjectStoreFull exception here.
     * 
     * @see com.ibm.ws.objectManager.ManagedObject#reserveSpaceInStore(com.ibm.ws.objectManager.ObjectManagerByteArrayOutputStream)
     */
    void reserveSpaceInStore(ObjectManagerByteArrayOutputStream byteArrayOutputStream)
                    throws ObjectManagerException {
        // During recovery we use the defeault mechanism because we have not pre-reserved the space, 
        // store full exceptions are supressed during recovery.
        if (owningToken.getObjectStore().getObjectManagerState().getObjectManagerStateState() == ObjectManagerState.stateReplayingLog)
            super.reserveSpaceInStore(byteArrayOutputStream);
        else {
            // Adjust the space reserved in the ObjectStore to reflect what we just serialized
            // and will eventually give to the ObjectStore.
            // We reserve the largest size even the ManagedObject may have become smaller because
            // there may be a larger version of this Object still about to commit.
            int currentSerializedSize = byteArrayOutputStream.getCount() + owningToken.objectStore.getAddSpaceOverhead();
            if (currentSerializedSize > latestSerializedSize) {
                latestSerializedSizeDelta = currentSerializedSize - latestSerializedSize;
                reservedSpaceInStore = reservedSpaceInStore - latestSerializedSizeDelta;
                latestSerializedSize = currentSerializedSize;
            } // if (currentSerializedSize > latestSerializedSize).
        }
    } // reserveSpaceInStore().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ManagedObject#becomeCloneOf(com.ibm.ws.objectManager.ManagedObject)
     */
    public void becomeCloneOf(ManagedObject other)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "becomeCloneOf"
                        , new Object[] { other }
                            );

        TreeMap otherTree = (TreeMap) other;
        root = otherTree.root;
        size = otherTree.size;
        // Transient state is dealt with by preCommit() and preBackout() methods of Entry.
        if (!backingOut) { // Was transient state corrected in preBackout?
            availableSize = otherTree.availableSize;
        } // if(!backingOut).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "becomeCloneOf"
                            );
    } // becomeCloneOf().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ManagedObject#preDelete(com.ibm.ws.objectManager.Transaction)
     */
    public synchronized void preDelete(Transaction transaction)
                    throws ObjectManagerException
    {
        final String methodName = "preDelete";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        methodName,
                        new Object[] { transaction });

        // The tree must be empty, apart from Entries this transaction will delete.
        Entry entry = null;
        if (root != null)
            entry = (Entry) AbstractTreeMap.minimum(getRoot());
        while (entry != null) {
            if (entry.willBeDeleted(transaction)) {
                entry = (Entry) successor(entry);
            } else {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               methodName,
                               "via CollentionNotEmptyException"
                                               + "size=" + size + "(long)"
                                               + " entry.state=" + entry.state + "(int) " + Entry.stateNames[entry.state]
                                               + "\n entry.getTransactionLock=" + entry.getTransactionLock() + "(TransactionLock)"
                                    );

                throw new CollectionNotEmptyException(this, size, transaction);

            } // if (  entry.willBeDeleted...
        } // while (entry != null).

        super.preDelete(transaction); // Also takes a lock.
        // Visibility is now restricted to our transaction only.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // preDelete().

    // --------------------------------------------------------------------------
    // Simplified serialization.
    // --------------------------------------------------------------------------

    /**
     * No argument constructor.
     * Used by SimplifiedSerialization and ObjectManager state to make the NamedObjects Tree.
     * 
     * @exception ObjectManagerException
     */
    TreeMap()
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "<init>"
                            );

        managedObjectsToAdd = new java.util.HashSet(1);
        managedObjectsToReplace = new java.util.HashSet();
        tokensToNotify = new java.util.HashSet(1);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "<init>"
                            );
    } // TReeMap().

    private static final byte SimpleSerialVersion = 0;

    // The serialized size of this.
    public static final long maximumSerializedSize()
    {
        return 1 // Version.
               + ManagedObject.maximumSerializedSize()
               + 2 // Flags.  
               + 2 * Entry.maximumSerializedSize()
               + 8 // Size.
        ;
    }

    /*
     * (non-Javadoc)
     * 
     * @see int SimplifiedSerialization.getSignature()
     */
    public int getSignature()
    {
        return signature_TreeMap;
    } // End of getSignature.

    /*
     * (non-Javadoc)
     * 
     * @see SimplifiedSerialization.writeObject(java.io.DataInputStream)
     */
    public final void writeObject(java.io.DataOutputStream dataOutputStream)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "writeObject",
                        new Object[] { dataOutputStream });

        try {
            dataOutputStream.writeByte(SimpleSerialVersion);
            super.writeObject(dataOutputStream);

            if (comparator == null) {
                dataOutputStream.writeByte(0);
            } else {
                dataOutputStream.writeByte(1);
                java.io.ObjectOutputStream objectOutputStream = new java.io.ObjectOutputStream(dataOutputStream);
                objectOutputStream.writeObject(comparator);
                objectOutputStream.close();
            } // if (head == null).
            if (root == null) {
                dataOutputStream.writeByte(0);
            } else {
                dataOutputStream.writeByte(1);
                root.writeObject(dataOutputStream);
            } // if (root == null).
            dataOutputStream.writeLong(size);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "writeObject", exception, "1:2341:1.44");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "writeObject",
                           exception);

            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        // Did we exceed the size limits?
        // Check comparator did not cause us to exceed maximum serialized length.
        if (dataOutputStream.size() > maximumSerializedSize())
        {
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            "writeObject",
                            new Object[] { new Long(maximumSerializedSize()),
                                          new Integer(dataOutputStream.size()) });
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "writeObject"
                           , "via SimplifiedSerializationSizeException"
                                );

            throw new SimplifiedSerializationSizeException(this,
                                                           maximumSerializedSize(),
                                                           dataOutputStream.size());
        } // if (dataOutputStream.size() > maximumSerializedSize())    

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "writeObject");
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
            trace.entry(this, cclass, methodName, new Object[] { dataInputStream,
                                                                objectManagerState });

        try {
            byte version = dataInputStream.readByte();
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this, cclass, methodName, new Object[] { new Byte(version) });
            super.readObject(dataInputStream, objectManagerState);

            if (dataInputStream.readByte() == 1) {
                // Use ManagedObjectInpuStream so as to get the correct classloader.
                java.io.ObjectInputStream objectInputStream = new ManagedObjectInputStream(dataInputStream, objectManagerState);
                comparator = (java.util.Comparator) objectInputStream.readObject();
                objectInputStream.close();
            } // if (dataInputStream.readByte() == 1).
            if (dataInputStream.readByte() == 1) {
                root = Token.restore(dataInputStream
                                     , objectManagerState
                                );
            } // if (dataInputStream.readByte() == 1).
            size = dataInputStream.readLong();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:2412:1.44");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, exception);
            throw new PermanentIOException(this, exception);

        } catch (java.lang.ClassNotFoundException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:2420:1.44");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { "ClassNotFoundException:2423",
                                                                   exception });
            throw new com.ibm.ws.objectManager.ClassNotFoundException(this, exception);
        } // try.

        availableSize = size; // Start as last commited.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // readObject().

    // --------------------------------------------------------------------------
    // implements java.io.Serializable
    // --------------------------------------------------------------------------
    /**
     * Customized deserialization.
     * 
     * @param objectInputStreamIn containing the TreeMap.
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream objectInputStreamIn)
                    throws java.io.IOException
                    , java.lang.ClassNotFoundException
    {

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "readObject"
                        , "objectInputStreamIn=" + objectInputStreamIn + "(java.io.ObjectInputStream)"
                            );

        objectInputStreamIn.defaultReadObject();
        availableSize = size; // Start as last commited.
        managedObjectsToAdd = new java.util.HashSet(1);
        managedObjectsToReplace = new java.util.HashSet();
        tokensToNotify = new java.util.HashSet(1); // Holds the set of tokens to notify about replacements. 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "readObject"
                            );

    } // readObject.

    // ----------------------------------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------------------------------

    /**
     * Node in the Tree. Doubles as a means to pass key-value pairs back to
     * user (see java.util.Map.Entry).
     */
    static class Entry
                    extends AbstractTreeMap.Entry
                    implements Map.Entry, SimplifiedSerialization
    {
        private static final Class cclass = TreeMap.Entry.class;
        private static final long serialVersionUID = 8326113960040570428L;

        /*---------------------- Define the state machine for Entry (begin) ------------*/
        // Tracks the lifecycle of the Entry.
        static final int stateError = 0; // A state error has occured.
        static final int stateConstructed = 1; // Not yet part of a tree.
        static final int stateToBeAdded = 2; // Part of the tree but not yet visible to other transactions.
        static final int stateAdded = 3; // Added.
        static final int stateNotAdded = 4; // Added, but removed because of backout.
        static final int stateToBeDeleted = 5; // Will be removed from the tree.
        static final int stateMustBeDeleted = 6; // Link will be deleted regardless of the transaction outcome.
        static final int stateRemoved = 7; // Removed from the tree.
        static final int stateDeleted = 8; // Removed from the tree and deleted.

        // The names of the states for diagnostic purposes.
        static final String stateNames[] = { "Error"
                                            , "Constructed"
                                            , "ToBeAdded"
                                            , "Added"
                                            , "NotAdded"
                                            , "ToBeDeleted"
                                            , "MustBeDeleted"
                                            , "Removed"
                                            , "Deleted"
        };
        // What happens when this Link is marked for addition to the list.
        static final int nextStateForRequestAdd[] = { stateError,
                                                     stateToBeAdded,
                                                     stateToBeAdded, // If added a second time from a checkpoint.
                                                     stateToBeAdded, // During recovery we start in Added state.
                                                     stateError,
                                                     stateError,
                                                     stateError,
                                                     stateError,
                                                     stateError };

        // What happens when this Entry is marked for deletion from the tree.
        static final int nextStateForRequestDelete[] = { stateError,
                                                        stateError,
                                                        stateMustBeDeleted, // Add then Delete in the same transaction.
                                                        stateToBeDeleted,
                                                        stateError,
                                                        stateToBeDeleted, // If deleted a second time from a checkpoint.
                                                        stateMustBeDeleted, // If deleted a second time from a checkpoint.
                                                        stateError,
                                                        stateError };

        // Note: no nextStateForRequestUnDelete because unlike LinkedList, the treeMap logs delete before it changes
        // the state to toBeDeleted, this way it does not have to deal with failures to write the log but does
        // have to hold a lock on the tree while it writes the log record.

        // What happens when this Link is removed from the tree structure.
        static final int nextStateForRemove[] = { stateError
                                                 , stateError
                                                 , stateNotAdded
                                                 , stateRemoved // OptimisticReplace recovered, before TransactionCheckpoint.
                                                 , stateError
                                                 , stateRemoved
                                                 , stateRemoved
                                                 , stateError
                                                 , stateError
        };

        // What happens when this Entry is commited in the tree.
        static final int nextStateForCommit[] = { stateError
                                                 , stateError
                                                 , stateAdded
                                                 , stateError
                                                 , stateError
                                                 , stateError
                                                 , stateError
                                                 , stateDeleted
                                                 , stateError
        };

        // What happens when this Entry is rolled back from the tree.
        static final int nextStateForBackout[] = { stateError
                                                  , stateError
                                                  , stateError
                                                  , stateError
                                                  , stateDeleted
                                                  , stateAdded
                                                  , stateError
                                                  , stateDeleted
                                                  , stateError
        };

        // What happens when we cannot progress in the current state.
        static final int nextStateForError[] = { stateError,
                                                stateError,
                                                stateError,
                                                stateError,
                                                stateError,
                                                stateError,
                                                stateError,
                                                stateError,
                                                stateError };

        transient int state; // The current state of the Link.
        // The previous state, not refereneced but will appear in a dump.
        private transient int previousState;
        /*---------------------- Define the state machine for Entry (end) --------------*/

        private Token treeToken; // The anchor for the tree root.
        private transient TreeMap treeMap;

        // The new dalue when an update to the Entry commits.
        transient Token uncommitedValue;

        /**
         * Make a new cell with given key, value, and parent, and with <tt>null</tt>
         * child links, and BLACK color.
         * 
         * @param treeMap containing this new Entry.
         * @param key for the new Entry.
         * @param value contained by the Entry.
         * @param parent of this Entry in the tree.
         * @param transaction which controls the addition of the new Entry.
         * @exception ObjectManagerException
         */
        protected Entry(TreeMap treeMap
                        , Object key
                        , Token value
                        , Entry parent
                        , Transaction transaction)
            throws ObjectManagerException
        {
            super(key, value);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "<init>"
                            , new Object[] { treeMap
                                            , key
                                            , value
                                            , parent
                                            , transaction }
                                );

            this.treeToken = treeMap.owningToken;
            this.treeMap = treeMap;

            if (parent != null)
                this.parent = parent.getToken();
            this.state = stateConstructed;
            previousState = -1; // No previous state.

            // Make the Entry an ObjectManager object in the same object store as the TreeMap.
            treeToken.getObjectStore().allocate(this);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "<init>"
                                );
        } // Entry().

        /**
         * Sets the left, less than branch in the commited tree.
         * 
         * @param newLeft the new left branch.
         * @exception ObjectManagerException
         */
        void setLeft(AbstractTreeMap.Entry newLeft)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "setLeft",
                            new Object[] { newLeft,
                                          this.left });
            super.setLeft(newLeft);
            treeMap.managedObjectsToReplace.add(this);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "setLeft"
                                );
        } // setLeft().

        /**
         * Sets the right, greater than branch, ready for commit.
         * 
         * @param newRight the new right branch when the transaction commits.
         * @exception ObjectManagerException
         */
        void setRight(AbstractTreeMap.Entry newRight)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "setRight",
                            new Object[] { newRight,
                                          this.right });
            super.setRight(newRight);
            treeMap.managedObjectsToReplace.add(this);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "setRight");
        } // setRight().

        /**
         * Sets the parent of the Entry.
         * 
         * @param newParent the new parent when the transaction commits.
         * @exception ObjectManagerException
         */
        void setParent(AbstractTreeMap.Entry newParent)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "setParent",
                            new Object[] { newParent,
                                          this.parent });
            super.setParent(newParent);
            // TODO Should be managedObjectsToAdd if we have just been constructed.
            // TODO Should also guard these with... if(state != stateToBeDeleted && state != stateMustBeDeleted)
            treeMap.managedObjectsToReplace.add(this);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "setParent");
        } // setParent().

        /**
         * Sets the color of the Entry, Red or Black.
         * 
         * @param newColor the new color of the uncommited tree.
         * @exception ObjectManagerException
         */
        void setColor(boolean newColor)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "setColor"
                            , new Object[] { new Boolean(newColor), (newColor ? "RED" : "BLACK")
                                            , new Boolean(color), (color ? "RED" : "BLACK") }
                                );

            super.setColor(newColor);
            treeMap.managedObjectsToReplace.add(this);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "setColor"
                                );
        } // setColor().

        /**
         * Mark this Entry for later deletion from the Tree.
         * Although we delete the entry from the map now, no actual deletion takes place
         * until we commit. If we back out, the entry is left the way it was. We have not removed
         * the entry from the list yet either in memory or inthe object store It will be removed
         * from the tree when we know the outcome of the transaction is commit.
         * 
         * Caller must be synchronized on treeMap.
         * 
         * @param transaction the unit of work which will complete the deletion.
         * @throws ObjectManagerException
         */
        protected void requestDelete(Transaction transaction)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "requestDelete"
                            , "transaction=" + transaction + "(Transaction)"
                                );
            // TODO Does not need transaction as a parameter.
            setState(nextStateForRequestDelete); // Make the state change.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "requestDelete"
                           , "state =" + state + "(int) " + stateNames[state] + "(String)"
                                );
        } // requestDelete().

        /**
         * Indicates if the transacton will complete deleteion of this Entry.
         * Caller must be synchronized on treeMap.
         * 
         * @param transaction the unit of work which will complete the deletion.
         * @return true if the Entry will be deleted by the transaction.
         */
        private final boolean willBeDeleted(Transaction transaction)
        {
            return ((state == stateToBeDeleted || state == stateMustBeDeleted)
            && lockedBy(transaction));
        } // willBeDeleted().

        // --------------------------------------------------------------------------
        // extends ManagedObject.
        // --------------------------------------------------------------------------

        /**
         * Modifiy the behaviour of the ManagedObject ObjectStore space reservation by taking
         * storage from the previously allocated reservedSpaceInStore rather than the store itself
         * this avoids the possibility of seeing an ObjectStoreFull exception here.
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#reserveSpaceInStore(com.ibm.ws.objectManager.ObjectManagerByteArrayOutputStream)
         */
        void reserveSpaceInStore(ObjectManagerByteArrayOutputStream byteArrayOutputStream)
                        throws ObjectManagerException {
            // During recovery we use the defeault mechanism because we have not pre-reserved the space, 
            // store full exceptions are supressed during recovery.
            if (owningToken.getObjectStore().getObjectManagerState().getObjectManagerStateState() == ObjectManagerState.stateReplayingLog)
                super.reserveSpaceInStore(byteArrayOutputStream);
            else {
                // Adjust the space reserved in the ObjectStore to reflect what we just serialized
                // and will eventually give to the ObjectStore.
                // We reserve the largset size even the ManagedObject may have become smaller because
                // there may be a larger version of this Object still about to commit.
                int currentSerializedSize = byteArrayOutputStream.getCount() + owningToken.objectStore.getAddSpaceOverhead();
                if (currentSerializedSize > latestSerializedSize) {
                    latestSerializedSizeDelta = currentSerializedSize - latestSerializedSize;
                    treeMap.reservedSpaceInStore = treeMap.reservedSpaceInStore - latestSerializedSizeDelta;
                    latestSerializedSize = currentSerializedSize;
                } // if (currentSerializedSize > latestSerializedSize).
            }
        } // reserveSpaceInStore().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#becomeCloneOf(com.ibm.ws.objectManager.ManagedObject)
         */
        public void becomeCloneOf(ManagedObject other)
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "becomeCloneOf"
                            , new Object[] { other }
                                );
            // Entries in a tree use an optimistic update methodology and hence perform backout by making
            // updates so we dont make the clone of a before image when backing out.
            if (!backingOut) { // Fixed up in preBackOut?
                Entry otherEntry = (Entry) other;
                treeToken = otherEntry.treeToken;
                treeMap = otherEntry.treeMap;
                key = otherEntry.key;
                value = otherEntry.value;
                left = otherEntry.left;
                right = otherEntry.right;
                parent = otherEntry.parent;
                color = otherEntry.color;
                // state is maintained by add() and delete() etc. methods. 
            } // if(!backingOut).
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "becomeCloneOf"
                                );
        } // becomeCloneOf().

        /*
         * Modify the behaviour of the ManagedObject add method to perform
         * extra work which we will also do at recovery time.
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#postAdd(com.ibm.ws.objectManager.Transaction, boolean)
         */
        public void postAdd(Transaction transaction
                            , boolean logged
                        )
                                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "postAdd"
                            , "transaction=" + transaction + "(Transaction)"
                              + " logged=" + logged + "(boolean)"
                                );

            super.postAdd(transaction, logged);
            // The link now appears as only part of the transaction adding it.

            if (logged) {

                // So that the state is set correctly both at runtime and on recovery we do it here.
                // No need to synchronize to protect state because only the locking transaction can change it.
                setState(nextStateForRequestAdd); // Make the state change.

                // We may need to undo the effects of the add so we need to be told before the transaction
                // backs out. We do this in the ObjectManager add method so that this is redriven during
                // recovery.
                transaction.requestCallback(owningToken);

            } // if(logged).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "postAdd"
                           , "treemap.Size=" + treeMap.size + "(long)"
                             + " treeMap.availableSize=" + treeMap.availableSize + "(long)"
                                );
        } // End of postAdd method.

        /*
         * Modify the behaviour of the ManagedObject delete method to perform
         * extra work which we will also do at recovery time. Actual removal
         * from the list done at pre commit time, but we need to make sure we
         * are called when this happens, even if it is during recovery.
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#postDelete(com.ibm.ws.objectManager.Transaction, boolean)
         */
        public void postDelete(Transaction transaction,
                               boolean logged
                        )
                                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "postDelete"
                            , new Object[] { transaction, new Boolean(logged) }
                                );

            super.postDelete(transaction,
                             logged); // Also takes a lock. 
            // Visibility is now restricted to our transaction only.

            if (logged) {
                // If we are recovering the request to delete will not have been seen so do it here.
                if (transaction.getObjectManagerStateState() == ObjectManagerState.stateReplayingLog)
                    synchronized (treeMap) {
                        // In principle no need to synchronize because recovery is single threaded.
                        setState(nextStateForRequestDelete); // Make the state change.
                    } // synchronized (list). 

                // Complete the deletion at prePrepare time.
                transaction.requestCallback(this.owningToken);
            } // if(logged).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "postDelete"
                           , "treemap.Size=" + treeMap.size + "(long)"
                             + " treeMap.availableSize=" + treeMap.availableSize + "(long)"
                                );
        } // End of postDelete method.

        /**
         * Driven after a ManagedObject is optimistically replaced and the log record to
         * record this has been written but not forced to disk.
         * 
         * @param transaction which has logged the optimistic replace.
         * @throws ObjectManagerException
         */
        public void optimisticReplaceLogged(Transaction transaction)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "optimisticReplaceLogged"
                            , "transaction=" + transaction + "(Transaction)"
                                );

            super.optimisticReplaceLogged(transaction);
            // So that the state is set correctly both at runtime and on recovery we do it here.
            // No need to synchronize to protect state because only the locking transaction can change it.
            setState(nextStateForRemove); // Make the state change. 

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "optimisticReplaceLogged"
                                );
        } // End of optimisticReplaceLogged method.

        /**
         * The transaction is about to commit so finalise up any changes to persistent state.
         * This is called for the transaction adding or deleting the Entry but not for other
         * entries, since they do not register for the callback.
         * 
         * @param transaction which is commiting.
         * @throws ObjectManagerException
         */
        public void preCommit(Transaction transaction)
                        throws ObjectManagerException
        {
            final String methodName = "preCommit";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction,
                                                                    new Integer(state),
                                                                    stateNames[state] });

            super.preCommit(transaction);

            // No need to synchronize to protect state because only the locking transaction can change it.
            switch (state) {
                case stateToBeAdded:
                    // Give up the space we reserved in case the add backed out.
                    // During recovery we don't do this because we will not have reserved the space anyway.
                    if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog)
                        owningToken.objectStore.reserve(-(int) treeMap.storeSpaceForRemove(), false);
                    break;

                case stateToBeDeleted:
                case stateMustBeDeleted:
                    // We implicitly write commit to the log if we write the following delete/replace records.
                    // as the transaction manager might change its mind and roll back after a restart if we dont complete
                    // the commit.
                    // Removal is only done if we are in the tree, it would not be done again during recovery
                    // if we succeed in writing the updates to the adjacent entries now.
                    // Now we lock the TreeMap to this thread alone, until we have restructured the tree.
                    treeMap.deleteEntry(this
                                        , transaction
                                    );
                    break;

                case stateRemoved:
                    // We have been driven during recovery after the removal from 
                    // stateToBeDeleted or stateMustBeDeleted has been recovered.    
                    break;

                default:
                    // Transition to error state and throw StateErrorException 
                    // it is not safe to continue in this state.
                    setState(nextStateForError);

            } // switch.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { new Integer(state),
                                                                   stateNames[state] });
        } // preCommit().

        /**
         * The transaction is about to backed out so fix up any transient state we have changed.
         * This is called for the transaction adding or deleting the Link but not for other Entries
         * chaining to this one, since they do not register for the callback.
         * We do not use the default ObjectManager behaviour of restoring a beofre immage
         * because we allow changes to be made to entries after they are written to the log but
         * before commit, so now we have to repair any changes dependant on the current state of
         * the tree.
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#preBackout(com.ibm.ws.objectManager.Transaction)
         */
        public void preBackout(Transaction transaction)
                        throws ObjectManagerException
        {
            final String methodName = "preBackout";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction,
                                                                    new Integer(state),
                                                                    stateNames[state] });
            super.preBackout(transaction);

            // No need to synchronize to protect state because only the locking transaction can change it.
            switch (state) {
                case stateToBeAdded:
                case stateMustBeDeleted:
                    // Remove this Entry from the tree. We previously added it assuming we would commit
                    // but we were wrong so now reverse the action of adding it by removing the entry
                    // from the tree. This causes extra updates to the ObjectStore and extra log records
                    // to be written.
                    // The OptimisticReplace for removal from the tree also records whether we are 
                    // commiting or backing out.
                    treeMap.deleteEntry(this, transaction);
                    break;

                case stateNotAdded:
                    // We have been driven during recovery after the backout and removal from stateToBeAdded has been recovered.
                    break;

                case stateToBeDeleted:
                    // Give back the space we would have used to remove the entry.
                    // During recovery we don't do this because we will not have reserved the space anyway.
                    if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog)
                        owningToken.objectStore.reserve(-(int) treeMap.storeSpaceForRemove(), false);
                    break;

                case stateRemoved:
                    // We have been driven during recovery after the backout and removal from stateMustBeAdded has been recovered.
                    break;

                default:
                    // Transition to error state and throw StateErrorException 
                    // it is not safe to continue in this state.
                    setState(nextStateForError);

            } // switch.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { new Integer(state),
                                                                   stateNames[state] });
        } // preBackout().

        /**
         * The transaction has commited so now we can make any updated entries available.
         * This is called for the transaction adding or deleting the Entry but not for other Entries
         * chaining to this one, since they do not lock the entry.
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#commit(com.ibm.ws.objectManager.Transaction, com.ibm.ws.objectManager.ObjectManagerByteArrayOutputStream, long, boolean)
         */
        public void commit(Transaction transaction
                           , ObjectManagerByteArrayOutputStream serializedBytes
                           , long logSequenceNumber
                           , boolean requiresCurrentCheckpoint)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "commit"
                            , new Object[] { transaction, serializedBytes, new Long(logSequenceNumber), new Boolean(requiresCurrentCheckpoint) }
                                );

            super.commit(transaction, serializedBytes, logSequenceNumber, requiresCurrentCheckpoint);

            // The entry must have been added or removed by this transaction otherwise optimisticReplaceCommit 
            // would have been called.

            // No need to synchronize to protect state because only the locking
            // transaction can change it. Unlocked has not occured yet. 
            switch (state) {
                case stateToBeAdded:
                    setState(nextStateForCommit);
                    synchronized (treeMap) { // Now we lock the LinkedList to this thread alone.
                        if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog)
                            treeMap.availableSize++; // Adjust list visible length available.
                    } // synchronized (treeMap).
                    break;

                default:
                    // Make the state change. Transition to error state and throw StateErrorException 
                    // if the state is invalid because it is not safe to continue.  
                    setState(nextStateForCommit);

            } // switch.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "commit",
                           new Object[] { new Long(treeMap.size),
                                         new Long(treeMap.availableSize) });
        } // commit().

        /**
         * The transaction has backed out so make any backed out entries available again.
         * This is called for the transaction adding or deleting the Entry but not for other Entries
         * chaining to this one, since they do not lock the entry.
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#backout(com.ibm.ws.objectManager.Transaction, long, boolean)
         */
        public void backout(Transaction transaction,
                            long logSequenceNumber,
                            boolean requiresCurrentCheckpoint)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "backout"
                            , new Object[] { transaction, new Long(logSequenceNumber), new Boolean(requiresCurrentCheckpoint) }
                                );

            super.backout(transaction, logSequenceNumber, requiresCurrentCheckpoint);

            // The entry must have been added or removed by this transaction otherwise optimisticReplaceBackout 
            // would have been called.

            // No need to synchronize to protect state because only the locking transaction can change it.
            switch (state) {

                case stateToBeDeleted:
                    synchronized (treeMap) { // Now we lock the TreeMap to this thread alone.
                        setState(nextStateForBackout); // Make the state change.
                        if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog)
                            treeMap.availableSize++; // Adjust list visible length available.
                    } // synchronized (treeMap).
                    break;

                default:
                    // Make the state change. Transition to error state and throw StateErrorException 
                    // if the state is invalid because it is not safe to continue.  
                    setState(nextStateForBackout);
            } // switch (state).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "backout"
                           , new Object[] { new Long(treeMap.size),
                                           new Long(treeMap.availableSize) });
        } // backout().

        /**
         * Driven just after the ObjectManager finishes replaying the log,
         * but before it backs out any incomplete tranmsactions and starts to make
         * forward progress.
         * 
         * @param transaction which has been recovered.
         * @throws ObjectManagerException
         */
        public void recoveryCompleted(Transaction transaction)
                        throws ObjectManagerException
        {
            final String methodName = "recoveryCompleted";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction,
                                                                    new Integer(state),
                                                                    stateNames[state] });

            // Synchronize should not be necessary since recovery is single threaded currently.
            synchronized (treeMap) { // Now we lock the TreeMap to this thread alone.

                switch (state) {
                    case stateToBeAdded:
                        // We have not yet added the link so temporarily reduce the length. We incremented list.length
                        // assuming we would commit and restored that to availableLength.
                        treeMap.availableSize--;
                        // Reserve space in the store, this should succeed because we reserved the space before we
                        // crashed.
                        owningToken.objectStore.reserve((int) treeMap.storeSpaceForRemove(), false);
                        break;

                    case stateMustBeDeleted:
                    case stateToBeDeleted:
                        // The transaction that wants to delete this link has not yet completed.
                        treeMap.availableSize--; // Adjust list visible length available.
                        // Reserve space in the store, this should succeed because we reserved the space before we
                        // crashed.
                        owningToken.objectStore.reserve((int) treeMap.storeSpaceForRemove(), false);
                        break;

                    case stateRemoved:
                        // We have been driven during recovery after the removal from
                        // stateToBeDeleted or stateMustBeDeleted has been recovered.
                        // For example we crashed after the Optimistic replace was written to the
                        // log but before commit was written.
                        break;

                    case stateNotAdded:
                        // We have been driven during recovery after the replay of add and delete
                        // has been recovered or when an add was backed out.
                        break;

                    default:
                        // Transition to error state and throw StateErrorException 
                        // it is not safe to continue in this state.
                        setState(nextStateForError);

                } // switch.

            } // synchronized (treeMap).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // recoveryCompleted().

        /**
         * Test a state transition.
         * 
         * @param nextState mapping the current state to the new state.
         * @throws InvalidStateException if the transition is invalid.
         */
        private void testState(int[] nextState)
                        throws InvalidStateException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this
                            , cclass
                            , "testState"
                            , new Object[] { nextState, new Integer(state), stateNames[state] }
                                );

            int newState = nextState[state]; // Make the state change.       

            if (newState == stateError) {
                InvalidStateException invalidStateException = new InvalidStateException(this, state, stateNames[state]);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "testState"
                               , new Object[] { invalidStateException, new Integer(newState), stateNames[newState] }
                                    );
                throw invalidStateException;
            } // if (state == stateError).  

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this
                           , cclass
                           , "testState"
                                );
        } // testState().

        /**
         * Makes a state transition.
         * 
         * @param nextState mapping the current state to the new state.
         * @throws StateErrorException if the transition is invalid.
         */
        private void setState(int[] nextState)
                        throws StateErrorException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "setState",
                            new Object[] { nextState, new Integer(state), stateNames[state] }
                                );
            // No need to synchronize in here bacause only one transaction can add or delete
            // Entries at a time. Other transactions performing OptimisticReplace updates 
            // do not change the state.
            previousState = state; // Capture the previous state for dump. 
            state = nextState[state]; // Make the state change.       

            if (state == stateError) {
                StateErrorException stateErrorException = new StateErrorException(this, previousState, stateNames[previousState]);
                ObjectManager.ffdc.processException(this, cclass, "setState", stateErrorException, "1:3283:1.44");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "setState"
                               , new Object[] { stateErrorException, new Integer(state), stateNames[state] }
                                    );
                throw stateErrorException;

            } // if (state == stateError).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "setState"
                           , new Object[] { new Integer(state), stateNames[state] }
                                );
        } // End of setState method.

        // ----------------------------------------------------------------------------------------
        // implements Map.Entry.
        // ----------------------------------------------------------------------------------------

        /**
         * Replaces the Token currently associated with the key with the given
         * Token within the scope of a transaction.
         * 
         * @param newValue the new Value to be associated with this Map.Entry
         *            when the transaction commits.
         * @param transaction which controls the update.
         * @return Token the value associated with the entry before this method was
         *         called.
         * @exception ObjectManagerException
         */
        public Token setValue(Token newValue,
                              Transaction transaction)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "setValue"
                            , new Object[] { newValue, transaction, this.value });

            // TODO Make this work.
            System.out.println("setValue not working!!!");
            if (true)
                throw new UnsupportedOperationException("setValue not supported");
            // Ensure it is safe to proceed.
            //!! Cannot lock this as it would prevent the rebalancing of the rest of the tree by another
            //!! transaction.
            transaction.lock(this);
            Token oldValue = this.uncommitedValue;
            this.uncommitedValue = value;
            transaction.requestCallback(owningToken);
            trace.exit(this, cclass
                       , "setValue"
                       , "returns oldValue=" + oldValue + "(Object)"
                            );
            return oldValue;
        } // of setValue.

        /**
         * @return int the current state of this Map.Entry.
         * @throws ObjectManagerException
         */
        public int getEntryState()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "getEntryState"
                                );

            final int[] entryStateMap = new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };
            int stateToReturn = entryStateMap[state];

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "getEntryState"
                           , "returns statetoReturn=" + stateToReturn + "(int) " + stateNames[stateToReturn] + "(String)"
                                );
            return stateToReturn;
        } // End of getState method.

        /**
         * Remove the entry from the map.
         * 
         * @param transaction coordinating the removal.
         * @throws ObjectManagerException
         */
        public void remove(Transaction transaction)
                        throws ObjectManagerException {
            final String methodName = "remove";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction });

            boolean spaceReserved = false;

            try {
                synchronized (transaction.internalTransaction) {
                    synchronized (treeMap) {
                        if ((state == Entry.stateToBeAdded)
                            && lockedBy(transaction)) {
                            // Log what we intend to do. Reserve enough spare log space so that the eventual 
                            // optimistic replace that removes the entry from the tree is certain to succeed 
                            // in being written as well.
                            transaction.delete(this, logSpaceForDelete);
                            // Mark for deletion. 
                            requestDelete(transaction);
                            // In the case where we delete an entry added by the same transaction we have not incremented 
                            // the available size yet because we have not yet committed so we would not decrement it.
                            // No treeMap.availableSize--;

                        } else if (state == Entry.stateAdded) {
                            // Reserve space in the store, if not available, we fail here.
                            owningToken.objectStore.reserve((int) treeMap.storeSpaceForRemove(), false);
                            spaceReserved = true;

                            // Log what we intend to do. Reserve enough spare log space so that the eventual
                            // optimistic replace that removes the entry from the tree is certain to succeed
                            // in being written as well.
                            transaction.delete(this, logSpaceForDelete);
                            // Mark for deletion.
                            requestDelete(transaction);
                            // If the entry was available to all adjust the visible length available.
                            treeMap.availableSize--;

                        } else {
                            // Cannot remove this one.
                            InvalidStateException invalidStateException = new InvalidStateException(this, state, stateNames[state]);
                            ObjectManager.ffdc.processException(this, cclass, methodName, invalidStateException, "1:3410:1.44");
                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this, cclass, methodName, new Object[] { "Invalid State 3412", invalidStateException });
                            throw invalidStateException;
                        } // if OK to remove.

                    } // synchronized (treeMap).
                } // synchronized (transaction.internalTransaction).

            } catch (LogFileFullException exception) {
                // No FFDC Code Needed, transaction.delete() has already done this.
                // Release space we reserved in the store.
                if (spaceReserved)
                    owningToken.objectStore.reserve(-(int) treeMap.storeSpaceForRemove(), false);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, exception);
                throw exception;

                // We should not see ObjectStoreFullException because the ObjectStore already 
                // holds sufficient space to guarantee deletes work.
//      } catch (ObjectStoreFullException exception) {
//      // No FFDC Code Needed, transaction.delete() has already done this.
//      treeMap.unRemove();
//      if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//      trace.exit(this, cclass, methodName, exception);
//      throw exception;

            } // try...

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // remove().

        // --------------------------------------------------------------------------
        // Simplified serialization.
        // --------------------------------------------------------------------------

        /**
         * No argument constructor.
         * 
         * @exception ObjectManagerException
         */
        Entry()
            throws ObjectManagerException
        {
            super(null, null);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "<init>"
                                );

            previousState = -1; // No previous state.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "<init>"
                                );
        } // Entry().

        private static final byte SimpleSerialVersion = 0;

//    private static final int maxsz = getSerializedSize();
//    static int getSerializedSize()
//    {
//      int size =0;
//      try {
//        Entry testEntry = new Entry(); 
//        testEntry.owningToken = new Token();
//        testEntry.treeToken = new Token();
//        testEntry.key = new Token();
//        testEntry.value = new Token();       
//        testEntry.parent = new Token();
//        testEntry.left = new Token();
//        testEntry.right = new Token();
//        testEntry.color = true;
//        
//        size = testEntry.getSerializedBytes().length; 
//      } catch (Exception e)
//      {System.out.println("GetSerializedSize exception="+e);}
//      System.out.println("size="+size);
//      return size;
//    }

        /*
         * @see com.ibm.ws.objectManager.ManagedObject#maximumSerializedSize()
         */
        public static final long maximumSerializedSize()
        {
            // TODO Should be able to set max size of key, not assume 99.
            return 1 // Version.
                   + ManagedObject.maximumSerializedSize()
                   + 3 // Flag bytes
                   + 99 // Unknown size of key.
                   + 5 * Token.maximumSerializedSize() // Value,Tree,Left,Right,Parent
                   + 4 // Colour
            ;
        }

        /*
         * (non-Javadoc)
         * 
         * @see int SimplifiedSerialization.getSignature()
         */
        public int getSignature()
        {
            return signature_TreeMap_Entry;
        } // getSignature().

        /*
         * (non-Javadoc)
         * 
         * @see SimplifiedSerialization.writeObject(java.io.DataInputStream)
         */
        public final void writeObject(java.io.DataOutputStream dataOutputStream)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "writeObject",
                            new Object[] { dataOutputStream });

            try {
                dataOutputStream.writeByte(SimpleSerialVersion);
                super.writeObject(dataOutputStream);

                treeToken.writeObject(dataOutputStream); // The tree responsible for this Entry.
                java.io.ObjectOutputStream objectOutputStream = new java.io.ObjectOutputStream(dataOutputStream);
                objectOutputStream.writeObject(key);
                objectOutputStream.close();
                value.writeObject(dataOutputStream);

                if (left == null) {
                    dataOutputStream.writeByte(0);
                } else {
                    dataOutputStream.writeByte(1);
                    left.writeObject(dataOutputStream);
                } // if (left == null).
                if (right == null) {
                    dataOutputStream.writeByte(0);
                } else {
                    dataOutputStream.writeByte(1);
                    right.writeObject(dataOutputStream);
                } // if (right == null).  
                if (parent == null) {
                    dataOutputStream.writeByte(0);
                } else {
                    dataOutputStream.writeByte(1);
                    parent.writeObject(dataOutputStream);
                } // if (parent == null).  
                dataOutputStream.writeInt(color ? 0 : 1);

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "writeObject", exception, "1:3560:1.44");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "writeObject"
                               , "via PermanentIOException"
                                    );
                throw new PermanentIOException(this
                                               , exception);
            } // catch (java.io.IOException exception).

            // Did we exceed the size limits?
            // Check whether key caused us to exceed maximum serialized length.
            if (dataOutputStream.size() > maximumSerializedSize())
            {
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this, cclass
                                , "writeObject"
                                , new Object[] { new Long(maximumSerializedSize()), new Integer(dataOutputStream.size()) }
                                    );
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "writeObject"
                               , "via SimplifiedSerializationSizeException"
                                    );
                throw new SimplifiedSerializationSizeException(this
                                                               , maximumSerializedSize()
                                                               , dataOutputStream.size());
            } // if (dataOutputStream.size() > maximumSerializedSize())

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "writeObject"
                                );
        } // writeObject().

        /*
         * (non-Javadoc)
         * 
         * @see SimplifiedSerialization.readObject(java.io.DataInputStream,ObjectManagerState)
         */
        public void readObject(java.io.DataInputStream dataInputStream
                               , ObjectManagerState objectManagerState
                        )
                                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "readObject"
                            , "dataInputStream=" + dataInputStream + "(java.io.DataInputStream)"
                              + " objectManagerState=" + objectManagerState + "(ObjectManagerState)"
                                );

            try {
                byte version = dataInputStream.readByte();
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this, cclass
                                , "readObject"
                                , "version=" + version + "(byte)"
                                    );
                super.readObject(dataInputStream
                                 , objectManagerState
                                );

                // The tree responsible for this Entry.
                treeToken = Token.restore(dataInputStream
                                          , objectManagerState
                                );
                ManagedObjectInputStream managedObjectInputStream = new ManagedObjectInputStream(dataInputStream, objectManagerState);
                key = managedObjectInputStream.readObject();
                managedObjectInputStream.close();

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "readObject", exception, "1:3633:1.44");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "readObject"
                               , "via PermanentIOException"
                                    );
                throw new PermanentIOException(this
                                               , exception);

            } catch (java.lang.ClassNotFoundException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "readObject", exception, "1:3646:1.44");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "readObject"
                               , "via ClassNotFoundException"
                                    );
                throw new com.ibm.ws.objectManager.ClassNotFoundException(this, exception);
            } // catch (java.io.IOException exception).   

            value = Token.restore(dataInputStream
                                  , objectManagerState
                            );
            try {
                if (dataInputStream.readByte() == 1)
                    left = Token.restore(dataInputStream
                                         , objectManagerState
                                    );
                if (dataInputStream.readByte() == 1)
                    right = Token.restore(dataInputStream
                                          , objectManagerState
                                    );
                if (dataInputStream.readByte() == 1)
                    parent = Token.restore(dataInputStream
                                           , objectManagerState
                                    );
                color = (dataInputStream.readInt() == 0);

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "readObject", exception, "1:3676:1.44");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "readObject"
                               , "via PermanentIOException"
                                    );
                throw new PermanentIOException(this
                                               , exception);
            } // catch (java.io.IOException exception). 

            treeMap = (TreeMap) (treeToken.getManagedObject());
            // The treeMap may have been deleted, put a dummy tree in place, 
            // to satisfy replay during recovery but do not allocate it to a store.
            if (treeMap == null)
                treeMap = new TreeMap();

            // Assume we were added, corrected in add() and delete() methods if appropriate.
            state = stateAdded;
            previousState = -1; // No previous state.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "readObject"
                                );
        } // readObject().

        // --------------------------------------------------------------------------
        // implements java.io.Serializable
        // --------------------------------------------------------------------------
        /**
         * Customized deserialization.
         * 
         * @param objectInputStreamIn containing the serialize Object.
         * @throws java.io.IOException
         * @throws java.lang.ClassNotFoundException
         */
        private void readObject(java.io.ObjectInputStream objectInputStreamIn)
                        throws java.io.IOException
                        , java.lang.ClassNotFoundException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "readObject"
                            , "objectInputStreamIn=" + objectInputStreamIn + "(java.io.ObjectInputStream)"
                                );

            objectInputStreamIn.defaultReadObject();
            try {
                treeMap = (TreeMap) (treeToken.getManagedObject());
                // The treeMap may have been deleted, put a dummy tree in place, 
                // to satisfy replay during recovery but do not allocate it to a store.
                if (treeMap == null)
                    treeMap = new TreeMap();

            } catch (ObjectManagerException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "readObject", exception, "1:3733:1.44");

                // TODO Dont like this.
                throw new java.lang.ClassNotFoundException();
            } // catch (ObjectManagerException exception).

            // Assume we were added, corrected in add() and delete() methods if appropriate.
            state = stateAdded;
            previousState = -1; // No previous state.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "readObject");
        } // readObject().

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            return new String("TreeMap:"
                              + "(key=" + key + " value=" + value + "/" + stateNames[state]
                              + " " + super.toString() + ")");
        } // toString().
    } // class Entry.
} // class TreeMap.
