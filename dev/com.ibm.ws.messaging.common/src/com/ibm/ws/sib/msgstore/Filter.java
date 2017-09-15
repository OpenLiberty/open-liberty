/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.msgstore;


/**
 * Defines the interface to callback used to determine 'match' during a filtered
 * get or browse operation.
 * 
 * @author drphill
 */
public interface Filter {
	
	/**
	 * Method filterMatches.
	 * <p>this method is called to determine if any particular {@link AbstractItem}
	 * is a suitable match for the purposes of the receiver. Implementors
	 * of the filter should use this method to indicate suitable matches
	 * by returning true.</p>
	 * <p>The order in which the {@link AbstractItem}s are tested is dependant upon 
	 * the implementation of the {@link ItemStream} being traversed.  The 
     * implemention should be confined to performing the test, and should not
	 * attempt to side effect the {@link ItemStream} or the {@link AbstractItem}s in 
     * any way.</p>
     * <p> For any given instance of cursor, this method must always return the same 
     * result for the same item presented.
     * </p>
	 * @param abstractItem {@link AbstractItem} to be tested
	 * @return boolean true if this member is a suitable match, false
	 * otherwise.
	 */
	public boolean filterMatches(AbstractItem abstractItem) throws MessageStoreException ;
	
}
