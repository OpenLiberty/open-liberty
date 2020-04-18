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

import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Represents thread context configuration for ThreadContext and ManagedExecutor instances.
 */
public interface ThreadContextConfig {
    /**
     * Creates a new ThreadContextDescriptor instance based on the config and current thread state.
     *
     * @return the new instance.
     */
    ThreadContextDescriptor captureThreadContext();
}
