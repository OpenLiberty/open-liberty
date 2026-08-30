/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

import javax.resource.ResourceException;
import javax.resource.cci.LocalTransaction;

/**
 * Allows a client to perform local transaction demarcation.  This class doesn't
 * actually do anything, it's sole purpose in life is to test how the transaction
 * manager and connection manager handle global and local transactions and their
 * related events.
 * @author kaczyns
 */
public class LocalTransactionCciImpl implements LocalTransaction {

	/**
	 * Holds a reference to the managed connection.
	 */
	private ManagedConnectionImpl mc = null;
	
	/**
	 * Constructor
	 */
	public LocalTransactionCciImpl(ManagedConnectionImpl mc)
	{
		this.mc = mc;
	}
	
	/**
	 * @see javax.resource.cci.LocalTransaction#begin()
	 */
	public void begin() throws ResourceException 
	{
		mc.localTransactionStarted(true);
	}

	/**
	 * @see javax.resource.cci.LocalTransaction#commit()
	 */
	public void commit() throws ResourceException 
	{
		mc.localTransactionEnded(true, true);
	}

	/**
	 * @see javax.resource.cci.LocalTransaction#rollback()
	 */
	public void rollback() throws ResourceException 
	{
		mc.localTransactionEnded(true, false);
	}
}
