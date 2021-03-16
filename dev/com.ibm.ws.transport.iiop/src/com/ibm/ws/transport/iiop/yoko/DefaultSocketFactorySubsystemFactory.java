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
package com.ibm.ws.transport.iiop.yoko;

import com.ibm.ws.transport.iiop.spi.IIOPEndpoint;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;
import org.apache.yoko.osgi.locator.LocalFactory;
import org.apache.yoko.osgi.locator.Register;
import org.apache.yoko.osgi.locator.ServiceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component(service = SubsystemFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "service.ranking:Integer=1" })
public class DefaultSocketFactorySubsystemFactory extends SubsystemFactory {
    private static enum MyLocalFactory implements LocalFactory {
        INSTANCE;
        public Class<?> forName(String name) throws ClassNotFoundException {
            return Class.forName(name);
        }
        public Object newInstance(Class cls) throws InstantiationException, IllegalAccessException {
            return null;
        }
    }

    private static final String IIOP_CONNECTION_HELPER = "-IIOPconnectionHelper";

    private Register providerRegistry;
    private ServiceProvider connectionHelperClass;

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        connectionHelperClass = new ServiceProvider(MyLocalFactory.INSTANCE, DefaultSocketFactory.class);
        providerRegistry.registerProvider(connectionHelperClass);
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregisterProvider(connectionHelperClass);
    }

    @Override
    public void addTargetORBInitProperties(Properties initProperties, Map<String, Object> orbProperties, List<IIOPEndpoint> endpoints, Map<String, Object> extraProperties) {}

    @Override
    public void addTargetORBInitArgs(Map<String, Object> targetProperties, List<String> args) {
        if (!args.contains(IIOP_CONNECTION_HELPER)) {
            args.add(IIOP_CONNECTION_HELPER);
            args.add(DefaultSocketFactory.class.getName());
        }
    }

    @Override
    public void addClientORBInitArgs(Map<String, Object> clientProperties, List<String> args) {
        if (!args.contains(IIOP_CONNECTION_HELPER)) {
            args.add(IIOP_CONNECTION_HELPER);
            args.add(DefaultSocketFactory.class.getName());
        }
    }
}
