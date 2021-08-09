/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.admin.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * This keeps a track of all ManagedServices.
 */
class ManagedServiceTracker extends ServiceTracker<ManagedService, ManagedService> {
    private static final String ME = ManagedServiceTracker.class.getName();
    private static final TraceComponent tc = Tr.register(ManagedServiceTracker.class, ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    /** Reference to OSGi bundle context. */
    private final ConfigAdminServiceFactory caFactory;

    private final Map<String, ManagedService> managedServices = new HashMap<String, ManagedService>();
    private final Map<String, ServiceReference<ManagedService>> managedServiceReferences = new HashMap<String, ServiceReference<ManagedService>>();

    /**
     * 
     * @param bc
     */
    public ManagedServiceTracker(ConfigAdminServiceFactory casf, BundleContext bc) {
        super(bc, ManagedService.class.getName(), null);
        this.caFactory = casf;
    }

    protected Future<?> notifyDeleted(ExtendedConfigurationImpl config) {
        config.checkLocked();
        String pid = config.getPid(false);
        ServiceReference<ManagedService> reference = getManagedServiceReference(pid);
        if (reference != null && config.bind(reference.getBundle()))
            return asyncUpdated(getManagedService(pid), pid, null);

        return null;
    }

    protected Future<?> notifyUpdated(ExtendedConfigurationImpl config) {
        config.checkLocked();
        String pid = config.getPid();
        ServiceReference<ManagedService> reference = getManagedServiceReference(pid);
        if (reference != null && config.bind(reference.getBundle())) {
            ManagedService ms = getManagedService(pid);
            // must make a copy
            Dictionary<String, Object> properties = config.getProperties();
            caFactory.modifyConfiguration(reference, properties, ms);
            return asyncUpdated(ms, pid, properties);
        }

        return null;
    }

    /**
     * Processes registered ManagedService and updates each with their own
     * configuration properties.
     * 
     * @param reference
     */
    @Override
    public ManagedService addingService(ServiceReference<ManagedService> reference) {
        String[] pids = getServicePid(reference);
        if (pids == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "handleRegistration(): Invalid service.pid type: " + reference);
            }
            return null;
        }

        ManagedService ms = context.getService(reference);
        if (ms == null)
            return null;

        synchronized (caFactory.getConfigurationStore()) {
            for (String pid : pids) {
                add(reference, pid, ms);
            }
        }

        return ms;
    }

    @Override
    public void modifiedService(ServiceReference<ManagedService> reference, ManagedService service) {
        String[] pids = getServicePid(reference);
        List<String> newPids = Collections.emptyList();
        if (pids != null) {
            newPids = Arrays.asList(pids);
        }

        synchronized (caFactory.getConfigurationStore()) {

            List<String> previousPids = getPidsForManagedService(service);

            HashSet<String> prevSet = new HashSet<String>(previousPids);
            HashSet<String> newSet = new HashSet<String>(newPids);

            if (!prevSet.equals(newSet)) {
                // remove those that are not gone
                for (String pid : previousPids) {
                    if (!newSet.contains(pid)) {
                        remove(reference, pid);
                    }
                }

                // add those that are new
                for (String pid : newPids) {
                    if (!prevSet.contains(pid)) {
                        add(reference, pid, service);
                    }
                }
            }
        }
    }

    /**
     * MangedService service removed. Process removal and unget service from its
     * context.
     * 
     * @param reference
     * @param service
     */
    @Override
    public void removedService(ServiceReference<ManagedService> reference, ManagedService service) {

        String[] pids = getServicePid(reference);
        if (pids == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "removedService(): Invalid service.pid type: " + reference);
            }

            return;
        }

        synchronized (caFactory.getConfigurationStore()) {
            for (String pid : pids) {
                remove(reference, pid);
            }
        }

        context.ungetService(reference);
    }

    private static String[] getServicePid(ServiceReference<ManagedService> reference) {
        Object pidObj = reference.getProperty(Constants.SERVICE_PID);
        if (pidObj instanceof String) {
            return new String[] { (String) pidObj };
        } else if (pidObj instanceof String[]) {
            return (String[]) pidObj;
        } else if (pidObj instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<String> pidCollection = (Collection<String>) pidObj;
            return pidCollection.toArray(new String[pidCollection.size()]);
        }

        return null;
    }

    private void add(ServiceReference<ManagedService> reference, String pid, ManagedService service) {
        ExtendedConfigurationImpl config = caFactory.getConfigurationStore().findConfiguration(pid);
        if (config == null) {
            if (trackManagedService(pid, reference, service)) {
                asyncUpdated(service, pid, null);
            }
        } else {
            config.lock();
            try {
                if (trackManagedService(pid, reference, service)) {
                    if (config.getFactoryPid(false) != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Configuration for " + Constants.SERVICE_PID + "=" + pid + " should only be used by a " + ManagedServiceFactory.class.getName(), config);
                    } else if (config.isDeleted()) {
                        asyncUpdated(service, pid, null);
                    } else if (config.bind(reference.getBundle())) {
                        Dictionary<String, Object> properties = config.getProperties();
                        caFactory.modifyConfiguration(reference, properties, service);
                        asyncUpdated(service, pid, properties);
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Configuration for " + Constants.SERVICE_PID + "=" + pid + " could not be bound to " + reference.getBundle().getLocation());
                    }
                }
            } finally {
                config.unlock();
            }
        }
    }

    private void remove(ServiceReference<ManagedService> reference, String pid) {
        ExtendedConfigurationImpl config = caFactory.getConfigurationStore().findConfiguration(pid);
        if (config == null) {
            untrackManagedService(pid, reference);
        } else {
            config.lock();
            try {
                untrackManagedService(pid, reference);
            } finally {
                config.unlock();
            }
        }
    }

    private boolean trackManagedService(String pid, ServiceReference<ManagedService> reference, ManagedService service) {
        synchronized (managedServiceReferences) {
            if (managedServiceReferences.containsKey(pid)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, ManagedService.class.getName() + " already registered for " + Constants.SERVICE_PID + "=" + pid);
                return false;
            }
            managedServiceReferences.put(pid, reference);
            managedServices.put(pid, service);
            return true;
        }
    }

    private void untrackManagedService(String pid, ServiceReference<ManagedService> reference) {
        synchronized (managedServiceReferences) {
            managedServiceReferences.remove(pid);
            managedServices.remove(pid);
        }
    }

    private ManagedService getManagedService(String pid) {
        synchronized (managedServiceReferences) {
            return managedServices.get(pid);
        }
    }

    private ServiceReference<ManagedService> getManagedServiceReference(String pid) {
        synchronized (managedServiceReferences) {
            return managedServiceReferences.get(pid);
        }
    }

    private Future<?> asyncUpdated(final ManagedService service, final String pid, final Dictionary<String, ?> properties) {
        return caFactory.updateQueue.add(pid, new Runnable() {
            @Override
            public void run() {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "asyncUpdated: service.updated called for " + pid);
                }
                try {
                    service.updated(properties);
                } catch (Throwable t) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "asyncUpdated(): Exception thrown while trying to update ManagedService.", t);
                    }
                    FFDCFilter.processException(t, ME, "asyncUpdated()",
                                                new Object[] { pid, service });

                }
            }
        });
    }

    private List<String> getPidsForManagedService(ManagedService service) {
        ArrayList<String> pids = new ArrayList<String>();
        synchronized (managedServiceReferences) {
            for (Map.Entry<String, ManagedService> entry : managedServices.entrySet()) {
                if (entry.getValue() == service)
                    pids.add(entry.getKey());
            }
        }

        return pids;
    }
}
