/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.filemonitor.internal;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.filemonitor.FileNotification;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 * Core of file monitor service. Looks for registered FileMonitors (via declarative services).
 *
 * The two required references (WsLocationAdmin and ScheduledExecutorService) are dynamic, to allow
 * those services to be refreshed/replaced without recycling the component.
 */
public abstract class CoreServiceImpl implements CoreService, FileNotification, ServerQuiesceListener {

    /**  */
    static final String MONITOR = "Monitor";

    static final TraceComponent tc = Tr.register(CoreServiceImpl.class);

    /** Injected location service */
    private final AtomicReference<WsLocationAdmin> locServiceRef = new AtomicReference<WsLocationAdmin>(null);

    /** Injected executor service */
    private final AtomicReference<ScheduledExecutorService> executorService = new AtomicReference<ScheduledExecutorService>(null);

    /** Concurrent map for (optional,multiple,dynamic) FileMonitor reference to allocated MonitorHolder */
    private final ConcurrentHashMap<ServiceReference<FileMonitor>, MonitorHolder> fileMonitors = new ConcurrentHashMap<ServiceReference<FileMonitor>, MonitorHolder>();

    /** BundleContext: used to retrieve services from the corresponding service reference: valid for life of the DS component */
    private volatile ComponentContext cContext = null;

    /** If true: issue (VERY) detailed/frequent trace about start/stop of every scan */
    private volatile boolean detailedScanTrace = false;

    /**
     * DS-driven component activation
     */
    @Activate
    protected void activate(ComponentContext cContext, Map<String, Object> properties) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "File monitor service activated", properties);
        }
        this.cContext = cContext;
        modified(properties);

        // Make sure all file monitors have been initialized.
        for (Map.Entry<ServiceReference<FileMonitor>, MonitorHolder> entry : fileMonitors.entrySet()) {
            entry.getValue().init();
        }
    }

    /**
     * DS-driven de-activation
     */
    @Deactivate
    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "File monitor service deactivated", "reason=" + reason, fileMonitors);
        }

        for (Map.Entry<ServiceReference<FileMonitor>, MonitorHolder> entry : fileMonitors.entrySet()) {
            entry.getValue().destroy();
        }
        fileMonitors.clear();
        this.cContext = null;
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        final String key = "detailedTraceEnabled";

        if (properties != null) {
            try {
                detailedScanTrace = MetatypeUtils.parseBoolean(properties.get(Constants.SERVICE_PID), key, properties.get(key), false);
            } catch (IllegalArgumentException e) {
                // FFDC will be cut, and parseBoolean will log the warning
                detailedScanTrace = false;
            }
        }
    }

    /**
     * Inject a ServiceReference for the required/dynamic ScheduledExecutorService.
     * If the service is different than the previous value, we need to queue
     * established monitors to cancel their tasks with the old executor service,
     * and reschedule them with the new one.
     *
     * @param schedulerRef ServiceReference for the required/dynamic ScheduledExcecutorService
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setScheduler(ScheduledExecutorService scheduler) {

        ScheduledExecutorService oldService = executorService.getAndSet(scheduler);

        if (oldService != null && fileMonitors != null) {
            // Monitors must reschedule their tasks with the new service
            for (MonitorHolder mh : fileMonitors.values()) {
                mh.reschedule();
            }
        }
    }

    /**
     * Remove the reference to the required/dynamic ScheduledExecutorService service.
     * Take care with removal: This is a dynamic reference, meaning that
     * should the instance of the bound location service go away _and_ there
     * is a replacement service already available/registered, DS will
     * bind to the replacement first (via setLocation) before calling
     * unset to unbind the old one.
     *
     * @param scheduler
     *            the service to remove.
     */
    protected void unsetScheduler(ScheduledExecutorService schedulerRef) {
        executorService.compareAndSet(schedulerRef, null);
    }

    /**
     * Retrieve the scheduler
     *
     * @return bound ScheduledExecutorService
     * @throws IllegalStateException if the ScheduledExecutorService is not found
     */
    @Override
    public ScheduledExecutorService getScheduler() {
        ScheduledExecutorService sRef = executorService.get();
        if (sRef == null) {
            throw new IllegalStateException("ScheduledExecutorService service is unavailable");
        }
        return sRef;
    }

    /**
     * Inject a ServiceReference for the required/dynamic WsLocationAdmin service.
     */
    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setLocation(WsLocationAdmin locRef) {
        locServiceRef.set(locRef);
    }

    /**
     * Remove the reference to the required/dynamic WsLocationAdmin service.
     */
    protected void unsetLocation(WsLocationAdmin locRef) {
        locServiceRef.compareAndSet(locRef, null);
    }

    /**
     * Retrieve the location service using the component context.
     * The location service is a required service (the component will
     * not be activated without it). The component context caches the
     * returned service (subsequent calls to locate are cheap).
     */
    @Override
    public WsLocationAdmin getLocationService() {
        WsLocationAdmin sRef = locServiceRef.get();
        if (sRef == null) {
            throw new IllegalStateException("WsLocationAdmin service is unavailable");
        }
        return sRef;
    }

    /**
     * Inject an instance of a <code>FileMonitor</code> service (provider of
     * that interface). Called whenever an instance of this service is available:
     * is dynamic, optional, and multiple.
     *
     * ==> synchronized against unsetMonitor
     *
     * @param monitorRef
     *            a reference to the FileMonitor instance to update
     */
    @Reference(service = FileMonitor.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, target = "(|(monitor.files=*)(monitor.directories=*))")
    protected void setMonitor(ServiceReference<FileMonitor> monitorRef) {

        // This is a "simple"/safe constructor that does not reference our required services (locate/executor).
        // Monitors can be registered/bound before the other services are (no guaranteed order for optional dynamic
        // services)
        MonitorHolder holder = createMonitorHolder(monitorRef);
        MonitorHolder existing = fileMonitors.put(monitorRef, holder);

        // Clean up existing/previous entry, if present
        if (existing != null) {
            // destroy is synchronized
            existing.destroy();
        }

        // Only initialize and start the monitor if the component has been activated
        if (cContext != null) {
            try {
                holder.init();
            } catch (RuntimeException re) {
                // Generated FFDC, rethrow to fail the bind
                fileMonitors.remove(monitorRef);
                throw re;
            }
        }

    }

    /**
     * Create a monitor holder for the given FileMonitor. The type of holder we create will
     * depend
     *
     * @param monitorRef
     * @return
     */
    protected abstract MonitorHolder createMonitorHolder(ServiceReference<FileMonitor> monitorRef);

    /**
     * Update an instance of a <code>FileMonitor</code> service (provider of
     * that interface).
     *
     *
     * @param monitorRef
     *            a reference to the FileMonitor instance to update
     */
    protected void updatedMonitor(ServiceReference<FileMonitor> monitorRef) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "CoreServiceImpl updatedMonitor", monitorRef);
        MonitorHolder existing = fileMonitors.get(monitorRef);
        if (existing == null)
            return;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "CoreServiceImpl updatedMonitor, existing found");
        // Only initialize and start the monitor if the component has been activated
        if (cContext != null) {
            try {
                // Create a root cache location for the bundle that registered this service;
                // there could be multiple monitors associated with a bundle, but all should be destroyed
                // if/when the bundle is uninstalled, so keep them "together"..
                //TODO n.b. cacheRoot is unused
                File cacheRoot = cContext.getBundleContext().getDataFile(Long.toString(monitorRef.getBundle().getBundleId()));
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "CoreServiceImpl updatedMonitor, about to refresh");
                existing.refresh(cacheRoot);
            } catch (RuntimeException re) {
                // Generated FFDC, rethrow to fail the bind
                fileMonitors.remove(monitorRef);
                throw re;
            }
        }
    }

    /**
     * Remove an instance of a <code>FileMonitor</code> service (provider of
     * that interface). Called whenever an instance of this service is removed
     * from the service registry.
     *
     * @param monitorRef
     *            a reference to the FileMonitor instance to remove
     */
    protected void unsetMonitor(ServiceReference<FileMonitor> monitorRef) {
        MonitorHolder holder = fileMonitors.remove(monitorRef);

        if (holder != null) {
            holder.destroy();
        }
    }

    @Override
    public FileMonitor getReferencedMonitor(ServiceReference<FileMonitor> monitorRef) {
        // This is done once per monitor: cached by the caller (MonitorHolder.init)
        return cContext.locateService(MONITOR, monitorRef);
    }

    @Override
    @Trivial
    public boolean isDetailedScanTraceEnabled() {
        return detailedScanTrace;
    }

    protected boolean frameworkIsStopping() {
        try {
            Bundle systemBundle = cContext.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            return systemBundle.getState() == Bundle.STOPPING;
        } catch (IllegalStateException e) {
            return true;
        }
    }

    @Override
    public void notifyFileChanges(Collection<String> created, Collection<String> modified, Collection<String> deleted) {
        // This method must convert the collections of paths (in string form) to sets (in file form).
        // It should use the absolute path rather than the canonical path to avoid referencing
        // symbolic links - which could convert a valid path under the installation directory into a
        // path that is no longer under the wlp directory - which would cause problems later.
        Set<File> absoluteCreated = PathUtils.getFixedPathFiles(created);
        Set<File> absoluteDeleted = PathUtils.getFixedPathFiles(deleted);
        Set<File> absoluteModified = PathUtils.getFixedPathFiles(modified);
        for (MonitorHolder mh : fileMonitors.values())
            mh.externalScan(absoluteCreated, absoluteDeleted, absoluteModified, true, null);
    }

    /**
     * Processes pending server configuration file events (additions/modifications/removals).
     */
    @Override
    public void processConfigurationChanges() {
        // The monitor ID is used to determine who is called to process file events.
        for (ServiceReference<FileMonitor> fm : fileMonitors.keySet()) {
            String monitorId = (String) fm.getProperty(com.ibm.ws.kernel.filemonitor.FileMonitor.MONITOR_IDENTIFICATION_NAME);
            if (monitorId != null && monitorId.equals("com.ibm.ws.kernel.monitor.config")) {
                fileMonitors.get(fm).processFileRefresh(false, null);
            }
        }
    }

    /**
     * Processes pending application artifact events (modifications).
     */
    @Override
    public void processApplicationChanges() {
        // Application update events are tracked by the artifact framework.
        // The registered artifact monitor ID and the ID of the artifact listener registered by the
        // application component are used to determine who is called to process file events.
        for (ServiceReference<FileMonitor> fm : fileMonitors.keySet()) {
            String monitorId = (String) fm.getProperty(com.ibm.ws.kernel.filemonitor.FileMonitor.MONITOR_IDENTIFICATION_NAME);
            if (monitorId != null && monitorId.equals("com.ibm.ws.kernel.monitor.artifact")) {
                fileMonitors.get(fm).processFileRefresh(false, "com.ibm.ws.app.listener");
            }
        }
    }

    @Override
    public void serverStopping() {

        for (MonitorHolder mh : fileMonitors.values()) {
            mh.serverStopping();
        }

    }

}