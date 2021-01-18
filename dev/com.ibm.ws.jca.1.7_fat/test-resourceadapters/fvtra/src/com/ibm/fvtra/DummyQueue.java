/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.fvtra;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jms.JMSException;
import javax.jms.Queue;

public class DummyQueue implements Queue {

    ConcurrentLinkedQueue<DummyMessage> internalQueue = new ConcurrentLinkedQueue<DummyMessage>();

    private String queueName;

    public DummyQueue() {
    }

    @Override
    public String getQueueName() throws JMSException {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
        DummyME.destinations.put(queueName, this);
    }

    public void addMessage(DummyMessage dm) {
        internalQueue.add(dm);
    }

}
