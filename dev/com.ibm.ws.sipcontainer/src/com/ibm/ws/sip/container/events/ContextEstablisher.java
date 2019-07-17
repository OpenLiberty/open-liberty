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
package com.ibm.ws.sip.container.events;

import javax.servlet.ServletContext;

/**
 * Defines the method that will establish or remove a WebContainer module context on SIP threads
 * @author Nitzan Nissim
 */
public interface ContextEstablisher {
	
	/**
	 * Establish context on thread
	 */
	public void establishContext();
	
	/**
	 * Establish context on thread and set the given classLoader as the thread context class loader
	 */
	public void establishContext( ClassLoader cl);
	
	/**
	 * Remove the established context from thread
	 */
	public void removeContext( ClassLoader cl);
	
	/**
	 * Returns the application ClassLoader
	 * @return
	 */
	public ClassLoader getApplicationClassLoader();
	
	/**
	 * Return the current thread class loader (wrap java 2 security)
	 * @return
	 */
	public ClassLoader getThreadCurrentClassLoader();
	
	/**
	 * Getter for servlet context
	 */
	public ServletContext getServletContext();

	
}
