/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.overlay.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.ws.artifact.ExtractableArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * Directory based overlay container implementation.
 * 
 * Modifies the content of a base container by means of an entry mask
 * and an alternates/additions container.
 * 
 * Also provides an in-memory cache of data for entries.  The cache is
 * keyed by type.
 */
public class DirectoryBasedOverlayContainerImpl implements OverlayContainer {
    /**
     * Collect the paths of entries of the container.
     * 
     * Collect local paths: Do not descend into nested containers.
     * 
     * @param container The container for which to collect paths.
     * @param storage The collected paths.
     */
    private static void collectPaths(ArtifactContainer container, Set<String> storage) {
        if ( !"/".equals( container.getPath() ) ) {
            storage.add( container.getPath() );
        }

        // TODO: This is very expensive!
        //
        // Conversion should be to local containers, only.
        //
        // Also, entries are added twice, since 'collectPaths' also does
        // an add.

        for ( ArtifactEntry entry : container ) {
            storage.add( entry.getPath() );

            ArtifactContainer entryAsContainer = entry.convertToContainer();
            if ( (entryAsContainer != null) && !entryAsContainer.isRoot()) {
                collectPaths(entryAsContainer, storage);
            }
        }
    }

    /**
     * Recursively delete a file.
     * 
     * Attempt to delete all child files, even if one or more
     * fails to delete.
     * 
     * @param rootFile The root file which is to be deleted.
     * @return True or false telling if the file and all of its
     *     children were deleted.
     */
    private static boolean removeFile(File rootFile) {
        if ( Utils.fileIsFile(rootFile) ) {
            return Utils.deleteFile(rootFile);

        } else {
            boolean allDeleted = true;

            File children[] = Utils.listFiles(rootFile);
            if ( children != null ) {
                for ( File child : children ) {
                    if ( !removeFile(child) ) {
                    	allDeleted = false;
                    }
                }
            }

            if ( allDeleted ) {
                if ( !Utils.deleteFile(rootFile) ) {
                	allDeleted = false;
                }
            }

            return allDeleted;
        }
    }

    //
    
    /**
     * Reference to the factory which created this container.
     * 
     * This is a non-delegating factory, currently always a
     * {@link OverlayContainerFactoryImpl}.
     */
    private final ContainerFactoryHolder containerFactory;

    // Links to the base container.

    /** The base container. */
    private final ArtifactContainer baseContainer;

    // Location for extracted nested base archives ...

    /**
     * On-disk root directory used to hold extracted archives.
     * 
     * A cache directory is a common feature of artifact containers.
     * The cache directory is provided to the container factory when
     * creating enclosed containers, which usually extract any nested
     * artifact which is being interpreted as a nested root container.
     * 
     * This might be the same as the location used by the base container,
     * which would make this reference redundant.
     */
    private File cacheDir;
    
    // Base container nesting ...

    /**
     * Link to the enclosing container (of the base container),
     * for a non-root-of-roots container.
     */
    private final ArtifactContainer enclosingContainer;
    /**
     * Link to the entry which was interpreted to create the
     * base container.  For a non-root-of-roots container.
     */
    private final ArtifactEntry entryInEnclosingContainer;

    // Overlay nesting ...
    
    /** The overlay of the base enclosing container. */
    private final DirectoryBasedOverlayContainerImpl enclosingOverlay;    

    /** Overlays of based enclosed containers. */
    private final Map<String, OverlayContainer> enclosedOverlays;

    // Overlay function ...

    // TODO: Does this really work?  Doesn't the redirect to the base container
    //       cause any nested overlays to be ignored?

    /**
     * Flag when the overlay is trivial and requests can be passed
     * directly to the base container.
     */    
    private volatile boolean isPassThroughMode;

    /** Paths of removed entries. */
    private final Set<String> maskedEntries;

    /** The artifact container containing added and replaced entries. */
    private ArtifactContainer overlayContainer;

    /** On-disk root directory containing added and replaced entries. */
    private File overlayDirectory;

    // Notification ...

    /**
     * The notifier of the overlay.  Tied in to the overlay directory,
     * which means it is not assigned until the overlay directory is
     * assigned.
     */
    private DirectoryBasedOverlayNotifier overlayNotifier;

    // Entry in-memory data store ...

    /**
     * Independent data store for entries.  This currently has nothing
     * to do with the overlay.
     */
    private final OverlayCache cacheStore;

    //
    
    /**
     * Create a new root-of-roots overlay.
     * 
     * @param base The base container of the new container.
     * @param cfHolder The factory which is creating the container.
     */
    public DirectoryBasedOverlayContainerImpl(ArtifactContainer base, ContainerFactoryHolder cfHolder) {
        this(base, cfHolder, null, null, null);
    }

    /**
     * Create a new root-of-roots or enclosed overlay container.
     * 
     * When the enclosing container, enclosing entry, and enclosing overlay are null,
     * the new container is a root-of-roots type container.  When the enclosing values
     * are non-null, the new container is an enclosed root type container.
     *   
     * @param base The base container of the new container.
     * @param containerFactory The factory which is creating the container.
     * @param enclosingContainer The root container of the enclosing entry.
     * @param entryInEnclosingContainer The entry which was interpreted to create
     *     the base container.
     * @param enclosingOverlay The overlay of the root container of the enclosing
     *     entry.
     */
    public DirectoryBasedOverlayContainerImpl(
    	ArtifactContainer baseContainer,
    	ContainerFactoryHolder containerFactory,
    	ArtifactContainer enclosingContainer, ArtifactEntry entryInEnclosingContainer,
    	DirectoryBasedOverlayContainerImpl enclosingOverlay) {

    	// Restrict overlays to root type base containers.
    	// That simplifies the implementation.
        if ( !baseContainer.isRoot() ) {
            throw new IllegalArgumentException();
        }

        // Pointer to the factory which created this container.
        this.containerFactory = containerFactory;
        
        // The container which is being overlaid.
        this.baseContainer = baseContainer;

        // Links to the next highest tier of containers.
        // Null if this container is the root-of-roots.
        this.enclosingContainer = enclosingContainer;
        this.entryInEnclosingContainer = entryInEnclosingContainer;

        // Link to the overlay of the enclosing root container.
        this.enclosingOverlay = enclosingOverlay;
        this.enclosedOverlays = new ConcurrentHashMap<String, OverlayContainer>();

        // Flag when the overlay is trivial and requests can be passed
        // directly to the base container.
        this.isPassThroughMode = true;

        // Local overlay settings: Table of entries removed from the
        // container.
        this.maskedEntries = new HashSet<String>();

        // Independent store used to associate typed data to entries.
        this.cacheStore = new OverlayCache();

        // These are not set until the overlay directory is set,
        // which is done as a step following the initial construction
        // of this container.

        // this.cacheDir
        // this.overlayDirectory
        // this.overlayContainer
        // this.overlayNotifier
    }

    /**
     * Complete this directory overlay by setting its overlay directory and its
     * overlay cache directory.
     * 
     * The overlay directory is created as a child directory ".overlay" of the
     * specified parent overlay directory.
     * 
     * The cache directory must exist and must actually be a directory.
     * 
     * The parent overlay directory must also exist and must also actually be a
     * directory.
     *
     * If the parent overlay directory is not completely empty, one of its children
     * must be ".overlay", and that child must be a directory.
     * 
     * A failure of any of these conditions results in the call failing with
     * an {@link IllegalArgumentException}.
     *
     * @param cacheDir The cache directory for the overlay.
     * @param parentOverlayDirectory The parent of the overlay directory.
     */
    @Override
    public synchronized void setOverlayDirectory(File cacheDir, File parentOverlayDirectory) {
        if ( !FileUtils.fileExists(cacheDir) || !Utils.fileIsDirectory(cacheDir) ) {
            throw new IllegalArgumentException();
        }

        if ( !FileUtils.fileExists(parentOverlayDirectory) || !Utils.fileIsDirectory(parentOverlayDirectory) ) {
            throw new IllegalArgumentException();
        }

        File useOverlayDirectory = new File(parentOverlayDirectory, ".overlay");
        if ( !( (FileUtils.listFiles(parentOverlayDirectory).length == 0) ||
                (FileUtils.fileExists(useOverlayDirectory) && FileUtils.fileIsDirectory(useOverlayDirectory)) ) ) {
            throw new IllegalArgumentException();
        }

        boolean emptyOverlay; // Quickly tell if the overlay starts out empty.
        if ( !FileUtils.fileExists(useOverlayDirectory) ) {
            FileUtils.fileMkDirs(useOverlayDirectory);
            emptyOverlay = true;
        } else if ( FileUtils.listFiles(useOverlayDirectory).length == 0 ) {
        	emptyOverlay = true;
        } else {
        	emptyOverlay = false;
        }

        ArtifactContainer useOverlayContainer =
        	this.containerFactory.getContainerFactory().getContainer(cacheDir, useOverlayDirectory);
        if ( useOverlayContainer == null ) {
            throw new IllegalStateException();
        }

        // All tests have passed ... go ahead and assign the cache and overlay.

        this.cacheDir = cacheDir;

        this.overlayDirectory = useOverlayDirectory;
        this.overlayContainer = useOverlayContainer;

        // The notifier, which needs the overlay container, can not be set.

        //if we have a parentOverlay, the parent must have been configured with the overlay dir.. 
        DirectoryBasedOverlayNotifier enclosingOverlayNotifier =
        	( (this.enclosingOverlay == null) ? null : this.enclosingOverlay.overlayNotifier );

        this.overlayNotifier =
        	new DirectoryBasedOverlayNotifier(this,
        		this.overlayContainer,
        		enclosingOverlayNotifier,
        		this.entryInEnclosingContainer);

        // 'getOverlaidPaths' is very expensive!
        //
        // Even with a replacement, simply checking the overlay directory
        // is faster.

        // Set<String> overlaid = this.getOverlaidPaths();
        // if ( overlaid.size() == 0 ) {
        if ( emptyOverlay ) {
            this.isPassThroughMode = true;
        } else {
            this.isPassThroughMode = false;
        }
    }
    
    //

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public ArtifactContainer getRoot() {
        return this;
    }

    @Override
    public ArtifactContainer getContainerBeingOverlaid() {
        return baseContainer;
    }

    @Override
    public String getPath() {
    	// Should be just '/', since this is a root container.
        return baseContainer.getPath();
    }

    @Override
    public String getName() {
        return "/";
    }

    //

    @Override
    public ArtifactContainer getEnclosingContainer() {
        return enclosingContainer;
    }

    @Override
    public ArtifactEntry getEntryInEnclosingContainer() {
        return entryInEnclosingContainer;
    }

    //

    /**
     * Answer the URLs of all entries of the container.
     * 
     * The result is the merge of URLs of entries in the base
     * container with URLs of entries in the overlay container.
     * 
     * The URLs are ordered, with all base URLs which were not
     * overlaid placed before all overlay URLs.
     * 
     * Masked entries are *not* removed from the result.  (This
     * might be an error.)
     * 
     * @return The URLs of all entries of the container.
     */
    @Override
    public Collection<URL> getURLs() {
    	// TODO: Masked entries are not removed!

        Collection<URL> urls = new LinkedHashSet<URL>();

        Collection<URL> baseUrls = baseContainer.getURLs();
        if ( baseUrls != null ) {
            urls.addAll(baseUrls);
        }
        
        Collection<URL> overlayUrls = overlayContainer.getURLs();
        if ( overlayUrls != null ) {
            urls.addAll(overlayUrls);
        }

        return urls;
    }

    @Override
    public void useFastMode() {
        baseContainer.useFastMode();
    }

    @Override
    public void stopUsingFastMode() {
        baseContainer.stopUsingFastMode();
    }
    
    @Override
    public Iterator<ArtifactEntry> iterator() {
        return new EnhancedEntryIterator(this, baseContainer.getPath(), overlayContainer);
    }

    @SuppressWarnings("deprecation")
	@Override
    public String getPhysicalPath() {
        return baseContainer.getPhysicalPath();
    }

    //

    @Override
    public ArtifactNotifier getArtifactNotifier() {
        if ( overlayNotifier == null ) {
            throw new IllegalStateException();
        }
        return overlayNotifier;
    }


    @Override
    public OverlayContainer getParentOverlay() {
        return enclosingOverlay;
    }
    
    //

    @Override
    public void addToNonPersistentCache(String path, Class<?> owner, Object data) {
        cacheStore.addToCache(path, owner, data);
    }

    @Override
    public void removeFromNonPersistentCache(String path, Class<?> owner) {
        cacheStore.removeFromCache(path, owner);
    }

    @Override
    public Object getFromNonPersistentCache(String path, Class<?> owner) {
        return cacheStore.getFromCache(path, owner);
    }
    
    //

    @Override
    public ArtifactEntry getEntry(String pathAndName) {
        //added to make getEntry go faster for non-overlaid containers (most jar files)
        if (isPassThroughMode) {
            ArtifactEntry baseEntry = baseContainer.getEntry(pathAndName);
            if (baseEntry != null) {
                //note use of baseEntry.getPath to set the overlay entry path, pathAndName is unsanitized, 
                //getPath will always be clean.
                return new OverlayDelegatingEntry(this, baseEntry, baseEntry.getName(), baseEntry.getPath(), overlayContainer);
            } else {
                return null;
            }
        }

        pathAndName = PathUtils.normalizeUnixStylePath(pathAndName);

        //check the path is not trying to go upwards.
        if (!PathUtils.isNormalizedPathAbsolute(pathAndName)) {
            return null;
        }

        //overlay only works at root, so all relative
        //paths can be converted to absolute safely.
        if (!pathAndName.startsWith("/")) {
            pathAndName = "/" + pathAndName;
        }

        if (pathAndName.equals("/") || pathAndName.equals("")) {
            return null;
        }

        //ignore masked entries.
        if (!maskedEntries.contains(pathAndName)) {
            ArtifactEntry target = null;
            if (overlayContainer != null) {
                target = overlayContainer.getEntry(pathAndName);
            }
            if (target == null) {
                //if attempting to use overrides failed, try the base ArtifactContainer.
                target = baseContainer.getEntry(pathAndName);
            }
            //make sure we always wrapper the return, so we get to handle the
            //navigation calls.
            if (target != null) {
                //swapped from using pathAndName, to using getPath, to build path-sanitised overlay entries 
                return new OverlayDelegatingEntry(this, target, target.getName(), target.getPath(), overlayContainer);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    //

    @Override
    public void mask(String path) {
        this.isPassThroughMode = false;
        //notifier is created when the dir overlay path is set via setOverlayDirectory
        if (this.overlayNotifier == null) {
            throw new IllegalStateException();
        }
        //use getEntry to see if we know about the path currently.
        if (this.getEntry(path) != null) {

            Set<String> paths = new HashSet<String>();
            paths.add(path);
            //convert the entry we know not to be null, into a container, only if it is not a new root.
            ArtifactContainer entryAsContainer = this.getEntry(path).convertToContainer(true);
            if (entryAsContainer != null) {
                collectPaths(entryAsContainer, paths);
            }

            //kick the notifier to tell it the path has 'gone'.
            this.overlayNotifier.notifyEntryChange(new DefaultArtifactNotification(this, Collections.<String> emptySet()),
                                                   new DefaultArtifactNotification(this, paths),
                                                   new DefaultArtifactNotification(this, Collections.<String> emptySet()));
        }
        maskedEntries.add(path);
    }

    @Override
    public void unMask(String path) {

        //notifier is created when the dir overlay path is set via setOverlayDirectory
        if (this.overlayNotifier == null) {
            throw new IllegalStateException();
        }
        maskedEntries.remove(path);

        // TODO: This is wrong!  Have to test if the overlay is empty, too.        
        this.isPassThroughMode = maskedEntries.isEmpty();

        //use getEntry to see if we know about the path now it's unmasked.
        if (this.getEntry(path) != null) {

            Set<String> paths = new HashSet<String>();
            paths.add(path);
            //convert the entry we know not to be null, into a container, only if it is not a new root.
            ArtifactContainer entryAsContainer = this.getEntry(path).convertToContainer(true);
            if (entryAsContainer != null) {
                collectPaths(entryAsContainer, paths);
            }

            //kick the notifier to tell it the path has been 'added'. 
            this.overlayNotifier.notifyEntryChange(new DefaultArtifactNotification(this, paths),
                                                   new DefaultArtifactNotification(this, Collections.<String> emptySet()),
                                                   new DefaultArtifactNotification(this, Collections.<String> emptySet()));
        }
    }

    @Override
    public boolean isMasked(String path) {
        return maskedEntries.contains(path);
    }

    @Override
    public Set<String> getMaskedPaths() {
        return maskedEntries;
    }

    /**
     * Clones an ArtifactEntry to the overlay directory, by reading the data from the ArtifactEntry, and writing
     * it to the directory at the requested path, or converting it to a ArtifactContainer and processing
     * the entries recursively.<br>
     * Entries that convert to ArtifactContainers that claim to be isRoot true, are not processed recursively.
     * 
     * @param e The ArtifactEntry to add
     * @param path The path to add the ArtifactEntry at
     * @param addAsRoot If the ArtifactEntry converts to a ArtifactContainer, should the isRoot be overridden? null = no, non-null=override value
     * @return true if the ArtifactEntry added successfully
     * @throws IOException if error occurred during reading of the streams.
     */
    private synchronized boolean cloneEntryToOverlay(ArtifactEntry e, String path, Boolean addAsRoot) throws IOException {

        //validate the ArtifactEntry.. 
        InputStream i = null;
        ArtifactContainer c = null;
        try {
            i = e.getInputStream();
            c = e.convertToContainer();
            if (i == null && c == null) {
                return false; //reject nonsense entries with no data & no ArtifactContainer.
            }
            if (i == null && c != null) {
                boolean root = addAsRoot != null ? addAsRoot.booleanValue() : c.isRoot();
                if (root)
                    return false; //reject ArtifactContainers with no data that wish to be a new root. 
            }

            if (i != null) {
                File f = new File(overlayDirectory, path);
                //make a dir to put it in if there isnt one yet..
                File parent = f.getParentFile();
                if (!FileUtils.fileExists(parent)) {
                    //might not be able to add this, if there's a file already clashing where we need a dir.
                    if (!FileUtils.fileMkDirs(parent))
                        return false;
                }

                FileOutputStream fos = null;
                try {
                    //set false to overwrite if its already there..
                    fos = Utils.getOutputStream(f, false);

                    //wrap it up in an attempt to ensure it's got some buffering.
                    i = new BufferedInputStream(i);

                    //rip out the data & spew it to the file.
                    byte buf[] = new byte[1024];
                    int len;
                    while ((len = i.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                    fos.close();

                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }

            } else {
                //can't obtain inputstream.. is this convertible ??
                if (c != null) {
                    for (ArtifactEntry nested : c) {
                        //we don't use the boolean root, as the override only applies for the 1st conversion.
                        //where it prevents us adding c.isRoot when we are told not to care.
                        cloneEntryToOverlay(nested, path + "/" + nested.getName(), c.isRoot());
                    }
                }
            }
        } finally {
            if (i != null) {
                try {
                    i.close();
                } catch (IOException e1) {
                }
            }
        }
        return true;
    }

    @Override
    public boolean addToOverlay(ArtifactEntry e) {
        //don't allow the add if we're not set with a dir yet!
        if (overlayContainer == null)
            throw new IllegalStateException();

        this.isPassThroughMode = false;

        try {
            return cloneEntryToOverlay(e, e.getPath(), null);
        } catch (IOException e1) {
            return false;
        }
    }

    @Override
    public boolean addToOverlay(ArtifactEntry e, String path, boolean addAsRoot) {
        //don't allow the add if we're not set with a dir yet!
        if (overlayContainer == null)
            throw new IllegalStateException();

        this.isPassThroughMode = false;

        try {
            return cloneEntryToOverlay(e, path, addAsRoot);
        } catch (IOException e1) {
            return false;
        }
    }

    @Override
    public synchronized boolean removeFromOverlay(String path) {
        //don't allow the remove if we're not set with a dir yet!
        if (overlayContainer == null)
            throw new IllegalStateException();

        //if path exists under this.overlayDirectory, then delete it, and any content beneath it.
        File f = new File(overlayDirectory, path);
        if (FileUtils.fileExists(f)) {
            return removeFile(f);
        }
        return false;
    }
    
    @Override
    public boolean isOverlaid(String path) {
        //the root is never allowed to be overlaid, that's the purpose of this overlay container.
        if ("/".equals(path))
            return false;

        if (overlayContainer != null)
            return overlayContainer.getEntry(path) != null;
        else
            return false;
    }

    @Override
    public Set<String> getOverlaidPaths() {
        Set<String> paths = new HashSet<String>();
        collectPaths(overlayContainer, paths); // This is expensive!
        return paths;
    }

    //
    
    /**
     * Iterator for a given path, which will replace overlaid Entries,
     * hide Masked entries, and add in additional Entries, as required.
     */
    private static class EnhancedEntryIterator implements Iterator<ArtifactEntry> {
        private final Iterator<ArtifactEntry> baseIter;
        private final Iterator<ArtifactEntry> overlaidEntries;
        private final DirectoryBasedOverlayContainerImpl overlay;
        private final ArtifactContainer fileOverlayContainer;
        private final Set<String> processedPaths;
        private final String path;
        private static final Set<ArtifactEntry> s = Collections.emptySet();

        private ArtifactEntry internalNext;

        /**
         * Build iterator for path, using overlay ArtifactContainer for data.
         * 
         * @param overlay The OverlayContainer to use to supply entries.
         * @param path The path within OverlayContainer being iterated.
         */
        public EnhancedEntryIterator(DirectoryBasedOverlayContainerImpl overlay, String path, ArtifactContainer c) {
            this.overlay = overlay;
            this.fileOverlayContainer = c;

            ArtifactContainer fileContainer = null;
            //fix the root path, so that concatenation doesn't produce //path/name
            if (path.equals("/")) {
                this.path = "";
                fileContainer = fileOverlayContainer;
            } else {
                this.path = path;
                ArtifactEntry e = null;
                if (fileOverlayContainer != null)
                    e = fileOverlayContainer.getEntry(path);
                if (e != null) {
                    fileContainer = e.convertToContainer();
                }
            }

            if (fileContainer == null) {
                this.overlaidEntries = s.iterator(); //use the emptyset as the iterator.
            } else {
                this.overlaidEntries = fileContainer.iterator(); //we got a container, use it's iterator..
            }

            //init an empty set to record paths we have already seen.
            this.processedPaths = new HashSet<String>();

            Iterator<ArtifactEntry> iterForBase = null;

            //configure the corresponding base ArtifactEntry.
            ArtifactContainer overlaidContainer = overlay.getContainerBeingOverlaid();
            if ("/".equals(path)) {
                iterForBase = overlaidContainer.iterator();
            } else {
                ArtifactEntry baseEntry = overlaidContainer.getEntry(path);
                if (baseEntry != null) {
                    ArtifactContainer baseCon = baseEntry.convertToContainer();
                    if (baseCon != null) {
                        iterForBase = baseCon.iterator();
                    }
                }
            }

            this.baseIter = iterForBase;

            //setup our 'next', although this is sometimes considered bad (to perform 'next' processing
            //when not in a 'next' call).. it's the simplest way to know if we even have a 'next' to return.
            this.internalNext = internalNext();
        }

        /**
         * Internal method to calculate the next ArtifactEntry to return, or null if there isn't one.
         * 
         * @return the next ArtifactEntry to use in this iteration, or null if finished.
         */
        private ArtifactEntry internalNext() {
            ArtifactEntry n = null;
            while (n == null) {
                if (baseIter != null && baseIter.hasNext()) {
                    //iterate over the original base ArtifactContainer entries
                    //(this is the ArtifactContainer underpinning this overlay)
                    //track entries visited (so as not to reprocess them during the overlay processing)
                    //mask out entries as requested
                    //return overlaid entries if overlaid
                    //original entries if not
                    ArtifactEntry possible = baseIter.next();
                    String possiblePath = path + "/" + possible.getName();

                    if (!overlay.getMaskedPaths().contains(possiblePath)) {
                        n = overlay.getEntry(possiblePath);
                        processedPaths.add(possiblePath);
                    }
                } else if (overlaidEntries.hasNext()) {
                    //overlaidEntries iterate over. 
                    ArtifactEntry possibleEntry = overlaidEntries.next();
                    String possiblePath = possibleEntry.getPath();

                    //dont process stuff already handled above.
                    if (!processedPaths.contains(possiblePath)) {
                        //dont process anything masked out.
                        if (!overlay.getMaskedPaths().contains(possiblePath)) {
                            //if the overlay is a ArtifactContainer this ArtifactEntry will be
                            //built to return the overlaid ArtifactContainer when convert is used.
                            n = overlay.getEntry(possiblePath);
                            processedPaths.add(possiblePath);
                        }
                    }

                } else {
                    //no more entries to process.. break the while loop
                    break;
                }
            }
            return n;
        }

        @Override
        public boolean hasNext() {
            return this.internalNext != null;
        }

        @Override
        public ArtifactEntry next() {
            ArtifactEntry toBeReturned = this.internalNext;
            //iterator semantics demands NoSuchElementException rather than null when the 
            //iterator has run out of options.
            if (toBeReturned != null) {
                this.internalNext = internalNext();
                return toBeReturned;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    //
    
    //
    
    /**
     * Every ArtifactContainer created by calling ArtifactEntry.convertToContainer on an ArtifactEntry from
     * an OverlayContainer, is always returned as an OverlayDelegatingContainer,
     * to allow overriding of the getEnclosingContainer/iterator/isRoot/getEntry methods
     */
    private static class OverlayDelegatingContainer implements ArtifactContainer {
        private final DirectoryBasedOverlayContainerImpl owner;
        private final ArtifactContainer delegate;
        private final String path;
        private final String name;
        private final ArtifactContainer fileContainer;

        public OverlayDelegatingContainer(DirectoryBasedOverlayContainerImpl owner, ArtifactContainer delegate, String path, String name, ArtifactContainer f) {
            this.owner = owner;
            this.delegate = delegate;
            this.path = path;
            this.name = name;
            this.fileContainer = f;
        }

        @Override
        public Iterator<ArtifactEntry> iterator() {
            //We use the overlay fs to provide entries to iterate, 
            //which works because the delegate is from the same
            //root as the ArtifactContainer being overlaid.
            return new EnhancedEntryIterator(owner, path, fileContainer);
        }

        @Override
        public ArtifactContainer getEnclosingContainer() {
            //rather than going to the delegate, we take the path of this node,
            //and request the parent from the Overlay FS instead.
            String parent = PathUtils.getParent(path);
            ArtifactContainer c = null;
            if (parent != null) {
                if ("/".equals(parent)) { //this delegating container can't be '/', but its parent can be.. 
                    c = owner;
                } else {
                    ArtifactEntry e = owner.getEntry(parent); //will always come back as a delegating e
                    c = e.convertToContainer(); // thus will always be delegating c
                }
            }
            return c;
        }

        @Override
        public ArtifactEntry getEntryInEnclosingContainer() {
            ArtifactEntry e = null;
            if (path != null) {
                if ("/".equals(path)) { //should never happen, '/' is represented by the overlay class itself, not by the delegatingcontainer
                    e = owner.entryInEnclosingContainer;
                } else {
                    e = owner.getEntry(path);
                }
            }
            return e;
        }

        @Override
        public String getPath() {
            //always return our path, it may differ from the delegates
            return path;
        }

        @Override
        public String getName() {
            //always return our name, it may differ from the delegates
            return name;
        }

        @Override
        public void useFastMode() {
            delegate.useFastMode();
        }

        @Override
        public void stopUsingFastMode() {
            delegate.stopUsingFastMode();
        }

        @Override
        public boolean isRoot() {
            return delegate.isRoot();
        }

        @Override
        public ArtifactContainer getRoot() {
            return owner;
        }

        @Override
        public ArtifactEntry getEntry(String pathAndName) {
            //delegate the getEntry call to the overlayFS to ensure overrides are handled.
            if (pathAndName.startsWith("/")) {
                return owner.getEntry(pathAndName);
            } else {
                return owner.getEntry(getPath() + "/" + pathAndName);
            }
        }

        @Override
        public String toString() {
            return "Overlay ArtifactContainer at virtual location " + path + " for " + delegate;
        }

        /** @throws UnsupportedOperationException */
        @Override
        public Collection<URL> getURLs() {
            Collection<URL> base = delegate.getURLs();
            HashSet<URL> set = new HashSet<URL>();

            //add the uris from the delegate, if there are any
            if (base != null)
                set.addAll(base);

            //if this delegate has an override directory.. add its uris too.
            ArtifactEntry e = fileContainer.getEntry(getPath());
            if (e != null) {
                ArtifactContainer correspondingFileContainer = e.convertToContainer();
                if (correspondingFileContainer != null) {
                    Collection<URL> fset = correspondingFileContainer.getURLs();
                    if (fset != null) {
                        set.addAll(fset);
                    }
                }
            }

            return set;
        }

        @SuppressWarnings("deprecation")
		@Override
        public String getPhysicalPath() {
            String path = delegate.getPhysicalPath();

            //if this delegate has an override.. use its path instead.
            ArtifactEntry e = fileContainer.getEntry(getPath());
            if (e != null) {
                ArtifactContainer correspondingFileContainer = e.convertToContainer();
                if (correspondingFileContainer != null) {
                    String cpath = correspondingFileContainer.getPhysicalPath();
                    if (cpath != null)
                        path = cpath;
                }
            }

            return path;
        }

        /** {@inheritDoc} */
        @Override
        public ArtifactNotifier getArtifactNotifier() {
            return owner.getArtifactNotifier();
        }
    }

    /**
     * Overlay ArtifactEntry, returned as a wrapper for all entries returned from the Overlay FS.
     * <p>
     * The overlay ArtifactEntry allows override of the getEnclosingContainer, path, name,
     * and convertToContainer.
     */
    private static class OverlayDelegatingEntry implements ExtractableArtifactEntry {
        private final ArtifactEntry delegate;
        private final DirectoryBasedOverlayContainerImpl root;
        private final String name;
        private final String path;
        private final ArtifactContainer fileContainer;

        /**
         * Build an OverlayEntry for a given ArtifactEntry, within a given OverlayFS, as a given path/name
         * 
         * @param co The overlay ArtifactContainer this ArtifactEntry is part of.
         * @param e The ArtifactEntry being wrappered for return.
         * @param name The name to return for this ArtifactEntry
         * @param path The path to return for this ArtifactEntry
         */
        public OverlayDelegatingEntry(DirectoryBasedOverlayContainerImpl co, ArtifactEntry e, String name, String path, ArtifactContainer f) {
            root = co;
            delegate = e;
            this.name = name;
            this.path = path;
            this.fileContainer = f;
            if (e == null) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public ArtifactContainer getEnclosingContainer() {
            String parent = PathUtils.getParent(path);
            ArtifactContainer c = null;
            if (parent != null) {
                if ("/".equals(parent)) {
                    c = root;
                } else {
                    //will always come back as a delegating ArtifactEntry, because root is overlayfs.
                    ArtifactEntry e = root.getEntry(parent);
                    //thus will always be delegating ArtifactContainer, because ArtifactEntry is delegating..
                    c = e.convertToContainer();
                }
            }
            //C will be null, or a delegating ArtifactContainer.. 
            //(must not return non-delegating ArtifactContainers, or they 'fall through' the overlay)
            return c;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public ArtifactContainer convertToContainer() {
            return convertToContainer(false);
        }

        @Override
        public ArtifactContainer convertToContainer(boolean localOnly) {
            ArtifactContainer converted = null;
            ArtifactContainer test = delegate.convertToContainer(localOnly);
            if (test != null) {
                boolean isRoot = test.isRoot();
                if (isRoot && !localOnly) {
                    converted = root.internalGetOverlayForEntryPath(test, this, path);
                } else {
                    converted = new OverlayDelegatingContainer(root, test, path, name, fileContainer);
                }
            }
            return converted;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return delegate.getInputStream();
        }

        /** {@inheritDoc} */
        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public String toString() {
            return "Overlay ArtifactEntry at " + path + " name " + name + " Wrapping " + delegate.toString();
        }

        @Override
        public ArtifactContainer getRoot() {
            return root;
        }

        /** {@inheritDoc} */
        @Override
        public long getLastModified() {
            long lastmod = delegate.getLastModified();

            //if this delegate has an override.. use its resource instead
            ArtifactEntry e = fileContainer.getEntry(getPath());
            if (e != null) {
                long clastmod = e.getLastModified();
                if (clastmod != 0L)
                    lastmod = clastmod;
            }

            return lastmod;
        }

        /** {@inheritDoc} */
        @Override
        public URL getResource() {
            URL resource = delegate.getResource();

            //if this delegate has an override.. use its resource instead
            ArtifactEntry e = fileContainer.getEntry(getPath());
            if (e != null) {
                URL cresource = e.getResource();
                if (cresource != null)
                    resource = cresource;
            }

            return resource;
        }

        @SuppressWarnings("deprecation")
        @Override
        public String getPhysicalPath() {
            String path = delegate.getPhysicalPath();

            //if this delegate has an override.. use its path instead
            ArtifactEntry e = fileContainer.getEntry(getPath());
            if (e != null) {
                String cpath = e.getPhysicalPath();
                if (cpath != null)
                    path = cpath;
            }

            return path;
        }

        @Override
        public File extract() throws IOException {
            if (delegate instanceof ExtractableArtifactEntry) {
                return ((ExtractableArtifactEntry) delegate).extract();
            }
            return null;
        }
    }

    //

    @Override
    public OverlayContainer getOverlayForEntryPath(String path) {
        OverlayContainer result = null;
        ArtifactEntry testEntry = getEntry(path);
        if (testEntry != null) {
            ArtifactContainer test = testEntry.convertToContainer();
            if (test.isRoot()) {
                result = internalGetOverlayForEntryPath(test, testEntry, path);
            }
        }
        return result;
    }
    
    private synchronized OverlayContainer internalGetOverlayForEntryPath(
    	ArtifactContainer containerBeingOverlaid,
    	ArtifactEntry entryRepresentingContainerInOverlay,
    	String pathToEntryInOverlay) {

    	OverlayContainer result = null;

    	if (this.enclosedOverlays.containsKey(pathToEntryInOverlay)) {
    		return this.enclosedOverlays.get(pathToEntryInOverlay);
    	}

    	OverlayContainer d = new DirectoryBasedOverlayContainerImpl(containerBeingOverlaid, containerFactory, entryRepresentingContainerInOverlay.getEnclosingContainer(), entryRepresentingContainerInOverlay, this);
    	String name = entryRepresentingContainerInOverlay.getName();
    	File originalOverlayDir = this.overlayDirectory.getParentFile(); //using parent of overlayDir, because overlay dir is actually dir/.overlay
    	File originalCacheDir = this.cacheDir;

    	String parentPath = PathUtils.getParent(pathToEntryInOverlay);

    	File newOverlayDir = new File(originalOverlayDir, pathToEntryInOverlay);
    	File newCacheDir = null;

    	//here we build the new overlay & cachedir for the entry.
    	//we try to minimise the no of .cache dirs needed
    	//the .cache dir is held in the same dir that held the 'entry'
    	//if there are 3 jars in a dir, there will be one .cache for the dir
    	//and inside the .cache will be dirs for each of the jars present.

    	//we have to do this, as if a jar is added to the overlay, and opened as a new root
    	//and thus a new overlay, it needs a location to store overrides, and because the jar
    	//is present, we can't use a dir with the name of the jar.. 

    	//this path manipulation for .cache is used to be compatible with the approach used
    	//in the artifact api.

    	if (entryRepresentingContainerInOverlay.getEnclosingContainer() != null && !entryRepresentingContainerInOverlay.getEnclosingContainer().isRoot()) {
    		//our container was not root, so we need to use it's path to ensure we remain unique
    		newCacheDir = new File(originalCacheDir, parentPath);
    		newCacheDir = new File(newCacheDir, ".cache");
    		newCacheDir = new File(newCacheDir, name);
    	} else {
    		//our container was root, so we use the .cache there.
    		newCacheDir = new File(originalCacheDir, ".cache");
    		newCacheDir = new File(newCacheDir, name);
    	}

    	boolean createOk = true;
    	if (!FileUtils.fileExists(newOverlayDir)) {
    		createOk &= FileUtils.fileMkDirs(newOverlayDir);
    	}
    	if (!FileUtils.fileExists(newCacheDir)) {
    		createOk &= FileUtils.fileMkDirs(newCacheDir);
    	}

    	if (createOk) {
    		d.setOverlayDirectory(newCacheDir, newOverlayDir);
    		this.enclosedOverlays.put(pathToEntryInOverlay, d);
    		result = d;
    	} else {
    		//we couldn't build our directories.. 
    		//need to exit here, to prevent users believing this
    		//nested overlay was just 'missing';
    		throw new IllegalStateException();
    	}

    	return result;
    }

    //
    
    /**
     * Represents an Imaginary node.
     * <p>
     * Imaginary nodes are created to support Entries added to the Overlay
     * within paths that did not exist. <br>(eg, adding /fred/wilma/betty.txt
     * when /fred has no wilma, would result in a StubContainer being created
     * to represent the imaginary wilma node, to hold betty.txt, and a StubEntry
     * to represent that StubContainer within /fred)
     * <p>
     * StubContainer/Entry are very light, and deliberately do not implement the
     * Path, Iteration, EnclosingContainer, concepts.
     */
    public static class StubEntry implements ArtifactEntry {
        private final String name;
        private final ArtifactContainer convertTo;

        public StubEntry(String name, ArtifactContainer convertTo) {
            this.name = name;
            this.convertTo = convertTo;
        }

        @Override
        public ArtifactContainer getEnclosingContainer() {
            throw new IllegalStateException();
        }

        @Override
        public String getPath() {
            throw new IllegalStateException();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public ArtifactContainer convertToContainer() {
            return convertTo;
        }

        @Override
        public ArtifactContainer convertToContainer(boolean localOnly) {
            if (localOnly && convertTo.isRoot()) {
                //refuse to convert non local's when localOnly is set.
                return null;
            }
            return convertTo;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return null;
        }

        @Override
        public long getSize() {
            return 0L;
        }

        @Override
        public ArtifactContainer getRoot() {
            return null;
        }

        @Override
        public long getLastModified() {
            return 0L;
        }

        @Override
        public URL getResource() {
            return null;
        }

        @Override
        public String getPhysicalPath() {
            return null;
        }
    }

    /**
     * Represents an Imaginary node.
     * <p>
     * Imaginary nodes are created to support Entries added to the Overlay
     * within paths that did not exist. <br>(eg, adding /fred/wilma/betty.txt
     * when /fred has no wilma, would result in a StubContainer being created
     * to represent the imaginary wilma node, to hold betty.txt, and a StubEntry
     * to represent that StubContainer within /fred)
     * <p>
     * StubContainer/Entry are very light, and deliberately do not implement the
     * Path, Iteration, EnclosingContainer, concepts.
     */
    public static class StubContainer implements ArtifactContainer {
        private final DirectoryBasedOverlayContainerImpl owner;
        private final String name;
        private final String path;

        public StubContainer(DirectoryBasedOverlayContainerImpl owner, String path, String name, ArtifactEntry e) {
            this.owner = owner;
            this.name = name;
            this.path = path;
        }

        @Override
        public Iterator<ArtifactEntry> iterator() {
            throw new IllegalStateException();
        }

        @Override
        public ArtifactContainer getEnclosingContainer() {
            throw new IllegalStateException();
        }

        @Override
        public ArtifactEntry getEntryInEnclosingContainer() {
            throw new IllegalStateException();
        }

        @Override
        public String getPath() {
            throw new IllegalStateException();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void useFastMode() {}

        @Override
        public void stopUsingFastMode() {}

        @Override
        public boolean isRoot() {
            //StubEntries are only needed for path components created for nodes that did
            //not exist within the original ArtifactContainer. As root must Always exist, this Stub
            //cannot represent root, so will always return false.
            return false;
        }

        @Override
        public ArtifactEntry getEntry(String pathAndName) {
            if (pathAndName.startsWith("/")) {
                return owner.getEntry(pathAndName);
            } else {
                return owner.getEntry(path + "/" + pathAndName);
            }
        }

        @Override
        public ArtifactContainer getRoot() {
            return null;
        }

        @Override
        public Collection<URL> getURLs() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPhysicalPath() {
            return null;
        }

        @Override
        public ArtifactNotifier getArtifactNotifier() {
            throw new UnsupportedOperationException();
        }
    }
}
