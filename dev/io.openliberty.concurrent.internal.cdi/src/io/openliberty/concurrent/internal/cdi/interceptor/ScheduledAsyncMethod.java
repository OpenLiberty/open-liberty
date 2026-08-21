/*******************************************************************************
 * Copyright (c) 2023,2026 IBM Corporation and others.
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

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;

import io.openliberty.concurrent.internal.cdi.ConcurrencyExtensionMetadata;
import io.openliberty.concurrent.internal.cdi.ScheduleCronTrigger;
import io.openliberty.concurrent.internal.cdi.ScheduledMethodAbstract;
import jakarta.interceptor.InvocationContext;

/**
 * A task that can be scheduled to run a method at the appropriate time,
 * according to the method's Asynchronous(runAt = Schedule) annotation.
 *
 * The first time executed, this task runs the asynchronous method by delegating
 * back to the interceptor (AsyncInterceptor) to run the proceed method on the
 * original invocation, similar to how an asynchronous method would normally run.
 * On all subsequent executions, this task runs the asynchronous method inline
 * by invoking the method on the bean again. In this case, a ThreadLocal is
 * first placed on the thread as a signal to the interceptor (AsyncInterceptor)
 * that this is not a new scheduled asynchronous method, but is instead a
 * subsequent execution that needs to proceed immediately inline.
 * In both cases, InvocationContext.proceed is used, but on different
 * InvocationContext instances, ensuring that each time all of the interceptors
 * run in the correct order. The ThreadLocal also ensures that all executions
 * use the same CompletableFuture instance to represent completion of the
 * application's original request.
 */
class ScheduledAsyncMethod extends ScheduledMethodAbstract {
    private static final TraceComponent tc = Tr.register(ScheduledAsyncMethod.class);

    /**
     * Scheduled executions other than the first are achieved by invoking the
     * asynchronous method on the bean from the scheduled executor thread
     * when the schedule indicates that it is time for the task to run.
     * This causes intercept to be invoked, which uses the presence of this
     * thread local to detect this situation and run the method inline.
     * This approach is used to ensure that all of the interceptors are invoked
     * with each execution of the task.
     */
    public static final ThreadLocal<CompletableFuture<?>> inlineExecutionFuture = //
                    new ThreadLocal<CompletableFuture<?>>();

    private final InvocationContext firstInvocation;
    private final AsyncInterceptor interceptor;
    private boolean isFirstExecution = true;

    /**
     * Constructor for Asynchronous annotation with runAt=Schedule.
     *
     * @param firstInvocation
     * @param interceptor
     * @param managedExecutor
     * @param triggers
     * @param skipIfLateBySeconds
     */
    public ScheduledAsyncMethod(InvocationContext firstInvocation,
                                AsyncInterceptor interceptor,
                                WSManagedExecutorService managedExecutor,
                                List<ScheduleCronTrigger> triggers,
                                List<Long> skipIfLateBySeconds) {
        super(firstInvocation.getMethod(), //
              managedExecutor.captureThreadContext(null), //
              managedExecutor, //
              triggers, //
              skipIfLateBySeconds);

        this.firstInvocation = firstInvocation;
        this.interceptor = interceptor;

        // Intentionally placed as last line of constructuor to ensure
        // intialization is complete before the first task execution runs
        ConcurrencyExtensionMetadata.scheduledExecutor //
                        .schedule(this, computeDelayNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Invokes the bean method that is annotated Asynchronous(runAt = Schedule).
     *
     * @return completion stage that is returned by the bean method, otherwise null
     * @throws InvocationTargetException if the bean method raises a declared
     *                                       exception
     * @throws Exception                 if another error occurs attempting to
     *                                       invoke the bean method
     */
    @Override
    @Trivial
    protected CompletionStage<?> invokeMethod() throws Exception, InvocationTargetException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        CompletionStage<?> result;
        if (isFirstExecution) {
            try {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "proceed with " + method.getName());
                result = interceptor.invoke(firstInvocation, future);
            } finally {
                isFirstExecution = false;
            }
        } else {
            // For subsequent executions, invoke the bean method again,
            // but use a thread local to signal that it should run inline
            // on the scheduled async method thread:
            inlineExecutionFuture.set(future);
            try {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "invoke " + method.getName() + " on bean instance",
                             firstInvocation.getParameters());
                method.setAccessible(true);
                result = (CompletionStage<?>) method //
                                .invoke(firstInvocation.getTarget(),
                                        firstInvocation.getParameters());
            } finally {
                inlineExecutionFuture.remove();
            }
        }
        return result;
    }
}