/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.Util.ensure;
import static com.ibm.ws.classloading.internal.Util.ensureNotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.ClassGenerator;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.classloading.internal.util.CanonicalStore;
import com.ibm.ws.classloading.internal.util.ClassRedefiner;
import com.ibm.ws.classloading.internal.util.Factory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.ClassLoadingConfigurationException;
import com.ibm.wsspi.classloading.GatewayConfiguration;

/**
 * A transient factory object for configuring and creating {@link AppClassLoader} objects. The methods of this class
 * are not thread-safe. It is recommended that instances of this class are not stored in fields or shared between threads.
 */
class ClassLoaderFactory extends GatewayBundleFactory {
    static final TraceComponent tc = Tr.register(ClassLoaderFactory.class);

    interface PostCreateAction {
        void invoke(AppClassLoader acl);
    }

    private final CanonicalStore<ClassLoaderIdentity, AppClassLoader> store;
    private final CompositeResourceProvider resourceProviders;
    private List<Container> classPath;
    private List<File> sharedLibPath;
    private ClassLoaderConfiguration config;
    private GatewayConfiguration gwConfig;
    private ClassLoader parentClassLoader;
    private DeclaredApiAccess access;
    private PostCreateAction postCreateAction;
    private ClassLoader externalBundleLoader;
    private final ClassRedefiner redefiner;
    private final ClassGenerator generator;
    private final GlobalClassloadingConfiguration globalConfig;

    ClassLoaderFactory(BundleContext bundleContext, RegionDigraph digraph, Map<Bundle, Set<GatewayClassLoader>> classloaders,
                       CanonicalStore<ClassLoaderIdentity, AppClassLoader> store,
                       CompositeResourceProvider resourceProviders, ClassRedefiner redefiner, ClassGenerator generator,
                       GlobalClassloadingConfiguration globalConfig) {
        super(bundleContext, digraph, classloaders);
        this.store = store;
        this.resourceProviders = resourceProviders;
        this.redefiner = redefiner;
        this.generator = generator;
        this.globalConfig = globalConfig;
    }

    private <P extends ClassLoader & DeclaredApiAccess> void setParent(P p) {
        this.parentClassLoader = p;
        this.access = p;
    }

    ClassLoaderFactory setClassPath(List<Container> classPath) {
        this.classPath = classPath;
        return this;
    }

    ClassLoaderFactory setSharedLibPath(List<File> classPath) {
        this.sharedLibPath = classPath;
        return this;
    }

    ClassLoaderFactory configure(ClassLoaderConfiguration config) {
        this.config = config;
        return this;
    }

    ClassLoaderFactory configure(GatewayConfiguration gwConfig) {
        this.gwConfig = gwConfig;
        return this;
    }

    ClassLoaderFactory useBundleAddOnLoader(ClassLoader loader) {
        this.externalBundleLoader = loader;
        return this;
    }

    ClassLoaderFactory onCreate(PostCreateAction action) {
        this.postCreateAction = action;
        return this;
    }

    AppClassLoader create() {
        AppClassLoader result = createClassLoader();
        store.store(config.getId(), result);
        return result;
    }

    AppClassLoader getCanonical() {
        // ensure we only invoke createClassLoader once (even from multiple factories)
        // by passing a factory into the canonical store to invoke only if necessary
        AppClassLoader canonical = store.retrieveOrCreate(config.getId(), new Factory<AppClassLoader>() {
            @Override
            public AppClassLoader createInstance() {
                return createClassLoader();
            }
        });
        return canonical;
    }

    private AppClassLoader createClassLoader() {
        validate();
        inferParentLoader();
        AppClassLoader result = config.getDelegateToParentAfterCheckingLocalClasspath()
                        ? new ParentLastClassLoader(parentClassLoader, config, classPath, access, redefiner, generator, globalConfig)
                        : new AppClassLoader(parentClassLoader, config, classPath, access, redefiner, generator, globalConfig);
        addSharedLibPaths(result);
        runPostCreateAction(result);
        return result;
    }

    private void validate() {
        // some of the messages in here are internal WAS error messages => no NLS
        // Don't check for empty classpaths, because that's actually totally fine
        if (this.config == null) {
            throw new ClassLoadingConfigurationException("ClassLoadingConfiguration must not be null");
        }
        if (this.gwConfig == null) {
            // no gateway implies this is a child classloader - the parent id must be in the config
            ensure("Child classloader must have a parent id set in its config", config.getParentId() != null);
        } else {
            // there is a gateway, so there MUST NOT be a parent id:
            ensure("Top-level classloader should not have a parent id set in its config", config.getParentId() == null);
        }
    }

    private void inferParentLoader() {
        if (gwConfig != null) {
            if (externalBundleLoader == null) {
                // DEAL WITH TOP LEVEL CLASSLOADER CASE (parent is gateway bundle)
                // gateway classloader must be re-created from config not re-used
                setParent(createGatewayBundleClassLoader(gwConfig, config, resourceProviders));
            } else {
                // DEAL WITH BUNDLE ADD-ON CLASSLOADER (parent is gateway to external bundle)
                setParent(GatewayClassLoader.createGatewayClassLoader(classloaders, gwConfig, externalBundleLoader, resourceProviders));
            }
        } else if (this.parentClassLoader == null) {
            // DEAL WITH CHILD CLASSLOADER (if parent not already cached)
            ClassLoaderIdentity parentID = this.config.getParentId();
            setParent(ensureNotNull("Could not find parent classloader with id '" + parentID + "'.", store.retrieve(parentID)));
        }
    }

    private void addSharedLibPaths(AppClassLoader result) {
        //used by the bundle addon loader to add shared libs.. there has to be a better way!
        if (this.sharedLibPath != null) {
            for (File f : sharedLibPath) {
                result.addLibraryFile(f);
            }
        }
    }

    private void runPostCreateAction(AppClassLoader result) {
        if (postCreateAction == null)
            return; // nothing to do here

        final String methodName = "runPostCreateAction(): ";
        try {
            postCreateAction.invoke(result);
        } catch (Exception swallowed) {
            // this exception should be auto-ffdc'd
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "Caught exception running post-create action for class loader: " + swallowed);
        }
    }
}
