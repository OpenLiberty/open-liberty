/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.failover.repository;

import com.ibm.ws.sip.container.tu.TransactionUserBase;

/**
 * This defines a storage for TransactionUserBase instances.
 * @author mordechai
 *
 */
public interface TuBaseRepository extends TransactionSupport 
{
	/**
	 * Adds a TuBase into the repository 
	 * @param sessionId - this is actually the parent TuWrapper.getId()
	 * @param tuBase - a tranasaction user base instance 
	 * @return previous object (if exists)
	 */
	public  TransactionUserBase put(String sessionId , TransactionUserBase tuBase);
	
	/**
	 * Search a TransactionUserBase by giving its session ID.
	 * @param sessionId - the replated SipSession id.
	 */
	public  TransactionUserBase get(String sessionId );
	
	/**
	 * Remove an object from repository.
	 * @param tuBase - the TuBase to be removed.
	 * @return the removed object (if found), otherwise returns null
	 */
	public  TransactionUserBase remove(TransactionUserBase tuBase);
}
