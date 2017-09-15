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
package com.ibm.websphere.servlet.event;


/**
 * Event listener interface used to receive notifications about filter errors.
 * 
 * @ibm-api
 */


public interface FilterErrorListener extends java.util.EventListener{

    /**
     * Triggered when an error occurs while executing the filter's init() method.
     * This method will be triggered if the filter throws an exception from its init() method.
     *
     * @see javax.servlet.Filter#init
     */
    public void onFilterInitError(FilterErrorEvent evt);

    /**
     * Triggered when an error occurs while executing the filter's doFilter() method.
     * This method will be triggered if the filter throws an exception from its
     * doFilter() method.
     *
     * @see javax.servlet.http.HttpServletResponse#sendError
     * @see javax.servlet.Filter#doFilter
     */
    public void onFilterDoFilterError(FilterErrorEvent evt);
 
    /**
     * Triggered when an error occurs while executing the filter's destroy() method.
     * This method will be triggered if the filter throws an exception from its destroy() method.
     *
     * @see javax.servlet.Servlet#destroy
     */
    public void onFilterDestroyError(FilterErrorEvent evt);

    
}
