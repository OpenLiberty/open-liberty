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
package com.ibm.ws.clientcontainer.remote.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.yoko.osgi.locator.BundleProviderLoader;
import org.apache.yoko.osgi.locator.Register;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.clientcontainer.remote.common.object.RemoteCORBAObjectInstanceImpl;
import com.ibm.ws.container.service.naming.RemoteObjectInstance;
import com.ibm.ws.container.service.naming.RemoteObjectInstanceEnumImpl;
import com.ibm.ws.container.service.naming.RemoteObjectInstanceImpl;
import com.ibm.ws.container.service.naming.RemoteReferenceObjectInstanceImpl;

/**
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, service = { ClientSupportStubRegisterer.class })
public class ClientSupportStubRegisterer {
    private static String[] CLASSES_TO_REGISTER = {
                                                    _ClientSupport_Stub.class.getName(),
                                                    RemoteObjectInstance.class.getName(),
                                                    RemoteObjectInstanceEnumImpl.class.getName(),
                                                    RemoteObjectInstanceImpl.class.getName(),
                                                    RemoteCORBAObjectInstanceImpl.class.getName(),
                                                    RemoteReferenceObjectInstanceImpl.class.getName()
    };

    private final static AtomicInteger providerCounter = new AtomicInteger(0);

    private Register providerRegistry;
    private final List<BundleProviderLoader> bundleProviderLoaders = new ArrayList<BundleProviderLoader>();

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        //bundleContext.get
        Bundle bundle = bundleContext.getBundle();

        for (String className : CLASSES_TO_REGISTER) {
            BundleProviderLoader bpl = new BundleProviderLoader(className, className, bundle, providerCounter.incrementAndGet());
            bundleProviderLoaders.add(bpl);
            providerRegistry.registerProvider(bpl);
        }
    }

    @Deactivate
    protected void deactivate() {
        Iterator<BundleProviderLoader> iter = bundleProviderLoaders.iterator();
        while (iter.hasNext()) {
            BundleProviderLoader bpl = iter.next();
            providerRegistry.unregisterService(bpl);
            iter.remove();
        }

    }
}