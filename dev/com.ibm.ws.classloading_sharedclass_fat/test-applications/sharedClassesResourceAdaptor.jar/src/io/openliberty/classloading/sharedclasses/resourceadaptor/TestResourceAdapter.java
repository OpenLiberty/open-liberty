/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.classloading.sharedclasses.resourceadaptor;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 *
 */
public class TestResourceAdapter implements ResourceAdapter {

    private static void print(String msg) {
        System.out.println("TestResourceAdapter: " + msg);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.resource.spi.ResourceAdapter#endpointActivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
     */
    @Override
    public void endpointActivation(MessageEndpointFactory arg0, ActivationSpec arg1) throws ResourceException {
        print("endpointActivation ( " + arg0 + " , " + arg1 + " )");

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.resource.spi.ResourceAdapter#endpointDeactivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
     */
    @Override
    public void endpointDeactivation(MessageEndpointFactory arg0, ActivationSpec arg1) {
        print("endpointDeactivation ( " + arg0 + " , " + arg1 + " )");

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.resource.spi.ResourceAdapter#getXAResources(javax.resource.spi.ActivationSpec[])
     */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
        print("getXAResources ( " + arg0 + " )");
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
     */
    @Override
    public void start(BootstrapContext arg0) throws ResourceAdapterInternalException {
        print("start ( " + arg0 + " )");

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.resource.spi.ResourceAdapter#stop()
     */
    @Override
    public void stop() {
        print("stop ()");

    }

}
