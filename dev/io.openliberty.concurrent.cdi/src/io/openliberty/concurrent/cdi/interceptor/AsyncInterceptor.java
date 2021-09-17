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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.Priority;
import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transactional;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import prototype.enterprise.concurrent.Async;

@Async
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 5)
public class AsyncInterceptor implements Serializable {
    private static final long serialVersionUID = 7447792334053278336L;

    @AroundInvoke
    @FFDCIgnore({ ClassCastException.class, NamingException.class }) // application errors raised directly to the app
    public Object intercept(InvocationContext context) throws Exception {
        Method method = context.getMethod();
        validateTransactional(method);
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

        AsyncMethod asyncMethod = new AsyncMethod(executor, context);
        Future<?> policyTaskFuture = executor.submit(asyncMethod, asyncMethod);
        // If caller cancels the future, cancel the policy executor task that will run it
        // TODO - should this action be taken whenever completed prematurely?
        asyncMethod.future.exceptionally(failure -> { // TODO this doesn't need thread context overhead
            if (failure instanceof CancellationException)
                policyTaskFuture.cancel(true);
            return null;
        });
        return asyncMethod.future;
    }

    /**
     * Limits the pairing of @Async and @Transactional to NOT_SUPPORTED and REQUIRES_NEW.
     *
     * @param method annotated method.
     * @throws UnsupportedOperationException for unsupported combinations.
     */
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
                    throw new UnsupportedOperationException("@Async @Transactional(" + tx.value().name() + ")");
            }
    }

    private static class AsyncMethod implements Runnable, ManagedTask, ManagedTaskListener {
        private final Map<String, String> execProps;
        private final CompletableFuture<Object> future;
        private final InvocationContext invocation;

        @Trivial
        private AsyncMethod(ManagedExecutorService executor, InvocationContext invocation) throws Exception {
            execProps = Collections.singletonMap(ManagedTask.IDENTITY_NAME, "@Async " + invocation.getMethod().getName());
            // TODO directly invoke executor.newIncompleteFuture() when v3.0 interface is added
            future = (CompletableFuture<Object>) executor.getClass().getMethod("newIncompleteFuture").invoke(executor);
            this.invocation = invocation;
        }

        @Override
        @Trivial
        public Map<String, String> getExecutionProperties() {
            return execProps;
        }

        @Override
        @Trivial
        public ManagedTaskListener getManagedTaskListener() {
            return this;
        }

        /**
         * Runs the async method.
         */
        @FFDCIgnore(Throwable.class) // application errors are raised directly to the appliaction
        @Override
        public void run() {
            Async.Result.setFuture(future);
            try {
                Object returnVal = invocation.proceed();
                if (returnVal != future)
                    if (returnVal instanceof CompletionStage) { // which includes CompletableFuture
                        @SuppressWarnings("unchecked")
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
        }

        /**
         * Complete the future exceptionally if the task is aborted.
         */
        @Override
        public void taskAborted(Future<?> policyTaskFuture, ManagedExecutorService executor, Object task, Throwable exception) {
            if (exception instanceof AbortedException && exception.getCause() != null)
                exception = new CancellationException(exception.getMessage()).initCause(exception.getCause());
            future.completeExceptionally(exception);
        }

        @Override
        @Trivial
        public void taskDone(Future<?> policyTaskFuture, ManagedExecutorService executor, Object task, Throwable exception) {
        }

        @Override
        @Trivial
        public void taskStarting(Future<?> policyTaskFuture, ManagedExecutorService executor, Object task) {
        }

        @Override
        @Trivial
        public void taskSubmitted(Future<?> policyTaskFuture, ManagedExecutorService executor, Object task) {
        }
    }
}