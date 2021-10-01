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
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import prototype.enterprise.concurrent.Async;

@ApplicationScoped
@Async
public class ApplicationScopedBean implements Serializable {
    private static final long serialVersionUID = -2075274815197982538L;

    private char character;

    /**
     * This method should run async because of the class level annotation
     * and its return type of CompletableFuture.
     */
    public CompletableFuture<String> appendThreadNameFuture(String part1) {
        return Async.Result.complete(part1 + getCharacter() + Thread.currentThread().getName());
    }

    /**
     * This method should run async because of the class level annotation
     * and its return type of CompletableFuture.
     */
    public CompletionStage<String> appendThreadNameStage(String part1) {
        try {
            ManagedExecutorService executor = InitialContext.doLookup("java:comp/env/concurrent/executorRef");
            // TODO invoke directly once added to spec:
            // return executor.completedStage(part1 + getCharacter() + Thread.currentThread().getName());
            Method completedStage = executor.getClass().getMethod("completedStage", Object.class);
            @SuppressWarnings("unchecked")
            CompletionStage<String> stage = (CompletionStage<String>) completedStage //
                            .invoke(executor, part1 + getCharacter() + Thread.currentThread().getName());
            return stage;
        } catch (NamingException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException x) {
            throw new CompletionException(x);
        }
    }

    /**
     * Asynchronous method that intentionally raises an error, for testing purposes.
     */
    public CompletableFuture<Integer> forceError() {
        throw new Error("Intentionally raising this error.");
    }

    public char getCharacter() {
        return character;
    }

    /**
     * Looks up a resource in JNDI, asynchronously to the calling thread.
     */
    public CompletableFuture<?> lookup(String jndiName) {
        try {
            return CompletableFuture.completedFuture(InitialContext.doLookup(jndiName));
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    /**
     * This is not an async method despite the class level annotation
     * because its return type is void.
     *
     * @param threadNameRef reference into which to put the name of the
     *                          thread where this method runs.
     */
    public void notAsync(AtomicReference<String> threadNameRef) {
        threadNameRef.set(Thread.currentThread().getName());
    }

    public void setCharacter(char character) {
        this.character = character;
    }
}
