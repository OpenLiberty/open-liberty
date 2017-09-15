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

/**
 * @author Andrew_Banks
 * 
 *         A list interface similar to the java.util.List interface which supports recoverable
 *         lists which are searched and modified under the scope fo Transactions.
 * @see java.util.List.
 */
public interface List
                extends Collection
{
    /**
     * Add the element at the current tail of the list. The new Token is visible to
     * this transaction immediately and to other transactions after this transaction
     * has committed. If the transaction backs out no addition takes place.
     * <p>
     * 
     * @param Token the handle to the ManagedObject to add, may be null.
     * @param Transaction controling the update.
     * @returns boolean true, the list is always modified on transaction commit.
     * @exception ObjectManagerException.
     */
    @Override
    boolean add(Token token,
                Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Appends the specified element to the tail of this list.
     * 
     * @see add(Token,Transaction).
     *      <p>
     * @param Token the handle to the ManagedObject to add, may be null.
     * @param Transaction controling the update.
     * @returns List.Entry, the contyainer of the Token within the list.
     * @exception ObjectManagerException.
     */
    List.Entry addEntry(Token token,
                        Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Retrieves, but does not delete the first Token in the list visible to a transaction.
     * 
     * @param Transaction controling visibility of the elements.
     * @return the first Token in the list.
     * @exception java.util.NoSuchElementException
     *                if there is nothing in the list or it has already been deleted by some other
     *                uncommited transaction.
     */
    public Token getFirst(Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Remove the Token at the head of the list. The Token is not visible to other
     * Transactions once this method returns. If the Transaction backs out it becomes
     * visible again. If the Transactions commits, the removal is completed and becomes
     * permanent.
     * 
     * @param Transaction cotroling the ultimate removal.
     * @return Token to be removed when thr Transaction commits.
     * @exception java.util.NoSuchElementException
     *                if there is nothing in the list or it has already been deleted by some other
     *                uncommited transaction.
     * @throws ObjectManagerException.
     */
    public Token removeFirst(Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Print a dump of the state.
     * 
     * @param java.io.PrintWriter to be written to.
     */
    @Override
    void print(java.io.PrintWriter printWriter)
                    throws ObjectManagerException;

    /**
     * Validates that the List structure is sound.
     * 
     * @param java.io.PrintStream where any inconsistencies are reported.
     * @return boolean true if the list is not corrupt.
     */
    public boolean validate(java.io.PrintStream printStream)
                    throws ObjectManagerException;

    /**
     * Returns a collection view of the List.Entry entries in this List. This is not a new List
     * and its does not extend ManagedObject, it is just a view of the underlying List.
     * <p>
     * Note that although the Set of Entries contains no duplicates of Tokens may contain duplicates.
     * <p>
     * 
     * @return Set which is a view of the List.Entry's contained in this List.
     * @throws ObjectManagerException.
     */
    Set entrySet()
                    throws ObjectManagerException;

    /**
     * A list Entry, the container for a Token in the List.
     * 
     * @See Map.Entry
     */
    interface Entry
    {
        /**
         * Returns the Token associated with the List.Entry.
         * 
         * @return Token associated with this Entry.
         */
        Token getValue();

        /**
         * Replaces the Token associated this Entry with the new Token, within the scope
         * of a Transaction. The new Token may be null.
         * The Entry must be visible to the updating Transaction.
         * 
         * @param Token to replace the existing one when the Transaction commits.
         *            The new Token is immediately visible to the Transaction the existing Token is visible to
         *            all Transactions.
         * @param Transaction which controls the update.
         * @return Token which was associated with this Entry.
         * 
         * @throws UnsupportedOperationException if unsupported by the List implementation.
         * @exception InvalidStateException if the Entry is not eligible for setting,
         *                for exampe because it has already been removed.
         * @throws ObjectManagerexception.
         */
        Token setValue(Token token,
                       Transaction transaction)
                        throws ObjectManagerException;

        /**
         * State definitions.
         * int stateError A state error has occured.
         * int stateConstructed Not yet part of a list.
         * int stateToBeAdded Part of the list but not available to other transactions.
         * int stateAdded Added, visible to all Transactions.
         * int stateNotAdded Added, but removed because of backout.
         * int stateToBeDeleted Will be removed from the list.
         * int stateMustBeDeleted Entry will be deleted regardless of the transaction outcome.
         * int stateRemoved Removed from the list.
         * int stateDeleted Removed from the list and deleted.
         * 
         * @author IBM Corporation
         */
        public static final int stateError = 0;
        public static final int stateConstructed = 1;
        public static final int stateToBeAdded = 2;
        public static final int stateAdded = 3;
        public static final int stateNotAdded = 4;
        public static final int stateToBeDeleted = 5;
        public static final int stateMustBeDeleted = 6;
        public static final int stateRemoved = 7;
        public static final int stateDeleted = 8;

        /**
         * Returns the state of the Enry.
         * 
         * @return int the state of this entry.
         * @throws ObjectManagerException.
         */
        int getEntryState()
                        throws ObjectManagerException;

        /**
         * Remove the entry from the list.
         * 
         * @param Transaction coordinating the removal.
         * @throws ObjectManagerException.
         */
        void remove(Transaction transaction)
                        throws ObjectManagerException;
    } // inner interface Entry. 

    /**
     * Find the index of the first occurrence of Token.
     * 
     * @param Token to find, may be null, in which case we find the first null.
     * @param Transaction controling visibility of Tokens.
     * @return long the index of the first occurrence of Token in this list.
     *         If the Token is not found return -1.
     *         If the Token contains more than Long.MAX_VALUE Tokens return Long.MAX_VALUE.
     */
    long indexOf(Token token,
                 Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Find the index of the last occurrence of Token.
     * 
     * @param Token to find, may be null, in which case we find the last null.
     * @param Transaction controling visibility of Tokens.
     * @return long the index of the first occurrence of Token in this list.
     *         If the Token is not found return -1.
     *         If the Token contains more than Long.MAX_VALUE Tokens return Long.MAX_VALUE.
     */
    long lastIndexOf(Token token,
                     Transaction transaction)
                    throws ObjectManagerException;

    /**
     * A cursor over Tokens in the list.
     * 
     * @return ListIterator for the list.
     * @exception ObjectManagerException.
     */
    ListIterator listIterator()
                    throws ObjectManagerException;

    /**
     * A ListIterator positioned at the specified index, according to the Transaction.
     * 
     * @param long the index to position the ListIterator at.
     * @param Transaction controling visibility in the list.
     * @return ListIterator the call next(Transaction) will return the Token at a depth equal to the index.
     * @throws IndexOutOfBoundsException if the index is negative or larger than the size of the list
     *             as viewed by the Transaction.
     */
    ListIterator listIterator(long index, Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Returns a cursor over the Entries in this list in sequence.
     * 
     * @return Iterator over the Entrys in this list in sequence.
     * @exception ObjectManagerException.
     * @deprecated use entrySet().iterator().
     */
    Iterator entryIterator()
                    throws ObjectManagerException;

    /**
     * Returns the part of this list between the head Entry (exclusive)
     * and Tail Entry (exclusive). A null head Entry indicates the head of the list,
     * null tail Entry indicates the tail of the list.
     * 
     * @param List.Entry head first Entry (exclusive) in the new subList.
     * @param List.Entry tail endpoint Entry (exclusive) in the subList.
     * @return List a view which is part of this list.
     * 
     * @throws SubListEntryNotInListException
     *             if either the fromEntry or the toEntry is not in the list.
     */
    List subList(List.Entry fromEntry,
                 List.Entry toEntry)
                    throws ObjectManagerException;
} // interface List.