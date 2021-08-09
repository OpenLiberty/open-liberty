/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.failover.repository;

import java.util.List;

import com.ibm.ws.sip.container.tu.TUKey;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * This class defines a storage for Transaction user wrappers which stored according to the
 * TUKey object.
 * Altough TuWrappers are not repilcated we will use this interface just in its
 * standalone implementation (for convient and uniformity of API)
 * @author mordechai
 */
public interface TUWrapperRepositoryRfc3261 extends TransactionSupport 
{
	/**
	 * Add a TransactionUserWrapper instance to the repository of TransactionUserWrappers.
	 * @param key - object wich holds toTag, fromTag and callId
	 * @param tuWrapper - a TransactionUserWrapper instance
	 * @return the previous value in the repository (if such value exsists ).
	 */
	public TransactionUserWrapper put(TUKey key , TransactionUserWrapper tuWrapper);
	
	/**
	 * Search a TransactionUserWrapper by giving its session ID.
	 * @param key - object wich holds toTag, fromTag and callId
	 */
	public TransactionUserWrapper get(TUKey key);
	
	/**
	 * Remove a TransactionUserWrapper by giving its session ID.
	 * @param key - object wich holds toTag, fromTag and callId
	 */
	public TransactionUserWrapper remove(TUKey key );
	
	
	/**
	 * this method is needed mainly for bootstrap sequence.  
	 * @return an ArrayList of all the TuWrappers.
	 */
	public List getAll();
}
