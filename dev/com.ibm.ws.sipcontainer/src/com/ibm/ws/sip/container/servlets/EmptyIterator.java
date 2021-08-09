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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Amir Perlman, Oct 14, 2004
 *
 * Dummy iterator implemenation for case where we must return an iterator in 
 * the API but we dont have collection object instantiated. 
 */
@SuppressWarnings("unchecked")
public class EmptyIterator<E> implements Iterator<E> {
    /**
     * Single instance to be used all to avoid creating unnecessary objects. 
     */
    private static final EmptyIterator c_emptyIterator = new EmptyIterator();
    
    /**
     * Construct a new empty iterator. 
     */
    private EmptyIterator()
    {
    }
    
    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext()
    {
        return false;
    }

    /**
     * @see java.util.Iterator#next()
     */
    public E next() {
        throw new NoSuchElementException();
    }

    /**
     * @see java.util.Iterator#remove()
     */
    public void remove() {
    	throw new UnsupportedOperationException("The empty iterator does not support removal of elements.");
	}


    /**
     * @return the singleton instance of this iterator. 
     */
	public static final <T> Iterator<T> getInstance() {
    	return (Iterator<T>)c_emptyIterator;
	}

    

    
}
