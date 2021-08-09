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
package com.ibm.ws.adaptable.module.structure;

import com.ibm.wsspi.artifact.ArtifactContainer;

/**
 * Callback interface to be used with InterpretedContainer to allow instances to redefine roots.
 * 
 * @ibm-spi
 */
public interface StructureHelper {

    /**
     * Override if the passed container should be considered as Container.isRoot true.<p>
     * <em><b>THIS CANNOT MAKE THE CONTAINER RETURN isRoot=false</b></em><p>
     * <i>if the behavior is required to override root containers to act as non-roots, a new story is required</i>
     * 
     * @param e the container to evaluate.
     * @return true if the container.isRoot response should be overridden to be true. false means no override takes place.
     */
    public boolean isRoot(ArtifactContainer e);

    /**
     * Determine if the path passed is supposed to exist. <p>
     * When the StructureHelper converts containers to be isRoot true, it must also be responsible for
     * stating that all paths beneath that container should no longer be valid.
     * 
     * @param e the container for the path to evaluate
     * @param path the path to evaluate in the Container
     * @return true if the path corresponds to an Entry that is still part of Container, false otherwise.
     */
    public boolean isValid(ArtifactContainer e, String path);

}
