/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.finder;

import java.io.Serializable;
import java.util.Vector;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.rmi.RemoteException;
import com.ibm.ws.ejb.portable.Constants;//d113380
import java.io.IOException;

/**
 * Client result base class to support remote finder result iterator and enumeration.
 * This class is intended to be used by a single client, therefore no synchronization
 * is attempted to support multi-client support. In multi-client scenario, user should
 * obtain another iterator from the Collection or get Enumeration from another
 * finder call.
 */
public class FinderResultClientBase implements Serializable {
    /*
     * header information for interop and serialization
     */
    final static byte[] eyecatcher = Constants.FINDER_RESULT_CLIENT_BASE_EYE_CATCHER;
    final static short platform = Constants.PLATFORM_DISTRIBUTED;
    final static short versionID = Constants.FINDER_RESULT_CLIENT_BASE_V1;
    private static final long serialVersionUID = 4403100038030697155L;
    /**
     * Stub reference to remove collection server for lazy wrapper retrieval.
     * This is set to null if greedy policy is used.
     */
    private transient FinderResultServer server; //d113380
    /**
     * Full collection of finder result collection used in greedy wrapper retrieval.
     * This is set to null if lazy policy is used.
     */
    protected transient Vector wrappers; //d113380
    /**
     * Set to true if no more element is available for next or nextElement operation.
     */
    private transient boolean exhausted; //d113380
    /**
     * reference to the parent Collection. This is only used for Collection processing
     * and is local to the client side. Therefore there is no need to serialize
     * this object.
     */
    private transient Object parentCollection; // d139782
    /**
     * Size of the remote wrapper retrieval during lazy wrapper retrieval.
     * If this is set to Integer.MAX_VALUE, greedy policy is deployed.
     */
    private transient int chunkSize; //d113380
    /**
     * Index to next return element from the wrappers.
     */
    private transient int itrIndex; // LIDB833.1 d113380
    /**
     * Server resource released, Collection can not be access indicator.
     */
    private transient boolean collectionExceptionPending; // d140126

    // LIDB833.1 Begin
    /**
     * Construct the FinderResultClientBase. This is used for lazy materialization
     * of finder result collection..
     */
    public FinderResultClientBase(FinderResultServer server, Vector colWrappers,
                                  Object parentCollection, // d139782
                                  int chunkSize)
    {
        this.server = server;
        this.chunkSize = chunkSize;
        this.itrIndex = 0;
        this.exhausted = colWrappers.size() == 0; // d139782
        this.wrappers = colWrappers;
        this.parentCollection = parentCollection; // d139782

        if (this.server != null) // d147409
        { // if the initial collection has already exhausted the result collection,
          // getNextWrapperCollection will unexported the serverImpl object, therefore
          // need to release the server object reference in the return
          // FinderResultClientBase to avoid ORB timeout while trying to look
          // for the referenced object.                              d140126
            boolean hasExhaustedCollection; // d147409

            try {
                // use reflection here to avoid FinderResultServerImpl dependency in the ejbportable jar
                Class serverImplClass = Class.forName("com.ibm.ejs.container.finder.FinderResultServerImpl");

                Method m = serverImplClass.getMethod("exhaustedCollection", null); // d147409
                hasExhaustedCollection = ((Boolean) m.invoke(server, null)).booleanValue(); // d147409

                if (hasExhaustedCollection) { // d147409
                    this.server = null;
                }
            } catch (Throwable t) {
                // FFDCFilter.processException(t, CLASS_NAME + ".FinderResultClientBase",
                //                             "121", this);
            }
        }
    }

    // LIDB833.1 End

    /**
     * Returns <tt>true</tt> if the enumeration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     * 
     * @return <tt>true</tt> if the iterator/enumeration has more elements.
     */
    public boolean hasMoreElements()
    {
        return (!exhausted); // d139782
    }

    /**
     * Returns the next element in the iterator/enumeration. This will look ahead
     * at least one element to satisfy the hasMoreElement/nextElement semantics
     * without disrupting the datasource cursor.
     * 
     * @return the next element in the iteration.
     * @exception NoSuchElementException enumeration has no more elements.
     */
    public Object nextElement()
    {
        // d139782 Begins
        if (hasMoreElements()) {
            // d140126 Begins
            // some previous nextElement call detected that the server resources has
            //  been released, the last element was returned. Any subsequent call
            //  to nextElement will result with a CollectionCannotBeFurtherAccessedException.
            if (collectionExceptionPending)
            {
                throw new CollectionCannotBeFurtherAccessedException();
            }
            // d140126 Ends

            // extract the next element from the local wrapper collection.
            Object result = wrappers.elementAt(itrIndex++); // LIDB833.1
            // test if local index has advanced to exceed the max count.
            if (server == null ||
                (parentCollection != null &&
                ((FinderResultClientCollection) parentCollection).hasAllWrappers()))
            { // server == null indicates greedy collection and all wrapper is available.
              // in lazy collection and Collection interface, potentially there are other
              //  iterator concurrently updating the client wrapper cached in the Collection object
              //  this check is used to examine this iterator's parent Collection for short-cut wrapper
              //  retrieval.
                if (itrIndex >= wrappers.size()) { // LIDB833.1
                    exhausted = true;
                }
            } else
            { // if next element index is not in the client wrapper cache,
              //  need to find out from the remote server to satisfy the next hasMoreElement()
              //  call semantics.
                if (itrIndex >= wrappers.size()) { // LIDB833.1
                    try {
                        // go remote to get the next sub-collection of wrappers
                        Vector nextWrappers = server.getNextWrapperCollection(wrappers.size(),
                                                                              chunkSize);
                        if (nextWrappers == null || nextWrappers.size() == 0) {
                            // well, there are no more from remote
                            exhausted = true;
                            if (parentCollection != null)
                            { // need to inform parent Collection all wrappers has been retrieved from server.
                                ((FinderResultClientCollection) parentCollection).allWrappersCached();
                            }
                        } else { // LIDB833.1
                            // append the next set of wrappers to the parent's wrapper set
                            wrappers.addAll(nextWrappers); // LIDB833.1
                        }
                    } catch (RemoteException rex) {
                        collectionExceptionPending = true; // d140126
                        //                        throw new CollectionCannotBeFurtherAccessedException(); // LIDB833.1
                    }
                }
            }
            return (result);
        }
        // d139782 Ends
        throw new NoSuchElementException();
    }

    // d113380 Begins
    // implemented readObject and writeObject to conform to Distributed/390 conventions
    private void writeObject(java.io.ObjectOutputStream out)
                    throws IOException
    {
        out.defaultWriteObject();
        // write out the header information
        out.write(eyecatcher);
        out.writeShort(platform);
        out.writeShort(versionID);
        // write out the data
        out.writeObject(server);
        out.writeObject(wrappers);
        out.writeBoolean(exhausted);
        out.writeInt(chunkSize);
        out.writeInt(itrIndex);
        // This is no need to stream out parentCollection & collectionExceptionPending
    }

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
        for (int i = 0; i < eyecatcher.length; i++)
        {
            if (eyecatcher[i] != ec[i])
            {
                String eyeCatcherString = new String(ec);
                throw new IOException("Invalid eye catcher '" + eyeCatcherString + "' in FinderResultClientBase input stream");
            }
        }

        in.readShort(); // platform
        in.readShort(); // vid

        server = (FinderResultServer) in.readObject();
        wrappers = (Vector) in.readObject();
        exhausted = in.readBoolean();
        chunkSize = in.readInt();
        itrIndex = in.readInt();
        // This is no need to stream in parentCollection & collectionExceptionPending
    }
    // d113380 Ends
}
