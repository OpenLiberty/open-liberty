/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.config.jmsadapter;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ConfigProperty;

@AdministeredObject(adminObjectInterfaces = { Queue.class, Destination.class })
public class JMSQueueImpl extends JMSDestinationImpl implements Queue {
    @ConfigProperty
    private String queueName;

    @Override
    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String value) {
        this.queueName = value;
    }
}
