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
package com.ibm.ws.jaxws;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduitFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 */
public class ConduitConfigurerService extends ServiceTracker<Bus, Bus> implements ManagedService {
    private Map<String, Object> currentConfig;

    public ConduitConfigurerService(BundleContext context) {
        super(context, Bus.class, null);
    }
   


    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        this.currentConfig = toMap(properties);
        Bus[] buses = (Bus[])getServices();
        if (buses == null) {
            return;
        }
        for (Bus bus : buses) {
            configureConduitFactory(bus);
        }
    }

    @Override
    public Bus addingService(ServiceReference<Bus> reference) {
        Bus bus = super.addingService(reference);
        configureConduitFactory(bus);
        return bus;
    }

    private Map<String, Object> toMap(Dictionary<String, ?> properties) {
        Map<String, Object> props = new HashMap<>();
        if (properties == null) {
            return props;
        }
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            props.put(key, properties.get(key));
        }
        return props;
    }

    
    private void configureConduitFactory(Bus bus) {
        AsyncHTTPConduitFactory conduitFactory = bus.getExtension(AsyncHTTPConduitFactory.class);
        conduitFactory.update(this.currentConfig);
    }
}