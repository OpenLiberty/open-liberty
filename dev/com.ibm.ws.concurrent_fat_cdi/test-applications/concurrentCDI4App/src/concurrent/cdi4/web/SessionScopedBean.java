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

import static jakarta.enterprise.concurrent.ContextServiceDefinition.SECURITY;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.SessionScoped;

import javax.naming.InitialContext;
import javax.naming.NamingException;

// When ALL_REMAINING isn't specified for any category (cleared/propagated/unchanged),
// it is automatically added to cleared
@ContextServiceDefinition(name = "java:global/concurrent/allcontextcleared",
                          cleared = SECURITY,
                          propagated = {},
                          unchanged = {})
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
    @Asynchronous(executor = "java:app/env/concurrent/sampleExecutorRef")
    public CompletableFuture<Boolean> await(CountDownLatch blocker, long time, TimeUnit unit,
                                            LinkedBlockingQueue<Object> lookedUpResources) {
        try {
            // Requires application component's context:
            ManagedExecutorService executor = InitialContext.doLookup("java:module/env/concurrent/timeoutExecutorRef");
            lookedUpResources.add(executor);

            return executor.completedFuture(blocker.await(time, unit));
        } catch (InterruptedException | NamingException x) {
            throw new CompletionException(x);
        }
    }

    public String getText() {
        return text;
    }

    @Asynchronous(executor = "java:comp/UserTransaction")
    public CompletionStage<String> jndiNameNotAnExecutor() {
        throw new AssertionError("This should be unreachable because java:comp/UserTransaction is not an executor");
    }

    @Asynchronous(executor = "java:comp/env/concurrent/doesNotExistRef")
    public CompletionStage<String> jndiNameNotFound() {
        throw new AssertionError("This should be unreachable due to the executor JNDI name not being found!");
    }

    public void setText(String text) {
        this.text = text;
    }
}
