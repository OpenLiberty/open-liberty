/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.classloading;

import java.io.File;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.library.ApplicationExtensionLibrary;
import com.ibm.wsspi.library.Library;

/**
 * The ClassLoadingService is used to construct and manage class loaders. These
 * can then be used to load application, module, or other level artifacts.
 */
public interface ClassLoadingService {
    /**
     * This method creates a top level class loader. The parent of a top level class loader
     * is a gateway into the server's class space. The ClassLoaderConfiguration is not expected
     * to have a parent set for this call.
     * <p>
     * Once the configuration objects are passed in, they belong to the ClassLoadingService.
     * They should not be modified or re-used for other invocations.
     * <p>
     * <strong>N.B. it is the caller's responsibility to ensure that this method is not
     * called concurrently for the same or equivalent {@link ClassLoaderIdentity} objects.
     * The results of concurrent invocation for the same identity are not defined.</strong>
     *
     * @param classPath A list of URLs that should be used to load classes
     * @param gwConfig The desired configuration for the gateway.
     * @param config The desired configuration of the ClassLoader.
     * @return The created class loader.
     */
    ClassLoader createTopLevelClassLoader(List<Container> classPath, GatewayConfiguration gwConfig, ClassLoaderConfiguration config);

    /**
     * This method creates a classloader for use from within a bundle. If a bundle needs to
     * invoke shared libraries specified in config, for example, it can use this method to
     * create a loader for the shared libraries which will also provide access to the classes
     * within the bundle.
     * <p>
     * Once the configuration objects are passed in, they belong to the ClassLoadingService.
     * They should not be modified or re-used for other invocations.
     * <p>
     * <strong>N.B. it is the caller's responsibility to ensure that this method is not
     * called concurrently for the same or equivalent {@link ClassLoaderIdentity} objects.
     * The results of concurrent invocation for the same identity are not defined.</strong>
     *
     * @param classPath A list of URLs that should be used to load classes
     * @param gwClassLoader The gateway ClassLoader.
     * @param config The desired configuration of the ClassLoader.
     * @return The created class loader.
     */
    ClassLoader createBundleAddOnClassLoader(List<File> classPath, ClassLoader gwClassLoader, ClassLoaderConfiguration config);

    /**
     * This method creates a lower level class loader, such as a module class loader. It has a parent
     * which could be a class loader created by a call to createTopLevelClassLoader, or createChildClassLoader.
     * The ClassLoaderConfiguration must have a parent set in this case.
     * <p>
     * Once the configuration objects are passed in, they belong to the ClassLoadingService.
     * They should not be modified or re-used for other invocations.
     * <p>
     * <strong>N.B. it is the caller's responsibility to ensure that this method is not
     * called concurrently for the same or equivalent {@link ClassLoaderIdentity} objects.
     * The results of concurrent invocation for the same identity are not defined.</strong>
     *
     * @param classpath A list of URLs that should be used to load classes
     * @param config The desired configuration of the ClassLoader.
     * @return the created class loader.
     */
    ClassLoader createChildClassLoader(List<Container> classpath, ClassLoaderConfiguration config);

    /**
     * @return A clean gateway configuration
     */
    GatewayConfiguration createGatewayConfiguration();

    /**
     * @return A clean class loader configuration.
     */
    ClassLoaderConfiguration createClassLoaderConfiguration();

    /**
     * This creates an immutable ClassLoaderIdentity. Multiple calls to
     * createIdentity with the same parameters may result in the same object.
     *
     * @param domain a unique name indicating the domain (i.e. the user) of the class loader
     * @param id an id unique within the domain
     * @return a new ClassLoaderIdentity composed of the domain and the id
     */
    ClassLoaderIdentity createIdentity(String domain, String id);

    /**
     * Attempt to create a {@link ClassLoader} that can be used to introspect
     * the classes on the class path of the provided {@link ClassLoader} without
     * loading them directly. The returned {@link ClassLoader} should be
     * discarded as early as possible to allow it and all its classes to be
     * garbage-collected.
     *
     * @param loader the class loader to clone
     * @return a new {@link ClassLoader} or <code>null</code> if <code>loader</code> was not created by this service
     *
     * @see javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()
     */
    ClassLoader getShadowClassLoader(ClassLoader loader);

    /**
     * Attempt to register a {@link ClassTransformer} with a {@link ClassLoader}.
     *
     * @param transformer the {@link ClassTransformer} to be registered
     * @param loader the {@link ClassLoader} to be modified
     * @return <code>true</code> if the operation succeeded, <code>false</code>
     *         if the {@link ClassLoader} was not created by this {@link ClassLoadingService}.
     */
    boolean registerTransformer(ClassTransformer transformer, ClassLoader loader);

    /**
     * Attempt to unregister a {@link ClassTransformer} from a {@link ClassLoader}.
     *
     * @param transformer the {@link ClassTransformer} to be unregistered
     * @param loader the {@link ClassLoader} to be modified
     * @return <code>true</code> if <code>loader</code> was modified,
     *         <code>false</code> if it was not a classloader created by this service
     *         or if <code>transformer</code> was never registered with it.
     */
    boolean unregisterTransformer(ClassTransformer transformer, ClassLoader loader);

    /**
     * This method builds a classloader that delegates to the provided classloaders in order. It
     * adds the parent classloader as the parent so it is consulted first and then consults the
     * follow on ones when a class load fails.
     *
     * @param parent the first classloader to query
     * @param classloaders the class loaders to unify,
     * @return a unified classloader
     */
    ClassLoader unify(ClassLoader parent, ClassLoader... classloaders);

    /**
     * Create or retrieve the shared class loader for a shared library.
     *
     * @param lib the shared library to create a class loader for
     * @return the unique class loader for the provided library
     */
    ClassLoader getSharedLibraryClassLoader(Library lib);

    /**
     * This will augment the application class loader with the ability to see more internal packages. These packages are ones that contain classes that are loaded from the context
     * class loader so are needed to be visible but shouldn't be available through the main application class loader.
     *
     * Note: It is the caller's responsibility to ensure the returned class loader is {@link #destroyThreadContextClassLoader destroyed} when it is no longer needed.,
     * in order to avoid leaking this loader (and all classes it loaded).
     *
     * @param applicationClassLoader The application class loader to augment
     * @return The new class loader that can be set as the thread context class loader
     */
    ClassLoader createThreadContextClassLoader(ClassLoader applicationClassLoader);

    /**
     * This will destroy the thread context class loader. If the thread
     * context class loader contains URLClassLoader, it may hold the jar
     * file lock when the caching is enabled on Windows platform.
     * So we need guarantee the resources could be released here.
     *
     * @param unifiedClassLoader The thread context class loader
     */
    void destroyThreadContextClassLoader(ClassLoader unifiedClassLoader);

    /**
     * This method returns whether or not the provided ClassLoader object
     * is an instance of an AppClassLoader.
     *
     * @param cl The class loader object to analyze.
     * @return true if an instance of AppClassLoader was provided, otherwise false.
     */
    boolean isAppClassLoader(ClassLoader cl);

    /**
     * This method returns whether or not the provided ClassLoader object
     * is an instance of an ThreadContextClassLoader.
     *
     * @param cl The class loader object to analyze.
     * @return true if an instance of ThreadContextClassLoader was provided, otherwise false.
     */
    boolean isThreadContextClassLoader(ClassLoader cl);

    /**
     * @param protectionDomainMap
     */
    void setSharedLibraryProtectionDomains(Map<String, ProtectionDomain> protectionDomainMap);

    /**
     * Returns a list of Libraries that are attached to application classloaders
     * without configuration, for the purposes of providing application extensions
     */
    List<ApplicationExtensionLibrary> getAppExtLibs();
}