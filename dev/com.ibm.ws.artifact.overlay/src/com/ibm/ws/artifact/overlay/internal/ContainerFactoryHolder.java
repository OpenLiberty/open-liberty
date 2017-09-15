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
package com.ibm.ws.artifact.overlay.internal;

import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;

/**
 * DS Safe way for containers/entries to obtain the container factory
 */
public interface ContainerFactoryHolder {
    /**
     * Get the ContainerFactory from this holder.<p>
     * Because the factory is a service, if someone removes the supplying bundle, without
     * first providing an alternate implementation, then we will explode with an IllegalStateException.<br>
     * It is not expected that this will happen.
     * 
     * @return the containerFactory
     * @throws IllegalStateException if the ContainerFactory has gone away.
     */
    public ArtifactContainerFactory getContainerFactory();
}
