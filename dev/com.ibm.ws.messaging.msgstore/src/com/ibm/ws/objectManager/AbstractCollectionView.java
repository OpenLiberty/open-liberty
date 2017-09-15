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
 * The root of the Abstract collection viewss, depends on iterator, which must be implemented,
 * as the basis of its operation. This provides a view of an underlying collection
 * and does not create a new persistent collection by extending managedObject.
 * 
 * @See Collection.
 * @See AbstractCollection.
 */
abstract class AbstractCollectionView
                implements Collection, Printable
{
    private static final Class cclass = AbstractCollectionView.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(AbstractCollectionView.class,
                                                                     ObjectManagerConstants.MSG_GROUP_OBJECTS);

    /**
     * Default no argument constructor.
     */
    protected AbstractCollectionView()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
        {
            trace.entry(this, cclass, "<init>");
            trace.exit(this, cclass, "<init>");
        }
    } // AbstractCollectionView().

    // ----------------------------------------------------------------------
    // Abstract methods.
    // ----------------------------------------------------------------------

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
     * @see com.ibm.ws.objectManager.Collection#size(com.ibm.ws.objectManager.Transaction)
     */
    public long size(Transaction transaction)
                    throws ObjectManagerException
    {
        long size = 0;
        for (Iterator iterator = iterator(); iterator.hasNext(transaction); iterator.next(transaction)) {
            size++;
            if (size == Long.MAX_VALUE)
                break;
        } // for...
        return size;
    } // size().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#size()
     */
    public long size()
                    throws ObjectManagerException
    {
        long size = 0;
        for (Iterator iterator = iterator(); iterator.hasNext(); iterator.next()) {
            size++;
            if (size == Long.MAX_VALUE)
                break;
        } // for...
        return size;
    } // size().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#isEmpty(com.ibm.ws.objectManager.Transaction)
     */
    public boolean isEmpty(Transaction transaction)
                    throws ObjectManagerException
    {
        return !iterator().hasNext(transaction);
    } // isEmpty().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#contains(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public boolean contains(Token token,
                            Transaction transaction)
                    throws ObjectManagerException
    {
        try {
            for (Iterator iterator = iterator(); iterator.next(transaction) != token;);
            return true;
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            return false;
        } // try.   
    } // contains().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#toArray(com.ibm.ws.objectManager.Transaction)
     */
    public synchronized Token[] toArray(Transaction transaction)
                    throws ObjectManagerException
    {
        // Throws ArrayIndexOutOfBoundsException if size is larger than Integer.MAX_VALUE,
        // because i++ will wrap and go negative.
        Token[] tokens = new Token[(int) size(transaction)];
        int i = 0;
        try {
            for (Iterator iterator = iterator();; tokens[i++] = (Token) iterator.next(transaction));
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            // We have run off the end of the collection.
        } // try.
        return tokens;
    } // toArray().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#add(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public boolean add(Token token,
                       Transaction transaction)
                    throws ObjectManagerException
    {
        throw new UnsupportedOperationException();
    } // add().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#remove(com.ibm.ws.objectManager.Token, com.ibm.ws.objectManager.Transaction)
     */
    public boolean remove(Token token,
                          Transaction transaction)
                    throws ObjectManagerException
    {
        try {
            Iterator iterator = iterator();
            while (iterator.next(transaction) != token);
            iterator.remove(transaction);
            return true;
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            return false;
        } // try.
    } // remove().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#containsAll(com.ibm.ws.objectManager.Collection, com.ibm.ws.objectManager.Transaction)
     */
    public boolean containsAll(Collection otherCollection,
                               Transaction transaction)
                    throws ObjectManagerException
    {
        try {
            for (Iterator iterator = otherCollection.iterator(); contains((Token) iterator.next(transaction), transaction););
            return false;
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            return true;
        } // try.
    } // containsAll().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#addAll(com.ibm.ws.objectManager.Collection, com.ibm.ws.objectManager.Transaction)
     */
    public boolean addAll(Collection otherCollection,
                          Transaction transaction)
                    throws ObjectManagerException
    {

        boolean modified = false;
        try {
            for (Iterator iterator = otherCollection.iterator();;)
                modified = add((Token) iterator.next(transaction), transaction);
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            // We have run off the end of the other collection.
        } // try.   
        return modified;
    } // addAll().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#removeAll(com.ibm.ws.objectManager.Collection, com.ibm.ws.objectManager.Transaction)
     */
    public boolean removeAll(Collection otherCollection,
                             Transaction transaction)
                    throws ObjectManagerException
    {
        boolean modified = false;
        try {
            for (Iterator iterator = iterator();;) {
                if (otherCollection.contains((Token) iterator.next(transaction), transaction)) {
                    iterator.remove(transaction);
                    modified = true;
                }
            }
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            // We have run off the end of the other collection.
        } // try.   
        return modified;
    } // removeAll().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#retainAll(com.ibm.ws.objectManager.Collection, com.ibm.ws.objectManager.Transaction)
     */
    public boolean retainAll(Collection otherCollection
                             , Transaction transaction
                    )
                                    throws ObjectManagerException
    {
        boolean modified = false;
        try {
            for (Iterator iterator = iterator();;) {
                if (!otherCollection.contains((Token) iterator.next(transaction), transaction)) {
                    iterator.remove(transaction);
                    modified = true;
                }
            } // for...      
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            // We have run off the end of the other collection.
        } // try.
        return modified;
    } // retainAll().

    public void clear(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "clear",
                        new Object[] { transaction });

        try {
            for (Iterator iterator = iterator();;) {
                iterator.next(transaction);
                iterator.remove(transaction);
            } // for...  
        } catch (java.util.NoSuchElementException exception) {
            // No FFDC code needed.
            // We have run off the end of the other collection.
        } // try. 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "clear");
    } // clear().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#print(java.io.PrintWriter)
     */
    public synchronized void print(java.io.PrintWriter printWriter)
    {
        try {
            printWriter.println("State Dump for:" + cclass.getName()
                                + "\n size()=" + size() + "(long)" + " size(null)=" + size(null) + "(long)"
                            );
            printWriter.println();

            printWriter.println("Tokens in order...");
            try {
                for (Iterator iterator = iterator();; printWriter.println(((Token) iterator.next(null)).toString()));
            } catch (java.util.NoSuchElementException exception) {
                // No FFDC code needed.
                // We have run off the end of the other collection.
            } // try.
        } catch (ObjectManagerException objectManagerException) {
            // No FFDC code needed.
            printWriter.println("Caught objectManagerException=" + objectManagerException);
            objectManagerException.printStackTrace(printWriter);
        } // try...

        printWriter.println();

    } // print().
} // AbstractCollectionView.
