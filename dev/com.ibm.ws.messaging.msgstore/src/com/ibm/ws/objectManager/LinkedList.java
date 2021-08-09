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

/**
 * A recoverable, doubly linked list. In order to avoid locking the list
 * during the prepare phase we allow updates on prepared links. This
 * means we cannot use the default ObjectManager behaviour of restoring
 * a before image of the modified links if the transacton backs out.
 * Instead we mark links for addition and deletion and if they are added
 * we must be prepared to write new links to remove them if the transaction
 * is backed out.
 * 
 * Locking Strategy.
 * -----------------
 * Lock Transaction.InternalTransaction then List.
 * 
 * @version 1.01 15 Feb 1996
 * @author Andrew Banks
 */
public class LinkedList
                extends AbstractList
                implements List, SimplifiedSerialization, Printable
{
    private static final Class cclass = LinkedList.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_LISTS);

    private static final long serialVersionUID = 4680368684147217734L;

    public static final boolean gatherStatistics = false; // Built for statistics if true.

    // The anchor for the beginning and the end of a list, including any elements
    // being added or removed under the scope of a transaction.
    // Protectged by synchronized locks on List.this.
    // First element in the list.
    protected Token head;
    // First available element in the list.
    protected transient Token availableHead;
    // Final element in the list.
    protected Token tail;

    // All updates to add or remove links in the list must be made together, so we
    // construct a single log record for all of them. This saves us from having to
    // decide whether we have the complete set if we have to perform recovery.
    // The following lists are used to construct the groups of links. 
    // Protected by synchronizing on List.this.
    transient java.util.List managedObjectsToAdd;
    transient java.util.List managedObjectsToReplace;
    transient java.util.List tokensToNotify;

    // Space reserved for transaction operations being performed under the synchronize lock 
    // on List.this.
    transient long reservedSpaceInStore;

    // Elements in the list assuming all of the current transactions commit.
    // TODO Need to handle sizes greater tha Long.MAX_VALUE.
    private long size = 0;
    // Elements in the list not including those just added or deleted which are
    // available to new transactions. The total length available to a transaction 
    // is the availableSize plus those elements added by that transaction.
    private transient long availableSize = 0;
    private transient Object availableSizeLock = new Object();

    // For gatherStatistics.
    // Maximum available length of the list.
    protected long maximumAvailableSize = 0;

    /**
     * Constructor creates an empty list.
     * 
     * @param transaction under which the new list is created.
     * @param objectStore where the list is stored. !! Dont seem to need this.
     * @throws ObjectManagerException
     */
    public LinkedList(Transaction transaction,
                      ObjectStore objectStore)
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "<init>",
                        new Object[] { transaction, objectStore });

        objectStore.allocate(this);
        reservedSpaceInStore = maximumSerializedSize() + owningToken.objectStore.getAddSpaceOverhead();
        owningToken.objectStore.reserve((int) reservedSpaceInStore, true);
        transaction.add(this);
        owningToken.objectStore.reserve(-(int) reservedSpaceInStore, false);

        managedObjectsToAdd = new java.util.ArrayList(1);
        managedObjectsToReplace = new java.util.ArrayList(3);
        tokensToNotify = new java.util.ArrayList(1);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "<init>");
    } // LinkedList().

    /**
     * @return long The number of bytes of logSpace to reserve for an addition
     *         to the list so that the later stages can complete.
     */
    static long logSpaceForAdd()
    {
        return (TransactionOptimisticReplaceLogRecord.maximumSerializedSize()
                + FileLogOutput.partHeaderLength
                + maximumSerializedSize() + 2 * Link.maximumSerializedSize()

                + TransactionCheckpointLogRecord.maximumSerializedSize()
                + FileLogOutput.partHeaderLength
                + Token.maximumSerializedSize() * 3);
    } // logSpaceForAdd().

    /**
     * @return long The number of bytes of logSpace to reserve for an deletion
     *         from the list so that the later stages can complete.
     */
    static long logSpaceForDelete()
    {
        return (TransactionOptimisticReplaceLogRecord.maximumSerializedSize()
                + FileLogOutput.partHeaderLength
                + Token.maximumSerializedSize() * 1 // For notify.
                + maximumSerializedSize() + 2 * Link.maximumSerializedSize()

                + TransactionOptimisticReplaceLogRecord.maximumSerializedSize()
                + FileLogOutput.partHeaderLength
                + Token.maximumSerializedSize() * 4);
    } // logSpaceForDelete().

    /**
     * The space we reserve in the ObjectStore before we begin a remove operation.
     * 
     * @return long a worst case estimate of the ObjectStore space needed to remove an entry
     *         from this list.
     */
    long storeSpaceForRemove() {
        return LinkedList.maximumSerializedSize() // List header.
               + 2 * LinkedList.Link.maximumSerializedSize() // Previous,Next links.  
               + 3 * owningToken.objectStore.getAddSpaceOverhead(); // Store overhead for all of the above.
    } // storeSpaceForRemove()  

    /**
     * The space we reserve in the ObjectStore before we begin an add operation.
     * 
     * @return long a worst case estimate of the ObjectStore space needed to add an entry
     *         to this list.
     */
    long storeSpaceForAdd() {
        return LinkedList.maximumSerializedSize() // List header.
               + 3 * LinkedList.Link.maximumSerializedSize() // Current,Previous,Next links.  
               + 4 * owningToken.objectStore.getAddSpaceOverhead() // Store overhead for all of the above.
               + storeSpaceForRemove(); // In case we backout the add.
    } // storeSpaceForAdd()

    /**
     * Increase the saved count of links by one.
     * Caller must be synchronized on list.
     */
    protected final void incrementSize()
    {
        size++;
        if (gatherStatistics)
            if (size > maximumAvailableSize)
                maximumAvailableSize = size;
    } // method incrementSize().

    /**
     * Reduce the saved count of links by one.
     * Caller must be synchronized on list.
     */
    protected final void decrementSize()
    {
        size--;
    } // decrementSize().

    /**
     * Increase the count of links available by one.
     */
    protected final void incrementAvailableSize()
    {
        synchronized (availableSizeLock) {
            availableSize++;
        } // synchronized (availableSizeLock).
    } // method incrementAvailableSize()

    /**
     * Reduce the count of links available by one.
     */
    protected final void decrementAvailableSize()
    {
        synchronized (availableSizeLock) {
            availableSize--;
        } // synchronized (availableSizeLock).
    } // decrementAvailableSize().

    /**
     * Builds a set of properties containing the current statistics.
     * 
     * @return java.util.Map the statistics.
     */

    public java.util.Map captureStatistics()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "captureStatistics");

        java.util.Map statistics = new java.util.HashMap();
        statistics.put("maximumAvailableSize",
                       Long.toString(maximumAvailableSize));

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "captureStatistics",
                       new Object[] { statistics });
        return statistics;
    } // captureStatistics().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#add(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public boolean add(Token token,
                       Transaction transaction)
                    throws ObjectManagerException
    {
        addEntry(token, transaction);
        return true;
    } // add().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#addEntry(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public List.Entry addEntry(Token token,
                               Transaction transaction)
                    throws ObjectManagerException
    {
        final String methodName = "addEntry";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { token,
                                                                transaction });

        // This differs from the default ObjectManager
        // behaviour because we optimistically add the link to the list and may need to 
        // undo this if the transaction backs out. Simply restoring before immages is not
        // enough because we dont lock the list during the prepare phase and other links
        // could be chanied off this one and commit their trandsactions before
        // this one.

        Link newLink = null;
        // Take a lock on transaction then the list itself, this protects the structure of the list 
        // and prevents deadlock with threads backing out the transaction. During backout the list 
        // lock is traven in the preBackout call back after the transaction lock has been taken. 
        synchronized (transaction.internalTransaction) {
            synchronized (this) {
                // Prevent other transactions using the list until its creation is commited.
                if (!(state == stateReady)
                    && !((state == stateAdded) && lockedBy(transaction))) {

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, new Object[] { new Integer(state),
                                                                           stateNames[state] });
                    throw new InvalidStateException(this, state, stateNames[state]);
                }

                // All optimistic replace updates to links in the list must be made
                // together, so we construct a single log record for all of them. 
                managedObjectsToAdd.clear();
                managedObjectsToReplace.clear();
                reservedSpaceInStore = storeSpaceForAdd();
                owningToken.objectStore.reserve((int) reservedSpaceInStore, false);

                // Create the new link and add it to its object store.
                newLink = new Link(this,
                                   token,
                                   tail,
                                   null,
                                   transaction);
                managedObjectsToAdd.add(newLink);

                // Chain the new Link in the list. If the transaction backs
                // out we will rewrite the links to remove the new one.
                if (head == null) // Only element in the list?
                {
                    head = newLink.getToken();
                    availableHead = head; // PK75215
                } else { // In the body of the list.
                    Link tailLink = (Link) tail.getManagedObject();
                    tailLink.next = newLink.getToken();
                    managedObjectsToReplace.add(tailLink);
                }
                tail = newLink.getToken();

                incrementSize(); // Adjust list length assuming we will commit.
                managedObjectsToReplace.add(this); // The anchor for the list.

                // Harden the updates. If the update fails, because the log is full,
                // we have affected the structure of the list so we will have to 
                // reverse the changes. Reserve enough space to reverse the update if the
                // transaction backs out.
                // TODO We need another optimistic replace with a more restricted state machine, this one
                // will allow an add after we have done prepare.
                try {
                    transaction.optimisticReplace(managedObjectsToAdd,
                                                  managedObjectsToReplace,
                                                  null, // No tokens to delete.
                                                  null, // No tokens to notify.
                                                  +logSpaceForDelete());
                    // Give up reserved space, but keep back enough to remove the entry if we have to.
                    owningToken.objectStore.reserve((int) (storeSpaceForRemove() - reservedSpaceInStore), false);

                } catch (InvalidStateException exception) {
                    // No FFDC Code Needed, user error.
                    // Remove the link we just added.
                    undoAdd(newLink);

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, exception);
                    throw exception;

                } catch (LogFileFullException exception) {
                    // No FFDC Code Needed, InternalTransaction has already done this.
                    // Remove the link we just added.
                    undoAdd(newLink);

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, exception);
                    throw exception;

                    // We should not see ObjectStoreFullException because we have preReserved 
                    // the ObjectStore space.
//        } catch (ObjectStoreFullException exception) {
//          // No FFDC Code Needed, InternalTransaction has already done this.
//          // Remove the link we just added.
//          undoAdd(newLink);
//          
//          if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//            trace.exit(this, cclass, methodName, exception);
//          throw exception;
                } // try.

            } // synchronized (this).
        } // synchronized (transaction.internalTransaction)

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { newLink });
        return (List.Entry) newLink;
    } // addEntry().

    /**
     * Insert before the given link. Caller must be synchronized on list.
     * 
     * @param newToken to insert.
     * @param insertPoint which the new link will be before.
     * @param transaction controling the insertion.
     * @return Link the inserted Link.
     * @throws ObjectManagerException
     * @exception java.util.NoSuchElementException if the current element is deleted.
     */
    protected Link insert(Token newToken,
                          Link insertPoint,
                          Transaction transaction)
                    throws ObjectManagerException
    {
        final String methodName = "insert";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { newToken,
                                                                insertPoint,
                                                                transaction });

        // Prevent other transactions using the list until its creation is commited.
        if (!(state == stateReady)
            && !((state == stateAdded) && lockedBy(transaction))) {

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { new Integer(state),
                                                                   stateNames[state] });
            throw new InvalidStateException(this, state, stateNames[state]);
        }

        // All replacements to links in the list must be made together, so we
        // construct a single log record for all of them.
        managedObjectsToAdd.clear();
        managedObjectsToReplace.clear();
        reservedSpaceInStore = storeSpaceForAdd();
        owningToken.objectStore.reserve((int) reservedSpaceInStore, false);

        // Create the new link and add it to its object store.
        Link newLink = new Link(this,
                                newToken,
                                insertPoint.previous,
                                insertPoint.owningToken,
                                transaction);
        managedObjectsToAdd.add(newLink);

        // Chain the new Link into the list.
        if (insertPoint.previous == null) // Cursor at head of list?
        {
            if (head == null) // Only/first element in the list?
                tail = newLink.getToken();
            else {
                insertPoint.previous = newLink.getToken();
                managedObjectsToReplace.add(insertPoint);
            }
            head = newLink.getToken();
        } else {
            // Chain the new element into the body of the list.
            Link previousLink = (Link) insertPoint.previous.getManagedObject();
            previousLink.next = newLink.getToken();
            managedObjectsToReplace.add(previousLink);
            // Cursor at the end of the list?
            if (insertPoint.owningToken == tail) {
                tail = newLink.getToken();
            } else { // Not at the end of the list.
                insertPoint.previous = newLink.getToken();
                managedObjectsToReplace.add(insertPoint);
            }
        } // If at head of list?

        // We dont know where the link is added so assume it is ahead of the
        // availableHead.
        availableHead = head;
        skipToBeDeleted();

        incrementSize(); // Adjust list length assuming we will commit.
        managedObjectsToReplace.add(this);

        // Harden the updates. If the update fails, because the log is full,
        // we have affected the structure of the list so we will have to 
        // reverse the changes. Reserve enough space to reverse the update if the
        // transaction backs out.
        try {
            transaction.optimisticReplace(managedObjectsToAdd,
                                          managedObjectsToReplace,
                                          null, // No tokens to delete.
                                          null, // No tokens to notify.
                                          +logSpaceForDelete());
            // Give up reserved space, but keep back enough to remove the entry if we have to.
            owningToken.objectStore.reserve((int) (storeSpaceForRemove() - reservedSpaceInStore), false);

        } catch (InvalidStateException exception) {
            // No FFDC Code Needed, user error.
            // Remove the link we just added.
            undoAdd(newLink);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, exception);
            throw exception;

        } catch (LogFileFullException exception) {
            // No FFDC Code Needed, InternalTransaction has already done this.
            // Remove the link we just added.
            undoAdd(newLink);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, exception);
            throw exception;

            // We should not see ObjectStoreFullException because we have preReserved 
            // the ObjectStore space.
//    } catch (ObjectStoreFullException exception) {
//      // No FFDC Code Needed, InternalTransaction has already done this.
//      // Remove the link we just added.
//      undoAdd(newLink);
// 
//      if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//        trace.exit(this, cclass, methodName, exception);
//      throw exception;
        } // try.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { newLink });
        return newLink;
    } // insert().

    /**
     * Reverse the action of addition to the list,
     * used after an add has failed to log anything.
     * Does not perform any logging.
     * The caller must be synchronized on List.
     * 
     * @param newLink to be removed from the list.
     * @throws ObjectManagerException
     */
    void undoAdd(Link newLink)
                    throws ObjectManagerException {
        final String methodName = "undoAdd";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { newLink });

        // Detach this Link from the list.
        if (newLink.next == null) { // Are we the tail of list?
            tail = newLink.previous;
        } else { // Not tail of the list.
            // Join up the backwards Link.
            Link nextLink = (Link) newLink.next.getManagedObject();
            nextLink.previous = newLink.previous;
        } // if at tail of list.

        if (newLink.previous == null) { // Are we the head of list?
            head = newLink.next;
        } else { // Not head of the list.
            // Join up the forwards Link.
            Link previousLink = (Link) newLink.previous.getManagedObject();
            previousLink.next = newLink.next;
        } // if at head of list.

        // Reset the availableHead.
        availableHead = head;
        skipToBeDeleted();
        decrementSize(); // Adjust list length. 

        // Give back all of the remaining space.
        owningToken.objectStore.reserve((int) -reservedSpaceInStore, false);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // undoAdd().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#getFirst(com.ibm.ws.objectManager.Transaction)
     */
    public Token getFirst(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "getFirst",
                        new Object[] { transaction });

        Link first;
        Token tokenToReturn;
        synchronized (this) {
            first = nextLink(null,
                             transaction,
                             getTransactionUnlockSequence());

            if (first == null) { // Is there a first element?
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "getFirst",
                               new Object[] { first });
                throw new java.util.NoSuchElementException();
            } // if (first == null).

            // Assign the tokenToReturn while we have the list locked so that the ObjectManager 
            // cannot backout this transaction and remove the link under anoy=ther transaction 
            // causing this thread to see a null Token as a return value.
            tokenToReturn = first.data;
        } // synchronized (this).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "getFirst",
                       new Object[] { tokenToReturn });
        return tokenToReturn;
    } // getFirst().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#remove(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public boolean remove(Token token,
                          Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "remove",
                        new Object[] { token, transaction });

        Iterator iterator = iterator();
        boolean removed = false;
        synchronized (iterator) {
            synchronized (transaction.internalTransaction) {
                synchronized (this) {
                    try {
                        while (iterator.next(transaction) != token);
                        iterator.remove(transaction);
                        removed = true;
                    } catch (java.util.NoSuchElementException exception) {
                        // No FFDC code needed.
                    } // try.
                } // synchronized (this).
            } // synchronized (transaction.internalTransaction).
        } // synchronized (iterator).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "remove",
                       new Object[] { new Boolean(removed) });
        return removed;
    } // remove().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#removeFirst(com.ibm.ws.objectManager.Transaction)
     */
    public Token removeFirst(Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "removeFirst";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { transaction });

        Link first;
        Token tokenToReturn;
        // Prevent a clash with a backing out transaction.
        synchronized (transaction.internalTransaction) {
            //TODO Having locked on the Internal transaction we should check that this still refers back to the
            //TODO callers external transaction.
            synchronized (this) {
                first = nextLink(null,
                                 transaction,
                                 getTransactionUnlockSequence());
                if (first == null) { // Is there a first element?
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, "via java.util.NoSuchElementException");
                    throw new java.util.NoSuchElementException();
                } // (first == null).

                // Assign the tokenToReturn while we have the list locked so that the ObjectManager 
                // cannot backout this transaction and remove the link under another transaction 
                // causing this thread to see a null Token as a return value.
                tokenToReturn = first.data;

                first.requestDelete(transaction); // Mark for deletion. 

                // If the link is available to all adjust list visible length available.
                // In the case where we delete a link added by the same transaction we have no incremented 
                // the available size yet because we have not yet committed so we would not decrement it.
                if (!first.isLocked())
                    decrementAvailableSize();
            } // synchronized (this).

            // We can release the synchronize lock on the list because we don't care if the
            // link which we are going to delete is changed before we write the log record.
            // Log what we intend to do. Reserve enough spare log space so that the eventual
            // optimistic replace that removes the link from the list is certain to succeed
            // in being written as well. 

            try {
                transaction.delete(first,
                                   logSpaceForDelete());
            } catch (LogFileFullException exception) {
                // No FFDC Code Needed, transaction.delete() has already done this.
                unRemove(first);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, exception);
                throw exception;

                // We should not see ObjectStoreFullException because the ObjectStore already 
                // holds sufficient space to guarantee deletes work.
//      } catch (ObjectStoreFullException exception) {
//        // No FFDC Code Needed, transaction.delete() has already done this.
//        unRemove(first);
//        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//        trace.exit(this, cclass, methodName, exception);
//        throw exception;

            } catch (InvalidStateException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    exception,
                                                    "1:698:1.40");
                unRemove(first);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, exception);
                throw exception;
            } // catch (LogFileFullException exception).
        } // synchronized (transaction.internalTransaction).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { tokenToReturn });
        return tokenToReturn;
    } // removeFirst().

    /**
     * Restore a link so that it is visible again in the list.
     * 
     * @param link to be restored in the list.
     * @throws StateErrorException
     * @throws ObjectManagerException
     */
    private synchronized void unRemove(Link link)
                    throws StateErrorException, ObjectManagerException {
        // Make the link visible again.
        link.setState(Link.nextStateForRequestUnDelete);
        availableHead = head;
        skipToBeDeleted();
        if (link.state == Link.stateAdded) {
            // Adjust list visible length available.
            incrementAvailableSize();
            // Release space we reserved in the store.
            owningToken.objectStore.reserve(-(int) storeSpaceForRemove(), false);
        }
    } // unRemove().

    /**
     * @param transaction governs visibility of elements in the list, may be null.
     * @return true if the list is empty.
     * @throws ObjectManagerException
     */
    public synchronized boolean isEmpty(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "isEmpty",
                        new Object[] { transaction });

        // Use Long.MAX_VALUE as the unlock point, this indicates that any link unlocked ahead
        // of the scan will indicate it is not empty.
        boolean isEmpty = (nextLink(null, transaction, Long.MAX_VALUE) == null);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "isEmpty",
                       new Object[] { new Boolean(isEmpty) });
        return isEmpty;
    } // isEmpty().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#size(com.ibm.ws.objectManager.Transaction)
     */
    public long size(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "size",
                        new Object[] { transaction });

        long listLength; // For return;

        synchronized (this) {
            synchronized (availableSizeLock) {
                listLength = availableSize;
            } // synchronized (availableSizeLock).

            if (transaction != null) {
                Token nextToken = head;
                // Add links ToBeAdded within the transaction.
                while (nextToken != null) {
                    Link nextLink = (Link) nextToken.getManagedObject();
                    if (nextLink.state == Link.stateToBeAdded
                        && nextLink.lockedBy(transaction))
                        listLength++;
                    nextToken = nextLink.next;
                } // while (nextToken != null).
            } // if (transaction != null)
        } // synchronized (this).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "size",
                       "returns listLength=" + listLength + "(long)");
        return listLength;
    } // Of method size().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#size()
     */
    public long size()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "size");

        //TODO Need to include toBeDeleted.
        long listLength = size; // For return;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "size",
                       "returns listLength=" + listLength + "(int)");
        return listLength;
    } // size().  

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#subList(com.ibm.ws.objectManager.List.Entry, com.ibm.ws.objectManager.List.Entry)
     */
    public List subList(List.Entry fromEntry,
                        List.Entry toEntry)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "subList",
                        new Object[] { fromEntry, toEntry });

        SubList subList = new SubList(this,
                                      (Link) fromEntry,
                                      (Link) toEntry);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "subList",
                       new Object[] { subList });
        return subList;
    } // subList().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#entrySet()
     */
    public Set entrySet()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "entrySet");

        EntrySet entrySet = new EntrySet();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "entrySet",
                       new Object[] { entrySet });
        return entrySet;
    } // entrySet().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#iterator()
     */
    public Iterator iterator()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "iterator");

        Iterator iterator = subList(null, null).iterator();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "iterator",
                       new Object[] { iterator });
        return iterator;
    } // iterator().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#listIterator()
     */
    public ListIterator listIterator()
                    throws ObjectManagerException

    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "listIterator");

        ListIterator listIterator = subList(null, null).listIterator();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "listIterator",
                       new Object[] { listIterator });
        return listIterator;
    } // listIterator().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#entryIterator()
     * 
     * @deprecated use entrySet().iterator().
     */
    public Iterator entryIterator()
                    throws ObjectManagerException

    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "entryIterator");

        Iterator entryIterator = subList(null, null).entryIterator();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "entryIterator",
                       new Object[] { entryIterator });
        return entryIterator;
    } // end of entryIterator().

    /**
     * Find the next link after the start link, visible to a transaction, before the unlockPoint.
     * 
     * Caller must be synchronized on LinkedList.
     * 
     * @param start
     *            the link where the search for the next link is to start,
     *            null implies the head of the list.
     * @param transaction which determines the visibility of Links. null indicates that no transaction
     *            visibility is to be checked.
     * @param transactionUnlockPoint which the next link must have been unlocked at
     *            or before.
     * @return Link in the list following the start position, as viewed by the transaction.
     *         Returns null if there is none, or the start is deleted.
     * @throws ObjectManagerException
     */
    protected Link nextLink(Link start,
                            Transaction transaction,
                            long transactionUnlockPoint)
                    throws ObjectManagerException
    {

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "nextLink",
                        new Object[] { start, transaction, new Long(transactionUnlockPoint), head, availableHead });

        Token nextToken = null;
        Link nextLink = null; // The link to return.

        if (start == null) { // Start at the head of the list?
            if (availableHead == null) {
                availableHead = head;
                skipToBeDeleted();
            } // if (availableHead == null). 
            nextToken = availableHead;

        } else if (start.state == Link.stateRemoved
                   || start.state == Link.stateDeleted) {
            // Start in the body of the list, as long as the start point is not already deleted.
            // Deleted links are no longer part of the list.
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "nextLink",
                           "returns null" + " start link is deleted");
            return null;
        } else { // if in body but deleted.
            nextToken = start.next;
        } // if (start == null).

        // Move forward through the ramaining list until we find an element
        // that is visible to this transaction.
        searchForward: while (nextToken != null) {
            nextLink = (Link) (nextToken.getManagedObject());
            if (nextLink.state == Link.stateAdded)
                if (!nextLink.wasLocked(transactionUnlockPoint))
                    break searchForward;

            // Transaction may be null, indicaiting sateToBeAdded links are not eligible.
            if (nextLink.state == Link.stateToBeAdded
                && transaction != null && nextLink.lockedBy(transaction))
                break searchForward;
            nextToken = nextLink.next; // Try the folowing link.
        } // while (nextToken != null ).

        if (nextToken == null) { // At the end of the list?
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "nextLink",
                           "returns null" + " empty list");
            return null;
        } // (nextToken == null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "nextLink",
                       new Object[] { nextLink });
        return nextLink;
    } // nextLink(Transaction).

    /**
     * Find the previous link before the start link, visible to a transaction, before the unlockPoint.
     * 
     * Caller must be synchronized on LinkedList.
     * 
     * @param start
     *            the link where the search for the next link is to start,
     *            null implies the tail of the list.
     * @param transaction which determines the visibility of Links. null indicates that no transaction
     *            visibility is to be checked.
     * @param transactionUnlockPoint which the next link must have been unlocked at
     *            or before.
     * @return Link in the list following the start position, as viewed by the transaction.
     *         Returns null if there is none, or the start is deleted.
     * @throws ObjectManagerException
     */
    protected Link previousLink(Link start,
                                Transaction transaction,
                                long transactionUnlockPoint)
                    throws ObjectManagerException
    {

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "previousLink",
                        new Object[] { start, transaction, new Long(transactionUnlockPoint) });

        Token previousToken = null;
        Link previousLink = null; // The link to return.

        if (start == null) { // Start at the tail of the list?       
            previousToken = tail;

        } else if (start.state == Link.stateRemoved
                   || start.state == Link.stateDeleted) {
            // Start in the body of the list, as long as the start point is not already deleted.
            // Deleted links are no loner part of the list.
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "previousLink",
                           "returns null" + " start link is deleted");
            return null;
        } else { // if in body but deleted.
            previousToken = start.previous;
        } // if (start == null).

        // Move forward through the ramaining list until we find an element
        // that is visible to this transaction.
        searchBackward: while (previousToken != null) {
            previousLink = (Link) (previousToken.getManagedObject());
            if (previousLink.state == Link.stateAdded)
                if (!previousLink.wasLocked(transactionUnlockPoint))
                    break searchBackward;

            // Transaction may be null, indicaiting sateToBeAdded links are not eligible.
            if (previousLink.state == Link.stateToBeAdded
                && transaction != null && previousLink.lockedBy(transaction))
                break searchBackward;
            previousToken = previousLink.next; // Try the folowing link.
        } // while (previousToken != null ).

        if (previousToken == null) { // At the start of the list?
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "nextLink",
                           "returns null" + " empty list");
            return null;
        } // (previousToken == null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "previousLink",
                       new Object[] { previousLink });
        return previousLink;
    } // nextLink(Transaction).

    /**
     * Find the next link after the start link, even if it is part of a transaction.
     * A dirty scan.
     * 
     * Caller must be synchronized on LinkedList.
     * 
     * @param start the link where the search for the next link is to start,
     *            null implies the head of the list.
     * @return Link in the list following the start position.
     *         Returns null if there is none, or the start is deleted.
     * @throws ObjectManagerException
     */
    protected Link nextLink(Link start)
                    throws ObjectManagerException
    {

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "nextLink",
                        new Object[] { start });

        Token nextToken = null;

        if (start == null) { // Start at the head of the list?
            nextToken = head;
        } else { // if in body but deleted.
            nextToken = start.next;
        } // if (start == null).

        Link nextLink = null;
        if (nextToken != null)
            nextLink = (Link) (nextToken.getManagedObject());

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "nextLink",
                       new Object[] { nextLink });
        return nextLink;
    } // nextLink().

    /**
     * Find the previous link before the start link,
     * even if it is part of a transaction.
     * A dirty scan.
     * 
     * Caller must be synchronized on LinkedList.
     * 
     * @param start the link where the search for the previous link is to start,
     *            null implies the tail of the list.
     * @return Link in the list before the start position.
     *         Returns null if there is none, or the start is deleted.
     * @throws ObjectManagerException
     */
    protected Link previousLink(Link start)
                    throws ObjectManagerException
    {

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "previousLink",
                        new Object[] { start });

        Token previousToken = null;

        if (start == null) { // Start at the head of the list?
            previousToken = head;
        } else { // if in body but deleted.
            previousToken = start.next;
        } // if (start == null).

        //  The link to return.
        Link previousLink = null;
        if (previousToken != null)
            previousLink = (Link) (previousToken.getManagedObject());

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "previousLink",
                       new Object[] { previousLink });
        return previousLink;
    } // previousLink().

    /**
     * Find the first available link after the start of the list. This search might return a link
     * locked by a transaction toBeAdded.
     * 
     * Caller must be synchronized on LinkedList.
     * 
     * @return Link which is the first in the list not scheduled for deletion,
     *         or null of there is no such link.
     * @throws ObjectManagerException
     */
    protected Link firstAvailableLink()
                    throws ObjectManagerException
    {

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "firstAvailableLink");

        if (availableHead == null) {
            availableHead = head;
            skipToBeDeleted();
        } // if (availableHead == null). 
        Link nextLink = null; // The link to return.
        if (availableHead != null) // Nothing available.
            nextLink = (Link) availableHead.getManagedObject();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "firstAvailableLink",
                       "returns nextLink=" + nextLink + "(Link)");
        return nextLink;
    } // Of method firstAvailableLink().

    /**
     * Move the availableHead past any ToBeDeleted links.
     * Caller must be synchronized on LinkedList.
     * 
     * @throws ObjectManagerException
     */
    protected void skipToBeDeleted()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "skipToBeDeleted");

        Token nextToken = availableHead;

        // Move forward past any links that are to be deleted. These might be there
        // because other links ahead of them have now been removed, or because a
        // delete of a link backed out possibly reinstating it near the head of the list.
        // See Link.postBackout.
        skipToBeDeleted: while (nextToken != null) {
            Link nextLink = (Link) (nextToken.getManagedObject());
            if (nextLink.state != Link.stateToBeDeleted
                && nextLink.state != Link.stateMustBeDeleted)
                break skipToBeDeleted;
            nextToken = nextLink.next; // Try the folowing link.
        } // while (nextToken != null ).
        availableHead = nextToken; // Remember what we skipped.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "skipToBeDeleted"
                            );
    } // Of method skipToBeDeleted().

    /**
     * Print a dump of the state.
     * 
     * @param printWriter to be written to.
     */
    public synchronized void print(java.io.PrintWriter printWriter)
    {
        printWriter.println("State Dump for:" + cclass.getName()
                            + " head=" + head + "(Token)" + " availableHead=" + availableHead + "(Token)" + " tail=" + tail + "(Token)"
                            + "\n size=" + size + "(long)" + " availableSize=" + availableSize + "(long)"
                            + " maximumAvailableSize=" + maximumAvailableSize
                        );
        printWriter.println();

        printWriter.println("Links in order...");
        try {
            for (Iterator iterator = entrySet().iterator(); iterator.hasNext();) {
                Link link = (Link) iterator.next();
                printWriter.println(link.toString());
            } // for links...
        } catch (ObjectManagerException objectManagerException) {
            // No FFDC Code Needed.
            printWriter.println("Caught objectManagerException=" + objectManagerException);
            objectManagerException.printStackTrace(printWriter);
        } // try...
        printWriter.println();
    } // print().

    /**
     * Check that the head leads to the tail in both the forwards and backwards
     * directions. Check that the number of messages in between matches the size.
     * 
     * @see com.ibm.ws.objectManager.List#validate(java.io.PrintStream)
     */
    public synchronized boolean validate(java.io.PrintStream printStream)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "validate",
                        new Object[] { printStream });

        boolean valid = true;
        Link tailLink = null;
        if (tail != null) {
            tailLink = (Link) tail.getManagedObject();
            if (tailLink.next != null) {
                printStream.println("tail link.next=" + tailLink.next + " not null");
                valid = false;
            }
        }

        // Forward scan.
        long numberOfLinks = 0;
        Token nextToken = head;
        Link nextLink = null;
        while (nextToken != null) {
            numberOfLinks++;
            nextLink = (Link) (nextToken.getManagedObject());
            nextToken = nextLink.next;
        } // while (...

        if (numberOfLinks != size) {
            printStream.println("counted=" + numberOfLinks + " not equal to size=" + size);
            valid = false;
        } // if (numberOfLinks != size).

        if (nextLink != tailLink) {
            printStream.println("final link=" + nextLink + "not equal tail=" + tailLink);
            valid = false;
        } // if (numberOfLinks != size).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "validate",
                       new Object[] { new Boolean(valid) });
        return valid;
    } // validate().

    // --------------------------------------------------------------------------
    // implements java.lang.Cloneable.
    // --------------------------------------------------------------------------

    /**
     * Make a shallow copy of the list, the copy contains all links visble to the transaction.
     * 
     * @param transaction under which the new cloned list is created.
     * @param objectStore where the cloned list is stored.
     * @return Object a shallow copy of the list. This is a new list, the objects in the list are not
     *         themselves cloned.
     * @throws ObjectManagerException
     */
    // TODO make use of collection interface instead of clone.
    public synchronized java.lang.Object clone(Transaction transaction,
                                               ObjectStore objectStore)
                    throws ObjectManagerException

    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "clone",
                        new Object[] { transaction, objectStore });

        LinkedList clonedList = new LinkedList(transaction,
                                               objectStore);
        // Capture the point at which the clone is made.
        long unlockPoint = getTransactionUnlockSequence();
        // Start at the top of the list.
        Link nextLink = nextLink(null,
                                 transaction,
                                 unlockPoint);
        // Build the new list by adding each element from the original list.
        while (nextLink != null) {
            clonedList.add(nextLink.data,
                           transaction);
            nextLink = nextLink(nextLink,
                                transaction,
                                unlockPoint);
        } // While (nextLink != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "clone",
                       new Object[] { clonedList });
        return clonedList;
    } // Of method clone().

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
            // We reserve the largest size even if the ManagedObject has become smaller, this is because
            // there may be a larger version of this Object still about to commit.
            int currentSerializedSize = byteArrayOutputStream.getCount() + owningToken.objectStore.getAddSpaceOverhead();
            if (currentSerializedSize > latestSerializedSize) {
                latestSerializedSizeDelta = currentSerializedSize - latestSerializedSize;
                reservedSpaceInStore = reservedSpaceInStore - latestSerializedSizeDelta;
                latestSerializedSize = currentSerializedSize;
            } // if (currentSerializedSize > latestSerializedSize).
        }
    } // reserveSpaceInStore().

    /**
     * Replace the state of this object with the same object in some other state. Used for to restore
     * the before image if a transaction rolls back or is read from the log during restart.
     * 
     * @param other
     *            is the object this object is to become a clone of.
     */
    public void becomeCloneOf(ManagedObject other)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "becomeCloneOf",
                        "other=" + other + "(ManagedObject)");

        // LinkedLists use an optimistic update methodology and hence perform
        // backout by making
        // updates so we dont make the clone of a before image when backing out.
        if (!backingOut) { // Was transient state corrected in preBackout?
            LinkedList otherList = (LinkedList) other;
            head = otherList.head;
            availableHead = otherList.availableHead;
            tail = otherList.tail;
            size = otherList.size;
            availableSize = otherList.availableSize;
        } // if(!backingOut).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "becomeCloneOf");
    } // End of becomeCloneOf method.

    /**
     * Called just before the list itself is deleted.
     * 
     * @param transaction causing the deleteion of the list.
     * @throws ObjectManagerException
     */
    public synchronized void preDelete(Transaction transaction)
                    throws ObjectManagerException
    {
        final String methodName = "preDelete";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { transaction });

        // TODO Remove this fix, we should not need it now we do not split checkpoint sets.
        // Defect 326031
        // We only need to do this check once so at recovery time we
        // can rely on it having been done when the transaction first 
        // ran.
        // TODO: Can we avoid this check by grouping transaction work into a single checkpoint?
        // TODO: Re tested this on 18/6/07 and the failure still occurs even though we now write
        // TODO: complete transactions in a checkpoint. Here is a scenarion that might cause this,
        // TODO: however the actual test ( performance) does the delete for the entry and list as two
        //       separate serial tracactions!
        // 
        //   Application Thread          Checkpoint Helper      LogRecords   
        //  
        //   Delete Entry                                       Delete Entry 
        //   Delete List                                        Delete List
        //                               SetRequiresCheckpoint
        // 
        //                               Checkpoint Start       Checkpoint Start. 
        //                                                      Checkpoint Entry(Deleted)
        //                                                      Checkpoint List(Deleted)
        //
        //   Commit                                             Commit
        //   Delete Entry/List from Store
        //                               Flush ObjectStore             
        //                               Checkpoint End         Checkpoint End.  
        // 
        //   
        //   Crash 
        // 
        //   Recover List in Deleted state, 
        //   Entry not yet recovered in deleted state.

        if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog) {
            // The list must be empty, apart from links this transaction will delete.
            Token nextToken = head;
            while (nextToken != null) {
                Link nextLink = (Link) nextToken.getManagedObject();
                if (nextLink.willBeDeleted(transaction))
                    nextToken = nextLink.next;
                else {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass,
                                   methodName,
                                   "via CollentionNotEmptyException"
                                                   + "size=" + size + "(long)"
                                                   + " nextLink.state=" + nextLink.state + "(int) " + Link.stateNames[nextLink.state]
                                                   + "\n nextLink.getTransactionLock()=" + nextLink.getTransactionLock() + "(transactionLock)"
                                        );

                    throw new CollectionNotEmptyException(this, size, transaction);
                    //System.out.println("LinkedList.preDelete nextLink.state ="+nextLink.state+" "+Link.stateNames[nextLink.state]
                    //                  +"\n nextLink.getTransactionLock()="+nextLink.getTransactionLock() 
                    //                  );
                    //break;
                } // if (  nextLink.state == Link.stateToBeDeleted...
            } // while (nextToken != null).
        } // if not recovering...

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
     * 
     * @throws ObjectManagerException
     */
    LinkedList()
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "<init>");

        managedObjectsToAdd = new java.util.ArrayList(1);
        managedObjectsToReplace = new java.util.ArrayList(3);
        tokensToNotify = new java.util.ArrayList(1);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "<init>");
    } // end of Constructor.

    private static final byte simpleSerialVersion = 0;

    // The serialized size of this.
    protected static long maximumSerializedSize()
    {
        return 1 // Version.
               + ManagedObject.maximumSerializedSize() + 1 // Flag.
               + 2 * Link.maximumSerializedSize() + 8 // Size.
        ;
    }

    /*
     * (non-Javadoc)
     * 
     * @see int SimplifiedSerialization.getSignature()
     */
    public int getSignature()
    {
        return signature_LinkedList;
    } // End of getSignature.

    /*
     * (non-Javadoc)
     * 
     * @see SimplifiedSerialization.writeObject(java.io.DataInputStream)
     */
    public void writeObject(java.io.DataOutputStream dataOutputStream)
                    throws ObjectManagerException
    {
        final String methodName = "writeObject";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { dataOutputStream });

        try {
            dataOutputStream.writeByte(simpleSerialVersion);
            super.writeObject(dataOutputStream);

            if (head == null) {
                dataOutputStream.writeByte(0);
            } else {
                dataOutputStream.writeByte(1);
                head.writeObject(dataOutputStream); // The first element in the list.
                tail.writeObject(dataOutputStream); // The final element in the list.
            } // if (head == null).
            dataOutputStream.writeLong(size);
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:1565:1.40");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           methodName,
                           "via PermanentIOException");
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        // We cannot check size as we are subclassed.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       methodName);
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
                            new Object[] { new Byte(version) });
            super.readObject(dataInputStream,
                             objectManagerState);
            if (dataInputStream.readByte() == 1) {
                // The first element in the list.
                head = Token.restore(dataInputStream,
                                     objectManagerState);
                //      The final element in the list.
                tail = Token.restore(dataInputStream,
                                     objectManagerState);
            } // if (dataInputStream.readByte() == 1).
            size = dataInputStream.readLong();
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:1620:1.40");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { exception });
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        // We cannot set the available head during recovery because the links may get deleted before
        // recovery is complete, instead force a scan when we use availableHead for the first time.
        availableHead = null;
        availableSize = size; // All links are initially available.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // readObject().

    // --------------------------------------------------------------------------
    // implements java.io.Serializable
    // --------------------------------------------------------------------------

    /**
     * Customized deserialization.
     * 
     * @param objectInputStream containing the serialized form of the LinkedList.
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream objectInputStream)
                    throws java.io.IOException
                    , java.lang.ClassNotFoundException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "readObject",
                        "objectInputStream=" + objectInputStream + "(java.io.ObjectInputStream)");

        objectInputStream.defaultReadObject();
        // We cannot set the available head during recovery because the links may get deleted before
        // recovery is complete, instead force a scan when we use availableHead for the first time.    
        availableHead = null;
        availableSize = size; // All links are initially available.
        availableSizeLock = new Object();
        managedObjectsToAdd = new java.util.ArrayList(1);
        managedObjectsToReplace = new java.util.ArrayList(3);
        tokensToNotify = new java.util.ArrayList(1);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "readObject");
    } // readObject().

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
        return new String("LinkedList"
                          + "(size=" + size + " availableSize=" + availableSize + ")"
                          + " " + super.toString());
    } // toString().

    // ----------------------------------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------------------------------

    /**
     * 
     * @author Andrew_Banks
     * 
     *         A subset view of the encompasing list deliniated by a head and tail,
     *         both of which must be in the encompassing list for the sublist to be valid.
     */
    static class SubList
                    extends AbstractListView
    {
        LinkedList list; // The outer list.
        Link head; // First element before the start of the list.
        Link tail; // Final element after the end of the list.

        /**
         * Constructor creates a subList.
         * 
         * @param list from which the subList is taken.
         * @param head the Link before the head of the new subList, null implies the head of the list.
         * @param tail the Link after the tail of the new subList, null implies the tail of the list.
         * @throws ObjectManagerException
         * 
         */
        private SubList(LinkedList list,
                        Link head,
                        Link tail)
            throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "<init>",
                            new Object[] { list, head, tail }
                                );

            this.list = list;
            this.head = head;
            this.tail = tail;

            if (head != null) {
                if (head.list != list
                    || head.state == Link.stateRemoved
                    || head.state == Link.stateDeleted) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass,
                                   "subList"
                                        );
                    throw new SubListEntryNotInListException(this, head);
                }
            } // if (head != null).

            if (tail != null) {
                if (tail.list != list
                    || tail.state == Link.stateRemoved
                    || tail.state == Link.stateDeleted) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass,
                                   "subList"
                                        );
                    throw new SubListEntryNotInListException(this, tail);
                }
            } // if (tail != null).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "<init>");
        } // SubList().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.Collection#iterator()
         */
        public Iterator iterator()
                        throws ObjectManagerException
        {
            return new SubListIterator(SubListIterator.VALUES);
        } // iterator().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.List#listIterator()
         */
        public ListIterator listIterator()
                        throws ObjectManagerException
        {
            return new SubListIterator(SubListIterator.VALUES);
        } // listIterator().

        /**
         * @return Iterator over Entries in the list.
         * @throws ObjectManagerException
         */
        public Iterator entryIterator()
                        throws ObjectManagerException
        {
            return new SubListIterator(SubListIterator.ENTRIES);
        } // entryIterator().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.List#addEntry(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
         */
        public List.Entry addEntry(Token token,
                                   Transaction transaction)
                        throws ObjectManagerException
        {
            synchronized (transaction.internalTransaction) {
                synchronized (list) {
                    Link newLink = list.insert(token,
                                               tail,
                                               transaction);
                    return (Entry) newLink;
                } // synchronized (LinkedList.this).
            } // synchronized (transaction.internalTransaction).
        } // addEntry().

        public List subList(Entry fromEntry,
                            Entry toEntry)
                        throws ObjectManagerException
        {
            return new SubList(list, (Link) fromEntry, (Link) toEntry);
        } // subList().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.List#entrySet()
         */
        public Set entrySet()
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "entrySet");

            EntrySet entrySet = new EntrySet();

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "entrySet",
                           new Object[] { entrySet });
            return entrySet;
        } // entrySet().

        /**
         * Find the next link after the start link, even if it is part of a transaction.
         * A dirty scan.
         * 
         * Caller must be synchronized on LinkedList.
         * 
         * @param start
         *            the link where the search for the next link is to start,
         *            null implies the head of the list.
         * @return Link in the list following the start position.
         *         Returns null if there is none, or the start is deleted.
         * @throws ObjectManagerException
         */
        protected Link nextLink(Link start)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "nextLink",
                            new Object[] { start });

            Token nextToken = null;

            if (start == null) { // Start at the head of the list?
                if (head == null)
                    nextToken = list.head;
                else
                    nextToken = head.next;
            } else { // if in body but deleted.
                nextToken = start.next;
            } // if (start == null).

            Link nextLink = null;
            if (nextToken != null)
                nextLink = (Link) (nextToken.getManagedObject());

            if (nextLink == tail) {
                nextLink = null;
            }

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "nextLink",
                           new Object[] { nextLink });
            return nextLink;
        } // nextLink().

        /**
         * Find the next link after the start link, visible to a transaction, before the unlockPoint.
         * 
         * Caller must be synchronized on LinkedList.
         * 
         * @param start
         *            the link where the search for the next link is to start,
         *            null implies the head of the list.
         * @param transaction which determines the visibility of Links. null indicates that no transaction
         *            visibility is to be checked.
         * @param transactionUnlockPoint which the next link must have been unlocked at
         *            or before.
         * @return Link in the list following the start position, as viewed by the transaction.
         *         Returns null if there is none, or the start is deleted.
         * @throws ObjectManagerException
         */
        protected Link nextLink(Link start,
                                Transaction transaction,
                                long transactionUnlockPoint)
                        throws ObjectManagerException
        {

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "nextLink",
                            new Object[] { start, transaction, new Long(transactionUnlockPoint) });

            Token nextToken = null;
            Link nextLink = null; // The link to return.

            if (start == null) { // Start at the head of the subList?
                if (head == null) { // Start at the Head  of the overall list?
                    if (list.availableHead == null) {
                        list.availableHead = list.head;
                        list.skipToBeDeleted();
                    } // if (availableHead == null). 
                    nextToken = list.availableHead;
                } else {
                    nextToken = head.next;
                } // if (head == null). 

            } else if (start.state == Link.stateRemoved
                       || start.state == Link.stateDeleted) {
                // Start in the body of the list, as long as the start point is not already deleted.
                // Deleted links are no longer part of the list.
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "nextLink",
                               "returns null" + " start link is deleted");
                return null;
            } else { // if in body but deleted.
                nextToken = start.next;
            } // if (start == null).

            // Move forward through the remaining list until we find an element
            // that is visible to this transaction.
            searchForward: while (nextToken != null) {
                nextLink = (Link) (nextToken.getManagedObject());
                if (nextLink == tail) {
                    nextToken = null;
                    break;
                }

                if (nextLink.state == Link.stateAdded)
                    if (!nextLink.wasLocked(transactionUnlockPoint))
                        break searchForward;

                // Transaction may be null, indicaiting sateToBeAdded links are not eligible.
                if (nextLink.state == Link.stateToBeAdded
                    && transaction != null && nextLink.lockedBy(transaction))
                    break searchForward;
                nextToken = nextLink.next; // Try the folowing link.
            } // while (nextToken != null ).

            if (nextToken == null) { // At the end of the list?
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "nextLink",
                               "returns null" + " empty list");
                return null;
            } // (nextToken == null).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "nextLink",
                           new Object[] { nextLink });
            return nextLink;
        } // nextLink(Transaction).

        /**
         * A cursor for for enumerating or iterating over a linked list, similar to the
         * java.util.ListIterator interface extended for transactions.
         */
        class SubListIterator
                        implements ListIterator
        {
            // LinkedListIterator is locked first if necessary, to protect the cursor. 
            // Any transaction is locked second, if necessary.
            // If the list must be traversed or altered it is locked third.

            // Type of Iterator.
            static final int VALUES = 1;
            static final int ENTRIES = 2;
            private int type;
            // Direction of search.
            // From head to tail.
            static final int FORWARD = 0;
            // From tail to head.
            static final int BACKWARD = 1;

            // The current position in the list.
            Link currentEntry = null;
            Link nextEntry = null;
            // True if the cursor has moved off the end of the list.
            boolean beyondEndOfList = false;

            /**
             * Constructs a new cursor for an existing SubList.
             * 
             * @param type of iterator, returns Values or Entries.
             * @throws ObjectManagerException
             */
            SubListIterator(int type)
                throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "<init>,",
                                new Object[] { new Integer(type) });

                this.type = type;

                if (head != null) {
                    currentEntry = head;
                    if (head.state == Link.stateRemoved
                        || head.state == Link.stateDeleted) {
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this,
                                       cclass,
                                       "<intit>");
                        throw new SubListEntryNotInListException(SubList.this, head);
                    }
                } // if (head != null).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "<init>");
            } // SubListIterator().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.Iterator#hasNext(com.ibm.ws.objectManager.Transaction)
             */
            public synchronized boolean hasNext(Transaction transaction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "hasNext",
                                new Object[] { transaction });

                boolean returnValue = false;
                synchronized (list) {
                    if (!beyondEndOfList) {
                        Entry nextAvailableEntry = nextAvailable(transaction, FORWARD);
                        if (nextAvailableEntry != null)
                            returnValue = true;
                    } // if (!beyondEndOfList).
                } // synchronized (list).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "hasNext",
                               new Object[] { new Boolean(returnValue) });
                return returnValue;
            } // hasNext(Transaction).

            /**
             * Determine if the list has dirty elements following the cursor.
             * 
             * @return true iff there is an element folowing the cursor.
             * @throws ObjectManagerException
             */
            public synchronized boolean hasNext()
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "hasNext",
                                new Object[] { currentEntry });

                boolean returnValue = false;
                synchronized (list) {
                    if (!beyondEndOfList) {
                        Entry nextAvailableEntry = nextAvailable(FORWARD);
                        if (nextAvailableEntry != null)
                            returnValue = true;
                    } // if (!beyondEndOfList).
                } // synchronized (list).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "hasNext",
                               new Object[] { new Boolean(returnValue) });
                return returnValue;
            } // hasNext().

            /**
             * @param transaction which determines the visibility of elements.
             * @return Object the next Object in the list, after advancing the cursor position.
             * @throws ObjectManagerException
             * @throws java.util.NoSuchElementException if already at the end of the visible list list,
             *             or the current element is deleted.
             */
            public synchronized Object next(Transaction transaction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "next",
                                new Object[] { transaction,
                                              currentEntry });

                Object currentObject = null;
                synchronized (list) {
                    // Already after the tail of the list?
                    if (!beyondEndOfList) {
                        currentEntry = nextAvailable(transaction, FORWARD);
                        nextEntry = null;

                        if (currentEntry == null)
                            beyondEndOfList = true;
                    } // if (!beyondEndOfTree).

                    if (beyondEndOfList) {
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this,
                                       cclass,
                                       "next",
                                       "via java.util.NoSuchElementException");
                        throw new java.util.NoSuchElementException();
                    } // if (beyondEndOfList).

                    switch (type) {
                        case VALUES:
                            currentObject = currentEntry.data;
                            break;

                        case ENTRIES:
                            currentObject = currentEntry;
                            break;
                    } // switch (type).
                } // synchronized (list).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "next",
                               new Object[] { currentObject });
                return currentObject;
            } // next(Transaction)

            /**
             * Determine the next entry available to the transaction after the current cursor.
             * Caller must be synchronized on LinkedList.this and beyondEndOfList must be false.
             * 
             * @param transaction controling visibility.
             * @param direction of search FORWARD or BACKWARD.
             * @return Link found, or null if there is none.
             * 
             * @throws ObjectManagerException
             * @throws java.util.ConcurrentModificationException if the cursor Entry has been deleted
             *             other than by invoking remove() from this iterator.
             */
            Link nextAvailable(Transaction transaction,
                               int direction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "nextAvailable",
                                new Object[] { currentEntry, nextEntry, transaction, new Integer(direction) }
                                    );

                Link nextAvailableEntry;
                long unlockPoint = list.getTransactionUnlockSequence();
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

                    } else if (direction == FORWARD) {
                        nextAvailableEntry = nextLink(nextEntry,
                                                      transaction,
                                                      unlockPoint);
                    } else {
                        nextAvailableEntry = list.previousLink(nextEntry,
                                                               transaction,
                                                               unlockPoint);
                    } // if ( (nextEntry.state..

                } else if (currentEntry == null) {
                    // At start of List.
                    nextAvailableEntry = nextLink(null,
                                                  transaction,
                                                  unlockPoint);

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
                        nextAvailableEntry = nextLink(currentEntry,
                                                      transaction,
                                                      unlockPoint);
                    } // if (currentEntry.state...

                } // (nextEntry...

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "nextAvailable"
                               , new Object[] { nextAvailableEntry }
                                    );
                return nextAvailableEntry;
            } // nextAvailable().

            /**
             * Dirty iterator advance.
             * 
             * @return Object the next Object in the list, after advancing the cursor position.
             * @throws ObjectManagerException
             * @exception java.util.NoSuchElementException
             *                if already at the end of the visible list list.
             */
            public synchronized Object next()
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "next",
                                new Object[] { currentEntry });

                Object currentObject = null;
                synchronized (list) {
                    if (!beyondEndOfList) {
                        currentEntry = nextAvailable(FORWARD);
                        nextEntry = null;

                        if (currentEntry == null)
                            beyondEndOfList = true;
                    } // if (!beyondEndOfList).

                    if (beyondEndOfList) {
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this,
                                       cclass,
                                       "next",
                                       "via java.util.NoSuchElementException");
                        throw new java.util.NoSuchElementException();
                    } // if (beyondEndOfList).

                    switch (type) {
                        case VALUES:
                            currentObject = currentEntry.data;
                            break;

                        case ENTRIES:
                            currentObject = (List.Entry) currentEntry;
                            break;
                    } // switch (type).
                } // synchronized (list).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "next",
                               new Object[] { currentObject });
                return currentObject;
            } // method next().

            /**
             * Determine the next Entry available after the current cursor.
             * The caller must be synchronised on LinkedList.this and beyondEndOfList must be false.
             * 
             * @param direction of search FORWARD or BACKWARD.
             * @return Entry found, or null if there is none.
             * 
             * @throws ObjectManagerException
             * @throws java.util.ConcurrentModificationException if the cursor Entry has been deleted
             *             other than by invoking remove() from this iterator.
             */
            Link nextAvailable(int direction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "nextAvailable",
                                new Object[] { new Integer(direction),
                                              currentEntry,
                                              nextEntry });
                Link nextAvailableEntry;
                if (nextEntry != null) {
                    if (nextEntry.state == Entry.stateRemoved
                        || nextEntry.state == Entry.stateDeleted) {
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this,
                                       cclass,
                                       "nextAvailable",
                                       "via java.util.ConcurrentModificationException");
                        throw new java.util.ConcurrentModificationException();

                    } else if (direction == FORWARD) {
                        nextAvailableEntry = nextLink(nextEntry);
                    } else {
                        nextAvailableEntry = list.previousLink(nextEntry);
                    } // if ( (nextEntry.state...

                } else if (currentEntry == null) {
                    if (direction == FORWARD)
                        nextAvailableEntry = nextLink(null);
                    else
                        nextAvailableEntry = list.previousLink(null);

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
                        nextAvailableEntry = nextLink(currentEntry);
                    } // if (currentEntry.state...
                } // if (nextEntry...

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "nextAvailable",
                               new Object[] { nextAvailableEntry });
                return nextAvailableEntry;
            } // nextAvailable().

            /**
             * Remove the element under the cursor, and advance the cursor to the next visible link. actual
             * removal from the list is defered until just before commit in order to reduce lock contention.
             * 
             * @param transaction controls commitment of the removal.
             * @return the removed Object
             * @throws ObjectManagerException
             * @exception java.lang.IllegalStateException if the cursor is before the beginning
             *                or after the end of the list.
             * @exception IllegalStateException if the entry has already been removed.
             */
            public synchronized Object remove(Transaction transaction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "remove",
                                new Object[] { transaction });

                if (currentEntry == null
                    || beyondEndOfList) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass
                                   , "remove"
                                   , "via IllegalStateException"
                                        );
                    throw new java.lang.IllegalStateException();
                } // if (currentEntry == null)...

                synchronized (transaction.internalTransaction) {
                    synchronized (list) {
                        currentEntry.remove(transaction);
                        nextEntry = nextLink(currentEntry,
                                             transaction,
                                             list.getTransactionUnlockSequence());
                        if (nextEntry == null) {
                            beyondEndOfList = true;
                        } // if (currentEntry == null).
                    } // synchronized (list).
                } // synchronized (transaction.internalTransaction).

                Object returnObject = null;
                switch (type) {
                    case VALUES:
                        returnObject = currentEntry.data;
                        break;

                    case ENTRIES:
                        returnObject = (List.Entry) currentEntry;
                        break;
                } // switch (type).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "remove",
                               new Object[] { returnObject });
                return returnObject;
            } // remove().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.ListIterator#hasPrevious(com.ibm.ws.objectManager.Transaction)
             */
            public boolean hasPrevious(Transaction transaction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "hasPrevious",
                                new Object[] { transaction });

                boolean returnValue = false;
                synchronized (list) {
                    if (beyondEndOfList || currentEntry != null) {
                        Entry nextAvailableEntry = nextAvailable(transaction, BACKWARD);
                        if (nextAvailableEntry != null)
                            returnValue = true;
                    } // if (beyondEndOfList...
                } // synchronized (list).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "hasPrevious",
                               new Object[] { new Boolean(returnValue) });
                return returnValue;
            } // hasPrevious().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.ListIterator#hasPrevious()
             */
            public synchronized boolean hasPrevious()
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "hasPrevious",
                                new Object[] { currentEntry });

                boolean returnValue = false;
                synchronized (list) {
                    if (beyondEndOfList || currentEntry != null) {
                        Entry nextAvailableEntry = nextAvailable(BACKWARD);
                        if (nextAvailableEntry != null)
                            returnValue = true;
                    } // if (beyondEndOfList...
                } // synchronized (list).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "hasPrevious",
                               new Object[] { new Boolean(returnValue) });
                return returnValue;
            } // hasPrevious().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.ListIterator#previous(com.ibm.ws.objectManager.Transaction)
             */
            public synchronized Object previous(Transaction transaction)
            {
                // TODO Auto-generated method stub
                return null;
            } // previous().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.ListIterator#previous()
             */
            public synchronized Object previous()
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "previous",
                                new Object[] { currentEntry });

                Object currentObject = null;
                synchronized (list) {
                    if (beyondEndOfList || currentEntry != null) {
                        currentEntry = nextAvailable(BACKWARD);
                        nextEntry = null;

                        if (currentEntry == null)
                            beyondEndOfList = true;
                    } // if (beyondEndOfList...

                    if (!beyondEndOfList && currentEntry == null) {
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this,
                                       cclass,
                                       "next",
                                       "via java.util.NoSuchElementException");
                        throw new java.util.NoSuchElementException();
                    } // if (!beyondEndOfList...

                    switch (type) {
                        case VALUES:
                            currentObject = currentEntry.data;
                            break;

                        case ENTRIES:
                            currentObject = (List.Entry) currentEntry;
                            break;
                    } // switch (type).
                } // synchronized (list).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "next",
                               new Object[] { currentObject });
                return currentObject;
            } // previous().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.ListIterator#nextIndex(com.ibm.ws.objectManager.Transaction)
             */
            public long nextIndex(Transaction transaction)
            {
                throw new UnsupportedOperationException();
            } // nextIndex().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.ListIterator#previousIndex(com.ibm.ws.objectManager.Transaction)
             */
            public long previousIndex(Transaction transaction)
            {
                throw new UnsupportedOperationException();
            } // previousIndex().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.ListIterator#set(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
             */
            public void set(Token newToken,
                            Transaction transaction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "set",
                                new Object[] { newToken, transaction });

                if (currentEntry == null
                    || beyondEndOfList) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass
                                   , "set"
                                   , "via IllegalStateException"
                                        );
                    throw new java.lang.IllegalStateException();
                } // if (currentEntry == null)...

                currentEntry.setValue(newToken, transaction);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "set");
            } // set().

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.ListIterator#add(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
             */
            public synchronized void add(Token newToken,
                                         Transaction transaction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "add",
                                new Object[] { newToken,
                                              transaction });

                // Take a lock on transaction then the list itself, this protects the structure of the list
                // and prevents deadlock with threads backing out the transaction. During backout the list
                // lock is traven in the preBackout call back after the transaction lock has been taken.
                synchronized (transaction.internalTransaction) {
                    synchronized (list) {
                        // Cursor must still be in the list.
                        if (beyondEndOfList) {
                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this, cclass
                                           , "remove"
                                           , "via IllegalStateException"
                                                );
                            throw new IllegalStateException();
                        } // if (beyondEndOfList).

                        //  Was the cursor deleted.
                        if (currentEntry.state == Entry.stateRemoved
                            || currentEntry.state == Entry.stateDeleted) {
                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this,
                                           cclass,
                                           "add",
                                           "via java.util.ConcurrentModificationException");
                            throw new java.util.ConcurrentModificationException();
                        } // (currentEntry.state...

                        // Move the cursor forward to the inserted link.
                        currentEntry = list.insert(newToken,
                                                   currentEntry,
                                                   transaction);
                    } // synchronized (list).
                } // synchronized (transaction.internalTransaction).

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "add");
            } // add().
        } // class SubListIterator. 

        /**
         * The Set of Entries in this list.
         */
        class EntrySet
                        extends AbstractSetView
        {
            public Iterator iterator()
                            throws ObjectManagerException
            {
                return entryIterator();
            } // iterator().
        } // class EntrySet.
    } // class SubList.

    /**
     * The Set of Entries in this list.
     */
    class EntrySet
                    extends AbstractSetView
    {
        public Iterator iterator()
                        throws ObjectManagerException
        {
            LinkedList.SubList subList = (LinkedList.SubList) LinkedList.this.subList(null, null);
            return subList.entryIterator();
        }

        public long size(Transaction transaction)
                        throws ObjectManagerException
        {
            return LinkedList.this.size(transaction);
        }

        public long size()
                        throws ObjectManagerException
        {
            return LinkedList.this.size();
        }
    } // class EntrySet.

    /**
     * The Link in the chain holding the data the next and the previous Links in the list.
     * 
     * @version 1.01 15 Feb 1996
     * @author Andrew Banks
     */
    static class Link
                    extends ManagedObject
                    implements List.Entry, SimplifiedSerialization
    {
        private static final Class cclass = LinkedList.Link.class;
        private static final long serialVersionUID = 4680368684147217734L;

        /*---------------------- Define the state machine (begin) ----------------------*/
        // Tracks the lifecycle of the Entry.
        static final int stateError = List.Entry.stateError; // A state error has occured.
        static final int stateConstructed = List.Entry.stateConstructed; // Not yet part of a list.
        static final int stateToBeAdded = List.Entry.stateToBeAdded; // Part of the list but not available to other transactions.
        static final int stateAdded = List.Entry.stateAdded; // Added.
        static final int stateNotAdded = List.Entry.stateNotAdded; // Added, but removed because of backout.
        static final int stateToBeDeleted = List.Entry.stateToBeDeleted; // Will be removed from the list.
        static final int stateMustBeDeleted = List.Entry.stateMustBeDeleted; // Link will be deleted regardless of the transaction outcome.
        static final int stateRemoved = List.Entry.stateRemoved; // Removed from the list.
        static final int stateDeleted = List.Entry.stateDeleted; // Removed from the list and deleted.

        // The names of the states for diagnostic purposes.
        static final String stateNames[] = { "Error",
                                            "Constructed",
                                            "ToBeAdded",
                                            "Added",
                                            "NotAdded",
                                            "ToBeDeleted",
                                            "MustBeDeleted",
                                            "Removed",
                                            "Deleted" };

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

        // What happens when this Link is marked for deletion from the list.
        static final int nextStateForRequestDelete[] = { stateError,
                                                        stateError,
                                                        stateMustBeDeleted, // Add then Delete in the same transaction.
                                                        stateToBeDeleted,
                                                        stateError,
                                                        stateToBeDeleted, // If deleted a second time from a checkpoint.
                                                        stateMustBeDeleted, // If deleted a second time from a checkpoint.
                                                        stateError,
                                                        stateError };

        // What happens when this Link is marked for deletion but the
        // transaction.delete() update fails, perhapse due to log full.
        // This does not handle reversal of a delete during a checkpoint, and completeley 
        // reverse nextStateForRequestDelete, which should not occur.
        static final int nextStateForRequestUnDelete[] = { stateError,
                                                          stateError,
                                                          stateError,
                                                          stateError,
                                                          stateError,
                                                          stateAdded,
                                                          stateToBeAdded,
                                                          stateError,
                                                          stateError };

        // What happens when this Link is removed from the list structure.
        static final int nextStateForRemove[] = { stateError,
                                                 stateError,
                                                 stateNotAdded,
                                                 stateRemoved, // OptimisticReplace recovered, before TransactionCheckpoint.
                                                 stateError,
                                                 stateRemoved,
                                                 stateRemoved,
                                                 stateError,
                                                 stateError };

        // What happens when this Link is commited in the list.
        static final int nextStateForCommit[] = { stateError,
                                                 stateError,
                                                 stateAdded,
                                                 stateError,
                                                 stateError,
                                                 stateError,
                                                 stateError,
                                                 stateDeleted,
                                                 stateError };

        // What happens when this Link is rolled back from the list.
        static final int nextStateForBackout[] = { stateError,
                                                  stateError,
                                                  stateError,
                                                  stateError,
                                                  stateDeleted,
                                                  stateAdded,
                                                  stateError,
                                                  stateDeleted,
                                                  stateError };

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

        transient volatile int state; // The current state of the Link.
        // The previous state, not refereneced but will appear in a dump.
        private transient int previousState;
        /*---------------------- Define the state machine (end) ------------------------*/

        private Token listToken; // The list responsible for this link.
        protected transient LinkedList list;
        protected Token data;
        protected Token next; // The folowing link in the list.
        protected Token previous; // The link in the list before this one.
        // The new data when an update to the Entry commits.
        transient Token uncommitedData;

        /**
         * Creates a link in the chain.
         * 
         * @param list that this link belongs to.
         * @param data the payloadthat this link refers to.
         * @param previous link in the chain, or null if none.
         * @param next Link in the chain, or null if none.
         * @param transaction which controls the addition of the new Link.
         * @exception ObjectManagerException
         */
        Link(LinkedList list,
             Token data,
             Token previous,
             Token next,
             Transaction transaction)
            throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "<init>",
                            new Object[] { list, data, previous, next, transaction });

            this.listToken = list.owningToken;
            this.list = list;
            this.data = data;
            this.previous = previous;
            this.next = next;
            this.state = stateConstructed;
            previousState = -1; // No previous state.

            // Make the Link a ManagedObject in the same ObjectStore as the LinkdList.
            listToken.getObjectStore()
                            .allocate(this);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "<init>");
        } // end of Constructor.

        /**
         * Mark this Link for later deletion from the list. Although we delete the link from the list
         * now, no actual deletion takes place until we commit. If we back out, the link is left the way
         * it was. Notice we have not rechained the list or updated the next and previous pointers so
         * the objectStore and in memory copy are both still in the list. The link will be removed from
         * the list when we know the outcome of the transaction is commit.
         * 
         * Caller must be synchronized on list.
         * 
         * @param transaction
         *            the unit of work which will complete the deletion.
         * @throws ObjectManagerException
         */
        protected void requestDelete(Transaction transaction)
                        throws ObjectManagerException {
            final String methodName = "requestDelete";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction });

            testState(nextStateForRequestDelete);

            // Reserve space in the store, if not available, we fail here.
            // Only reserve space if the entry was added by another, already committed, transaction.
            if (state == stateAdded)
                owningToken.objectStore.reserve((int) list.storeSpaceForRemove(), false);

            // TODO Does not need transaction as a parameter.
            setState(nextStateForRequestDelete); // Make the link invisible.

            if (owningToken == list.availableHead) { // Move the available head on?
                list.skipToBeDeleted();
            } // if (owningToken == list.availableHead).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // requestDelete().

        /**
         * Indicates if the transacton will complete deleteion of this Link.
         * Caller must be synchronized on List.
         * 
         * @param transaction the unit of work which will complete the deletion.
         * @return true if the Link will be deleted by the transaction.
         */
        final boolean willBeDeleted(Transaction transaction)
        {
            return ((state == stateToBeDeleted || state == stateMustBeDeleted)
            && lockedBy(transaction));
        } // willBeDeleted().

        /**
         * Remove a link from the list based on a previous requestDelete of it then harden the new state
         * of the list. We have already logged the deletion of the link so we now need to rechain those
         * links currently before and after it. This may be redriven at recovery so we need to detect if
         * the link has already been removed from the list so that we dont disrupt the pervious and next
         * links in error when we are already removed from the list.
         * 
         * @param transaction unit of work which will complete the deletion.
         * @throws ObjectManagerException
         */
        private void removeFromList(Transaction transaction)
                        throws ObjectManagerException {
            final String methodName = "removeFromList";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction });

            // We are going to mess with the list structure so lock the LinkedList to
            // this thread alone.
            synchronized (list) {
                // list.managedObjectsToAdd.clear(); // Not used.
                list.managedObjectsToReplace.clear(); // Reset from last time.
                list.tokensToNotify.clear(); // Reset from last time.
                list.reservedSpaceInStore = (int) list.storeSpaceForRemove();
                // Detach this Link from the list.
                if (next == null) { // Are we the tail of list?
                    list.tail = previous;
                } else { // Not tail of the list.
                    // Join up the backwards Link.
                    Link nextLink = (Link) next.getManagedObject();
                    nextLink.previous = previous;
                    list.managedObjectsToReplace.add(nextLink);
                } // if at tail of list.

                if (previous == null) { // Are we the head of list?
                    list.head = next;
                } else { // Not head of the list.
                    // Join up the forwards Link.
                    Link previousLink = (Link) previous.getManagedObject();
                    previousLink.next = next;
                    list.managedObjectsToReplace.add(previousLink);
                } // if at head of list.

                list.decrementSize(); // Adjust list length, assuming we commit.
                list.managedObjectsToReplace.add(list);
                list.tokensToNotify.add(owningToken); // Will drive state change to stateRemoved.

                // Harden the updates. Release some of the space we reserved earlier,
                // when we deleted the link, or, if we are backing out, when we added it.
                // Releasing the reserved space ensures that the replace will succeed.
                // If the optimistic replace gets forced to disk after we have started to 
                // commit or backout the commit or backout is completed at restart. 
                transaction.optimisticReplace(null,
                                              list.managedObjectsToReplace,
                                              null, // No tokens to delete.
                                              list.tokensToNotify,
                                              -logSpaceForDelete());
                // Release any surplus space we reserved in the store.
                // During recovery we don't do this because we will not have reserved the space anyway.
                if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog)
                    owningToken.objectStore.reserve(-(int) list.reservedSpaceInStore, false);
            } // synchronized (list).

            // Help the garbage collector by preventing it from running long chains.
            listToken = null;
            data = null;
            next = null;
            previous = null;

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           methodName);
        } // removeFromList().

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
                    list.reservedSpaceInStore = list.reservedSpaceInStore - latestSerializedSizeDelta;
                    latestSerializedSize = currentSerializedSize;
                } // if (currentSerializedSize > latestSerializedSize).
            }
        } // reserveSpaceInStore().

        /**
         * Replace the state of this object with the same object in some other state. Used for to
         * restore the before image if a transaction rolls back or is read from the log during restart.
         * 
         * @param other
         *            is the object this object is to become a clone of.
         */
        public void becomeCloneOf(ManagedObject other)
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "becomeCloneOf",
                            "other=" + other + "(ManagedObject)");

            // Links in a list use an optimistic update methodology and hence perform
            // backout by making
            // updates so we dont make the clone of a before image when backing out.
            if (!backingOut) { // Was transient state corrected in preBackout?
                Link otherLink = (Link) other;
                listToken = otherLink.listToken;
                list = otherLink.list;
                data = otherLink.data;
                next = otherLink.next;
                previous = otherLink.previous;
                // state is maintained by add() and delete() etc. methods.
            } // if(!backingOut).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "becomeCloneOf");
        } // End of becomeCloneOf method.

        /*
         * Modify the behaviour of the ManagedObject add method to perform extra work which we will also
         * do at recovery time.
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#postAdd(com.ibm.ws.objectManager.Transaction, boolean)
         */
        public void postAdd(Transaction transaction
                            , boolean logged
                        )
                                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "postAdd",
                            "transaction=" + transaction + "(Transaction)"
                                            + " logged=" + logged + "(boolean)"
                                );

            super.postAdd(transaction
                          , logged
                            );
            // The link now appears as only part of the transaction adding it.

            if (logged) {

                // So that the state is set correctly both at runtime and on recovery we do it here.
                // No need to synchronize to protect state because only the locking
                // transaction can change it.
                setState(nextStateForRequestAdd); // Make the state change.

                // We may need to undo the effects of the add so we need to be told before
                // the transaction 
                // backs out. We do this in the ObjectManager add method so that this is
                // redriven during recovery.
                transaction.requestCallback(owningToken);

            } // if(logged).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "postAdd");
        } // End of postAdd method.

        /*
         * Modify the behaviour of the ManagedObject postDelete method to perform extra work which we will
         * also do at recovery time. Actual removal from the list done at pre commit time, but we need
         * to make sure we are called when this happens, even if it is during recovery.(non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#postDelete(com.ibm.ws.objectManager.Transaction, boolean)
         */
        public void postDelete(Transaction transaction,
                               boolean logged)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "postDelete",
                            new Object[] { transaction, new Boolean(logged) }
                                );

            super.postDelete(transaction,
                             logged);
            // Visibility is now restricted to our transaction only.

            if (logged) {
                // If we are recovering, the request to delete will not have been seen so
                // do it here.
                if (transaction.getObjectManagerStateState() == ObjectManagerState.stateReplayingLog)
                    synchronized (list) {
                        // In principle no need to synchronize because recovery is single
                        // threaded.
                        setState(nextStateForRequestDelete); // Make the state change.
                    } // synchronized (list).

                // Complete the deletion at prePrepare time.
                transaction.requestCallback(this.owningToken);

            } // if(logged).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "postDelete");
        } // postDelete().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#optimisticReplaceLogged(com.ibm.ws.objectManager.Transaction)
         */
        public void optimisticReplaceLogged(Transaction transaction)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "optimisticReplaceLogged",
                            "transaction=" + transaction + "(Transaction)");

            super.optimisticReplaceLogged(transaction);
            // So that the state is set correctly both at runtime and on recovery we
            // do it here. 
            // No need to synchronize to protect state because only the deleting
            // thread has the list locked.
            setState(nextStateForRemove); // Make the state change.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "optimisticReplaceLogged");
        } // End of optimisticReplaceLogged method.

        /**
         * The transaction is about to commit so finalise any changes. This is called for the
         * transaction adding or deleting the Link but not for other Links chaining before or after this
         * one, since they do not register for the callback.
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#preCommit(com.ibm.ws.objectManager.Transaction)
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

            // No need to synchronize to protect state because only the locking
            // transaction can change it.
            switch (state) {
                case stateToBeAdded:
                    // Give up the space we reserved in case the add backed out.
                    // During recovery we don't do this because we will not have reserved the space anyway.
                    if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog)
                        owningToken.objectStore.reserve(-(int) list.storeSpaceForRemove(), false);
                    break;

                case stateToBeDeleted:
                case stateMustBeDeleted:
                    // Removal is only done if we are in still the list and have not been
                    // notified that the OptimisticReplace
                    // has been done. It would not be done again during recovery
                    // if we succeed in writing the updates to the adjacent links now.
                    //
                    // We must commit the transaction, as we are now past the point
                    // of no return, this is acomplished by recording the transaction 
                    // state as part of the optimistic replace log record. 
                    removeFromList(transaction); // Detach the link from the list.

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
         * The transaction is about to backed out so fix up anything we have changed. This is called for
         * the transaction adding or deleting the Link but not for other Links chaining before or after
         * this one, since they do not register for the callback. We do not use the default
         * ObjectManager behaviour of restoring a beofre immage because we allow changes to be made to
         * links after they are written to the log but before commit, so now we have to repair any
         * changes dependant on the current state of the list.
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

            // No need to synchronize to protect state because only the locking
            // transaction can change it.
            switch (state) {
                case stateToBeAdded:
                case stateMustBeDeleted:
                    // The OptimisticReplace for removal from the lists also records whether we are 
                    // commiting or backing out.
                    synchronized (list) {
                        removeFromList(transaction); // Reverse the addition.
                        // If we are currently the availableHead, reset the availableHead to the head of the list.
                        // We could work back through the list looking for a suitable link taking the state into
                        // account.
                        if (getToken() == list.availableHead) {
                            list.availableHead = list.head;
                            list.skipToBeDeleted();
                        } // if (getToken() == list.availableHead).
                    } // synchronized (list).
                    break;

                case stateNotAdded:
                    // We have been driven during recovery after the backout and removal from stateToBeAdded has been recovered.
                    break;

                case stateToBeDeleted:
                    // Give back the space we would have used to remove the link.
                    // During recovery we don't do this because we will not have reserved the space anyway.
                    if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog)
                        owningToken.objectStore.reserve(-(int) list.storeSpaceForRemove(), false);
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
         * The transaction has commited so now we can make any updated links available. This is called
         * for the transaction adding or deleting the Link but not for other Links chaining before or
         * after this one, since they do not lock the link.
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#commit(com.ibm.ws.objectManager.Transaction, com.ibm.ws.objectManager.ObjectManagerByteArrayOutputStream, long, boolean)
         */
        protected void commit(Transaction transaction
                              , ObjectManagerByteArrayOutputStream serializedBytes
                              , long logSequenceNumber
                              , boolean requiresCurrentCheckpoint
                        )
                                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "commit"
                            , new Object[] { transaction, serializedBytes, new Long(logSequenceNumber), new Boolean(requiresCurrentCheckpoint) }
                                );

            super.commit(transaction, serializedBytes, logSequenceNumber, requiresCurrentCheckpoint);

            // No need to synchronize to protect state because only the locking
            // transaction can change it. Unlock has not occured yet. 
            switch (state) {
                case stateToBeAdded:
                    setState(nextStateForCommit);
                    if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog) {
                        list.incrementAvailableSize(); // Adjust list visible length available.
                    } // if (!recovering).
                    break;

                default:
                    // Make the state change. Transition to error state and throw StateErrorException 
                    // if the state is invalid because it is not safe to continue.  
                    setState(nextStateForCommit);

            } // switch.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "commit");
        } // commit().

        /**
         * The transaction has backed out so make any backed out links available again. This is called
         * for the transaction adding or deleting the Link but not for other Links chaining before or
         * after this one, since they do not lock the link.
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#backout(com.ibm.ws.objectManager.Transaction, long, boolean)
         */
        public void backout(Transaction transaction,
                            long logSequenceNumber,
                            boolean requiresCurrentCheckpoint)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this
                            , cclass
                            , "backout"
                            , new Object[] { transaction, new Long(logSequenceNumber), new Boolean(requiresCurrentCheckpoint) });

            super.backout(transaction, logSequenceNumber, requiresCurrentCheckpoint);

            // No need to synchronize to protect state because only the locking transaction can change it.
            switch (state) {

                case stateToBeDeleted: // We have canceled a delete of a link.  
                    synchronized (list) {
                        setState(nextStateForBackout); // Make the state change.
                        if (transaction.getObjectManagerStateState() == ObjectManagerState.stateReplayingLog) {
                            // We cannot call skipToBedeleted during recovery bacause the adjacent links may
                            // not exist any more. The next getter will reset the availableHead cursor.
                            list.availableHead = null;
                        } else {
                            list.incrementAvailableSize(); // Adjust list visible length available.
                            // We might have put this link back near the head.
                            list.availableHead = list.head;
                            list.skipToBeDeleted();

                        } // if(recovering).
                    } // synchronized (list).
                    break;

                default:
                    // Make the state change. Transition to error state and throw StateErrorException 
                    // if the state is invalid because it is not safe to continue.  
                    setState(nextStateForBackout);
            } // switch (state).     

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this
                           , cclass
                           , "backout");
        } // backout().

        /**
         * Driven just after the ObjectManager finishes replaying the log, but before it backs out any
         * incomplete transactions and starts to make forward progress.
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

            // Synchronize should not be necessary since recovery is currently single
            // threaded.
            synchronized (list) { // Now we lock the LinkedList to this thread alone.

                switch (state) {
                    case stateToBeAdded:
                        // We have not yet added the link so temporarily reduce the size. We
                        // incremented list.size assuming we would commit and restored that 
                        // to availableSize. If this transaction commits we will increment 
                        // available size again.
                        list.decrementAvailableSize();
                        // Reserve space in the store, this should succeed because we reserved the space before we
                        // crashed.
                        owningToken.objectStore.reserve((int) list.storeSpaceForRemove(), false);
                        break;

                    case stateToBeDeleted:
                    case stateMustBeDeleted:
                        // The transaction that wants to delete this link has not yet
                        // completed. Adjust list visible length available.
                        list.decrementAvailableSize();
                        // Move the available head forward?
                        if (owningToken == list.availableHead) {
                            list.skipToBeDeleted();
                        } // if (owningToken == list.availableHead).
                          // Reserve space in the store, this should succeed because we reserved the space before we
                          // crashed.
                        owningToken.objectStore.reserve((int) list.storeSpaceForRemove(), false);
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

            } // synchronized (list).

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
        protected void setState(int[] nextState)
                        throws StateErrorException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "setState",
                            new Object[] { nextState, new Integer(state), stateNames[state] }
                                );

            // No need to synchronize in here bacause only one transaction can add or delete
            // links at a time. Other transactions performing OptimisticReplace updates 
            // do not change the state.
            previousState = state; // Capture the previous state for dump.
            state = nextState[state]; // Make the state change.

            if (state == stateError) {
                StateErrorException stateErrorException = new StateErrorException(this, previousState, stateNames[previousState]);
                ObjectManager.ffdc.processException(this, cclass, "setState", stateErrorException, "1:3471:1.40");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "setState"
                               , new Object[] { stateErrorException, new Integer(state), stateNames[state] }
                                    );
                throw stateErrorException;
            } // if (state == stateError).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "setState",
                           new Object[] { new Integer(state), stateNames[state] }
                                );
        } // setState().

        // ----------------------------------------------------------------------------------------
        // implements List.Entry.
        // ----------------------------------------------------------------------------------------

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.List.Entry#getValue()
         */
        public Token getValue()
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "getValue");
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "getValue",
                           new Object[] { data });
            return data;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.List.Entry#setValue(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
         */
        public Token setValue(Token newToken,
                              Transaction transaction)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "setValue",
                            new Object[] { newToken, transaction, this.data });

            synchronized (transaction.internalTransaction) {
                synchronized (list) {
                    list.insert(newToken,
                                this,
                                transaction);
                    remove(transaction);
                } // synchronized(list).
            } // synchronized (transaction.internalTransaction).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "setValue");
            return data;
        } // setValue().

        /**
         * @return int the current state of this List.Entry.
         * @throws ObjectManagerException
         */
        public int getEntryState()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "getEntryState");

            int stateToReturn = state;

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "getEntryState",
                           "returns statetoReturn=" + stateToReturn + "(int) " + stateNames[stateToReturn] + "(String)");
            return stateToReturn;
        } // getEntryState().

        /**
         * Remove the entry from the list.
         * 
         * @param transaction coordinating the removal.
         * @throws ObjectManagerException
         */
        public void remove(Transaction transaction)
                        throws ObjectManagerException {
            final String methodName = "remove";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction });

            synchronized (transaction.internalTransaction) {
                synchronized (list) {

                    if (state == stateAdded) {
                        requestDelete(transaction); // Mark for deletion.
                        list.decrementAvailableSize();

                    } else if (state == stateToBeAdded && lockedBy(transaction)) {
                        requestDelete(transaction); // Mark for deletion.
                        // If the link was added by the same transaction do not decrement the available size because
                        // it has not yet been incremented.
                    } else {
                        InvalidStateException invalidStateException = new InvalidStateException(this, state, stateNames[state]);
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this, cclass, methodName, new Object[] { invalidStateException,
                                                                               new Integer(state),
                                                                               stateNames[state] });
                        throw invalidStateException;
                    } // if (state == stateAdded).

                } // synchronized (list).

                // We can release the synchronize lock on the list because we dont care if the
                // link which we are going to delete is changed before we write the log record.
                // Log what we intend to do. Reserve enough spare log space so that the eventual
                // optimistic replace that removes the link from the list is certain to succeed
                // in being written as well.
                try {
                    transaction.delete(this,
                                       logSpaceForDelete());

                } catch (LogFileFullException exception) {
                    // No FFDC Code Needed, transaction.delete() has already done this.
                    list.unRemove(this);
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, exception);
                    throw exception;

                    // There should be no ObjectStoreFull exceptions for Transaction.delete.
                    // Space is reserved by the store itself for delete operations. 
//        } catch (ObjectStoreFullException exception) {
//          // No FFDC Code Needed, transaction.delete() has already done this.
//          list.unRemove(this);
//          if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//            trace.exit(this, cclass, methodName, exception);
//          throw exception;
//          
                } catch (InvalidStateException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this,
                                                        cclass,
                                                        methodName,
                                                        exception,
                                                        "1:3619:1.40");
                    list.unRemove(this);
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, exception);
                    throw exception;
                } // try.
            } // synchronized (transaction.internalTransaction).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // remove().

        // --------------------------------------------------------------------------
        // implements SimplifiedSerialization.
        // --------------------------------------------------------------------------

        /**
         * No argument constructor.
         * 
         * @exception ObjectManagerException
         */
        Link()
            throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "<init>");

            previousState = -1; // No previous state.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "<init>");
        } // end of Constructor.

        private static final byte SimpleSerialVersion = 0;

        // The serialized size of this.
        // TODO Assumes data is a token. We could allow the user to ser the max size
        // and police this, that way they couls still
        //      use any Object. Also need to do the same thing for value,keyand
        // comparatot in TreeMap.
        protected static long maximumSerializedSize()
        {
            return 1 // Version.
                   + ManagedObject.maximumSerializedSize() + 2 // Flag bytes
                   + 4 * Token.maximumSerializedSize() // List,Data,Next,Previous
            ;
        } // end of maximumSerializedSize().

        /*
         * (non-Javadoc)
         * 
         * @see int SimplifiedSerialization.getSignature()
         */
        public int getSignature()
        {
            return signature_LinkedList_Link;
        } // End of getSignature.

        /*
         * (non-Javadoc)
         * 
         * @see SimplifiedSerialization.writeObject(java.io.DataInputStream)
         */
        public void writeObject(java.io.DataOutputStream dataOutputStream)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "writeObject",
                            "dataOutputStream=" + dataOutputStream + "(java.io.DataOutputStream)");

            try {
                dataOutputStream.writeByte(SimpleSerialVersion);
                super.writeObject(dataOutputStream);

                // The list responsible for this Link.
                listToken.writeObject(dataOutputStream);
                data.writeObject(dataOutputStream);

                if (next == null) {
                    dataOutputStream.writeByte(0);
                } else {
                    dataOutputStream.writeByte(1);
                    next.writeObject(dataOutputStream); // The folowing link in the list.
                } // if (next == null).
                if (previous == null) {
                    dataOutputStream.writeByte(0);
                } else {
                    dataOutputStream.writeByte(1);
                    // The link in the list beforethis one.
                    previous.writeObject(dataOutputStream);
                } // if (previous == null).

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, ".writeObject", exception, "1:3716:1.40");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "writeObject",
                               exception);
                throw new PermanentIOException(this,
                                               exception);
            } // catch (java.io.IOException exception).

            // We cannot check size as we are subclassed.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "writeObject");
        } // Of writeObject().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#readObject(java.io.DataInputStream, com.ibm.ws.objectManager.ObjectManagerState)
         */
        public void readObject(java.io.DataInputStream dataInputStream,
                               ObjectManagerState objectManagerState)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "readObject",
                            "dataInputStream="
                                            + dataInputStream
                                            + "(java.io.DataInputStream)"
                                            + " objectManagerState="
                                            + objectManagerState
                                            + "(ObjectManagerState)");

            try {
                byte version = dataInputStream.readByte();
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this, cclass,
                                "readObject",
                                new Object[] { new Byte(version) });
                super.readObject(dataInputStream,
                                 objectManagerState);

                // The list responsible for this link.
                listToken = Token.restore(dataInputStream,
                                          objectManagerState);
                data = Token.restore(dataInputStream,
                                     objectManagerState);
                // The folowing link in the list.
                if (dataInputStream.readByte() == 1)
                    next = Token.restore(dataInputStream,
                                         objectManagerState);
                // The link in the list before this one.
                if (dataInputStream.readByte() == 1)
                    previous = Token.restore(dataInputStream,
                                             objectManagerState);

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, ".readObject", exception, "1:3775:1.40");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "readObject",
                               "via PermanentIOException");
                throw new PermanentIOException(this,
                                               exception);
            } // catch (java.io.IOException exception).

            list = (LinkedList) (listToken.getManagedObject());
            // The list may have been deleted, put a dummy list in place, 
            // to satisfy replay during recovery but do not allocate it to a store.
            if (list == null)
                list = new LinkedList();
            // Assume we were added, corrected in add() and delete() methods if
            // appropriate.
            state = stateAdded;
            previousState = -1; // No previous state.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "readObject");
        } // readObject().

        // --------------------------------------------------------------------------
        // implements java.io.Serializable
        // --------------------------------------------------------------------------

        /**
         * Customized deserialization.
         * 
         * @param objectInputStream containing the serialized form of the Link.
         * @throws java.io.IOException
         * @throws java.lang.ClassNotFoundException
         */

        private void readObject(java.io.ObjectInputStream objectInputStream)
                        throws java.io.IOException
                        , java.lang.ClassNotFoundException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "readObject",
                            "objectInputStream=" + objectInputStream + "(java.io.ObjectInputStream)");

            objectInputStream.defaultReadObject();

            try {
                list = (LinkedList) (listToken.getManagedObject());
                // The list may have been deleted, put a dummy list in place, 
                // to satisfy replay during recovery but do not allocate it to a store.
                if (list == null)
                    list = new LinkedList();

            } catch (ObjectManagerException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    "readObject",
                                                    exception,
                                                    "1:3835:1.40");
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "readObject"
                               , exception);
                throw new java.lang.ClassNotFoundException(exception.toString());
            } // catch (ObjectManagerException exception).

            // Assume we were added, corrected in add() and delete() methods if
            // appropriate.
            state = stateAdded;
            previousState = -1; // No previous state.

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "readObject");
        } // End of method readObject.

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
            return new String("LinkedList.Link"
                              + "(data=" + data + ")"
                              + "/" + stateNames[state]
                              + " " + super.toString());
        } // toString().
    } // Of inner class Link.
} // class LinkedList.
