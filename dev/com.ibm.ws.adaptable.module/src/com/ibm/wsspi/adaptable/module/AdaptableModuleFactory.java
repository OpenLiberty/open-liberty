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
package com.ibm.wsspi.adaptable.module;

import java.io.File;

import com.ibm.wsspi.artifact.ArtifactContainer;

/**
 * Factory to create Adaptable Modules.
 */
public interface AdaptableModuleFactory {

    /**
     * Obtain an adaptable container for a given artifact api container.
     * 
     * @param overlayDirectory directory holding overlay content.
     * @param cacheDirForOverlayContent directory overlay can use as a cache location.
     * @param container the underlying artifact container for this module.
     * @return Adaptable Container.
     */
    Container getContainer(File overlayDirectory, File cacheDirForOverlayContent, ArtifactContainer container);
}
