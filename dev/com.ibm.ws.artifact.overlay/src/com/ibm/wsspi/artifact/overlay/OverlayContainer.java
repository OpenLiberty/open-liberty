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
package com.ibm.wsspi.artifact.overlay;

import java.io.File;
import java.util.Set;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;

/**
 * Version of overlay container with only support for adding/overriding
 * of entries.
 * <p>
 * Overlay Container extends the Artifact API Container, making it useable whereever
 * such Containers are used.
 * <br>
 * The aim is to allow a Container to be Wrapped with a layer that can..
 * <ul>
 * <li> Mask & UnMask Entries that may be contained inside. A Masked path does not have to exist already, and if Masked, will also hide overlaid entries.
 * Masked Paths can be dirs, entries, subdirs, etc.
 * <li> Overlay an Entry, or UnOverlay an Entry. Overlaid Entries will return the supplied Entry instead of looking in the wrapped Container for it.
 * UnOverlaying an entry will restore access to the original Entry in the wrapped container.
 * </ul><p><p>
 * Overlaid Entries may override Entry.convertToContainer, but if so the Overlay <em>must not</em> contain overlaid Entries for paths within Containers returned from such
 * overrides.
 */
public interface OverlayContainer extends ArtifactContainer {

    /**
     * Set the location this overlay should use to obtain & store data to.<p>
     * f must exist, must be a directory, and must be writable.<p>
     * Until this method is called, addEntry is non functional on this overlay.
     * 
     * @param cacheDirForOverlayContent location to hold extracted nested overlaid archives if any.
     * @param f Directory to hold overlay data.
     * @throws IllegalStateException if the directory has already been set
     * @throws IllegalArgumentException if f does not exist/is not a directory
     */
    public void setOverlayDirectory(File cacheDirForOverlayContent, File f);

    /**
     * Get the Container this Overlay is wrapping<p>
     * Intended to allow access to original data, to avoid users creating the
     * 'remove, read data, add new overlay based on orig data'
     * pattern.
     */
    public ArtifactContainer getContainerBeingOverlaid();

    /**
     * Hides any Entry at the path supplied.<p>
     * Applies to both overlaid, and original Entries. <br>
     * Applies even if Entry is added via overlay after mask invocation.
     * 
     * @param path The path to hide.
     */
    public void mask(String path);

    /**
     * UnHides an Entry previously hidden via 'mask'.
     * <br>
     * Has no effect if path is not masked.
     * 
     * @param path The path to unhide.
     */
    public void unMask(String path);

    /**
     * Query if a path is currently masked via 'mask'.
     * 
     * @param path The path to query.
     * @return true if masked, false otherwise.
     */
    public boolean isMasked(String path);

    /**
     * Obtain the current set of masked paths.
     * 
     * @return Set of Strings comprising the current mask entries.
     */
    public Set<String> getMaskedPaths();

    /**
     * Adds an Entry to the Overlay.<p>
     * Entry is added at Entry.getPath(), Entry/path need not already exist within the Container<br>
     * User must ensure that Entry remains usable.
     * (Eg underlying artifacts are not closed, removed etc)
     * <p>
     * If the Entry is convertible to Container, behavior is undefined if
     * attempts are made to overlay the contained paths via this method.
     * 
     * @param e the Entry to Add.
     * @return true if the entry was added successfully, false otherwise.
     */
    public boolean addToOverlay(ArtifactEntry e);

    /**
     * Adds an Entry to the Overlay.<p>
     * Entry is added at path Entry/path need not already exist within the Container<br>
     * User must ensure that Entry remains usable.
     * (Eg underlying artifacts are not closed, removed etc)
     * <p>
     * If the Entry is convertible to Container, behavior is undefined if
     * attempts are made to overlay the contained paths via this method.
     * 
     * @param e the Entry to Add.
     * @param path the Location to add the Entry at within the Overlay (Entry.getPath is ignored).
     * @param representsNewRoot if e can convertToContainer, should that container be treated as isRoot true? (Entry.convertToContainer.isRoot is ignored).
     * @return true if the entry was added successfully, false otherwise.
     */
    public boolean addToOverlay(ArtifactEntry e, String path, boolean representsNewRoot);

    /**
     * Removes any overlay for a given path.<p>
     * 
     * @param path The path to stop overlaying.
     */
    public boolean removeFromOverlay(String path);

    /**
     * Queries if a path is currently overlaid.<p>
     * Note this will only query the paths added via addToOverlay, if Entries added via
     * addToOverlay can convert to containers, paths within will NOT be reported
     * 
     * @param path The path to query
     * @return true if the path has an overlay registered. False otherwise.
     */
    public boolean isOverlaid(String path);

    /**
     * Obtains the set of paths within the OverlayContainer known to be overlaid.
     * 
     * @return The set of paths within the OverlayContainer known to be overlaid.
     */
    public Set<String> getOverlaidPaths();

    /**
     * Obtain the nested overlay stored for a given path.<p>
     * Overlays apply within a given Root of an ArtifactContainer only, if you
     * navigate to a container where isRoot=true, then it is a new Overlay, and
     * if you wish to override content in it, you must obtain it via this method.<p>
     * 
     * This method will return null if the path does not represent an ArtifactEntry where
     * isRoot=true, within the current overlay.
     * 
     * @param path Path to obtain Overlay for nested root.
     * @return Overlay if one is available, null otherwise.
     */
    public OverlayContainer getOverlayForEntryPath(String path);

    /**
     * Obtain the overlay container that holds this one.<p>
     * May return null if this overlay container overlays the top most root.
     * 
     * @return the OverlayContainer that holds this OverlayContainer, or null if there is none.
     */
    public OverlayContainer getParentOverlay();

    /**
     * Stores some data associated with the given container/entry path within
     * non persistent in memory cache associated with this overlay instance.
     * 
     * @param path Path to associate data with.
     * @param owner Class of caller setting data, allows multiple adapters to cache against a given path.
     * @param data Data to store for caller.
     */
    public void addToNonPersistentCache(String path, Class owner, Object data);

    /**
     * Removes some data associated with the given container/entry path within
     * non persistent in memory cache associated with this overlay instance.
     * 
     * @param path Path to associate data with.
     * @param owner Class of caller setting data, allows multiple adapters to cache against a given path.
     */
    public void removeFromNonPersistentCache(String path, Class owner);

    /**
     * Obtains some data associated with the given container/entry path within
     * non persistent in memory cache associated with this overlay instance.
     * 
     * @param path Path associated with data.
     * @param owner Class of caller getting data, allows multiple adapters to cache against a given path.
     * @returns Cached data if any was held, or null if none was known.
     */
    public Object getFromNonPersistentCache(String path, Class owner);
}
