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
package com.ibm.wsspi.artifact.overlay;

import com.ibm.wsspi.artifact.ArtifactContainer;

/**
 * Factory for obtaining OverlayContainers.
 */
public interface OverlayContainerFactory {
    /**
     * Create an overlay, for the requested overlayType.
     * 
     * @param <T> The type that will be returned
     * @param overlayType instance of the class of the type requested.
     * @param b the container to base the overlay over.
     * @return Instance of T, or null if unable to handle request.
     */
    <T extends OverlayContainer> T createOverlay(Class<T> overlayType, ArtifactContainer b);
}
