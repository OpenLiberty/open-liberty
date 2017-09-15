/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

@SuppressWarnings("unchecked")
public class EmptyIterator implements Iterator 
{
   private static Iterator _instance = new EmptyIterator ();
   
   /**
    * Constructor for EmptyIterator.
    */
   public EmptyIterator() 
   {
		super();    
   }
   
   /**
    * @return boolean
    * @see java.util.Iterator#hasNext()
    */
   public boolean hasNext() 
   {
		return false;    
   }
   
   /**
    * @return Object
    * @see java.util.Iterator#next()
    */
   public Object next() 
   {
		throw new NoSuchElementException();    
   }
   
   /**
    * @see java.util.Iterator#remove()
    */
   public void remove() 
   {
		throw new RuntimeException("Not supported");    
   }
   
   /**
    * @return java.util.Iterator
    */
   public static Iterator getInstance() 
   {
		return _instance;    
   }
}
