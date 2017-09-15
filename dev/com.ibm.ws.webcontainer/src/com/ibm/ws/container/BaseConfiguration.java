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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.ws.webcontainer.util.EmptyIterator;

@SuppressWarnings("unchecked")
public class BaseConfiguration implements Configuration 
{
   private String _id = null;
	// Begin f269714, LI3477 - ServletConfig creation for Security
   protected Map _attributes = null;
	// End f269714, LI3477 - ServletConfig creation for Security

   
   /**
    * Constructor for ConfigImpl.
    * @param id
    */
   public BaseConfiguration(String id) 
   {
		super();
		this._id = id;    
   }
   
   /**
    * @return String
    * @see com.ibm.ws.servlet.container.Config#getId()
    */
   public String getId() 
   {
		return _id;    
   }
   
   /**
    * @param key
    * @param value
    */
   public void addAttribute(Object key, Object value) 
   {
		if (_attributes == null)
			_attributes = new HashMap();
		_attributes.put(key, value);    
   }
   
   /**
    * @param key
    * @return Object
    */
   public Object removeAttribute(Object key) 
   {
		if (_attributes != null) {
			return _attributes.remove(key);
		}
		return null;    
   }
   
   /**
    * @param key
    * @return Object
    */
   public Object getAttribute(Object key) 
   {
		if (_attributes != null) {
			return _attributes.get(key);
		}
		return null;    
   }
   
   /**
    * @return java.util.Iterator
    */
   public Iterator getAttributeNames() 
   {
		if (_attributes != null) {
			return _attributes.keySet().iterator();
		}
		return EmptyIterator.getInstance();    
   }
   
   /**
    * @return java.util.Iterator
    */
   public Iterator getAttributeValues() 
   {
		if (_attributes != null) {
			return _attributes.values().iterator();
		}
		return EmptyIterator.getInstance();    
   }
   
   /**
    * @param attrs
    */
   protected void setAttributes(Map attrs) 
   {
		this._attributes = attrs;    
   }
   
   /**
    * @param wccmObj
    */
   public void populateFrom(Object wccmObj)
   {
   }
}
