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
package com.ibm.wsspi.adaptable.module;

import java.net.URL;
import java.util.Collection;

/**
 * An adaptable container.
 */
public interface Container extends Adaptable, Iterable<Entry> {

    /**
     * Obtain adaptable entry from this container.<br>
     * Path may be relative to this node, or absolute to the local root.<br>
     * Local root is the 'enclosingContainer' above this Container in the hierarchy, that returns true for 'isRoot'.
     * 
     * @param pathAndName path to obtain entry at.
     * @return Entry if located, null otherwise.
     */
    public Entry getEntry(String pathAndName);

    /**
     * Get name for this Container.<p>
     * eg. /wibble/fish will give fish, /wibble will give wibble
     * 
     * @return name of this Container
     */
    public String getName();

    /**
     * Get path for this Container.
     * 
     * @return path for this Container.
     */
    public String getPath();

    /**
     * Get the Container that encloses this one.<p>
     * Even containers that return true for isRoot, may return an enclosing Container.<br>
     * Eg. a Jar within a directory, the jar will return true for isRoot, and it's enclosing container would be the directory.
     * 
     * @return the Container enclosing this one, or null if there is none.
     */
    public Container getEnclosingContainer();

    /**
     * True if this Container represents a root for paths.<p>
     * Even containers that return true for isRoot, may still be enclosed by another Container.
     * Eg. a Jar within a directory, the jar will return true for isRoot, and it's enclosing container would be the directory.
     * 
     * @return true if this Container is a root, false otherwise.
     */
    public boolean isRoot();

    /**
     * Gets the Container that represents the root of this Entries
     * hierarchy. eg. the Container with path "/".
     * 
     * @return container representing / (or self, if this isRoot=true)
     */
    public Container getRoot();

    /**
     * This may not be implemented by all implementations of this interface and it should never return <code>null</code> but should return an empty collection instead.
     * 
     * @return A collection of URLs that represent all of the locations on disk that contribute to this container
     * @throws UnsupportedOperationException if the implementation does not support this method
     */
    public Collection<URL> getURLs();

    /**
     * <p>
     * Get path for this Entity. Not all implementations of this interface need to support this method and should return <code>null</code> if they do not support them.
     * </p>
     * <p>
     * If the entry is container within an archive file such as a JAR or ZIP then this will return <code>null</code>.
     * </p>
     * <p>
     * For directories that have more than one physical location mapped to them then this will return the first mapped resource.
     * </p>
     * 
     * @return String representing physical path on disk for this entity.. null if there is none. null is very possible.
     * @deprecated added purely to support getRealPath on ServletContext .. post alpha this will need replacing.
     */
    @Deprecated
    public String getPhysicalPath();
}
