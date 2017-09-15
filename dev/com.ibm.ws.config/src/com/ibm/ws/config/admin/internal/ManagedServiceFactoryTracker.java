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
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * This keeps a track of all registered ManagedServiceFactory.
 */
class ManagedServiceFactoryTracker extends ServiceTracker<ManagedServiceFactory, ManagedServiceFactory> {
    private static final String ME = ManagedServiceFactoryTracker.class.getName();
    private static final TraceComponent tc = Tr.register(ManagedServiceFactoryTracker.class, ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    /** ConfigurationAdmin service factory */
    private final ConfigAdminServiceFactory caFactory;

    private final Map<String, ManagedServiceFactory> managedServiceFactories = new HashMap<String, ManagedServiceFactory>();
    private final Map<String, ServiceReference<ManagedServiceFactory>> managedServiceFactoryReferences = new HashMap<String, ServiceReference<ManagedServiceFactory>>();

    /**
     * Constructor for ManagedServiceFactoryTracker.
     * 
     * @param bc
     *            - bundle context
     */
    public ManagedServiceFactoryTracker(ConfigAdminServiceFactory casf, BundleContext bc) {
        super(bc, ManagedServiceFactory.class.getName(), null);
        caFactory = casf;
    }

    /**
     * Processes registered ManagedServiceFactory and updates each with their own
     * configuration properties.
     * 
     * @param reference
     *            - ServiceReference for MangedServiceFactory
     */
    @Override
    public ManagedServiceFactory addingService(ServiceReference<ManagedServiceFactory> reference) {
        String[] factoryPids = getServicePid(reference);
        if (factoryPids == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "handleRegistration(): Invalid service.pid type: " + reference);
            }

            return null;
        }

        ManagedServiceFactory msf = context.getService(reference);
        if (msf == null)
            return null;

        synchronized (caFactory.getConfigurationStore()) {

            for (String factoryPid : factoryPids) {
                add(reference, factoryPid, msf);
            }
        }

        return msf;
    }

    @Override
    public void modifiedService(ServiceReference<ManagedServiceFactory> reference, ManagedServiceFactory service) {
        String[] pids = getServicePid(reference);
        List<String> newPids = Collections.emptyList();
        if (pids != null) {
            newPids = Arrays.asList(pids);
        }

        synchronized (caFactory.getConfigurationStore()) {

            List<String> previousPids = getPidsForManagedServiceFactory(service);

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
     * MangedServiceFactory service removed. Process removal and unget service
     * from its context.
     * 
     * @param reference
     * @param service
     */
    @Override
    public void removedService(ServiceReference<ManagedServiceFactory> reference, ManagedServiceFactory service) {
        String[] factoryPids = getServicePid(reference);

        if (factoryPids == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "removedService(): Invalid service.pid type: " + reference);
            }
            return;
        }

        synchronized (caFactory.getConfigurationStore()) {
            for (String pid : factoryPids) {
                remove(reference, pid);
            }
        }

        context.ungetService(reference);
    }

    private static String[] getServicePid(ServiceReference<ManagedServiceFactory> reference) {
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

    private void add(ServiceReference<ManagedServiceFactory> reference, String factoryPid, ManagedServiceFactory service) {
        ExtendedConfigurationImpl[] configs = caFactory.getConfigurationStore().getFactoryConfigurations(factoryPid);
        try {
            for (int i = 0; i < configs.length; ++i)
                configs[i].lock();

            if (trackManagedServiceFactory(factoryPid, reference, service)) {
                for (int i = 0; i < configs.length; ++i) {
                    if (configs[i].isDeleted()) {
                        // ignore this config
                    } else if (configs[i].bind(reference.getBundle())) {
                        Dictionary<String, Object> properties = configs[i].getProperties();
                        caFactory.modifyConfiguration(reference, properties, service);
                        asyncUpdated(service, configs[i].getFactoryPid(), configs[i].getPid(), properties);
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "add(): Configuration for " + Constants.SERVICE_PID + "=" + configs[i].getPid() + " could not be bound to "
                                         + reference.getBundle().getLocation());
                        }
                    }
                }
            }
        } finally {
            for (int i = 0; i < configs.length; ++i)
                configs[i].unlock();
        }
    }

    private void remove(ServiceReference<ManagedServiceFactory> reference, String factoryPid) {
        ExtendedConfigurationImpl[] configs = caFactory.getConfigurationStore().getFactoryConfigurations(factoryPid);
        try {
            for (int i = 0; i < configs.length; ++i)
                configs[i].lock();
            untrackManagedServiceFactory(factoryPid, reference);
        } finally {
            for (int i = 0; i < configs.length; ++i)
                configs[i].unlock();
        }
    }

    private boolean trackManagedServiceFactory(String factoryPid, ServiceReference<ManagedServiceFactory> reference, ManagedServiceFactory service) {
        synchronized (managedServiceFactoryReferences) {
            if (managedServiceFactoryReferences.containsKey(factoryPid)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "trackManagedServiceFactory(): " + ManagedServiceFactory.class.getName() + " already registered for " + Constants.SERVICE_PID + "=" + factoryPid);
                }
                return false;
            }
            managedServiceFactoryReferences.put(factoryPid, reference);
            managedServiceFactories.put(factoryPid, service);
            return true;
        }
    }

    private void untrackManagedServiceFactory(String factoryPid, ServiceReference<ManagedServiceFactory> reference) {
        synchronized (managedServiceFactoryReferences) {
            managedServiceFactoryReferences.remove(factoryPid);
            managedServiceFactories.remove(factoryPid);
        }
    }

    private ManagedServiceFactory getManagedServiceFactory(String factoryPid) {
        synchronized (managedServiceFactoryReferences) {
            return managedServiceFactories.get(factoryPid);
        }
    }

    private ServiceReference<ManagedServiceFactory> getManagedServiceFactoryReference(String factoryPid) {
        synchronized (managedServiceFactoryReferences) {
            return managedServiceFactoryReferences.get(factoryPid);
        }
    }

    private Future<?> asyncDeleted(final ManagedServiceFactory service, final String factoryPid, final String pid) {
        return caFactory.updateQueue.add(factoryPid, new Runnable() {
            @Override
            public void run() {
                try {
                    service.deleted(pid);
                } catch (Throwable t) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc,
                                 "asyncDelete(): Exception thrown while trying to update ManagedServiceFactory.  Exception = "
                                                 + t.toString());
                    }
                    FFDCFilter.processException(t, ME,
                                                "asyncDelete()",
                                                new Object[] { factoryPid, pid, service });
                }
            }
        });
    }

    private Future<?> asyncUpdated(final ManagedServiceFactory service, final String factoryPid, final String pid, final Dictionary<String, ?> properties) {
        return caFactory.updateQueue.add(factoryPid, new Runnable() {
            @Override
            public void run() {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "asyncUpdated: service.updated called for " + pid + " (" + factoryPid + ")");
                }
                try {
                    service.updated(pid, properties);
                } catch (Throwable t) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc,
                                 "asyncUpdated(): Exception thrown while trying to update ManagedServiceFactory.  Exception = "
                                                 + t.toString());
                    }
                    FFDCFilter.processException(t, ME,
                                                "asyncUpdated()",
                                                new Object[] { factoryPid, pid, service });
                }
            }
        });
    }

    protected Future<?> notifyDeleted(ExtendedConfigurationImpl config) {
        config.checkLocked();
        String factoryPid = config.getFactoryPid(false);
        ServiceReference<ManagedServiceFactory> reference = getManagedServiceFactoryReference(factoryPid);
        if (reference != null && config.bind(reference.getBundle()))
            return asyncDeleted(getManagedServiceFactory(factoryPid), factoryPid, config.getPid(false));

        return null;
    }

    protected Future<?> notifyUpdated(ExtendedConfigurationImpl config) {
        config.checkLocked();
        String factoryPid = config.getFactoryPid();
        ServiceReference<ManagedServiceFactory> reference = getManagedServiceFactoryReference(factoryPid);
        if (reference != null && config.bind(reference.getBundle())) {
            ManagedServiceFactory msf = getManagedServiceFactory(factoryPid);
            // must make a copy
            Dictionary<String, Object> properties = config.getProperties();
            caFactory.modifyConfiguration(reference, properties, msf);
            return asyncUpdated(msf, factoryPid, config.getPid(), properties);
        }

        return null;
    }

    private List<String> getPidsForManagedServiceFactory(ManagedServiceFactory service) {
        ArrayList<String> pids = new ArrayList<String>();
        synchronized (managedServiceFactoryReferences) {
            for (Map.Entry<String, ManagedServiceFactory> entry : managedServiceFactories.entrySet()) {
                if (entry.getValue() == service)
                    pids.add(entry.getKey());
            }

            return pids;
        }
    }
}
