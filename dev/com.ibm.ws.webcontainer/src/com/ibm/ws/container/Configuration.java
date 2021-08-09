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
package com.ibm.ws.container;

import java.util.Iterator;

/**
 * Config interface used by all internal configs
 */
public interface Configuration 
{
   
   /**
    * Identifier for configuration
    * @return String
    */
   public String getId();
   
   /**
    * To get at attribute
    * @param key
    * @return Object
    */
   public Object getAttribute(Object key);
   
   /**
    * To add attribute
    * @param key
    * @param attribute
    */
   public void addAttribute(Object key, Object attribute);
   
   /**
    * Get at attribute names
    * @return java.util.Iterator
    */
   @SuppressWarnings("unchecked")
   public Iterator getAttributeNames();
   
   /**
    * Get at attribute values
    * @return java.util.Iterator
    */
   @SuppressWarnings("unchecked")
   public Iterator getAttributeValues();
   
   /**
    * Remove attribute
    * @param key
    * @return Object
    */
   public Object removeAttribute(Object key);
   
   /**
    * @param wccmObj
    */
   public void populateFrom(Object wccmObj);
}
