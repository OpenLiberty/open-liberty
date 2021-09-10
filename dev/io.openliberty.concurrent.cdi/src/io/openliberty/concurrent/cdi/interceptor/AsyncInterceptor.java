/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.cdi.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.annotation.Priority;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import prototype.enterprise.concurrent.Async;

@Async
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 10)
public class AsyncInterceptor implements Serializable {
    private static final long serialVersionUID = 7447792334053278336L;

    @AroundInvoke
    @FFDCIgnore({ ClassCastException.class, NamingException.class }) // application errors raised directly to the app
    public Object intercept(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        Async methodAnno = method.getAnnotation(Async.class);
        Async anno = methodAnno == null ? method.getDeclaringClass().getAnnotation(Async.class) : methodAnno;
        Class<?> returnType = method.getReturnType();
        boolean returnsCompletionStage = returnType.equals(CompletableFuture.class)
                                         || returnType.equals(CompletionStage.class);
        // Class level @Async only applies to methods that return completion stages
        if (methodAnno == null && !returnsCompletionStage)
            return context.proceed();
        // Method level @Async must be on a method that returns completion stage or void
        if (!returnsCompletionStage && !returnType.equals(Void.class))
            throw new UnsupportedOperationException("@Async with return type " + returnType.getName());

        ManagedExecutorService executor;
        try {
            executor = InitialContext.doLookup(anno.executor());
        } catch (ClassCastException x) {
            throw new RejectedExecutionException("executor: " + anno.executor(), x);
        } catch (NamingException x) {
            throw new RejectedExecutionException(x);
        }
        // TODO directly invoke executor.newIncompleteFuture() when v3.0 interface is added
        CompletableFuture<Object> future = (CompletableFuture<Object>) executor.getClass().getMethod("newIncompleteFuture").invoke(executor);
        Future<?> policyTaskFuture = executor.submit(() -> {
            Async.Result.setFuture(future);
            try {
                Object returnVal = context.proceed();
                if (returnVal != future)
                    if (returnVal instanceof CompletionStage) { // which includes CompletableFuture
                        CompletionStage<Object> stage = (CompletionStage<Object>) returnVal;
                        stage.whenComplete((result, failure) -> {
                            if (failure == null)
                                future.complete(result);
                            else
                                future.completeExceptionally(failure);
                        });
                    } else if (returnVal == null) {
                        future.complete(null);
                    } else { // returned object is not a CompletionStage or CompletableFuture
                        throw new UnsupportedOperationException("@Async with result type " + returnVal.getClass().getName());
                    }
            } catch (Throwable x) {
                future.completeExceptionally(x);
                // TODO when is setRollbackOnly appropriate?
            } finally {
                Async.Result.setFuture(null);
            }
        });
        // If caller cancels the future, cancel the policy executor task that will run it
        // TODO - should this action be taken whenever completed prematurely?
        future.exceptionally(failure -> {
            if (failure instanceof CancellationException)
                policyTaskFuture.cancel(true);
            return null;
        });
        return future;
    }
}