/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.web.server.version20.container;

import java.util.concurrent.atomic.AtomicReference;

interface LibertyFactoryBase {

    static final AtomicReference<LibertyWebServer> usingDefaultHost = new AtomicReference<>();

    String getContextPath();

    boolean shouldUseDefaultHost(LibertyWebServer container);

    default boolean acquireDefaultHost(LibertyWebServer container) {
        // only use default host if this is the first container
        return usingDefaultHost.compareAndSet(null, container);
    }

    default void stopUsingDefaultHost(LibertyWebServer container) {
        usingDefaultHost.compareAndSet(container, null);
    }
}
