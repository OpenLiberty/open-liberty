/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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

import javax.jms.Topic;
import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;

/**
 * javax.jms.Topic administered object for FVT
 */
public class FVTTopic implements Topic, ResourceAdapterAssociation, Serializable {
    private static final long serialVersionUID = 1557430607598372401L;

    private transient FVTResourceAdapter adapter;
    private String topicName; // simulates a config property

    @Override
    public String getTopicName() {
        return topicName;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (FVTResourceAdapter) adapter;
    }

    @Override
    public String toString() {
        return super.toString() + ':' + topicName;
    }
}
