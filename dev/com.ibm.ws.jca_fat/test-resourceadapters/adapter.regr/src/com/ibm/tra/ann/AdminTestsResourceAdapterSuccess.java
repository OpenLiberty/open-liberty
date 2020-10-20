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
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

@Connector(displayName = "TRA_jca16_admin_AnnotatedResourceAdapter_Success")
public class AdminTestsResourceAdapterSuccess implements ResourceAdapter {

    @Override
    public void endpointActivation(MessageEndpointFactory arg0,
                                   ActivationSpec arg1) throws ResourceException {}

    @Override
    public void endpointDeactivation(MessageEndpointFactory arg0,
                                     ActivationSpec arg1) {}

    @Override
    public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
        return null;
    }

    @Override
    public void start(BootstrapContext arg0) throws ResourceAdapterInternalException {}

    @Override
    public void stop() {}

}
