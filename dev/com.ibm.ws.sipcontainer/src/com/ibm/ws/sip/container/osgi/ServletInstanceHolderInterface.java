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
package com.ibm.ws.sip.container.osgi;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipServlet;

public interface ServletInstanceHolderInterface {

	/**
	 * 
	 * @param appName
	 * @param className
	 * @param servletInstance
	 */
	public void addSipletInstance(String appName,String className,Object servletInstance);
	
	/**
	 * Store members to allow later on trigger of listener
	 * 
	 * @param appName - Application name
	 * @param sipServlet - sip servlet instance reference
	 * @param sipletContext - sip servlet context
	 */
	public void saveSipletReference(String appName, SipServlet sipServlet, ServletContext sipletContext);
	
	
	/**
	 * Trigger listener
	 */
	public void triggerSipletInitServlet(long appQueueIndex);
	
	
	/**
	 *  servlets are stored to allow later the servlet initialized call
	 */
	public void saveOnStartupServlet();
	
	

}
