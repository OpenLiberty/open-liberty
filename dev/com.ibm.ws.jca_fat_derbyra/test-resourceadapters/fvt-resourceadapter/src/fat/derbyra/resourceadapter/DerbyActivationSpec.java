/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.derbyra.resourceadapter;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpointFactory;

/**
 * Activation spec, which, for any Map admin object, whenever a value is replaced
 * for a key that matches the specified key prefix, notifies a message driven bean.
 */
public class DerbyActivationSpec implements ActivationSpec {
    private ResourceAdapter adapter;
    String keyPrefix;
    final ConcurrentLinkedQueue<MessageEndpointFactory> messageEndpointFactories = new ConcurrentLinkedQueue<MessageEndpointFactory>();

    public String getKeyPrefix() {
        return keyPrefix;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = adapter;
    }

    @Override
    public void validate() throws InvalidPropertyException {
        System.out.println("Validated ActivationSpec with keyPrefix " + keyPrefix + " and resource adapter " + adapter);
    }
}
