/*******************************************************************************
 * Copyright (c) 2011,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.internal.ZipFileContainerUtils.ZipEntryData;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;

/**
 * A nested container of a zip file type container.
 */
public class ZipFileNestedDirContainer implements ArtifactContainer {
    /**
     * Create a nested (non-root) zip file type container.  The container
     * represents a directory within a zip file.
     *
     * A directory entry may be explicit in the root zip file, in which case
     * no entries need be present within the directory, meaning, an iterator
     * created on an explicit directory may be empty.
     *
     * A directory entry may be implied by the presence of an entry for
     * which the directory is a parent.  An implied directory must have
     * at least one child entry.
     *
     * Nested zip file containers are created directly from zip file entries.
     *
     * @param rootContainer The root container of this nested container.
     * @param entryInEnclosingContainer The entry which was interpreted
     *     to create this container.
     * @param name The name of the entry from which the container was created.
     * @param a_path The absolute path to the entry from which the container
     *     was created.
     */
    public ZipFileNestedDirContainer(
        ZipFileContainer rootContainer,
        ZipFileEntry entryInEnclosingContainer,
        String name, String a_path) {

        this.rootContainer = rootContainer;

        this.entryInEnclosingContainer = entryInEnclosingContainer;

        this.name = name;
        this.a_path = a_path;
    }

    // Root container ...
    //
    // Most of the behavior of zip file nested containers derives from
    // their root container and their relative path.

    private final ZipFileContainer rootContainer;

    @Trivial
    @Override
    public boolean isRoot() {
        return false;
    }

    @Trivial
    @Override
    public ZipFileContainer getRoot() {
        return rootContainer;
    }

    @Trivial
    @Override
    public ArtifactNotifier getArtifactNotifier() {
        return rootContainer.getArtifactNotifier();
    }

    // The local name and absolute path
    //
    // Storage of both paths adds overhead.  Most containers should
    // have many fewer nested containers than they have entries, in
    // which case the added overhead should be reasonable.

    private final String name;
    private final String a_path;

    @Trivial
    @Override
    public String getName() {
        return name;
    }

    @Trivial
    @Override
    public String getPath() {
        return a_path;
    }

    @Trivial
    public String getAbsolutePath() {
        return a_path;
    }

    @Trivial
    public String getRelativePath() {
        return a_path.substring(1);
    }

    @Override
    @Deprecated
    public String getPhysicalPath() {
        // TODO:
        //
        // Should this call through to the zip entry of the container?
        //
        // A return value of null currently is correct, since zip
        // entries always answer null for their physical path.  However,
        // zip entries can be extracted, and their implementation of
        // 'getPhysicalPath' might change to answer the extraction
        // location.  If the implementation is changed in this way,
        // the implementation of 'getPhysicalPath' for nested directory
        // zip containers might need to be changed.
        return null;
    }

    //

    private final ZipFileEntry entryInEnclosingContainer;

    /**
     * Answer the immediately enclosing container.
     *
     * This will be a {@link ZipFileContainer} or a
     * {@link ZipFileNestedDirContainer}.
     *
     * @return The immediately enclosing container.
     */
    @Trivial
    @Override
    public ArtifactContainer getEnclosingContainer() {
        return getEntryInEnclosingContainer().getEnclosingContainer();
    }

    /**
     * Answer the entry which was interpreted to create this nested container.
     * An non-null entry will always be available, even for implied containers.
     *
     * @return The entry which was interpreted to create this nested container.
     */
    @Trivial
    @Override
    public ZipFileEntry getEntryInEnclosingContainer() {
        return entryInEnclosingContainer;
    }

    //

    @Trivial
    @Override
    public Iterator<ArtifactEntry> iterator() {
        ZipEntryData[] allEntryData = rootContainer.getZipEntryData();
        if ( allEntryData.length == 0 ) {
            return Collections.emptyIterator();
        } else {
            Map<String, ZipFileContainerUtils.IteratorData> allIteratorData = rootContainer.getIteratorData();
            ZipFileContainerUtils.IteratorData thisIteratorData = allIteratorData.get( getRelativePath() );

            if ( thisIteratorData == null ) {
                return Collections.emptyIterator();
            } else {
                return new ZipFileContainerUtils.ZipFileEntryIterator(rootContainer, this, allEntryData, thisIteratorData);
            }
        }
    }

    // Entry access ...

    @Trivial
    @Override
    public void useFastMode() {
        getRoot().useFastMode();
    }

    @Trivial
    @Override
    public void stopUsingFastMode() {
        getRoot().stopUsingFastMode();
    }

    @Override
    public ZipFileEntry getEntry(String entryPath) {
        String a_entryPath;
        if ( entryPath.isEmpty() || (entryPath.charAt(0) != '/') ) {
            a_entryPath = getAbsolutePath() + "/" + entryPath;
        } else {
            a_entryPath = entryPath;
        }
        return getRoot().getEntry(a_entryPath);
    }

    /**
     * Answer the URLs of this entry.  See {@link ZipFileContainer#createEntryUri}
     * for details.
     *
     * Answer a singleton, unless the archive and entry values cause a malformed
     * URL to be created. If a malformed URL is created, answer an empty collection.
     *
     * @return The collection of URLs of this entry.
     */
    @Override
    public Collection<URL> getURLs() {
        try {
            // As a container URL, the URL must end with a trailing slash.
            URL entryUrl = rootContainer.createEntryUri( getRelativePath() + "/" ).toURL();
            return Collections.singleton(entryUrl);
        } catch ( MalformedURLException e ) {
            // FFDC
            return Collections.emptyList();
        }
    }
}
