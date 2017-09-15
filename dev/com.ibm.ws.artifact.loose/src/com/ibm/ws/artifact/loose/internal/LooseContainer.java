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
package com.ibm.ws.artifact.loose.internal;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.ws.artifact.loose.internal.LooseArchive.EntryInfo;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;

public class LooseContainer extends AbstractLooseEntity implements ArtifactContainer {
    public LooseContainer(LooseArchive looseArchive, EntryInfo ei, String pathAndName) {
        super(looseArchive, ei, pathAndName);
    }

    @Override
    public Iterator<ArtifactEntry> iterator() {
        return getParent().iterator(getPath());
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
        // no normalization required (we think) - ZipFileNestedDirContainer does the same thing 

        //if pathAndName starts with a "/" it is absolute in vfs, else add parent to make it absolute before returning
        if (pathAndName.startsWith("/")) {
            return getParent().getEntry(pathAndName);
        } else {
            return getParent().getEntry(getPath() + "/" + pathAndName);
        }
    }

    @Override
    public ArtifactContainer getRoot() {
        return getParent();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<URL> getURLs() {
        /*
         * There may be two directories pointing to this path so we need to get the URIs from both directories. Therefore just ask our parent for the URIs that point to us (this
         * will then include this object and any other directories that map to this)
         */
        return getParent().getURLs(getPath());
    }

    @Override
    public String getPhysicalPath() {
        /*
         * Use the entry info to get the physical path for this entry. If it's null then it means that this is a purely virtual entry (i.e. isBeneath returned true and matches
         * returned false for a dir entry beneath this one) in which case there is by definition no physical path to return
         */
        EntryInfo entryInfo = getEntryInfo();
        if (entryInfo != null) {
            return entryInfo.getFirstPhysicalPath(getPath());
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactNotifier getArtifactNotifier() {
        return getParent().getArtifactNotifier();
    }
}