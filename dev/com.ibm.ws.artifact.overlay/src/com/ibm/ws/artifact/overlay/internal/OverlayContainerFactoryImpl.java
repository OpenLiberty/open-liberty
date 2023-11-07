/*******************************************************************************
 * Copyright (c) 2019,2023 IBM Corporation and others.
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
package com.ibm.ws.artifact.overlay.internal;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Set;

import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;

//@formatter:off
/**
 * Overlay container factory.  This is typed as a delegating factory, but does not
 * delegate.
 *
 * The main API is {@link #createOverlay(Class, ArtifactContainer)}, which always
 * creates a directory overlay container ({@link DirectoryBasedOverlayContainerImpl}).
 */
public class OverlayContainerFactoryImpl implements OverlayContainerFactory, ContainerFactoryHolder {
    // Factory delegation ...

    // This appears to be residual ... the root delegating container
    // factory is not used.

    private ArtifactContainerFactory containerFactory;

    protected synchronized void activate(ComponentContext ctx) {
        // EMPTY
    }

    protected synchronized void deactivate(ComponentContext ctx) {
        this.containerFactory = null;
    }

    protected synchronized void setContainerFactory(ArtifactContainerFactory containerFactory) {
        this.containerFactory = containerFactory;
    }

    protected synchronized void unsetContainerFactory(ArtifactContainerFactory containerFactory) {
        if ( this.containerFactory == containerFactory ) {
            this.containerFactory = null;
        }
    }

    @Override
    public synchronized ArtifactContainerFactory getContainerFactory() {
        if ( containerFactory == null ) {
            throw new IllegalStateException("Null container factory");
        }
        return containerFactory;
    }

    // Main factory API ...

    /**
     * Main API: Create an overlay container.
     *
     * According to the API definition, creation of an overlay container should delegate to
     * a factory according to the overlay type parameter.  This implementation is hard-coded
     * to only accept {@link OverlayContainer} as the overlay type, and answers null for
     * any other type parameter.  A {@link DirectoryBasedOverlayContainerImpl} is always
     * created when the type parameter is {@link OverlayContainer}.
     *
     * @param overlayType The type of overlay container which is to be created.  This
     *     implementation only accepts {@link OverlayContainer}.  Null is returned for
     *     any other parameter value.
     * @param baseContainer The base container of the overlay.
     *
     * @return T The overlay container which was created.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends OverlayContainer> T createOverlay(Class<T> overlayType,
                                                        ArtifactContainer baseContainer) {

        // Don't use OSGI to determine a delegate factory that creates
        // the concrete container.  Hard code the mapping of type parameter OverlayContainer
        // to DirectoryBasedOverlayContainerImpl.
        //
        // Previously, an in-memory overlay container was supported.

        if ( !overlayType.equals(OverlayContainer.class) ) {
            return null;
        }

        return (T) registry.getOverlay(baseContainer, this);
    }

    // Introspection ...

    /** Table of active overlay containers. */
    private final DirectoryBasedOverlayContainerRegistry registry =
        new DirectoryBasedOverlayContainerRegistry();

    /**
     * Register (add) a container to the registry.
     *
     * See {@link DirectoryBasedOverlayContainerRegistry#add}.
     *
     * @param container A container which is to be added.
     */
    private void register(DirectoryBasedOverlayContainerImpl container) {
        registry.add(container);
    }

    /**
     * Introspect the registered containers.
     *
     * @param outputWriter The writer which receives the introspection output.
     */
    public void introspect(PrintWriter outputWriter) {
        outputWriter.println("Active Containers:");

        if ( registry.isEmpty() ) {
            outputWriter.println("  ** NONE **");
            return;
        }

        Set<DirectoryBasedOverlayContainerImpl> snapshot = registry.getSnapshotSet();

        format(outputWriter, "  Number of Registered Containers: [ %d ]", snapshot.size());
        outputWriter.println();

        outputWriter.println("Containers in Set:");
        for ( DirectoryBasedOverlayContainerImpl overlayContainer : snapshot ) {
            // Directory overlay containers are root contains, meaning, their path
            // is always '/'.
            //
            // However, they may be root-of-root containers, which have no enclosing
            // entry, or may be enclosed root containers, that is, entries which have
            // been interpreted as containers.
            //
            // For overlays which are enclosed roots, display the path from the enclosing
            // root to the entry which was interpreted.  Otherwise, display "ROOT" to
            // indicate that the container is a root-of-roots.

            ArtifactEntry enclosingEntry = overlayContainer.getEntryInEnclosingContainer();
            String enclosingEntryPath;
            if ( enclosingEntry == null ) {
                enclosingEntryPath = "ROOT";
            } else {
                enclosingEntryPath = overlayContainer.getFullPath(enclosingEntry);
            }
            format(outputWriter, "  [ %s ] [ %s ]", enclosingEntryPath, overlayContainer);

            ArtifactContainer baseContainer = overlayContainer.getContainerBeingOverlaid();
            format(outputWriter, "      Base [ %s ]", baseContainer);
            for ( URL baseURL : baseContainer.getURLs() ) {
                format(outputWriter, "        URL [ %s ]", baseURL);
            }

            ArtifactContainer fileContainer = overlayContainer.getFileOverlay();
            format(outputWriter, "      File [ %s ]", fileContainer);
            for ( URL fileURL : fileContainer.getURLs() ) {
                format(outputWriter, "        URL [ %s ]", fileURL);
            }
        }

        for ( DirectoryBasedOverlayContainerImpl containerEntry : snapshot ) {
            containerEntry.introspect(outputWriter);
        }
    }

    private void format(PrintWriter writer, String format, Object ... parms) {
        writer.println(String.format(format, parms));
    }
}
//@formatter:on