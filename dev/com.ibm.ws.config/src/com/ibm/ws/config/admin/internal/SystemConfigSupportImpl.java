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
package com.ibm.ws.config.admin.internal;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.config.MetaTypeProviderConstants;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.admin.SystemConfigSupport;

/**
 *
 */
class SystemConfigSupportImpl implements SystemConfigSupport {

    private final ConfigAdminServiceFactory caFactory;
    private final ServiceRegistration<?> caSupportRef;

    /** Event tracker for sending metatype completion events */
    private ServiceTracker<EventAdmin, EventAdmin> eventTracker = null;

    /**
     * @param configAdminServiceFactory
     */
    public SystemConfigSupportImpl(BundleContext bc, ConfigAdminServiceFactory configAdminServiceFactory) {
        caFactory = configAdminServiceFactory;

        // register SystemConfigSupport service
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("service.vendor", "IBM");
        this.caSupportRef = bc.registerService(SystemConfigSupport.class.getName(), this, properties);

        eventTracker = new ServiceTracker<EventAdmin, EventAdmin>(bc, EventAdmin.class.getName(), null);
        eventTracker.open();
    }

    public void stop() {
        caSupportRef.unregister();

        if (eventTracker != null) {
            eventTracker.close();
            eventTracker = null;
        }
    }

    @Override
    public void openManagedServiceTrackers() {
        caFactory.openManagedServiceTrackers();
    }

    @Override
    public ExtendedConfiguration lookupConfiguration(ConfigID id) {
        return caFactory.lookupConfiguration(id);
    }

    @Override
    public Set<ConfigID> getReferences(ConfigID id) {
        return caFactory.getReferences(id);
    }

    @Override
    public void registerConfiguration(ConfigID id, ExtendedConfiguration config) {
        caFactory.registerConfiguration(id, (ExtendedConfigurationImpl) config);
    }

    @Override
    public ExtendedConfiguration findConfiguration(String pid) {
        return caFactory.findConfiguration(pid);
    }

    @Override
    public boolean waitForAll(Collection<Future<?>> futureList, long timeout, TimeUnit timeUnit) {
        return UpdateQueue.waitForAll(futureList, timeout, timeUnit);
    }

    @Override
    public void fireMetatypeAddedEvent(String pid) {
        final String factoryPid = pid;
        final EventAdmin eventAdmin = eventTracker.getService();
        caFactory.updateQueue.add(pid, new Runnable() {

            @Override
            public void run() {
                Map<String, Object> properties = new HashMap<String, Object>();
                properties.put(MetaTypeProviderConstants.UPDATED_PID, factoryPid);
                Event event = new Event(MetaTypeProviderConstants.METATYPE_PROVIDER_ADDED_TOPIC, properties);
                eventAdmin.postEvent(event);

            }
        });

    }

    @Override
    public void fireMetatypeRemovedEvent(String pid) {
        final String factoryPid = pid;
        final EventAdmin eventAdmin = eventTracker.getService();
        caFactory.updateQueue.add(pid, new Runnable() {

            @Override
            public void run() {
                Map<String, Object> properties = new HashMap<String, Object>();
                properties.put(MetaTypeProviderConstants.UPDATED_PID, factoryPid);
                Event event = new Event(MetaTypeProviderConstants.METATYPE_PROVIDER_REMOVED_TOPIC, properties);
                eventAdmin.postEvent(event);

            }
        });
    }
}
