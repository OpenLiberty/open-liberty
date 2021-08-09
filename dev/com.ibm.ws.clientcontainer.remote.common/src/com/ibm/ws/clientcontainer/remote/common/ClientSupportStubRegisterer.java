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

import com.ibm.ws.clientcontainer.remote.common.object.RemoteCORBAObjectInstanceImpl;
import com.ibm.ws.container.service.naming.RemoteObjectInstance;
import com.ibm.ws.container.service.naming.RemoteObjectInstanceEnumImpl;
import com.ibm.ws.container.service.naming.RemoteObjectInstanceImpl;
import com.ibm.ws.container.service.naming.RemoteReferenceObjectInstanceImpl;
import org.apache.yoko.osgi.locator.LocalFactory;
import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, service = { ClientSupportStubRegisterer.class })
public class ClientSupportStubRegisterer {
    private static enum MyLocalFactory implements LocalFactory {
        INSTANCE;
        public Class<?> forName(String name) throws ClassNotFoundException {
            return Class.forName(name);
        }
        public Object newInstance(Class cls) throws InstantiationException, IllegalAccessException {
            return null;
        }
    }

    private final static ServiceProvider[] providers = {
        new ServiceProvider(MyLocalFactory.INSTANCE, _ClientSupport_Stub.class),
        new ServiceProvider(MyLocalFactory.INSTANCE, RemoteObjectInstance.class),
        new ServiceProvider(MyLocalFactory.INSTANCE, RemoteObjectInstanceEnumImpl.class),
        new ServiceProvider(MyLocalFactory.INSTANCE, RemoteObjectInstanceImpl.class),
        new ServiceProvider(MyLocalFactory.INSTANCE, RemoteCORBAObjectInstanceImpl.class),
        new ServiceProvider(MyLocalFactory.INSTANCE, RemoteReferenceObjectInstanceImpl.class)
    };

    private final static AtomicInteger providerCounter = new AtomicInteger(0);

    private Register providerRegistry;

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        for (ServiceProvider sp: providers) {
            providerRegistry.registerProvider(sp);
        }
    }

    @Deactivate
    protected void deactivate() {
        for (ServiceProvider sp: providers) {
            providerRegistry.unregisterProvider(sp);
        }
    }
}