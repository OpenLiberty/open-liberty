/*******************************************************************************
 * Copyright (c) 2012,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.bundle.adapters;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * Simple container to overlay container adapter.  Simply return
 * the overlay container parameter value as the adapted value.
 */
public class ContainerAdapterOverlay implements ContainerAdapter<OverlayContainer> {
    @Override
    public OverlayContainer adapt(
        Container rootContainer,
        OverlayContainer rootOverlayContainer,
        ArtifactContainer artifactContainer,
        Container adaptedContainer) {

        return rootOverlayContainer;
    }
}
