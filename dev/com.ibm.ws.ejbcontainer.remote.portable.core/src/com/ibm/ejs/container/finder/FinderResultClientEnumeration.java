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

import com.ibm.ws.ejb.portable.Constants;//d113380
import java.util.Enumeration;
import java.util.Vector;
import java.rmi.RemoteException;
import java.io.Serializable;
import java.io.IOException;

public class FinderResultClientEnumeration extends FinderResultClientBase
                implements Enumeration, Serializable
{
    final static byte[] frceEyecatcher = Constants.FINDER_RESULT_CLIENT_ENUMERATION_EYE_CATCHER;//d113380.7
    final static short frcePlatform = Constants.PLATFORM_DISTRIBUTED;//d113380.7
    final static short frceVersionID = Constants.FINDER_RESULT_CLIENT_ENUMERATION_V1;//d113380.7
    private static final long serialVersionUID = 92421000330697159L;//d113380.7

    /**
     * Construct the FinderResultClientEnumeration for greedy collection extraction.
     */
    public FinderResultClientEnumeration(Vector wrappers)
    {
        super(null, wrappers, null, Integer.MAX_VALUE); // d139782
    }

    /**
     * Construct the FinderResultClientEnumeration for lazy collection extraction.
     */
    public FinderResultClientEnumeration(FinderResultServer serverImpl, // LIDB833.1
                                         int chunkSize) throws RemoteException
    {
        // get the first chunk of collection to avoid another trip first time around
        super(serverImpl, serverImpl.getNextWrapperCollection(0, chunkSize),
              null, chunkSize); // d139782
    }

    private void writeObject(java.io.ObjectOutputStream out)
                    throws IOException
    {
        out.defaultWriteObject();

        out.write(frceEyecatcher);
        out.writeShort(frcePlatform);
        out.writeShort(frceVersionID);
    }

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
        } //d164415 end 

        // validate that the eyecatcher matches
        for (int i = 0; i < frceEyecatcher.length; i++)
        {
            if (frceEyecatcher[i] != ec[i])
            {
                String eyeCatcherString = new String(ec);
                throw new IOException("Invalid eye catcher '" + eyeCatcherString + "' in FinderResultClientEnumeration input stream");
            }
        }
        in.readShort(); // platform
        in.readShort(); // vid
    }//readObject

    // d140126.2 Begins
    public Vector getCurrentWrappers()
    {
        return wrappers;
    }
    // d140126.2 Ends
}
