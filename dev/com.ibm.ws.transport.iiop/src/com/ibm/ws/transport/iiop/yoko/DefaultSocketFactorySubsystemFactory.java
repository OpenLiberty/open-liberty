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

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.yoko.osgi.locator.BundleProviderLoader;
import org.apache.yoko.osgi.locator.Register;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.transport.iiop.spi.IIOPEndpoint;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

/**
 *
 */
@Component(service = SubsystemFactory.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "service.ranking:Integer=1" })
public class DefaultSocketFactorySubsystemFactory extends SubsystemFactory {

    /**  */
    private static final String IIOP_CONNECTION_HELPER = "-IIOPconnectionHelper";

    private Register providerRegistry;
    private BundleProviderLoader connectionHelperClass;

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        Bundle bundle = bundleContext.getBundle();
        connectionHelperClass = new BundleProviderLoader(DefaultSocketFactory.class.getName(), DefaultSocketFactory.class.getName(), bundle, 1);
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
