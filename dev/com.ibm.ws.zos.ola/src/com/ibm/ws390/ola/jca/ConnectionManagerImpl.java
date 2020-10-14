/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Default implementation of the ConnectionManager interface.  This allows a
 * client to get a connection to the resource in a non-managed (non-WebSphere)
 * environment.  We don't try to get fancy here - they want a connection,
 * we give them one.
 * @author kaczyns
 */
public class ConnectionManagerImpl implements ConnectionManager {
	
	private static final TraceComponent tc = Tr.register(ConnectionManagerImpl.class);
	private static final TraceUtil tu = new TraceUtil(tc);

	/**
	 * 
	 */
	private static final long serialVersionUID = 236090710797762028L;

	/**
	 * Trace header
	 */
	private String header = new String(" !!ConnectionManagerImpl: ");

	/**
	 * Flag telling us if debug mode is on.  Defaults to false.
	 */
	private boolean debugMode = false;
	
	/**
	 * Constructor
	 */
	public ConnectionManagerImpl(boolean debugMode)
	{
		this.debugMode = debugMode;
	}

	/**
	 * Give the connection to whoever asks for it.  New up a ManagedConnection
	 * and return the connection associated with it.
	 * @see javax.resource.spi.ConnectionManager#allocateConnection(ManagedConnectionFactory, ConnectionRequestInfo)
	 */
	public Object allocateConnection(
			ManagedConnectionFactory mcf,
			ConnectionRequestInfo crii)
		throws ResourceException 
	{
		tu.debug(header + "allocateConnection called, mcf = " + mcf + " crii = " + crii, this.debugMode);

		if ((mcf == null) || (!(mcf instanceof com.ibm.ws390.ola.ManagedConnectionFactoryImpl)))
		{
			throw new ResourceException("ManagedConnectionFactory null or invalid, " + mcf);
		}
		
		ManagedConnection mc = mcf.createManagedConnection(null, crii);
		tu.debug("ConnectionManagerImpl: inside allocateConnection. Allocated cm: "+mc, this.debugMode);
		
		return mc.getConnection(null, crii);
	}
}
