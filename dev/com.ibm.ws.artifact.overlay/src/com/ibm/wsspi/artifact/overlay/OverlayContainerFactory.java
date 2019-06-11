/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.artifact.overlay;

import com.ibm.wsspi.artifact.ArtifactContainer;

/**
 * Factory type for overlay containers.
 * 
 * Only root-of-root overlay containers are created.
 *
 * Used by:
 *
 * <code>
 * com.ibm.ws.adaptable.module.internal.AdaptableModuleFactoryImpl.getContainer(File, File, ArtifactContainer)
 * </code>
 *
 * An adaptable module has an artifact container which is converted
 * into an overlay container.
 * 
 * When creating an overlay container, the base container need not be a root-of-roots
 * container.  However, the overlay container has no access to the enclosing container
 * of the base container.
 */
public interface OverlayContainerFactory {
    /**
     * Create an overlay container of a requested type.  The
     * overlay is a root-of-roots overlay container.
     *
     * @param <T> The type of overlay container to create.
     * @param overlayType The class of the container type.
     * @param baseContainer The base container on which to create the overlay container.
     *
     * @return An overlay container of the specified type.  Null if a container
     *     could not be created.
     */
    <T extends OverlayContainer> T createOverlay(Class<T> overlayType, ArtifactContainer baseContainer);
}
