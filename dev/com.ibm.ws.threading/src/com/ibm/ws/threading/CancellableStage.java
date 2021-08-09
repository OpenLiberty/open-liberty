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
package com.ibm.ws.threading;

/**
 * CancellableStage represents a CompletionStage that is tied to a PolicyTaskFuture,
 * but might be unavailable when the PolicyTaskFuture is created. This abstraction
 * allows it to be supplied at a later time, and also allows for the fact that
 * CompletionStage, unlike CompletableFuture, does not have a cancel method.
 */
public interface CancellableStage {
    /**
     * See corresponding method: java.util.concurrent.Future.cancel(mayInterruptIfRunning)
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * See corresponding method: java.util.concurrent.CompletableFuture.completeExceptionally(Throwable)
     */
    boolean completeExceptionally(Throwable x);
}
