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
package com.ibm.wsspi.webcontainer.filter;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * A representation of the configuration for a filter
 * 
 * @ibm-private-in-use
 */
public interface IFilterConfig extends com.ibm.websphere.servlet.filter.IFilterConfig {

    /**
     * Set the large icon
     * @param largeIcon
     */
    public void setLargeIcon(String largeIcon);

    /**
     * Set the small icon
     * @param smallIcon
     */
    public void setSmallIcon(String smallIcon);


    /**
     * Get the dispatch type
     * @return
     */
    public DispatcherType[] getDispatchType();

    //  begin 296658    allow FilterConfig to override the default classloader used    WASCC.web.webcontainer
    /**
     * Get the classloader where this Filter should be loaded from.
     * Default is WebApp's classloader.
     * @return
     */
	public ClassLoader getFilterClassLoader();

    /**
     * Get the filter class name
     * @return
     */
    public String getFilterClassName();

    /**
     * Set the ServletContext this Filter should be associated with.
     * @param ServletContext
     */
	public void setIServletContext(IServletContext servletContext);

	/**
	 * Set whether resource should be considered internal. 
	 * 	 * @return
	 */
	public void setInternal(boolean isInternal);
	
	/**
	 * Checks if resource should be considered internal.
	 * @return
	 */
	public boolean isInternal();

    public void setFilter(Filter filter);

    public void setFilterClass(Class<? extends Filter> filterClass);
    
    public Filter getFilter();

    public Class<? extends Filter> getFilterClass();
    
    public void setFilterClassName(String className);

	
}
