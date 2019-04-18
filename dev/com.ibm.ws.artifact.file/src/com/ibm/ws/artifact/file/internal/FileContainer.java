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
package com.ibm.ws.artifact.file.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;

import com.ibm.ws.artifact.file.ContainerFactoryHolder;
import com.ibm.ws.artifact.file.FileArtifactNotifier;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * Represents an artifact api Container, over a java.io.File directory.
 */
public class FileContainer implements com.ibm.wsspi.artifact.ArtifactContainer {

    private final ContainerFactoryHolder containerFactoryHolder;
    private final ArtifactContainer parent;
    private final ArtifactEntry thisInParent;
    private final File dir;

    private final File cacheDir;

    private final boolean isRoot;
    private final FileContainer root;

    private final ArtifactNotifier artifactNotifier;

    /**
     * Used to build a FileContainer for a File, not present within an enclosing container.
     *
     * @param cacheDir location to host this containers cached data.
     * @param f the File to use.
     * @param c somewhere to obtain the current container factory from.
     */
    FileContainer(File cacheDir, File f, ContainerFactoryHolder c) {
        this.cacheDir = cacheDir;
        dir = f;
        isRoot = true;
        root = this;
        artifactNotifier = new FileArtifactNotifier(this, c, dir.getAbsolutePath());
        thisInParent = null;
        parent = null;
        containerFactoryHolder = c;
    }

    /**
     * Builds a FileContainer that is enclosed by another Container instance.
     *
     * @param cacheDir location for this container to use for cache data.
     * @param parent the enclosing ArtifactContainer.
     * @param e the ArtifactEntry in the enclosing ArtifactContainer representing this ArtifactContainer.
     * @param f the File object on disk representing this ArtifactContainer.
     */
    FileContainer(File cacheDir, ArtifactContainer parent, ArtifactEntry e, File f, ContainerFactoryHolder c) {
        this.cacheDir = cacheDir;
        dir = f;
        this.parent = parent;
        this.thisInParent = e;
        isRoot = true;
        root = this;
        artifactNotifier = new FileArtifactNotifier(this, c, dir.getAbsolutePath());
        containerFactoryHolder = c;
    }

    /**
     * Builds a FileContainer enclosed by another Container, where the Container created
     * may be a non-root Container.
     *
     * @param parent the enclosing Container.
     * @param e the entry in the enclosing Container representing this Container.
     * @param f the File object on disk representing this Container.
     * @param isRoot true if this Container should be treated as a root Container, false otherwise.
     * @param root the Container representing root for this container hierarchy.
     */
    FileContainer(File cacheDir, ArtifactContainer parent, ArtifactEntry e, File f, ContainerFactoryHolder c, boolean isRoot, FileContainer root) {
        this.cacheDir = cacheDir;
        this.dir = f;
        this.parent = parent;
        this.thisInParent = e;
        this.isRoot = isRoot;
        if (isRoot) {
            this.root = this;
            artifactNotifier = new FileArtifactNotifier(this, c, dir.getAbsolutePath());
        } else {
            this.root = root;
            this.artifactNotifier = root.artifactNotifier;
        }
        containerFactoryHolder = c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRoot() {
        return isRoot;
    }

    /**
     * Small class that allows iteration of Entries within a FileContainer.
     */
    private static class FileEntryIterator implements Iterator<ArtifactEntry> {
        final Iterator<File> children;
        final ArtifactContainer enclosingContainer;
        final ContainerFactoryHolder containerFactoryHolder;
        final FileContainer root;

        FileEntryIterator(ArtifactContainer c, File f, FileContainer r, ContainerFactoryHolder cfh) {
            File childs[] = FileUtils.listFiles(f);
            if (childs != null) {
                children = Collections.unmodifiableList(Arrays.asList(childs)).iterator();
            } else {
                //the directory this iterator refers to has been deleted..
                //so we cannot iterate it.. we'll use an empty collection to maintain
                //consistency.
                children = Collections.unmodifiableList(new ArrayList<File>()).iterator();
            }
            enclosingContainer = c;
            containerFactoryHolder = cfh;
            root = r;
        }

        @Override
        public boolean hasNext() {
            return children.hasNext();
        }

        @Override
        public ArtifactEntry next() {
            return new FileEntry(enclosingContainer, children.next(), root, containerFactoryHolder);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public Iterator<ArtifactEntry> iterator() {
        return new FileEntryIterator(this, dir, root, containerFactoryHolder);
    }

    @Override
    public ArtifactContainer getEnclosingContainer() {
        return parent;
    }

    @Override
    public ArtifactEntry getEntryInEnclosingContainer() {
        return thisInParent;
    }

    @Override
    public String getPath() {
        if (!isRoot) {
            return thisInParent.getPath();
        } else {
            return "/";
        }
    }

    @Override
    public String getName() {
        if (!isRoot) {
            return dir.getName();
        } else {
            return "/";
        }
    }

    @Override
    public void stopUsingFastMode() {}

    @Override
    public void useFastMode() {}

    @Override
    public ArtifactEntry getEntry(String pathAndName) {
        //safeguard..
        //check the path is not trying to leave the archive
        pathAndName = PathUtils.normalizeUnixStylePath(pathAndName);

        if (pathAndName.equals("/") || pathAndName.equals("")) {
            return null;
        }

        //trim trailing /'s
        if (pathAndName.endsWith("/")) {
            pathAndName = pathAndName.substring(0, pathAndName.length() - 1);
        }

        if (isRoot) {
            if (!PathUtils.isNormalizedPathAbsolute(pathAndName))
                return null;
        }

        // If the first element is trying to go up a level then do this now, if we are root we'll of already thrown an exception by now
        if (pathAndName.startsWith("../")) {
            return parent.getEntry(pathAndName.substring(3));
        } else if ("..".equals(pathAndName)) {
            //if the parent is root.. we can't get an Entry that represents it.
            if (parent.isRoot()) {
                return null;
            }
            return new FileEntry(parent.getEnclosingContainer(), dir.getParentFile(), root, containerFactoryHolder);
        }

        //quick test..
        File target = new File(dir, pathAndName);
        if ((!isRoot && !pathAndName.startsWith("/")) || isRoot) {
            //if path is relative, and we're not root, then validate path
            //or if we are root, just validate it anyways.
            if (!FileUtils.fileExists(target) || !PathUtils.checkCase(target, pathAndName)) {
                //no file/dir ? bug out early.
                return null;
            }
        }

        //if there's no / in the name.. it's immediately relative to here.. build & return it.
        if (pathAndName.indexOf("/") == -1) {
            if (FileUtils.fileExists(target) && PathUtils.checkCase(target, pathAndName))
                return new FileEntry(this, target, root, containerFactoryHolder);
            else
                return null;
        }

        //if it starts with a / it's an absolute path, so we walk back up our
        //parent chain until we find the one claiming to be our local root,
        //then invoke getEntry there with the path =)
        if (pathAndName.startsWith("/")) {
            ArtifactContainer c = this;
            while (c != null && !c.isRoot()) {
                c = c.getEnclosingContainer();
            }
            return c == null ? null : c.getEntry(pathAndName.substring(1));
        }

        //else.. it's a relative request to a non-root node..
        //  or.. it's a relative nested request to the root node..

        //the request is valid, but we have to create the chain of Containers that
        //link from this node, to the Entry being returned.

        File top = dir;
        File container = target;
        Deque<File> s = new ArrayDeque<File>();

        //navigate upwards from this Node, pushing the nodes to a Stack as we go.
        while (!container.equals(top)) {
            s.push(container);
            container = container.getParentFile();
        }

        //pop the stack until empty, creating the Container & Entries as required
        ArtifactContainer p = this;
        ArtifactEntry e = null;
        File f = s.pop();
        //process the stack until empty.
        while (f != null) {

            if (FileUtils.fileIsDirectory(f)) {
                //if it's a dir, build a Container for it
                if (!s.isEmpty()) {
                    //if the stack isnt empty, we're still going upwards, so make a Container
                    //to wrap the entry for the current level.
                    p = new FileContainer(cacheDir, p, new FileEntry(p, f, root, containerFactoryHolder), f, containerFactoryHolder, false, root); //false because cant be root!
                } else {
                    //if the stack is empty, this is the last layer to be created, end on an Entry.
                    e = new FileEntry(p, f, root, containerFactoryHolder);
                }
            } else if (FileUtils.fileIsFile(f)) {
                //if its a file, build an Entry for it.
                e = new FileEntry(p, f, root, containerFactoryHolder);
            }

            //pop the stack into f, or make f null if stack is empty.
            if (!s.isEmpty()) {
                f = s.pop();
            } else {
                f = null;
            }
        }

        return e;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer getRoot() {
        return root;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<URL> getURLs() {
        // There is only ever a single URI for the directory so return this
        try {
            return Collections.singleton(dir.toURI().toURL());
        } catch (MalformedURLException e) {
            return Collections.emptySet();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getPhysicalPath() {
        return dir.getAbsolutePath();
    }

    //package protected method used by entries to obtain the cachedir.
    File getCacheDir() {
        return this.cacheDir;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactNotifier getArtifactNotifier() {
        return artifactNotifier;
    }
}
