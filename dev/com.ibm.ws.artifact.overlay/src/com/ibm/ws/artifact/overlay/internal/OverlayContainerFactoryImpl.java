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
package com.ibm.ws.artifact.overlay.internal;

import java.io.PrintWriter;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.artifact.overlay.util.internal.TimeUtil;
import com.ibm.ws.artifact.overlay.util.internal.TimedWeakIdentityMap;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;

/**
 * Factory for overlay containers.
 * 
 * This factory is used to create root-of-root overlay containers.
 * 
 * A reference is kept to the top level, delegating, container factory,
 * but, this delegate factory never uses the top level factory to create
 * nested containers.  The delegate factory is only used to create the
 * file container for the file overlay.  See
 * {@link DirectoryOverlayContainerImpl#setOverlayDirectory}.
 *
 * TODO: Overlay containers exist as a parallel tier above from usual
 * artifact containers.  These perhaps ought not to be managed using
 * the usual delegate factory mechanism.
 */
public class OverlayContainerFactoryImpl implements OverlayContainerFactory, ContainerFactoryHolder {
    // DS hooks

    /** The top level, delegating, container factory. */
    private ArtifactContainerFactory delegatingContainerFactory;

    @Override
    public synchronized ArtifactContainerFactory getContainerFactory() {
        if ( delegatingContainerFactory == null ) {
            throw new IllegalStateException();
        }
        return delegatingContainerFactory;
    }

    protected synchronized void activate(ComponentContext ctx) {
        // EMPTY
    }

    protected synchronized void deactivate(ComponentContext ctx) {
        this.delegatingContainerFactory = null;
    }

    protected synchronized void setContainerFactory(ArtifactContainerFactory cf) {
        this.delegatingContainerFactory = cf;
    }

    protected synchronized void unsetContainerFactory(ArtifactContainerFactory cf) {
        if ( this.delegatingContainerFactory == cf ) {
            this.delegatingContainerFactory = null;
        }
    }

    // Factory API ...

    // Used by:
    //   com.ibm.ws.adaptable.module.internal.
    //     AdaptableModuleFactoryImpl.getContainer(File, File, ArtifactContainer)
    //
    // Followed immediately by a call to:
    //   com.ibm.wsspi.artifact.overlay.
    //     OverlayContainer.setOverlayDirectory(File, File)

    /**
     * Attempt to create an overlay for a root-of-roots container.
     * 
     * The overlay type must be {@link OverlayContainer}.  Any other type
     * is ignored and null is returned.
     * 
     * The base container must be a root-of-roots container, as the new overlay
     * container has no mechanism to navigate above itself.
     * 
     * @param overlayType The type of container which is to be created. Only
     *     {@link OverlayContainer} is supported.
     * @param baseContainer The base container on which to construct the
     *     overlay container.
     *     
     * @return An overlay container of the specified type.  Null if the
     *     container could not be created.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends OverlayContainer> T createOverlay(Class<T> overlayType, ArtifactContainer baseContainer) {
        // We don't use OSGi to map overlay types to overlay implementation types.
        // This factory implementation hard codes an association of 'OverlayContainer'
        // to 'DirectoryBasedOverlayContainerImpl'.

        // The only implementation currently provided is DirectoryBasedOverlayContainerImpl.
        // The memory based implementation was retired.

        if ( overlayType.equals(OverlayContainer.class) ) {
            DirectoryOverlayContainerImpl container =
                new DirectoryOverlayContainerImpl(baseContainer, this);
            register(container);
            return (T) container;

        } else {
            return null;
        }
    }

    //

    /** Storage for active containers. */
    private final TimedWeakIdentityMap<DirectoryOverlayContainerImpl> activeContainers =
        new TimedWeakIdentityMap<>();

    private void register(DirectoryOverlayContainerImpl container) {
        activeContainers.add(container); // 'add' is synchronized.
    }

    public void introspect(PrintWriter outputWriter) {
        List<? extends Map.Entry<? extends DirectoryOverlayContainerImpl, Long>> snapshot =
            activeContainers.snapshot(); // 'snapshot' is synchronized

        long createNano = activeContainers.getCreateNano();

        outputWriter.println("Directory overlay containers:");
        outputWriter.println("  Base time [ " + TimeUtil.toAbsSec(createNano) + " (s) ]");
        outputWriter.println();

        outputWriter.println("Active containers:");

        if ( snapshot.isEmpty() ) {
            outputWriter.println("  ** NONE **");

        } else {
            for ( Map.Entry<? extends DirectoryOverlayContainerImpl, Long> activeContainerEntry : snapshot ) {
                DirectoryOverlayContainerImpl container = activeContainerEntry.getKey();
                Long putNano = activeContainerEntry.getValue();
                outputWriter.println("  [ " + container + " ] at [ " + TimeUtil.toRelSec(createNano, putNano) + " (s) ]");

                ArtifactContainer baseContainer = container.getContainerBeingOverlaid(); 
                outputWriter.println("    Base [ " + baseContainer + " ]");
                for ( URL baseUrl : baseContainer.getURLs() ) {
                    outputWriter.println("      URL [ " + baseUrl + " ]");
                }

                ArtifactContainer fileContainer = container.getFileOverlay();
                outputWriter.println("    File [ " + fileContainer + " ]");
                for ( URL fileUrl : fileContainer.getURLs() ) {
                    outputWriter.println("      URL [ " + fileUrl + " ]");
                }
            }

            for ( Map.Entry<? extends DirectoryOverlayContainerImpl, Long> activeContainerEntry : snapshot ) {
                DirectoryOverlayContainerImpl container = activeContainerEntry.getKey();
                container.introspect(outputWriter);
            }
        }
    }
}
