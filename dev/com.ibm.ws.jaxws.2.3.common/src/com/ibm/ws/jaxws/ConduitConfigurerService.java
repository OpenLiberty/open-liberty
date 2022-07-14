/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *  Lifted from org.apache.cxf.transport.http.asyncclient.Activator
 */
public class ConduitConfigurerService extends ServiceTracker<Bus, Bus> implements ManagedService {
    private Map<String, Object> currentConfig;
    
    // Liberty Code Change
    private static final TraceComponent tc = Tr.register(ConduitConfigurerService.class);

 // Liberty Code Change
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