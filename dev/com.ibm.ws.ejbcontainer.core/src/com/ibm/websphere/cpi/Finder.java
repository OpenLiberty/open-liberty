/*******************************************************************************
 * Copyright (c) 1999, 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cpi;

import java.util.Enumeration;
import javax.ejb.*;

/**
 * This interface provides access to the set of objects that make up the
 * result of a find on a bean's home interface.
 * 
 * The set of objects may be accessed using an Enumeration-like interface
 * that maintains a current element. The set may be enumerated exactly
 * once, there is no way to reset it to the beginning.
 * 
 * This interface is present in the current cpi package as it reflects
 * the current design. It is possible (and better!) to redesign the interfaces
 * so that this interface is internal to the persister implementation.
 * However, this will impact the existing code generation, and so is
 * deferred till the Aquila release, when CPI is completely redesigned.
 */

public interface Finder extends Enumeration
{

    /**
     * Close this finder and free any associated resources
     * 
     * Once this method has been called the finder is in an undefined state,
     * and should not be used.
     */
    public void close();

    /**
     * Get the primary key for the current element of the set of found
     * objects.
     * 
     * @return Object representing the primary key of the current element
     *         of the set.
     */
    public Object getPrimaryKey()
                    throws Exception;

    /**
     * Advance the result set and return the next EJBObject.
     * 
     * @return EJBObject which is the next object in the result set.
     */
    public EJBObject nextObject()
                    throws Exception;

    /**
     * Advance the result set and return the primary key of the next EJBObject
     * 
     * @return Object representing the primary key of the next element in the set.
     */
    public Object nextKey()
                    throws Exception;

    /**
     * Return true iff there are more elements available in the result set
     * 
     * @result boolean: true iff more elements in the result set.
     */
    public boolean hasMore()
                    throws Exception;

} // Finder

