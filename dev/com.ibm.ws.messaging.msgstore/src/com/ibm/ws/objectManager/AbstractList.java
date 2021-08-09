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
 * A starter implementation of a List. This implentation
 * creates another recoverable List by extending ManagedObject.
 * 
 * @see List
 * @see AbstractCollection
 * @see AbstractListView
 * 
 */
public abstract class AbstractList
                extends AbstractCollection
                implements List
{
    private static final Class cclass = AbstractList.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(AbstractList.class,
                                                                     ObjectManagerConstants.MSG_GROUP_LISTS);

    /**
     * Default no argument constructor.
     */
    protected AbstractList()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
        {
            trace.entry(this, cclass, "<init>");
            trace.exit(this, cclass, "<init>");
        }
    } // AbstractList().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#add(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public boolean add(Token token,
                       Transaction transaction)
                    throws ObjectManagerException
    {
        List.Entry entry = addEntry(token, transaction);
        return (entry != null);
    } // add().

    /**
     * Appends the specified element to the end of this list (optional operation).
     * <p>
     * 
     * Lists that support this operation may place limitations on what elements may be added to this list. In particular,
     * some lists will refuse to add null elements, and others will impose restrictions on the type of elements that may
     * be added. List classes should clearly specify in their documentation any restrictions on what elements may be
     * added.
     * 
     * @param object to be appended to this list.
     * @param transaction controling the update.
     * @return <tt>List.Entry</tt> created by the addition to the list, or null.
     * 
     * @throws UnsupportedOperationException
     *             if the <tt>add</tt> method is not supported by this list.
     * @throws ClassCastException
     *             if the class of the specified element prevents it from being added to this list.
     * @throws NullPointerException
     *             if the specified element is null and this list does not support null elements.
     * @throws IllegalArgumentException
     *             if some aspect of this element prevents it from being added to this list.
     * @exception ObjectManagerException
     */
    public List.Entry addEntry(Object object,
                               Transaction transaction)
                    throws ObjectManagerException
    {
        throw new UnsupportedOperationException();
    } // addEntry.

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

        Token first = (Token) iterator().next(transaction);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "getFirst",
                       new Object[] { first });
        return first;
    } // getFirst().

    /**
     * Remove the first element in the list.
     * 
     * @param transaction
     *            the transaction cotroling the ultimate removal.
     * @return the object removed.
     * @exception java.util.NoSuchElementException
     *                if there is nothing in the list or it has already been deleted by some other
     *                uncommited transaction.
     * @throws ObjectManagerException.
     */
    public Token removeFirst(Transaction transaction)
                    throws ObjectManagerException
    {
        Iterator iterator = entrySet().iterator();
        List.Entry entry = (List.Entry) iterator.next(transaction);
        iterator.remove(transaction);
        return entry.getValue();
    } // removeFirst().

    /**
     * Replaces the element at the specified position in this list with the specified element (optional operation).
     * <p>
     * 
     * This implementation always throws an <tt>UnsupportedOperationException</tt>.
     * 
     * @param long
     *        the index of element to replace.
     * @param element
     *            element to be stored at the specified position.
     * @param Transaction
     *            controling the update of the element in the List.
     * @return the element previously at the specified position.
     * 
     * @throws UnsupportedOperationException
     *             if the <tt>set</tt> method is not supported by this List.
     * @throws ClassCastException
     *             if the class of the specified element prevents it from being added to this list.
     * @throws IllegalArgumentException
     *             if some aspect of the specified element prevents it from being added to this list.
     * 
     * @throws IndexOutOfBoundsException
     *             if the specified index is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>).
     * @throws ObjectManagerException.
     */

    public Object set(long index,
                      Object element,
                      Transaction transaction)
                    throws ObjectManagerException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the element at the specified position in this list (optional operation). Shifts any subsequent elements to
     * the left (subtracts one from their indices). Returns the element that was removed from the list.
     * <p>
     * 
     * This implementation always throws an <tt>UnsupportedOperationException</tt>.
     * 
     * @param long
     *        the index of the element to remove.
     * @param Transaction
     *            controling the insertion of the element into the List.
     * @return the element previously at the specified position.
     * 
     * @throws UnsupportedOperationException
     *             if the <tt>remove</tt> method is not supported by this list.
     * @throws IndexOutOfBoundsException
     *             if the specified index is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>).
     * @throws ObjectManagerException.
     */
    public Object remove(long index,
                         Transaction transaction)
                    throws ObjectManagerException
    {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#indexOf(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public long indexOf(Token token,
                        Transaction transaction)
                    throws ObjectManagerException
    {
        long index = 0;
        try {
            for (Iterator iterator = iterator(); index < Long.MAX_VALUE; index++) {
                if (iterator.next(transaction) == token) {
                    break;
                }
            }
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            index = -1;
        } // try.
        return index;
    } // indexOf().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#lastIndexOf(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public long lastIndexOf(Token token,
                            Transaction transaction)
                    throws ObjectManagerException
    {
        // We search from the head of the list since any attempt to search from the tail inevitably 
        // involves scanning through the list to position ourselvs at the tail. If the list is
        // modified during the scan the index will be wrong.
        long index = 0;
        long lastIndex = -1;
        try {
            for (Iterator iterator = iterator(); index < Long.MAX_VALUE; index++) {
                if (iterator.next(transaction) == token) {
                    lastIndex = index;
                }
            }
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            index = -1;
        } // try.
        return lastIndex;
    } // lastIndexOf().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#iterator()
     */
    public abstract Iterator iterator()
                    throws ObjectManagerException;

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#listIterator()
     */
    public ListIterator listIterator()
                    throws ObjectManagerException
    {
        throw new UnsupportedOperationException();
    } // listIterator.

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#listIterator(long, com.ibm.ws.objectManager.Transaction)
     */
    public ListIterator listIterator(long index,
                                     Transaction transaction)
                    throws ObjectManagerException
    {

        if (index < 0)
            throw new IndexOutOfBoundsException("Index: " + index);

        ListIterator listIterator = listIterator();
        try {
            for (long i = 0; i < index; i++)
                listIterator.next(transaction);
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            throw new IndexOutOfBoundsException("Index: " + index);
        } // try.
        return listIterator;
    } // listIterator().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#subList(com.ibm.ws.objectManager.List.Entry, com.ibm.ws.objectManager.List.Entry)
     */
    public List subList(List.Entry fromEntry,
                        List.Entry toEntry)
                    throws ObjectManagerException
    {
        throw new UnsupportedOperationException();
    } // subList().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#entrySet()
     */
    public Set entrySet()
    {
        throw new UnsupportedOperationException();
    } // entrySet().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.List#validate(java.io.PrintStream)
     */
    public boolean validate(java.io.PrintStream printStream)
                    throws ObjectManagerException
    {
        throw new UnsupportedOperationException();
    } // validate().
} // class AbstractList.
