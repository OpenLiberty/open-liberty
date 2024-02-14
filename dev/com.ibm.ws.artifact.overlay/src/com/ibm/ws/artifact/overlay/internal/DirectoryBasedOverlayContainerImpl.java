/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.artifact.ExtractableArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.DefaultArtifactNotification;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * Implementation of overlay ArtifactContainer that holds all data on disk in a directory.<p>
 * Will both store added content to directory, and use existing content as overlay.<br>
 * Eg, init'ing an overlay from a dir full of data will result in that data being overlaid,
 * and if entries are added they will addto/alter the dir contents.
 */
@SuppressWarnings({ "deprecation", "rawtypes" })
public class DirectoryBasedOverlayContainerImpl implements OverlayContainer {
    private static final TraceComponent tc = Tr.register(DirectoryBasedOverlayContainerImpl.class);

    /** ArtifactContainer being wrapped */
    private final ArtifactContainer base;
    /** The set of paths being masked/hidden */
    private final Set<String> maskSet;
    /** DS safe way to obtain reference to ArtifactContainerFactory */
    private final ContainerFactoryHolder cfHolder;

    /** Single source for overlay */
    private ArtifactContainer fileOverlayContainer;
    /** Actual FS location to use as backing store */
    private File overlayDirectory;
    /** Directory used by overlay to hold extracted zip archives */
    private File cacheDirForOverlay;
    /** Notifier to give back.. cannot be built till the overlay source is set */
    private DirectoryBasedOverlayNotifier overlayNotifier;

    /** Used for nested overlays, which are used to handle wrappered nested roots. */
    private final ArtifactContainer enclosingContainer;
    private final ArtifactEntry entryInEnclosingContainer;

    private final Map<String, OverlayContainer> nestedOverlays = new ConcurrentHashMap<String, OverlayContainer>();
    private final OverlayCache cacheStore = new OverlayCache();
    private final DirectoryBasedOverlayContainerImpl parentOverlay;

    private volatile boolean isPassThroughMode = true;

    /**
     * Build an overlay for the given ArtifactContainer
     *
     * @param base the ArtifactContainer to overlay.
     */
    public DirectoryBasedOverlayContainerImpl(ArtifactContainer base, ContainerFactoryHolder cfHolder) {
        this(base, cfHolder, null, null, null);
    }

    public DirectoryBasedOverlayContainerImpl(ArtifactContainer base, ContainerFactoryHolder cfHolder, ArtifactContainer parent, ArtifactEntry entryInParent,
                                              DirectoryBasedOverlayContainerImpl parentOverlay) {
        //Although overlays 'could' work at any ArtifactContainer depth, it
        //makes the implementation a lot simpler if they are restricted
        //to the root level only.
        //Requirements are met by a root-only overlay.. so restricting it here.
        if (!base.isRoot()) {
            throw new IllegalArgumentException("Base container [ " + base + " ] is not a root container");
        }

        this.base = base;
        this.maskSet = new HashSet<String>();
        this.cfHolder = cfHolder;
        this.enclosingContainer = parent;
        this.entryInEnclosingContainer = entryInParent;
        this.parentOverlay = parentOverlay;
    }

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
        private StringBuilder baseIterBuilder = null;
        int prefixLength = 0;

        /**
         * Build iterator for path, using overlay ArtifactContainer for data.
         *
         * @param overlay The OverlayContainer to use to supply entries.
         * @param path    The path within OverlayContainer being iterated.
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
                    if (baseIterBuilder == null) {
                        baseIterBuilder = new StringBuilder(path);
                        baseIterBuilder.append('/');
                        prefixLength = baseIterBuilder.length();
                    } else {
                        baseIterBuilder.setLength(prefixLength);
                    }

                    ArtifactEntry possible = baseIter.next();
                    baseIterBuilder.append(possible.getName());
                    String possiblePath = baseIterBuilder.toString();

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

        /** {@inheritDoc} */
        @Override
        public String getPhysicalPath() {
            String dpath = delegate.getPhysicalPath();

            //if this delegate has an override.. use its path instead.
            ArtifactEntry e = fileContainer.getEntry(getPath());
            if (e != null) {
                ArtifactContainer correspondingFileContainer = e.convertToContainer();
                if (correspondingFileContainer != null) {
                    String cpath = correspondingFileContainer.getPhysicalPath();
                    if (cpath != null)
                        dpath = cpath;
                }
            }

            return dpath;
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
         * @param co   The overlay ArtifactContainer this ArtifactEntry is part of.
         * @param e    The ArtifactEntry being wrappered for return.
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
                throw new IllegalArgumentException("Null delegate entry [ " + name + " ] [ " + path + " ] in [ " + root + " ]");
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
            URL resource = null;

            //if this delegate has an override.. use its resource instead
            ArtifactEntry e = fileContainer.getEntry(getPath());
            if (e != null) {
                resource = e.getResource();
            }

            if (resource == null) {
                resource = delegate.getResource();
            }
            return resource;
        }

        /** {@inheritDoc} */
        @Override
        public String getPhysicalPath() {
            String dpath = delegate.getPhysicalPath();

            //if this delegate has an override.. use its path instead
            ArtifactEntry e = fileContainer.getEntry(getPath());
            if (e != null) {
                String cpath = e.getPhysicalPath();
                if (cpath != null)
                    dpath = cpath;
            }

            return dpath;
        }

        @Override
        public File extract() throws IOException {
            if (delegate instanceof ExtractableArtifactEntry) {
                return ((ExtractableArtifactEntry) delegate).extract();
            }
            return null;
        }
    }

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

    @Override
    public OverlayContainer getParentOverlay() {
        return parentOverlay;
    }

    private synchronized OverlayContainer internalGetOverlayForEntryPath(ArtifactContainer containerBeingOverlaid, ArtifactEntry entryRepresentingContainerInOverlay,
                                                                         String pathToEntryInOverlay) {
        OverlayContainer result = null;

        if (this.nestedOverlays.containsKey(pathToEntryInOverlay)) {
            return this.nestedOverlays.get(pathToEntryInOverlay);
        }

        OverlayContainer d = new DirectoryBasedOverlayContainerImpl(containerBeingOverlaid, cfHolder, entryRepresentingContainerInOverlay.getEnclosingContainer(), entryRepresentingContainerInOverlay, this);
        String name = entryRepresentingContainerInOverlay.getName();
        File originalOverlayDir = this.overlayDirectory.getParentFile(); //using parent of overlayDir, because overlay dir is actually dir/.overlay
        File originalCacheDir = this.cacheDirForOverlay;

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

        boolean okOverlay = false;
        if (!FileUtils.fileExists(newOverlayDir)) {
            okOverlay = FileUtils.fileMkDirs(newOverlayDir);
            createOk &= okOverlay;
        } else {
            okOverlay = true;
        }

        boolean okCache = false;
        if (!FileUtils.fileExists(newCacheDir)) {
            okCache = FileUtils.fileMkDirs(newCacheDir);
            createOk &= okCache;
        } else {
            okCache = true;
        }

        if (createOk) {
            d.setOverlayDirectory(newCacheDir, newOverlayDir);
            this.nestedOverlays.put(pathToEntryInOverlay, d);
            result = d;
        } else {
            //we couldn't build our directories..
            //need to exit here, to prevent users believing this
            //nested overlay was just 'missing';
            String msg1 = okOverlay ? "" : "Failed to create overlay directory [ " + newOverlayDir.getAbsolutePath() + " ]. ";
            String msg2 = okCache ? "" : "Failed to create cache directory [ " + newCacheDir.getAbsolutePath() + " ]";
            throw new IllegalStateException(msg1 + msg2);
        }

        return result;
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
    public static class StubEntry implements ArtifactEntry {
        private final String name;
        private final ArtifactContainer convertTo;

        public StubEntry(String name, ArtifactContainer convertTo) {
            this.name = name;
            this.convertTo = convertTo;
        }

        @Override
        public ArtifactContainer getEnclosingContainer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPath() {
            throw new UnsupportedOperationException();
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

        /** {@inheritDoc} */
        @Override
        public long getLastModified() {
            return 0L;
        }

        /** {@inheritDoc} */
        @Override
        public URL getResource() {
            return null;
        }

        /** {@inheritDoc} */
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
            throw new UnsupportedOperationException();
        }

        @Override
        public ArtifactContainer getEnclosingContainer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ArtifactEntry getEntryInEnclosingContainer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void useFastMode() {
            // EMPTY
        }

        @Override
        public void stopUsingFastMode() {
            // EMPTY
        }

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

        /** @throws UnsupportedOperationException */
        @Override
        public Collection<URL> getURLs() {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        public String getPhysicalPath() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public ArtifactNotifier getArtifactNotifier() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void useFastMode() {
        base.useFastMode();
    }

    @Override
    public void stopUsingFastMode() {
        base.stopUsingFastMode();
    }

    @Override
    public boolean isRoot() {
        return true; //should always be true.. constructor enforced.
    }

    @Override
    public ArtifactEntry getEntry(String pathAndName) {
        //added to make getEntry go faster for non-overlaid containers (most jar files)
        if (isPassThroughMode) {
            ArtifactEntry baseEntry = base.getEntry(pathAndName);
            if (baseEntry != null) {
                //note use of baseEntry.getPath to set the overlay entry path, pathAndName is unsanitized,
                //getPath will always be clean.
                return new OverlayDelegatingEntry(this, baseEntry, baseEntry.getName(), baseEntry.getPath(), fileOverlayContainer);
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
        if (!maskSet.contains(pathAndName)) {
            ArtifactEntry target = null;
            if (fileOverlayContainer != null) {
                target = fileOverlayContainer.getEntry(pathAndName);
            }
            if (target == null) {
                //if attempting to use overrides failed, try the base ArtifactContainer.
                target = base.getEntry(pathAndName);
            }
            //make sure we always wrapper the return, so we get to handle the
            //navigation calls.
            if (target != null) {
                //swapped from using pathAndName, to using getPath, to build path-sanitised overlay entries
                return new OverlayDelegatingEntry(this, target, target.getName(), target.getPath(), fileOverlayContainer);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public Iterator<ArtifactEntry> iterator() {
        return new EnhancedEntryIterator(this, base.getPath(), fileOverlayContainer);
    }

    @Override
    public ArtifactContainer getEnclosingContainer() {
        return enclosingContainer;
    }

    @Override
    public ArtifactEntry getEntryInEnclosingContainer() {
        return entryInEnclosingContainer;
    }

    @Override
    public String getPath() {
        return base.getPath();
    }

    @Override
    public String getName() {
        return "/";
    }

    @Override
    public ArtifactContainer getContainerBeingOverlaid() {
        return base;
    }

    @Override
    public void mask(String path) {
        this.isPassThroughMode = false;
        //notifier is created when the dir overlay path is set via setOverlayDirectory
        if (this.overlayNotifier == null) {
            throw new IllegalStateException("Null notifier");
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
        maskSet.add(path);
    }

    @Override
    public void unMask(String path) {

        //notifier is created when the dir overlay path is set via setOverlayDirectory
        if (this.overlayNotifier == null) {
            throw new IllegalStateException("Null notifier");
        }
        maskSet.remove(path);
        this.isPassThroughMode = maskSet.isEmpty();

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
        return maskSet.contains(path);
    }

    @Override
    public Set<String> getMaskedPaths() {
        return maskSet;
    }

    /**
     * Clones an ArtifactEntry to the overlay directory, by reading the data from the ArtifactEntry, and writing
     * it to the directory at the requested path, or converting it to a ArtifactContainer and processing
     * the entries recursively.<br>
     * Entries that convert to ArtifactContainers that claim to be isRoot true, are not processed recursively.
     *
     * @param e         The ArtifactEntry to add
     * @param path      The path to add the ArtifactEntry at
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
                    // FFDC
                }
            }
        }
        return true;
    }

    @Override
    public boolean addToOverlay(ArtifactEntry e) {
        //don't allow the add if we're not set with a dir yet!
        if (fileOverlayContainer == null)
            throw new IllegalStateException("Null overlay container");

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
        if (fileOverlayContainer == null)
            throw new IllegalStateException("Null overlay container");

        this.isPassThroughMode = false;

        try {
            return cloneEntryToOverlay(e, path, addAsRoot);
        } catch (IOException e1) {
            return false;
        }
    }

    /**
     * Recursively remove a file/directory.
     *
     * @param f
     * @return true if the delete succeeded, false otherwise
     */
    private boolean removeFile(File f) {
        if (Utils.fileIsFile(f)) {
            return Utils.deleteFile(f);
        } else {
            //delete directory f..
            boolean ok = true;
            //remove all the children..
            File children[] = Utils.listFiles(f);
            if (children != null) {
                for (File child : children) {
                    ok &= removeFile(child);
                }
            }
            //once children are deleted, remove the directory
            if (ok) {
                ok &= Utils.deleteFile(f);
            }
            return ok;
        }
    }

    @Override
    public synchronized boolean removeFromOverlay(String path) {
        //don't allow the remove if we're not set with a dir yet!
        if (fileOverlayContainer == null)
            throw new IllegalStateException("Null overlay container");

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

        if (fileOverlayContainer != null)
            return fileOverlayContainer.getEntry(path) != null;
        else
            return false;
    }

    /**
     * Little recursive routine to collect all the files present within a ArtifactContainer.<p>
     *
     * @param c The ArtifactContainer to process
     * @param s The set to add paths to.
     */
    private void collectPaths(ArtifactContainer c, Set<String> s) {
        if (!"/".equals(c.getPath())) {
            s.add(c.getPath());
        }
        for (ArtifactEntry e : c) {
            s.add(e.getPath());
            ArtifactContainer n = e.convertToContainer();
            if (n != null && !n.isRoot()) {
                collectPaths(n, s);
            }
        }
    }

    @Override
    public Set<String> getOverlaidPaths() {
        HashSet<String> s = new HashSet<String>();
        //not exactly a cheap operation...
        collectPaths(fileOverlayContainer, s);
        return s;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void setOverlayDirectory(File overlayCacheDir, File overlayDir) {
        // The 'isDirectory' includes an 'exists' test.

        if (!Utils.fileIsDirectory(overlayCacheDir)) {
            String path = overlayCacheDir.getAbsolutePath();
            if (!FileUtils.fileExists(overlayCacheDir)) {
                Tr.error(tc, "overlay.cache.does.not.exist", path);
                throw new IllegalArgumentException("Overlay cache [ " + path + " ] does not exist");
            } else {
                Tr.error(tc, "overlay.cache.not.directory", path);
                throw new IllegalArgumentException("Overlay cache [ " + path + " ] is not a directory");
            }
        }

        if (!Utils.fileIsDirectory(overlayDir)) {
            String path = overlayDir.getAbsolutePath();
            if (!FileUtils.fileExists(overlayDir)) {
                Tr.error(tc, "overlay.does.not.exist", path);
                throw new IllegalArgumentException("Overlay [ " + path + " ] does not exist");
            } else {
                Tr.error(tc, "overlay.not.directory", path);
                throw new IllegalArgumentException("Overlay [ " + path + " ] is not a directory");
            }
        }

        File overlay = new File(overlayDir, ".overlay");

        // The previous check.  This is too convoluted.

        // if (!(FileUtils.listFiles(overlayDir).length == 0 ||
        //       (FileUtils.fileExists(overlay) && FileUtils.fileIsDirectory(overlay)))) {
        //     throw new IllegalArgumentException();
        // }

        // If the overlay exists, a file listing is not needed, since the
        // overlay directory cannot be empty.
        //
        // If the overlay exists, it must be a directory.
        //
        // If the overlay does not exist, the overlay directory is required
        // to be empty.  (Note: We do not do the corresponding test if the
        // overlay exists, which would be that the overlay be the only
        // file in the overlay directory.)  It is not clear why the overlay
        // directory is required to have at most the overlay file.

        if (!checkOverlay(overlay)) {
            File[] overlayFiles = FileUtils.listFiles(overlayDir);
            if (overlayFiles.length == 0) {
                // Expected: The overlay directory is empty.  Create the overlay file.
                FileUtils.fileMkDir(overlay);

            } else {
                // Expected, but only if the overlay directory is a singleton and the
                // overlay file is the only element, and the overlay file exists and
                // is a directory

                // Normally, 'checkOverlay' cannot answer true, above, and false, here.
                // The value can change because multiple equal directory overlays can
                // exist, and these can concurrently create the overlay file.

                // Since the directory overlays are independent instances, normal
                // synchronization won't work.  A new "file name" based lock would be
                // needed.  As an (ugly) work-around, the code handles the concurrent
                // creation of the overlay file by doing 'checkOverlay' twice.

                boolean isValid;
                if (isValid = (overlayFiles.length == 1)) { // Maybe valid ...
                    File firstFile = overlayFiles[0];
                    if (isValid = firstFile.getName().equals(".overlay")) { // .. still maybe valid ...
                        if (!checkOverlay(firstFile)) { // No longer exists!
                            throw new IllegalStateException("Strange: Inconsistent overlay [ " + firstFile.getAbsolutePath() + " ]");
                        } else {
                            // Exists and is a directory ... valid.
                        }
                    }
                }
                if (!isValid) {
                    String path = overlayDir.getAbsolutePath();
                    Tr.error(tc, "overlay.not.empty", path);
                    throw new IllegalArgumentException("Overlay [ " + path + " ] is not empty");
                }
            }

        } else {
            // Expected: The overlay file exists and is a directory.
            //
            // We don't care if there are any other files, which is inconsistent
            // with the other branch.
        }

        this.overlayDirectory = overlay;
        this.cacheDirForOverlay = overlayCacheDir;

        this.fileOverlayContainer = cfHolder.getContainerFactory().getContainer(overlayCacheDir, overlay);

        //could be null if the file impl bundle is awol.. nasty.
        //this should never happen, so throw exception if it does.
        if (this.fileOverlayContainer == null) {
            throw new IllegalStateException("Failed to obtain overlay container for overlay [ " + overlay.getAbsolutePath() + " ]");
        }

        //initialise the overlay notifier.
        //if we have a parentOverlay, the parent must have been configured with the overlay dir..
        DirectoryBasedOverlayNotifier parentN = this.parentOverlay == null ? null : this.parentOverlay.overlayNotifier;
        this.overlayNotifier = new DirectoryBasedOverlayNotifier(this, fileOverlayContainer, parentN, this.entryInEnclosingContainer);

        Set<String> overlaid = this.getOverlaidPaths();
        if (overlaid.size() == 0) {
            this.isPassThroughMode = true;
        } else {
            this.isPassThroughMode = false;
        }
    }

    private boolean checkOverlay(File overlay) {
        if (FileUtils.fileExists(overlay)) {
            if (!FileUtils.fileIsDirectory(overlay)) {
                String path = overlay.getAbsolutePath();
                Tr.error(tc, "overlay.exists.not.directory", path);
                throw new IllegalArgumentException("Overlay [ " + path + " ] exists but is not a directory");
            } else {
                return true;
                // We don't care if the overlay directory has any other elements!
            }
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer getRoot() {
        return this;
    }

    @Override
    public Collection<URL> getURLs() {
        //merge the base uri's with the fileoverlay uri, put the base URIs in the front.
        Collection<URL> set = new LinkedHashSet<URL>();

        Collection<URL> baseUrls = this.base.getURLs();
        if (baseUrls != null)
            set.addAll(baseUrls);

        Collection<URL> overlay = this.fileOverlayContainer.getURLs();
        if (overlay != null)
            set.addAll(overlay);

        return set;
    }

    /** {@inheritDoc} */
    @Override
    public String getPhysicalPath() {
        return base.getPhysicalPath();
    }

    /** {@inheritDoc} */
    @Override
    public void addToNonPersistentCache(String path, Class owner, Object data) {
        cacheStore.addToCache(path, owner, data);
    }

    /** {@inheritDoc} */
    @Override
    public void removeFromNonPersistentCache(String path, Class owner) {
        cacheStore.removeFromCache(path, owner);
    }

    /** {@inheritDoc} */
    @Override
    public Object getFromNonPersistentCache(String path, Class owner) {
        return cacheStore.getFromCache(path, owner);
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactNotifier getArtifactNotifier() {
        if (this.overlayNotifier == null) {
            throw new IllegalStateException("Null overlay notifier");
        }
        return this.overlayNotifier;
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
        if (entryRootEntry == null) {
            return entryPath;
        }

        // Otherwise, walk upwards, concatenating the enclosing entry
        // paths.

        StringBuilder builder = new StringBuilder(entry.getPath());
        while (entryRootEntry != null) {
            builder.insert(0, '/');
            builder.insert(0, entryRootEntry.getPath());

            entryRootEntry = entryRootEntry.getRoot().getEntryInEnclosingContainer();
        }
        return builder.toString();
    }

    private Map<String, Map<Class, Object>> getCacheSnapshot() {
        return cacheStore.getSnapshot();
    }

    protected ArtifactContainer getFileOverlay() {
        return fileOverlayContainer;
    }

    public void introspect(PrintWriter outputWriter) {
        outputWriter.println();

        ArtifactEntry useEnclosingEntry = getEntryInEnclosingContainer();
        String enclosingIntrospect = "Directory Overlay Container [ %s ] [ %s ]";
        if (useEnclosingEntry == null) {
            outputWriter.println(String.format(enclosingIntrospect, "ROOT", this.toString()));
        } else {
            outputWriter.println(String.format(enclosingIntrospect, getFullPath(useEnclosingEntry), this.toString()));
        }

        if (cacheStore.isCacheEmpty()) {
            outputWriter.println("  ** EMPTY **");
        } else {
            Map<String, Map<Class, Object>> snapshot = getCacheSnapshot();
            for (Map.Entry<String, Map<Class, Object>> pathEntry : snapshot.entrySet()) {
                outputWriter.println(String.format("  [ %s ]", pathEntry.getKey()));
                Map<Class, Object> entriesForPath = pathEntry.getValue();
                if (entriesForPath.isEmpty()) {
                    outputWriter.println("      ** EMPTY **");
                } else {
                    String subEntryFormat = "      [ %s ] [ %s ]";
                    for (Map.Entry<Class, Object> subEntry : entriesForPath.entrySet()) {
                        outputWriter.println(String.format(subEntryFormat, subEntry.getKey(), subEntry.getValue() == null ? "** NULL **" : subEntry.getValue()));
                    }
                }
            }
        }
    }
}
