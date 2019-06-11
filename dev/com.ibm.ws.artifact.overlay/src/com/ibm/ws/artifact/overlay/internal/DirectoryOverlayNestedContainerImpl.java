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

import java.net.URL;
import java.util.Collection;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * Implementation of a nested (non-root) overlay container.
 */
public class DirectoryOverlayNestedContainerImpl implements ArtifactContainer {
	/**
	 * Create a nested (non-root) overlay container.
	 *
	 * The path and name usually match the path and name of the base entry,
	 * but won't if a nested container was created on a root base entry.
	 *
	 * @param rootOverlayContainer The root of this new overlay container.
	 * @param resolvedContainer An entry from the root overlay container which
	 *     underlies this nested container.  The entry is from the base container
	 *     of the root container or is from the file container of the root
	 *     container.
	 * @param path The path assigned to the nested container.
	 * @param name The name assigned to the nested container.
	 */
    public DirectoryOverlayNestedContainerImpl(
    	DirectoryOverlayContainerImpl rootOverlayContainer,
    	ArtifactContainer resolvedContainer, String path, String name) {

        this.rootOverlayContainer = rootOverlayContainer;

        this.resolvedContainer = resolvedContainer;
        this.path = path;
        this.name = name;
    }

    @Override
    public String toString() {
    	return super.toString() + "(" + path + ", " + resolvedContainer + ")";
    }

    //

    /** The overlay container which encloses this nested container. */
    private final DirectoryOverlayContainerImpl rootOverlayContainer;

    /**
     * Tell if this container is a root container.
     * 
     * This implementation always answers false, since this is a nested container.
     * 
     * @return True or false telling if this container is a root container.  This
     *     implementation always answers false.
     */
    @Override
    public boolean isRoot() {
        // TODO: Answering the value from the resolved container is not
        //       consistent with the return value of 'getRoot', which
        //       answers the root overlay container.

        return false;
    }

    @Override
    public DirectoryOverlayContainerImpl getRoot() {
        return rootOverlayContainer;
    }

    @Override
    public DirectoryOverlayNotifierImpl getArtifactNotifier() {
        return rootOverlayContainer.getArtifactNotifier();
    }

    //

    /**
     * The nested contain which was obtained from either the
     * base container or the file overlay container of the root
     * container. 
     */
    private final ArtifactContainer resolvedContainer;

    private final String path;
    private final String name;

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getName() {
        return name;
    }

    // TODO: These fast mode implementations seem wrong, since
    //       iteration is performed within the space of the root overlay.

    @Override
    public void useFastMode() {
        resolvedContainer.useFastMode();
    }

    @Override
    public void stopUsingFastMode() {
        resolvedContainer.stopUsingFastMode();
    }

    //

    @Override
    public DirectoryOverlayIteratorImpl iterator() {
        return new DirectoryOverlayIteratorImpl(rootOverlayContainer, path);
    }

    @Override
    public ArtifactEntry getEntry(String nestedPath) {
        if ( nestedPath.charAt(0) != '/' ) {
        	nestedPath = getPath() + "/" + nestedPath;
        }
        return rootOverlayContainer.getEntry(nestedPath);
    }

    @Override
    public ArtifactContainer getEnclosingContainer() {
        String parentPath = PathUtils.getParent(path);
        if ( (parentPath.length() == 1) && (parentPath.charAt(0) == '/') ) {
        	return rootOverlayContainer;
        } else {
        	ArtifactEntry parentEntry = rootOverlayContainer.getEntry(parentPath);
        	return parentEntry.convertToContainer();
        }
    }

    @Override
    public ArtifactEntry getEntryInEnclosingContainer() {
    	return rootOverlayContainer.getEntry(path);
    }

    //

    @Override
    public Collection<URL> getURLs() {
        return resolvedContainer.getURLs();

        // TODO: Neither the updated implementation nor the removed
        //       implementation is correct.
        //
        //       The URLs might properly be a base URLs adjusted to the
        //       current nesting plus a file overlay URL adjusted to the
        //       current nesting.
        //
        //       The URLs might also be the URL of a base JAR plus a relative
        //       path, plus a file overlay URL adjusted to the current nesting.
        //
        //       In a pass through case, the URLs would be just the base URLs.
        //
        //       Added file URLs could be empty, if the current nesting moves
        //       outside of the file overlay directory structure.
        //
        // Collection<URL> base = delegate.getURLs();
        // HashSet<URL> set = new HashSet<URL>();
        // if (base != null)
        //     set.addAll(base);
        // ArtifactEntry e = fileContainer.getEntry(getPath());
        // if (e != null) {
        //     ArtifactContainer correspondingFileContainer = e.convertToContainer();
        //     if (correspondingFileContainer != null) {
        //         Collection<URL> fset = correspondingFileContainer.getURLs();
        //         if (fset != null) {
        //             set.addAll(fset);
        //         }
        //     }
        // }
        // return set;
    }

    @SuppressWarnings({ "deprecation" })
	@Override
    public String getPhysicalPath() {
        return resolvedContainer.getPhysicalPath();

        // TODO: Neither the updated implementation nor the removed
        //       implementation is correct.  See the comment on 'getURLs'
        //       for a discussion.

        // String path = delegate.getPhysicalPath();
        // ArtifactEntry e = fileContainer.getEntry(getPath());
        // if (e != null) {
        //     ArtifactContainer correspondingFileContainer = e.convertToContainer();
        //     if (correspondingFileContainer != null) {
        //         String cpath = correspondingFileContainer.getPhysicalPath();
        //         if (cpath != null)
        //             path = cpath;
        //     }
        // }
        // return path;
    }
}