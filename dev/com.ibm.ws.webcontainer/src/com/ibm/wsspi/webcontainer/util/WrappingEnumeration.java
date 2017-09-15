/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
*
* 
* WrappingEnumeration wraps a collection of strings to handle getHeaderNames and getHeaders
* return type mismatch.
* 
* @ibm-private-in-use
* 
* @since   WAS8.0
* 
*/
public class WrappingEnumeration implements Enumeration {
	private Collection<String> targetCollection=null;
	
	private Iterator it=null;
	
	public WrappingEnumeration(Collection<String>  targetCollection) {
		this.targetCollection = targetCollection;
	}
	
	public Collection<String> getTargetCollection() {
		return targetCollection;
	}

	
	@Override
	public boolean hasMoreElements() {
		if (this.targetCollection==null)
		{
			return false;
		}
		else
		{
			if (it==null){
				it = targetCollection.iterator();
			}
			return (it.hasNext());
		}
	}

	@Override
	public Object nextElement() {
		if (this.targetCollection==null)
		{
			return null;
		}
		else
		{
			if (it==null){
				it = targetCollection.iterator();
			}
			return (it.next());
		}
	}
	
}