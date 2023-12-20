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

/**
 * Represents a context which can be applied to a thread
 * <p>
 * Instances can be created via {@link RMAsyncProvider#captureContext()}
 */
public interface RMContext {

    /**
     * An RM context object which applies no context
     */
    RMContext NOOP = Runnable::run;

    /**
     * Runs a runnable using the context captured by this object
     *
     * @param runnable the runnable to run
     */
    void execute(Runnable runnable);
}
