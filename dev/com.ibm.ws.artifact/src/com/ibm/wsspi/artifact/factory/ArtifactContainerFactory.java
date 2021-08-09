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
package com.ibm.wsspi.artifact.factory;

import java.io.File;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;

/**
 * Factory for artifact API.<p>
 * Uses service based implementations to return appropriate Container abstraction for given Objects.
 * <p>
 * Have DS inject the service implementing this interface to your bundle, and use the getters to
 * have the api build a container for a given Object.
 */
public interface ArtifactContainerFactory {
    /**
     * Obtain a container for the passed Object. <p>
     * This is a root-level scenario, where the Object is not considered enclosed by any other Container.
     * 
     * @param workAreaCacheDir the directory to use as a performance cache for this Container.
     * @param o the object to underpin this Container instance.
     * @return Container if it was possible to obtain one representing Object, or null if not.
     */
    ArtifactContainer getContainer(File workAreaCacheDir, Object o);

    /**
     * Obtain a container for the passed Object. <p>
     * This is an enclosed scenario, where the Object is considered enclosed by the passed Container.
     * 
     * @param workAreaCacheDir the directory to use as a performance cache for this Container.
     * @param parent the Container that o is considered to be part of.
     * @param entry the Entry within parent that o is considered to represent.
     * @param o the object to underpin this Container instance.
     * @return Container if it was possible to obtain one representing Object, or null if not.
     */
    ArtifactContainer getContainer(File workAreaCacheDir, ArtifactContainer parent, ArtifactEntry entry, Object o);
}
