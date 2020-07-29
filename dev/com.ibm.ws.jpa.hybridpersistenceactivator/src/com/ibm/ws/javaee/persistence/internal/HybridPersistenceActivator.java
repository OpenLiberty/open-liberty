/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.persistence.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;

import org.apache.geronimo.osgi.locator.ProviderLocator;
import org.apache.geronimo.specs.jpa.PersistenceActivator;

import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * An override of the default persistence activator that provides a hybrid mechanism
 * to discover persistence providers by searching for providers available from
 * META-INF/services in addition to those registered in the OSGi service registry.
 */
public class HybridPersistenceActivator extends PersistenceActivator {
    private volatile WeakHashMap<ModuleMetaData, List<PersistenceProvider>> providerCache = new WeakHashMap<ModuleMetaData, List<PersistenceProvider>>();

    /**
     * This method returns a combination of those persistence providers available from
     * the application classloader in addition to those in the OSGi service registry.
     * OSGi providers are not cached and should not be cached because bundles can
     * be moved in and out of the system and it is the job of the the service tracker
     * to maintain them.
     */
    @Override
    public List<PersistenceProvider> getPersistenceProviders() {
        List<PersistenceProvider> nonOSGiProviders = null;
        boolean findProviders = false;

        try {
            // TODO: This should be wrapped in a try-catch block, and should consider null return values.
            final ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();

            // Query the provider cache per-ModuleMetaData
            nonOSGiProviders = providerCache.get(mmd);
            if (nonOSGiProviders == null) {
                nonOSGiProviders = new ArrayList<PersistenceProvider>();
                providerCache.put(mmd, nonOSGiProviders);
                findProviders = true;
            }
        } catch (Throwable t) {
            nonOSGiProviders = new ArrayList<PersistenceProvider>();
            findProviders = true;
        }

        // Get all providers not registered in OSGi.  These will be any third-party providers
        if (findProviders) {
            // try to get the context classloader first, if that fails, use the loader
            // that loaded this class
            ClassLoader cl = PrivClassLoader.get(null);
            if (cl == null) {
                cl = PrivClassLoader.get(HybridPersistenceActivator.class);
            }
            try {
                // load the providers into the provider cache for the context (or current) classloader
                List<Object> providers = ProviderLocator.getServices(PersistenceProvider.class.getName(), getClass(), cl);
                for (Iterator<Object> provider = providers.iterator(); provider.hasNext();) {
                    Object o = provider.next();
                    if (o instanceof PersistenceProvider) {
                        nonOSGiProviders.add((PersistenceProvider) o);
                    }
                }
            } catch (Exception e) {
                throw new PersistenceException("Failed to load provider from META-INF/services", e);
            }
        }

        List<PersistenceProvider> combinedProviders = new ArrayList<PersistenceProvider>(nonOSGiProviders);
        combinedProviders.addAll(super.getPersistenceProviders());
        return combinedProviders;
    }

    /**
     * Clears cached providers
     */
    @Override
    public void clearCachedProviders() {
        this.providerCache.clear();
        super.clearCachedProviders();
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
