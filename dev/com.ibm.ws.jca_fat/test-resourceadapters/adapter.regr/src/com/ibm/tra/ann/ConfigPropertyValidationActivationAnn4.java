/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra.ann;

import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import com.ibm.tra.inbound.impl.TRAMessageListener3;

@Activation(
            messageListeners = { TRAMessageListener3.class })
public class ConfigPropertyValidationActivationAnn4 implements javax.resource.spi.ActivationSpec, javax.resource.spi.ResourceAdapter {

    public ConfigPropertyValidationActivationAnn4() {
        super();
    }

    public String xyz;

    public String getXyz() {
        return xyz;
    }

    @SuppressWarnings("unused")
    @ConfigProperty(
                    supportsDynamicUpdates = false, defaultValue = "TestValue")
    public void setXyz(String xyz) {
        this.xyz = xyz;
    }

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ResourceAdapterAssociation#getResourceAdapter()
 */
    @Override
    public ResourceAdapter getResourceAdapter() {
        return null;
    }

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ResourceAdapterAssociation#setResourceAdapter(javax.resource.spi.ResourceAdapter)
 */
    @Override
    public void setResourceAdapter(ResourceAdapter arg0) throws ResourceException {}

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ActivationSpec#validate()
 */
    @Override
    public void validate() throws InvalidPropertyException {}

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ResourceAdapter#endpointActivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
 */
    @Override
    public void endpointActivation(MessageEndpointFactory arg0, ActivationSpec arg1) throws ResourceException {}

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ResourceAdapter#endpointDeactivation(javax.resource.spi.endpoint.MessageEndpointFactory, javax.resource.spi.ActivationSpec)
 */
    @Override
    public void endpointDeactivation(MessageEndpointFactory arg0, ActivationSpec arg1) {}

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ResourceAdapter#getXAResources(javax.resource.spi.ActivationSpec[])
 */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
        return null;
    }

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
 */
    @Override
    public void start(BootstrapContext arg0) throws ResourceAdapterInternalException {}

/*
 * (non-Javadoc)
 *
 * @see javax.resource.spi.ResourceAdapter#stop()
 */
    @Override
    public void stop() {}

}
