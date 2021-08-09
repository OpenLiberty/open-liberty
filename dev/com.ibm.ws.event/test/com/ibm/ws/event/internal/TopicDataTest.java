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
package com.ibm.ws.event.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TopicDataTest {

    String topic = "com/ibm/websphere/event/topic/Test";
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    List<HandlerHolder> eventHandlers;
    TopicData topicData;

    @Before
    public void initialize() {
        eventHandlers = new ArrayList<HandlerHolder>();
        topicData = new TopicData(topic, executorService, eventHandlers);
    }

    @After
    public void destroy() {
        topicData = null;
        eventHandlers = null;
    }

    @Test
    public void testTopicData() {
        TopicData topicData = new TopicData(null, null, null);
        assertNull(topicData.getTopic());
        assertNull(topicData.getExecutorService());
        assertNull(topicData.getEventHandlers());

        topicData = new TopicData(topic, executorService, eventHandlers);
        assertSame(topic, topicData.getTopic());
        assertSame(executorService, topicData.getExecutorService());
        assertSame(eventHandlers, topicData.getEventHandlers());
    }

    @Test
    public void testGetReference() {
        topicData = new TopicData(topic, executorService, eventHandlers);
        assertNotNull(topicData);
        assertNotNull(topicData.getReference());
        assertSame(topicData, topicData.getReference().get());
    }

    @Test
    public void testClearReference() {
        topicData = new TopicData(topic, executorService, eventHandlers);
        assertSame(topicData, topicData.getReference().get());
        topicData.clearReference();
        assertNull(topicData.getReference().get());
    }

    @Test
    public void testToString() {
        String string = topicData.toString();
        assertNotNull(string);
        assertTrue(string.contains("topic="));
        assertTrue(string.contains(topic));
    }

}
