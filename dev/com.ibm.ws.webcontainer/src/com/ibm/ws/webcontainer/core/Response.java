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
import javax.servlet.ServletResponse;

import com.ibm.websphere.servlet.response.IResponse;



public interface Response extends ServletResponse
{
	public void start();
	
	public void finish() throws ServletException;
	
	public void initForNextResponse(IResponse res);
    
    //public int getStatusCode();  //340473
    
    public void destroy();
}
