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
package com.ibm.ws.event.internal.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.service.event.EventHandler;

import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.Topic;
import com.ibm.ws.event.internal.EventEngineImpl;
import com.ibm.ws.event.internal.EventImpl;
import com.ibm.ws.event.internal.adapter.OSGiHandlerAdapter;

public class OSGiHandlerAdapterTest {

    private final static class TestEventEngine extends EventEngineImpl {};

    private final TestEventEngine eventEngine = new TestEventEngine();

    @Test
    public void testOSGiHandlerAdapter() {

        EventHandler handler = new EventHandler() {
            public void handleEvent(org.osgi.service.event.Event event) {
                        }
        };
        OSGiHandlerAdapter adapter = new OSGiHandlerAdapter(handler);
        assertSame(handler, adapter.osgiEventHandler);
    }

    private static class TestEventHandler implements EventHandler {
        org.osgi.service.event.Event event;

        public void handleEvent(org.osgi.service.event.Event event) {
            this.event = event;
        }
    }

    @Test
    public void testHandleEvent() {
        final String topicName = "com/ibm/websphere/test/Topic";
        final Topic topic = new Topic(topicName);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Event.EVENT_TOPIC.getName(), topicName);
        for (int i = 0; i < 100; i++) {
            props.put("key" + i, Integer.toString(i));
        }

        EventImpl eventImpl = eventEngine.createEvent(topic);
        eventImpl.setProperties(props);

        TestEventHandler handler = new TestEventHandler();
        OSGiHandlerAdapter adapter = new OSGiHandlerAdapter(handler);
        adapter.handleEvent(eventImpl);

        assertEquals(handler.event.getTopic(), topicName);
        for (int i = 0; i < 100; i++) {
            assertTrue(handler.event.getProperty("key" + i).equals(Integer.toString(i)));
        }
    }

}
