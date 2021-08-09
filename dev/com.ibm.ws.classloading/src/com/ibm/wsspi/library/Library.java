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
package com.ibm.wsspi.library;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.config.Fileset;

/**
 * A library, configured in server.xml, may contain folders, files (i.e. JARs and native libraries), and filesets.
 * <p>
 * <strong>Do not implement this interface.</strong> Liberty class loaders will only work with the Liberty implementations of this interface.
 */
public interface Library {

    /**
     * The unique identifier for this shared library.
     */
    String id();

    /**
     * This method returns the {@link java.util.Collection} of Filesets
     * 
     * @return a list of contained Filesets
     */
    Collection<Fileset> getFilesets();

    /**
     * Get the single classloader for this shared library.
     * There should be at most one of these in existence at any one time.
     */
    ClassLoader getClassLoader();

    /**
     * Get the allowed API types for this shared library.
     */
    EnumSet<ApiType> getApiTypeVisibility();

    /**
     * This method returns the {@link java.util.Collection} of Files
     * 
     * @return a list of contained Files
     */
    Collection<File> getFiles();

    /**
     * This method returns the {@link java.util.Collection} of Folders
     * 
     * @return a list of contained Folders
     */
    Collection<File> getFolders();

    /**
     * This method returns all the artifact containers from this shared library
     * 
     * @return a collection of contained artifact containers
     */
    Collection<ArtifactContainer> getContainers();

}
