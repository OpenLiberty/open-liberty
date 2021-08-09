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
package com.ibm.websphere.servlet.filter;

import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;

/**
 * A representation of the configuration for a filter
 * 
 * @ibm-api
 */
public interface IFilterConfig extends FilterConfig, FilterRegistration.Dynamic{
	
	/**
	 * @deprecated
	 */
    public static final int FILTER_REQUEST = 0;
    /**
	 * @deprecated
	 */
    public static final int FILTER_FORWARD = 1;
    /**
	 * @deprecated
	 */
    public static final int FILTER_INCLUDE = 2;
    /**
	 * @deprecated
	 */
    public static final int FILTER_ERROR = 3;
        
    /**
     * Set the ClassLoader this Filter should be loaded from.
     * @param filterClassLoader
     */
	public void setFilterClassLoader(ClassLoader filterClassLoader);
	//  end 296658    allow FilterConfig to override the default classloader used    WASCC.web.webcontainer
    /**
     * Add an init parmameter
     * @param name
     * @param value
     */
    public void addInitParameter(String name, String value);

//    /**
//     * Set the dispatch types this filter should run for
//     * @param dispatchType
//     */
//    public void setDispatchType(DispatcherType[] dispatchMode);
//    
    /**
     * Set the dispatch mode this filter should run for
     * @deprecated Please use Servlet 3.0 methods for adding filters dynamically.
     * @param dispatchMode
     */
    public void setDispatchMode(int[] dispatchMode);
    
    /**
     * Set the display name for this config
     * @param displayName
     */
    public void setDisplayName(String displayName);

    /**
     * Set the discription for this config
     * @param description
     */
    public void setDescription(String description);

    /**
     * Set the name of this filter
     * @param name
     */
    public void setName(String name);
    
    public void setFilterClassName(String className);
    
    public boolean isAsyncSupported();
}
