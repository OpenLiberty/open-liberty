/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.faulttolerance30.internal.test.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class ContextTestBean {

    @Asynchronous
    public Future<Void> runInAsync(Runnable runnable) {
        runnable.run();
        return CompletableFuture.completedFuture(null);
    }

    @Asynchronous
    @Fallback(fallbackMethod = "fallbackMethod")
    public Future<Void> runInFallback(Runnable runnable) {
        throw new RuntimeException();
    }

    @SuppressWarnings("unused") // Used as fallback method
    private Future<Void> fallbackMethod(Runnable runnable) {
        runnable.run();
        return CompletableFuture.completedFuture(null);
    }

    @Asynchronous
    public CompletionStage<Void> waitOnLatchCS(Future<?> latch) {
        try {
            latch.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(null);
    }
}