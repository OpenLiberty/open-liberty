/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.ext;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.Trigger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * Extend this interface to intercept and replace resource reference lookups for
 * <code>managedScheduledExecutorService</code>.
 *
 * At most one extension implementation can be supplied across the entire system.
 * A feature that provides this extension point makes itself incompatible
 * with every other feature that also provides this extension point.
 * Do not implement if this restriction is unacceptable.
 */
@Trivial
public class ManagedScheduledExecutorExtension extends ManagedExecutorExtension implements ManagedScheduledExecutorService {
    private final ManagedScheduledExecutorService scheduledExecutor;

    protected ManagedScheduledExecutorExtension(WSManagedExecutorService executor, ResourceInfo resourceInfo) {
        super(executor, resourceInfo);
        this.scheduledExecutor = (ManagedScheduledExecutorService) executor;
    }

    @Override
    public final <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(callable, delay, unit);
    }

    @Override
    public final <V> ScheduledFuture<V> schedule(Callable<V> callable, Trigger trigger) {
        return scheduledExecutor.schedule(callable, trigger);
    }

    @Override
    public final ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(command, delay, unit);
    }

    @Override
    public final ScheduledFuture<?> schedule(Runnable command, Trigger trigger) {
        return scheduledExecutor.schedule(command, trigger);
    }

    @Override
    public final ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public final ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduledExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
}
