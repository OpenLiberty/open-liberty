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
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.Set;
import java.util.zip.ZipEntry;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * Iterator for Entries, that works for Zip contained data.
 * <p>
 * Class functions by relying on the allEntries NavigableMap and the Path, to locate all entries 'below' the
 * path we are iterating at, and then cutting those back to a set of just the 1st level beneath that path.
 */
public class ZipFileEntryIterator implements Iterator<ArtifactEntry> {
    private final ContainerFactoryHolder containerFactoryHolder;
    private final File archiveFile;

    private final ArtifactContainer enclosingContainer;
    private final ZipFileContainer rootContainer;
    private final String path;
    private final Set<String> pathSubSet = new HashSet<String>();
    private final Iterator<String> pathSubiter;
    private final NavigableMap<String, ZipEntry> allEntries;

    /**
     * Build an iterator for entries in allEntries immediately below path
     * 
     * @param zc The ZipFileContainer representing the archive this iterator is using data from
     * @param c The Container being iterated
     * @param f The File on disk holding the zip data (may be null for non file based zips)
     * @param allEntries The sorted map of all entries in the Zip, Path->ZipEntry
     * @param path The path to iterate at
     */
    ZipFileEntryIterator(ZipFileContainer zc, ArtifactContainer c, File f, NavigableMap<String, ZipEntry> allEntries, String absolutePath,
                         ContainerFactoryHolder cfh) {
        this.enclosingContainer = c;
        this.rootContainer = zc;
        this.archiveFile = f;
        this.path = absolutePath;
        this.allEntries = allEntries;
        this.containerFactoryHolder = cfh;

        //Use submap to obtain a set of all paths that would exist at or below the requested path.
        //This works because the allEntries map is sorted using our comparator, which means when we
        //request the subMap from 'path' to 'path+0' we receive the set of all paths which are
        //either below the requested path in the path hierarchy, or are equal to the requested path.
        //
        //"blah" + 0 is the next possible directory name in alphabetical order: 
        //we want to find everything up to but not including the next directory, 
        //so anything *strictly* between "blah" and "blah" + 0 in the order created by the comparator
        //is a child of "blah"
        String firstKey;
        String lastKey;
        String rootPath = this.path.substring(1); // remove leading /
        boolean includeLast = false;
        boolean includeFirst = false;
        if (path.length() == 1) {
            firstKey = allEntries.firstKey();
            lastKey = allEntries.lastKey();
            includeFirst = true;
            includeLast = true;
        } else {
            firstKey = rootPath;
            lastKey = firstKey + 0;
        }
        for (String e : allEntries.subMap(firstKey, includeFirst, lastKey, includeLast).keySet()) {
            //grab the name of the element just below the current path.
            String name = PathUtils.getChildUnder(e, rootPath);
            this.pathSubSet.add(name);
        }

        //Use the set of immediate children of path as our underlying iterator.
        this.pathSubiter = pathSubSet.iterator();
    }

    @Override
    public boolean hasNext() {
        return pathSubiter.hasNext();
    }

    @Override
    public ArtifactEntry next() {
        String name = pathSubiter.next();

        String pathAndName = path;
        if (path.length() > 1)
            pathAndName += '/';
        pathAndName += name;
        String absolutePath = pathAndName;

        pathAndName = pathAndName.substring(1);
        ZipEntry ze = allEntries.get(pathAndName);
        if (ze != null) {
            //we have a real entry..
            return new ZipFileEntry(rootContainer, ze, name, absolutePath, archiveFile, allEntries, containerFactoryHolder);
        } else {
            //we need to create a fake entry to represent
            //a directory within the zip that was lacking a zipentry

            //zipfileentry is built to do this when null is passed as its entry.
            return new ZipFileEntry(rootContainer, null, name, absolutePath, archiveFile, allEntries, containerFactoryHolder);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
