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
package com.ibm.ws.concurrent.internal;

import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.threading.CancellableStage;

/**
 * Abstraction that enables PolicyExecutor to cancel completion stages.
 */
@Trivial
class CancellableStageRef extends AtomicReference<ManagedCompletableFuture<?>> implements CancellableStage {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        ManagedCompletableFuture<?> cf = get();
        return cf != null && cf.super_cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean completeExceptionally(Throwable x) {
        ManagedCompletableFuture<?> cf = get();
        return cf != null && cf.super_completeExceptionally(x);
    }
}
