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
 */
public class OverlayContainerFactoryImpl implements OverlayContainerFactory, ContainerFactoryHolder {

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OverlayContainer> T createOverlay(
    	Class<T> overlayType,
    	ArtifactContainer baseContainer) {

        // Don't use OSGI to find the overlay implementation.  That is not required for now.

    	// Only support the specific type OverlayContainer.
    	//
    	// Only support a directory based implementation.  The previously supported
    	// in-memory implementation was retired.

        if ( overlayType.equals(OverlayContainer.class) ) {
            return (T) new DirectoryBasedOverlayContainerImpl(baseContainer, this);

        } else {
        	return null;
        }
    }

    private ArtifactContainerFactory containerFactory;

    protected synchronized void activate(ComponentContext ctx) {
    	// Empty
    }

    protected synchronized void deactivate(ComponentContext ctx) {
        this.containerFactory = null;
    }

    protected synchronized void setContainerFactory(ArtifactContainerFactory cf) {
        this.containerFactory = cf;
    }

    protected synchronized void unsetContainerFactory(ArtifactContainerFactory cf) {
        if ( this.containerFactory == cf ) {
            this.containerFactory = null;
        }
    }

    @Override
    public synchronized ArtifactContainerFactory getContainerFactory() {
        if ( containerFactory == null ) {
            throw new IllegalStateException();
        }
        return containerFactory;
    }
}
