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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.Bundle;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * The bundle entry is an {@link Entry} inside a {@link Bundle} object. This should only be used for file entries within the bundle with the {@link BundleContainer} available for
 * directory entries.
 */
public class BundleEntry implements ArtifactEntry {

    protected final URL bundleUrl;

    protected final BundleArchive rootContainer;

    protected ArtifactContainer enclosingContainer;
    protected ArtifactEntry entryInEnclosingContainer;

    /**
     * Allows the construction of a new BundleEntry.
     * 
     * @param bundleUrl The URL within the {@link Bundle} pointing to this entry
     * @param rootContainer The root container that contains this entry
     */
    protected BundleEntry(URL bundleUrl, BundleArchive rootContainer) {
        super();
        this.bundleUrl = bundleUrl;
        this.rootContainer = rootContainer;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer convertToContainer() {
        return convertToContainer(false);
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer convertToContainer(boolean local) {
        if (!local) {
            // See if the container factory can create a container from us
            File newCacheDir = null;
            String relativeLocation = getEnclosingContainer().getPath();
            if (relativeLocation.equals("/")) {
                newCacheDir = rootContainer.getCacheDir();
            } else {
                //use of substring 1 is ok here, because this zip entry MUST be within a container, and the smallest path
                //as container can have is "/", which is dealt with above, therefore, in this branch the relativeLocation MUST 
                //be longer than "/"
                newCacheDir = new File(rootContainer.getCacheDir(), relativeLocation.substring(1));
            }
            //newCacheDir = new File(newCacheDir, this.getName());
            return this.rootContainer.getContainerFactory().getContainer(newCacheDir, this.getEnclosingContainer(), this, this.bundleUrl);
        } else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getInputStream() throws IOException {
        return this.bundleUrl.openStream();
    }

    /** {@inheritDoc} */
    @Override
    public long getLastModified() {
        try {
            return this.bundleUrl.openConnection().getLastModified();
        } catch (IOException e) {
            // FFDC and return 0
            return 0l;
        }
    }

    /** {@inheritDoc} */
    @Override
    public URL getResource() {
        return this.bundleUrl;
    }

    /** {@inheritDoc} */
    @Override
    public long getSize() {
        try {
            return this.bundleUrl.openConnection().getContentLength();
        } catch (IOException e) {
            // FFDC and return 0
            return 0l;
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer getEnclosingContainer() {
        // Lazily load the enclosing container, using the root container to get the entry
        if (this.enclosingContainer == null) {
            String parentPath = PathUtils.getParent(this.getPath());
            this.enclosingContainer = ("/".equals(parentPath)) ? this.rootContainer : this.rootContainer.getEntry(parentPath).convertToContainer();
        }
        return this.enclosingContainer;
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        // The path is always relative to the containing bundle so can use our URL to get this
        return this.bundleUrl.getPath();
    }

    /** Returns <code>null</code> */
    @Override
    public String getPhysicalPath() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer getRoot() {
        return this.rootContainer;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return PathUtils.getName(this.getPath());
    }

}
