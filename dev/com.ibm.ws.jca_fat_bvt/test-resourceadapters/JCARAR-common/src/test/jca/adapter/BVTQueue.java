/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package test.jca.adapter;

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.MessageListener;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterAssociation;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

/**
 * java.util.Queue administered object for BVT
 */
public class BVTQueue extends AbstractQueue<String> implements ResourceAdapterAssociation, Serializable {
    private static final long serialVersionUID = 1557430607598372401L;

    private transient BVTResourceAdapter adapter;

    private String queueName; // simulates a config property

    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> queues =
                    new ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>();

    public String getQueueName() {
        return queueName;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    @Override
    public Iterator<String> iterator() {
        return queues.get(queueName).iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean offer(String o) {
        boolean result;
        MessageEndpointFactory endpointFactory = adapter.endpointFactories.get(queueName);
        MessageEndpoint endpoint = null;
        if (endpointFactory == null)
            result = queues.get(queueName).offer(o);
        else
            try {
                endpoint = endpointFactory.createEndpoint(null);
                BVTRecord record = new BVTRecord();
                record.setRecordName("Adding an element");
                record.setRecordShortDescription("This element is being added to the queue");
                record.add(o);
                ((MessageListener) endpoint).onMessage(record);

                result = queues.get(queueName).offer(o);

            } catch (RuntimeException x) {
                throw x;
            } catch (Exception x) {
                throw new RuntimeException(x);
            } finally {
                if (endpoint != null)
                    endpoint.release();
            }
        return result;
    }

    @Override
    public String peek() {
        return queues.get(queueName).peek();
    }

    @Override
    public String poll() {
        return queues.get(queueName).poll();
    }

    public void setQueueName(String queueName) {
        queues.putIfAbsent(queueName, new ConcurrentLinkedQueue<String>());
        this.queueName = queueName;
    }

    /** {@inheritDoc} */
    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (BVTResourceAdapter) adapter;
    }

    @Override
    public int size() {
        return queues.get(queueName).size();
    }

    @Override
    public String toString() {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this)) +
               (queueName == null ? "" : super.toString());
    }
}
