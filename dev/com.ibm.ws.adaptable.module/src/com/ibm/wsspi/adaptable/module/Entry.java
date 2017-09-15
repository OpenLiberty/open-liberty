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

/**
 * An adaptable entry.
 */
public interface Entry extends Adaptable {

    /**
     * Gets the name of this entry.
     * 
     * @return name of Entry
     */
    public String getName();

    /**
     * Gets the path of this entry.
     * 
     * @return path of Entry
     */
    public String getPath();

    /**
     * Obtain size of this Entries data, if any.
     * 
     * @return number of bytes this Entry represents.
     */
    long getSize();

    /**
     * Obtain the time that this entry was last modified
     * 
     * @return A long value indicating the last modified time or 0L
     *         if the value is not available
     */
    long getLastModified();

    /**
     * <p>
     * This method should return a URL suitable for the ServletContext.getResource(String path) method.
     * </p>
     * <p>
     * If this Entry represents a container then this method will not work because some implementations of this API may map more than one location to a single container. Therefore
     * to load all of the locations that contribute to a container you should do:
     * </p>
     * <code>
     * Container container = entry.convertToContainer();<br/>
     * if (container != null) {<br/>
     * &nbsp;&nbsp;Collection&lt;URI&gt; allUrisForContainer = container.getUri();<br/>
     * }<br/>
     * </code>
     * <p>
     * This may return null if this Entry is a virtual container that does not have a real location on disk.
     * </p>
     * 
     * @return The URL pointing to this entry on disk
     * @see ServletContext.getResource(String path)
     */
    URL getResource();

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
     * @deprecated added purely to support getRealPath on ServletContext ..
     */
    @Deprecated
    public String getPhysicalPath();

    /**
     * Gets the container this entry lives in..
     * 
     * @return container this entry lives in..
     */
    public Container getEnclosingContainer();

    /**
     * Gets the Container that represents the root of this Entries
     * hierarchy. eg. the Container with path "/".
     * 
     * @return container representing /
     */
    public Container getRoot();

}
