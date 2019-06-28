/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.feature;

/**
 * Components wishing to receive a synchronous notification that the server is
 * started should register an implementation of this service implementation.
 * Asychronous notifications are provided by watching for the ServerStarted
 * service being registered. If the server is already started when this method
 * is called then it will be called immediately.
 */
public interface SynchronousServerStartedListener {

    /**
     * This method is called prior to the ServerStarted service being registered
     * and the server started log message being written. It is called synchronous
     * to startup meaning those other two events will not occur until this method
     * has returned.
     */
    public void serverStarted();
}
