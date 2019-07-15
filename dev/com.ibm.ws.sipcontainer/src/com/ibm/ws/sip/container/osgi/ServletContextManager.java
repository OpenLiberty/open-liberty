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
package com.ibm.ws.sip.container.osgi;

import javax.servlet.ServletConfig;

public class ServletContextManager {
	
	private static ServletContextManager instance = null;
	private ServletContextFactory factory = null;
	

	/**
	 * 
	 * @return single instance of ServetContextManager
	 */
	public static ServletContextManager getInstance(){
		if (instance == null){
			instance = new ServletContextManager();
		}
		
		return instance;
	}
	
	/**
	 * 
	 * @param factory
	 */
	public void setContextFactory(ServletContextFactory factory){
		this.factory = factory;
	}
	
	/**
	 * 
	 * @return contextFactory
	 */
	public ServletContextFactory getContextFactory(){
		return factory;
	}

	/**
	 * 
	 * @param ctx
	 * @return ServletConfig
	 */
	public ServletConfig wrapContext(ServletConfig ctx){
		return factory.wrapContext(ctx);
	}
	
	

}
