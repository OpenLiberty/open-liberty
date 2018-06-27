/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.internal;

import org.apache.yoko.osgi.locator.LocalFactory;
import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class WSClassRegistration {
    private enum MyLocalFactory implements LocalFactory {
        INSTANCE;
        public Class<?> forName(String name) throws ClassNotFoundException {
            return Class.forName(name);
        }
        public Object newInstance(Class cls) throws InstantiationException, IllegalAccessException {
            return cls.newInstance();
        }
    }
    private Register providerRegistry;
    private ServiceProvider proClass;
    private ServiceProvider utilClass;

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        proClass = new ServiceProvider(MyLocalFactory.INSTANCE,"javax.rmi.CORBA.PortableRemoteObjectClass", WSPortableRemoteObjectImpl.class, 2);
        providerRegistry.registerService(proClass);

        utilClass = new ServiceProvider(MyLocalFactory.INSTANCE,"javax.rmi.CORBA.UtilClass", WSUtilImpl.class, 2);
        providerRegistry.registerService(utilClass);
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregisterService(proClass);
        providerRegistry.unregisterService(utilClass);
    }
}
