/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.spi;

import org.eclipse.microprofile.context.ThreadContext;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.internal.ContextServiceImpl;

/**
 * Provides a static method that enables the ThreadContextBuilderImpl,
 * which is in a different bundle, to create ThreadContext instances.
 */
@Trivial
public class ThreadContextFactory {
    /**
     * Creates a new MicroProfile ThreadContext instance.
     *
     * @param name      unique name for the new instance.
     * @param hash      hash code for the new instance.
     * @param eeVersion Jakarta/Java EE version that is enabled in the Liberty server.
     * @param config    represents thread context propagation configuration.
     * @return the new instance.
     */
    public static ThreadContext createThreadContext(String name, int hash, int eeVersion, ThreadContextConfig config) {
        return new ContextServiceImpl(name, hash, eeVersion, config);
    }
}
