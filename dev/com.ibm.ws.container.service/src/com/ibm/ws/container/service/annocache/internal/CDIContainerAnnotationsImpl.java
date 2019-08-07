/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache.internal;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

import com.ibm.wsspi.adaptable.module.Container;

import com.ibm.ws.container.service.annocache.CDIContainerAnnotations;

public class CDIContainerAnnotationsImpl extends ContainerAnnotationsImpl implements CDIContainerAnnotations {
    // private final String CLASS_NAME = "CDIContainerAnnotationsImpl";

    /** Predefined module category for CDI annotations results. */
    public static final String CDI_CATEGORY_NAME = "cdi";

    public CDIContainerAnnotationsImpl(
        AnnotationsAdapterImpl annotationsAdapter,
        Container rootContainer, OverlayContainer rootOverlayContainer,
        ArtifactContainer rootArtifactContainer, Container rootAdaptableContainer,
        String appName, boolean isUnnamedMod, String modName) {

        super(annotationsAdapter,
              rootContainer, rootOverlayContainer,
              rootArtifactContainer, rootAdaptableContainer,
              appName, isUnnamedMod, modName, CDI_CATEGORY_NAME);
    }
}

