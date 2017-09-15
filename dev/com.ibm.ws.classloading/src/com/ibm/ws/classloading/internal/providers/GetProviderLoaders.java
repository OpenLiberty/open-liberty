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
package com.ibm.ws.classloading.internal.providers;

import static com.ibm.ws.classloading.internal.providers.Providers.bundleContext;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_VENDOR;

import java.util.Collection;
import java.util.EnumSet;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.ClassProvider;
import com.ibm.ws.classloading.LibertyClassLoader;
import com.ibm.ws.classloading.internal.LibertyLoader;
import com.ibm.ws.classloading.internal.util.BlockingList.Listener;
import com.ibm.ws.classloading.internal.util.BlockingList.Retriever;
import com.ibm.ws.classloading.internal.util.BlockingList.Slot;
import com.ibm.ws.classloading.internal.util.ElementNotReadyException;
import com.ibm.ws.classloading.internal.util.ElementNotValidException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.classloading.ApiType;

public class GetProviderLoaders implements Retriever<String, LibertyLoader>, Listener<String, LibertyLoader> {
    static final TraceComponent tc = Tr.register(GetProviderLoaders.class);

    private final String id;
    private final EnumSet<ApiType> apis;

    public GetProviderLoaders(String id, EnumSet<ApiType> gwApis) {
        this.id = id;
        this.apis = gwApis;
    }

    @Override
    public void listenFor(final String providerId, final Slot<? super LibertyLoader> slot) {
        String filterString = String.format("(&(%s=%s)(id=%s))",
                                            OBJECTCLASS,
                                            ClassProvider.class.getName(),
                                            providerId);
        Filter filter;
        try {
            filter = bundleContext.createFilter(filterString);
            new ServiceTracker<ClassProvider, Void>(bundleContext, filter, null) {
                @Override
                @FFDCIgnore({ ElementNotReadyException.class, ElementNotValidException.class })
                public Void addingService(ServiceReference<ClassProvider> providerRef) {
                    final String methodName = "addingService(): ";
                    try {
                        slot.fill(getLoaderFromProvider(providerId, providerRef));
                        this.close(); // my work here is done
                    } catch (ElementNotReadyException butKeepOnListening) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName + "failed to retrieve element but will keep trying. Exception was " + butKeepOnListening);
                    } catch (ElementNotValidException soStopListening) {
                        slot.delete();
                        this.close(); // my work here is done
                    }
                    return null;
                }
            };
        } catch (InvalidSyntaxException e) { // ffdc this
            if (tc.isErrorEnabled())
                Tr.error(tc, "cls.provider.id.invalid", id, providerId, e.toString());
            slot.delete();
        }
    }

    @Override
    public LibertyLoader fetch(String pid) throws ElementNotReadyException, ElementNotValidException {
        final String methodName = "fetch(): ";
        if (bundleContext == null) {
            throw new ElementNotValidException("Cannot retrieve providers outside OSGi framework");
        }

        // Filter the service references by ID.
        String filterString = String.format("(id=%s)", pid);
        String filter = filterString;
        Collection<ServiceReference<ClassProvider>> refs;
        try {
            refs = bundleContext.getServiceReferences(ClassProvider.class, filter);
        } catch (InvalidSyntaxException e) {
            if (tc.isErrorEnabled())
                Tr.error(tc, "cls.provider.id.invalid", id, pid, e.getMessage());
            throw new ElementNotValidException("Cannot look up provider because the filter '" + filterString + "' has bad syntax: ", e);
        }

        if (refs.isEmpty()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "Could not find provider with id=" + pid);
            throw new ElementNotReadyException("No ClassProvider available with id= " + pid);
        }

        return getLoaderFromProvider(pid, refs.iterator().next());
    }

    private LibertyLoader getLoaderFromProvider(String providerId, ServiceReference<ClassProvider> providerRef)
                    throws ElementNotReadyException, ElementNotValidException {
        final String methodName = "getLoaderFromProvider(): ";

        ClassProvider provider = bundleContext.getService(providerRef);

        if (provider == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "failed retrieving service - provider went away before it could be retrieved");
            throw new ElementNotReadyException("ClassProvider with id=" + providerId + " disappeared while we were trying to use it");
        }

        LibertyClassLoader lcl = provider.getDelegateLoader();

        if (lcl == null) {
            if (tc.isErrorEnabled())
                Tr.error(tc, "cls.provider.loader.null", id, providerId, providerRef.getProperty(SERVICE_VENDOR));
            throw new ElementNotValidException("Provider did not return a classloader on request");
        }

        if (!!!(lcl instanceof LibertyLoader)) {
            if (tc.isErrorEnabled())
                Tr.error(tc, "cls.provider.loader.unknown", id, providerId, providerRef.getProperty(SERVICE_VENDOR));
            throw new ElementNotValidException("Provider returned an unknown loader type: " + lcl.getClass());
        }

        LibertyLoader ll = (LibertyLoader) lcl;
        if (!!!Providers.checkAPITypesMatch(id, apis, providerId, ll.getApiTypeVisibility())) {
            throw new ElementNotValidException("Provider API types do not match class loader API types");
        }

        return ll;
    }

}