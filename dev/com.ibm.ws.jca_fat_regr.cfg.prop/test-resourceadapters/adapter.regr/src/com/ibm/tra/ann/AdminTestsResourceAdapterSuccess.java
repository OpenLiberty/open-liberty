/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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

    public void endpointActivation(MessageEndpointFactory arg0,
                                   ActivationSpec arg1) throws ResourceException {
    }

    public void endpointDeactivation(MessageEndpointFactory arg0,
                                     ActivationSpec arg1) {
    }

    public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
        return null;
    }

    public void start(BootstrapContext arg0) throws ResourceAdapterInternalException {
    }

    public void stop() {
    }

}
