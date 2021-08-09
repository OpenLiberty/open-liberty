/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.dynamicconfigadapter;

import java.util.concurrent.ConcurrentHashMap;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * Fake resource adapter for the FAT bucket.
 */
public class DynaCfgResourceAdapter implements ResourceAdapter {
    final ConcurrentHashMap<ActivationSpec, MessageEndpointFactory> endpointFactories = new ConcurrentHashMap<ActivationSpec, MessageEndpointFactory>();

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) throws ResourceException {
        System.out.println("endpointActivation called for " + endpointFactory + "," + activationSpec);
        if (endpointFactories.putIfAbsent(activationSpec, endpointFactory) != null)
            throw new NotSupportedException("Multiple endpoint activations for same activation spec: " + activationSpec);
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) {
        System.out.println("endpointDeactivation called for " + endpointFactory + "," + activationSpec);
        boolean removed = endpointFactories.remove(activationSpec, endpointFactory);
        if (!removed)
            throw new IllegalStateException();
    }

    /** {@inheritDoc} */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        System.out.println("Resource adapter started");
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
        System.out.println("Resource adapter stopped");
    }
}
