/*******************************************************************************
 * Copyright (c) 2011,2023 IBM Corporation and others.
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
package com.ibm.wsspi.library;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.config.Fileset;

/**
 * API for shared libraries.
 * 
 * Shared libraries, within Liberty, are repository of resources which are made available
 * through a class loader.
 * 
 * Resources may be present in many different ways: Through file sets; through a files, which may
 * be directories; through artifact containers. 
 * 
 * <strong>Do not implement this interface.</strong> Liberty class loaders will only work with
 * the Liberty implementations of this interface.
 */
public interface Library {
    /**
     * Answer the unique identifier of this shared library.
     * 
     * @return The unique identifier of this library.
     */
    String id();

    /**
     * Answer the allowed API types of this library.  This defines
     * how applications may access the resources of this library.
     * 
     * @return The allowed API types of this library.
     */
    EnumSet<ApiType> getApiTypeVisibility();

    //

    /**
     * Answer file sets which provide resources to this library.
     * 
     * @return The file sets which provide resources to this library.
     */
    Collection<Fileset> getFilesets();

    /**
     * Answer the file type resources which are directly accessible within
     * this library.
     * 
     * @return The file resources which are directly accessible within
     *     this library.
     */
    Collection<File> getFiles();

    /**
     * Answer the directory type file resources which are directly accessible
     * within this library.  Files beneath the directories are accessible
     * within this library.
     * 
     * @return The directory type file resources which are directly accessible
     *     within this library.
     */
    Collection<File> getFolders();

    /**
     * Answer the artifact containers which provide resources accessible within
     * this library.  The entries of the containers are accessible as resources.
     * Entries of enclosed containers (nested root containers, for example,
     * nested archives) are not accessible as resources.
     * 
     * Although they may exist as discrete files (for example, a JAR file), the
     * artifact containers themselves are not accessible resources.
     * 
     * @return The artifact containers which are accessible within this library.
     */
    Collection<ArtifactContainer> getContainers();

    //
    
    /**
     * Answer the class loader which provides access to the resources of
     * this library.
     * 
     * There should be at most one of these in existence at a time.
     * 
     * @return The class loader of this library.
     */
    ClassLoader getClassLoader();
}
