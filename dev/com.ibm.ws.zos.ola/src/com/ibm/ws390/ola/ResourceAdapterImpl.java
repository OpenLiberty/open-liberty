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
package com.ibm.ws390.ola;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * ResourceAdapter implementation which is driven when the server starts.
 *
 * @author kaczyns
 */
public class ResourceAdapterImpl implements ResourceAdapter {

	/**
	 * The status of the ResourceAdapter instance
	 */
	private boolean _isRunning = false;

	/**
	 * Saved bootstrap context.
	 */
	@SuppressWarnings("unused")
	private BootstrapContext _context = null;


	/**
	 * @see javax.resource.spi.ResourceAdapter#start(BootstrapContext)
	 */
	public void start(BootstrapContext context)
		throws ResourceAdapterInternalException
	{
		_context = context;
		_isRunning = true;
	}


	/**
	 * @see javax.resource.spi.ResourceAdapter#stop()
	 */
	public void stop()
	{
		_isRunning = false;
	}

	
	/**
	 * Am I running?
	 */
	public boolean isRunning()
	{
		return _isRunning;
	}


	/**
	 * This method is called when the application server starts an application
	 * which wants to listen for a particular type of message.  The
	 * ActivationSpec will tell us what 'type' of messages we need to
	 * send to the MessageEndpoint obtained from the MessageEndpointFactory.
	 *
	 * @see javax.resource.spi.ResourceAdapter#endpointActivation(MessageEndpointFactory, ActivationSpec)
	 */
	public void endpointActivation(MessageEndpointFactory meFactory,
		                           ActivationSpec aSpec)
		throws ResourceException
	{
	}


	/**
	 * @see javax.resource.spi.ResourceAdapter#endpointDeactivation(MessageEndpointFactory, ActivationSpec)
	 */
	public void endpointDeactivation(MessageEndpointFactory meFactory,
	 						   		 ActivationSpec aSpec)
	{
	}
	

	/**
	 * @see javax.resource.spi.ResourceAdapter#getXAResources(ActivationSpec[])
	 */
	public XAResource[] getXAResources(ActivationSpec[] arg0)
		throws ResourceException {
		throw new javax.resource.NotSupportedException
                  ("This getXAResources() method is Not Supported.");  /* @578463A*/
		//return null;
	}
}
