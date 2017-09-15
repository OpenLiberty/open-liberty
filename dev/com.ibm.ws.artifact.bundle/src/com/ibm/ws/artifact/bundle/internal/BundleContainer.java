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
package com.ibm.ws.artifact.bundle.internal;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.osgi.framework.Bundle;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;

/**
 * This represents a container within a bundle. It also implements the Entry interface as Containers within a bundle are never root objects so are also entries. The root of a
 * bundle is represented by {@link BundleArchive}.
 */
public class BundleContainer extends BundleEntry implements ArtifactContainer, ArtifactEntry {

    /**
     * Constructs a new instance of this class.
     * 
     * @param bundleUrl The URL within the {@link Bundle} pointing to this entry
     * @param rootContainer The root container that contains this entry
     */
    protected BundleContainer(URL bundleUrl, BundleArchive rootContainer) {
        super(bundleUrl, rootContainer);
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactEntry getEntry(String pathAndName) {
        pathAndName = ('/' == pathAndName.charAt(0)) ? pathAndName : this.getPath() + "/" + pathAndName;
        return this.getRoot().getEntry(pathAndName);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<URL> getURLs() {
        return Collections.singleton(bundleUrl);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRoot() {
        // Can never be root because BundleArchive is always the root of a bundle archive
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void stopUsingFastMode() {
        // No-op
    }

    /** {@inheritDoc} */
    @Override
    public void useFastMode() {
        // No-op
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<ArtifactEntry> iterator() {
        // Use the utility on bundle archive that will get an iterator for any level of the archive
        return this.rootContainer.iterator(this.getPath());
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer convertToContainer() {
        return convertToContainer(false);
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer convertToContainer(boolean local) {
        //we are always a local container, so do not need to worry about the local flag.

        // We are an Entry and a Container so just return ourself
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        // Overridden to strip the "/" off the end. We only ever create a BundleContainer if the URL ends in a "/" so we can do this safely
        String pathWithSlash = super.getPath();
        return pathWithSlash.substring(0, pathWithSlash.length() - 1);
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactEntry getEntryInEnclosingContainer() {
        // Lazily load the enclosing container, using the root container to get the entry
        if (this.entryInEnclosingContainer == null) {
            //this should work, because getPath should NEVER return "/" for a BundleContainer
            //(as "/" in a Bundle is a BundleArchive)
            this.entryInEnclosingContainer = getEntry(getPath());
        }
        return this.entryInEnclosingContainer;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactNotifier getArtifactNotifier() {
        return rootContainer.getArtifactNotifier();
    }
}
