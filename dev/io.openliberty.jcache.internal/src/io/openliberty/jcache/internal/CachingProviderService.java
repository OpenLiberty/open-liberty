/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package io.openliberty.jcache.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.library.spi.SpiLibrary;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.library.Library;

/**
 * Service that configures a {@link CachingProvider} to use with JCache caching.
 */
@Component(service = CachingProviderService.class, immediate = true, configurationPolicy = REQUIRE, configurationPid = "io.openliberty.jcache.cachingprovider")
public class CachingProviderService {
    private static final TraceComponent tc = Tr.register(CachingProviderService.class);

    private static final String KEY_PROVIDER_CLASS = "providerClass";
    private static final String KEY_ID = "id";

    private CachingProvider cachingProvider = null;
    private Set<Library> commonLibraries = new HashSet<Library>();
    private Library jCacheLibrary;
    private ClassLoader classLoader = null;
    private String cachingProviderClass = null;
    private String id = null;

    private ClassLoadingService classLoadingService = null;

    /** An object that the CachingProviderService, CacheManagerService and CacheService should sync on before closing. */
    private Object closeSyncObject = null;

    /**
     * Activate this OSGi component.
     *
     * @param configProps The configuration to use when activating this component.
     * @throws Exception If the component could not activate.
     */
    @Activate
    public void activate(Map<String, Object> configProps) throws Exception {
        closeSyncObject = new Object();

        /*
         * Get the cache name and the ID.
         */
        cachingProviderClass = (String) configProps.get(KEY_PROVIDER_CLASS);
        id = (String) configProps.get(KEY_ID);

        /*
         * load JCache provider from configured library, which is either specified as a
         * libraryRef.
         *
         * TODO???? No doPriv due to limitations in OSGi and security manager. If
         * running with SecurityManager, permissions will need to be granted explicitly.
         */
        try {
            ClassLoader classloader = getUnifiedClassLoader();
            if (cachingProviderClass != null && !cachingProviderClass.trim().isEmpty()) {
                cachingProvider = Caching.getCachingProvider(cachingProviderClass, classloader);
            } else {
                cachingProvider = Caching.getCachingProvider(classloader);
            }
        } catch (Throwable e) {
            Tr.error(tc, "CWLJC0004_GET_PROVIDER_FAILED", id, e);
            throw e;
        }
    }

    @Deactivate
    @FFDCIgnore({ Exception.class })
    public void deactivate() {
        /*
         * Close the CachingProvider.
         */
        if (cachingProvider != null) {
            synchronized (closeSyncObject) {
                try {
                    cachingProvider.close();
                } catch (Exception e) {
                    Tr.warning(tc, "CWLJC0014_CLOSE_CACHINGPRVDR_ERR", ((id == null) ? "" : id), e);
                }
            }
        }

        /*
         * Null out any instance fields.
         */
        cachingProvider = null;
        classLoader = null;
        closeSyncObject = null;
    }

    /**
     * Get a unified {@link ClassLoader} that will first search this bundle's
     * {@link ClassLoader}, and then any {@link ClassLoader}s defined by the
     * referenced libraries.
     *
     * @return The unified {@link ClassLoader}.
     */
    @SuppressWarnings("restriction")
    public ClassLoader getUnifiedClassLoader() {
        if (classLoader != null) {
            return classLoader;
        }

        /*
         * Create an array of follow-on classloaders from the libraries.
         */
        int numFollowOns = ((commonLibraries != null) ? commonLibraries.size() : 0) + 1;
        ClassLoader[] followOns = new ClassLoader[numFollowOns];

        /*
         * First add the SpiLibrary ClassLoader for the JCache implementation so it can access the
         * javax.cache classes. Then add each of the common libraries that may contain user classes
         * that may be stored in the cache.
         */
        int idx = 0;
        followOns[idx++] = ((SpiLibrary) jCacheLibrary).getSpiClassLoader(CachingProviderService.class.getName());
        if (commonLibraries != null) {
            for (Library commonLib : commonLibraries) {
                /*
                 * We don't support referencing the same library from the commonLibraryRef and jCacheLibraryRef.
                 */
                if (commonLib.id().equalsIgnoreCase(jCacheLibrary.id())) {
                    String msg = Tr.formatMessage(tc, "CWLJC0006_MULTI_REF_LIB", (this.id == null) ? "" : this.id, commonLib.id());
                    Tr.error(tc, msg);
                    throw new IllegalStateException(msg);
                }
                followOns[idx++] = commonLib.getClassLoader();
            }
        }

        /*
         * Create the unified classloader.
         */
        ClassLoader unifiedClassLoader = classLoadingService.unify(CachingProviderService.class.getClassLoader(),
                                                                   followOns);

        /*
         * Wrap the unified classloader with a dummy/delegating classloader.
         */
        classLoader = new CacheServiceClassLoader(unifiedClassLoader);
        return classLoader;
    }

    /**
     * Get the {@link CachingProvider} for this {@link CachingProviderService}.
     *
     * @return The {@link CachingProvider}.
     */
    public CachingProvider getCachingProvider() {
        return cachingProvider;
    }

    /**
     * Set the {@link Library} for this {@link CachingProviderService}.
     *
     * @param library The {@link Library}.
     */
    @Reference(name = "commonLibrary", cardinality = ReferenceCardinality.MULTIPLE, target = "(id=unbound)")
    public void setCommonLibrary(Library library) {
        commonLibraries.add(library);
        classLoader = null; // Need to reload with new libraries.
    }

    /**
     * Unset the {@link Library} for this {@link CachingProviderService}.
     *
     * @param library The {@link Library}.
     */
    public void unsetCommonLibrary(Library library) {
        commonLibraries.remove(library);
        classLoader = null; // Need to reload with remaining libraries.
    }

    /**
     * Set the {@link Library} for this {@link CachingProviderService}.
     *
     * @param library The {@link Library}.
     */
    @Reference(name = "jCacheLibrary", cardinality = ReferenceCardinality.MANDATORY, target = "(id=unbound)")
    public void setJCacheLibrary(Library library) {
        jCacheLibrary = library;
        classLoader = null; // Need to reload with new libraries.
    }

    /**
     * Unset the {@link Library} for this {@link CachingProviderService}.
     *
     * @param library The {@link Library}.
     */
    public void unsetLibrary(Library library) {
        if (jCacheLibrary != null) {
            jCacheLibrary = null;
            classLoader = null; // Need to reload with remaining libraries.
        }
    }

    /**
     * Set the {@link ClassLoadingService} for this {@link CachingProviderService}.
     *
     * @param classLoadingService The {@link ClassLoadingService}.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setClassLoadingService(ClassLoadingService classLoadingService) {
        this.classLoadingService = classLoadingService;
    }

    /**
     * Unset the {@link ClassLoadingService} for this {@link CachingProviderService}.
     *
     * @param classLoadingService The {@link ClassLoadingService}.
     */
    public void unsetClassLoadingService(ClassLoadingService classLoadingService) {
        this.classLoadingService = null;
    }

    @Override
    public String toString() {
        return super.toString() + "{id=" + id + ", cachingProvider=" + cachingProvider + ", jCacheLibrary=" + jCacheLibrary + ", commonLibraries=" + commonLibraries + "}";
    }

    /**
     * Get the synch object to be used when closing the CachingProvider, CacheManager or the Cache itself.
     *
     * @return The sync object.
     */
    Object getCloseSyncObject() {
        return closeSyncObject;
    }
}
