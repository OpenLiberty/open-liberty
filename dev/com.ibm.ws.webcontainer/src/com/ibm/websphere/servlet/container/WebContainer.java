/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.servlet.container;

import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet.response.IResponse;

public abstract class WebContainer {

    // RTC 160610. Moving Feature from WebContainerConstants.
    public static enum Feature {RRD,ARD};

    /**
    *
    * @return The instance of the WebContainer
    * 
    * Call this method to get at an instance of the WebContainer
    */
	public static WebContainer getWebContainer() {
		
		return com.ibm.wsspi.webcontainer.WebContainer.getWebContainer();
	}
	
	/**
    *
    * @param req
    * @param res
    * @throws Exception
    * 
    * Call this method to force the webcontainer to handle the request. The request
    * should have enough information in it for the webcontainer to handle the request.
    */
	public abstract void handleRequest(IRequest req, IResponse res) throws Exception;
	
	
	/**
    *
    * @param classname
    * 
    * Adds a global servlet listener with the specified classname. 
    * The class must be on the classpath of all web applications. For example,
    * the /lib directory at the root of the application server.
    */
	public static void addGlobalListener(String className){
		com.ibm.ws.webcontainer.WebContainer.addGlobalListener(className);
	}
	
}
