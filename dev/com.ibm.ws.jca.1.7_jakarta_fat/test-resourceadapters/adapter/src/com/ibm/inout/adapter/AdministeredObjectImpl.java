/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

import com.ibm.adapter.message.FVTMessageProviderImpl;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterAssociation;
import jakarta.resource.spi.ResourceAdapterInternalException;

public class AdministeredObjectImpl extends FVTMessageProviderImpl implements ResourceAdapterAssociation {

    public AdministeredObjectImpl() throws ResourceAdapterInternalException {
        super();
    }

    private ResourceAdapter resourceAdapterFromAssociation = null;
    /**
     *
     */
    private static final long serialVersionUID = 542158669630712048L;

    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapterFromAssociation;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter arg0) throws ResourceException {
        if (resourceAdapterFromAssociation == null) {
            resourceAdapterFromAssociation = arg0;
        } else {
            throw new ResourceException("Cannot call setResourceAdapter twice");
        }
    }

}
