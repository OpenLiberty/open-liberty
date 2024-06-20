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
import javax.resource.spi.LocalTransaction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Spi implementation of the LocalTransaction interface.  This is used by the app server to begin
 * a transaction when we're running in a global transaction (wrapped as a 1PC resource), or when
 * the container cleans up the local transaction that the client forgot to end.
 */
public class LocalTransactionSpiImpl implements LocalTransaction {
	
	private static final TraceComponent tc = Tr.register(LocalTransactionSpiImpl.class);
	private static final TraceUtil tu = new TraceUtil(tc);
	
	/**
	 * Trace header
	 */
	private String header = new String(" !!LocalTransactionSpiImpl: ");

	/**
	 * Flag to tell us if we're in debug mode.
	 */
	private boolean debugMode = false;
	
	/**
	 * Reference to the managed connection which created us.
	 */
	private ManagedConnectionImpl mc = null;
	
	/**
	 * Constructor
	 */
	public LocalTransactionSpiImpl(ManagedConnectionImpl mc)
	{
		this.mc = mc;
		this.debugMode = mc.isDebugMode();
	}
	
	/**
	 * Called when the application server wishes to begin a local transaction.
	 * @see javax.resource.spi.LocalTransaction#begin()
	 */
	public void begin() throws ResourceException 
	{
		tu.debug(header + "begin called", debugMode);

		mc.localTransactionStarted(false);
	}

	/**
	 * Called when the application server wishes to commit a local transaction.
	 * @see javax.resource.spi.LocalTransaction#commit()
	 */
	public void commit() throws ResourceException 
	{
		tu.debug(header + "commit called", debugMode);

		mc.localTransactionEnded(false, true);
	}

	/**
	 * Called when the application server wishes to backout a local transaction.
	 * @see javax.resource.spi.LocalTransaction#rollback()
	 */
	public void rollback() throws ResourceException 
	{
		tu.debug(header + "rollback called", debugMode);

		mc.localTransactionEnded(false, false);
	}
}
