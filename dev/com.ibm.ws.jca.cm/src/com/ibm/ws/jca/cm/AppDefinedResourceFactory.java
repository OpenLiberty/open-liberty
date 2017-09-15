/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cm;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * AppDefinedResourceFactory is a Future-like wrapper for an application defined resource factory.
 * When createResource is invoked on the AppDefinedResourceFactory,
 * it waits, if necessary for ConfigurationAdmin to finish creating the resource factory, and then
 * delegates createResource to that resource factory.
 */
public class AppDefinedResourceFactory implements com.ibm.ws.resource.ResourceFactory {
    private static final TraceComponent tc = Tr.register(AppDefinedResourceFactory.class);

    /**
     * Name of the application that defines this resource.
     */
    private final String appName;

    /**
     * ResourceFactoryBuilder instance that created this resource factory.
     */
    private final ResourceFactoryBuilder builder;

    /**
     * Unique identifier for the resource factory.
     */
    private final String id;

    /**
     * Service tracker for this resource factory.
     */
    private final ServiceTracker<ResourceFactory, ResourceFactory> tracker;

    /**
     * Construct a Future-like wrapper for an application-defined resource factory.
     * 
     * @param builder the resource factory builder
     * @param bundleContext the bundle context
     * @param id unique identifier for the resource factory
     * @param filter filter for the resource factory
     * @param appName name of the application that defines the resource factory
     * @throws InvalidSyntaxException if the filter has incorrect syntax
     */
    public AppDefinedResourceFactory(ResourceFactoryBuilder builder, BundleContext bundleContext, String id, String filter, String appName) throws InvalidSyntaxException {
        this.appName = appName;
        this.builder = builder;
        this.id = id;

        // The resource factory is activated asynchronously. ServiceTracker is used to wait for it when we need it.
        tracker = new ServiceTracker<ResourceFactory, ResourceFactory>(bundleContext, bundleContext.createFilter(filter), null);
        tracker.open();
    }

    /**
     * @see com.ibm.wsspi.resource.ResourceFactory#createResource(com.ibm.wsspi.resource.ResourceInfo)
     */
    @Override
    public Object createResource(ResourceInfo info) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createResource", info);

        Object resource;

        try {
            ResourceFactory factory = tracker.waitForService(5000);
            if (factory == null)
                throw new Exception(ConnectorService.getMessage("MISSING_RESOURCE_J2CA8030", info.getType(), id, "application", appName));

            resource = factory.createResource(info);
        } catch (Exception x) {
            FFDCFilter.processException(x, getClass().getName(), "129", this);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "createResource", x);
            throw x;
        } catch (Error x) {
            FFDCFilter.processException(x, getClass().getName(), "134", this);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "createResource", x);
            throw x;
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "createResource", resource);
        return resource;
    }

    /**
     * @see com.ibm.ws.resource.ResourceFactory#createResource(com.ibm.ws.resource.ResourceRefInfo)
     */
    @Override
    public Object createResource(ResourceRefInfo ref) throws Exception {
        return createResource((ResourceInfo) ref);
    }

    /**
     * Destroy this application-defined resource by removing its configuration
     * and the configuration of all other services that were created for it.
     * 
     * @throws Exception if an error occurs.
     */
    @Override
    public void destroy() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "destroy", id);

        tracker.close();

        StringBuilder filter = new StringBuilder(FilterUtils.createPropertyFilter(AbstractConnectionFactoryService.ID, id));
        filter.insert(filter.length() - 1, '*');

        builder.removeExistingConfigurations(filter.toString());

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "destroy");
    }

    @Override
    public void modify(Map<String, Object> props) throws Exception {
        throw new UnsupportedOperationException();
    }
}
