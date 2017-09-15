/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.classloading;

import java.util.EnumSet;
import java.util.List;

import org.osgi.framework.Version;

/**
 * <p>
 * This interface allows for configuration of the gateway for an application.
 * The caller constructs an instance and calls the setters, these are then
 * used by the ClassLoadingService to construct a gateway class loader instance
 * for the application.
 * </p>
 * <p>
 * It is possible to configure GatewayClassLoaders to use the {@link APIResolverHook} to filter the packages that are available to be
 * loaded. This resolver hook will only be enabled if a call to {@link #setApiTypeVisibility(ApiType...)} or {@link #setApiTypeVisibility(Iterable)} has been made, if no call is
 * made then the gateway class loader will not be configured to filter any of the packages based on their API type and all packages will be available.
 * </p>
 */
public interface GatewayConfiguration {
    /**
     * @param bundleRequirements a list of bundle requirements. A bundle
     *            requirement contains a bundle symbolic name, followed by a set of
     *            attributes and directives which are separated by semicolons.
     */
    GatewayConfiguration setRequireBundle(List<String> bundleRequirements);

    /** @see #setRequireBundle(List) */
    GatewayConfiguration setRequireBundle(String... bundleRequirements);

    /**
     * @param packageImports a list of package import requirements. A
     *            package requirements contains a package name, followed by a set
     *            of attributes and directives which are separated by semi-colons.
     */
    GatewayConfiguration setImportPackage(List<String> packageImports);

    /** @see #setImportPackage(List) */
    GatewayConfiguration setImportPackage(String... packageImports);

    /**
     * @param packageImports a list of dynamic package import requirements. A
     *            package requirements contains a package name, followed by a set
     *            of attributes and directives which are separated by semi-colons. A
     *            dynamic package import is wired on demand, rather than up front.
     */
    GatewayConfiguration setDynamicImportPackage(List<String> packageImports);

    /** @see #setDynamicImportPackage(List) */
    GatewayConfiguration setDynamicImportPackage(String... packageImports);

    /** @param name The name of the application. This should be unique. */
    GatewayConfiguration setApplicationName(String name);

    /** @param version The version of an application. */
    GatewayConfiguration setApplicationVersion(Version version);

    /** @param delegateToSystem true if findSystemClass should be called if the bundle is unable to load the class. */
    GatewayConfiguration setDelegateToSystem(boolean delegateToSystem);

    boolean getDelegateToSystem();

    /**
     * Sets the allowed API types. Even if no <code>types</code> are supplied this will enable the filtering of packages for gateway class loaders constructed from this
     * configuration. Once it is set it cannot be unset.
     * 
     * @param types The type that of APIs that are allowed to be loaded by the gateway class loader
     * @return The GatewayConfiguration with this property set
     */
    GatewayConfiguration setApiTypeVisibility(ApiType... types);

    /**
     * Sets the allowed API types. Even if no <code>types</code> are supplied this will enable the filtering of packages for gateway class loaders constructed from this
     * configuration. Once it is set it cannot be unset.
     * 
     * @param types The type that of APIs that are allowed to be loaded by the gateway class loader
     * @return The GatewayConfiguration with this property set
     */
    GatewayConfiguration setApiTypeVisibility(Iterable<ApiType> types);

    Iterable<String> getRequireBundle();

    Iterable<String> getImportPackage();

    Iterable<String> getDynamicImportPackage();

    /**
     * <p>
     * Returns a set of the API types that are allowed to be seen by a gateway class loader created from this configuration, the options for the set are:
     * </p>
     * <ul>
     * <li><code>null</code>: Do not restrict the packages visible to the class loader</li>
     * <li>Empty: Restrict the packages available to just "internal" packages</li>
     * <li>Non Empty Set: Restrict the packages available to just the types supplied and "internal" packages</li>
     * </ul>
     * 
     * @return The set of allowed API types
     */
    EnumSet<ApiType> getApiTypeVisibility();

    String getApplicationName();

    Version getApplicationVersion();
}