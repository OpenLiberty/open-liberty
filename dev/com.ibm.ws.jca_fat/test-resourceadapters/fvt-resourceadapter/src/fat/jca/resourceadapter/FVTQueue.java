/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.resourceadapter;

import java.io.Serializable;

import javax.jms.Queue;
import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;

/**
 * javax.jms.Queue administered object for FVT
 */
public class FVTQueue implements Queue, ResourceAdapterAssociation, Serializable {
    private static final long serialVersionUID = 1557430607598372401L;

    private transient FVTResourceAdapter adapter;
    private String queueName; // simulates a config property

    @Override
    public String getQueueName() {
        return queueName;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (FVTResourceAdapter) adapter;
    }

    @Override
    public String toString() {
        return super.toString() + ':' + queueName;
    }
}
