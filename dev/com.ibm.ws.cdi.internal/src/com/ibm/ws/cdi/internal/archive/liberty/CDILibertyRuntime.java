/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.archive.liberty;

import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;

/**
 * Holds references to all the services that CDI needs to access.
 */
public interface CDILibertyRuntime extends CDIRuntime {
    /**
     * @return the ArtifactContainerFactory
     */
    public ArtifactContainerFactory getArtifactContainerFactory();

    /**
     * @return the AdaptableModuleFactory
     */
    public AdaptableModuleFactory getAdaptableModuleFactory();

}
