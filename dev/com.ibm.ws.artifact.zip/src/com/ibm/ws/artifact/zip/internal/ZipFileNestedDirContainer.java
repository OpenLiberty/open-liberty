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
package com.ibm.ws.artifact.zip.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.zip.ZipEntry;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;

/**
 * Represents a directory node within Zip structured data.<p>
 * The directory may, or may not exist within the Zip, as Zip allows entries to be present at
 * paths without their parent directories being present as ZipEntries.<p>
 * This class is fairly lightweight, as it defers most of it's capability to the ZipFileContainer, or the Iterator.
 */
public class ZipFileNestedDirContainer implements ArtifactContainer {
    private final File archiveFile;
    private final ArtifactEntry entryInEnclosingContainer;
    private final ArtifactContainer enclosingContainer;
    private final ZipFileContainer rootContainer;
    private final String name;
    private final NavigableMap<String, ZipEntry> allEntries;
    private final ContainerFactoryHolder containerFactoryHolder;

    /**
     * Create a Container for the path & name given.
     * 
     * @param zc The ZipFileContainer this Container is part of.
     * @param c The Container enclosing this one, will not be null.
     * @param af The File underpinning this Zip (may be null if Zip is Entry based)
     * @param entryInEnclosingContainer The entry representing this Container in the enclosing container.
     * @param allEntries The map of all entries in the ZipFileContainer, path->zipentry
     * @param path The path of this container
     * @param name The name of this container
     */
    public ZipFileNestedDirContainer(ZipFileContainer zc, ArtifactContainer c, File af, ArtifactEntry entryInEnclosingContainer, NavigableMap<String, ZipEntry> allEntries,
                                     String name,
                                     ContainerFactoryHolder cfh) {
        this.enclosingContainer = c;
        this.rootContainer = zc;
        this.archiveFile = af;
        this.entryInEnclosingContainer = entryInEnclosingContainer;
        this.name = name;
        this.allEntries = allEntries;
        this.containerFactoryHolder = cfh;
    }

    @Override
    public Iterator<ArtifactEntry> iterator() {
        return new ZipFileEntryIterator(rootContainer, this, archiveFile, allEntries, getPath(), containerFactoryHolder);
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
        //determine this Entries path by using path for the enclosing container & adding our name            
        String path = enclosingContainer.getPath();
        if (!path.equals("/")) {
            path += '/';
        }
        path += getName();

        return path;
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
        return false;
    }

    @Override
    public ArtifactEntry getEntry(String pathAndName) {
        //pathAndName can be relative, or absolute.. 
        //convert to absolute & invoke the rootContainer getEntry
        // no normalization required (we think) - LooseContainer does the same thing
        if (pathAndName.startsWith("/")) {
            return rootContainer.getEntry(pathAndName);
        } else {
            return rootContainer.getEntry(getPath() + "/" + pathAndName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer getRoot() {
        return rootContainer;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<URL> getURLs() {
        // We are in a JAR so use the jar:<url>!/<path_in_jar> sytax for our URI
        try {
            URL entryUrl = rootContainer.createEntryUri(getPath() + "/", archiveFile).toURL();
            return Collections.singleton(entryUrl);
        } catch (MalformedURLException e) {
        }

        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public String getPhysicalPath() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactNotifier getArtifactNotifier() {
        return rootContainer.getArtifactNotifier();
    }

}
