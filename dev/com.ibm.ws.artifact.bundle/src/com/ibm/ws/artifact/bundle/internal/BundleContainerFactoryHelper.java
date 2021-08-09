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
package com.ibm.ws.artifact.bundle.internal;

import java.io.File;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.artifact.contributor.ArtifactContainerFactoryHelper;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * This instance of {@link ContainerFactoryHelper} will test if the object having a container created for it is a {@link Bundle} object and if it is will create a container for it.
 */
public class BundleContainerFactoryHelper implements ArtifactContainerFactoryHelper {

    private final AtomicServiceReference<ArtifactContainerFactory> containerFactoryReference = new AtomicServiceReference<ArtifactContainerFactory>("containerFactory");

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer createContainer(File cacheDir, Object o) {
        if (o instanceof Bundle) {
            return new BundleArchive(cacheDir, (Bundle) o, null, null, this);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer createContainer(File cacheDir, ArtifactContainer parent, ArtifactEntry entry, Object o) {
        if (o instanceof Bundle) {
            return new BundleArchive(cacheDir, (Bundle) o, parent, entry, this);
        }
        return null;
    }

    // OSGi methods
    /**
     * Initializes the reference to the container factory
     * 
     * @param ctx
     */
    protected void activate(ComponentContext ctx) {
        containerFactoryReference.activate(ctx);
    }

    /**
     * Sets the container factory. Should only be called by DS.
     * 
     * @param cf
     */
    protected void setContainerFactory(ServiceReference<ArtifactContainerFactory> cf) {
        containerFactoryReference.setReference(cf);
    }

    /**
     * Unsets the container factory. Should only be called by DS.
     * 
     * @param cf
     */
    protected void unsetContainerFactory(ServiceReference<ArtifactContainerFactory> cf) {
        containerFactoryReference.unsetReference(cf);
    }

    /**
     * Returns the container factory for creating containers for sub-objects.
     * 
     * @return The container factory
     */
    public ArtifactContainerFactory getContainerFactory() {
        return containerFactoryReference.getServiceWithException();
    }

}
