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
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.Extension;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduitFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.jaxws.bus.ExtensionProvider;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusListener;

/**
 *  Lifted from org.apache.cxf.transport.http.asyncclient.Activator
 */
//Liberty Code Change
@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class JaxwsConduitConfigActivator implements ExtensionProvider {
    private ConduitConfigurerService conduitConfigurerService;
    public static BundleContext context;
    @Activate
    public void start(BundleContext context) throws Exception {
        conduitConfigurerService = new ConduitConfigurerService(context);
        conduitConfigurerService.open();
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, "org.apache.cxf.transport.http.async");
        context.registerService(ManagedService.class.getName(), conduitConfigurerService, properties);
        this.context = context;
    }

    @Deactivate
    public void stop(BundleContext context) throws Exception {
        conduitConfigurerService.close();
    }

 // Liberty Code Change
    @Override
    public Extension getExtension(Bus bus) {
        return new Extension((ConduitConfigurer.class), HTTPConduitConfigurer.class);
    }

}

