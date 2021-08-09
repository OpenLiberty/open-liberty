/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.filter;

import javax.servlet.DispatcherType;

import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

public interface IFilterMapping {

	public abstract int getMappingType();

	/**
	 * Returns the filterConfig.
	 * @return FilterConfig
	 */
	public abstract IFilterConfig getFilterConfig();

	/**
	 * Returns the urlPattern.
	 * @return String
	 */
	public abstract String getUrlPattern();

	/**
	 * Sets the filterConfig.
	 * @param filterConfig The filterConfig to set
	 */
	public abstract void setFilterConfig(IFilterConfig filterConfig);

	/**
	 * Sets the urlPattern.
	 * @param urlPattern The urlPattern to set
	 */
	public abstract void setUrlPattern(String filterURI);

	public abstract IServletConfig getServletConfig();

	/**
	 * @return DispatcherType[]
	 */
	public abstract DispatcherType[] getDispatchMode();

	/**
	 * Sets the dispatchMode.
	 * @param dispatchMode The dispatchMode to set
	 */
	public abstract void setDispatchMode(DispatcherType[] dispatchMode);

}
