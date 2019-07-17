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

/**
 * The class represents a set of actions that provide locking mechanism
 * in a database like data structure.
 * @author mordechai
 */
public interface TransactionSupport 
{
	/**
	 * Signals a transaction has been started.
	 * @return optional the transaction ID which has been started.
	 */
	public Object beginTx();

	/**
	 * begin to commit a transaction (which has been started by beginTx())
	 * @param the transaction to be commited. this should matc the return value of beginTx()
	 * if null is provided the application should commit all uncommited work.
	 * @return optional the transaction ID which has been committed.
	 */
	public Object commitTx(Object txKey);

	/**
	 * Signals a transaction should be canceled.
	 * @return optional - the transaction ID which has been cancelled.
	 */
	public Object rollback(Object txKey);

}
