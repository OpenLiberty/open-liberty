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
import java.net.URL;

import com.ibm.ws.artifact.ExtractableArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * Entry implementation for overlay containers.
 */
public class DirectoryOverlayEntryImpl implements ExtractableArtifactEntry {
    /**
     * Create an entry for a root overlay container.
     * 
     * The entry is one which is not masked by the overlay container, and
     * which is in either the file overlay container or the base container.
     *
     * @param rootOverlayContainer The root overlay container.
     * @param resolvedEntry An entry from the file overlay container or the
     *     base container.
     */
    public DirectoryOverlayEntryImpl(
        DirectoryOverlayContainerImpl rootOverlayContainer,
        ArtifactEntry resolvedEntry) {

        this.rootOverlayContainer = rootOverlayContainer;
        this.resolvedEntry = resolvedEntry;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getPath() + ", " + resolvedEntry.toString() + ")";
    }

    //

    private final DirectoryOverlayContainerImpl rootOverlayContainer;

    @Override
    public ArtifactContainer getRoot() {
        return rootOverlayContainer;
    }

    //

    private final ArtifactEntry resolvedEntry;

    @Override
    public String getPath() {
        return resolvedEntry.getPath();
    }

    @Override
    public String getName() {
        return resolvedEntry.getName();
    }

    @Override
    public long getSize() {
        return resolvedEntry.getSize();
    }

    @Override
    public long getLastModified() {
        return resolvedEntry.getLastModified();

        // Removed.
        //
        // 'baseOrFileEntry' already encodes the resolution of
        // this entry to a base entry or to a file entry.
        //
        // Retrieval of the resource from the resolved entry is correct.
        //
        // Note that 'getInputStream' and 'setSize' call through to the
        // resolved entry.

        // long lastmod = delegate.getLastModified();
        // ArtifactEntry e = fileContainer.getEntry(getPath());
        // if (e != null) {
        // long clastmod = e.getLastModified();
        // if (clastmod != 0L)
        //     lastmod = clastmod;
        // }
        // return lastmod;
    }

    //

    @Override
    public InputStream getInputStream() throws IOException {
        return resolvedEntry.getInputStream();
    }

    @Override
    public URL getResource() {
        return resolvedEntry.getResource();

        // Removed. See the comment on 'getLastModified'.

        // URL resource = null;
        // ArtifactEntry e = fileContainer.getEntry(getPath());
        // if (e != null) {
        //     resource = e.getResource();
        // }
        // if (resource == null) {
        //     resource = delegate.getResource();
        // }
        // return resource;
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getPhysicalPath() {
        return resolvedEntry.getPhysicalPath();
        // Removed. See the comment on 'getLastModified'.

        // String path = delegate.getPhysicalPath();
        // ArtifactEntry e = fileContainer.getEntry(getPath());
        // if (e != null) {
        //     String cpath = e.getPhysicalPath();
        //     if (cpath != null)
        //         path = cpath;
        // }
        // return path;
    }

    /**
     * Extract this entry.
     * 
     * Delegate extraction to the underly entry.
     * 
     * Answer the extracted file.
     * 
     * Answer null and do not extract if the underlying entry is not
     * an extractable entry (type {@link ExtractableArtifactEntry}).
     * 
     * @return The extracted file.  Null if extraction was not performed.
     * 
     * @throws IOException Thrown if an error occurred while extracting
     *     the entry.
     */
    @Override
    public File extract() throws IOException {
        if ( resolvedEntry instanceof ExtractableArtifactEntry ) {
            return ((ExtractableArtifactEntry) resolvedEntry).extract();
        }
        return null;
    }

    //

    /**
     * Answer the container which encloses this entry.  The root overlay
     * container will be answered if this entry is immediately beneath the
     * root overlay container.  Otherwise, a new nested container will be
     * created.
     *
     * @return The container which encloses this entry.
     */
    @Override
    public ArtifactContainer getEnclosingContainer() {
        String parentPath = PathUtils.getParent( getPath() );
        if ( (parentPath.length() == 1) && (parentPath.charAt(0) == '/') ) {
            return rootOverlayContainer;
        } else {
            DirectoryOverlayEntryImpl enclosingEntry = rootOverlayContainer.getEntry(parentPath);
            return enclosingEntry.convertToLocalContainer();
        }
    }

    //

    /**
     * Convert this entry into a container.  Allow nested root overlay containers to
     * be created.
     * 
     * See {@link #convertToNonLocalContainer()}.
     *
     * @return The entry converted to a nested container.  Null if the entry does not
     *     convert to a container.
     */
    @Override
    public ArtifactContainer convertToContainer() {
        return convertToNonLocalContainer();
    }

    public static final boolean NONLOCAL_ALLOWED = false;
    public static final boolean LOCAL_ONLY = true;

    /**
     * Convert this entry into a container.
     *
     * If restricted by the control parameter, always create a local nested container,
     * regardless of whether the converted entry is a root container.
     *
     * If unrestricted, create a local nested container if the converted entry is not
     * a root container, otherwise, create a nested root overlay container.
     *
     * Nested root overlay containers are cached by the root overlay container: At most
     * will be created for each entry.
     *
     * Nested non-root overlay containers are not cached: Each conversion will obtain
     * a new container instance.
     *
     * See {@link #convertToNonLocalContainer()} and {@link #convertToLocalContainer()}.
     *
     * @param localOnly Control parameter: Tell if the converted container is restricted
     *     to being a local nested container.
     *
     * @return The entry converted to a nested container.  Null if the entry does not
     *     convert to a container.
     */
    @Override
    public ArtifactContainer convertToContainer(boolean localOnly) {
    	return ( localOnly ? convertToLocalContainer() : convertToNonLocalContainer() );
    }

    /**
     * Attempt to convert this entry to a nested container.  Convert according
     * to the conversion of the resolved entry:
     * 
     * Answer null if the resolved entry does not convert to a container.
     * 
     * Answer a non-root nested overlay container if the resolved entry does not convert
     * to a nested root container.
     * 
     * Answer a root nested overlay container if the resolved entry converts to a nested
     * root container.
     *
     * @return The entry converted to a nested container.
     */
    protected ArtifactContainer convertToNonLocalContainer() {
        ArtifactContainer resolvedContainer = resolvedEntry.convertToContainer(NONLOCAL_ALLOWED);
        if ( resolvedContainer == null ) {
            return null;
        }

        if ( !resolvedContainer.isRoot() ) {
        	return rootOverlayContainer.getNestedOverlay( resolvedContainer, getPath(), getName() );
        } else {
            return rootOverlayContainer.getNestedRootOverlay( resolvedContainer, this, getPath() );
        }
    }

    /**
     * Attempt to convert this entry to a local nested container.  Convert according to the
     * conversion of the resolved entry.
     * 
     * Answer null if the resolved entry does not convert to a local nested container.
     * 
     * Answer a local nested container if the resolved entry resolves to a local nested
     * container.
     *
     * @return This entry converted to a local nested container.
     */
    protected DirectoryOverlayNestedContainerImpl convertToLocalContainer() {
        ArtifactContainer baseOrFileContainer = resolvedEntry.convertToContainer(LOCAL_ONLY);
        if ( baseOrFileContainer == null ) {
            return null;
        } else {
        	return rootOverlayContainer.getNestedOverlay( baseOrFileContainer, getPath(), getName() );
        }
    }
}
