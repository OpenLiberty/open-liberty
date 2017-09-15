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
package com.ibm.ws.webcontainer.servlet;

import com.ibm.wsspi.webcontainer.servlet.IServletConfig;


public class ServletMapping
{
	private String urlPattern;
	private IServletConfig servletConfig;
	
	/**
	 * Constructor.
	 *
	 * @param config
	 * @param pattern
	 */
	public ServletMapping(IServletConfig config, String pattern)
	{
		this.servletConfig = config;
		this.urlPattern = pattern;
	}
	
	/**
	 * Constructor.
	 */
	public ServletMapping()
	{
	    // nothing
	}
	
	/**
	 * Returns the servletConfig.
	 * @return ServletConfig
	 */
	public IServletConfig getServletConfig() {
		return this.servletConfig;
	}

	/**
	 * Returns the urlPattern.
	 * @return String
	 */
	public String getUrlPattern() {
		return this.urlPattern;
	}

	/**
	 * Sets the servletConfig.
	 *
	 * @param config
	 */
	public void setServletConfig(IServletConfig config) {
		this.servletConfig = config;
	}

	/**
	 * Sets the urlPattern.
	 *
	 * @param pattern
	 */
	public void setUrlPattern(String pattern) {
		this.urlPattern = pattern;
	}
	
	public String toString()
	{
		return ""+this.servletConfig.getServletName()+":"+this.urlPattern;
	}

}
