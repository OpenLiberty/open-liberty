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

import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * This class defines a storage for Transaction user wrappers.
 * Altough TuWrappers are not repilcated we will use this interface just in its
 * standalone implementation (for convient and uniformity of API)
 * @author mordechai
 */
public interface TuWrapperRepository extends TransactionSupport 
{
	/**
	 * Add a TransactionUserWrapper instance to the repository of TransactionUserWrappers.
	 * @param sessionId - the replated SipSession id.
	 * @param tuWrapper - a TransactionUserWrapper instance
	 * @return the previous value in the repository (if such value exsists ).
	 */
	public TransactionUserWrapper put(String sessionId , TransactionUserWrapper tuWrapper);
	
	/**
	 * Search a TransactionUserWrapper by giving its session ID.
	 * @param sessionId - the replated SipSession id.
	 */
	public TransactionUserWrapper get(String sessionId );
	
	/**
	 * Remove a TransactionUserWrapper by giving its session ID.
	 * @param sessionId - the replated SipSession id.
	 */
	public TransactionUserWrapper remove(String sessionId );
	
	
	/**
	 * this method is needed mainly for bootstrap sequence.  
	 * @return an ArrayList of all the TuWrappers.
	 */
	public List getAll();
}
