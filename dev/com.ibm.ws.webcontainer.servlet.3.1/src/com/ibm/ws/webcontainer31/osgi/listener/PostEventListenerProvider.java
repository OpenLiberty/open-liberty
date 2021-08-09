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
package com.ibm.ws.webcontainer31.osgi.listener;


import com.ibm.ws.webcontainer.async.AsyncContextImpl;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * PostEventListenerProviders are notified to register listeners after all other listeners are registered during application start.
 * For application start the registration order is:
 * 1. PreEventProvider listeners.
 * 2. Application Listeners.
 * 3. Listeners registered by a servlet context listener.
 * 4. PostEventPorvider listeners
 * 
 * PostEventListenerProviders will be called in reverse service ranking order - the highest ranked service will be called last.
 */

public interface PostEventListenerProvider {
    
    public void registerListener(IServletContext sc);
    
    /*
     * This method is called just before the  AsyncListener onComplete. onError or onTimeout
     * methods are called. It can be used to register an AsyncListener which will be run after
     * any application AsyncListenes. 
     */
    public void registerListener(IServletContext sc, AsyncContextImpl ac);

}
