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

package com.ibm.ws.logging.internal.osgi;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCConfigurator;

/**
 * This class is instantiated during RAS bundle activation. It registers itself
 * as a ManagedService with the Config service in order to receive updates to
 * the RAS Tr configuration.
 */
public class LoggingConfigurationService implements ManagedService {
    private static final TraceComponent tc = Tr.register(LoggingConfigurationService.class);

    /** PID: identifies bundle to ConfigAdminService */
    public static final String RAS_TR_CFG_PID = "com.ibm.ws.logging";

    /** reference to registered RAS config service */
    private ServiceRegistration<ManagedService> configRef = null;

    protected BundleContext context;

    /*
     * Indicates whether instrumentation agent is available to implement dynamic config changes
     */
    private final boolean instrumentation;

    /**
     * Constructor.
     * 
     * @param context
     */
    public LoggingConfigurationService(BundleContext context, boolean instrumentationActive) {
        this.context = context;
        this.instrumentation = instrumentationActive;

        // Register this as a "ManagedService" to get calls when the config is
        // updated after we've taken care of merging config manually via
        // getConfiguration
        configRef = context.registerService(ManagedService.class, this, defaultProperties());

        TrConfigurator.setInstrumentation(instrumentation);
    }

    /**
     * Stop this service and free any allocated resources when the owning bundle
     * is being stopped.
     */
    public void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Stopping the Logging managed service");
        }
        // disconnect from the config admin
        this.configRef.unregister();
        this.configRef = null;
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public synchronized void updated(Dictionary properties) throws ConfigurationException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "properties updated " + properties);

        if (properties == null)
            return;

        Map<String, Object> newMap = null;
        if (properties instanceof Map) {
            newMap = (Map<String, Object>) properties;
        } else {
            newMap = new HashMap<String, Object>();
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                newMap.put(key, properties.get(key));
            }
        }

        // Update Tr and/or FFDC configurations.
        // --> of concern is changing the log directory.
        TrConfigurator.update(newMap);
        FFDCConfigurator.update(newMap);
    }

    protected static Hashtable<String, String> defaultProperties() {
        Hashtable<String, String> ht = new Hashtable<String, String>();
        ht.put(org.osgi.framework.Constants.SERVICE_PID, RAS_TR_CFG_PID);
        return ht;
    }
}
