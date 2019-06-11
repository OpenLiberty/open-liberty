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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;

/**
 * Iterator implementation for directory overlays.
 * 
 * The implementation is tied to a controlling root overlay container, which
 * is used to tell when entries of the iterator are masked.
 * 
 * The iterator is obtained either from a root overlay container, or from
 * a nested overlay container.  When obtained from a root overlay container,
 * the iterator provides access to the immediate children of the container.
 * When obtained from a nested overlay container, the iterator provides
 * access to the immediate children at the path of the of the nested container.
 *
 * In either case, the iterator is constructed to relative to the root overlay
 * container, using a path to specify the nesting level of the iterator.
 * 
 * The root overlay container is guaranteed to be unmasked.  Any nested overlay
 * container is obtained from a nested entry, which must also be unmasked.
 * However, the entries accessed from the iterator may be masked.  Mask checking
 * is done using the root overlay container.
 */
public class DirectoryOverlayIteratorImpl implements Iterator<ArtifactEntry> {
    private final DirectoryOverlayContainerImpl rootOverlayContainer;
    private final String path;

    /**
     * Create an iterator across an overlay container.
     *
     * @param rootOverlayContainer The root overlay container over which to iterate.
     * @param path The starting location of the iterator.
     */
    protected DirectoryOverlayIteratorImpl(DirectoryOverlayContainerImpl rootOverlayContainer, String path) {
        this.rootOverlayContainer = rootOverlayContainer;
        this.path = ( path.equals("/") ? "" : path ); 

        this.baseEntries = getEntries( rootOverlayContainer.getContainerBeingOverlaid(), path );
        this.fileEntries = getEntries( rootOverlayContainer.getFileOverlay(), path );

        this.nextPath = null;
        this.nextPathIsBase = false;

        this.processedBasePaths = new HashSet<String>();

        advancePath();
    }

    // Invoked by:
    //
    // DirectoryOverlayContainerImpl.iterator()
    // DirectoryOverlayNestedContainerImpl.iterator()
    //    DirectoryOverlayIteratorImpl(DirectoryOverlayContainerImpl, String)

    /**
     * Answer an iterator for a specified container at a specified path.
     *
     * The entries are of the specified container, which will be either the
     * base container of an overlay container, or will be the file container
     * of the overlay container.  The entries will not be overlay entries!
     *
     * Answer an empty iterator if the container is null or if no container
     * is available at the specified path.
     *
     * @param container The container to iterate across.
     * @param path The path of the beginning of iteration.
     *
     * @return An iterator across entries of the container.
     */
    private static Iterator<ArtifactEntry> getEntries(ArtifactContainer container, String path) {
        ArtifactContainer nestedContainer;

        if ( path.equals("/") ) {
            nestedContainer = container;

        } else if ( container != null ) {
            ArtifactEntry entry = container.getEntry(path);
            if ( entry != null ) {
                // TODO: This allows the entry to be converted into a new
                //       root container.  That would happen, for example,
                //       if the entry were a JAR file.  Is this the intent?
                nestedContainer = entry.convertToContainer();
            } else {
                nestedContainer = null;
            }
        } else {
            nestedContainer = null;
        }

        if ( nestedContainer == null ) {
            return Collections.emptyIterator();
        } else {
            return nestedContainer.iterator();
        }
    }

    //

    /**
     * Remove an entry from the container.
     * 
     * This implementation does not support {@link #remove}, and
     * always throws an {@link UnsupportedOperationException}.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    // Iteration state ...

    private final Iterator<ArtifactEntry> baseEntries;
    private final Iterator<ArtifactEntry> fileEntries;

    /** The pre-fetched next path.  Null if no next entry is available. */
    private String nextPath;
    private boolean nextPathIsBase;

    /**
     * Processed base paths.  Recorded during base iteration.
     * Skipped during file iteration.
     */
    private final Set<String> processedBasePaths;

    /**
     * Tell if a next entry is available.
     * 
     * @return True or false telling if a next entry is available.
     */
    @Override
    public boolean hasNext() {
        return ( nextPath != null );
    }

    /**
     * Answer the next entry.
     * 
     * @return The next entry.
     * 
     * @throws NoSuchElementException Thrown if no next entry is available.
     */
    @Override
    public DirectoryOverlayEntryImpl next() {
        if ( nextPath == null ) {
            throw new NoSuchElementException();
        }

        // The path may be obtained from either the base container or 
        // the file container.  The entry may also be obtained from
        // from either the base container or the file container.
        //
        // A path obtained from the file container can be obtained
        // from either the base container or the file container.
        //
        // A path obtained from the file container will always be
        // obtained from the file container.
        //
        // The path will always obtain an entry: The path was verified
        // to not be masked by the root container.

        String lastPath = nextPath;
        boolean lastPathIsBase = nextPathIsBase;

        advancePath();

        // There are two cases:
        //
        // 1) The path was from base entries.  Then, a lookup in the file
        //    entries is attempted.  If the file lookup fails, a lookup
        //    is done in the base entries.  One of these lookups must succeed.
        //
        // 2) The path was from file entries.  Then, a lookup is done in
        //    file entries, and that lookup must succeed.

        DirectoryOverlayEntryImpl nextEntry = rootOverlayContainer.getFileEntry(lastPath);
        if ( nextEntry == null ) {
            if ( !lastPathIsBase ) {
                throw new IllegalStateException();
            }
            nextEntry = rootOverlayContainer.getBaseEntry(lastPath);
            if ( nextEntry == null ) {
                throw new IllegalStateException();
            }
        }
        return nextEntry;
    }

    /**
     * Advance the next path value.
     * 
     * Iterate first across base entries, then across file entries.
     * 
     * Mask both collections according to the root overlay.
     * 
     * Skip file entries which were already processed as base entries.
     * 
     * The selection of a path from the base entries does not mean the
     * next entry (as obtained from {@link DirectoryOverlayContainerImpl#getEntry(String)})
     * will be a base entry.  The overlay container resolves the path against
     * file entries first.
     *
     * @return The next available path.
     */
    private void advancePath() {
        Set<String> maskedPaths = rootOverlayContainer.getMaskedPaths();

        String advancedPath = null;
        boolean advancedPathIsBase = false;

        while ( (advancedPath == null) && baseEntries.hasNext() ) {
            ArtifactEntry baseEntry = baseEntries.next();
            String basePath = path + "/" + baseEntry.getName();

            if ( !maskedPaths.contains(basePath) ) {
                advancedPath = basePath;
                processedBasePaths.add(advancedPath);
                advancedPathIsBase = true;
            }
        }

        while ( (advancedPath == null) && fileEntries.hasNext() ) {
            ArtifactEntry fileEntry = fileEntries.next();
            String filePath = path + "/" + fileEntry.getName();

            if ( !processedBasePaths.contains(advancedPath) &&
                 !maskedPaths.contains(filePath) ) {
                advancedPath = filePath;
                advancedPathIsBase = false;
            }
        }

        this.nextPath = advancedPath;
        this.nextPathIsBase = advancedPathIsBase;
    }
}
