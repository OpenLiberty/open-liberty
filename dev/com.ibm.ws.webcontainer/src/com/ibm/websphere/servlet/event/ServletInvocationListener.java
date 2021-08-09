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
 * Event listener interface used for notifications about Servlet service invocations.
 * Implementors of this interface must be very cautious about the time spent processing
 * these events because they occur on the servlet's actual request processing path.
 * 
 * @ibm-api
 */
public interface ServletInvocationListener extends java.util.EventListener{

    /**
     * Triggered just prior to the execution of Servlet.service().
     *
     * @see javax.servlet.service
     */
    public void onServletStartService(ServletInvocationEvent evt);

    /**
     * Triggered just after the execution of Servlet.service().
     *
     * @see javax.servlet.service
     */
    public void onServletFinishService(ServletInvocationEvent evt);

}
