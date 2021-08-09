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
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.EJBObject;

import com.ibm.ws.ejb.portable.Constants;

public class PortableFinderCollection extends AbstractCollection
                implements Serializable {

    // This class is one of the 7 byvalue classes identified as part of the SUID mismatch
    // situation. Since this class,  and the other six classes implement Serializable,
    // the desire is that the container should own the process of marshalling and
    // demarshalling these classes. Therefore, the following buffer contents have been
    // agreed upon between AE WebSphere container and WebSphere390 container:
    //
    // |------- Header Information -----------||-- Object Contents --|
    // [ eyecatcher ]               [ platform ][ version id ][     Data Section    ]
    //     byte[EYE_CATCHER_LENGTH]  short         short         instance fields
    //
    // This class, and the other six, overide the default implementation of the
    // Serializable methods 'writeObject' and 'readObject'. The implementations
    // of these methods in each of the seven identified byvalue classes read
    // and write the buffer contents as mapped above for thier respective
    // classes.
    //
    // In order to preserve n/n-1 compatability, the writeObject
    // method has been implemented to write the fields as byte arrays into an
    // outgoing byte[] buffer which is then written en masse to the outStream.
    // Coorospondingly, the readObject method reads the entire stream into
    // a byte[] and then extracts each field from that byte[].
    // This allows the flexibility to 'hop over' any portions of the
    // common section that may have expanded in remote instances of this class.
    //

    private static final long serialVersionUID = -4430653083633582029L;

    public PortableFinderCollection(EJBObject[] elements)
    {
        greedy = true;
        this.elements = elements;
        finderEnumerator = new PortableFinderEnumerator(elements);
    }

    public PortableFinderCollection(EJBObject[] prefetchElements, boolean exhausted,
                                    RemoteEnumerator vEnum)
    {
        this.elements = prefetchElements;
        this.exhausted = exhausted;
        this.vEnum = vEnum;
        finderEnumerator = new PortableFinderEnumerator(prefetchElements, exhausted, vEnum);
    }

    public Iterator iterator()
    {

        this.elements = finderEnumerator.loadEntireCollection();
        return (new FinderCollectionIterator(elements));
    }

    public int size()
    {
        return (finderEnumerator.size());
    }

    /**
     * Load the entire collection object before invoking the super class
     * Can be optimized to look in batches of elements instead of loading
     * the whole collection all at once.
     */
    public boolean contains(Object obj)
    {

        elements = finderEnumerator.loadEntireCollection();
        return (super.contains(obj));
    }

    /**
     * Load the entire collection object before invoking the super class
     */
    public boolean containsAll(Collection collection)
    {

        elements = finderEnumerator.loadEntireCollection();
        return (super.containsAll(collection));
    }

    /**
     * Load the entire collection object before invoking the super class
     */
    public Object[] toArray()
    {
        elements = finderEnumerator.loadEntireCollection();
        return (super.toArray());
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

        // write out the header information
        out.write(eyecatcher);
        out.writeShort(platform);
        out.writeShort(versionID);

        // write out the instance fields:
        out.writeObject(finderEnumerator);
        out.writeBoolean(greedy);
        // write the stubified element array
        out.writeObject(elements);
        out.writeBoolean(exhausted);
        out.writeObject(vEnum);

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

        // d164415 start
        int bytesRead = 0;
        for (int offset = 0; offset < Constants.EYE_CATCHER_LENGTH; offset += bytesRead)
        {
            bytesRead = in.read(ec, offset, Constants.EYE_CATCHER_LENGTH - offset);
            if (bytesRead == -1)
            {
                throw new IOException("end of input stream while reading eye catcher");
            }
        } // d164415 end

        // validate that the eyecatcher matches
        for (int i = 0; i < eyecatcher.length; i++) {
            if (eyecatcher[i] != ec[i]) {
                throw new IOException();
            }
        }

        // eyecatcher was ok, so read in remainder of the
        // header
        in.readShort(); // platform
        in.readShort(); // version

        // read in the data section:
        // read in the finderEnumerator obj
        finderEnumerator = (PortableFinderEnumerator) in.readObject();

        // read in the greedy flag
        greedy = in.readBoolean();

        // read in the elements obj
        elements = (EJBObject[]) in.readObject();

        // read in the exhausted flag
        exhausted = in.readBoolean();

        // read in the remoteEnumerator obj
        vEnum = (RemoteEnumerator) in.readObject();

    }

    // header information
    final static byte[] eyecatcher = Constants.FINDER_COLLECTION_EYE_CATCHER;;
    final static short platform = Constants.PLATFORM_DISTRIBUTED;
    final static short versionID = Constants.FINDER_COLLECTION_V1;

    // common section
    private transient PortableFinderEnumerator finderEnumerator;
    private transient boolean greedy = false;
    private transient EJBObject[] elements;
    private transient boolean exhausted;
    private transient RemoteEnumerator vEnum;

    // length of the eyecatcher array

}
