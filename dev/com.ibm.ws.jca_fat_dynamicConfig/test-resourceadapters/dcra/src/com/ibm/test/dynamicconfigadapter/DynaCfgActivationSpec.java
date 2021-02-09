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

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;

public class DynaCfgActivationSpec implements ActivationSpec, ResourceAdapterAssociation {
    private ResourceAdapter adapter;
    private int messageFilterMax;
    private int messageFilterMin;

    public int getMessageFilterMax() {
        return messageFilterMax;
    }

    public int getMessageFilterMin() {
        return messageFilterMin;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public void setMessageFilterMax(int messageFilterMax) {
        this.messageFilterMax = messageFilterMax;
    }

    public void setMessageFilterMin(int messageFilterMin) {
        this.messageFilterMin = messageFilterMin;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = adapter;
    }

    @Override
    public void validate() throws InvalidPropertyException {
        if (adapter == null)
            throw new InvalidPropertyException("resourceAdapter not set on ResourceAdapterAssociation");
        if (messageFilterMin > messageFilterMax)
            throw new InvalidPropertyException("messageFilterMin=" + messageFilterMin + ", messageFilterMax=" + messageFilterMax);
    }
}
