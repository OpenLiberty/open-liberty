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
 * Event listener interface used to receive notifications about servlet errors.
 * 
 * @ibm-api
 */

public interface ServletErrorListener extends java.util.EventListener{

    /**
     * Triggered when an error occurs while executing the servlet's init() method.
     * This method will be triggered if the servlet throws an exception from its init() method.
     *
     * @see javax.servlet.Servlet#init
     */
    public void onServletInitError(ServletErrorEvent evt);

    /**
     * Triggered when an error occurs while executing the servlet's service() method.
     * This method will be triggered if the servlet throws an exception from its
     * service() method or if the servlet calls the response.sendError() method.
     *
     * @see javax.servlet.http.HttpServletResponse#sendError
     * @see javax.servlet.Servlet#service
     */
    public void onServletServiceError(ServletErrorEvent evt);
 
    /**
     * Triggered when an error occurs while executing the servlet's destroy() method.
     * This method will be triggered if the servlet throws an exception from its destroy() method.
     *
     * @see javax.servlet.Servlet#destroy
     */
    public void onServletDestroyError(ServletErrorEvent evt);

    /**
     * Triggered when a servlet request for service has been denied.
     * This event occurs when a request is received for a servlet, but
     * the servlet is not available to process the request.  This is typically
     * triggered when the servlet throws an UnavailableException or if the servlet
     * calls the response.sendError() method with an error code of
     * SC_SERVICE_UNAVAILABLE.
     *
     * @see javax.servlet.http.HttpServletResponse#SC_SERVICE_UNAVAILABLE
     * @see javax.servlet.http.HttpServletResponse#sendError
     * @see javax.servlet.UnavailableException
     */
    public void onServletServiceDenied(ServletErrorEvent evt);

}
