/*******************************************************************************
 * Copyright (c) 2017,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi4.web;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.ALL_REMAINING;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.TRANSACTION;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.context.RequestScoped;

@ContextServiceDefinition(name = "java:comp/concurrent/txcontextunchanged",
                          propagated = APPLICATION,
                          unchanged = { "MyUnavailableContextType", TRANSACTION },
                          cleared = ALL_REMAINING)
@ManagedScheduledExecutorDefinition(name = "java:comp/concurrent/appContextExecutor",
                                    context = "java:comp/concurrent/txcontextunchanged")
@RequestScoped
public class RequestScopedBean extends ReqBeanSuperclass {
    private int number;

    /**
     * Asynchronous method that awaits the specified latch for up to the timeout.
     */
    @Asynchronous(executor = "java:module/env/concurrent/timeoutExecutorRef")
    public CompletionStage<Boolean> await(CountDownLatch latch, long timeout, TimeUnit unit) {
        try {
            return CompletableFuture.completedFuture(latch.await(timeout, unit));
        } catch (InterruptedException x) {
            throw new CompletionException(x);
        }
    }

    /**
     * Annotatively specify the executor of an asynchronous method. Return the executor that is actually used.
     */
    @Asynchronous(executor = "java:app/env/concurrent/sampleExecutorRef")
    public CompletableFuture<Executor> getExecutorOfAsyncMethods() throws Exception {
        CompletableFuture<Executor> future = Asynchronous.Result.getFuture();
        // CompletatbleFuture.defaultExecutor() is unavailable on Java 8 CompletableFuture
        Method CompletableFuture_defaultExecutor;
        try {
            CompletableFuture_defaultExecutor = CompletableFuture.class.getMethod("defaultExecutor");
            future.complete((Executor) CompletableFuture_defaultExecutor.invoke(future));
        } catch (NoSuchMethodException x) {
            future.completeExceptionally(x);
        }
        return future;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}
