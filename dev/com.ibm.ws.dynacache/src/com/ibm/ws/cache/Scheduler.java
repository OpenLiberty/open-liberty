/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

@Component(service = Scheduler.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class Scheduler {

    private static TraceComponent tc = Tr.register(Scheduler.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    private static Scheduler instance;

    private static void setInstance(Scheduler value) {
        instance = value;
    }

    /** Injected Scheduled executor service Replacement for AlarmManager */
    private final AtomicReference<ScheduledExecutorService> scheduledExecutorService = new AtomicReference<ScheduledExecutorService>(null);

    /** To schedule work "immediately */
    private final AtomicReference<ExecutorService> executorService = new AtomicReference<ExecutorService>(null);

    private final AtomicReference<WsLocationAdmin> locationAdminRef = new AtomicReference<WsLocationAdmin>(null);

    /**
     * Inject a ServiceReference for the required/dynamic ScheduledExecutorService. If the service is different than the
     * previous value, we need to queue established monitors to cancel their tasks with the old executor service, and
     * reschedule them with the new one.
     * 
     * @param schedulerRef
     *            ServiceReference for the required/dynamic ScheduledExcecutorService
     */

    @Reference(service = ScheduledExecutorService.class, target = "(deferrable=false)")
    protected void setScheduledExecutorService(ScheduledExecutorService scheduler) {
        scheduledExecutorService.getAndSet(scheduler);
    }

    @Reference(service = ExecutorService.class, target = "(service.vendor=IBM)")
    protected void setExecutorService(ExecutorService es) {
        executorService.getAndSet(es);
    }

    /**
     * Remove the reference to the required/dynamic ScheduledExecutorService service. Take care with removal: This is a
     * dynamic reference, meaning that should the instance of the bound location service go away _and_ there is a
     * replacement service already available/registered, DS will bind to the replacement first (via setLocation) before
     * calling unset to unbind the old one.
     * 
     * @param scheduler
     *            the service to remove.
     */
    protected void unsetScheduledExecutorService(ScheduledExecutorService schedulerRef) {
        scheduledExecutorService.compareAndSet(schedulerRef, null);
    }

    protected void unsetExecutorService(ExecutorService esRef) {
        executorService.compareAndSet(esRef, null);
    }

    /**
     * Retrieve the scheduler
     * 
     * @return bound ScheduledExecutorService
     * @throws IllegalStateException
     *             if the ScheduledExecutorService is not found
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        ScheduledExecutorService sRef = scheduledExecutorService.get();
        if (sRef == null) {
            throw new IllegalStateException("ScheduledExecutorService service is unavailable");
        }
        return sRef;
    }

    public ExecutorService getExecutorService() {
        ExecutorService eRef = executorService.get();
        if (eRef == null) {
            throw new IllegalStateException("ExecutorService service is unavailable");
        }
        return eRef;
    }

    public static void createNonDeferrable(long sleepInterval, final Object context, final Runnable run) {
        if (null != instance) {
            instance.getScheduledExecutorService().schedule(new WrappedRunnable(run), sleepInterval, TimeUnit.MILLISECONDS);
        }
    }

    public static Future<?> submit(Runnable task) {
        if (null != instance) {
            return instance.getExecutorService().submit(new WrappedRunnable(task));
        }
        
        return null;
    }

    static class WrappedRunnable implements Runnable {
        Runnable _r = null;

        WrappedRunnable(Runnable r) {
            _r = r;
        }

        @Override
        public void run() {
            try {
                _r.run();
            } catch (Exception ex) {
                FFDCFilter.processException(ex, "com.ibm.ws.cache.Scheduler.createNonDeferrable(long, Object, Runnable)", "77", this);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "exception during wakeUp", ex.getCause());
            }
        }
    }

    public void activate() {
        setInstance(this);
    }

    public void deactivate() {
        setInstance(null);
    }

    public static WsLocationAdmin getLocationAdmin() {
        return instance.locationAdminRef.get();
    }

    @Reference(service = WsLocationAdmin.class)
    protected void setLocationAdmin(WsLocationAdmin vr) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setWsLocationAdmin ", vr);
        }
        locationAdminRef.set(vr);
    }

    protected void unsetLocationAdmin(WsLocationAdmin vr) {
        locationAdminRef.compareAndSet(vr, null);
    }

}
