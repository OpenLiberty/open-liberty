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
 * This event context is used to register listeners for various servlet context events.
 * These events will be triggered by the servlet engine as appropriate during servlet
 * processing. An implementation of this event context is available to all servlets
 * as a ServletContext attribute by using the ServletContext.getAttribute()
 * method.
 *
 * <h3>Sample Usage (from within a servlet):</h3>
 * <pre>
 * ServletContextEventSource sces = (ServletContextEventSource)getServletContext().getAttribute(
 *                                 ServletContextEventSource.ATTRIBUTE_NAME);
 * sces.addServletErrorListener(myErrorListener);
 * </pre>
 * 
 * @ibm-api
 */
public interface ServletContextEventSource{
    /**
     * The ServletContext attribute name that the servlet context event source can be retrieved using.
     */
    public static final String ATTRIBUTE_NAME = "com.ibm.websphere.servlet.event.ServletContextEventSource";

    /**
     * Register a listener for application events.
     */
    public void addApplicationListener(ApplicationListener al);

    /**
     * Deregister a listener for application events.
     */
    public void removeApplicationListener(ApplicationListener al);

    /**
     * Register a listener for servlet invocation events.
     */
    public void addServletInvocationListener(ServletInvocationListener sil);

    /**
     * Deregister a listener for servlet invocation events.
     */
    public void removeServletInvocationListener(ServletInvocationListener sil);

    /**
     * Register a listener for servlet error events.
     */
    public void addServletErrorListener(ServletErrorListener sel);

    /**
     * Deregister a listener for servlet error events.
     */
    public void removeServletErrorListener(ServletErrorListener sel);

    /**
     * Register a listener for servlet events.
     */
    public void addServletListener(ServletListener sl);

    /**
     * Deregister a listener for servlet events.
     */
    public void removeServletListener(ServletListener sl);
    

    /**
     * Register a listener for filter invocation events.
     */
    public void addFilterInvocationListener(FilterInvocationListener fil);

    /**
     * Deregister a listener for filter invocation events.
     */
    public void removeFilterInvocationListener(FilterInvocationListener fil);
    
    /**
     * Register a listener for filter error events.
     */
    public void addFilterErrorListener(FilterErrorListener fil);

    /**
     * Deregister a listener for filter error events.
     */
    public void removeFilterErrorListener(FilterErrorListener fil);
    
    /**
     * Register a listener for filter events.
     */
    public void addFilterListener(FilterListener fil);

    /**
     * Deregister a listener for filter events.
     */
    public void removeFilterListener(FilterListener fil);

}
