/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.internal.interfaces;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Provides the services needed for Reactive Messaging components to do asynchronous tasks
 * <p>
 * Instances must be created via {@link RMAsyncProviderFactory}
 */
public interface RMAsyncProvider {

    /**
     * Captures the context from the current thread
     * <p>
     * The returned object can be used to apply this context to another thread
     *
     * @return the context object
     * @throws IllegalArgumentException if a named context service is requested but could not be found
     */
    RMContext captureContext();

    /**
     * Gets the executor service to use
     *
     * @return the executor service
     */
    ExecutorService getExecutorService();

    /**
     * Gets the scheduled executor service to use
     *
     * @return the scheduled executor service
     */
    ScheduledExecutorService getScheduledExecutorService();

}
