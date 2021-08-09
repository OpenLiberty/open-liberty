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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import com.ibm.ws.ejb.portable.Constants;//d113380
import java.io.IOException;

public class FinderResultClientIterator extends FinderResultClientBase
                implements Iterator
{
    /*
     * Interop eyecatcher, platform, and version
     */
    final static byte[] frciEyecatcher = Constants.FINDER_RESULT_CLIENT_ITERATOR_EYE_CATCHER;//d113380.7
    final static short frciPlatform = Constants.PLATFORM_DISTRIBUTED;//d113380.7
    final static short frciVersionID = Constants.FINDER_RESULT_CLIENT_ITERATOR_V1;//d113380.7
    private static final long serialVersionUID = 94441000330697159L;//d113380.7

    /**
     * Construct the FinderResultClientIterator.
     */
    public FinderResultClientIterator(FinderResultServer server, Vector colWrappers,
                                      FinderResultClientCollection parentCollection, // d139782
                                      int chunkSize) // LIDB833.1
    {
        super(server, colWrappers, parentCollection, chunkSize); // LIDB833.1 d139782
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     * 
     * @return <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext()
    {
        return (hasMoreElements());
    }

    /**
     * Returns the next element in the iteration.
     * 
     * @return the next element in the iteration.
     * @exception NoSuchElementException iteration has no more elements.
     */
    public Object next()
    {
        return (nextElement());
    }

    /**
     * Removes from the underlying collection the last element returned by the
     * iterator (optional operation). This method can be called only once per
     * call to f<tt>next</tt>. The behavior of an iterator is unspecified if
     * the underlying collection is modified while the iteration is in
     * progress in any way other than by calling this method.
     * 
     * @exception UnsupportedOperationException if the <tt>remove</tt>
     *                operation is not supported by this Iterator.
     * @exception IllegalStateException if the <tt>next</tt> method has not
     *                yet been called, or the <tt>remove</tt> method has already
     *                been called after the last call to the <tt>next</tt>
     *                method.
     */
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    /* d113380 interop implementation for readObject and writeObject */
    private void writeObject(java.io.ObjectOutputStream out)
                    throws IOException
    {
        out.defaultWriteObject();
        out.write(frciEyecatcher);
        out.writeShort(frciPlatform);
        out.writeShort(frciVersionID);
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
        for (int i = 0; i < frciEyecatcher.length; i++)
        {
            if (frciEyecatcher[i] != ec[i])
            {
                String eyeCatcherString = new String(ec);
                throw new IOException("Invalid eye catcher '" + eyeCatcherString + "' in FinderResultClientIterator input stream");
            }
        }

        in.readShort(); // platform
        in.readShort(); // vid
    }//readObject
    /* d113380 */
}
