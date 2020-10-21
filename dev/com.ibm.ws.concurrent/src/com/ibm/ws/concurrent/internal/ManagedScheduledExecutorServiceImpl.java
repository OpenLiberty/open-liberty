/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.Trigger;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrency.policy.ConcurrencyPolicy;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(configurationPid = "com.ibm.ws.concurrent.managedScheduledExecutorService", configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ExecutorService.class, ManagedExecutorService.class, //
                       ResourceFactory.class, ApplicationRecycleComponent.class, //
                       ScheduledExecutorService.class, ManagedScheduledExecutorService.class },
           reference = @Reference(name = "ApplicationRecycleCoordinator", service = ApplicationRecycleCoordinator.class),
           property = { "creates.objectClass=java.util.concurrent.ExecutorService",
                        "creates.objectClass=java.util.concurrent.ScheduledExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedScheduledExecutorService" })
public class ManagedScheduledExecutorServiceImpl extends ManagedExecutorServiceImpl implements ManagedScheduledExecutorService {

    private static final TraceComponent tc = Tr.register(ManagedScheduledExecutorServiceImpl.class);

    /**
     * Controls how often we purge the list of tracked futures.
     */
    private static final int FUTURE_PURGE_INTERVAL = 20;

    /**
     * Count of futures. In combination with FUTURE_PURGE_INTERVAL, this determines when to purge completed futures from the list.
     */
    private volatile int futureCount;

    /**
     * Futures for tasks that might be scheduled or running. These are tracked so that we can meet the requirement
     * of canceling or interrupting tasks that are scheduled or running when the managed executor service goes away.
     * Futures from execute/submit/invokeAll/invokeAny methods are not tracked in this structure.
     */
    private final ConcurrentLinkedQueue<ScheduledFuture<?>> futures = new ConcurrentLinkedQueue<ScheduledFuture<?>>();

    /**
     * Reference to the (unmanaged) scheduled executor service for this managed scheduled executor service.
     */
    ScheduledExecutorService scheduledExecSvc;

    @Activate
    @Override
    @Trivial
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        super.activate(context, properties);
    }

    @Deactivate
    @Override
    protected void deactivate(ComponentContext context) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // Cancel scheduled or running tasks
        for (Future<?> future = futures.poll(); future != null; future = futures.poll())
            if (!future.isDone() && future.cancel(true))
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "canceled scheduled task", future);

        super.deactivate(context);
    }

    @Modified
    @Override
    @Trivial
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        super.modified(context, properties);
    }

    /**
     * Purge completed futures from the list we are tracking.
     * This method should be invoked every so often so that we don't leak memory.
     */
    @Trivial
    private final void purgeFutures() {
        for (Iterator<ScheduledFuture<?>> it = futures.iterator(); it.hasNext();)
            if (it.next().isDone())
                it.remove();
    }

    /**
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        ScheduledTask<V> scheduledTask = new ScheduledTask<V>(this, task, true, delay, null, null, unit);
        if (futures.add(scheduledTask.future) && ++futureCount % FUTURE_PURGE_INTERVAL == 0)
            purgeFutures();
        return scheduledTask.future;
    }

    /**
     * @see javax.enterprise.concurrent.ManagedScheduledExecutorService#schedule(java.util.concurrent.Callable, javax.enterprise.concurrent.Trigger)
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> task, Trigger trigger) {
        if (trigger == null)
            throw new NullPointerException(Trigger.class.getName());

        ScheduledTask<V> scheduledTask = new ScheduledTask<V>(this, task, true, trigger);
        if (futures.add(scheduledTask.future) && ++futureCount % FUTURE_PURGE_INTERVAL == 0)
            purgeFutures();
        return scheduledTask.future;
    }

    /**
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        ScheduledTask<Void> scheduledTask = new ScheduledTask<Void>(this, task, false, delay, null, null, unit);
        if (futures.add(scheduledTask.future) && ++futureCount % FUTURE_PURGE_INTERVAL == 0)
            purgeFutures();
        return scheduledTask.future;
    }

    /**
     * @see javax.enterprise.concurrent.ManagedScheduledExecutorService#schedule(java.lang.Runnable, javax.enterprise.concurrent.Trigger)
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        if (trigger == null)
            throw new NullPointerException(Trigger.class.getName());

        ScheduledTask<?> scheduledTask = new ScheduledTask<Void>(this, task, false, trigger);
        if (futures.add(scheduledTask.future) && ++futureCount % FUTURE_PURGE_INTERVAL == 0)
            purgeFutures();
        return scheduledTask.future;
    }

    /**
     * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (period <= 0)
            throw new IllegalArgumentException(Long.toString(period));

        ScheduledTask<Void> scheduledTask = new ScheduledTask<Void>(this, task, false, initialDelay < 0 ? 0 : initialDelay, null, period, unit);
        if (futures.add(scheduledTask.future) && ++futureCount % FUTURE_PURGE_INTERVAL == 0)
            purgeFutures();
        return scheduledTask.future;
    }

    /**
     * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        if (delay <= 0)
            throw new IllegalArgumentException(Long.toString(delay));

        ScheduledTask<Void> scheduledTask = new ScheduledTask<Void>(this, task, false, initialDelay < 0 ? 0 : initialDelay, delay, null, unit);
        if (futures.add(scheduledTask.future) && ++futureCount % FUTURE_PURGE_INTERVAL == 0)
            purgeFutures();
        return scheduledTask.future;
    }

    @Override
    @Reference(policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    @Trivial
    protected void setConcurrencyPolicy(ConcurrencyPolicy svc) {
        super.setConcurrencyPolicy(svc);
    }

    @Override
    @Reference(policy = ReferencePolicy.STATIC)
    @Trivial
    protected void setConcurrencyService(ConcurrencyService svc) {
        super.setConcurrencyService(svc);
    }

    @Override
    @Reference(policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    @Trivial
    protected void setContextService(ServiceReference<WSContextService> ref) {
        super.setContextService(ref);
    }

    @Override
    @Reference(service = JavaEEVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        super.setEEVersion(ref);
    }

    @Override
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(id=unbound)")
    @Trivial
    protected void setLongRunningPolicy(ConcurrencyPolicy svc) {
        super.setLongRunningPolicy(svc);
    }

    @Reference(target = "(deferrable=false)")
    @Trivial
    protected void setScheduledExecutor(ScheduledExecutorService svc) {
        scheduledExecSvc = svc;
    }

    @Override
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(component.name=com.ibm.ws.transaction.context.provider)")
    @Trivial
    protected void setTransactionContextProvider(ServiceReference<ThreadContextProvider> ref) {
        super.setTransactionContextProvider(ref);
    }

    @Override
    @Trivial
    protected void unsetConcurrencyPolicy(ConcurrencyPolicy svc) {
        super.unsetConcurrencyPolicy(svc);
    }

    @Override
    @Trivial
    protected void unsetConcurrencyService(ConcurrencyService svc) {
        super.unsetConcurrencyService(svc);
    }

    @Override
    @Trivial
    protected void unsetContextService(ServiceReference<WSContextService> ref) {
        super.unsetContextService(ref);
    }

    @Override
    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        super.unsetEEVersion(ref);
    }

    @Override
    @Trivial
    protected void unsetLongRunningPolicy(ConcurrencyPolicy svc) {
        super.unsetLongRunningPolicy(svc);
    }

    @Trivial
    protected void unsetScheduledExecutor(ScheduledExecutorService svc) {
        scheduledExecSvc = null;
    }

    @Override
    @Trivial
    protected void unsetTransactionContextProvider(ServiceReference<ThreadContextProvider> ref) {
        super.unsetTransactionContextProvider(ref);
    }
}