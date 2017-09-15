/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.artifact;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Represents an Entry within a Container.
 * Note that Container does not require hashCode/equals, and thus should not be used as a key
 * in HashMaps, etc.
 */
public interface ArtifactEntry extends EnclosedEntity {

    /**
     * Attempt to convert this entry into a Container.<p>
     * Returned container may be a new type of artifact, check {@link ArtifactContainer#isRoot} on the return.
     * 
     * @return Container if conversion is possible, null otherwise.
     */
    ArtifactContainer convertToContainer();

    /**
     * Attempt to convert this entry into a ArtifactContainer.<p>
     * Returned container may be a new type of artifact, check {@link ArtifactContainer#isRoot} on the return.
     * 
     * @param localOnly pass true if conversion should be restricted to returning Containers where isRoot returns false.
     * @return ArtifactContainer if conversion is possible, null otherwise.
     */
    ArtifactContainer convertToContainer(boolean localOnly);

    /**
     * Obtain inputstream for entry.
     * 
     * @return inputStream if possible for this entry, or null otherwise.
     * @throws IOException if there is supposed to be a stream, but an error occurred obtaining it.
     */
    InputStream getInputStream() throws IOException;

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

    //Concepts absent, but under discussion. 
    // - notification api.
}
