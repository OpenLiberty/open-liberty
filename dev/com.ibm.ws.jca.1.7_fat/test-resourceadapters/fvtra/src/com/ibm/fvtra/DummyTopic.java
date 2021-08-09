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
import javax.jms.Topic;

public class DummyTopic implements Topic {

    ConcurrentLinkedQueue<DummyMessage> internalQueue = new ConcurrentLinkedQueue<DummyMessage>();

    private String topicName;

    public DummyTopic() {
    }

    @Override
    public String getTopicName() throws JMSException {
        return topicName;
    }

    public void addMessage(DummyMessage dm) {
        internalQueue.add(dm);
    }

    /**
     * @param topicName the topicName to set
     */
    public void setTopicName(String topicName) {
        this.topicName = topicName;
        DummyME.destinations.put(topicName, this);
    }

}
