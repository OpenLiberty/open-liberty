/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.SessionScoped;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import prototype.enterprise.concurrent.Async;

@SessionScoped
public class SessionScopedBean implements Serializable {
    private static final long serialVersionUID = -3903544320641023668L;

    private String text;

    /**
     * First, looks up a resource from the application component's name space
     * and adds it to the supplied queue.
     * Then, waits up to the specified time for the specified latch,
     * returning true if the latch is counted down
     * and false if we time out waiting for it.
     */
    @Async(executor = "java:app/env/concurrent/sampleExecutorRef")
    public CompletableFuture<Boolean> await(CountDownLatch blocker, long time, TimeUnit unit,
                                            LinkedBlockingQueue<Object> lookedUpResources) {
        try {
            // Requires application component's context:
            ManagedExecutorService executor = InitialContext.doLookup("java:module/env/concurrent/timeoutExecutorRef");
            lookedUpResources.add(executor);

            // TODO invoke directly once Concurrency 3.0 interface is available
            //return executor.completedFuture(blocker.await(time, unit));
            @SuppressWarnings("unchecked")
            CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) executor.getClass()
                            .getMethod("completedFuture", Object.class)
                            .invoke(executor, blocker.await(time, unit));
            return future;
        } catch (InterruptedException | NamingException // TODO remove the following execeptions along with above
                        | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException x) {
            throw new CompletionException(x);
        }
    }

    public String getText() {
        return text;
    }

    @Async(executor = "java:comp/UserTransaction")
    public CompletionStage<String> jndiNameNotAnExecutor() {
        throw new AssertionError("This should be unreachable because java:comp/UserTransaction is not an executor");
    }

    @Async(executor = "java:comp/env/concurrent/doesNotExistRef")
    public CompletionStage<String> jndiNameNotFound() {
        throw new AssertionError("This should be unreachable due to the executor JNDI name not being found!");
    }

    public void setText(String text) {
        this.text = text;
    }
}
