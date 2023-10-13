/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.workcontext.jca;

import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.XAResource;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.Connector;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.WorkManager;

/**
 * Example resource adapter.
 */
@Connector
public class ResourceAdapterImpl implements ResourceAdapter {
    final ConcurrentHashMap<ActivationSpecImpl, MessageEndpointFactory> endpointFactories = new ConcurrentHashMap<ActivationSpecImpl, MessageEndpointFactory>();
    // LH 05-10-23 make public
    //public BootstrapContext bootstrapContext;
    public WorkManager wmInstance;

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) throws ResourceException {
        endpointFactories.putIfAbsent((ActivationSpecImpl) activationSpec, endpointFactory);
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) {
        endpointFactories.remove(activationSpec, endpointFactory);
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
        return null;
    }

    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        // 07/17/23
        System.out.println(" -- debug RA impl start -- ");
        wmInstance = bootstrapContext.getWorkManager();
    }

    // - 07/17/23
    public WorkManager getWmInstance() {
        System.out.println(" -- debug RA impl getWmInstance -- ");
        return wmInstance;
    }

    @Override
    public void stop() {
    }
}
