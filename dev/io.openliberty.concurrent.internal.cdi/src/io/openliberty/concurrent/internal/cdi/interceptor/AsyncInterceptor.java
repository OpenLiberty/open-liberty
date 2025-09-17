/*******************************************************************************
 * Copyright (c) 2021,2025 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.cdi.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.concurrent.internal.messages.ConcurrencyNLS;
import jakarta.annotation.Priority;
import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.Schedule;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@Asynchronous
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 5)
public class AsyncInterceptor implements Serializable {
    private static final long serialVersionUID = 7447792334053278336L;

    private static final TraceComponent tc = Tr.register(AsyncInterceptor.class);

    @AroundInvoke
    @FFDCIgnore({ ClassCastException.class, NamingException.class }) // application errors raised directly to the app
    public Object intercept(InvocationContext invocation) throws Exception {
        Method method = invocation.getMethod();
        Asynchronous anno = invocation.getInterceptorBinding(Asynchronous.class);

        // Is it a scheduled asynchronous method?
        Schedule[] schedules = anno == null ? new Schedule[0] : anno.runAt();
        if (schedules.length > 0) {
            // Identify requested inline execution for scheduled executions other than the first,
            CompletableFuture<?> future = ScheduledAsyncMethod.inlineExecutionFuture.get();
            if (future != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "inline subsequent execution of scheduled async method");
                return invoke(invocation, future);
            }
        }

        validateTransactional(invocation);

        if (method.getDeclaringClass().getAnnotation(Asynchronous.class) != null)
            throw new UnsupportedOperationException(ConcurrencyNLS.getMessage("CWWKC1401.class.anno.disallowed",
                                                                              "@Asynchronous",
                                                                              method.getDeclaringClass().getName()));

        // @Asynchronous must be on a method that returns completion stage or void
        Class<?> returnType = method.getReturnType();
        if (!returnType.equals(CompletableFuture.class)
            && !returnType.equals(CompletionStage.class)
            && !returnType.equals(Void.TYPE)) // void
            throw new UnsupportedOperationException(ConcurrencyNLS.getMessage("CWWKC1400.unsupported.return.type",
                                                                              returnType,
                                                                              method.getName(),
                                                                              method.getClass().getName(),
                                                                              "@Asynchronous",
                                                                              "[CompletableFuture, CompletionStage, void]"));

        Object executor;
        try {
            executor = InitialContext.doLookup(anno.executor());
        } catch (NamingException x) {
            throw new RejectedExecutionException(x);
        }

        WSManagedExecutorService managedExecutor;
        try {
            managedExecutor = (WSManagedExecutorService) executor;
        } catch (ClassCastException x) {
            TreeSet<String> interfaces = new TreeSet<String>();
            for (Class<?> c = executor.getClass(); c != null; c = c.getSuperclass())
                for (Class<?> i : executor.getClass().getInterfaces())
                    interfaces.add(i.getName());

            throw new RejectedExecutionException(ConcurrencyNLS.getMessage("CWWKC1402.not.managed.executor",
                                                                           "@Asynchronous",
                                                                           method.getName(),
                                                                           method.getClass().getName(),
                                                                           anno.executor(),
                                                                           executor.getClass().getName(),
                                                                           interfaces), x);
        }

        if (schedules.length == 0) {
            // Immediate one-time asynchronous method

            return managedExecutor.newAsyncMethod(this::invoke, invocation);
        } else {
            // Scheduled asynchronous method - arrange first execution

            List<Long> skipIfLateBySeconds = new ArrayList<Long>();
            List<ScheduleCronTrigger> triggers = new ArrayList<>();
            for (Schedule schedule : schedules) {
                skipIfLateBySeconds.add(schedule.skipIfLateBy());
                triggers.add(ScheduleCronTrigger.create(schedule));
            }

            return new ScheduledAsyncMethod(invocation, this, managedExecutor, triggers, skipIfLateBySeconds).future;
        }
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
    @Trivial
    public <T> CompletionStage<T> invoke(InvocationContext invocation,
                                         CompletableFuture<T> future) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc,
                     "invoke " + invocation.getMethod().getName(),
                     invocation,
                     future);
        Asynchronous.Result.setFuture(future);
        try {
            @SuppressWarnings("unchecked")
            CompletionStage<T> asyncMethodResultStage = //
                            (CompletionStage<T>) invocation.proceed();

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc,
                        "invoke " + invocation.getMethod().getName(),
                        asyncMethodResultStage);
            return asyncMethodResultStage;
        } catch (Throwable x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc,
                        "invoke " + invocation.getMethod().getName(),
                        x);
            throw x instanceof CompletionException //
                            ? (CompletionException) x //
                            : new CompletionException(x);
        } finally {
            Asynchronous.Result.setFuture(null);
        }
    }

    /**
     * When @Asynchronous is paired with @Transactional, limits the TxType to
     * NOT_SUPPORTED or REQUIRES_NEW.
     *
     * @param invocation asynchronous method invocation context.
     * @throws UnsupportedOperationException for unsupported combinations.
     */
    @Trivial
    private static void validateTransactional(InvocationContext invocation) //
                    throws UnsupportedOperationException {
        Transactional tx = invocation.getInterceptorBinding(Transactional.class);
        if (tx != null)
            switch (tx.value()) {
                case NOT_SUPPORTED:
                case REQUIRES_NEW:
                    break;
                default:
                    Method method = invocation.getMethod();
                    throw new UnsupportedOperationException(ConcurrencyNLS //
                                    .getMessage("CWWKC1403.unsupported.tx.type",
                                                "@Transactional",
                                                tx.value(),
                                                "@Asynchronous",
                                                method.getName(),
                                                method.getDeclaringClass().getName(),
                                                Arrays.asList(TxType.REQUIRES_NEW,
                                                              TxType.NOT_SUPPORTED)));
            }
    }
}