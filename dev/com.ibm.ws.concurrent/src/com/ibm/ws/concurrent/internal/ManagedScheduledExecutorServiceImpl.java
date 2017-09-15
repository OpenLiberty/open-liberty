/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.apache.felix.scr.ext.annotation.DSExt;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.ibm.ws.bnd.metatype.annotation.Ext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.resource.ResourceFactory;

@ObjectClassDefinition(factoryPid = "com.ibm.ws.concurrent.managedScheduledExecutorService", name = "%managedScheduledExecutorService",
                       description = "%managedScheduledExecutorService.desc",
                       localization = Ext.LOCALIZATION)
@Ext.Alias("managedScheduledExecutorService")
@Ext.SupportExtensions
interface ManagedScheduledExecutorServiceConfig extends FullManagedExecutorServiceConfig {

}

@Component(configurationPid = "com.ibm.ws.concurrent.managedScheduledExecutorService", configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ExecutorService.class, ManagedExecutorService.class, ResourceFactory.class, ApplicationRecycleComponent.class, ScheduledExecutorService.class,
                       ManagedScheduledExecutorService.class },
           reference = @Reference(name = ManagedExecutorServiceImpl.APP_RECYCLE_SERVICE, service = ApplicationRecycleCoordinator.class) ,
           property = { "creates.objectClass=java.util.concurrent.ExecutorService",
                        "creates.objectClass=java.util.concurrent.ScheduledExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedExecutorService",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedScheduledExecutorService" })
@DSExt.ConfigureWithInterfaces
public class ManagedScheduledExecutorServiceImpl extends ManagedExecutorServiceImpl implements ManagedScheduledExecutorService {
    /**
     * Reference to the (unmanaged) scheduled executor service for this managed scheduled executor service.
     */
    @Reference(target = "(deferrable=false)")
    ScheduledExecutorService scheduledExecSvc;

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

}