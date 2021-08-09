/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.artifact.factory.contributor;

import java.io.File;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;

/**
 * To be implemented by providers of Container/Entry abstractions.
 * <p>
 * Implementations will register services meeting this interface with the handlesType service attribute set.
 * <p>
 * <ul>
 * <li>handlesType - (required, String|String[]) representing the fully qualified class names of types this helper can convert to a {@link Container}.<br>
 * <li>handlesEntries - (optional, String|String[]) if present, the case insensitive list of filename extensions this helper is willing to try to convert from just an {@link Entry}
 * . If absent, Helper will be invoked to convert {@link Entry} without filtering by file extension.
 * </ul>
 */
public interface ArtifactContainerFactoryContributor {

    /**
     * Create a container for the passed Object. <p>
     * This is a root-level scenario, where the Object is not considered enclosed by any other Container.<p>
     * 
     * <em>The container returned <b>MUST</b> return true for {@link ArtifactContainer#isRoot}</em> <br>
     * If a scenario requires non-root Containers to be returned, this will require a new story.
     * 
     * @param workAreaCacheDir the directory to use as a performance cache for this Container.
     * @param p the object to underpin this Container instance.
     * @return Container if it was possible to convert Object into one, or null if not.
     */
    ArtifactContainer createContainer(File workAreaCacheDir, Object o);

    /**
     * Create a container for the passed Object. <p>
     * This is an enclosed scenario, where the Object is considered enclosed by the passed Container.<p>
     * 
     * <em>The container returned <b>MUST</b> return true for {@link ArtifactContainer#isRoot}</em> <br>
     * If a scenario requires non-root Containers to be returned, this will require a new story.
     * 
     * @param workAreaCacheDir the directory to use as a performance cache for this Container.
     * @param parent the Container that o is considered to be part of.
     * @param entry the Entry within parent that o is considered to represent.
     * @param o the object to underpin this Container instance.
     * @return Container if it was possible to convert Object into one, or null if not.
     */
    ArtifactContainer createContainer(File workAreaCacheDir, ArtifactContainer parent, ArtifactEntry entry, Object o);
}
