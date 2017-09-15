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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * PluginManager tracks and allows customization via ConfigurationPlugin
 */
class PluginManager {
    private final PluginTracker pluginTracker;

    public PluginManager(BundleContext context) {
        pluginTracker = new PluginTracker(context);
    }

    public void start() {
        pluginTracker.open();
    }

    public void stop() {
        pluginTracker.close();
    }

    public void modifyConfiguration(ServiceReference<?> managedReference, Dictionary<String, Object> properties) {
        if (properties == null)
            return;

        ServiceReference<ConfigurationPlugin>[] references = pluginTracker.getServiceReferences();
        for (int i = 0; i < references.length; ++i) {
            String[] pids = (String[]) references[i].getProperty(ConfigurationPlugin.CM_TARGET);
            if (pids != null) {
                String pid = (String) properties.get(Constants.SERVICE_PID);
                if (!Arrays.asList(pids).contains(pid))
                    continue;
            }
            ConfigurationPlugin plugin = pluginTracker.getService(references[i]);
            if (plugin != null)
                plugin.modifyConfiguration(managedReference, properties);
        }
    }

    private static class PluginTracker extends ServiceTracker<ConfigurationPlugin, ConfigurationPlugin> {
        final Integer ZERO = Integer.valueOf(0);
        private final TreeSet<ServiceReference<ConfigurationPlugin>> serviceReferences = new TreeSet<ServiceReference<ConfigurationPlugin>>(new Comparator<ServiceReference<?>>() {
            @Override
            public int compare(ServiceReference<?> o1, ServiceReference<?> o2) {
                return getRank(o1).compareTo(getRank(o2));
            }

            private Integer getRank(ServiceReference<?> ref) {
                Object ranking = ref.getProperty(ConfigurationPlugin.CM_RANKING);
                if (ranking == null || !(ranking instanceof Integer))
                    return ZERO;
                return ((Integer) ranking);
            }
        });

        public PluginTracker(BundleContext context) {
            super(context, ConfigurationPlugin.class.getName(), null);
        }

        /*
         * NOTE: this method alters the contract of the overriden method.
         * Rather than returning null if no references are present, it
         * returns an empty array.
         */
        @Override
        public ServiceReference<ConfigurationPlugin>[] getServiceReferences() {
            synchronized (serviceReferences) {
                @SuppressWarnings("unchecked")
                ServiceReference<ConfigurationPlugin>[] result = new ServiceReference[0];
                return serviceReferences.toArray(result);
            }
        }

        @Override
        public ConfigurationPlugin addingService(ServiceReference<ConfigurationPlugin> reference) {
            synchronized (serviceReferences) {
                serviceReferences.add(reference);
            }
            return context.getService(reference);
        }

        @Override
        public void modifiedService(ServiceReference<ConfigurationPlugin> reference, ConfigurationPlugin service) {
            // nothing to do
        }

        @Override
        public void removedService(ServiceReference<ConfigurationPlugin> reference, ConfigurationPlugin service) {
            synchronized (serviceReferences) {
                serviceReferences.remove(reference);
            }
            context.ungetService(reference);
        }
    }
}
