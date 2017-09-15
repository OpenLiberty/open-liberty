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
 * A recoverable LinkedList with greater parallelism. Each link is given a
 * sequence number, for links added to the end of the overall list the sequence
 * number is incremented. For links added into the body of the list, the
 * insertion point is identified and its sequence number is copied into the
 * inserted link.
 * 
 * Locking strategy.
 * -----------------
 * LinkedLists lock Transaction.InternalTransaction then List.
 * ConcurrentLists lock InternalTransaction,HeadSequenceNumber,TailSequenceNumber,SubList, if necessary.
 * 
 * @see com.ibm.ws.objectManager.LinkedList
 */
public class ConcurrentLinkedList
                extends AbstractList
                implements List, SimplifiedSerialization, Printable
{
    private static final Class cclass = ConcurrentLinkedList.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(ConcurrentLinkedList.class,
                                                                     ObjectManagerConstants.MSG_GROUP_LISTS);

    private static final long serialVersionUID = 1260307151888854043L;

    private int activeSubListCount = 0; // the number of subLists we will add
                                        // links to.
    private transient ConcurrentSubList[] subLists; // The sub lists.
    private Token[] subListTokens; // The sub list Tokens.

    // If a lock on head and tail is required, lock head then tail.
    // The sequencenumber for the overall head link.
    protected transient long headSequenceNumber;
    transient HeadSequenceNumberLock headSequenceNumberLock;

    class HeadSequenceNumberLock {}

    // The sequencenumber for the overall tail link.
    private transient long tailSequenceNumber;
    transient TailSequenceNumberLock tailSequenceNumberLock;

    class TailSequenceNumberLock extends java.util.concurrent.locks.ReentrantLock {
        private static final long serialVersionUID = 7094411719880377822L;
    } // If Java V1.5 compatible.
      // class TailSequenceNumberLock extends ReentrantLock {} // If not Java V1.5 compatible.
      // Indicate that the tailsequenceNumber has not yet been set.

    private transient boolean tailSequenceNumberSet = false;

    // For gatherStatistics.  
    protected transient long removeFirstPredictedSubListCount = 0;
    // Number of times removeFirst had to search all lists.
    protected transient long removeFirstFullSearchCount = 0;
    // Number of times we remove() a link without contention.
    protected transient long removeUncontendedCount = 0;
    // Number of times we remove() a link with contention.
    protected transient long removeContendedCount = 0;

    /**
     * Build a set of ConcurrentSubLists.
     * 
     * @param transaction contyroling the creation of the ConcurrentList and the subLists.
     * @param objectStore where the lists are stored.
     * @param subListCount of the number of subLists.
     * @throws ObjectManagerException
     */
    public ConcurrentLinkedList(Transaction transaction,
                                ObjectStore objectStore,
                                int subListCount)
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "<init>",
                        new Object[] { transaction, objectStore, new Integer(subListCount) });

        Token objectManagerList = objectStore.allocate(this);

        this.activeSubListCount = subListCount;
        subLists = new ConcurrentSubList[subListCount];
        subListTokens = new Token[subListCount];
        headSequenceNumber = 0;
        headSequenceNumberLock = new HeadSequenceNumberLock();
        tailSequenceNumber = 0;
        tailSequenceNumberLock = new TailSequenceNumberLock();
        tailSequenceNumberSet = true;
        for (int i = 0; i < subLists.length; i++) {
            subLists[i] = new ConcurrentSubList(objectManagerList,
                                                transaction,
                                                objectStore);
            subListTokens[i] = subLists[i].getToken();
        } // for subLists.

        transaction.add(this);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "<init>");
    } // ConcurrentLinkedList().

    /**
     * @return int the activeSubListCount.
     */
    public int getActiveSublistCount()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this, cclass,
                        "getActiveSublistCount");
            trace.exit(this, cclass,
                       "getActiveSublistCount",
                       "return=" + activeSubListCount);
        }
        return activeSubListCount;
    } // getActiveSubListCount().

    /**
     * Increment the headSequenceNumber, after removal of a link in a subList.
     * Caller must already be synchronized on headSequenceNumberLock.
     * 
     * @param link causing the increment.
     * @throws ObjectManagerException
     */
    public void incrementHeadSequenceNumber(ConcurrentSubList.Link link)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "incrementheadSequenceNumber",
                        new Object[] { link });

        if (link.next == null) { // This subList is now empty.
            headSequenceNumber++; // Look at another list next time.
        } else { // Still more links in this subList.
            ConcurrentSubList.Link nextLink = (ConcurrentSubList.Link) link.next.getManagedObject();
            // If insertion into the body of the list has taken place,
            // then a sequenceNumber will have been duplicated.
            if (nextLink.sequenceNumber != headSequenceNumber)
                headSequenceNumber++;
        } // if( firstLink.next == null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "incrementHeadSequenceNumber",
                       new Object[] { new Long(headSequenceNumber) });
    } // incrementHeadSequenceNumber().

    /**
     * @param activeSubListCount to set.
     * @param tranaction controling the change.
     */
    //  public void setActiveSublistCount(int activeSubListCount
    //                                   ,Transaction transaction
    //                                   )
    //  {
    //    // TODO change the actual number of subLists.
    //    this.activeSubListCount = activeSubListCount;
    //  }

    /**
     * Find the subList containing a sequenceNumber. @param long the
     * sequenceNumber whose ConcurrentSubList is to be found. @return
     * ConcurrentSubList which could contain the sequenceNumber.
     * 
     * @param sequenceNumber who's subList is to be found.
     * @return ConcurrentSubList containing the sequenceNumber.
     */
    private final ConcurrentSubList getSublist(long sequenceNumber)
    {
        return subLists[(int) (sequenceNumber % activeSubListCount)];
    } // Of method getSublist.

    //  /**
    //   * Reset the headSequenceNumber.
    //   * Caller must be synchronized on headSequenceNumberLock.
    //   */
    //  private void resetHeadSequenceNumber()
    //               throws ObjectManagerException
    //  {
    //    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
    //      trace.entry(this,cclass
    //                 ,"resetHeadSequenceNumber"
    //                 );
    //
    //    synchronized (tailSequenceNumberLock) {
    //      headSequenceNumber = tailSequenceNumber;
    //    } // synchronized (tailSequenceNumberLock).
    //
    //    // Look at each subList in turn.
    //    for (int i = 0; i < subListTokens.length; i++) {
    //      synchronized (subLists[i]) {
    //        // Establish the lowest existing headSequenceNumber.
    //        ConcurrentSubList.Link head =
    // (ConcurrentSubList.Link)subLists[i].firstAvailableLink();
    //        if (head != null) {
    //          headSequenceNumber = Math.min(headSequenceNumber,head.sequenceNumber);
    //        } // if (head != null).
    //      } // synchronized (subLists[i]).
    //    } // for subList.
    //    
    //    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
    //      trace.exit(this,cclass
    //                ,"resetHeadSequenceNumber"
    //                );
    //  } // End of method resetHeadSequenceNumber.

    /**
     * Reset the tailSequenceNumber.
     * Caller must be synchronized on tailSequenceNumberLock.
     * 
     * @throws ObjectManagerException
     */
    private void resetTailSequenceNumber()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "resetTailSequenceNumber"
                            );

        for (int i = 0; i < subListTokens.length; i++) {
            subLists[i] = ((ConcurrentSubList) subListTokens[i].getManagedObject());
            // In case another thread is currently removing the tail, take a safe copy.
            Token tailToken = subLists[i].tail;
            // Establish the highest existing tailSequenceNumber.
            if (tailToken != null) {
                ConcurrentSubList.Link tail = (ConcurrentSubList.Link) tailToken.getManagedObject();
                tailSequenceNumber = Math.max(tailSequenceNumber,
                                              tail.sequenceNumber);
            } // if (tailToken != null).
        } // for subList.

        tailSequenceNumberSet = true;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "resetHeadSequenceNumber"
                            );
    } // resetTailSequenceNumber().

    /*
     * Builds a set of properties containing the current statistics. @return
     * java.util.Map the statistics.
     */
    public java.util.Map captureStatistics()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "captureStatistics");

        // Consolidate statistics from the subLists.
        // The number of links in the deepest subList.
        long maximumAvailableSizeOfAnySublist = 0;
        for (int i = 0; i < subLists.length; i++) {
            maximumAvailableSizeOfAnySublist = Math.max(maximumAvailableSizeOfAnySublist,
                                                        subLists[i].maximumAvailableSize);
        } // for subLists.

        java.util.Map statistics = new java.util.HashMap();
        statistics.put("removeFirstPredictedSubListRemoveCount",
                       Long.toString(removeFirstPredictedSubListCount));
        statistics.put("removeFirstFullSearchCount",
                       Long.toString(removeFirstFullSearchCount));
        statistics.put("maximumAvailableSizeOfAnySublist",
                       Long.toString(maximumAvailableSizeOfAnySublist));
        statistics.put("removeContendedCount",
                       Long.toString(removeContendedCount));
        statistics.put("removeUncontendedCount",
                       Long.toString(removeUncontendedCount));

        removeFirstPredictedSubListCount = 0;
        removeFirstFullSearchCount = 0;
        removeUncontendedCount = 0;
        removeContendedCount = 0;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "captureStatistics",
                       "returns statistics=" + statistics + "(java.util.Map)");
        return statistics;
    } // method captureStatistics().

    // --------------------------------------------------------------------------
    // implements List.
    // --------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see ws.sib.objectManager.List#size(ws.sib.objectManager.Transaction)
     */
    public long size(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "size",
                        "Transaction=" + transaction);

        long totalSize = 0; // Size to return.
        for (int i = 0; i < subLists.length; i++) {
            totalSize = totalSize + subLists[i].size(transaction);
        } // for subLists.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "size",
                       "return=" + totalSize);
        return totalSize;
    } // size(Transaction).

    /*
     * (non-Javadoc)
     * 
     * @see ws.sib.objectManager.List#size()
     */
    public long size()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "size");

        long totalSize = 0; // Size to return.
        for (int i = 0; i < subLists.length; i++) {
            totalSize = totalSize + subLists[i].size();
        } // for subLists.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "size",
                       "return=" + totalSize);
        return totalSize;
    } // size().

    /*
     * (non-Javadoc)
     * 
     * @see ws.sib.objectManager.List#isEmpty(ws.sib.objectManager.Transaction)
     */
    public boolean isEmpty(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "isEmpty",
                        "Transaction=" + transaction);

        boolean empty = true; // Empty status to return.
        for (int i = 0; i < subLists.length; i++) {
            if (!subLists[i].isEmpty(transaction)) {
                empty = false;
                break;
            }
        } // for subLists.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "size",
                       "return=" + empty);
        return empty;
    } // isEmpty().

    /*
     * (non-Javadoc)
     * 
     * @see ws.sib.objectManager.List#iterator()
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
     * @see ws.sib.objectManager.List#listIterator()
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
    } // Of listIterator().

    /*
     * (non-Javadoc)
     * 
     * @see ws.sib.objectManager.List#entryIterator()
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
    } // entryIterator().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#add(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public boolean add(Token token,
                       Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "add",
                        new Object[] { token, transaction });

        List.Entry newEntry = null;
        // Lock the tailSequenceNumber until the new link is in the list.
        // This prevents the possibility that after we had incremented the tail sequence number
        // but before we had added it to the subList some thread resets the headSequenceNumber 
        // and skipped passed the link we just added.
        synchronized (transaction.internalTransaction) {
            tailSequenceNumberLock.lock();
            try {
                if (!tailSequenceNumberSet)
                    resetTailSequenceNumber();
                // Establish the sequenceNumber of this Object in the overall list.
                long usableTailSequenceNumber = ++tailSequenceNumber;

                // Figure out which subList the Object should be added to.
                ConcurrentSubList list = getSublist(usableTailSequenceNumber);
                // Add the link near the tail of the list according to its assigned
                // sequence number.
                newEntry = list.addEntry(token,
                                         transaction,
                                         usableTailSequenceNumber,
                                         this);
            } finally {
                if (tailSequenceNumberLock.isHeldByCurrentThread())
                    tailSequenceNumberLock.unlock();
            } // try (tailSequenceNumberLock).
        } // synchronized (transaction.internalTransaction).

        boolean modified = (newEntry != null);
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "add",
                       new Object[] { new Boolean(modified) });
        return modified;
    } // of add().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#addEntry(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public List.Entry addEntry(Token token,
                               Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "addEntry",
                        new Object[] { token, transaction });

        List.Entry newEntry = null;
        // Lock the tailSequenceNumber until the new link is in the list.
        // This prevents the possibility that after we had incremented the tail
        // sequence number but before we had added it to the
        // subList some thread resets the headSequenceNumber and skipped passed the
        // link we just added.
        synchronized (transaction.internalTransaction) {
            long usableTailSequenceNumber;
            tailSequenceNumberLock.lock();
            try {
                if (!tailSequenceNumberSet)
                    resetTailSequenceNumber();
                // Establish the sequenceNumber of this Object in the overall list.
                usableTailSequenceNumber = ++tailSequenceNumber;

                // Figure out which subList the Object should be added to.      
                ConcurrentSubList list = getSublist(usableTailSequenceNumber);
                // Add the link near the tail of the list according to its assigned
                // sequence number.
                newEntry = list.addEntry(token,
                                         transaction,
                                         usableTailSequenceNumber,
                                         this);
            } finally {
                if (tailSequenceNumberLock.isHeldByCurrentThread())
                    tailSequenceNumberLock.unlock();
            } // try (tailSequenceNumberLock).
        } // synchronized (transaction.internalTransaction).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "addEntry",
                       new Object[] { newEntry });
        return newEntry;
    } // addEntry().

    /**
     * Insert before the given link.
     * 
     * @param token to insert.
     * @param insertPoint which the new link will be before.
     * @param transaction controling the insertion.
     * @return ConcurrentSubList.Link the inserted Link.
     * @throws ObjectManagerException
     */
    protected ConcurrentSubList.Link insert(Token token,
                                            ConcurrentSubList.Link insertPoint,
                                            Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "insert",
                        new Object[] { token, insertPoint, transaction });

        ConcurrentSubList.Link newLink = null;

        synchronized (transaction.internalTransaction) {
            // Establish the sequenceNumber of this Object in the overall list,
            // before the insert point.
            long sequenceNumber = insertPoint.sequenceNumber;
            sequenceNumber--;
            // Figure out which subList the Object should be added to.
            ConcurrentSubList list = getSublist(sequenceNumber);
            // Add the link near the tail of the list according to its assigned
            // sequence number.
            tailSequenceNumberLock.lock();
            try {
                newLink = list.addEntry(token,
                                        transaction,
                                        sequenceNumber,
                                        this);
            } finally {
                if (tailSequenceNumberLock.isHeldByCurrentThread())
                    tailSequenceNumberLock.unlock();
            } // try (tailSequenceNumberLock).
        } // synchronized (transaction.internalTransaction).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "insert",
                       new Object[] { newLink });
        return newLink;
    } // insert().

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

        // The true head of the combined subLists. This is the link at
        // the head of a subList with the largest sequenceNumber.
        ConcurrentSubList.Link firstLink = null;
        Token tokenToReturn = null;

        // The search is conducted in two phases.
        // 1) Look at the list indicated by the headSequenceNumber, in the hope that
        // we can avoid
        //    locking and searching all of the subLists.
        // 2) Look at all of the subLists for an unlocked Link or a Link locked by
        // our transaction,
        //    also correct the headSequenceNumber as we do this.
        //    The headSequenceNumber may have falen behind due to a message being
        // removed by some
        //    other method such as remove(Object,transaction) or the producer backed
        // out leaving a gap.

        ConcurrentSubList subListForRemoval = null;

        synchronized (headSequenceNumberLock) {
            // Capture the ObjectManagers unlock sequence number. Anything unlocked after 
            // this will not count. The next line delimits the point before which additions to 
            // list must committed and unlocked.
            long unlockPoint = getTransactionUnlockSequence();
            // Phase 1.
            // --------
            // Try deletetion of an unlocked message based on the predicted subList.

            subListForRemoval = getSublist(headSequenceNumber);
            synchronized (subListForRemoval) { // Look at the target subList.

                firstLink = (ConcurrentSubList.Link) subListForRemoval.nextLink(null,
                                                                                transaction,
                                                                                unlockPoint);
                //       firstLink =
                // (ConcurrentSubList.Link)subListForRemoval.firstAvailableLink();

                if (firstLink != null) { // See if an available link was found.
                    // See if it is the link we expected.
                    if (firstLink.sequenceNumber == headSequenceNumber) {
                        firstLink.requestDelete(transaction);// Mark for deletion.
                        // If the link is available to all adjust list visible length available.
                        // In the case where we delete a link added by the same transaction we have no incremented 
                        // the available size yet because we have not yet committed so we would not decrement it.
                        if (!firstLink.isLocked())
                            subListForRemoval.decrementAvailableSize();

                        incrementHeadSequenceNumber(firstLink); // Update the
                                                                // headSequenceNumber.
                        // Assign the tokenToReturn while we have the subList locked so that the ObjectManager 
                        // cannot backout this transaction and remove the link under another transaction 
                        // causing this thread to see a null Token as a return value.
                        tokenToReturn = firstLink.data;
                    } else { // Not the sequence number we were expecting.
                        firstLink = null; // No use then, do a full search.
                    } // if (firstLink.sequenceNumber == headSequenceNumber).
                } // if (firstLink != null).
            } // synchronized (subListForRemoval).

            if (firstLink == null) {

                // Stop new additions while we work out the true head and tail of
                // the list.
                tailSequenceNumberLock.lock();
                try {
                    // Check to see if the overall list is empty. We delay this test until
                    // now because we have to lock the tail sequence number to do this.
                    // The headSequenceNumber is advanced after removal from the head, so that it refers
                    // to the next sequence we will remove. TailSequence 
                    // refers to the last one we added and has not been advanced yet.
                    if (headSequenceNumber == tailSequenceNumber + 1) {
                        throw new java.util.NoSuchElementException();
                    } else { // Prepare to reset the headSequenceNumber.
                        if (!tailSequenceNumberSet)
                            resetTailSequenceNumber();
                        headSequenceNumber = tailSequenceNumber + 1;
                    } // if (headSequenceNumber == tailSequenceNumber+1).

                    // Phase 2.
                    // --------
                    // Nothing found so far, possibly for one of the following reasons:
                    // 1) The message indicated by headSequenceNumber had been removed by
                    //    another method (eg. remove())
                    // 2) It was backed out without being added.
                    // 3) We skipped ahead previously and now the message nearer the head
                    // has
                    //    completed its commit.
                    // 4) We need to look deeper into each list for messages locked by our
                    // transaction.

                    // Set up for a search of any message available to our transaction.
                    long fullSearchHeadSequenceNumber = Long.MAX_VALUE;

                    // Search for the list that contains the firstLink.
                    for (int i = 0; i < subLists.length; i++) {
                        synchronized (subLists[i]) {
                            // Continue setting the fullSearchHeadSequenceNumber for the
                            // specified transaction.
                            ConcurrentSubList.Link first = (ConcurrentSubList.Link) subLists[i].nextLink(null,
                                                                                                         transaction,
                                                                                                         unlockPoint);
                            if (first != null) { // Is there a first element?
                                if (first.sequenceNumber < fullSearchHeadSequenceNumber) {
                                    fullSearchHeadSequenceNumber = first.sequenceNumber;
                                    subListForRemoval = subLists[i];
                                    firstLink = first;

                                    // Assign the tokenToReturn while we have the subList locked so that the ObjectManager 
                                    // cannot backout this transaction and remove the link under anoy=ther transaction 
                                    // causing this thread to see a null Token as a return value.
                                    tokenToReturn = firstLink.data;
                                } // if (first.sequenceNumber < headSequenceNumber).
                            } // if (first != null).

                            // nextLink() skipped over anything that was toBeAdded for another
                            // transaction however these links
                            // are still eligible for the headSequenceNumber, so look again.
                            ConcurrentSubList.Link head = (ConcurrentSubList.Link) subLists[i].firstAvailableLink();
                            if (head != null) {
                                headSequenceNumber = Math.min(headSequenceNumber,
                                                              head.sequenceNumber);
                            } // if (head != null).
                        } // synchronized (subLists[i]).
                    } // for subLists.
                } finally {
                    if (tailSequenceNumberLock.isHeldByCurrentThread())
                        tailSequenceNumberLock.unlock();
                } // try (tailSequenceNumberLock).

                if (firstLink == null) { // Was an overallFirst found?
                    java.util.NoSuchElementException exception = new java.util.NoSuchElementException();
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, exception);
                    throw exception;

                } else {
                    // A suitable link was found.
                    // We have released the synchronize lock on the subLists, but we still
                    // hold the lock on the headSequenceNumber so no other thread can mark any of
                    // the Links, take the lock on the relevant subList again so that we can mark 
                    // it for update.
                    // It is not possible for a caller of remove(Object) to steal our link because we hold the 
                    // headSequenceNumber lock.
                    synchronized (subListForRemoval) {
                        firstLink.requestDelete(transaction);// Mark for deletion.
                        // If the link is available to all adjust list visible length available.
                        // In the case where we delete a link added by the same transaction we have no incremented 
                        // the available size yet because we have not yet committed so we would not decrement it.
                        if (!firstLink.isLocked())
                            subListForRemoval.decrementAvailableSize();

                        // Update the headSequenceNumber, if it is the head of the overall
                        // list.
                        if (firstLink.sequenceNumber == headSequenceNumber) {
                            incrementHeadSequenceNumber(firstLink);
                        } // if ( firstLink.sequenceNumber == headSequenceNumber ).
                    } // synchronized (subListForRemoval).
                    if (LinkedList.gatherStatistics)
                        removeFirstFullSearchCount++;
                } // if (firstLink == null).

            } // if (firstLink == null).
            else if (LinkedList.gatherStatistics)
                removeFirstPredictedSubListCount++;
        } // synchronized (headSequenceNumberLock).

        // We can release the synchronize lock on the list because we don't care if the
        // link which we are going to delete is changed before we write the log record.
        // Anyhow it is now marked as ToBeDeleted.
        // Log what we intend to do. Reserve enough spare log space so that the eventual
        // optimistic replace that removes the link from the list is certain to succeed
        // in being written as well.
        try {
            transaction.delete(firstLink,
                               ConcurrentSubList.logSpaceForDelete());

        } catch (LogFileFullException exception) {
            // No FFDC Code Needed, transaction.delete() has already done this.
            firstLink.unRemove(this);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, exception);
            throw exception;

            // We should not see ObjectStoreFullException because we have preReserved 
            // the ObjectStore space.
//    } catch (ObjectStoreFullException exception) {
//      // No FFDC Code Needed, transaction.delete() has already done this.
//      firstLink.unRemove(this);
//      if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//        trace.exit(this, cclass, methodName, exception);
//      throw exception;

        } catch (InvalidStateException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:793:1.26");
            firstLink.unRemove(this);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, exception);
            throw exception;
        } // try.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { tokenToReturn });
        return tokenToReturn;
    } // removeFirst().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#remove(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public boolean remove(Token token,
                          Transaction transaction)
                    throws ObjectManagerException
    {
        final String methodName = "remove";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { token,
                                                                transaction });

        // Make an initial unsynchronized search for the link.
        ConcurrentSubList.Link linkToRemove = findLink(token,
                                                       transaction);

        boolean found = false;
        if (linkToRemove != null) { // Was a candidate found found?
            // We did not take the synchronize lock on the subList, so it could
            // have been modified.
            // Take the lock on the relevant subList so that we can mark it for
            // update.
            synchronized (headSequenceNumberLock) {
                synchronized (linkToRemove.list) {
                    if ((linkToRemove.state == ConcurrentSubList.Link.stateAdded)
                        || (linkToRemove.state == ConcurrentSubList.Link.stateToBeAdded && linkToRemove.lockedBy(transaction))) {
                        linkToRemove.requestDelete(transaction); // Mark for deletion.
                        // If the link is available to all adjust list visible length available.
                        // In the case where we delete a link added by the same transaction we have no incremented
                        // the available size yet because we have not yet committed so we would not decrement it.
                        if (!linkToRemove.isLocked())
                            linkToRemove.list.decrementAvailableSize();

                        found = true;

                        // Update the headSequenceNumber if we just removed the head of the
                        // overall list.
                        if (linkToRemove.sequenceNumber == headSequenceNumber) {
                            incrementHeadSequenceNumber(linkToRemove);
                        } // if (linkToRemove.sequenceNumber == headSequenceNumber ).

                        if (LinkedList.gatherStatistics)
                            removeUncontendedCount++;
                    } // if (linkToRemove.state == ConcurrentSubList.Link.stateAdded).
                } // synchronized (linkToRemove.list).
            } // synchronized (headSequenceNumberLock).

            // If the link was deleted meanwhile then have anothther look, but this
            // time take a lock.
            if (!found) {
                synchronized (headSequenceNumberLock) {
                    linkToRemove = findLink(token,
                                            transaction);
                    if (linkToRemove != null) {
                        synchronized (linkToRemove.list) {
                            linkToRemove.requestDelete(transaction); // Mark for deletion.
                            // If the link is available to all adjust list visible length available.
                            // In the case where we delete a link added by the same transaction we have no incremented 
                            // the available size yet because we have not yet committed so we would not decrement it.
                            if (!linkToRemove.isLocked())
                                linkToRemove.list.decrementAvailableSize();
                            found = true;

                            // Update the headSequenceNumber if we just removed the head of
                            // the overall list.
                            if (linkToRemove.sequenceNumber == headSequenceNumber) {
                                incrementHeadSequenceNumber(linkToRemove);
                            } // synchronized (headSequenceNumberLock)

                            if (LinkedList.gatherStatistics)
                                removeContendedCount++;
                        } // synchronized (linkToRemove.list).
                    } // if (linkToRemove != null).
                } // synchronized (headSequenceNumberLock).
            } // if (!found)
        } // if (linkToRemove != null).

        // We can release the synchronize lock on the list because we dont care if the
        // link which we are going to delete is changed before we write the log record.
        // Anyhow it is now marked as ToBeDeleted.
        // Log what we intend to do. Reserve enough spare log space so that the eventual
        // optimistic replace that removes the link from the list is certain to succeed
        // in being written as well.
        if (linkToRemove != null) {
            try {
                transaction.delete(linkToRemove,
                                   ConcurrentSubList.logSpaceForDelete());

            } catch (LogFileFullException exception) {
                // No FFDC Code Needed, transaction.delete() has already done this.
                linkToRemove.unRemove(this);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, exception);
                throw exception;

                // We should not see ObjectStoreFullException because we have preReserved 
                // the ObjectStore space.
//      } catch (ObjectStoreFullException exception) {
//        // No FFDC Code Needed, transaction.delete() has already done this.
//        linkToRemove.unRemove(this);
//        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//          trace.exit(this, cclass, methodName, exception);
//        throw exception;

            } catch (InvalidStateException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:912:1.26");
                linkToRemove.unRemove(this);
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass, methodName, exception);
                throw exception;

            } // catch (LogFileFullException exception).
        } // if (linkToRemove != null)

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { new Boolean(found) });
        return found;
    } // remove().

    /*
     * Find a link in the set of lists that references the target existing object.
     * 
     * @param Object the object that the target link refers to.
     * 
     * @return ConcurrentSubList.Link refering to ConcurrentSubList or null if there is
     * none.
     */
    private ConcurrentSubList.Link findLink(Object existingObject,
                                            Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "findLink",
                        "ExistingObject=" + existingObject + ", Transaction=" + transaction);

        ConcurrentSubList.Link foundLink = null; // The Link pointing to the target
                                                 // Object.
        long linkSequenceNumber = Long.MAX_VALUE;

        // Capture the ObjectManagers unlock sequence number. Anything unlocked after 
        // this will not count. The next line delimits the point before which additions to 
        // list must committed and unlocked.
        long unlockPoint = getTransactionUnlockSequence();

        // Look in each subList in turn for the existingObject.
        for (int i = 0; i < subLists.length; i++) {
            synchronized (subLists[i]) {
                ConcurrentSubList.Link nextLink = null;
                // Start at the top of the list.
                nextLink = (ConcurrentSubList.Link) subLists[i].nextLink(null,
                                                                         transaction,
                                                                         unlockPoint);
                // Move forward through the list until we find an element
                // that is visible to this transaction, and has the correct data.
                searchForward: while (nextLink != null) {
                    if (nextLink.data == existingObject)
                        break searchForward;
                    nextLink = (ConcurrentSubList.Link) subLists[i].nextLink(nextLink,
                                                                             transaction,
                                                                             unlockPoint);
                } // While (nextLink != null).

                if (nextLink != null) { // Did we find anything?
                    if (nextLink.sequenceNumber < linkSequenceNumber) {
                        linkSequenceNumber = nextLink.sequenceNumber;
                        foundLink = nextLink;
                    } // if (nextLink.sequenceNumber > linkToRemoveSequenceNumber).
                } // if (nextLink != null).

            } // synchronized (subLists).
        } // for subLists.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "findLink",
                       "return=" + foundLink);
        return foundLink;
    } // end of findLink().

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
                                      (ConcurrentSubList.Link) fromEntry,
                                      (ConcurrentSubList.Link) toEntry);
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "subList",
                       subList);
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

    /**
     * Print a dump of the state.
     * 
     * @param printWriter to be written to.
     */
    public synchronized void print(java.io.PrintWriter printWriter)
    {
        printWriter.println("State Dump for:" + cclass.getName()
                            + " headSequenceNumber=" + headSequenceNumber + "(long)" + " tailSequenceNumber=" + tailSequenceNumber + "(long)"
                        );
        printWriter.println();

        printWriter.println("Sublists...");
        for (int i = 0; i < subLists.length; i++)
        {
            subLists[i].print(printWriter);
        } // for subLists...

    } // print().

    /**
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
        for (int i = 0; i < subLists.length; i++) {
            if (!subLists[i].validate(printStream))
                valid = false;
        } // for...

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "validate",
                       new Object[] { new Boolean(valid) });
        return valid;
    } // validate().

    // --------------------------------------------------------------------------
    // extends ManagedObject.
    // --------------------------------------------------------------------------

    /**
     * Replace the state of this object with the same object in some other state.
     * Used for to restore the before image if a transaction rolls back or is read
     * from the log during restart.
     * 
     * @param other the ManagedObject this ManagedObject is to become a clone of.
     */
    public void becomeCloneOf(ManagedObject other)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "becomeCloneOf",
                        "Other=" + other);

        ConcurrentLinkedList otherConcurrentLinkedList = (ConcurrentLinkedList) other;
        subLists = otherConcurrentLinkedList.subLists;
        headSequenceNumber = otherConcurrentLinkedList.headSequenceNumber;
        tailSequenceNumber = otherConcurrentLinkedList.tailSequenceNumber;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "becomeCloneOf");
    } // becomeCloneOf().

    /**
     * Called just before the list itself is deleted.
     * 
     * @param transaction controling deletion of the list.
     * @throws ObjectManagerException
     */
    public void preDelete(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "preDelete",
                        "transaction=" + transaction + "(Transaction)");

        synchronized (headSequenceNumberLock) {
            tailSequenceNumberLock.lock();
            try {
                // Defect 322295
                // We only need to do this check at runtime. At recovery time we can rely on the
                // check been done before the OM went down. Also in some cases the subList array may
                // be padded with nulls depending on how many were successfully deleted from the
                // object store before the OM went down.
                if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog) {
                    // Check that the sublists are empty.
                    for (int i = 0; i < subLists.length; i++) {
                        // The list must be empty, apart from links this transaction will delete.
                        Token nextToken = subLists[i].head;
                        while (nextToken != null) {
                            LinkedList.Link nextLink = (LinkedList.Link) nextToken.getManagedObject();
                            if (nextLink.willBeDeleted(transaction))
                                nextToken = nextLink.next;
                            else {
                                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                    trace.exit(this, cclass,
                                               "preDelete",
                                               "via CollentionNotEmptyException"
                                                               + "size()=" + size() + "(long)"
                                                               + " nextLink.state=" + nextLink.state + "(int) " + LinkedList.Link.stateNames[nextLink.state]
                                                               + "\n nextLink.getTransactionLock()=" + nextLink.getTransactionLock() + "(TransactionLock)"
                                                    );

                                throw new CollectionNotEmptyException(this, size(), transaction);
                                //System.out.println("LinkedList.preDelete nextLink.state ="+nextLink.state+" "+LinkedList.Link.stateNames[nextLink.state]
                                //                                                                                              +"\n nextLink.getTransactionLock()="+nextLink.getTransactionLock() 
                                //                                                                                              );
                                //break;
                            } // if (  nextLink.state == Link.stateToBeDeleted...
                        } // while (nextToken != null).

                    } // for subLists.
                } // if not recovering...

                super.preDelete(transaction); // Also takes a lock.
                // Visibility is now restricted to our transaction only.

                // Now delete the sublists. 
                // If we are recovering, the delete of the sublists will have already been done
                // so don't do it again.
                if (transaction.getObjectManagerStateState() != ObjectManagerState.stateReplayingLog) {
                    transaction.optimisticReplace(null, null, java.util.Arrays.asList(subLists), null);
                } // if not recovering...

            } finally {
                if (tailSequenceNumberLock.isHeldByCurrentThread())
                    tailSequenceNumberLock.unlock();
            } // try (tailSequenceNumberLock).
        } // synchronized (headSequenceNumberLock).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "preDelete");
    } // End of preDelete method.

    // --------------------------------------------------------------------------
    // Simplified serialization.
    // --------------------------------------------------------------------------

    /**
     * No argument constructor.
     * 
     * @exception ObjectManagerException
     */
    ConcurrentLinkedList()
        throws ObjectManagerException
    {
        super();
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this, cclass,
                        "<init>");
            trace.exit(this, cclass,
                       "<init>");
        }
    } // end of Constructor.

    private static final byte SimpleSerialVersion = 0;

    /*
     * (non-Javadoc)
     * 
     * @see int SimplifiedSerialization.getSignature()
     */
    public int getSignature()
    {
        return signature_ConcurrentLinkedList;
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
            trace.entry(this, cclass,
                        "writeObject",
                        "dataOutputStream=" + dataOutputStream);

        try {
            dataOutputStream.writeByte(SimpleSerialVersion);
            super.writeObject(dataOutputStream);

            dataOutputStream.writeInt(activeSubListCount);
            for (int i = 0; i < subListTokens.length; i++) {
                subListTokens[i].writeObject(dataOutputStream);
            } // for subLists.

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                "writeObject",
                                                exception,
                                                "1:1229:1.26");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "writeObject"
                           , "via PermanentIOException"
                                );
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "writeObject");
    } // Of writeObject().

    /*
     * (non-Javadoc)
     * 
     * @see SimplifiedSerialization.readObject(java.io.DataInputStream,ObjectManagerState)
     */
    public void readObject(java.io.DataInputStream dataInputStream,
                           ObjectManagerState objectManagerState)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "readObject",
                        "DataInputStream="
                                        + dataInputStream
                                        + ", ObjectManagerState="
                                        + objectManagerState);

        try {
            byte version = dataInputStream.readByte();
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this, cclass
                            , "readObject"
                            , "version=" + version + "(byte)"
                                );
            super.readObject(dataInputStream,
                             objectManagerState);

            activeSubListCount = dataInputStream.readInt();
            subListTokens = new Token[activeSubListCount];

            for (int i = 0; i < subListTokens.length; i++) {
                subListTokens[i] = Token.restore(dataInputStream,
                                                 objectManagerState);
            } // for subLists.    

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "readObject", exception, "1:1280:1.26");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "readObject"
                           , "via PermanentIOException"
                                );
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        // Force a full search when the first removeFirst operation occurs.
        headSequenceNumber = Long.MAX_VALUE;
        headSequenceNumberLock = new HeadSequenceNumberLock();
        tailSequenceNumber = 0;
        tailSequenceNumberLock = new TailSequenceNumberLock();
        subLists = new ConcurrentSubList[subListTokens.length];

        // Defect 322295
        // This check has been added to handle the case where a ConcurrentLinkedList 
        // has been deleted but only (some or all of) the sub list deletions were 
        // forced to the object store before a crash. 
        //
        //  i.e.  [subList del][subList del][OS Force][subList del][CLList del][crash]
        //
        // This results in the ConcurrentLinkedList having tokens that point to some sub 
        // lists that no longer have managed objects so the references being setup below 
        // could be null. At recovery time however these null references will be replaced
        // with DummyManagedObjects when each individual sub list delete is replayed by
        // the log. This results in a ClassCastException below.
        //
        // To avoid the exception we only add the real sub list managed objects to the 
        // array. This should be ok as once recovery has completed the entire linked list
        // should have been deleted so no exhaustive list of sub lists is required.
        //
        // TODO: Can we avoid this check by grouping transaction work into a single checkpoint?
        for (int i = 0; i < subListTokens.length; i++) {

            ManagedObject managedObject = subListTokens[i].getManagedObject();

            if (managedObject instanceof ConcurrentSubList)
            {
                subLists[i] = (ConcurrentSubList) managedObject;
            }
        } // for subList.

        // During recovery some of the sublists may not have completed their recovery when the concurrentList is found.
        // Also some of the links in the subLists may have already been deleted by later Transactions and so be 
        // unavailable during recovery.
        // Also if we look at the sublists during recovery we create cyclic dependancies between the overall 
        // lists and the subLists. We defer setting the tailSequenceNumber until first use of the list. 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "readObject");
    } // readObject().

//  // --------------------------------------------------------------------------
//  // implements java.io.Serializable
//  // --------------------------------------------------------------------------
// 
//  /**
//   * Customized deserialization.
//   *
//   * @param objectInputStream containing the serialized form of the Object.
//   * @throws java.io.IOException
//   * @throws java.lang.ClassNotFoundException
//   */
//  private void readObject(java.io.ObjectInputStream objectInputStream)
//          throws java.io.IOException
//                ,java.lang.ClassNotFoundException
//  {
//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//      trace.entry(this,cclass,
//                  "readObject",
//                  "ObjectInputStream=" + objectInputStream);
//
//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//      trace.exit(this,cclass,
//                 "readObject"
//                 ,"via UnsupportedOperationException");
//    throw new UnsupportedOperationException();
//  } // End of method readObject.

    // --------------------------------------------------------------------------
    // Inner Classes
    // -------------------------------------------------------------------------- 

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
        ConcurrentLinkedList list; // The outer list.
        ConcurrentSubList.Link head; // First element before the start of the list.
        long headSequenceNumber = 0;
        ConcurrentSubList.Link tail; // Final element after the end of the list.
        long tailSequenceNumber = Long.MAX_VALUE;

        /**
         * Constructor creates a subList.
         * 
         * @param list from which the subList is taken.
         * @param head the link before the head of the new subList,
         *            null implies the head of the list.
         * @param tail the link after the tail of the new subList,
         *            null implies the tail of the list..
         * @throws ObjectManagerException
         * 
         */
        private SubList(ConcurrentLinkedList list,
                        ConcurrentSubList.Link head,
                        ConcurrentSubList.Link tail)
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
                if (((ConcurrentSubList) (head.list)).concurrentListToken != list.getToken()
                    || head.state == LinkedList.Link.stateRemoved
                    || head.state == LinkedList.Link.stateDeleted) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "subList");
                    throw new SubListEntryNotInListException(this,
                                                             head);
                }
                headSequenceNumber = head.sequenceNumber;
            } // if (head != null).      

            if (tail != null) {
                if (((ConcurrentSubList) (tail.list)).concurrentListToken != list.getToken()
                    || tail.state == LinkedList.Link.stateRemoved
                    || tail.state == LinkedList.Link.stateDeleted) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "subList");
                    throw new SubListEntryNotInListException(this,
                                                             tail);
                }
                tailSequenceNumber = tail.sequenceNumber;
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
            return new SubListIterator(LinkedList.SubList.SubListIterator.VALUES);
        } // iterator().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.List#listIterator()
         */
        public ListIterator listIterator()
                        throws ObjectManagerException
        {
            return new SubListIterator(LinkedList.SubList.SubListIterator.VALUES);
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
            return new SubListIterator(LinkedList.SubList.SubListIterator.ENTRIES);
        } // entryIterator().

        public List.Entry addEntry(Token token,
                                   Transaction transaction)
                        throws ObjectManagerException
        {
            ConcurrentSubList.Link newLink = list.insert(token,
                                                         tail,
                                                         transaction);
            return newLink;
        } // addEntry().

        public List subList(Entry fromEntry,
                            Entry toEntry)
                        throws ObjectManagerException
        {
            return new SubList(list, (ConcurrentSubList.Link) fromEntry, (ConcurrentSubList.Link) toEntry);
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
         * A cursor for enumerating or iterating over a the subList.
         * similar to the java.util.ListIterator interface.
         * The cursor does not skip back to elements unlocked behind it when it is
         * moving forward through the list.
         * 
         * Locking Strategy.
         * -----------------
         * ListIterator is locked first if necessary, to protect the cursor. If the
         * overall list must be traversed or altered it is locked before ConcurrentSubList is locked.
         * The full hierarchy is: Iterator,InternalTransaction,HeadSequenceNumber,TailSequenceNumber,Sublist.
         * The iterator is never synchroized under transaction!
         */
        class SubListIterator
                        implements ListIterator
        {
            //  Types of Iterators.
            static final int VALUES = 1;
            static final int ENTRIES = 2;
            private int type;

            // Iterators over the separate subLists.
            private LinkedList.SubList.SubListIterator[] subListIterator = new LinkedList.SubList.SubListIterator[list.subLists.length];
            // The subList containing the overall cursor
            private int currentSubListIndex = 0;
            // The sequence number of the cursor. 
            private long currentSequenceNumber = 0;

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
                                "<init>",
                                new Object[] { new Integer(type) });

                this.type = type;
                // Create a cursor in each subList. We cannot use LinkedList.SubList to delimit our SubList because
                // the relevant delimiters in each LinkedList might not exist and we cannot simply use the head and 
                // tail of the LinkedList because they might ultimatey bracket elements that do not belong in our 
                // SubList.
                for (int i = 0; i < list.subLists.length; i++) {
                    subListIterator[i] = (LinkedList.SubList.SubListIterator) list.subLists[i].entrySet().iterator();
                } // for subLists.

                // Set the cursors to reflect the head if there is one.
                if (head != null) {
                    if (head.state == LinkedList.Link.stateRemoved
                        || head.state == LinkedList.Link.stateDeleted) {
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this,
                                       cclass,
                                       "<intit>");
                        throw new SubListEntryNotInListException(SubList.this,
                                                                 head);
                    } // if (   head.state...

                    // The sequence number of the cursor. 
                    currentSequenceNumber = head.sequenceNumber;

                    for (int i = 0; i < list.subLists.length; i++) {

                        // The subList containing the overall cursor.
                        if (list.subLists[i] == head.list) {
                            currentSubListIndex = i;
                            subListIterator[i].currentEntry = head;
                        } else {
                            while (subListIterator[i].hasNext()) {
                                ConcurrentSubList.Link next = (ConcurrentSubList.Link) subListIterator[i].next();
                                if (next.sequenceNumber > head.sequenceNumber) {
                                    subListIterator[i].previous();
                                    break;
                                }
                            }
                        }
                    } // for list.subLists...
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

                boolean nextExists = false; // Return value.
                // See if any subList has a valid next Link.
                for (int i = 0; i < list.subLists.length && !nextExists; i++) {

                    synchronized (list.subLists[i]) {
                        if (!subListIterator[i].beyondEndOfList) {
                            resetCursorIfNoLongerInList(i);
                            ConcurrentSubList.Link nextAvailableEntry = (ConcurrentSubList.Link) subListIterator[i].nextAvailable(transaction,
                                                                                                                                  LinkedList.SubList.SubListIterator.FORWARD);
                            if (nextAvailableEntry != null
                                && nextAvailableEntry != tail
                                && nextAvailableEntry.sequenceNumber <= tailSequenceNumber)
                                nextExists = true;
                        } // if (!subListIterator[i].beyondEndOfList).
                    } // synchronized (list.subLists[i]).

                } // for subList.

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "hasNext",
                               "return=" + nextExists);
                return nextExists;
            } // hasNext(Transaction).

            /*
             * (non-Javadoc)
             * 
             * @see com.ibm.ws.objectManager.Iterator#hasNext()
             */
            public synchronized boolean hasNext()
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "hasNext");

                boolean nextExists = false; // Return value.
                // See if any subList has a valid next Link.
                for (int i = 0; i < list.subLists.length && !nextExists; i++) {

                    synchronized (list.subLists[i]) {
                        if (!subListIterator[i].beyondEndOfList) {
                            resetCursorIfNoLongerInList(i);
                            ConcurrentSubList.Link nextAvailableEntry = (ConcurrentSubList.Link) subListIterator[i].nextAvailable(LinkedList.SubList.SubListIterator.FORWARD);
                            if (nextAvailableEntry != null
                                && nextAvailableEntry != tail
                                && nextAvailableEntry.sequenceNumber <= tailSequenceNumber)
                                nextExists = true;
                        } // if (!subListIterator[i].beyondEndOfList).
                    } // synchronized (list.subLists[i]).

                } // for subList.

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "hasNext",
                               new Object[] { new Boolean(nextExists) });
                return nextExists;
            } // hasNext().

            /**
             * @param transaction which determines the visibility of elements.
             * @return the next element, after advancing the cursor position.
             * @exception java.util.NoSuchElementException
             *                if already at the end of the visible list list, or the
             *                current element is deleted.
             * @throws ObjectManagerException
             */
            public synchronized Object next(Transaction transaction)
                            throws ObjectManagerException
            {
                return next(true, transaction);
            } // next().

            /**
             * @return the next dirty element, after advancing the cursor position.
             * @exception java.util.NoSuchElementException
             *                if already at the end of the visible list list.
             * @throws ObjectManagerException
             */
            public synchronized Object next()
                            throws ObjectManagerException
            {
                return next(false, null);
            } // next()

            /**
             * Search for the next entry in the overall list.
             * 
             * @param clean true if a clean seach is made, false if a dirty search is made.
             * @param transaction which determines the visibility of elements, ignored if the
             *            search is dirty.
             * 
             * @return the next element, after advancing the cursor position.
             * @exception java.util.NoSuchElementException
             *                if already at the end of the visible list list, or the
             *                current element is deleted.
             * @throws ObjectManagerException
             */
            private Object next(boolean clean, Transaction transaction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "next",
                                new Object[] { transaction });

                // Search for the list that now contains the overall next link, start with the current sublist
                // to see if there is a link with the same or equal sequence number there. 
                // Stop when we have looked at all subLists.
                long nextSequenceNumber = currentSequenceNumber;
                long bestSequenceNumber = Long.MAX_VALUE;
                int index = currentSubListIndex;
                int firstIndex = index;
                int bestIndex = -1;
                ConcurrentSubList.Link overallNext = null; // The true next link.
                Object returnObject = null;

                do {
                    synchronized (list.subLists[index]) {
                        if (subListIterator[index].beyondEndOfList) {
                            // See if new, later Entries have been added since we went beyond the end of the list.
                            ConcurrentSubList.Link link = (ConcurrentSubList.Link) list.subLists[index].previousLink(null);
                            while (link != null
                                   && link.sequenceNumber >= nextSequenceNumber) {
                                subListIterator[index].currentEntry = link;
                                subListIterator[index].beyondEndOfList = false;
                                link = (ConcurrentSubList.Link) list.subLists[index].previousLink(link);
                            } //  while(nextLink.sequenceNumber...              
                        } // if (subListIterator[index].beyondEndOfList).

                        if (!subListIterator[index].beyondEndOfList) {
                            resetCursorIfNoLongerInList(index);

                            ConcurrentSubList.Link next = null;
                            if (clean) {
                                // TODO transaction could hide the tail and cause us to step over it!
                                next = (ConcurrentSubList.Link) subListIterator[index].nextAvailable(transaction,
                                                                                                     LinkedList.SubList.SubListIterator.FORWARD);
                            } else {
                                next = (ConcurrentSubList.Link) subListIterator[index].nextAvailable(LinkedList.SubList.SubListIterator.FORWARD);
                            }

                            if (next != null
                                && next != tail) {
                                if (next.sequenceNumber == nextSequenceNumber) {
                                    // We have a match on the predicted sequenceNumber.
                                    bestSequenceNumber = next.sequenceNumber;
                                    bestIndex = index;
                                    overallNext = next;
                                    // Assign the returnObject while we have the sublistLocked, in case the transaction backs out 
                                    // and the link is removed before we return.
                                    returnObject = returnObject(overallNext);
                                    break;

                                } else if (next.sequenceNumber < bestSequenceNumber
                                           && next.sequenceNumber <= tailSequenceNumber) {
                                    // In case all subLists have moved past the predicted sequenceNumber update the best so far.
                                    bestSequenceNumber = next.sequenceNumber;
                                    bestIndex = index;
                                    overallNext = next;
                                    // Assign the returnObject while we have the sublistLocked, in case the transaction backs out 
                                    // and the link is removed before we return.
                                    returnObject = returnObject(overallNext);
                                } // if (next.sequenceNumber > nextSequenceNumber).
                            } // if (next != null).

                        } // if (!subListIterator[index].beyondEndOfList).
                    } // synchronized (subLists[index]).

                    nextSequenceNumber++;
                    index++;
                    if (index == list.subLists.length)
                        index = 0;
                } while (index != firstIndex);

                if (overallNext == null) { // Was an overallNext found?
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "next",
                                   "via java.util.NoSuchElementException");
                    throw new java.util.NoSuchElementException();
                } // if (overallNext == null).

                // A next link was found.
                currentSequenceNumber = bestSequenceNumber;
                currentSubListIndex = bestIndex;
                subListIterator[bestIndex].currentEntry = overallNext;
                subListIterator[bestIndex].nextEntry = null;

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "next",
                               "return=" + returnObject);
                return returnObject;
            } // next().

            /**
             * Check to see if we need to reset the cursor, according to the sequenceNumber,
             * otherwise a transaction that removed an entry from a sublist not holding the true cursor
             * could cause a ConcurrentModificationExcepton.
             * 
             * @param index into the subLists array.
             * @throws ObjectManagerException
             */
            private void resetCursorIfNoLongerInList(int index)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this,
                                cclass,
                                "resetCursorIfNoLongerInList",
                                new Object[] { subListIterator[index].currentEntry });

                if (subListIterator[index].currentEntry != null
                    && (subListIterator[index].currentEntry.state == Entry.stateRemoved
                    || subListIterator[index].currentEntry.state == Entry.stateDeleted)) {

                    ConcurrentSubList.Link next = null;
                    ConcurrentSubList.Link previous = null;
                    next = (ConcurrentSubList.Link) list.subLists[index].nextLink(next);
                    while (next != null && next.sequenceNumber < currentSequenceNumber) {
                        previous = next;
                        next = (ConcurrentSubList.Link) list.subLists[index].nextLink(next);
                    }
                    subListIterator[index].currentEntry = previous;
                    subListIterator[index].nextEntry = null;

                } // if (   subListIterator[index].currentEntry.state...

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "resetCursorIfNoLongerInList",
                               new Object[] { subListIterator[index].currentEntry });
            } // resetCursorIfNoLongerInList.

            /*
             * (non-Javadoc)
             * 
             * @see ws.sib.objectManager.ListIterator#remove(ws.sib.objectManager.Transaction)
             */
            public synchronized Object remove(Transaction transaction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "remove",
                                "Transaction=" + transaction);

                ConcurrentSubList.Link link = (ConcurrentSubList.Link) subListIterator[currentSubListIndex].remove(transaction);
                Object returnObject = returnObject(link);

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "remove",
                               new Object[] { returnObject });
                return returnObject;
            } // remove().

            /**
             * Gives the Object of the correct type returned by this iterator.
             * 
             * @param link which holds the return Object.
             * @return Object of the type given up by this iterator.
             */
            private Object returnObject(ConcurrentSubList.Link link)
            {
                switch (type) {
                    case VALUES:
                        return link.data;

                    case ENTRIES:
                        return (List.Entry) link;

                    default:
                        return null;
                } // switch (type).
            } // returnObject().

            /*
             * (non-Javadoc)
             * 
             * @see ws.sib.objectManager.ListIterator#hasPrevious(ws.sib.objectManager.Transaction)
             */
            public boolean hasPrevious(Transaction transaction)
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "hasPrevious",
                                new Object[] { transaction });

                boolean nextExists = false; // Return value.
                // See if any subList has a valid next Link.
                for (int i = 0; i < list.subLists.length && !nextExists; i++) {

                    synchronized (list.subLists[i]) {
                        if (subListIterator[i].beyondEndOfList || subListIterator[i].currentEntry != null) {
                            ConcurrentSubList.Link nextAvailableEntry = (ConcurrentSubList.Link) subListIterator[i].nextAvailable(transaction,
                                                                                                                                  LinkedList.SubList.SubListIterator.BACKWARD);
                            if (nextAvailableEntry != null
                                && nextAvailableEntry != head
                                && nextAvailableEntry.sequenceNumber >= headSequenceNumber)
                                nextExists = true;
                        } // if (subListIterator[i].beyondEndOfList).
                    } // synchronized (list.subLists[i]).

                } // for subList.

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "hasPrevious",
                               new Object[] { new Boolean(nextExists) });
                return nextExists;
            } // hasPrevious().

            /*
             * (non-Javadoc)
             * 
             * @see ws.sib.objectManager.ListIterator#hasPrevious()
             */
            public boolean hasPrevious()
                            throws ObjectManagerException
            {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.entry(this, cclass,
                                "hasPrevious");

                boolean nextExists = false; // Return value.
                // See if any subList has a valid next Link.
                for (int i = 0; i < list.subLists.length && !nextExists; i++) {

                    synchronized (list.subLists[i]) {
                        if (subListIterator[i].beyondEndOfList || subListIterator[i].currentEntry != null) {
                            ConcurrentSubList.Link nextAvailableEntry = (ConcurrentSubList.Link) subListIterator[i].nextAvailable(LinkedList.SubList.SubListIterator.BACKWARD);
                            if (nextAvailableEntry != null
                                && nextAvailableEntry != head
                                && nextAvailableEntry.sequenceNumber <= headSequenceNumber)
                                nextExists = true;
                        } // if (subListIterator[i].beyondEndOfList).
                    } // synchronized (list.subLists[i]).

                } // for subList.

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass,
                               "hasPrevious",
                               new Object[] { new Boolean(nextExists) });
                return nextExists;
            } // hasPrevious().

            /*
             * (non-Javadoc)
             * 
             * @see ws.sib.objectManager.ListIterator#previous(ws.sib.objectManager.Transaction)
             */
            public Object previous(Transaction transaction)
            {
                // TODO Auto-generated method stub
                return null;
            }

            /*
             * (non-Javadoc)
             * 
             * @see ws.sib.objectManager.ListIterator#previous()
             */
            public Object previous()
            {
                // TODO Auto-generated method stub
                return null;
            }

            /*
             * (non-Javadoc)
             * 
             * @see ws.sib.objectManager.ListIterator#nextIndex(ws.sib.objectManager.Transaction)
             */
            public long nextIndex(Transaction transaction)
            {
                throw new UnsupportedOperationException();
            } // nextIndex().

            /*
             * (non-Javadoc)
             * 
             * @see ws.sib.objectManager.ListIterator#previousIndex(ws.sib.objectManager.Transaction)
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

                subListIterator[currentSubListIndex].set(newToken, transaction);

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
                    synchronized (list.subLists[currentSubListIndex]) {
                        // Cursor must still be in the list.
                        if (subListIterator[currentSubListIndex].beyondEndOfList) {
                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this, cclass
                                           , "remove"
                                           , "via IllegalStateException"
                                                );
                            throw new IllegalStateException();
                        } // if (beyondEndOfList).

                        // Was the cursor deleted.
                        if (subListIterator[currentSubListIndex].currentEntry.state == Entry.stateRemoved
                            || subListIterator[currentSubListIndex].currentEntry.state == Entry.stateDeleted) {
                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this,
                                           cclass,
                                           "add",
                                           "via java.util.ConcurrentModificationException");
                            throw new java.util.ConcurrentModificationException();
                        } // (currentEntry.state...

                        // Move the cursor forward to the inserted link.
                        ConcurrentSubList.Link newLink = list.insert(newToken,
                                                                     (ConcurrentSubList.Link) subListIterator[currentSubListIndex].currentEntry,
                                                                     transaction);
                        for (int i = 0; i < list.subLists.length; i++) {
                            if (list.subLists[i] != newLink.list) {
                                subListIterator[currentSubListIndex].currentEntry = newLink;
                                currentSubListIndex = i;
                                break;
                            }
                        }
                    } // synchronized (list.subLists[currentSubListIndex]).
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
                return new SubListIterator(LinkedList.SubList.SubListIterator.ENTRIES);
            }
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
            return entryIterator();
        } // iterator().

        public long size(Transaction transaction)
                        throws ObjectManagerException
        {
            return ConcurrentLinkedList.this.size(transaction);
        } // size().

        public long size()
                        throws ObjectManagerException
        {
            return ConcurrentLinkedList.this.size();
        } // size().
    } // class EntrySet.

    /**
     * ReentrantLock supporting explicit lock and unlock.
     * 
     * @version 1.01 15 Feb 1996
     * @author Andrew Banks
     * 
     * @see java.util.concurrent.locks.ReentrantLock
     */
    static protected class ReentrantLock
    {
        // Number of times the locking thread has entered this lock.
        private int locked = 0;
        private Thread lockingThread;
        private int waiters = 0;

        /**
         * Creates a Lock.
         */
        private ReentrantLock()
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "<init>");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>");
        } // ReentrantLock().

        /**
         * Take the lock, blocks if necessary.
         * 
         * @see java.util.concurrent.locks.ReentrantLock#lock()
         */
        void lock()
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "lock",
                            new Object[] { new Integer(locked),
                                          lockingThread,
                                          new Integer(waiters) });

            Thread thisThread = Thread.currentThread();
            synchronized (this) {
                while (lockingThread != thisThread && lockingThread != null) {

                    waiters++; // Count the waiting threads.
                    // Repeat attempts to wait, in case we are interrupted by thread.interrupt().
                    for (;;) {
                        try {
                            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                                trace.debug(this,
                                            cclass,
                                            "lock",
                                            new Object[] { "About to wait():2174",
                                                          new Integer(waiters) });
                            wait();
                            break;

                        } catch (InterruptedException exception) {
                            // No FFDC Code Needed.
                            ObjectManager.ffdc.processException(this,
                                                                cclass,
                                                                "lock",
                                                                exception,
                                                                "1:2185:1.26");

                            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                                trace.event(this,
                                            cclass,
                                            "addBuffers",
                                            exception);
                        } // catch (InterruptedException exception).
                    } // for (;;).

                    waiters--; // Count the waiting threads.
                } // while (lockingThread != thisThread...

                locked++;
                lockingThread = thisThread;
            } // synchronized (this).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "lock");
        } // lock().

        /**
         * Release the lock.
         * 
         * @see java.util.concurrent.locks.ReentrantLock#unlock()
         */
        void unlock()
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "unlock",
                            new Object[] { new Integer(waiters) });

            synchronized (this) {
                if (lockingThread != Thread.currentThread()) {
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "unlock"
                                   , new Object[] { lockingThread });
                    throw new IllegalMonitorStateException("Holder:" + lockingThread);
                }
                locked--;
                if (locked == 0) {
                    lockingThread = null;
                    if (waiters > 0)
                        notify();
                } // if (locked == 0).
            } // synchronized (this).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "unlock");
        } // unLock().

        boolean isHeldByCurrentThread()
        {
            return (lockingThread == Thread.currentThread());
        }
    } // inner class Lock.
} // Of class ConcurrentLinkedList.
