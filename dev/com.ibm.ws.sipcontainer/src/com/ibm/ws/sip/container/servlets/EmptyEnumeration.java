/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * @author Amir Perlman, 23 Feb 2005
 *
 * Empty Enumeration implemenation where we need to return an enumeration but 
 * the enumerated structure is either empty or not initialized.  
 */
public class EmptyEnumeration implements Enumeration 
{
    /**
     * The singleton instance. 
     */
    private final static EmptyEnumeration c_emptyEnumeration = 
        									new EmptyEnumeration();

    /**
     * Construct a new Empty Enumeration
     */
    private EmptyEnumeration()
    {
    }
    
    /**
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements() 
    {
        return false;
    }

    /**
     * @see java.util.Enumeration#nextElement()
     */
    public Object nextElement() 
    {
        throw new NoSuchElementException();
    }
    
    /**
     * Gets the singleton instance of the iterator. 
     * @return
     */
    public static EmptyEnumeration getInstance()
    {
        return c_emptyEnumeration; 
    }
}
