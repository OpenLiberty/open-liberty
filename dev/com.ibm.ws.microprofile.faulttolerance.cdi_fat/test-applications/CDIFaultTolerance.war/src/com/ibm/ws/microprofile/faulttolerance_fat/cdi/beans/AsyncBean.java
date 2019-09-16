/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;

@RequestScoped
public class AsyncBean {

    @Asynchronous
    public Future<String> runTask(SyntheticTask<String> task) {
        return CompletableFuture.completedFuture(task.call());
    }

    @Asynchronous
    public Future<Void> runTaskVoid(SyntheticTask<Void> task) {
        return CompletableFuture.completedFuture(task.call());
    }

    @Timeout(TestConstants.TIMEOUT)
    @Asynchronous
    public Future<String> runTaskTimeout(SyntheticTask<String> task) {
        return CompletableFuture.completedFuture(task.call());
    }

    /**
     * Returns the current thread ID
     * <p>
     * Annotated with {@code @Asynchronous} so it's good for checking whether it's really running asynchronously.
     *
     * @return the ID of the thread where this method runs
     */
    @Asynchronous
    public Future<Long> getThreadId() {
        return CompletableFuture.completedFuture(Thread.currentThread().getId());
    }
}
