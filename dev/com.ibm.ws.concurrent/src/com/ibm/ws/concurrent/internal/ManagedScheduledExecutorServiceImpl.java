/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
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
package com.ibm.ws.concurrent.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.Trigger;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.resource.ResourceFactory;

@Component(configurationPid = "com.ibm.ws.concurrent.managedScheduledExecutorService",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ExecutorService.class,
                       ManagedExecutorService.class,
                       ResourceFactory.class,
                       ApplicationRecycleComponent.class,
                       ScheduledExecutorService.class,
                       ManagedScheduledExecutorService.class },
           reference = @Reference(name = "ApplicationRecycleCoordinator",
                                  service = ApplicationRecycleCoordinator.class))
public class ManagedScheduledExecutorServiceImpl //
                extends ManagedExecutorServiceImpl //
                implements ManagedScheduledExecutorService {

    /**
     * Reference to the (unmanaged) scheduled executor service for this managed scheduled executor service.
     */
    ScheduledExecutorService scheduledExecSvc;

    @Deactivate
    @Override
    @Trivial
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    /**
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        ScheduledTask<V> scheduledTask = new ScheduledTask<V>(this, task, true, delay, null, null, unit);
        trackFuture(scheduledTask.future);
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
        trackFuture(scheduledTask.future);
        return scheduledTask.future;
    }

    /**
     * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        ScheduledTask<Void> scheduledTask = new ScheduledTask<Void>(this, task, false, delay, null, null, unit);
        trackFuture(scheduledTask.future);
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
        trackFuture(scheduledTask.future);
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
        trackFuture(scheduledTask.future);
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
        trackFuture(scheduledTask.future);
        return scheduledTask.future;
    }

    @Reference(target = "(deferrable=false)")
    @Trivial
    protected void setScheduledExecutor(ScheduledExecutorService svc) {
        scheduledExecSvc = svc;
    }

    @Trivial
    protected void unsetScheduledExecutor(ScheduledExecutorService svc) {
        scheduledExecSvc = null;
    }
}