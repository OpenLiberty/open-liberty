/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import java.io.PrintWriter;
import java.net.URL;
import java.util.Set;

import com.ibm.wsspi.artifact.ArtifactEntry;
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
            
            DirectoryBasedOverlayContainerImpl tempReference = new DirectoryBasedOverlayContainerImpl(b, this);

            register(tempReference);

            return (T) tempReference;
        }

        return null;
    }

    private ArtifactContainerFactory containerFactory = null;
    

    protected synchronized void activate(ComponentContext ctx) {
        // EMPTY
    }

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

    /**
     * Registry to store all the containers created with this.createOverlay()
     */
    private final DirectoryBasedOverlayContainerRegistry registeredContainers =
        new DirectoryBasedOverlayContainerRegistry();

    /**
     * method to add a container to the registrt
     * @param container to add to registry
     */
    private void register(DirectoryBasedOverlayContainerImpl container) {
        registeredContainers.add(container);
    }

    /**
     * Introspection method to print then number of registered containers, the enclosing and current containers in the registry and the Base / File URLs associated with them
     * @param outputWriter PrintWriter to print the introspection
     */
    public void introspect(PrintWriter outputWriter) {

        outputWriter.println("Active Containers:");

        if (registeredContainers.isEmpty()) {
            outputWriter.println("  ** NONE **");
        } else {

            Set<DirectoryBasedOverlayContainerImpl> snapshotSet = registeredContainers.getSnapshotSet();

            outputWriter.println(String.format("  Number of Registered Containers: [ %d ]",snapshotSet.size()));
            outputWriter.println();

            outputWriter.println("Containers in Set:");
            for(DirectoryBasedOverlayContainerImpl containerEntry: snapshotSet){

                //print the enclosing container and current container////////////////////
                ArtifactEntry useEnclosingEntry = containerEntry.getEntryInEnclosingContainer();
                String enclosingEntryIntrospectFormat = "  [ %s ] [ %s ]";
                if(useEnclosingEntry == null) {
                    outputWriter.println(String.format(enclosingEntryIntrospectFormat, "ROOT" ,containerEntry.toString()));
                }
                else {
                    outputWriter.println(String.format(enclosingEntryIntrospectFormat, containerEntry.getFullPath(useEnclosingEntry) ,containerEntry.toString()));
                }
                
                //print the base container reference and URLs////////////////////////////
                ArtifactContainer baseContainer = containerEntry.getContainerBeingOverlaid();
                outputWriter.println(String.format("      Base [ %s ]", baseContainer.toString()));
                for(URL baseURL : baseContainer.getURLs()){
                    outputWriter.println(String.format("        URL [ %s ]",baseURL.toString()));
                }

                //print the file container reference and URLs////////////////////////////
                ArtifactContainer fileContainer = containerEntry.getFileOverlay();
                outputWriter.println(String.format("      File [ %s ]", fileContainer.toString()));
                for(URL fileURL : fileContainer.getURLs()){
                    outputWriter.println(String.format("        URL [ %s ]", fileURL.toString()));
                }


            }

            //introspect each of the containers
            for(DirectoryBasedOverlayContainerImpl containerEntry : snapshotSet){
                containerEntry.introspect(outputWriter);
            }

        }
    }
}
