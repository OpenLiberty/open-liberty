/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.cache;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.cache.spi.CachingProvider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.library.Library;

/**
 * Registers the library of the javax.cache.spi.CachingProvider bell with a predetermined id and properties
 * that enable the httpSessionCache metatype to default to it in the absence of a libraryRef.
 */
@Component(name = "com.ibm.ws.session.cache.defaultprovidersupport", configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class DefaultCachingProviderSupport {
    private String libraryId;
    private ServiceRegistration<Library> registration;

    @Activate
    protected void activate(ComponentContext context) throws InvalidSyntaxException {
        BundleContext bundleContext = context.getBundleContext();

        /*
         * Register the library as a service.
         */
        registration = registerLibrary(bundleContext, this.libraryId);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        registration.unregister();
    }

    @Reference(service = CachingProvider.class, target = "(exported.from=*)")
    protected void setCachingProvider(ServiceReference<CachingProvider> ref) {
        libraryId = (String) ref.getProperty("exported.from");
    }

    protected void unsetCachingProvider(ServiceReference<CachingProvider> ref) {
        libraryId = null;
    }

    /**
     * Register a library that can be used to satisfy the library reference required by {@link CacheStoreService}.
     * 
     * @param context   The {@link BundleContext} to use to register the service.
     * @param libraryId The ID for the library to use.
     * @return The {@link ServiceRegistration} for the new service.
     * @throws InvalidSyntaxException If the filter formed by using the library ID was invalid.
     */
    public static ServiceRegistration<Library> registerLibrary(BundleContext context, String libraryId) throws InvalidSyntaxException {
        /*
         * Find the service reference for the library.
         */
        String filter = FilterUtils.createPropertyFilter("id", libraryId);
        ServiceReference<Library> libraryRef = context.getServiceReferences(Library.class, filter).iterator().next();
        Library library = context.getService(libraryRef);

        /*
         * Create some properties to register the service so it is findable.
         */
        Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
        serviceProps.put("id", "com.ibm.ws.session.cache.defaultprovider.library");
        serviceProps.put("service.ranking", -1); // prefer other libraries
        serviceProps.put("zero", 0); // useful for writing a filter that matches only when ${count(libraryRef)} is 0.

        /*
         * Register the service.
         */
        return context.registerService(Library.class, library, serviceProps);
    }
}
