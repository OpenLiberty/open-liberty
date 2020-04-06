/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets;

import javax.servlet.ServletConfig;

import com.ibm.ws.sip.container.osgi.ServletContextFactory;

public class ServletContextFactoryImpl implements ServletContextFactory{

	public ServletConfig wrapContext(ServletConfig ctx) {
		
		ServletConfig retContext = null;
		
    	if( ctx instanceof ServletConfigWrapper){
    		//in the WASX version we will get a ServletConfigWrapper 
    		//from the super.getServletConfig()
    		retContext = ctx;
    	}
    	else{
    		retContext = new ServletConfigWrapper(ctx);
    	}

    	return retContext;
	}
	
	
	

}
