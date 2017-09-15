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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.internal.DeclaredApiAccess;
import com.ibm.ws.classloading.internal.LibertyLoader;
import com.ibm.ws.classloading.internal.util.BlockingList;
import com.ibm.ws.classloading.internal.util.BlockingList.Logger;
import com.ibm.ws.classloading.internal.util.BlockingListMaker;
import com.ibm.ws.classloading.internal.util.CompositeIterable;
import com.ibm.ws.library.internal.SharedLibraryFactory;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.library.Library;

public class Providers {
    static final TraceComponent tc = Tr.register(Providers.class);
    static final BundleContext bundleContext;

    static {
        Bundle ourBundle = FrameworkUtil.getBundle(SharedLibraryFactory.class);
        bundleContext = (ourBundle != null) ? ourBundle.getBundleContext() : null;
    }

    public static List<Library> getPrivateLibraries(ClassLoaderConfiguration config) {
        List<String> privateLibraries = config.getSharedLibraries();
        if (privateLibraries == null || privateLibraries.isEmpty()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RETURN (privateLibraries == null || privateLibraries.isEmpty())");
            return Collections.emptyList();
        }

        // Make sure we have a bundleContext.
        if (checkBundleContext() == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RETURN --> checkBundleContext() == null");
            return Collections.emptyList();
        }

        GetLibraries getLibraries = new GetLibraries(config.getId().getId());
        return BlockingListMaker.defineList().waitFor(10, SECONDS).fetchElements(getLibraries).listenForElements(getLibraries).log(LOGGER).useKeys(privateLibraries).make();
    }

    public static List<LibertyLoader> getCommonLibraryLoaders(ClassLoaderConfiguration config, DeclaredApiAccess apiAccess) {
        List<String> commonLibIds = config.getCommonLibraries();
        if (commonLibIds == null || commonLibIds.isEmpty()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RETURN (commonLibIds == null || commonLibIds.isEmpty())");
            return Collections.emptyList();
        }

        final EnumSet<ApiType> gwApis = apiAccess.getApiTypeVisibility();
        if (gwApis == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RETURN  -->  gwApis == null !!!!");
            return Collections.emptyList();
        }

        checkBundleContext();

        // this list will try to retrieve the libraries on demand
        // and it will block until they are available
        GetLibraryLoaders getLibraryLoaders = new GetLibraryLoaders(config.getId().getId(), gwApis);
        return BlockingListMaker.defineList().waitFor(10, SECONDS).fetchElements(getLibraryLoaders).listenForElements(getLibraryLoaders).log(LOGGER).useKeys(commonLibIds).make();
    }

    public static List<LibertyLoader> getProviderLoaders(ClassLoaderConfiguration config, DeclaredApiAccess apiAccess) {
        final String methodName = "getProviderLoaders(): ";
        List<String> providerIds = config.getClassProviders();
        if (providerIds == null || providerIds.isEmpty()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "EARLY RETURN: provider ids = " + providerIds);
            return Collections.emptyList();
        }

        final EnumSet<ApiType> gwApis = apiAccess.getApiTypeVisibility();
        if (gwApis == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "EARLY RETURN: gwApis == null");
            return Collections.emptyList();
        }

        checkBundleContext();

        // this list will try to retrieve the providers on demand
        // and it will block until they are available
        GetProviderLoaders getProviderLoaders = new GetProviderLoaders(config.getId().getId(), gwApis);
        return BlockingListMaker.defineList().waitFor(10, SECONDS).fetchElements(getProviderLoaders).listenForElements(getProviderLoaders).log(LOGGER).useKeys(providerIds).make();
    }

    @SuppressWarnings("unchecked")
    public static Iterable<LibertyLoader> getDelegateLoaders(ClassLoaderConfiguration config, DeclaredApiAccess apiAccess) {
        return new CompositeIterable<LibertyLoader>(getCommonLibraryLoaders(config, apiAccess), getProviderLoaders(config, apiAccess));
    }

    /**
     * Retrieve a library from the service registry.
     *
     * @return A library with the given id or <code>null</code> if no match was found
     */
    static Library getSharedLibrary(String id) {

        if (bundleContext == null) {
            return null;
        }

        // Filter the SharedLibrary service references by ID.
        String filter = "(" + "id=" + id + ")";
        Collection<ServiceReference<Library>> refs = null;
        try {
            refs = bundleContext.getServiceReferences(Library.class, filter);
        } catch (InvalidSyntaxException e) {
            if (tc.isErrorEnabled()) {
                Tr.error(tc, "cls.library.id.invalid", id, e.toString());
            }
            return null;
        }

        if (refs.isEmpty())
            return null;

        return bundleContext.getService(getHighestRankedService(refs));
    }

    private static ServiceReference<Library> getHighestRankedService(Collection<ServiceReference<Library>> refs) {
        ServiceReference<Library> ref = null;
        if (refs != null) {
            int curHighRanking = -1;
            for (ServiceReference<Library> r : refs) {
                Object o = r.getProperty("service.ranking");
                if (o != null && o instanceof Integer) {
                    int ranking = (Integer) o;
                    if (ref == null) {
                        ref = r;
                        curHighRanking = ranking;
                    } else if (ranking > curHighRanking) {
                        ref = r;
                        curHighRanking = ranking;
                    }
                }
            }
        }
        return ref;
    }

    static boolean checkAPITypesMatch(Library sharedLibrary, String loaderID, EnumSet<ApiType> loaderAPIs) {
        final String methodName = "checkAPITypesMatch(for library): ";
        Object libAPIs = sharedLibrary.getApiTypeVisibility();
        if (libAPIs == null ? loaderAPIs == null : libAPIs.equals(loaderAPIs)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "Loader " + loaderID + " was allowed to use library " + sharedLibrary.id()
                             + " because their allowed API types were consistent",
                         loaderAPIs, libAPIs);
            return true;
        }
        if (tc.isWarningEnabled())
            Tr.warning(tc, "cls.class.space.conflict", loaderID, loaderAPIs, sharedLibrary.id(), libAPIs);

        return true;
    }

    static boolean checkAPITypesMatch(String cid, EnumSet<ApiType> consumerApiTypes, String pid, EnumSet<ApiType> providerApiTypes) {
        final String methodName = "checkAPITypesMatch(for provider): ";
        if (consumerApiTypes == null ? providerApiTypes == null : consumerApiTypes.equals(providerApiTypes)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "Loader " + cid + " was allowed to use provider " + pid
                             + " because their allowed API types were consistent",
                         consumerApiTypes, providerApiTypes);
            return true;
        }
        if (tc.isWarningEnabled())
            Tr.warning(tc, "cls.provider.class.space.conflict", cid, consumerApiTypes, pid, providerApiTypes);
        return true;
    }

    /**
     * Check to make sure the bundleContext has been set.
     *
     * @return The BundleContext, or null if it could not be retrieved.
     */
    private static BundleContext checkBundleContext() {
        if (bundleContext == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "BundleContext is null and should not be");
            }
        }
        return bundleContext;
    }

    static final Logger LOGGER = new Logger() {
        @Override
        public void logTimeoutEvent(BlockingList<?, ?> list) {
            if (tc.isAuditEnabled() && tc.isWarningEnabled())
                for (Object id : list.getUnmatchedKeys())
                    Tr.warning(tc, "cls.library.missing", id);
        }
    };

}
