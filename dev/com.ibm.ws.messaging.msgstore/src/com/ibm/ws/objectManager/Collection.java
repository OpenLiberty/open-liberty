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
 * The root interface for Collections based on the java.util.Collection interface
 * but extended to support transactions. A collection
 * represents a group of Tokens. Collections allow or disallow duplicate Tokens.
 * Some collections are ordered and others unordered.
 * 
 * @see java.util.*
 * @see Set
 * @see List
 * @see Map
 */

public interface Collection {
    /**
     * Count of the elements in this collection, visible to the Transaction.
     * If the collection contains more than <tt>Long.MAX_VALUE</tt> elements,
     * returns <tt>Long.MAX_VALUE</tt>.
     * <p>
     * 
     * @param Transaction which sees the elements. A null transaction implies the
     *            number of elements visible to all Transactions.
     * @return long the number of elements in this collection.
     * 
     * @exception ObjectManagerException.
     */
    long size(Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Returns the number of elements in this collection. This is
     * the dirty size including additions and deletions by curently
     * active Transactions. If the collection contains more than
     * <tt>Long.MAX_VALUE</tt> elements, returns <tt>Long.MAX_VALUE</tt>.
     * <p>
     * 
     * @return long the number of elements in this collection.
     * 
     * @exception ObjectManagerException.
     */
    long size()
                    throws ObjectManagerException;

    /**
     * Determine whether this collection contains no elements, according to
     * the Transaction.
     * <p>
     * 
     * @param Transaction which sees the element, null implies empty for any transaction.
     * @return boolean <tt>true</tt> if this collection contains no elements
     *         available to the Transaction.
     * @exception ObjectManagerException.
     */
    boolean isEmpty(Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Returns <tt>true</tt> if this collection contains the specified
     * token, which may be null.
     * <p>
     * 
     * @param Token present in the collection.
     * @param Transaction which sees the Token.
     * @return <tt>true</tt> if this collection contains the Token.
     * @exception ObjectManagerException.
     */
    boolean contains(Token token,
                     Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Returns an iterator over the elements in this collection. There are no
     * guarantees concerning the order in which the elements are returned
     * (unless this collection is an instance of some class that provides a
     * guarantee).
     * <p>
     * 
     * @return an <tt>Iterator</tt> over the elements in this collection
     */
    Iterator iterator()
                    throws ObjectManagerException;

    /**
     * Makes a new array containing all of the Tokens in this collection,
     * some of which may be null. The order in the array and the position
     * of any nulls is determined by the ordering guarantees of the collection.
     * <p>
     * 
     * @param Transaction which sees the elements in the array.
     * @return Token[] containing all of the elements in this
     *         collection visible to the transaction.
     * @exception ObjectManagerException.
     * @throws ArrayIndexOutOfBoundsException If the collecion
     *             contains more than <tt>Integer.MAX_VALUE</tt> elements.
     */
    Token[] toArray(Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Include the Token in the collection and make it visible to other transactions
     * when the transaction commits.
     * <p>
     * 
     * @param Token to be added to the collection under the scope of the
     *            Transaction.
     * @param Transaction controling the addition. If the transaction backs out
     *            the result is as if no addition was made.
     * @return boolean true if the collection is changed, false if it the Token is
     *         not added because duplicates are not allowed.
     * 
     * @throws UnsupportedOperationException if the <tt>add</tt> method is not
     *             supported by this collection.
     * 
     * @throws ObjectManagerException.
     */
    boolean add(Token token,
                Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Removes the first occurence of the Token from this collection
     * under the scope of the Transaction. One this method has completed the Token is
     * no longer visible to other Transactions. The actual removal only takes place
     * when the Transaction commits.
     * <p>
     * 
     * @param Token to be removed, if present, may be null.
     * @param Transaction controling the removal. If the Transaction
     *            backs out the removal does not take place and the Token becomes visible
     *            to other Transactions again.
     * @return boolean true if this Token was fond and removed.
     * 
     * @throws UnsupportedOperationException remove is not supported by this
     *             collection.
     * @exception ObjectManagerException.
     */
    boolean remove(Token token,
                   Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Determine if this collection contains all of the Tokens in the
     * other collection, according to the transaction.
     * <p>
     * 
     * @param Collection which should contain all of the Tokens in this collection.
     * @param Transaction which determines visibility of the Tokens, may be null.
     * @return boolean true if this collection contains all of the Tokens
     *         in the other collection
     * @exception ObjectManagerException.
     */
    boolean containsAll(Collection otherCollection,
                        Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Add all of the Tokens in the other collection to this collection.
     * <p>
     * 
     * @param c elements to be inserted into this collection.
     * @param Transaction which controls the addition.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call
     * 
     * @exception ObjectManagerException.
     */
    boolean addAll(Collection otherCollection,
                   Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Removes all of the Tokens in the other collection that are also present
     * in this one. The actual removal is completed when the Transaction commits.
     * 
     * @param Collection containing Tokens to be removed from this one.
     * @param Transaction controling visibility of Tokens and the completion
     *            of the update.
     * @return boolean true if this collection is modified.
     * 
     * @exception ObjectManagerException.
     */
    boolean removeAll(Collection otherCollection,
                      Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Keep only the Tokens in this collection that are also present in
     * the other Collection.
     * <p>
     * 
     * @param Collection containing the Tokens kept in this one.
     * @param Transaction controling visibility of Tokens and the commitment of the
     *            update.
     * @return boolean true if this collection modified.
     * 
     * @exception ObjectManagerException.
     */
    boolean retainAll(Collection otherCollection,
                      Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Removes all of the elements from this collection (optional operation).
     * This collection will be empty after this method returns unless it
     * throws an exception.
     * 
     * @throws UnsupportedOperationException if the <tt>clear</tt> method is
     *             not supported by this collection.
     * @exception ObjectManagerException.
     */
    void clear(Transaction transaction)
                    throws ObjectManagerException;

    /**
     * Print a dump of the state of this collection.
     * 
     * @param java.io.PrintWriter to be written to.
     */
    public void print(java.io.PrintWriter printWriter)
                    throws ObjectManagerException;
} // interface Collection. 
