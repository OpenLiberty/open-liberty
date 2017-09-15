/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.classloading;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.ServiceConfigurationError;

import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

/**
 * This class is to be declared as a service by any component wishing to 'export'
 * resource files so they can be loaded by application classloaders.
 */
public class ResourceProvider {
    private static final String RESOURCE_LIST_PROPERTY = "resources";

    private Collection<String> resourceNames;

    private ClassLoader bundleLoader;

    private String bundleID;

    /**
     * DS method to activate this component
     */
    protected void activate(BundleContext bCtx, Map<String, Object> properties) {
        bundleID = bCtx.getBundle().getSymbolicName() + "-" + bCtx.getBundle().getVersion();
        bundleLoader = bCtx.getBundle().adapt(BundleWiring.class).getClassLoader();
        try {
            Object prop = properties.get(RESOURCE_LIST_PROPERTY);
            if (prop instanceof String) {
                String resource = (String) prop;
                resourceNames = Arrays.asList(resource);
            } else if (prop instanceof String[]) {
                String[] resources = (String[]) prop;
                resourceNames = Arrays.asList(resources);
            } else if (prop == null) {
                // Internal WAS error => no NLS
                // unacceptable not to provide a resource list - better not to instantiate this component
                throw new ClassLoadingConfigurationException("Missing property " + RESOURCE_LIST_PROPERTY);
            } else {
                // Internal WAS error => no NLS
                throw new ClassLoadingConfigurationException("Unexpected value for property " + RESOURCE_LIST_PROPERTY + "=" + prop);
            }
        } catch (ClassLoadingConfigurationException e) {
            // catch the exception so it is FFDC'd by the implementation
            // re-throw to abort component creation
            // Internal WAS error => no NLS
            throw new ServiceConfigurationError("Incorrectly configured ResourceProvider in bundle " + bundleID, e);
        }
    }

    public URL findResource(String resourceName) throws SecurityException {
        return bundleLoader.getResource(resourceName);
    }

    public Enumeration<URL> findResources(String resourceName) throws SecurityException, IOException {
        return bundleLoader.getResources(resourceName);
    }

    public Collection<String> getResourceNames() {
        return resourceNames;
    }
}
