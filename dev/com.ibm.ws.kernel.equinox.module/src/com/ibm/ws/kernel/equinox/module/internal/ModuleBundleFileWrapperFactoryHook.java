/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.equinox.module.internal;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.hookregistry.ActivatorHookFactory;
import org.eclipse.osgi.internal.hookregistry.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.wsspi.kernel.equinox.module.ModuleBundleFileFactory;
import com.ibm.wsspi.kernel.equinox.module.ModuleDelegateClassLoaderFactory;
import com.ibm.wsspi.kernel.equinox.module.ModuleInfo;
import com.ibm.wsspi.kernel.equinox.module.ModuleLocationUtils;

/**
 * An Equinox adaptor that implements the necessary hooks to plugin
 * ModuleBundleFileFactory implementations into the equinox framework.
 */
public class ModuleBundleFileWrapperFactoryHook implements BundleFileWrapperFactoryHook, HookConfigurator, ActivatorHookFactory {

    // used to track ModuleBundleFileFactory service implementations
    volatile ServiceTracker<ModuleBundleFileFactory, ModuleBundleFileFactory> bundleFileFactoryTracker;
    // used to track ModuleDelegateClassLoaderFactory service implementations
    volatile ServiceTracker<ModuleDelegateClassLoaderFactory, ModuleDelegateClassLoaderFactory> delegateLoaderFactoryTracker;

    @Override
    public void addHooks(HookRegistry hookRegistry) {
        // register this as a bundle file wrapper factory hook
        hookRegistry.addBundleFileWrapperFactoryHook(this);
        // need to be an adaptor hook to get access to a BundleContext of the system bundle.
        hookRegistry.addActivatorHookFactory(this);
        // register a ClassLoaderHook so we can create our own delegating bundle class loaders when needed.
        hookRegistry.addClassLoaderHook(new ClassLoaderHook() {

            /** {@inheritDoc} */
            @Override
            public ModuleClassLoader createClassLoader(ClassLoader parent, EquinoxConfiguration configuration, BundleLoader delegate, Generation generation) {
                // find a delegate for the specified bundle generation
                ClassLoader delegateClassLoader = getDelegateClassLoader(generation);
                if (delegateClassLoader == null) {
                    // none found; return null for default framework behavior
                    return null;
                }
                // this is a bundle that we want to create a delegating class loader for
                return new ModuleDelegateClassLoader(parent, configuration, delegate, generation, delegateClassLoader);
            }

            private ClassLoader getDelegateClassLoader(Generation generation) {
                // check the service registry for a factory
                ServiceTracker<ModuleDelegateClassLoaderFactory, ModuleDelegateClassLoaderFactory> current = delegateLoaderFactoryTracker;
                if (current == null) {
                    // none available return
                    return null;
                }
                ModuleDelegateClassLoaderFactory factory = current.getService();
                if (factory == null) {
                    // no service returned; just return null
                    return null;
                }
                // ask the factory for a delegating class loader
                return factory.getDelegateClassLoader(generation.getRevision().getBundle());
            }
        });
    }

    @Override
    public BundleFileWrapper wrapBundleFile(final BundleFile bundleFile, final Generation generation, final boolean base) {
        ServiceTracker<ModuleBundleFileFactory, ModuleBundleFileFactory> currentTracker = bundleFileFactoryTracker;
        // currently only the highest ranked ModuleBundleFileFactory is used
        // TODO may want to ask multiple if they exist, but for now we assume this is a singleton service
        ModuleBundleFileFactory factory = currentTracker == null ? null : currentTracker.getService();
        if (factory == null) {
            return null;
        }

        String location = null;

        /*
         * obtain location from the bundleFile..
         *
         * We cannot use generation.getBundleInfo().getLocation() for base bundles because the
         * returned location varies depending on the framework the target bundle is being
         * installed to - e.g., it's different for OSGi Application bundles in the shared bundle space.
         * Critically this can lead to an inability to associate a given container instance with the
         * returned location.
         */
        if (base) {
            location = ModuleLocationUtils.getLocationFromBundleFile(bundleFile);
            if (location == null) {
                return null;
            }
        } else {
            //getLocation added by equinox to support this scenario =)
            location = generation.getBundleInfo().getLocation();
        }

        final String floc = location;
        // create a ModuleInfo based on the content we are wrapping
        return factory.createBundleFile(new ModuleInfo() {
            @Override
            public boolean isBundleRoot() {
                return base;
            }

            @Override
            public BundleFile getBundleFile() {
                return bundleFile;
            }

            @Override
            public String getLocation() {
                return floc;
            }
        });
    }

    private void frameworkStart(BundleContext context) throws BundleException {
        // initialize the factory service tracker
        bundleFileFactoryTracker = new ServiceTracker<ModuleBundleFileFactory, ModuleBundleFileFactory>(context, ModuleBundleFileFactory.class, null);
        bundleFileFactoryTracker.open();

        delegateLoaderFactoryTracker = new ServiceTracker<ModuleDelegateClassLoaderFactory, ModuleDelegateClassLoaderFactory>(context, ModuleDelegateClassLoaderFactory.class, null);
        delegateLoaderFactoryTracker.open();
    }

    private void frameworkStop(BundleContext context) throws BundleException {
        // close down the factory trackers
        bundleFileFactoryTracker.close();
        delegateLoaderFactoryTracker.close();
    }

    /** {@inheritDoc} */
    @Override
    public BundleActivator createActivator() {
        return new BundleActivator() {
            @Override
            public void stop(BundleContext context) throws Exception {
                frameworkStop(context);
            }

            @Override
            public void start(BundleContext context) throws Exception {
                frameworkStart(context);
            }
        };
    }

}
