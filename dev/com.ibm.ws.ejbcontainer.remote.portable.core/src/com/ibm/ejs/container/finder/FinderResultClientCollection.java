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

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.lang.reflect.Method;
import java.io.Serializable;
import java.rmi.RemoteException;
import javax.ejb.FinderException;
import com.ibm.ws.ejb.portable.Constants;//d113380
import java.io.IOException;

public class FinderResultClientCollection extends AbstractCollection
                implements Serializable, Set { // d114594

    /*
     * Interop eyecatcher, platform, and version
     */
    final static byte[] eyecatcher = Constants.FINDER_RESULT_CLIENT_COLLECTION_EYE_CATCHER;//d113380
    final static short platform = Constants.PLATFORM_DISTRIBUTED;//d113380
    final static short versionID = Constants.FINDER_RESULT_CLIENT_COLLECTION_V1;//d113380
    private static final long serialVersionUID = 96031000330697159L;//d113380

    // attributes should be public qualified for Serializable to work.
    /**
     * Stub reference to remove collection server for lazy wrapper retrieval.
     * This is set to null if greedy policy is used.
     */
    public transient FinderResultServer server;//d113380
    /**
     * Collection of finder result collection used in both lazy and greedy wrapper retrieval.
     */
    public transient Vector allWrappers;//d113380
    /**
     * When all the collection wrappers, which is shared by all iterators created
     * from this Collection, is retrieved from the server, this attribute is
     * set to true;
     */
    private transient boolean hasAllWrappers; // d139782
    /**
     * Size of the remote wrapper retrieval during lazy wrapper retrieval.
     * If this is set to Integer.MAX_VALUE, greedy policy is deployed.
     */
    public transient int chunkSize;//d113380

    /**
     * Construct the FinderResultClientCollection for greedy collection extraction
     * <ul>
     * <li>If chunkSize is set to Integer.MAX_VALUE, the client collection will
     * perform the greedy extraction of all wrapper from the collection and
     * wire them to the client.
     * </ul>
     */
    public FinderResultClientCollection(Vector wrappers)
        throws FinderException, RemoteException
    {
        this.chunkSize = Integer.MAX_VALUE;
        this.allWrappers = wrappers;
    }

    /**
     * Construct the FinderResultClientCollection for lazy collection extraction
     * <ul>
     * <li>If chunkSize is set to Integer.MAX_VALUE, the client collection will
     * perform the greedy extraction of all wrapper from the collection and
     * wire them to the client.
     * </ul>
     */
    public FinderResultClientCollection(FinderResultServer serverImpl, // LIDB833.1
                                        int chunkSize)
        throws FinderException, RemoteException
    {
        this.chunkSize = chunkSize;
        this.server = serverImpl;

        // LIDB833.1 Begin
        // get the first chunk of collection to avoid another trip first time around
        this.allWrappers = serverImpl.getNextWrapperCollection(0, chunkSize);

        // if the initial collection has already exhausted the result collection,
        // getNextWrapperCollection will unexported the serverImpl object, therefore
        // need to release the server object reference in the return
        // FinderResultClientCollection to avoid ORB timeout while trying to look
        // for the referenced object.
        boolean hasExhaustedCollection; // d147409

        try {
            // use reflection here to avoid FinderResultServerImpl dependency in the ejbportable jar
            Class serverImplClass = Class.forName("com.ibm.ejs.container.finder.FinderResultServerImpl"); // d122794

            Method m = serverImplClass.getMethod("exhaustedCollection", null); // d147409
            hasExhaustedCollection = ((Boolean) m.invoke(serverImpl, null)).booleanValue(); // d147409

            if (hasExhaustedCollection) { // d147409
                this.server = null;
            }
        } catch (Throwable t) {
            // FFDCFilter.processException(t, CLASS_NAME + ".FinderResultClientCollection",
            //                             "112", this);
        }
        // LIDB833.1 End
    }

    /**
     * Returns the number of elements in this collection. If the collection
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     * 
     * @return the number of elements in this collection.
     * @exception CollectionCannotBeFurtherAccessedException is thrown when error
     *                is caught communication to the server in getting the collection size.
     */
    public int size()
    {
        // d139782 Begins
        try
        { // performance short-cut if client has already retrieve all finder result wrappers.
            return (server != null && !hasAllWrappers ? server.size() : allWrappers.size());
        } catch (Throwable t)
        {
            throw new CollectionCannotBeFurtherAccessedException("Caught exception in size() method call:" +
                                                                 t.getMessage());
        }
        // d139782 Ends
    }

    /**
     * Returns an Iterator over the elements in this collection. There are no
     * guarantees concerning the order in which the elements are returned
     * (unless this collection is an instance of some class that provides a
     * guarantee).
     * 
     * @return an <tt>Iterator</tt> over the elements in this collection
     */
    public Iterator iterator()
    {
        // LIDB833.1
        return (new FinderResultClientIterator(server, allWrappers, this, chunkSize)); // d139782
    }

    // d139782 Begins
    /**
     * Return the hasAllWrappers value.
     */
    void allWrappersCached()
    {
        hasAllWrappers = true;
    }

    /**
     * Return the hasAllWrappers value.
     */
    boolean hasAllWrappers()
    {
        return hasAllWrappers;
    }

    // d139782 Ends

    /*
     * d113380 readObject and writeObject implementations
     */
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
        out.writeObject(allWrappers);
        out.writeInt(chunkSize);
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
                throw new IOException("Invalid eye catcher '" + eyeCatcherString + "' in FinderResultClientCollection input stream");
            }
        }
        in.readShort(); // platform
        in.readShort(); // vid

        server = (FinderResultServer) in.readObject();
        allWrappers = (Vector) in.readObject();
        chunkSize = in.readInt();
    }//readObject

    /*
     * d113380 readObject and writeObject implementations
     */

    // d135330 Begins
    /**
     * Override the toString method in the AbstractCollection to avoid the iteration of
     * all the collection entries from the server cross the wire and basically perform
     * a greedy collection extraction from the server.
     */
    public String toString()
    {
        return this.getClass().getName() +
               " : collection increment= " + chunkSize +
               " : local wrapper collection size= " + allWrappers.size();
    }
    // d135330 Ends
}
