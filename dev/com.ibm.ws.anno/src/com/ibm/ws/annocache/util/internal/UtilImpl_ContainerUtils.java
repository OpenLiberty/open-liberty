/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.util.internal;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;

public class UtilImpl_ContainerUtils {
    /**
     * Answer the root-of-roots of a target container.
     *
     * @param container The container from which to obtain the root-of-roots.
     *
     * @return The root-of-roots of a target container.
     */
    public static ArtifactContainer getRootOfRoots(ArtifactContainer container) {
        ArtifactEntry entry = container.getEntryInEnclosingContainer();
        while ( entry != null ) {
            container = container.getRoot();
            entry = container.getEntryInEnclosingContainer();
        }
        return container;
    }

    /**
     * Answer the full path of a container from the root-of-roots.  Answer
     * "/" if the container is the root-of-roots.
     * 
     * @param container The container for which to obtain the path.
     * 
     * @return The full path of the container from the root-of-roots.
     */
    public static String getFullInternalPath(ArtifactContainer container) {
        ArtifactEntry entry = container.getEntryInEnclosingContainer();
        if ( entry == null ) {
            return "/";
        }

        StringBuilder pathBuilder = new StringBuilder();
        while ( entry != null ) {
            pathBuilder.insert(0,  entry.getPath());
            container = entry.getRoot();
            entry = container.getEntryInEnclosingContainer();
        }

        return pathBuilder.toString();
    }

    /**
     * Answer the external path of a container.  Usually, the container
     * is a root-of-roots.  Obtain the external path from the URL of the
     * container, which must have a file URL.  If the container has more
     * than one URL, use the first URL to otain the external path.
     * 
     * The external path is the canonical path of the file created using the
     * path of the container's URL.
     *
     * @param container The container for which to obtain an external path.
     * 
     * @return The external path of the container.
     */
    public static String getExternalPath(ArtifactContainer container) {
        Collection<URL> urls = container.getURLs();
        if ( urls.isEmpty() ) {
            return null;
        }
        URL firstUrl = null;
        for ( URL url : urls ) {
            firstUrl = url;
            break;
        }

        // 'firstUrl' will always be assigned, due to the 'urls.isEmpty()' check.
        @SuppressWarnings("null")
		String firstPath = firstUrl.getPath();
        File firstFile = new File(firstPath);

        return UtilImpl_FileUtils.getCanonicalPath(firstFile);
    }

    /**
     * Answer the full external path of a container.  This is the external
     * path of the root-of-roots of the container plus the full internal
     * path of the container.  (Or, just the external path when the container
     * is a root-of-roots.)
     * 
     * @param container The container for which to obtain the full external
     *     path.
     * @return The full external path of the container.
     */
    public static String getFullExternalPath(ArtifactContainer container) {
        String internalPath;
        
        ArtifactEntry entry = container.getEntryInEnclosingContainer();
        if ( entry == null ) {
            internalPath = null;
        } else {
            StringBuilder pathBuilder = new StringBuilder();
            while ( entry != null ) {
                pathBuilder.insert(0,  entry.getPath());
                container = entry.getRoot();
                entry = container.getEntryInEnclosingContainer();
            }
            internalPath = pathBuilder.toString();
        }

        String externalPath = getExternalPath(container);

        if ( internalPath == null ) {
            return externalPath;
        } else {
            return externalPath + internalPath;
        }
    }

    //

    /**
     * Answer the container as its enclosing entry.  Answer null for a root-of-roots
     * container.
     *
     * @param container The container for which to obtain the enclosing entry.
     *
     * @return The enclosing entry of the container.
     */
    public static Entry getEntryInEnclosingContainer(Container container) {
        try {
            return container.adapt(Entry.class);
        } catch ( UnableToAdaptException e ) {
            // FFDC
            return null;
        }
    }

    /**
     * Answer the root-of-roots of a target container.
     *
     * @param container The container from which to obtain the root-of-roots.
     *
     * @return The root-of-roots of a target container.
     */
    public static Container getRootOfRoots(Container container) {
        Entry entry = getEntryInEnclosingContainer(container);
        while ( entry != null ) {
            container = container.getRoot();
            entry = getEntryInEnclosingContainer(container);
        }
        return container;
    }

    /**
     * Answer the full path of a container from the root-of-roots.  Answer
     * "/" if the container is the root-of-roots.
     * 
     * @param container The container for which to obtain the path.
     * 
     * @return The full path of the container from the root-of-roots.
     */
    public static String getFullInternalPath(Container container) {
        Entry entry = getEntryInEnclosingContainer(container);
        if ( entry == null ) {
            return "/";
        }

        StringBuilder pathBuilder = new StringBuilder();
        while ( entry != null ) {
            pathBuilder.insert(0,  entry.getPath());
            container = entry.getRoot();
            entry = getEntryInEnclosingContainer(container);
        }

        return pathBuilder.toString();
    }

    /**
     * Answer the external path of a container.  Usually, the container
     * is a root-of-roots.  Obtain the external path from the URL of the
     * container, which must have a file URL.  If the container has more
     * than one URL, use the first URL to otain the external path.
     * 
     * The external path is the canonical path of the file created using the
     * path of the container's URL.
     *
     * @param container The container for which to obtain an external path.
     * 
     * @return The external path of the container.
     */
    public static String getExternalPath(Container container) {
        Collection<URL> urls = container.getURLs();
        if ( urls.isEmpty() ) {
            return null;
        }
        URL firstUrl = null;
        for ( URL url : urls ) {
            firstUrl = url;
            break;
        }

        // 'firstUrl' will always be assigned, due to the 'urls.isEmpty()' check.
        @SuppressWarnings("null")
        String firstPath = firstUrl.getPath();
        File firstFile = new File(firstPath);

        return UtilImpl_FileUtils.getCanonicalPath(firstFile);
    }

    /**
     * Answer the full external path of a container.  This is the external
     * path of the root-of-roots of the container plus the full internal
     * path of the container.  (Or, just the external path when the container
     * is a root-of-roots.)
     * 
     * @param container The container for which to obtain the full external
     *     path.
     * @return The full external path of the container.
     */
    public static String getFullExternalPath(Container container) {
        String internalPath;
        
        Entry entry = getEntryInEnclosingContainer(container);
        if ( entry == null ) {
            internalPath = null;
        } else {
            StringBuilder pathBuilder = new StringBuilder();
            while ( entry != null ) {
                pathBuilder.insert(0,  entry.getPath());
                container = entry.getRoot();
                entry = getEntryInEnclosingContainer(container);
            }
            internalPath = pathBuilder.toString();
        }

        String externalPath = getExternalPath(container);

        if ( internalPath == null ) {
            return externalPath;
        } else {
            return externalPath + internalPath;
        }
    }
}
