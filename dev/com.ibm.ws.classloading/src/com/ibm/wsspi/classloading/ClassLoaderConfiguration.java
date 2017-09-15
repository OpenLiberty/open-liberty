/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.classloading;

import java.security.ProtectionDomain;
import java.util.List;

import com.ibm.wsspi.adaptable.module.Container;

/**
 * This interface defines the configuration of a classloader.
 */
public interface ClassLoaderConfiguration {
    /**
     * @param delegateLast true indicates that the parent classloader should be
     *            consulted after the local class space. This is not the default behaviour for
     *            normal class loading. false indicates normal class loading semantics should be used.
     */
    ClassLoaderConfiguration setDelegateToParentAfterCheckingLocalClasspath(boolean delegateLast);

    /**
     * @param id The identity for this classloader
     */
    ClassLoaderConfiguration setId(ClassLoaderIdentity id);

    /**
     * @param id The identity of the parent to this classloader, if a parent is required.
     */
    ClassLoaderConfiguration setParentId(ClassLoaderIdentity id);

    /** @param libs the names of shared libraries that should be associated with this classloader */
    ClassLoaderConfiguration setSharedLibraries(List<String> libs);

    /** @see #setSharedLibraries(List) */
    ClassLoaderConfiguration setSharedLibraries(String... libs);

    List<String> getSharedLibraries();

    /** @param libs the names of common shared libraries that should be associated with this classloader */
    ClassLoaderConfiguration setCommonLibraries(List<String> libs);

    /** @see #setCommonLibraries(List) */
    ClassLoaderConfiguration setCommonLibraries(String... libs);

    List<String> getCommonLibraries();

    ClassLoaderConfiguration setClassProviders(List<String> providers);

    ClassLoaderConfiguration setClassProviders(String... providers);

    List<String> getClassProviders();

    ClassLoaderConfiguration setNativeLibraryContainers(List<Container> containers);

    ClassLoaderConfiguration setNativeLibraryContainers(Container... containers);

    List<Container> getNativeLibraryContainers();

    ClassLoaderIdentity getParentId();

    ClassLoaderIdentity getId();

    boolean getDelegateToParentAfterCheckingLocalClasspath();

    ClassLoaderConfiguration setProtectionDomain(ProtectionDomain domain);

    ProtectionDomain getProtectionDomain();
}
