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
import java.util.Enumeration;

import javax.ejb.EJBObject;

import com.ibm.ws.ejb.portable.Constants;

public class PortableFinderEnumeration implements Enumeration, Serializable {

    // This class is one of the 7 byvalue classes identified as part of the SUID mismatch
    // situation. Since this class,  and the other six classes implement Serializable,
    // the desire is that the container should own the process of marshalling and
    // demarshalling these classes. Therefore, the following buffer contents have been
    // agreed upon between AE WebSphere container and WebSphere390 container:
    //
    // |------- Header Information -----------||-- Object Contents --|
    // [ eyecatcher ][ platform ][ version id ][     Data Section    ]
    //     byte[4]       short          short       instance fields
    //
    // This class, and the other six, overide the default implementation of the
    // Serializable methods 'writeObject' and 'readObject'. The implementations
    // of these methods in each of the seven identified byvalue classes read
    // and write the buffer contents as mapped above for thier respective
    // classes.
    //

    private static final long serialVersionUID = 6932676449261895025L;

    // header information
    final static byte[] eyecatcher = Constants.FINDER_ENUMERATION_EYE_CATCHER;;
    final static short platform = Constants.PLATFORM_DISTRIBUTED;
    final static short versionID = Constants.FINDER_ENUMERATION_V1;

    //data
    private transient PortableFinderEnumerator finderEnumerator = null;

    public PortableFinderEnumeration(EJBObject[] elements)
    {
        finderEnumerator = new PortableFinderEnumerator(elements);
    }

    public PortableFinderEnumeration(EJBObject[] prefetchedElements, boolean exhausted,
                                     RemoteEnumerator vEnum)
    {
        finderEnumerator = new PortableFinderEnumerator(prefetchedElements,
                        exhausted, vEnum);
    }

    public boolean hasMoreElements()
    {

        return (finderEnumerator.hasMoreElements());
    }

    public Object nextElement()
    {

        return (finderEnumerator.nextElement());
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

        // write out the instance data
        out.writeObject(finderEnumerator);

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
        // read in the eyecatcher for this object (WSFE in ascii)
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

        finderEnumerator = (PortableFinderEnumerator) in.readObject();

    }

}
