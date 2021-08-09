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

import java.net.URL;
import java.util.Collection;

/**
 * Represents an immediate enclosure of a number of Entries.<p>
 * Entries within this Container, may themselves be Containers {@see Entry#convertToContainer()} <p>
 * Note that Container does not require hashCode/equals, and thus should not be used as a key
 * in HashMaps, etc.
 * 
 * This can be used to represent, for example, a directory on the file system or a JAR file.
 */
public interface ArtifactContainer extends Iterable<ArtifactEntry>, EnclosedEntity {

    /**
     * Instruct this container it may commit to using more resources
     * to speed access to it's content.
     * <p>
     * Fast Mode is enabled at the root container, and enables/disables for all containers beneath that root.<br>
     * Fast Mode does not cascade into new root containers (eg, where Entry.convertToContainer().isRoot()==true)
     * <p>
     * Calling this method requires you to later invoke {@link ArtifactContainer#stopUsingFastMode} <p>
     * This method is equivalent to {@link ArtifactContainer.getRoot().useFastMode()}
     */
    public void useFastMode();

    /**
     * Instruct this container that you no longer require it to consume
     * resources to speed access to it's content.
     * <p>
     * Fast Mode is enabled at the root container, and enables/disables for all containers beneath that root.<br>
     * Fast Mode does not cascade into new root containers (eg, where Entry.convertToContainer().isRoot()==true)
     * <p>
     * Calling this method requires you to have previously invoked {@link ArtifactContainer#useFastMode}<p>
     * This method is equivalent to {@link ArtifactContainer.getRoot().useFastMode()}
     */
    public void stopUsingFastMode();

    /**
     * Does this container represent the root of an artifact.
     * 
     * @return true if root, false otherwise.
     */
    public boolean isRoot();

    /**
     * Obtain an entry from this container.<p>
     * If path is absolute (starts with '/') and this Container is not root {@see Container#isRoot} the
     * path is interpreted as being from the Container that is root enclosing this one.<br>
     * If the path is not absolute, (does not start with '/'), or this Container is root and the path is absolute,
     * then the entry is searched for relative to this Container.
     * 
     * @param pathAndName absolute or relative path to search for.
     * @return ArtifactEntry if Entry was found, null otherwise.
     */
    public ArtifactEntry getEntry(String pathAndName);

    /**
     * This may not be implemented by all implementations of this interface and it should never return <code>null</code> but should return an empty collection instead.
     * 
     * @return A collection of URLs that represent all of the locations on disk that contribute to this container
     * @throws UnsupportedOperationException if the implementation does not support this method
     */
    public Collection<URL> getURLs();

    /**
     * Get the Entry representing this entity within its EnclosingContainer
     * 
     * @return entry representing this entity in the enclosing container, or null if there is no enclosing container, and therefore no entry.
     */
    public ArtifactEntry getEntryInEnclosingContainer();

    /**
     * Obtain the notification manager for this container.
     * 
     * @return
     */
    public ArtifactNotifier getArtifactNotifier();
}
