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

import java.lang.reflect.Method;

import javax.resource.ResourceException;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.MessageListener;
import javax.resource.cci.Record;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;

/**
 * Notifies message driven beans that a value in a DerbyMap was replaced.
 */
public class DerbyMessageWork implements Work {
    private final DerbyActivationSpec activationSpec;
    private final Object key, value, previous;

    DerbyMessageWork(DerbyActivationSpec activationSpec, Object key, Object value, Object previous) {
        this.activationSpec = activationSpec;
        this.key = key;
        this.value = value;
        this.previous = previous;
    }

    @Override
    public void release() {}

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        System.out.println("Work impl will notify MDB for " + key + ", value " + previous + " updated to " + value);

        for (MessageEndpointFactory mef : activationSpec.messageEndpointFactories)
            try {
                MappedRecord record = new DerbyMappedRecord();
                record.setRecordName("MapValueReplaced");
                record.setRecordShortDescription("A value in DerbyMap was replaced with a new value.");
                record.put("key", key);
                record.put("newValue", value);
                record.put("previousValue", previous);

                MessageEndpoint endpoint = mef.createEndpoint(null);
                MessageListener listener = (MessageListener) endpoint;
                Method onMessage = MessageListener.class.getMethod("onMessage", Record.class);
                endpoint.beforeDelivery(onMessage);
                record = (MappedRecord) listener.onMessage(record);
                endpoint.afterDelivery();
                endpoint.release();

                System.out.println("Response from MDB has record name " + record.getRecordName() +
                                   " and description: " + record.getRecordShortDescription() +
                                   ". Content is " + record);
            } catch (ResourceException x) {
                x.printStackTrace();
            } catch (NoSuchMethodException x) {
                x.printStackTrace();
            } finally {
            }
    }
}
