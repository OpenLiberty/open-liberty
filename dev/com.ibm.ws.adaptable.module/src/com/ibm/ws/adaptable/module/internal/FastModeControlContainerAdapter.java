/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.adaptable.module.internal;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.FastModeControl;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 *
 */
public class FastModeControlContainerAdapter implements ContainerAdapter<FastModeControl> {

    @Override
    public FastModeControl adapt(Container root, OverlayContainer rootOverlay,
                                 ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        return new FastModeControlImpl(rootOverlay);
    }

    private static class FastModeControlImpl implements FastModeControl {

        private final OverlayContainer rootOverlay;

        public FastModeControlImpl(OverlayContainer rootOverlay) {
            this.rootOverlay = rootOverlay;
        }

        @Override
        public void useFastMode() {
            rootOverlay.useFastMode();
        }

        @Override
        public void stopUsingFastMode() {
            rootOverlay.stopUsingFastMode();
        }
    }
}
