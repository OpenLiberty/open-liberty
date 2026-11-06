/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.Util.ensure;
import static com.ibm.ws.classloading.internal.Util.ensureNotNull;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
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
    private final List<ClassFileTransformer> systemTransformers;

    ClassLoaderFactory(BundleContext bundleContext, RegionDigraph digraph, Map<Bundle, Set<GatewayClassLoader>> classloaders,
                       CanonicalStore<ClassLoaderIdentity, AppClassLoader> store,
                       CompositeResourceProvider resourceProviders, ClassRedefiner redefiner, ClassGenerator generator,
                       GlobalClassloadingConfiguration globalConfig, List<ClassFileTransformer> systemTransformers) {
        super(bundleContext, digraph, classloaders, globalConfig.jvmPackages());
        this.store = store;
        this.resourceProviders = resourceProviders;
        this.redefiner = redefiner;
        this.generator = generator;
        this.globalConfig = globalConfig;
        this.systemTransformers = systemTransformers;
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
                        ? new ParentLastClassLoader(parentClassLoader, config, classPath, access, redefiner, generator, globalConfig, systemTransformers)
                        : new AppClassLoader(parentClassLoader, config, classPath, access, redefiner, generator, globalConfig, systemTransformers);
        addSharedLibPaths(result);
        runPostCreateAction(result);
        registerOwnerAppNames(result);
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
                setParent(GatewayClassLoader.createGatewayClassLoader(classloaders, gwConfig, externalBundleLoader, resourceProviders, jvmPackages));
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

    /**
     * Registers the owning app name on the GCL of every gateway that this ACL
     * depends on — its own GCL (for top-level app modules) and the GCL of each
     * common-library delegate (for the reverse-link case).
     *
     * <p>Only top-level app-domain ACLs carry a meaningful app name.
     * Library, thread-context, and bundle-add-on ACLs are skipped. Note: Shared libraries
     * used exclusively by BELL services (without an application reference) do not have an owning
     * application to recycle and are not registered here.</p>
     */
    private void registerOwnerAppNames(AppClassLoader result) {
        String domain = result.getKey().getDomain();
        if (!isTopLevelAppDomain(domain) && !"EARApplication".equals(domain)) {
            return; // library / thread-context / bundle-add-on — nothing to register
        }
        String appName = "EARApplication".equals(domain)
                        ? result.getKey().getId()
                        : extractAppName(result.getKey().getId());

        // Register on the ACL's own GCL (parentClassLoader was set by inferParentLoader).
        // Normal path: parentClassLoader is a GatewayClassLoader directly.
        // GSL path: parentClassLoader is the GSL AppClassLoader (app -> GSL ACL -> GCL);
        // walk up the AppClassLoader parent chain to reach the GCL.
        ClassLoader ownerGateway = parentClassLoader;
        while (ownerGateway instanceof AppClassLoader) {
            ownerGateway = ownerGateway.getParent();
        }
        if (ownerGateway instanceof GatewayClassLoader) {
            ((GatewayClassLoader) ownerGateway).addOwnerAppName(appName);
        }

        // Register on each common-library delegate's GCL.
        for (LibertyLoader delegate : result.getBeforeAppDelegateLoaders()) {
            registerOnDelegateGcl(delegate, appName);
        }
        for (LibertyLoader delegate : result.getAfterAppDelegateLoaders()) {
            registerOnDelegateGcl(delegate, appName);
        }
    }

    private static void registerOnDelegateGcl(LibertyLoader delegate, String appName) {
        if (!(delegate instanceof AppClassLoader)) {
            return;
        }
        // The delegate ACL's Java parent is its GCL.
        ClassLoader parent = ((AppClassLoader) delegate).getParent();
        if (parent instanceof GatewayClassLoader) {
            ((GatewayClassLoader) parent).addOwnerAppName(appName);
        }
    }

    private static boolean isTopLevelAppDomain(String domain) {
        return ClassLoadingServiceImpl.isTopLevelAppDomain(domain);
    }

    private static String extractAppName(String id) {
        return ClassLoadingServiceImpl.extractAppName(id);
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
