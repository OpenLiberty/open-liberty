/*******************************************************************************
 * Copyright (c) 2001, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.finder;

import java.util.Vector;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FinderResultServer extends Remote
{
    /**
     * Returns a sub-collection of finder result wrappers from a previous executed
     * finder method, starting at index for a maximum
     * of count elements. If remaining wrappers is less than count, all the
     * remaining elements are returned. If no more element remains, a null vector
     * is returned. Index is zero based.
     */
    public Vector getNextWrapperCollection(int start, int count)
                    throws RemoteException;

    /**
     * Returns the number of elements in this finder result collection.
     * For Enumeration result, the size is set to -1.<p>
     * Note: Try to avoid calling this class because this will in turns invoke
     * the pm key collection's size() method. The pm collection needs to
     * access all the rows to figure out the total size of the collection which
     * may inadvertently cause other concurrency problems/issues.
     * This method is defined to meet the Collection interface contract.
     * 
     * @return the number of elements in this collection
     */
    public int size() // d139782
    throws RemoteException; // d139782
}
