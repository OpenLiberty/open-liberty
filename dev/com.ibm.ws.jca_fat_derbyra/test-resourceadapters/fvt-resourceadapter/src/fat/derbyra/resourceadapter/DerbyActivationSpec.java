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
    private DerbyResourceAdapter adapter;
    String keyPrefix; // config-property
    final ConcurrentLinkedQueue<MessageEndpointFactory> messageEndpointFactories = new ConcurrentLinkedQueue<MessageEndpointFactory>();
    String qmid; // config-property

    /**
     * Track an XA resource for recovery
     *
     * @param xaRes XA resource instance in need of recovery
     */
    void addRecoverableResource(DerbyXAResource xaRes) {
        ConcurrentLinkedQueue<DerbyXAResource> newList = new ConcurrentLinkedQueue<DerbyXAResource>();
        ConcurrentLinkedQueue<DerbyXAResource> oldList = adapter.recoverableXAResources.putIfAbsent(keyPrefix, newList);
        (oldList == null ? newList : oldList).add(xaRes);
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getQmid() {
        return qmid == null ? DerbyXAResource.XA_RECOVERY_QMID : qmid;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public void setQmid(String qmid) {
        this.qmid = qmid;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (DerbyResourceAdapter) adapter;
    }

    @Override
    public void validate() throws InvalidPropertyException {
        System.out.println("Validated " + this + " with keyPrefix " + keyPrefix + " and resource adapter " + adapter);
    }
}
