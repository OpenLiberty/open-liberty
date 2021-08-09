/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.web.interfaces;

import com.ibm.ws.webcontainer.async.AsyncContextImpl;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public interface PostEventListenerProvider {

    /*
     * This method is called to notify the provider to register any listener which must be
     * registered after any application listener.
     * For application start the registration order is:
     * 1. PreEventProvider listeners. Highest priority goes first.
     * 2. Application Listeners.
     * 3. Listeners registered by a servlet context listener.
     * 4. PostEventProvider listeners. Highest priority goes last.
     */
    public void registerListener(IServletContext sc);

    /*
     * This method is called to notify the provider to register an asyncListener which must be called
     * after any application asyncListeners.
     * AyncListeners are notified in this order"
     * 1. PreEventProvider asyncListeners. Highest priority goes first.
     * 2. Application AsyncListeners
     * 3. PostEventPorvider asyncListeners. Highest priority last.
     */
    public void registerListener(IServletContext sc, AsyncContextImpl ac);

}
