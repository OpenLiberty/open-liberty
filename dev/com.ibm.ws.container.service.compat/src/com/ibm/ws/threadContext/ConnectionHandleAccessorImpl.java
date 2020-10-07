/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threadContext;

import com.ibm.ws.jca.cm.handle.HandleListInterface;

/**
 * Stub until LazyAssociatableConnectionManager is designed with J2C.
 */
public class ConnectionHandleAccessorImpl {
    private static final ConnectionHandleAccessorImpl instance = new ConnectionHandleAccessorImpl();

    public static ConnectionHandleAccessorImpl getConnectionHandleAccessor() {
        return instance;
    }

    private final ThreadContext<HandleListInterface> threadContext = new ThreadContextImpl<HandleListInterface>();

    public ThreadContext<HandleListInterface> getThreadContext() {
        return threadContext;
    }
}
