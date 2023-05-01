/*******************************************************************************
 * Copyright (c) 2011,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.adaptable.module.internal;

import java.io.File;

import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.adapters.AdapterFactoryService;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;

//@formatter:off
public class AdaptableModuleFactoryImpl implements AdaptableModuleFactory, FactoryHolder {
    // Component lifecycle ...

    protected void activate(ComponentContext ctx) {
        //no op
    }

    protected void deactivate(ComponentContext ctx) {
        containerFactory = null;
        overlayContainerFactory = null;
        adapterFactoryService = null;
    }

    // Component services ...

    /**
     * The root, delegating, container factory.
     *
     * This is no longer in use.  This factory uses instead the
     * overlay container and the adapter factory.
     */
    private volatile ArtifactContainerFactory containerFactory;

    protected void setContainerFactory(ArtifactContainerFactory cf) {
        containerFactory = cf;
    }

    protected void unsetContainerFactory(ArtifactContainerFactory cf) {
        if ( containerFactory == cf ) {
            containerFactory = null;
        }
    }

    @Override
    public ArtifactContainerFactory getContainerFactory() {
        if ( containerFactory != null ) {
            return containerFactory;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Adapter factory.  This is not in use. Adaptable containers
     * are created directly as {@link AdaptableContainer} instances.
     */
    private volatile AdapterFactoryService adapterFactoryService;

    protected void setAdapterFactoryService(AdapterFactoryService afs) {
        adapterFactoryService = afs;
    }

    protected void unsetAdapterFactoryService(AdapterFactoryService afs) {
        if ( adapterFactoryService == afs ) {
            adapterFactoryService = null;
        }
    }

    @Override
    public AdapterFactoryService getAdapterFactoryService() {
        if ( adapterFactoryService != null ) {
            return adapterFactoryService;
        } else {
            throw new IllegalStateException();
        }
    }

    //

    /**
     * The factory for overlay containers.  This retains a very small
     * portion of component services.  However, only a single overlay
     * factory implementation is available, and that factory always
     * creates directory overlay containers.
     */
    private volatile OverlayContainerFactory overlayContainerFactory;

    protected void setOverlayContainerFactory(OverlayContainerFactory ocf) {
        overlayContainerFactory = ocf;
    }

    protected void unsetOverlayContainerFactory(OverlayContainerFactory ocf) {
        if ( overlayContainerFactory == ocf ) {
            overlayContainerFactory = null;
        }
    }

    @Override
    public OverlayContainerFactory getOverlayContainerFactory() {
        if ( overlayContainerFactory != null ) {
            return overlayContainerFactory;
        } else {
            throw new IllegalStateException();
        }
    }

    //

    /**
     * Main API: Create an overlay container for a specified artifact container.
     *
     * @param overlayDir The overlay directory.
     * @param overlayCacheDir The cache directory for the overlay.
     * @param artifactContainer The base artifact container of the overlay.
     *
     * @return A new overlay container.
     */
    @Override
    public Container getContainer(File overlayDir, File overlayCacheDir,
                                  ArtifactContainer artifactContainer) {
        OverlayContainer overlayContainer =
            getOverlayContainerFactory().createOverlay(OverlayContainer.class, artifactContainer);
        if ( overlayContainer == null ) {
            return null;
        }

        overlayContainer.setOverlayDirectory(overlayCacheDir, overlayDir);
        return new AdaptableContainerImpl(overlayContainer, this);
    }
}
//@formatter:on