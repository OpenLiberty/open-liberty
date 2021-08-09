/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.adaptable.module;

import com.ibm.wsspi.artifact.ArtifactContainer;

/**
 *
 */
public interface FastModeControl {

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

}
