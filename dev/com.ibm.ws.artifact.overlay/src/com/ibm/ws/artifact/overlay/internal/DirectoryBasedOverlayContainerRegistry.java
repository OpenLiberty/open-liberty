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

import java.util.Set;
import java.util.HashSet;
import java.util.WeakHashMap;

/**
 * Implementation of a WeakHashMap used as a Set to store all registered DirectoryBasedOverlayContainersImpl
 * created by OverlayContainerFactoryImpl
 */
public class DirectoryBasedOverlayContainerRegistry{

    /**
     * WeakHashMap will map the registered containers to a boolean object when they are created.
     * If a container is removed the the value of the key will be set to null to the GC will
     * sweep the container.
     */
    private final WeakHashMap<DirectoryBasedOverlayContainerImpl, Boolean> containers =
        new WeakHashMap<DirectoryBasedOverlayContainerImpl, Boolean>();

    /**
     * Threadsafe wrapper to add elements to the registry
     * 
     * @param toAdd A new DirectoryBasedOverlayContainerImpl to add the registry
     * @return boolean if the container was able to be put into the registry
     */
    public boolean add(DirectoryBasedOverlayContainerImpl toAdd){
        Boolean retValue;
        synchronized(containers){
            retValue = containers.put(toAdd, true);
        }
        return retValue == null ? false : retValue.booleanValue();
    }

    /**
     * Threadsafe wrapper to remove elements from the registry
     * 
     * @param toRemove A DirectoryBasedOverlayContainerImpl to remove from the registry
     * @return boolean if the container was able to be removed properly
     */
    public boolean remove(DirectoryBasedOverlayContainerImpl toRemove){
        Boolean retValue;
        synchronized(containers){
            retValue = containers.remove(toRemove);
        }
        return  retValue == null ? false : retValue.booleanValue();
    }

    /**
     * Threadsafe wrapper to check if the registry is empty
     * 
     * @return boolean isEmpty() of WeakHashMap
     */
    public boolean isEmpty(){
        synchronized(containers){
            return containers.isEmpty();
        }
    }

    /**
     * Threadsafe way to get a new Set with all the current registered DirectoryBasedOverlayContainerImpls
     * 
     * @return A new set representing the current state of registry keySet
     */
    public Set<DirectoryBasedOverlayContainerImpl> getSnapshotSet(){
        synchronized(containers){
            return new HashSet<DirectoryBasedOverlayContainerImpl>(containers.keySet());
        }
    }
}
