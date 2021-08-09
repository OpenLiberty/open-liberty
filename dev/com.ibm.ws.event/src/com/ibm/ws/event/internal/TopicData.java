/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.event.internal;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public final class TopicData {

    final private String topic;
    final private ExecutorService executorService;
    final private List<HandlerHolder> eventHandlers;
    final private AtomicReference<TopicData> reference;

    TopicData(String topic, ExecutorService executorService, List<HandlerHolder> eventHandlers) {
        this.topic = topic;
        this.executorService = executorService;
        this.eventHandlers = eventHandlers;
        this.reference = new AtomicReference<TopicData>(this);
    }

    public String getTopic() {
        return topic;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public List<HandlerHolder> getEventHandlers() {
        return eventHandlers;
    }

    public AtomicReference<TopicData> getReference() {
        return reference;
    }

    public void clearReference() {
        reference.set(null);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";topic=").append(topic);
        return sb.toString();
    }
}
