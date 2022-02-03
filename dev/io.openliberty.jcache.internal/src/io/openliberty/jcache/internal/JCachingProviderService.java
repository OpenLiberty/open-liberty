/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.library.Library;

/**
 * Service that configures a {@link CachingProvider} to use with JCache caching.
 */
@Component(service = JCachingProviderService.class, immediate = true, configurationPolicy = REQUIRE, configurationPid = "io.openliberty.jcache.cachingprovider",
           property = { "service.vendor=IBM" })
public class JCachingProviderService {
    private static final TraceComponent tc = Tr.register(JCachingProviderService.class);

    private static final String KEY_PROVIDER_CLASS = "providerClass";
    private static final String KEY_ID = "id";

    private CachingProvider cachingProvider = null;
    private Set<Library> libraries;
    private ClassLoader classLoader = null;
    private String cachingProviderClass = null;
    private String id = null;

    private ClassLoadingService classLoadingService = null;

    // Flag tells us if the message for a call to a beta method has been issued
    private static boolean issuedBetaMessage = false;

    /**
     * Activate this OSGi component.
     *
     * @param configProps The configuration to use when activating this component.
     * @throws Exception If the component could not activate.
     */
    @Activate
    public void activate(Map<String, Object> configProps) throws Exception {
        /*
         * Don't run if not in beta.
         */
        betaFenceCheck();

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
            if (cachingProviderClass != null && !cachingProviderClass.trim().isEmpty()) {
                cachingProvider = Caching.getCachingProvider(cachingProviderClass, getUnifiedClassLoader());
            } else {
                cachingProvider = Caching.getCachingProvider(getUnifiedClassLoader());
            }
        } catch (Throwable e) {
            Tr.error(tc, "CWLJC0004_GET_PROVIDER_FAILED", id, e);
            throw e;
        }
    }

    @Deactivate
    public void deactivate() {
        cachingProvider = null;
        classLoader = null;
    }

    /**
     * Prevent beta functionality from being used when not running the beta edition.
     *
     * @throws UnsupportedOperationException if we are not running the beta edition.
     */
    private void betaFenceCheck() throws UnsupportedOperationException {
        /*
         * Not running beta edition, throw exception
         */
        if (!ProductInfo.getBetaEdition()) {
            throw new UnsupportedOperationException("The jCachingProvider feature is beta and is not available.");
        } else {
            /*
             * Running beta exception, issue message if we haven't already issued one for
             * this class
             */
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class " + this.getClass().getName()
                            + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
        }
    }

    /**
     * Get a unified {@link ClassLoader} that will first search this bundle's
     * {@link ClassLoader}, and then any {@link ClassLoader}s defined by the
     * referenced libraries.
     *
     * @return The unified {@link ClassLoader}.
     */
    public ClassLoader getUnifiedClassLoader() {
        if (classLoader != null) {
            return classLoader;
        }

        /*
         * Create an array of follow-on classloaders from the libraries.
         */
        ClassLoader[] followOns = new ClassLoader[libraries.size()];
        int idx = 0;
        for (Library lib : libraries) {
            followOns[idx++] = lib.getClassLoader();
        }

        /*
         * Create the unified classloader.
         */
        ClassLoader unifiedClassLoader = classLoadingService.unify(JCachingProviderService.class.getClassLoader(),
                                                                   followOns);

        /*
         * Wrap the unified classloader with a dummy/delegating classloader.
         */
        classLoader = new JCacheServiceClassLoader(unifiedClassLoader);
        return classLoader;
    }

    /**
     * Get the {@link CachingProvider} for this {@link JCachingProviderService}.
     *
     * @return The {@link CachingProvider}.
     */
    public CachingProvider getCachingProvider() {
        return cachingProvider;
    }

    /**
     * Set the {@link Library} for this {@link JCachingProviderService}.
     *
     * @param library The {@link Library}.
     */
    @Reference(name = "library", cardinality = ReferenceCardinality.AT_LEAST_ONE, target = "(id=unbound)")
    public void setLibrary(Library library) {
        if (libraries == null) {
            libraries = new HashSet<Library>();
        }
        libraries.add(library);
        classLoader = null; // Need to reload with new libraries.
    }

    /**
     * Unset the {@link Library} for this {@link JCachingProviderService}.
     *
     * @param library The {@link Library}.
     */
    public void unsetLibrary(Library library) {
        if (libraries != null) {
            libraries.remove(library);

            classLoader = null; // Need to reload with remaining libraries.
        }
    }

    /**
     * Set the {@link ClassLoadingService} for this {@link JCachingProviderService}.
     *
     * @param classLoadingService The {@link ClassLoadingService}.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setClassLoadingService(ClassLoadingService classLoadingService) {
        this.classLoadingService = classLoadingService;
    }

    /**
     * Unset the {@link ClassLoadingService} for this {@link JCachingProviderService}.
     *
     * @param classLoadingService The {@link ClassLoadingService}.
     */
    public void unsetClassLoadingService(ClassLoadingService classLoadingService) {
        this.classLoadingService = null;
    }

    @Override
    public String toString() {
        return super.toString() + "{id=" + id + ", cachingProvider=" + cachingProvider + ", libraries=" + libraries + "}";
    }
}
