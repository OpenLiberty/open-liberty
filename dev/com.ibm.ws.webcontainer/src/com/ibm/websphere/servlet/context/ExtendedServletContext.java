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
package com.ibm.websphere.servlet.context;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import com.ibm.websphere.servlet.filter.IFilterConfig;
import com.ibm.websphere.webcontainer.async.AsyncRequestDispatcher;

/**
 * Servlet Context Extensions for IBM WebSphere Application Server
 * 
 * @ibm-api
 */
public interface ExtendedServletContext extends ServletContext{
	/**
	 * Gets the IFilterConfig object for this context or creates
     * one if it doesn't exist.
	 * @param id
	 * @return
	 */
        public IFilterConfig getFilterConfig(String id);
    
        /**
         * Adds a filter against a specified mapping into this context
         * @param mapping
         * @param config
         */
        public void addMappingFilter(String mapping, IFilterConfig config);
        

        /**
         * Returns an asynchronous request dispatcher to do asynchronous includes
         * @param path
         */
        public AsyncRequestDispatcher getAsyncRequestDispatcher(String path);
        
        /**
         * Returns a map of all the dynamic servlet registrations keyed by name
         */
        public Map<String, ? extends ServletRegistration.Dynamic> getDynamicServletRegistrations();

}
