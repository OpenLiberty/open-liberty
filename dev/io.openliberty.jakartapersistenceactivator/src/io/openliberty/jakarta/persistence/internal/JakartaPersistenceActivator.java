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
package io.openliberty.jakarta.persistence.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceProviderResolver;
import jakarta.persistence.spi.PersistenceProviderResolverHolder;

/**
 * An override of the default persistence activator that provides a hybrid mechanism
 * to discover persistence providers by searching for providers available from
 * META-INF/services in addition to those registered in the OSGi service registry.
 */
public class JakartaPersistenceActivator implements BundleActivator, PersistenceProviderResolver {
    public static final String PERSISTENCE_PROVIDER = "jakarta.persistence.spi.PersistenceProvider";

    private BundleContext ctx = null;
    private ServiceTracker persistenceProviderTracker = null;

    private final Map<String, PersistenceProvider> providers = new WeakHashMap<String, PersistenceProvider>();

    @Override
    public void start(BundleContext ctx) throws Exception {
        this.ctx = ctx;

        JPAServiceTracker jpaServiceTracker = new JPAServiceTracker();
        persistenceProviderTracker = new ServiceTracker(ctx, PERSISTENCE_PROVIDER, jpaServiceTracker);
        persistenceProviderTracker.open();

        PersistenceProviderResolverHolder.setPersistenceProviderResolver(this);
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
        persistenceProviderTracker.close();
        persistenceProviderTracker = null;

        PersistenceProviderResolverHolder.setPersistenceProviderResolver(null);
    }

    @Override
    public void clearCachedProviders() {
        synchronized (providers) {
            providers.clear();
        }
    }

    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        ArrayList<PersistenceProvider> providerList = new ArrayList<PersistenceProvider>();
        synchronized (providers) {
            providerList.addAll(providers.values());
        }
        providerList.addAll(findProvidersByClassLoader());

        return providerList;
    }

    // TODO: Should implement a secondary map that caches persistence providers bundled with applications
    private List<PersistenceProvider> findProvidersByClassLoader() {
        List<PersistenceProvider> nonOSGiProviders = new ArrayList<PersistenceProvider>();

        // try to get the context classloader first, if that fails, use the loader
        // that loaded this class
        ClassLoader cl = PrivClassLoader.get(null);
        if (cl == null) {
            cl = PrivClassLoader.get(JakartaPersistenceActivator.class);
        }
        try {
            ServiceLoader<PersistenceProvider> providers = ServiceLoader.load(PersistenceProvider.class, cl);

            // load the providers into the provider cache for the context (or current) classloader
            for (Iterator<PersistenceProvider> provider = providers.iterator(); provider.hasNext();) {
                nonOSGiProviders.add(provider.next());
            }
        } catch (Exception e) {
            throw new PersistenceException("Failed to load provider from META-INF/services", e);
        }

        return Collections.unmodifiableList(nonOSGiProviders);
    }

    private class JPAServiceTracker implements ServiceTrackerCustomizer {

        @Override
        public Object addingService(ServiceReference sr) {
            PersistenceProvider provider = null;
            try {
                provider = (PersistenceProvider) ctx.getService(sr);
                String name = (String) sr.getProperty(PERSISTENCE_PROVIDER);
                synchronized (providers) {
                    providers.put(name, provider);
                }
            } catch (Throwable t) {

            }

            return provider;
        }

        @Override
        public void modifiedService(ServiceReference sr, Object obj) {

            try {
                PersistenceProvider provider = (PersistenceProvider) ctx.getService(sr);
                String name = (String) sr.getProperty(PERSISTENCE_PROVIDER);
                synchronized (providers) {
                    providers.remove(name);
                    providers.put(name, provider);
                }
            } catch (Throwable t) {

            }
        }

        @Override
        public void removedService(ServiceReference sr, Object obj) {
            String name = (String) sr.getProperty(PERSISTENCE_PROVIDER);
            synchronized (providers) {
                providers.remove(name);
            }
        }
    }

    /**
     * Utility class to handle privileged classloader access
     */
    private static class PrivClassLoader implements PrivilegedAction<ClassLoader> {
        private final Class<?> c;

        public static ClassLoader get(Class<?> c) {
            PrivClassLoader action = new PrivClassLoader(c);
            if (System.getSecurityManager() != null) {
                return AccessController.doPrivileged(action);
            }
            return action.run();
        }

        private PrivClassLoader(Class<?> c) {
            this.c = c;
        }

        @Override
        public ClassLoader run() {
            if (this.c != null) {
                return this.c.getClassLoader();
            }
            return Thread.currentThread().getContextClassLoader();
        }
    }

}
