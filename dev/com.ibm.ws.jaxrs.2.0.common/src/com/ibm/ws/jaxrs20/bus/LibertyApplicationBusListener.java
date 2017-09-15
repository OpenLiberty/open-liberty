/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.bus;

import org.apache.cxf.Bus;

/**
 * The interface is used to receiving the Bus lifecycle events. The most usage is that, the provider hopes to
 * provide extra functions for each bus instanced created.
 * 
 * CXF also provides a BusLifecycleListener, the key difference is that, @link {@link #preInit(Bus)} is provided, which
 * could be used to execute any actions prior to any CXF embedded bus listener.
 * 
 * The implementor must register itself in LibertyApplicationBusFactory, or declare as a DS service.
 * 
 */
public interface LibertyApplicationBusListener {

    /**
     * Invoke on the initialization for the bus instance before any CXF extensions are loaded
     * 
     * @param bus
     */
    public void preInit(Bus bus);

    /**
     * Invoked while the bus is successfully initialized, it will be invoked after all the CXF extensions are loaded and
     * BusLifeCycleListener.initComplete invocation
     * 
     * @param bus
     */
    public void initComplete(Bus bus);

    /**
     * Invoked before the bus is shutdown
     * 
     * @param bus
     */
    public void preShutdown(Bus bus);

    /**
     * Invoked after the bus is shutdown
     * 
     * @param bus
     */
    public void postShutdown(Bus bus);
}
