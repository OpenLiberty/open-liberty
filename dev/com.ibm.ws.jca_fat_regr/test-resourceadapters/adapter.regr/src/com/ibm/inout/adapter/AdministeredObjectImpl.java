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
package com.ibm.inout.adapter;

import javax.resource.ResourceException;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.ResourceAdapterInternalException;

import com.ibm.adapter.message.FVTMessageProvider;
import com.ibm.adapter.message.FVTMessageProviderImpl;

@AdministeredObject(adminObjectInterfaces = { FVTMessageProvider.class })
public class AdministeredObjectImpl extends FVTMessageProviderImpl implements ResourceAdapterAssociation {

    public AdministeredObjectImpl() throws ResourceAdapterInternalException {
        super();
    }

    private ResourceAdapter resourceAdapterFromAssociation = null;
    /**
     * 
     */
    private static final long serialVersionUID = 542158669630712048L;

    public ResourceAdapter getResourceAdapter() {
        return resourceAdapterFromAssociation;
    }

    public void setResourceAdapter(ResourceAdapter arg0) throws ResourceException {
        if (resourceAdapterFromAssociation == null) {
            resourceAdapterFromAssociation = arg0;
        } else {
            throw new ResourceException("Cannot call setResourceAdapter twice");
        }
    }

}
