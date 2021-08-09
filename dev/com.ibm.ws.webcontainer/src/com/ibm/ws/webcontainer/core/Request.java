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
package com.ibm.ws.webcontainer.core;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.wsspi.webcontainer.IRequest;


public interface Request extends ServletRequest
{
	public String getRequestURI();
	
	public void start();
	
	public void finish() throws ServletException;
	
	public void initForNextRequest(IRequest req);
	
	public void setWebAppDispatcherContext(WebAppDispatcherContext ctx);	
	
	public WebAppDispatcherContext getWebAppDispatcherContext();
	
	public String getServletPath();
	
	public String getPathInfo();
	
	public Response getResponse();
    
    public void destroy();
}
