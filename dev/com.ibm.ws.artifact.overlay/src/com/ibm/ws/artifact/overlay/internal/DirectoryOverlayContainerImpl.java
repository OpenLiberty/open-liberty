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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.ws.artifact.overlay.internal.OverlayCacheImpl.OverlayCacheDataImpl;
import com.ibm.ws.artifact.overlay.util.internal.TimeUtil;

/**
 * Implementation of overlay ArtifactContainer that holds all data on disk in a directory.<p>
 * Will both store added content to directory, and use existing content as overlay.<br>
 * Eg, init'ing an overlay from a dir full of data will result in that data being overlaid,
 * and if entries are added they will addto/alter the dir contents.
 */
public class DirectoryOverlayContainerImpl implements OverlayContainer {
    // Utility ...

    /**
     * Collect the paths of all entries of a container.  Add the path of
     * the container itself, unless the container is a root container.
     *
     * @param container The ArtifactContainer to process
     * @param paths The set to add paths to.
     */
    private static void collectPaths(ArtifactContainer container, Set<String> paths) {
        // When invoked from 'getOverlaidPaths()', the container is a root file container.
        // When invoked from 'mask' or 'unMask', the container is a non-root overlay container.

        for ( ArtifactEntry entry : container ) {
            paths.add( entry.getPath() );

            ArtifactContainer childContainer = entry.convertToContainer(true);
            if ( childContainer != null ) {
                collectPaths(childContainer, paths);
            }
        }
    }

    // Identity ...

    /** Reference to the factory which created this overlay container. */
    private final ContainerFactoryHolder cfHolder;

    /** The container which is being overlaid. */
    private final ArtifactContainer baseContainer;

    @Override
    public ArtifactContainer getContainerBeingOverlaid() {
        return baseContainer;
    }

    /**
     * Enable fast mode for this container.  This enables
     * fast mode for the base container and in the file
     * overlay container.
     */
    @Override
    public void useFastMode() {
        if ( fileOverlayContainer == null ) {
            throw new IllegalStateException();
        }

        baseContainer.useFastMode();
        fileOverlayContainer.useFastMode();
    }

    @Override
    public void stopUsingFastMode() {
        if ( fileOverlayContainer == null ) {
            throw new IllegalStateException();
        }

        baseContainer.stopUsingFastMode();
        fileOverlayContainer.stopUsingFastMode();
    }

    //

    /**
     * Tell if this overlay container is a root container.
     *
     * @return True or false telling if this overlay container is
     *     a root container.  This implementation always answers true:
     *     Overlay containers are currently restricted to always
     *     being root containers.
     */
    @Override
    public boolean isRoot() {
        return true;
    }

    /**
     * Answer the root container of this container.
     * 
     * @return The root container of this container.  This implementation
     *     always answers the receiver, since the receiver is restricted
     *     to always be a root container.
     */
    @Override
    public DirectoryOverlayContainerImpl getRoot() {
        return this;
    }

    /**
     * Answer the path of this container.
     * 
     * @return The path of this container.  This implementation always
     *     answers "/", since the receiver is restricted to always be
     *     a root container.
     */
    @Override
    public String getPath() {
        return "/";
        // return baseContainer.getPath();
    }

    /**
     * Answer the name of this container.
     * 
     * @return The name of this container.  This implementation always
     *     answers "/", since the receiver restricted to always be a
     *     root container.
     */
    @Override
    public String getName() {
        return "/";
    }

    // Notification ...

    /** Notifier of this overlay container. */
    private DirectoryOverlayNotifierImpl notifier;

    @Override
    public DirectoryOverlayNotifierImpl getArtifactNotifier() {
        if ( notifier == null ) {
            throw new IllegalStateException();
        }
        return notifier;
    }
    
    private void notifyAdded(Set<String> addedPaths) {
        notifier.notifyEntryChange(
            new DefaultArtifactNotification(this, addedPaths),
            new DefaultArtifactNotification(this, Collections.<String> emptySet()),
            new DefaultArtifactNotification(this, Collections.<String> emptySet()) );
    }

    private void notifyRemoved(Set<String> removedPaths) {
        notifier.notifyEntryChange(
            new DefaultArtifactNotification(this, Collections.<String> emptySet()),
            new DefaultArtifactNotification(this, removedPaths),
            new DefaultArtifactNotification(this, Collections.<String> emptySet()) );
    }

    // Extraction directory ...

    /** Directory for extracted data. */
    private File cacheDir;

    // Values for root (but not root-of-root) overlay containers.

    /**
     * The root of the container enclosing this container.
     *
     * Null for a root-of-root overlay container.
     */
    private final DirectoryOverlayContainerImpl enclosingRootOverlayContainer;

    /**
     * Answer the root of the container enclosing this container.
     * 
     * Answer null if this is a root-of-roots container.
     *
     * @return The root of the container enclosing this container.
     *     Null if this is a root-of-roots container.
     */
    @Override
    public DirectoryOverlayContainerImpl getParentOverlay() {
        return enclosingRootOverlayContainer;
    }

    /**
     * The resolved entry of the enclosing overlay container which
     * was selected to create this overlay container.
     * 
     * Null if this container is a root-of-roots container.
     */
    private final ArtifactEntry resolvedEntryInEnclosingContainer;

    /**
     * The container of the resolved entry of this container.
     * 
     * Null if this container is a root-of-roots container.
     */
    private final ArtifactContainer resolvedEnclosingContainer;

    /**
     * Answer the entry which was resolved in the enclosing container
     * to create this overlay container.  The resolved entry is an
     * entry of the base container or of the file overlay container
     * of the enclosing overlay container.
     * 
     * Answer null if this container is a root-of-roots container.
     * 
     * @return The entry of the enclosing container which was resolved
     *     to create this overlay container.
     */
    @Override
    public ArtifactEntry getEntryInEnclosingContainer() {
        return resolvedEntryInEnclosingContainer;
    }

    /**
     * Answer the container of the resolved entry of this container.
     * The container of the resolved entry is a local container of
     * the base container of the enclosing overlay container, or is
     * a local container of the file overlay container of the enclosing
     * overlay container.
     *
     * @return The container of the resolved entry of this container.
     */
    @Override
    public ArtifactContainer getEnclosingContainer() {
        return resolvedEnclosingContainer;
    }

    //

    /**
     * Create an overlay container for a root-of-roots base container.
     *
     * @param baseContainer The base root-of-roots container.
     * @param cfHolder The factory used to create the overlay container.
     */
    public DirectoryOverlayContainerImpl(
        ArtifactContainer baseContainer,
        ContainerFactoryHolder cfHolder) {

        this(baseContainer, cfHolder, null, null, null);
    }

    /**
     * Create an overlay container for a root base container.  The parent parameters
     * will be null when the base container is a root-of-roots container, and non-null
     * when the base container is a root container but no the root-of-roots.
     * 
     * The new container will be stored as a nested container of the enclosing root container. 
     * 
     * @param baseContainer The container underlying this container.  Must be a root container.
     * @param cfHolder The factory used to create the overlay container.
     *
     * @param baseContainer The base root container.
     * @param cfHolder The factory used to create the overlay container.
     *
     * @param resolvedEnclosingContainer The container enclosing the entry of the base container.
     * @param resolvedEntryInEnclosingContainer The entry of the base container.
     * @param enclosingRootOverlayContainer The root of the overlay container which
     *     encloses this container.
     */
    public DirectoryOverlayContainerImpl(
        ArtifactContainer baseContainer,
        ContainerFactoryHolder cfHolder,

        ArtifactContainer resolvedEnclosingContainer,
        ArtifactEntry resolvedEntryInEnclosingContainer,
        DirectoryOverlayContainerImpl enclosingRootOverlayContainer) {

        // Identity ...

        // Overlays are restricted to root base containers, since this
        // considerably simplifies the implementation.
        if ( !baseContainer.isRoot() ) {
            throw new IllegalArgumentException();
        }

        this.cfHolder = cfHolder;
        this.baseContainer = baseContainer;

        // For enclosed root containers ... null if this is a root-of-roots container

        this.enclosingRootOverlayContainer = enclosingRootOverlayContainer;
        this.resolvedEntryInEnclosingContainer = resolvedEntryInEnclosingContainer;
        this.resolvedEnclosingContainer = resolvedEnclosingContainer;

        // Overlay functionality ... masking and the file overlay

        this.fileOverlayParentDir = null; // set in 'setOverlayDirectory'
        this.fileOverlayDir = null; // set in 'setOverlayDirectory'
        this.fileOverlayContainer = null; // set in 'setOverlayDirectory'

        this.maskedPaths = null; // Null until path is masked.

        this.isPassThroughMode = true; // No masks or file entries (yet).

        // Cache of nested root overlay containers.  Initially, there are none.
        this.nestedOverlays = new HashMap<String, DirectoryOverlayContainerImpl>();

        this.cacheStore = null; // Null until a value is stored in the cache.
    }

    /**
     * Complete the initialization of this overlay container.
     * 
     * Set the cache directory and the file overlay directory.
     * 
     * A call to this method must be made to complete the initialization
     * of this overlay container.  Attempts to use the container before
     * a call is made to this method will result in exceptions.
     * 
     * @param cacheDir The cache directory for this container.
     * @param overlayParentDir The parent directory of the file overlay
     *     directory of this overlay container.
     */
    @Override
    public void setOverlayDirectory(File cacheDir, File overlayParentDir) {
        // The cache directory must exist and must be a directory.
        if ( !FileUtils.fileExists(cacheDir) || !Utils.fileIsDirectory(cacheDir) ) {
            throw new IllegalArgumentException();
        }

        this.cacheDir = cacheDir;

        // Make sure the file overlay directory is usable.
        //
        // The file overlay directory is the directory named ".overlay" beneath
        // the overlay parent directory.  The overlay directory is required
        // to be the only file in the overlay parent directory.
        //
        // Then:
        //
        // (1) The file overlay parent directory must exist and must be a directory.
        //
        // (2) The file overlay directory will be created if it does not exist, but
        // only if the parent overlay directory is empty.
        //
        // (3) The file overlay directory, if it already exists, must be a directory.

        if ( !FileUtils.fileExists(overlayParentDir) ||
             !Utils.fileIsDirectory(overlayParentDir) ) {
            throw new IllegalArgumentException();
        }

        File overlayDir = new File(overlayParentDir, ".overlay");

        if ( !FileUtils.fileExists(overlayDir) ) {
            if ( FileUtils.listFiles(overlayParentDir).length != 0 ) {
                throw new IllegalArgumentException();
            } else {
                FileUtils.fileMkDirs(overlayDir);
            }
        } else if ( !FileUtils.fileIsDirectory(overlayDir) ) {
            throw new IllegalArgumentException();
        } else {
            // Usable as is
        }

        this.fileOverlayParentDir = overlayParentDir;
        this.fileOverlayDir = overlayDir;
        this.fileOverlayContainer = cfHolder.getContainerFactory().getContainer(cacheDir, overlayDir);
        if ( this.fileOverlayContainer == null ) {
            throw new IllegalStateException();
        }

        // Since no masks are yet set, pass through mode is determined
        // entirely by the presence of overlay paths.
        this.isPassThroughMode = getOverlaidPaths().isEmpty();

        DirectoryOverlayNotifierImpl rootNotifier =
            ( (this.enclosingRootOverlayContainer == null) ? null : this.enclosingRootOverlayContainer.getArtifactNotifier() );
        this.notifier = new DirectoryOverlayNotifierImpl(
            this, this.fileOverlayContainer, rootNotifier, this.resolvedEntryInEnclosingContainer);
    }

    //

    /**
     * Answer the URLs of the locations of this overlay container.
     * 
     * The URLs are the concatenation of the URLs of the base container
     * with the URLs of the file overlay container.
     * 
     * The result collection is ordered, with base URLs before file overlay
     * URLs.
     * 
     * @return URLs of this overlay container.
     */
    @Override
    public Collection<URL> getURLs() {
        Collection<URL> urls = new LinkedHashSet<URL>();

        Collection<URL> baseUrls = baseContainer.getURLs();
        if ( baseUrls != null ) {
            urls.addAll(baseUrls);
        }

        Collection<URL> overlayUrls = fileOverlayContainer.getURLs();
        if ( overlayUrls != null ) {
            urls.addAll(overlayUrls);
        }

        return urls;
    }

    /**
     * Answer the physical path of this container.
     * 
     * Answer the physical path of the base container.
     * 
     * @return The physical path of this container.
     */
    @SuppressWarnings("deprecation")
    @Override
    public String getPhysicalPath() {
        return baseContainer.getPhysicalPath();
    }

    //

    /**
     * Get a nested root overlay container for a specified path.
     * 
     * Find the overlay entry for the specified path, then create
     * (or retrieve) a nested root overlay container for that entry.
     * 
     * Answer null if no entry is found at the specified path, or
     * if the specified path does not convert to a root container.
     * 
     * @return A nested root overlay container for a specified path.
     *     Null if the path does not reach an entry which converts
     *     to a root container.
     */
    @Override
    public OverlayContainer getOverlayForEntryPath(String path) {
        DirectoryOverlayEntryImpl entry = getEntry(path);
        if ( entry == null ) {
            return null;
        }

        ArtifactContainer entryAsContainer = entry.convertToContainer();
        if ( !entryAsContainer.isRoot() ) {
            return null;
        }

        return getNestedRootOverlay(entryAsContainer, entry, path);
    }

    /**
     * Create a nested overlay container.
     *
     * @param nestedContainer The nested base or file container of the new nested
     *     overlay container.
     * @param nestedPath The path assigned to the new nested container.
     * @param nestedName The name assigned to the new nested container.
     *
     * @return A new nested overlay container.
     */
    protected DirectoryOverlayNestedContainerImpl getNestedOverlay(
        ArtifactContainer nestedContainer, String nestedPath, String nestedName) {
        return new DirectoryOverlayNestedContainerImpl(this, nestedContainer, nestedPath, nestedName);
    }

    //

    /** Table of nested root overlay containers. */
    private final Map<String, DirectoryOverlayContainerImpl> nestedOverlays;

    /**
     * Create or retrieve a nested root overlay container.
     * 
     * The nested root container is cached.  Multiple requests for a
     * container with the same path will obtain the same container.
     *
     * @param resolvedNestedContainer The nested base or file container of the
     *     new nested root overlay container.
     * @param resolvedNestedEntry The nested base or file entry of
     *     the new nested root overlay container.
     * @param nestedPath The path of the new container relative to
     *     this container.
     *
     * @return The nested root overlay container for the specified path. 
     */
    protected synchronized DirectoryOverlayContainerImpl getNestedRootOverlay(
        ArtifactContainer resolvedNestedContainer, ArtifactEntry resolvedNestedEntry, String nestedPath) {

        DirectoryOverlayContainerImpl nestedOverlay = nestedOverlays.get(nestedPath);
        if ( nestedOverlay != null ) {
            return nestedOverlay;
        }

        File nestedFileOverlayDir = new File(fileOverlayParentDir, nestedPath);

        if ( !FileUtils.fileExists(nestedFileOverlayDir) ) {
            if ( !FileUtils.fileMkDirs(nestedFileOverlayDir) ) {
                throw new IllegalStateException();
            }
        }

        String nestedName = resolvedNestedEntry.getName();
        String nestedParentPath = PathUtils.getParent(nestedPath);

        ArtifactContainer resolvedNestedEnclosingContainer = resolvedNestedEntry.getEnclosingContainer();

        File nestedCacheDir;
        if ( (resolvedNestedEnclosingContainer != null) && !resolvedNestedEnclosingContainer.isRoot() ) {
            nestedCacheDir = new File(cacheDir, nestedParentPath);
        } else {
            nestedCacheDir = cacheDir;
        }
        nestedCacheDir = new File(nestedCacheDir, ".cache");
        nestedCacheDir = new File(nestedCacheDir, nestedName);

        if ( !FileUtils.fileExists(nestedCacheDir)) {
            if ( !FileUtils.fileMkDirs(nestedCacheDir) ) {
                throw new IllegalStateException();
            }
        }

        nestedOverlay = new DirectoryOverlayContainerImpl(
            resolvedNestedContainer, cfHolder,
            resolvedNestedEnclosingContainer, resolvedNestedEntry,
            this);
        nestedOverlay.setOverlayDirectory(nestedCacheDir, nestedFileOverlayDir);
        nestedOverlays.put(nestedPath, nestedOverlay);

        return nestedOverlay;
    }

    //

    @Override
    public DirectoryOverlayEntryImpl getEntry(String nestedPath) {
        // Pass through mode is enabled when no entries are masked
        // and when no file overlay is present.  When pass through
        // mode is enabled nested entries are only available in
        // the base container.

        if ( isPassThroughMode ) {
            ArtifactEntry baseEntry = baseContainer.getEntry(nestedPath);
            if ( baseEntry == null ) {
                return null;
            }

            return new DirectoryOverlayEntryImpl(this, baseEntry);
        }

        // Normalize the path, and make sure it doesn't reach above the
        // current container.

        nestedPath = PathUtils.normalizeUnixStylePath(nestedPath);
        if ( !PathUtils.isNormalizedPathAbsolute(nestedPath) ) {
            return null;
        }

        if ( nestedPath.isEmpty() ) {
            return null; // An empty path maps to the root container.

        } else if ( nestedPath.charAt(0) == '/' ) {
            if ( nestedPath.length() == 1 ) {
                return null; // The path "/" maps to the root container.
            } else {
                // The path already starts with "/" and is not just a a slash.
            }

        } else {
            // Change to a rooted path, since this is a root container.
            nestedPath = "/" + nestedPath;
        }

        // Entry adjustments proceed as follows:
        //
        // 1) First, remove masked entries.
        // 2) Second, allow the file overlay to add or replace entries.
        // 3) Finally, obtain the entry from the base container.

        // Masking does *not* handle paths which are more
        // deeply nested than an explicit mask.

        if ( (maskedPaths != null) && maskedPaths.contains(nestedPath) ) {
            return null;
        }

        ArtifactEntry baseOrFileEntry;
        if ( fileOverlayContainer != null ) {
            baseOrFileEntry = fileOverlayContainer.getEntry(nestedPath);
        } else {
            baseOrFileEntry = null;
        }
        if ( baseOrFileEntry == null ) {
            baseOrFileEntry = baseContainer.getEntry(nestedPath);
        }
        if ( baseOrFileEntry == null ) {
            return null;
        }

        return new DirectoryOverlayEntryImpl(this, baseOrFileEntry);
    }

    /**
     * Attempt to answer an entry for a specified path as a base entry.
     * The path is already normalized, and is known to not be masked.
     *
     * @param nestedPath The path to retrieve.
     *
     * @return An overlay entry built on the base entry of the path.
     */
    protected DirectoryOverlayEntryImpl getBaseEntry(String nestedPath) {
        ArtifactEntry baseEntry = baseContainer.getEntry(nestedPath);
        return ( (baseEntry == null) ? null : new DirectoryOverlayEntryImpl(this, baseEntry) );
    }

    /**
     * Attempt to answer an entry for a specified path as a file entry.
     * The path is already normalized, and is known to not be masked.
     *
     * @param nestedPath The path to retrieve.
     *
     * @return An overlay entry built on the file entry of the path.
     */
    protected DirectoryOverlayEntryImpl getFileEntry(String nestedPath) {
        ArtifactEntry fileEntry = fileOverlayContainer.getEntry(nestedPath);
        return ( (fileEntry == null) ? null : new DirectoryOverlayEntryImpl(this, fileEntry) );
    }

    @Override
    public DirectoryOverlayIteratorImpl iterator() {
        return new DirectoryOverlayIteratorImpl(this, getPath());
    }

    // Overlay global ... 

    /** Cache of whether there are any masked paths or any file overlay entries. */
    private volatile boolean isPassThroughMode;

    // File overlay ...

    /** The parent file of the file overlay directory. */
    private File fileOverlayParentDir;
    /** The file overlay directory. */
    private File fileOverlayDir;
    /** An artifact container for the file overlay directory. */
    private ArtifactContainer fileOverlayContainer;

    /**
     * Answer the file overlay container.  This is null
     * until {@link #setOverlayDirectory(File, File)} is called.
     *
     * @return The file overlay container.
     */
    protected ArtifactContainer getFileOverlay() {
        return fileOverlayContainer;
    }

    @Override
    public Set<String> getOverlaidPaths() {
        if ( fileOverlayContainer == null ) {
            throw new IllegalStateException();
        }

        HashSet<String> overlaidPaths = new HashSet<String>();
        collectPaths(fileOverlayContainer, overlaidPaths);
        return overlaidPaths;
    }

    @Override
    public boolean addToOverlay(ArtifactEntry externalEntry) {
        if ( fileOverlayContainer == null ) {
            throw new IllegalStateException();
        }

        isPassThroughMode = false;

        try {
            return transferIn(externalEntry, externalEntry.getPath(), null);
        } catch ( IOException e ) {
            return false; // FFDC
        }
    }

    @Override
    public boolean addToOverlay(ArtifactEntry externalEntry, String nestedPath, boolean addAsRoot) {
        if ( fileOverlayContainer == null ) {
            throw new IllegalStateException();
        }

        isPassThroughMode = false;

        try {
            return transferIn(externalEntry, nestedPath, addAsRoot);
        } catch ( IOException e ) {
            return false; // FFDC
        }
    }

    @Override
    public synchronized boolean removeFromOverlay(String path) {
        if ( fileOverlayContainer == null ) {
            throw new IllegalStateException();
        }

        File overlayFile = new File(fileOverlayDir, path);
        if ( !FileUtils.fileExists(overlayFile) ) {
            return false;
        } else {
            return Utils.removeFile(overlayFile);
        }
    }

    /**
     * Tell if a target path is overlaid.  That is, if the file
     * overlay container contains an entry at the target path.
     * 
     * @param targetPath The path which is to be tested.
     *
     * @return True or false telling if the path is overlaid.
     */
    @Override
    public boolean isOverlaid(String targetPath) {
        if ( fileOverlayContainer == null ) {
            throw new IllegalStateException();
        }

        // the root is never allowed to be overlaid, that's the purpose of this overlay container.
        if ( (targetPath == null) || (targetPath.length() != 1) || (targetPath.charAt(0) != '/') ) {
            return false;
        } else {
            return ( fileOverlayContainer.getEntry(targetPath) != null );
        }
    }

    /**
     * Clones an ArtifactEntry to the overlay directory, by reading the data from the ArtifactEntry, and writing
     * it to the directory at the requested path, or converting it to a ArtifactContainer and processing
     * the entries recursively.<br>
     * Entries that convert to ArtifactContainers that claim to be isRoot true, are not processed recursively.
     *
     * @param externalEntry The ArtifactEntry to add
     * @param nestedPath The path to add the ArtifactEntry at
     * @param addAsRoot If the ArtifactEntry converts to a ArtifactContainer, should the isRoot be overridden? null = no, non-null=override value
     * @return true if the ArtifactEntry added successfully
     * @throws IOException if error occurred during reading of the streams.
     */
    private synchronized boolean transferIn(ArtifactEntry externalEntry, String nestedPath, Boolean addAsRoot) throws IOException {
        InputStream inputStream = null;

        try {
            try {
                inputStream = externalEntry.getInputStream();
            } catch ( IOException e ) {
                return false; // FFDC
            }

            if ( inputStream != null ) {
                return create(inputStream, nestedPath);

            } else {
                ArtifactContainer externalContainer = externalEntry.convertToContainer();

                boolean root = ((addAsRoot != null) ? addAsRoot.booleanValue() : externalContainer.isRoot());
                if ( root ) {
                    return false;
                }

                if ( externalContainer != null ) {
                    for ( ArtifactEntry nestedExternalEntry : externalContainer ) {
                        transferIn(nestedExternalEntry, nestedPath + "/" + nestedExternalEntry.getName(), externalContainer.isRoot());
                    }
                }
            }

        } finally {
            if ( inputStream != null ) {
                try {
                    inputStream.close();
                } catch ( IOException e ) {
                    // FFDC
                }
            }
        }

        return true;
    }

    private boolean create(InputStream inputStream, String nestedPath) {
        File outputFile = new File(fileOverlayDir, nestedPath);
        File outputFileParent = outputFile.getParentFile();

        if ( !FileUtils.fileExists(outputFileParent) ) {
            if ( !FileUtils.fileMkDirs(outputFileParent) ) {
                return false;
            }
        } else if ( !FileUtils.fileIsDirectory(outputFileParent) ) {
            return false;
        }

        OutputStream outputStream = null;
        
        try {
            outputStream = Utils.getOutputStream(outputFile, false);

            byte transferBuffer[] = new byte[16 * 1024];

            int bytesRead;
            while ( (bytesRead = inputStream.read(transferBuffer)) > 0 ) {
                outputStream.write(transferBuffer, 0, bytesRead);
            }

            return true;

        } catch ( IOException e ) {
            return false; // FFDC

        } finally {
            if ( outputStream != null ) {
                try {
                    outputStream.close();
                } catch ( IOException e ) {
                    // FFDC
                }
            }
        }
    }

    // Masking ...

    // TODO: Masking works ... but barely.
    //
    // Notification shows that masking a path removes that path and all more
    // deeply nested paths.
    //
    // But the implementation only effects masks on directly matched paths.
    // A request for an entry which is deeper than an explicit mask will
    // succeed.  Only when entries are obtained by top-down iteration do the
    // masks have their intended effect.
    //
    // There are other oddities: Masking a non-existent entry is allowed.
    // Masking an entry beneath (or above) an already masked entry is allowed. 

    /** The explicitly masked paths. Null if no paths are masked. */
    private Set<String> maskedPaths;

    /**
     * Tell if a path is explicitly masked.
     * 
     * A path which is deeper than a masked path will not detect as
     * being masked.
     *
     * @return True or false telling if a path is explicitly masked.
     */
    @Override
    public boolean isMasked(String path) {
        return ( (maskedPaths != null) && maskedPaths.contains(path) );
    }

    /**
     * Answer the explicitly masked paths of this container.
     * 
     * @return The explicitly masked paths of this container.
     */
    @Override
    public Set<String> getMaskedPaths() {
        return ( (maskedPaths == null) ? Collections.emptySet() : maskedPaths );
    }

    /**
     * Mask a path.  This removes from view the specific entry which
     * is reached by the path.
     *
     * Masking a path which is shallower than or deeper than an already
     * masked path is allowed.
     * 
     * Mask a path which does not reach an entry is allowed.
     * 
     * Masking a null, empty, or root path is not allowed.
     *
     * Masking effectively removes entries from this container.
     * Notification is performed for the removed entries.
     * 
     * @param nestedPath The path which is to be masked.
     */
    @Override
    public void mask(String nestedPath) {
        if ( notifier == null ) {
            throw new IllegalStateException();

        } else if ( (nestedPath == null) || nestedPath.isEmpty() ||
                    ((nestedPath.length() == 1) && (nestedPath.charAt(0) == '/')) ) {
            throw new IllegalArgumentException();
        }

        if ( maskedPaths == null ) {
            maskedPaths = new HashSet<String>();
        } else if ( maskedPaths.contains(nestedPath) ) {
            return;
        }

        // TODO: The computed paths are incorrect if the nested path
        //       is deeper than another nested path.  'getEntry' only
        //       makes use of explicit masks.

        DirectoryOverlayEntryImpl maskedEntry = getEntry(nestedPath);

        Set<String> newlyMaskedPaths;

        if ( maskedEntry != null ) {
            newlyMaskedPaths = new HashSet<String>();
            newlyMaskedPaths.add(nestedPath);

            DirectoryOverlayNestedContainerImpl maskedContainer = maskedEntry.convertToLocalContainer();            
            if ( maskedContainer != null ) {
                collectPaths(maskedContainer, newlyMaskedPaths);
            }

        } else {
            newlyMaskedPaths = null;
        }

        maskedPaths.add(nestedPath);
        isPassThroughMode = false;

        if ( newlyMaskedPaths != null ) {
            notifyRemoved(newlyMaskedPaths);
        }
    }

    /**
     * Unmask a path.
     *
     * Do nothing if the path is not already explicitly masked.
     *
     * Unmasking a path unmasks that path and any more deeply
     * masked paths, except for those paths which are independently
     * masked.
     *
     * Unmasking effectively adds entries to this container.
     * Notification is performed for the added entries.
     *
     * Unmasking a null, empty, or root path is not allowed.
     *
     * @param nestedPath The path which is to be unmasked.
     */
    @Override
    public void unMask(String nestedPath) {
        if ( notifier == null ) {
            throw new IllegalStateException();

        } else if ( (nestedPath == null) || nestedPath.isEmpty() ||
                    ((nestedPath.length() == 1) && (nestedPath.charAt(0) == '/')) ) {
            throw new IllegalArgumentException();
        }

        if ( (maskedPaths == null) || !maskedPaths.remove(nestedPath) ) {
            return;
        }

        isPassThroughMode = maskedPaths.isEmpty();

        // TODO: The computed paths are incorrect if the nested path
        //       is deeper than another nested path.  'getEntry' only
        //       makes use of explicit masks.

        DirectoryOverlayEntryImpl unmaskedEntry = getEntry(nestedPath); 
        if ( unmaskedEntry != null ) {
            Set<String> previouslyMaskedPaths = new HashSet<String>();
            previouslyMaskedPaths.add(nestedPath);

            DirectoryOverlayNestedContainerImpl unmaskedContainer = unmaskedEntry.convertToLocalContainer();
            if ( unmaskedContainer != null ) {
                collectPaths(unmaskedContainer, previouslyMaskedPaths);
            }

            notifyAdded(previouslyMaskedPaths);
        }
    }

    //

    /** A non-persistent cache associated with this overlay container. */
    private volatile OverlayCacheImpl cacheStore;

    /** Control parameter: Obtain the cache store.  Initialize it if necessary. */
    private static final boolean DO_INIT = true;

    /** Control parameter: Obtain the cache store.  Do not force the store to be initialized. */ 
    private static final boolean DO_NOT_INIT = false;

    /**
     * Retrieve the overlay cache.  According to the control parameter,
     * initialize the cache if necessary.
     *
     * @param init Control parameter: When true, force the cache to be
     *     initialized.  When false, do not force the cache to be initialized.
     *
     * @return The cache associated with this container.  Null if the
     *    cache has not been initialized and initialization was not forced
     *    by the control parameter.
     */
    private OverlayCacheImpl getCacheStore(boolean init) {
        // Variable access is thread safe.
        if ( !init ) {
            // Take a snapshot of the cache store.
            // Initialization of the container's cache pointer
            // after the snapshot is taken is an accepted outcome.
            return cacheStore;
        }

        // Initialization was requested: Use double locking
        // to make sure the store is initialized.

        if ( cacheStore == null ) {
            synchronized(this) {
                if ( cacheStore == null ) {
                    cacheStore = new OverlayCacheImpl();
                }
            }
        }

        return cacheStore;
    }

    /**
     * Obtain a snapshot of the state of the cache store.
     *
     * @return A snapshot of the state of the cache store.
     */
    private List<Map.Entry<String, List<Map.Entry<Class<?>, OverlayCacheDataImpl>>>> getCacheSnapshot() {
        OverlayCacheImpl useCacheStore = getCacheStore(DO_NOT_INIT);
        if ( useCacheStore == null ) {
            return Collections.emptyList();
        } else {
            return useCacheStore.snapshot();
        }
    }

    private Long getCacheCreateNano() {
        OverlayCacheImpl useCacheStore = getCacheStore(DO_NOT_INIT);
        if ( useCacheStore == null ) {
            return null;
        } else {
            return Long.valueOf( useCacheStore.getCreateNano() );
        }
    }

    /**
     * Add data to the cache store.
     * 
     * The intent is for the data to be an instance of the specified data
     * type.  That intent is not enforced by the API.
     *
     * Adding data to the cache store forces the cache store to be
     * initialized.
     *
     * @param path The path to map to the data.
     * @param owner A data type to map to the data.
     * @param data Data which is to be mapped.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void addToNonPersistentCache(String path, Class owner, Object data) {
        getCacheStore(DO_INIT).addToCache(path, owner, data);
    }

    /**
     * Remove data from the cache store.
     * 
     * Do nothing if no data is mapped to the path and data type.
     *
     * @param path The path for which to remove data.
     * @param owner The data type for which to remove data.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void removeFromNonPersistentCache(String path, Class owner) {
        OverlayCacheImpl useCacheStore = getCacheStore(DO_NOT_INIT);
        if ( useCacheStore != null ) {
            useCacheStore.removeFromCache(path, owner);
        }
    }

    /**
     * Retrieve data from the cache store.
     *
     * Answer null if no data is mapped to the path and data type.
     *
     * @param path The path to retrieve.
     * @param owner The data type to retrieve.
     *
     * @return Data mapped to the path and data type.  Null if no data
     *     is mapped to the path and data type.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object getFromNonPersistentCache(String path, Class owner) {
        OverlayCacheImpl useCacheStore = getCacheStore(DO_NOT_INIT);
        return ( (useCacheStore == null) ? null : useCacheStore.getFromCache(path, owner) );
    }

    /**
     * Answer the path to an entry from the root-of-roots container.
     *
     * @param entry The entry for which to obtain the path.
     *
     * @return The path to the entry from the root-of-roots container.
     */
    public String getFullPath(ArtifactEntry entry) {
        String entryPath = entry.getPath();

        // If the immediate root container is the root of roots, just
        // answer the path to the entry.

        ArtifactEntry entryRootEntry = entry.getRoot().getEntryInEnclosingContainer();
        if ( entryRootEntry == null ) {
            return entryPath;
        }

        // Otherwise, walk upwards, concatenating the enclosing entry
        // paths.

        StringBuilder builder = new StringBuilder(entry.getPath());
        while ( entryRootEntry != null ) {
            builder.insert(0,  '/');
            builder.insert(0,  entryRootEntry.getPath());

            entryRootEntry = entryRootEntry.getRoot().getEntryInEnclosingContainer();
        }
        return builder.toString();
    }

    public void introspect(PrintWriter writer) {
        writer.println();

        ArtifactEntry useEnclosingEntry = getEntryInEnclosingContainer();
        if ( useEnclosingEntry == null ) {
            writer.println("Directory overlay container [ ROOT ] [ " + this + " ]");
        } else {
            writer.println("Directory overlay container [ " + getFullPath(useEnclosingEntry) + "] [ " + this + " ]");
        }

        Long cacheCreate = getCacheCreateNano();
        if ( cacheCreate == null ) {
            writer.println("  ** UNINITIALIZED **");
            return;
        }

        long cacheCreateNano = cacheCreate.longValue();

        writer.println("  Base time [ " + TimeUtil.toAbsSec(cacheCreateNano) + " (s) ]");

        List<Map.Entry<String, List<Map.Entry<Class<?>, OverlayCacheDataImpl>>>> cacheSnapshot =
            getCacheSnapshot();

        if ( cacheSnapshot.isEmpty() ) {
            writer.println("  ** EMPTY **");

        } else {
            for ( Map.Entry<String, List<Map.Entry<Class<?>, OverlayCacheDataImpl>>> snapshotForPath : cacheSnapshot ) {
                String path = snapshotForPath.getKey();
                writer.println("  [ " + path + " ]");

                List<Map.Entry<Class<?>, OverlayCacheDataImpl>> entriesForPath = snapshotForPath.getValue();

                if ( entriesForPath.isEmpty() ) {
                    writer.println("    ** EMPTY **");

                } else {
                    for ( Map.Entry<Class<?>, OverlayCacheDataImpl> snapshotEntry : entriesForPath ) {
                        Class<?> dataType = snapshotEntry.getKey();
                        OverlayCacheDataImpl cacheData = snapshotEntry.getValue();

                        Object value = cacheData.value;
                        String valueText = ( (value == null) ? "** NULL **" : value.getClass().getName() );

                        writer.println("    [ " + dataType + " ] [ " + valueText + " ]");
                        writer.println("      Put [ " + TimeUtil.toRelSec(cacheCreateNano, cacheData.putNano) + " (s) ]");
                        writer.println("      Get [ " + TimeUtil.toRelSec(cacheCreateNano, cacheData.getNano) + " (s) ]");
                    }
                }
            }
        }
    }
}
