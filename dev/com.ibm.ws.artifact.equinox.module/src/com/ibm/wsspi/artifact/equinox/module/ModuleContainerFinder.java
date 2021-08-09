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
package com.ibm.wsspi.artifact.equinox.module;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.equinox.module.ModuleBundleFileFactory;

/**
 * The module container finder service. This service is used
 * to find a {@link Container container} for a specific bundle
 * location. The module container finder service is used
 * by a {@link ModuleBundleFileFactory} service implementation
 * to find a container that can be used as the content of
 * a root bundle file.
 */
public interface ModuleContainerFinder {
    /**
     * Finds a {@link Container container} for the specified
     * bundle location. The bundle location is the same location
     * that is used to install the bundle into the framework.
     * The returned container will be used as the content of the
     * root bundle file of the bundle which has the specified location.
     * 
     * @param location The bundle location to find a container for
     * @return the container for the bundle or {@code null} if none
     *         is found.
     */
    public Container findContainer(String location);
}
