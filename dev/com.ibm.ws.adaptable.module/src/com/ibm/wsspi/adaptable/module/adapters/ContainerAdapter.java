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
package com.ibm.wsspi.adaptable.module.adapters;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * Interface for a Container Adapter Service.
 * 
 * @param <T> The type this Service adapts Containers to.
 */
public interface ContainerAdapter<T> {
    /**
     * Adapt from the Adaptable 'containerToAdapt' to type T<p>
     * Note that artifact layer paths may not be equivalent to adaptable paths.<br>
     * Use the passed 'artifactContainer' to know what the artifact layer path is for the containerToAdapt<p>
     * This allows the Adaptable Layer to have a different concept of 'isRoot' hierarchy than the artifact.
     * 
     * @param root the container that returns isRoot=true for containerToAdapt (will be containerToAdapt if containerToAdapt.isRoot=true)
     * @param rootOverlay the artifact layer container that holds the data underpinning this adaptable.
     * @param artifactContainer the artifact container corresponding to the containerToAdapt
     * @param containerToAdapt the adaptable container to be adapted.
     * @return
     */
    T adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException;
}
