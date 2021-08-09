/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.response;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

public class CollectionEnumerationHybrid<E> extends ArrayList<E> implements Enumeration<E> {
	private static final long serialVersionUID = -7103072034780794758L;
	private Iterator<E> iterator;
	
	@Override
	public boolean hasMoreElements() {
		if (iterator==null)
			iterator = this.iterator();
		return iterator.hasNext();
	}

	@Override
	public E nextElement() {
		if (iterator==null)
			iterator = this.iterator();
		return iterator.next();
	}

}
