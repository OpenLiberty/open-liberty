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
package com.ibm.wsspi.artifact.overlay;

import java.io.File;
import java.util.Set;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;

/**
 * Type for root overlay containers.
 * 
 * Supports masking of entries, and addition or replacement of entries using a file
 * overlay. 
 */
public interface OverlayContainer extends ArtifactContainer {
    /**
     * Set the cache directory of the overlay container, and set
     * the overlay file of the container.
     *
     * The cache directory must exist, must be a directory, and
     * must be writable.
     *
     * (Note: The cache directory and the overlay file relate to
     * distinct function.  The coupling is accepted as the dual
     * assignments complete the initialization of a new overlay
     * container.)
	 *
     * @param cacheDir The cache directory of the overlay container.
     * @param overlayFile The directory containing overlay content for the
     *     container. 
     */
    public void setOverlayDirectory(File cacheDir, File overlayFile);

    // Masking ...

    /**
     * Mask the entry at the specified path.
     *
     * @param path The path to mask.
     */
    public void mask(String path);

    /**
     * Unmask an entry at the specified path.
     *
     * @param path The path to unmask.
     */
    public void unMask(String path);

    /**
     * Tell if a path is masked.
     * 
     * @param path The path to test.
     *
     * @return True or false telling if a path is masked.
     */
    public boolean isMasked(String path);

    /**
     * Obtain the current set of masked paths.
     * 
     * @return The set of masked paths.
     */
    public Set<String> getMaskedPaths();

    // File overlays ...

    /**
     * Attempt to add an entry to this overlay container.
     * 
     * @param entry The entry which is to be added.
     *
     * @return True or false telling if the entry was added.
     */
    public boolean addToOverlay(ArtifactEntry entry);

    /**
     * Attempt to add an entry to this overlay container.  Override the path
     * and root settings of the entry.
     * 
     * @param entry The entry which is to be added.
     * @param path The path to assign to the entry.
     * @param isRoot Override of the whether the entry is a root entry.
     *
     * @return True or false telling if the entry was added.
     */
    public boolean addToOverlay(ArtifactEntry entry, String path, boolean isRoot);

    /**
     * Remove an entry from the overlay.
     * 
     * @param path The path to remove from the overlay.
     * 
     * @return True or false telling if an entry was removed.
     */
    public boolean removeFromOverlay(String path);

    /**
     * Tell if an entry is overlaid.
     * 
     * @param path The path to test.
     *
     * @return True or false telling if the path is overlaid.
     */
    public boolean isOverlaid(String path);

    /**
     * Answer the paths of overlaid entries.
     * 
     * @return The paths of overlaid entries.
     */
    public Set<String> getOverlaidPaths();

    //

    /**
     * Answer the root container of the entry of this overlay container.
     *
     * Answer null if this overlay container is a root-of-roots
     * container.  (A root-of-roots overlay container has an entry
     * if and only if the container is not a root-of-roots container.)
     *
     * @return The root container of the entry of this overlay container.
     *     Null if this container is a root-of-roots container.
     */
    public OverlayContainer getParentOverlay();

    /**
     * Answer the base container of the overlay container.
     *
     * @return The base container of the overlay container.
     */
    public ArtifactContainer getContainerBeingOverlaid();

    /**
     * Answer a nested root overlay container for a specified path.
     *
     * Answer null if the the path does not reach an entry, or if the
     * entry does not convert to a root container.
     *
     * @param path The path to retrieve as a nested root overlay container.
     *
     * @return A nested root overlay container. Created or retrieved from
     *     cache. Null if no entry is found at the specified path, or if
     *     the entry does not convert to a root container.
     */
    public OverlayContainer getOverlayForEntryPath(String path);

    // Non-persistent cache ...

    /**
     * Store data to the cache managed by this container.  Map the
     * data to a specified path and data type.
     *
     * The data should be of the specified data type.  This is
     * not enforced by the API.
     * 
     * @param path The path to map to the data.
     * @param dataType The data type to map to the data.
     * @param data Data to be mapped.
     */
    @SuppressWarnings("rawtypes")
    public void addToNonPersistentCache(String path, Class dataType, Object data);

    /**
     * Remove data from the cache managed by this container.  Unmap the
     * data from the specified path and data type.
     * 
     * Do nothing if the data is not mapped to the specified path and data type.
     * 
     * @param path The path mapped to the data.
     * @param dataType The data type mapped to the data.
     */
    @SuppressWarnings("rawtypes")
    public void removeFromNonPersistentCache(String path, Class owner);

    /**
     * Retrieve data from the cache managed by this container.
     * 
     * @param path The path which is mapped to the target data.
     * @param dataType The data type which is mapped to the target data.
     *
     * @return Data mapped to the specified path and data type.  Null if
     *     no data is mapped to the path and data type.
     */
    @SuppressWarnings("rawtypes")
    public Object getFromNonPersistentCache(String path, Class dataType);
}
