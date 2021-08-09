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

import java.util.Enumeration;
import java.util.Iterator;

@SuppressWarnings("unchecked")
public class IteratorEnumerator implements Enumeration 
{
   private Iterator _iterator = null;
   
   /**
    * Constructor for IteratorEnumerator.
    * @param iter
    */
   public IteratorEnumerator(Iterator iter) 
   {
		this._iterator = iter;    
   }
   
   /**
    * @return boolean
    * @see java.util.Enumeration#hasMoreElements()
    */
   public boolean hasMoreElements() 
   {
		return this._iterator.hasNext();    
   }
   
   /**
    * @return Object
    * @see java.util.Enumeration#nextElement()
    */
   public Object nextElement() 
   {
		return this._iterator.next();    
   }
}
