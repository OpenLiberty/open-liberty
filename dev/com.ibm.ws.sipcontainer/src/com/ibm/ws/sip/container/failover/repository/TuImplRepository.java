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

import com.ibm.ws.sip.container.tu.TransactionUserImpl;

/**
 * This class defines a storage for Trnasaction User Implementation classes.
 * @author mordechai
 *
 */
public interface TuImplRepository  extends TransactionSupport 
{
	/**
	 * Adds a new TU to the repository. 
	 * @param sessionId - the connected SIP session id (string)
	 * @param tuImpl - a transactionuserimpl instance we want to save
	 * @return previous value (if exists) , maybe null if new key inserted.
	 */
	public TransactionUserImpl put(String sessionId , TransactionUserImpl tuImpl);

	/**
	 * Retrieves a TUimpk from the repository. 
	 * @param sessionId - the connected SIP session id (string)
	 * @return the value of the TUimpl , maybe null if not found.
	 */
	public TransactionUserImpl get(String sessionId );
	
	/**
	 * removes a Transaction impl from the repository. 
	 * @param tuImpl - a transaction user implementation class.
	 * @return the existing sip application session , maybe null if not found.
	 */
	public TransactionUserImpl remove(TransactionUserImpl tuImpl );
}