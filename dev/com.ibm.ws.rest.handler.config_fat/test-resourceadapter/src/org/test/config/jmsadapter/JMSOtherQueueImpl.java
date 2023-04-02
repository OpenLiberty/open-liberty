/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package org.test.config.jmsadapter;

import javax.jms.Queue;
import javax.resource.spi.AdministeredObject;
import javax.resource.spi.ConfigProperty;

@AdministeredObject(adminObjectInterfaces = Queue.class)
public class JMSOtherQueueImpl implements Queue {
    @ConfigProperty(defaultValue = "qm")
    private String queueManager;

    @ConfigProperty
    private String queueName;

    public String getQueueManager() {
        return queueManager;
    }

    @Override
    public String getQueueName() {
        return queueName;
    }

    public void setQueueManager(String value) {
        this.queueManager = value;
    }

    public void setQueueName(String value) {
        this.queueName = value;
    }
}
