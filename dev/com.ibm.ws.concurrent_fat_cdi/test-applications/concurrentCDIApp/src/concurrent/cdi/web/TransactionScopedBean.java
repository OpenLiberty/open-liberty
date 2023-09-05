/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package concurrent.cdi.web;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.transaction.TransactionScoped;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@TransactionScoped
public class TransactionScopedBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private AtomicInteger intRef = new AtomicInteger();

    public int get() {
        return intRef.get();
    }

    public int increment(int amount) {
        return intRef.addAndGet(amount);
    }

    @Asynchronous
    public CompletableFuture<Integer> incrementAsync(int amount) {
        try {
            // Requires application component's context:
            ManagedExecutorService executor = InitialContext.doLookup("java:module/env/concurrent/timeoutExecutorRef");
            if (executor == null)
                throw new AssertionError("Null result of resource reference lookup.");

            return Asynchronous.Result.complete(intRef.addAndGet(amount));
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    public void set(int amount) {
        intRef.set(amount);
    }
}
