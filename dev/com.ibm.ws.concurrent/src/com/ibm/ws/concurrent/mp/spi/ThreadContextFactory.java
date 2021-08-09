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
package com.ibm.ws.concurrent.mp.spi;

import org.eclipse.microprofile.context.ThreadContext;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.internal.ThreadContextImpl;

/**
 * Provides a static method that enables the ThreadContextBuilderImpl,
 * which is in a different bundle, to create ThreadContext instances.
 */
@Trivial
public class ThreadContextFactory {
    /**
     * Creates a new MicroProfile ThreadContext instance.
     *
     * @param name   unique name for the new instance.
     * @param hash   hash code for the new instance.
     * @param config represents thread context propagation configuration.
     * @return the new instance.
     */
    public static ThreadContext createThreadContext(String name, int hash, ThreadContextConfig config) {
        return new ThreadContextImpl(name, hash, config);
    }
}
