/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.Trigger;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrency.policy.ConcurrencyPolicy;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(configurationPid = "com.ibm.ws.concurrent.managedScheduledExecutorService", configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ExecutorService.class, ManagedExecutorService.class, ResourceFactory.class, ApplicationRecycleComponent.class, ScheduledExecutorService.class,
                       ManagedScheduledExecutorService.class },
           reference = @Reference(name = ManagedExecutorServiceImpl.APP_RECYCLE_SERVICE, service = ApplicationRecycleCoordinator.class),
           property = { "creates.objectClass=java.util.concurrent.ExecutorService",
                        "creates.objectClass=java.util.concurrent.ScheduledExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedScheduledExecutorService" })
public class ManagedScheduledExecutorServiceImpl extends ManagedExecutorServiceImpl implements ManagedScheduledExecutorService {
    /**
     * Reference to the (unmanaged) scheduled executor service for this managed scheduled executor service.
     */
    @Reference(target = "(deferrable=false)")
    ScheduledExecutorService scheduledExecSvc;

    @Override
    @Modified
    @Trivial
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        super.modified(context, properties);
    }

    /**
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        @SuppressWarnings("unchecked")
        Class<? extends Callable<V>> callableType = task == null ? null : (Class<? extends Callable<V>>) task.getClass();
        ScheduledTask<V> scheduledTask = new ScheduledTask<V>(this, task, callableType, delay, null, null, unit);
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

        @SuppressWarnings("unchecked")
        Class<? extends Callable<V>> callableType = task == null ? null : (Class<? extends Callable<V>>) task.getClass();
        ScheduledTask<V> scheduledTask = new ScheduledTask<V>(this, task, callableType, trigger);
        if (futures.add(scheduledTask.future) && ++futureCount % FUTURE_PURGE_INTERVAL == 0)
            purgeFutures();
        return scheduledTask.future;
    }

    /**
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        ScheduledTask<Void> scheduledTask = new ScheduledTask<Void>(this, task, null, delay, null, null, unit);
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

        ScheduledTask<?> scheduledTask = new ScheduledTask<Void>(this, task, null, trigger);
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

        ScheduledTask<Void> scheduledTask = new ScheduledTask<Void>(this, task, null, initialDelay < 0 ? 0 : initialDelay, null, period, unit);
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

        ScheduledTask<Void> scheduledTask = new ScheduledTask<Void>(this, task, null, initialDelay < 0 ? 0 : initialDelay, delay, null, unit);
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
    @Reference(policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    @Trivial
    protected void setContextService(ServiceReference<WSContextService> ref) {
        super.setContextService(ref);
    }

    @Override
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(id=unbound)")
    @Trivial
    protected void setLongRunningPolicy(ConcurrencyPolicy svc) {
        super.setLongRunningPolicy(svc);
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
    protected void unsetContextService(ServiceReference<WSContextService> ref) {
        super.unsetContextService(ref);
    }

    @Override
    @Trivial
    protected void unsetLongRunningPolicy(ConcurrencyPolicy svc) {
        super.unsetLongRunningPolicy(svc);
    }

    @Override
    @Trivial
    protected void unsetTransactionContextProvider(ServiceReference<ThreadContextProvider> ref) {
        super.unsetTransactionContextProvider(ref);
    }
}