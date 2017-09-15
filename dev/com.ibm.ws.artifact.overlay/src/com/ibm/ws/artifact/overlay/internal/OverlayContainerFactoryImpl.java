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
package com.ibm.ws.artifact.overlay.internal;

import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;

/**
 * Default factory implementation, expandable by new overlay types as needed..
 * <p>
 */
public class OverlayContainerFactoryImpl implements OverlayContainerFactory, ContainerFactoryHolder {

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends OverlayContainer> T createOverlay(Class<T> overlayType, ArtifactContainer b) {

        //We don't use osgi to find overlay impls, as that's not required for now.
        //Instead we use a quick if/else block to handle the request.
        if (overlayType.equals(OverlayContainer.class)) {
            //we now only support the DirBasedOverlay, the in-memory one has been retired.
            //the naming and interfaces have been fixed up so that OverlayContainer now offers
            //the ability of the DirectoryBased one.
            return (T) new DirectoryBasedOverlayContainerImpl(b, this);
        }

        return null;
    }

    private ArtifactContainerFactory containerFactory = null;

    protected synchronized void activate(ComponentContext ctx) {}

    protected synchronized void deactivate(ComponentContext ctx) {
        this.containerFactory = null;
    }

    protected synchronized void setContainerFactory(ArtifactContainerFactory cf) {
        this.containerFactory = cf;
    }

    protected synchronized void unsetContainerFactory(ArtifactContainerFactory cf) {
        if (this.containerFactory == cf) {
            this.containerFactory = null;
        }
    }

    @Override
    public synchronized ArtifactContainerFactory getContainerFactory() {
        if (containerFactory == null) {
            throw new IllegalStateException();
        }
        return containerFactory;
    }

}
