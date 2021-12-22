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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transactional;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.enterprise.concurrent.Asynchronous;

@Asynchronous
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 5)
public class AsyncInterceptor implements Serializable {
    private static final long serialVersionUID = 7447792334053278336L;

    @AroundInvoke
    @FFDCIgnore({ ClassCastException.class, NamingException.class }) // application errors raised directly to the app
    public Object intercept(InvocationContext invocation) throws Exception {
        Method method = invocation.getMethod();
        validateTransactional(method);
        if (method.getDeclaringClass().getAnnotation(Asynchronous.class) != null)
            throw new UnsupportedOperationException("@Asynchronous " + method.getDeclaringClass()); // TODO NLS?

        Asynchronous anno = method.getAnnotation(Asynchronous.class);

        // @Asynchronous must be on a method that returns completion stage or void
        Class<?> returnType = method.getReturnType();
        if (!returnType.equals(CompletableFuture.class)
            && !returnType.equals(CompletionStage.class)
            && !returnType.equals(Void.TYPE)) // void
            throw new UnsupportedOperationException("@Asynchronous " + returnType.getName() + " " + method.getName()); // TODO NLS?

        WSManagedExecutorService executor;
        try {
            executor = InitialContext.doLookup(anno.executor());
        } catch (ClassCastException x) {
            throw new RejectedExecutionException("executor: " + anno.executor(), x); // TODO NLS?
        } catch (NamingException x) {
            throw new RejectedExecutionException(x);
        }

        return executor.newAsyncMethod(this::invoke, invocation);
    }

    /**
     * Invokes the asynchronous method either on a thread from the managed executor, or possibly
     * inline in response to a join or untimed get.
     *
     * @param <T>        type of result.
     * @param invocation interceptor's invocation context.
     * @param future     CompletableFuture that will be returned to the caller of the asynchronous method.
     * @return completion stage (or null) that is returned by the asynchronous method.
     * @throws CompletionException if the asynchronous method invocation raises an exception or error.
     */
    @FFDCIgnore(Throwable.class) // errors raised by an @Asynchronous method implementation
    public <T> CompletionStage<T> invoke(InvocationContext invocation, CompletableFuture<T> future) {
        Asynchronous.Result.setFuture(future);
        try {
            @SuppressWarnings("unchecked")
            CompletionStage<T> asyncMethodResultStage = (CompletionStage<T>) invocation.proceed();
            return asyncMethodResultStage;
        } catch (Throwable x) {
            throw (x instanceof CompletionException ? (CompletionException) x : new CompletionException(x));
        } finally {
            Asynchronous.Result.setFuture(null);
        }
    }

    /**
     * Limits the pairing of @Asynchronous and @Transactional to NOT_SUPPORTED and REQUIRES_NEW.
     *
     * @param method annotated method.
     * @throws UnsupportedOperationException for unsupported combinations.
     */
    @Trivial
    private static void validateTransactional(Method method) throws UnsupportedOperationException {
        Transactional tx = method.getAnnotation(Transactional.class);
        if (tx == null)
            tx = method.getDeclaringClass().getAnnotation(Transactional.class);
        if (tx != null)
            switch (tx.value()) {
                case NOT_SUPPORTED:
                case REQUIRES_NEW:
                    break;
                default:
                    throw new UnsupportedOperationException("@Asynchronous @Transactional(" + tx.value().name() + ")");
            }
    }
}