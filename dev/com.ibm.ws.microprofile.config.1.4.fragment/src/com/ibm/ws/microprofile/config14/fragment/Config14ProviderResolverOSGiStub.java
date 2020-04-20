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
package com.ibm.ws.microprofile.config14.fragment;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.management.ServiceNotFoundException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.kernel.service.utils.FrameworkState;

public class Config14ProviderResolverOSGiStub extends ConfigProviderResolver {

    /** {@inheritDoc} */
    @Override
    public Config getConfig() {
        return getConfigProviderResolver().getConfig();
    }

    /** {@inheritDoc} */
    @Override
    public Config getConfig(ClassLoader loader) {
        return getConfigProviderResolver().getConfig(loader);
    }

    /** {@inheritDoc} */
    @Override
    public ConfigBuilder getBuilder() {
        return getConfigProviderResolver().getBuilder();
    }

    /** {@inheritDoc} */
    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        getConfigProviderResolver().registerConfig(config, classLoader);
    }

    /** {@inheritDoc} */
    @Override
    public void releaseConfig(Config config) {
        getConfigProviderResolver().releaseConfig(config);
    }

    /**
     * Get the OSGi bundle context for the given class
     *
     * @param clazz the class to find the context for
     * @return the bundle context
     */
    static BundleContext getBundleContext() {
        BundleContext context;
        if (FrameworkState.isValid()) {
            Bundle bundle = FrameworkUtil.getBundle(Config14ProviderResolverOSGiStub.class);

            if (bundle != null) {
                context = AccessController.doPrivileged((PrivilegedAction<BundleContext>) () -> bundle.getBundleContext());
            } else {
                throw new RuntimeException("OSGi Bundle not found!");
            }
        } else {
            throw new RuntimeException("OSGi Framework not valid");
        }
        return context;
    }

    /**
     * Get the OSGi ConfigProviderResolver service
     *
     * @param bundleContext the bundle context to use to find the service
     * @return the ConfigProviderResolver service
     * @throws InvalidFrameworkStateException if the server OSGi framework is being shutdown
     * @throws ServiceNotFoundException       if an instance of the requested service can not be found
     */
    static ConfigProviderResolver getConfigProviderResolver() {
        return getService(getBundleContext(), ConfigProviderResolver.class);
    }

    /**
     * Find a service of the given type
     *
     * @param bundleContext The context to use to find the service
     * @param serviceClass  The class of the required service
     * @return the service instance
     * @throws InvalidFrameworkStateException if the server OSGi framework is being shutdown
     * @throws ServiceNotFoundException       if an instance of the requested service can not be found
     */
    private static <T> T getService(BundleContext bundleContext, Class<T> serviceClass) {
        if (!FrameworkState.isValid()) {
            throw new RuntimeException("OSGi Framework not valid");
        }

        ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);

        T service = null;
        if (ref != null) {
            service = AccessController.doPrivileged((PrivilegedAction<T>) () -> bundleContext.getService(ref));
        }

        if (service == null) {
            //One last check to make sure the framework didn't start to shutdown after we last looked
            if (!FrameworkState.isValid()) {
                throw new RuntimeException("OSGi Framework not valid");
            } else {
                throw new RuntimeException("OSGi Service not found: " + serviceClass);
            }
        }
        return service;
    }
}