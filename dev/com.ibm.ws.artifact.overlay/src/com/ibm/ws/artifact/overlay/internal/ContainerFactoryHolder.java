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

import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;

/**
 * Declarative service safe reference to a container factory.
 * 
 * The container factory type is not the same type as delegate container
 * factories: The delegating container factory is expected to create
 * containers by iterating across available delegates, answering the
 * first container which was created, and answering null if no delegate
 * created a container. 
 *
 * Used to manage container factory references: The factory reference
 * is aware of declarative service events and sets and clears the
 * factory reference to match the declarative service state.
 */
public interface ContainerFactoryHolder {
    /**
     * Answer the referenced container factory.  Throw an exception
     * if the container factory is not active.
     *
     * Set and un-set by declarative service events.
     * 
     * @return The active container factory.
     *
     * @throws IllegalStateException Thrown if the container factory is
     *     not available.
     */
    ArtifactContainerFactory getContainerFactory();
}
