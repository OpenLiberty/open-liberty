/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.persistence;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.NoSuchElementException;

import javax.ejb.EJBObject;

import com.ibm.ws.ejb.portable.Constants;

public class PortableFinderEnumerator
                implements EnhancedEnumeration, Serializable {

    // This class is one of the 7 byvalue classes identified as part of the SUID mismatch
    // situation. Since this class,  and the other six classes implement Serializable,
    // the desire is that the container should own the process of marshalling and
    // demarshalling these classes. Therefore, the following buffer contents have been
    // agreed upon between AE WebSphere container and WebSphere390 container:
    //
    // |------- Header Information -----------||-- Object Contents --|
    // [ eyecatcher ][ platform ][ version id ][     Data Section    ]
    //     byte[4]       short          short      instance fields
    //
    // This class, and the other six, overide the default implementation of the
    // Serializable methods 'writeObject' and 'readObject'. The implementations
    // of these methods in each of the seven identified byvalue classes read
    // and write the buffer contents as mapped above for thier respective
    // classes.
    //

    //////////////////////////////////////////////////////////////////////////
    //
    // constants
    //

    /**
     * Number of elements to fetch on each request to the remote
     * result set
     */
    static final int PREFETCH_COUNT = 25;

    //////////////////////////////////////////////////////////////////////////
    //
    // constructors
    //

    /**
     * Constructor for a greedy-mode FinderEnumerator; elements contains
     * all of the EJBObjects in the result set, or is null if the enumeration
     * is empty.
     */
    PortableFinderEnumerator(EJBObject[] elements)
    {
        this.elements = elements;
        exhausted = true;
    }

    /**
     * Constructor for a lazy-mode FinderEnumerator; prefetchElements contains
     * the first few elements, prefetched from the result set; if exhausted
     * is false, enum is a RemoteEnumerator used to fetch the remaining
     * elements.
     */
    PortableFinderEnumerator(EJBObject[] prefetchElements, boolean exhausted,
                             RemoteEnumerator vEnum)
    {
        this.elements = prefetchElements;
        this.exhausted = exhausted;
        this.vEnum = vEnum;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Enumeration interface
    //

    /**
     * Obtain the next element from the enumeration
     */
    public Object nextElement()
    {
        try {
            return nextElementR();
        } catch (NoMoreElementsException e) {
            // FFDCFilter.processException(e, CLASS_NAME + ".nextElement", "109", this);
            throw new NoSuchElementException();
        } catch (EnumeratorException e) {
            // FFDCFilter.processException(e, CLASS_NAME + ".nextElement", "112", this);
            throw new RuntimeException(e.toString());
        } catch (NoSuchObjectException e) {
            // FFDCFilter.processException(e, CLASS_NAME + ".nextElement", "115", this);
            throw new IllegalStateException("Cannot access finder result outside transaction");
        } catch (RemoteException e) {
            // FFDCFilter.processException(e, CLASS_NAME + ".nextElement", "118", this);
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Find out if there are any more elements available
     */
    public boolean hasMoreElements()
    {
        try {
            return hasMoreElementsR();
        } catch (NoMoreElementsException e) {
            // FFDCFilter.processException(e, CLASS_NAME + ".hasMoreElements", "131", this);
            return false;
        } catch (EnumeratorException e) {
            // FFDCFilter.processException(e, CLASS_NAME + ".hasMoreElements", "134", this);
            throw new RuntimeException(e.toString());
        } catch (NoSuchObjectException e) {
            // FFDCFilter.processException(e, CLASS_NAME + ".hasMoreElements", "137", this);
            throw new IllegalStateException("Cannot access finder result outside transaction");
        } catch (RemoteException e) {
            // FFDCFilter.processException(e, CLASS_NAME + ".hasMoreElements", "140", this);
            throw new RuntimeException(e.toString());
        }
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // EnhancedEnumeration interface
    //

    /**
     * Find out if there are any more elements available; this method
     * will perform prefetching from the remote result set.
     */
    public synchronized boolean hasMoreElementsR()
                    throws RemoteException, EnumeratorException
    {
        if (elements != null && index < elements.length) {
            return true;

        } else if (!exhausted) {

            // Reset our local cache and attempt to fetch the next batch
            // from the server

            try {
                elements = null;
                index = 0;
                elements = fetchElements(PREFETCH_COUNT);
                return true;
            } catch (NoMoreElementsException ex) {
                // We've exhausted the remote result set
                // FFDCFilter.processException(ex, CLASS_NAME + ".hasMoreElementsR",
                //                             "172", this);
                return false;
            }

        }

        // Exhausted!
        return false;

    }

    /**
     * Obtain the next element from the enumeration
     */
    public synchronized Object nextElementR()
                    throws RemoteException, EnumeratorException
    {
        if (!hasMoreElementsR()) {
            throw new NoMoreElementsException();
        }

        return elements[index++];
    }

    /**
     * Obtain the next n elements from the enumeration; the array may
     * contain fewer than n elements if the enumeration is exhausted.
     */
    public synchronized Object[] nextNElements(int n)
                    throws RemoteException, EnumeratorException
    {
        if (!hasMoreElementsR()) {
            throw new NoMoreElementsException();
        }

        EJBObject[] remainder = null;

        final int numCached = elements.length - index;

        if (!exhausted && numCached < n) {

            // We must fetch more elements from the remote result set
            // to satisfy the request

            try {
                remainder = fetchElements(n - numCached);
            } catch (NoMoreElementsException ex) {
                // FFDCFilter.processException(ex, CLASS_NAME + ".nextNElements",
                //                             "220", this);
            }

        }

        final int numRemaining = remainder != null ? remainder.length : 0;
        final int totalAvail = numCached + numRemaining;
        final int numToReturn = Math.min(n, totalAvail);

        final EJBObject[] result = new EJBObject[numToReturn];

        final int numFromCache = Math.min(numToReturn, numCached);
        System.arraycopy(elements, index, result, 0, numFromCache);
        index += numFromCache;

        if (remainder != null) {
            System.arraycopy(remainder, 0, result, numFromCache, numRemaining);
        }

        return result;
    }

    /**
     * Expensive operation, we have to get all the remaining elements to
     * compute the size of the Collection. Defeats the purpose of having
     * lazy enumeration.
     */
    public int size() {
        loadEntireCollection();
        if (elements == null)
            return 0;
        else
            return (elements.length);
    }

    /**
     * Load the entire result set in a greedy fashion. This is required
     * to support methods on the Collection interface
     */
    public EJBObject[] loadEntireCollection() {

        EJBObject[] result = null;

        try {

            result = (EJBObject[]) allRemainingElements();

        } catch (NoMoreElementsException e) {

            // FFDCFilter.processException(e, CLASS_NAME + ".loadEntireCollection",
            //                             "270", this);
            return elements;

        } catch (EnumeratorException e) {

            // FFDCFilter.processException(e, CLASS_NAME + ".loadEntireCollection",
            //                             "276", this);
            throw new RuntimeException(e.toString());

        } catch (RemoteException e) {

            // FFDCFilter.processException(e, CLASS_NAME + ".loadEntireCollection",
            //                             "282", this);
            throw new RuntimeException(e.toString());
        }

        elements = result;
        return result;
    }

    /**
     * Obtain all of the remaining elements from the enumeration
     */
    public synchronized Object[] allRemainingElements()
                    throws RemoteException, EnumeratorException
    {
        if (!hasMoreElementsR()) {
            throw new NoMoreElementsException();
        }

        EJBObject[] remainder = null;

        if (!exhausted) {

            // We must fetch the remaining elements from the remote
            // result set

            try {
                //110799
                remainder = vEnum.allRemainingElements();
            } catch (NoMoreElementsException ex) {
                // FFDCFilter.processException(ex, CLASS_NAME + ".allRemainingElements",
                //                             "313", this);
            }

            catch (java.rmi.NoSuchObjectException exc) {

                // FFDCFilter.processException(exc, CLASS_NAME + ".allRemainingElements",
                //                             "319", this);

                throw new com.ibm.ejs.container.finder.CollectionCannotBeFurtherAccessedException("Cannot access finder result outside transaction");

            }

            //110799
            finally {
                exhausted = true;
                vEnum = null;
            }
        }

        // Concatenate the unenumerated elements from our local cache
        // and the remaining elements from the remote result set.

        final int numCached = elements.length - index;
        final int numRemaining = remainder != null ? remainder.length : 0;

        final EJBObject[] result = new EJBObject[numCached + numRemaining];

        System.arraycopy(elements, index, result, 0, numCached);
        if (remainder != null) {
            System.arraycopy(remainder, 0, result, numCached, numRemaining);
        }

        elements = null;

        return result;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // implementation
    //

    private final EJBObject[] fetchElements(int count)
                    throws RemoteException, EnumeratorException
    {
        EJBObject[] batch = null;

        try {

            if (vEnum != null)
                batch = vEnum.nextNElements(count);
            return batch;
        }
        //110799

        catch (NoMoreElementsException ex) {
            // FFDCFilter.processException(ex, CLASS_NAME + ".fetchElements", "375", this);
            throw ex;

        } catch (java.rmi.NoSuchObjectException exc) {

            // FFDCFilter.processException(exc, CLASS_NAME + ".fetchElements", "384", this);
            throw new com.ibm.ejs.container.finder.CollectionCannotBeFurtherAccessedException("Cannot access finder result outside transaction");

            //110799

        } finally {

            if (batch == null || batch.length < count) {
                // If we got fewer elements than we asked for, we've
                // exhausted the remote result set
                exhausted = true;
                vEnum = null;
            }

        }
    }

    // Once the SUID mismatch problem is overcome, the underlying
    // object structures differ between WAS/390 and WAS/workstation
    // We will implement the writeObject method for this object
    // in order to explicitly controll the marshalling of this
    // object.
    private void writeObject(java.io.ObjectOutputStream out)
                    throws IOException
    {

        out.defaultWriteObject();
        // the outgoing elements of the EJBObject
        // write out the header information
        out.write(eyecatcher);
        out.writeShort(platform);
        out.writeShort(versionID);
        //110799
        try {
            out.writeObject(vEnum);

        } catch (Throwable exc) {
            // FFDCFilter.processException(exc, CLASS_NAME + ".writeObject", "423", this);
            throw new com.ibm.ejs.container.finder.CollectionCannotBeFurtherAccessedException(exc.toString());

        }
        //110799

        out.writeObject(elements);

        out.writeInt(index);
        out.writeBoolean(exhausted);

    }

    // Once the SUID mismatch problem is overcome, the underlying
    // object structures differ between WAS/390 and WAS/workstation
    // We will implement the readObject method for this object
    // in order to explicitly controll the demarshalling of this
    // object.
    private void readObject(java.io.ObjectInputStream in)
                    throws IOException, ClassNotFoundException
    {

        in.defaultReadObject();

        byte[] ec = new byte[Constants.EYE_CATCHER_LENGTH];

        //d164415 start
        int bytesRead = 0;
        for (int offset = 0; offset < Constants.EYE_CATCHER_LENGTH; offset += bytesRead)
        {
            bytesRead = in.read(ec, offset, Constants.EYE_CATCHER_LENGTH - offset);
            if (bytesRead == -1)
            {
                throw new IOException("end of input stream while reading eye catcher");
            }
        } //d164415 end

        // validate that the eyecatcher matches
        for (int i = 0; i < eyecatcher.length; i++) {
            if (eyecatcher[i] != ec[i]) {
                throw new IOException();
            }
        }

        in.readShort(); // platform
        in.readShort(); // version
        //110799

        try {
            vEnum = (RemoteEnumerator) in.readObject();
        } catch (Throwable exc) {
            // FFDCFilter.processException(exc, CLASS_NAME + ".readObject", "471", this);
            throw new com.ibm.ejs.container.finder.CollectionCannotBeFurtherAccessedException(exc.toString());

        }

        //110799

        elements = (EJBObject[]) in.readObject();
        index = in.readInt();
        exhausted = in.readBoolean();

    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Data
    //

    // header information
    final static byte[] eyecatcher = Constants.FINDER_ENUMERATOR_EYE_CATCHER;;
    final static short platform = Constants.PLATFORM_DISTRIBUTED;
    final static short versionID = Constants.FINDER_ENUMERATOR_V1;

    // Reference to the remote, lazy result set
    private transient RemoteEnumerator vEnum = null;

    // Local cache of enumeration elements
    private transient EJBObject[] elements = null;

    // Index of next element in local cache
    private transient int index = 0;

    // Set to true when we've exhausted the remote result set (there may,
    // however, still be elements remaining in our local cache)
    private transient boolean exhausted = false;

    // Java serialization UID; must remain fixed!
    private static final long serialVersionUID = 4603100038030697154L;

    // length of the eyecatcher buffer

}
