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
 * A recoverable, doubly linked list. In order to avoid locking the list during
 * the prepare phase we allow updates on prepared links. This means we cannot
 * use the default Object manager behaviour of restoring a before image of the
 * modified links if the transacton backs out. Instead we mark links for
 * addition and deletion and if they are added we must be prepared to write new
 * links to remove them if the transaction is backed out.
 * 
 * The list is protected by synchronizing on the list.
 * 
 * @version 1.01 15 Feb 1996
 * @author Andrew Banks
 */
class ConcurrentSubList extends LinkedList
{
    private static final Class cclass = ConcurrentSubList.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_LISTS);

    private static final long serialVersionUID = -6402774319655418538L;

    protected Token concurrentListToken; // The owning List.

    // Cannot keep a concurrentList ManagedObject as this causes a ciurcular
    // restore at readObject time.

    /**
     * Constructor creates an empty list.
     * 
     * @param concurrentListToken for the ConcurrentLinkedList which owns this sibList.
     * @param transaction under which the new list is created.
     * @param objectStore where the list is stored.
     * @throws ObjectManagerException
     */
    protected ConcurrentSubList(Token concurrentListToken,
                                Transaction transaction,
                                ObjectStore objectStore)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { concurrentListToken,
                                                                transaction,
                                                                objectStore });

        // Cannot use the superclass constructor because it would do the
        // transaction.add() before we have set up concurrentListToken.
        this.concurrentListToken = concurrentListToken;

        objectStore.allocate(this);
        reservedSpaceInStore = maximumSerializedSize() + owningToken.objectStore.getAddSpaceOverhead();
        owningToken.objectStore.reserve((int) reservedSpaceInStore, true);
        transaction.add(this);
        owningToken.objectStore.reserve(-(int) reservedSpaceInStore, false);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // ConcurrentSubList().

    /**
     * @return long The number of bytes of logSpace to reserve for an addition to the list so that the later stages can
     *         complete.
     */
    static long logSpaceForAdd()
    {
        return (TransactionOptimisticReplaceLogRecord.maximumSerializedSize()
                + FileLogOutput.partHeaderLength
                + ConcurrentSubList.maximumSerializedSize() + 2 * ConcurrentSubList.Link.maximumSerializedSize()

                + TransactionCheckpointLogRecord.maximumSerializedSize()
                + FileLogOutput.partHeaderLength
                + Token.maximumSerializedSize() * 3);
    } // logSpaceForAdd().

    /**
     * @return long The number of bytes of logSpace to reserve for an deletion from the list so that the later stages can
     *         complete.
     */
    static long logSpaceForDelete()
    {
        return (TransactionOptimisticReplaceLogRecord.maximumSerializedSize()
                + FileLogOutput.partHeaderLength
                + Token.maximumSerializedSize() * 1 // For notify.
                + ConcurrentSubList.maximumSerializedSize() + 2 * ConcurrentSubList.Link.maximumSerializedSize()

                + TransactionCheckpointLogRecord.maximumSerializedSize()
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
        return ConcurrentSubList.maximumSerializedSize() // List header
               + 2 * ConcurrentSubList.Link.maximumSerializedSize() // Previous,Next links  
               + 3 * owningToken.objectStore.getAddSpaceOverhead(); // Store overhead for all of the above.
    } // storeSpaceForRemove()  

    /**
     * The space we reserve in the ObjectStore before we begin an add operation.
     * 
     * @return long a worst case estimate of the ObjectStore space needed to add an entry
     *         to this list.
     */
    long storeSpaceForAdd() {
        return ConcurrentSubList.maximumSerializedSize() // List header
               + 3 * ConcurrentSubList.Link.maximumSerializedSize() // Current,Previous,Next links  
               + 4 * owningToken.objectStore.getAddSpaceOverhead() // Store overhead for all of the above.
               + storeSpaceForRemove(); // In case we backoutTheAdd.
    } // storeSpaceForAdd()

    /**
     * Add the element to the list according to its sequenceNumber. Usually this
     * will be near at the current tail of the list.
     * The caller must already be snchronized on transaction.internalTransaction.
     * 
     * @param token to add.
     * @param transaction controling the update.
     * @param sequenceNumber of the link we are adding.
     * @param parentList owning this SubList.
     * @return List.Entry added or null.
     * @throws ObjectManagerException
     */
    protected Link addEntry(Token token,
                            Transaction transaction,
                            long sequenceNumber,
                            ConcurrentLinkedList parentList)
                    throws ObjectManagerException
    {
        final String methodName = "addEntry";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { token,
                                                                transaction,
                                                                new Long(sequenceNumber),
                                                                parentList});
        Link newLink;
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

            // TODO Do not need to do the following now that we hold the
            // tailSequenceNumberLock while we do the add.
            // Move backward through the list until we find an element that has a
            // sequence number
            // less than the one we want to add.
            Token previousToken = tail;
            Link previousLink = null;
            searchBackward: while (previousToken != null) {
                previousLink = (Link) previousToken.getManagedObject();
                if (previousLink.sequenceNumber <= sequenceNumber)
                    break searchBackward;
                previousToken = previousLink.previous;
            } // While (previousLink != null).

            Token nextToken = null;
            if (previousToken == null) // We are to go at the head of the list.
                nextToken = head; // May be null after us if the list is empty.
            else
                // Chain the remainder of the list after us.
                nextToken = previousLink.next;

            // Create the new link and add it to its object store.
            newLink = new Link(this, // Create the Link for the new object.
                               token, // Object referenced by the link.
                               previousToken,
                               nextToken,
                               transaction, // Transaction controling the addition.
                               sequenceNumber);
            managedObjectsToAdd.add(newLink);

            // Chain the new Link in the list. If the transaction backs
            // out we will rewrite the links to remove the new one.
            if (previousToken == null) { // First element in the list?
                head = newLink.getToken();
                availableHead = head;
            } else { // Not the head of the list.
                previousLink.next = newLink.getToken();
                managedObjectsToReplace.add(previousLink);
                // See if we should move the availableHead up the list ( towards the head ).
                if (availableHead == null) // All of the list ahead of us is to be
                                           // deleted.
                    availableHead = newLink.getToken();
                else if (((Link) availableHead.getManagedObject()).sequenceNumber > sequenceNumber)
                    availableHead = newLink.getToken();
            } // if (previousLink == null).

            if (nextToken == null) { // Anything after the insertPoint?
                tail = newLink.getToken(); // Last element in the list.
            } else { // Not the tail of the list.
                Link nextLink = (Link) nextToken.getManagedObject();
                nextLink.previous = newLink.getToken();
                managedObjectsToReplace.add(nextLink);
            } // if (nextToken == null).

            // clone(Transaction transaction, ObjectStore objectStore) does not lock the cloned list.
            if ( parentList.tailSequenceNumberLock.isHeldByCurrentThread())
              parentList.tailSequenceNumberLock.unlock();
      
            incrementSize(); // Adjust list length assuming we will commit.
            managedObjectsToReplace.add(this); // The anchor for the list.

            // Harden the updates. If the update fails, because the log is full,
            // we have affected the structure of the list so we will have to 
            // reverse the changes. Reserve enough space to reverse the update if the
            // transaction backs out.
            try {
                transaction.optimisticReplace(managedObjectsToAdd,
                                              managedObjectsToReplace,
                                              null, // No tokens to delete.
                                              null, // No tokens to notify.
                                              logSpaceForDelete());
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
//      } catch (ObjectStoreFullException exception) {
//        // No FFDC Code Needed, InternalTransaction has already done this.
//        // Remove the link we just added.
//        undoAdd(newLink);       
//        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//          trace.exit(this, cclass, methodName, exception);
//        throw exception;
            } // try.

        } // synchronized (this).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { newLink });
        return newLink;
    } // addEntry().

    // --------------------------------------------------------------------------
    // implements java.lang.Cloneable.
    // --------------------------------------------------------------------------
    /**
     * Make a shallow copy of the list, the copy contains all links visble to the
     * transaction.
     * 
     * @param transaction the transaction under which the new cloned list is created.
     * @param objectStore where the cloned list is stored.
     * @return a shallow copy of the list. This is a new list, the objects in the
     *         list are not themselves cloned.
     * @throws ObjectManagerException
     */
    public synchronized java.lang.Object clone(Transaction transaction,
                                               ObjectStore objectStore)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "clone",
                        new Object[] { transaction, objectStore });

        ConcurrentSubList clonedList = new ConcurrentSubList(concurrentListToken,
                                                             transaction,
                                                             objectStore);
        ConcurrentLinkedList concurrentLinkedList = (ConcurrentLinkedList)concurrentListToken.getManagedObject();   
    
        synchronized (transaction.internalTransaction) {
            // Capture the point at which the clone is made.
            long unlockPoint = getTransactionUnlockSequence();
            // Start at the top of the list.
            Link nextLink = (ConcurrentSubList.Link) nextLink(null,
                                                              transaction,
                                                              unlockPoint);
            // Build the new list by adding each element from the original list.
            while (nextLink != null) {
                clonedList.addEntry(nextLink.data,
                                    transaction,
                                    0,
                                    concurrentLinkedList);
                nextLink = (ConcurrentSubList.Link) nextLink(nextLink,
                                                             transaction,
                                                             unlockPoint);
            } // While (nextLink != null).
        } // synchronized (transaction.internalTransaction).

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
     * Replace the state of this object with the same object in some other
     * state. Used for to restore the before image if a transaction rolls back
     * or is read from the log during restart.
     * 
     * @param other
     *            is the object this object is to become a clone of.
     */
    public void becomeCloneOf(ManagedObject other)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "becomeCloneOf",
                        "Other="
                                        + other);

        super.becomeCloneOf(other);

        concurrentListToken = ((ConcurrentSubList) other).concurrentListToken;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "becomeCloneOf");
    } // End of becomeCloneOf method.

    // --------------------------------------------------------------------------
    // Simplified serialization.
    // --------------------------------------------------------------------------

    /**
     * No argument constructor.
     * 
     * @exception ObjectManagerException
     */
    ConcurrentSubList()
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

    // The serialized size of this.

    protected static long maximumSerializedSize()
    {
        return 1 // Version.
               + LinkedList.maximumSerializedSize()
               + Token.maximumSerializedSize() //ConcurrentList.
        ;
    }

    /*
     * (non-Javadoc)
     * 
     * @see int SimplifiedSerialization.getSignature()
     */
    public int getSignature()
    {
        return signature_ConcurrentSublist;
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
                        "DataOutputStream="
                                        + dataOutputStream);

        try {
            dataOutputStream.writeByte(SimpleSerialVersion);
            super.writeObject(dataOutputStream);
            // The owning ConcurrentLinkedlist.
            concurrentListToken.writeObject(dataOutputStream);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "witeObject", exception, "1:431:1.23");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "writeObject"
                           , "via PermanentIOException"
                                );
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        // Did we exceed the size limits?
        if (dataOutputStream.size() > maximumSerializedSize())
        {
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this, cclass
                            , "writeObject"
                            , "maximumSerializedSize()=" + maximumSerializedSize() + "(long)"
                              + "dataOutputStream.size()=" + dataOutputStream.size() + "(int)"
                                );
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
                        "dataInputStream="
                                        + dataInputStream
                                        + " objectManagerState="
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
            // Re-establish the owning ConcurrentLinkedList.
            concurrentListToken = Token.restore(dataInputStream,
                                                objectManagerState);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "readObject", exception, "1:497:1.23");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "readObject"
                           , "via PermanentIOException"
                                );
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

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
     * @param objectInputStream containing the serialized for of the Object.
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
                        "ObjectInputStream="
                                        + objectInputStream);

        // Super class already handled by serilaization.
        objectInputStream.defaultReadObject();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "readObject"
                       , "via UnsupportedOperationException");
        throw new UnsupportedOperationException();
    } // End of method readObject.

    /**
     * The Link in the chain holding the data the next and the previous Links in
     * the list.
     * 
     * @version 1.01 15 Feb 1996
     * @author Andrew Banks
     */
    static class Link
                    extends LinkedList.Link
    {
        private static final long serialVersionUID = 1012127769762316826L;

        protected long sequenceNumber;

        /**
         * Creates a link in the chain.
         * 
         * @param list that this link belongs to.
         * @param payload the link refers to.
         * @param previous link in the chain, or null if none.
         * @param next the following link in the chain, or null if none.
         * @param transaction which controls the addition of the new Link.
         * @param sequenceNumber of the link.
         * @exception ObjectManagerException
         */
        Link(ConcurrentSubList list,
             Token payload,
             Token previous,
             Token next,
             Transaction transaction,
             long sequenceNumber)
            throws ObjectManagerException
        {
            super(list,
                  payload,
                  previous,
                  next,
                  transaction);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "<init>",
                            new Object[] { list, payload, previous, next, transaction, new Long(sequenceNumber) });

            this.sequenceNumber = sequenceNumber;

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "<init>");
        } // Link().

        // --------------------------------------------------------------------------
        // extends LinkedList.Link.
        // --------------------------------------------------------------------------

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#preBackout(com.ibm.ws.objectManager.Transaction)
         */
        public void preBackout(Transaction transaction)
                        throws ObjectManagerException {
            final String methodName = "preBackout";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction });

            // No need to synchronize to protect state because only the locking
            // transaction can change it.
            switch (state) {
                case stateToBeAdded:
                    ConcurrentLinkedList concurrentLinkedList = (ConcurrentLinkedList) ((ConcurrentSubList) list).concurrentListToken.getManagedObject();

                    synchronized (concurrentLinkedList.headSequenceNumberLock) {
                        if (sequenceNumber == concurrentLinkedList.headSequenceNumber)
                            if (next == null)
                                concurrentLinkedList.headSequenceNumber++;
                            else {
                                ConcurrentSubList.Link nextLink = (ConcurrentSubList.Link) next.getManagedObject();
                                // If insertion into the body of the list has taken place,
                                // then a sequenceNumber will have been duplicated.
                                if (nextLink.sequenceNumber != concurrentLinkedList.headSequenceNumber)
                                    concurrentLinkedList.headSequenceNumber++;
                            }
                    } // synchronized (concurrentLinkedList.headSequenceNumber).

                    break;

            } // switch.

            super.preBackout(transaction);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // preBackout().

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.ManagedObject#backout(com.ibm.ws.objectManager.Transaction,boolean)
         */
        public void backout(Transaction transaction,
                            long logSequenceNumber,
                            boolean requiresCurrentCheckpoint)
                        throws ObjectManagerException {
            final String methodName = "backout";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction,
                                                                    new Long(logSequenceNumber),
                                                                    new Boolean(requiresCurrentCheckpoint) });
            // We need to find an lock the encompassing list headSequenceNumberLock before we invove the super class,
            // because it takes a lock on the sub list.
            ConcurrentLinkedList concurrentLinkedList = (ConcurrentLinkedList) ((ConcurrentSubList) list).concurrentListToken.getManagedObject();
            synchronized (concurrentLinkedList.headSequenceNumberLock) {
                // No need to synchronize to protect state because only the locking
                // transaction can change it.
                switch (state) {

                    case stateToBeDeleted: // We have canceled a delete of a link.
                        // If we have reappeared ahead of the head sequenceNumber set
                        // the headSequenceNumber back to this link.
                        if (sequenceNumber < concurrentLinkedList.headSequenceNumber) {
                            concurrentLinkedList.headSequenceNumber = sequenceNumber;
                        } // if (sequenceNumber < ...
                        break;

                } // switch.

                // Call the super class after we have looked at the state.
                super.backout(transaction,
                              logSequenceNumber,
                              requiresCurrentCheckpoint);
            } // synchronized (concurrentLinkedList.headSequenceNumber).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // backout().

        // --------------------------------------------------------------------------
        // extends ManagedObject.
        // --------------------------------------------------------------------------

        /**
         * Replace the state of this object with the same object in some other
         * state. Used for to restore the before image if a transaction rolls back
         * or is read from the log during restart.
         * 
         * @param other
         *            is the object this object is to become a clone of.
         */
        public void becomeCloneOf(ManagedObject other)
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            "becomeCloneOf",
                            "Other="
                                            + other);

            super.becomeCloneOf(other);

            // Links in a list use an optimistic update methodology and hence perform
            // backout by making
            // updates so we dont make the clone of a before image when backing out.
            if (!backingOut) { // Was transient state corrected in preBackout?
                Link otherLink = (Link) other;
                sequenceNumber = otherLink.sequenceNumber;
            } // if(!backingOut).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "becomeCloneOf");
        } // End of becomeCloneOf method.

        // ----------------------------------------------------------------------------------------
        // implements List.Entry.
        // ----------------------------------------------------------------------------------------

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.List.Entry#remove(com.ibm.ws.objectManager.Transaction)
         */
        public void remove(Transaction transaction)
                        throws ObjectManagerException {
            final String methodName = "remove";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass, methodName, new Object[] { transaction });

            ConcurrentLinkedList concurrentLinkedList = (ConcurrentLinkedList) ((ConcurrentSubList) list).concurrentListToken.getManagedObject();

            synchronized (transaction.internalTransaction) {
                synchronized (concurrentLinkedList.headSequenceNumberLock) {
                    synchronized (list) {

                        if (state == stateAdded) {
                            requestDelete(transaction); // Mark for deletion.
                            list.decrementAvailableSize();

                        } else if (state == stateToBeAdded && lockedBy(transaction)) {
                            requestDelete(transaction); // Mark for deletion.
                            // If the link was added by the same transaction do not decrement the available size because
                            // it has not yet been incremented.
                        } else {
                            InvalidStateException invalidStateException = new InvalidStateException(this,
                                                                                                    state,
                                                                                                    stateNames[state]);
                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this, cclass, methodName, new Object[] { invalidStateException,
                                                                                   new Integer(state),
                                                                                   stateNames[state] });
                            throw invalidStateException;
                        } // if (state == stateAdded).

                        // Update the headSequenceNumber, if it is the head of the overall list.
                        if (sequenceNumber == concurrentLinkedList.headSequenceNumber)
                            concurrentLinkedList.incrementHeadSequenceNumber(this);

                    } // synchronized (list).
                } // synchronized (concurrentLinkedList.headSequenceNumber).

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
                    unRemove(concurrentLinkedList);
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, exception);
                    throw exception;

                    // There should be no ObjectStoreFull exceptions for Transaction.delete.
                    // Space is reserved by the store itself for delete operations. 
//        } catch (ObjectStoreFullException exception) {
//          // No FFDC Code Needed, transaction.delete() has already done this.
//          unRemove(concurrentLinkedList);
//          if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//            trace.exit(this, cclass, methodName, exception);
//          throw exception;

                } catch (InvalidStateException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:783:1.23");
                    unRemove(concurrentLinkedList);
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass, methodName, exception);
                    throw exception;
                } // catch (LogFileFullException exception).
            } // synchronized (transaction.internalTransaction).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // remove().

        /**
         * Make a link visible again in a subList.
         * 
         * @param concurrentLinkedList owning this subList.
         * @throws StateErrorException
         * @throws ObjectManagerException
         */
        void unRemove(ConcurrentLinkedList concurrentLinkedList)
                        throws StateErrorException, ObjectManagerException {
            synchronized (concurrentLinkedList.headSequenceNumberLock) {
                synchronized (list) {
                    // Make the link visible again.
                    setState(nextStateForRequestUnDelete);
                    list.availableHead = list.head;
                    list.skipToBeDeleted();
                    if (state == stateAdded)
                        list.incrementAvailableSize(); // Adjust list visible length available.

                    // If we have reappeared ahead of the head sequenceNumber set
                    // the headSequenceNumber back to this link.
                    if (sequenceNumber < concurrentLinkedList.headSequenceNumber)
                        concurrentLinkedList.headSequenceNumber = sequenceNumber;
                } // synchronized (list).
            } // synchronized (concurrentLinkedList.headSequenceNumber).

            // Release space we reserved in the store.
            owningToken.objectStore.reserve(-(int) list.storeSpaceForRemove(), false);
        } // unRemove().

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
            super();
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
                trace.entry(this, cclass,
                            "<init>");
                trace.exit(this, cclass,
                           "<init>");
            }
        } // end of Constructor.

        private static final byte SimpleSerialVersion = 0;

        // The serialized size of this.
        protected static final long maximumSerializedSize()
        {
            return 1 // Version.
            + LinkedList.Link.maximumSerializedSize()
            + 8 // SequenceNumber
            ;
        } // maximumSerializedSize(). 

        /*
         * (non-Javadoc)
         * 
         * @see int SimplifiedSerialization.getSignature()
         */
        public int getSignature()
        {
            return signature_ConcurrentSublist_Link;
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
                trace.entry(this, cclass,
                            "writeObject",
                            new Object[] { dataOutputStream });

            try {
                dataOutputStream.writeByte(SimpleSerialVersion);
                super.writeObject(dataOutputStream);
                dataOutputStream.writeLong(sequenceNumber);

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "witeObject", exception, "1:881:1.23");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "writeObject"
                               , "via PermanentIOException"
                                    );
                throw new PermanentIOException(this,
                                               exception);
            } // catch (java.io.IOException exception).

            // Did we exceed the size limits?
            if (dataOutputStream.size() > maximumSerializedSize())
            {
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this, cclass
                                , "writeObject"
                                , "maximumSerializedSize()=" + maximumSerializedSize() + "(long)"
                                  + "dataOutputStream.size()=" + dataOutputStream.size() + "(int)"
                                    );
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
                trace.exit(this, cclass,
                           "writeObject");
        } // writeObject().

        /*
         * (non-Javadoc)
         * 
         * @see SimplifiedSerialization.readObject(java.io.DataInputStream,ObjectManagerState)
         */
        public void readObject(java.io.DataInputStream dataInputStream,
                               ObjectManagerState objectManagerState)
                        throws ObjectManagerException
        {
            final String methodName = "readObject";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass,
                            methodName,
                            new Object[] { dataInputStream, objectManagerState });

            try {
                byte version = dataInputStream.readByte();
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this, cclass,
                                methodName,
                                new Object[] { new Byte(version) });
                super.readObject(dataInputStream,
                                 objectManagerState);
                sequenceNumber = dataInputStream.readLong();
            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:940:1.23");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { exception });
                throw new PermanentIOException(this,
                                               exception);
            } // catch (java.io.IOException exception).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "readObject");
        } // readObject().

    } // Of inner class Link.

} // class ConcurrentSubList.
