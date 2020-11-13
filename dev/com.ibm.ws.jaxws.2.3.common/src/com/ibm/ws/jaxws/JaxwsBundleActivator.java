/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import com.ibm.ws.jaxws.client.JaxwsHttpConduitConfigApplier;

/**
 *
 */
@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class JaxwsBundleActivator implements ExtensionProvider {
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

    @Override
    public Extension getExtension(Bus bus) {
        return new Extension((ConduitConfigurer.class), HTTPConduitConfigurer.class);
    }

}

