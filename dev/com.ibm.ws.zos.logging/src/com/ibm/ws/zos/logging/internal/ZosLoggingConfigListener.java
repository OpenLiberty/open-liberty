/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * Keeps track of config updates to <zosLogging> and forwards them to
 * ZosLoggingBundleActivator.
 */
public class ZosLoggingConfigListener {

    /**
     * Config updates are relayed to this guy, who then registers/deregisters
     * z LogHandlers accordingly.
     */
    private final ZosLoggingBundleActivator zosLoggingBundleActivator;

    /**
     * Used to track and handle registration of this service
     */
    private ServiceRegistration<ZosLoggingConfigListener> serviceRegistration;

    /**
     * CTOR.
     */
    public ZosLoggingConfigListener(ZosLoggingBundleActivator zosLoggingBundleActivator) {
        this.zosLoggingBundleActivator = zosLoggingBundleActivator;
    }

    /**
     * Forward config update to ZosLoggingBundleActivator.
     */
    public void updated(Dictionary conf) {
        // We can be called with a null dictionary. Skip.
        if (conf == null)
            return;

        zosLoggingBundleActivator.configUpdated(conf);
    }

    /**
     * Register as a ManagedService
     *
     * @return this
     */
    protected synchronized ZosLoggingConfigListener register(BundleContext bundleContext) {

        if (serviceRegistration != null) {
            return this; // Already registered.
        }

        Dictionary<String, Object> props = new Hashtable<String, Object>(1);
        props.put(Constants.SERVICE_VENDOR, "IBM");

        // register the ManagedService that will handle the config
        serviceRegistration = bundleContext.registerService(ZosLoggingConfigListener.class, this, props);
        return this;
    }

    /**
     * Unregister this ManagedService from OSGI.
     */
    protected synchronized void unregister() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

}