/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.extension;

import java.util.List;

import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 *
 * Extension factories are registered with the Webcontainer/WebcontainerService
 * as providers of handlers (ExtensionProcessors) for generic URL patterns. Any
 * request for resources that match the supplied URL pattern (except in the case
 * where there already exists a more specific pattern and matching target (possibly
 * supplied by the application assembler), will be routed to the ExtensionProcessor
 * supplied by the Factory.
 * @ibm-private-in-use
 */
public interface ExtensionFactory
{
	/**
	*
	* @param webapp - The WebApp that the created ExtensionProcessor should be associated with
	* @return An instance of WebExtensionProcessor which will be associated with the URL patterns
	* 			that this factory is associated with, and has the capability of handling requests.
	*
	* @throws Exception The creation process can throw any kind of exception, and it will 
	* be caught and logged by the webcontainer
	* 
	* This method will be called by the container during initialization. A WebExtensionFactory
	* typically furnishes a single type of WebExtensionProcessor, a singleton in most cases.
	*/
	public ExtensionProcessor createExtensionProcessor(IServletContext webapp) throws Exception;

	/**
	 *
	 * @return The list of all the URI patterns that the WebExtensionProcessors created by
	 * 			this factory will handle.
	 * 
	 * This method will be called by the webcontainer while setting up its internal URL
	 * routing datastructures. Patters supplied in this list should be in accord with the 
	 * allowable patterns specified in the Java Servlet Specification (under URL patterns).
	 */
	@SuppressWarnings("unchecked")
	public List getPatternList();
	
}
