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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Filter;

import com.ibm.websphere.event.EventLocal;
import com.ibm.websphere.event.ReservedKey;
import com.ibm.websphere.event.Topic;

public class EventImplTest {

    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();

    final Topic topic = new Topic("com/ibm/liberty/test/Topic");

    EventImpl event;

    @Before
    public void initialize() {
        event = new EventImpl(topic);
    }

    @After
    public void destroy() {
        event = null;
    }

    @Test
    public void testGetSetTopicData() {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final List<HandlerHolder> holders = new ArrayList<HandlerHolder>();
        final TopicData topicData = new TopicData("com/ibm/liberty/Test", executorService, holders);

        assertNull(event.getTopicData());
        event.setTopicData(topicData);
        assertSame(topicData, event.getTopicData());
    }

    @Test
    public void testGetSetPropertyString() {
        final String propertyName = "some.valid.property";
        Object value = new Object();

        // Everything should be empty and clear
        assertTrue(event.getProperties().isEmpty());
        assertNull(event.getProperty("some.invalid.property"));
        assertNull(event.getProperty(propertyName));
        assertTrue(event.getProperties().isEmpty());

        // Set the property
        event.setProperty(propertyName, value);

        // Make sure it's there
        assertSame(value, event.getProperty(propertyName));
        assertSame(value, event.getProperty(new ReservedKey(propertyName)));

        // Check the collection based accessors
        assertTrue(!event.getProperties().isEmpty());
        assertTrue(event.getProperties().containsKey(propertyName));
        assertTrue(event.getPropertyNames().contains(propertyName));
    }

    @Test
    public void testRepeatedGetSetPropertyString() {
        for (int i = 0; i < 100; i++) {
            event.setProperty("key", Integer.valueOf(i));
            assertEquals(Integer.valueOf(i), event.getProperty("key"));
        }
    }

    @Test
    public void testGetSetPropertyReservedKey() {
        ReservedKey reservedKey = new ReservedKey("some.reserved.key");
        Object value = new Object();

        // Everything should be empty and clear
        assertTrue(event.getProperties().isEmpty());
        assertNull(event.getProperty("some.unknown.property"));
        assertTrue(event.getProperties().isEmpty());

        // Set the property
        event.setProperty(reservedKey, value);

        // Make sure it's there
        assertSame(value, event.getProperty(reservedKey));
        assertSame(value, event.getProperty(reservedKey.getName()));

        assertTrue(!event.getProperties().isEmpty());
        assertTrue(event.getProperties().containsKey(reservedKey.getName()));
        assertTrue(event.getPropertyNames().contains(reservedKey.getName()));
    }

    @Test
    public void testRepeatedGetSetPropertyReservedKey() {
        final ReservedKey key = new ReservedKey("repeated.reserved.key");
        for (int i = 0; i < 100; i++) {
            event.setProperty(key, Integer.valueOf(i));
            assertEquals(Integer.valueOf(i), event.getProperty(key));
        }
    }

    @Test
    public void setGetSetPropertyStringReservedKey() {
        ReservedKey key = new ReservedKey("some.reserved.key");

        // Set the property using the string
        event.setProperty(key.getName(), Integer.valueOf(1));
        assertEquals(Integer.valueOf(1), event.getProperty(key.getName()));

        // Overwrite the property using a key
        event.setProperty(key, Integer.valueOf(2));
        assertEquals(Integer.valueOf(2), event.getProperty(key));

        // Make sure both return the same (updated) value
        assertEquals(event.getProperty(key), event.getProperty(key.getName()));
    }

    @Test
    public void testGetPropertyStringClassOfT() {
        final ArrayList<?> value = new ArrayList<Object>();

        event.setProperty("key", value);

        assertSame(value, event.getProperty("key", ArrayList.class));
        assertSame(value, event.getProperty("key", List.class));
        assertSame(value, event.getProperty("key", Object.class));
    }

    @Test(expected = ClassCastException.class)
    public void testGetPropertyStringClassOfTIncompatibleT() {
        event.setProperty("key", new ArrayList<Object>());
        Map<?, ?> map = event.getProperty("key", Map.class);
        assertNull("map " + map + " should not be a map", map);
    }

    @Test
    public void testGetPropertyReservedKeyClassOfT() {
        ReservedKey key = new ReservedKey("some.key");
        final ArrayList<?> value = new ArrayList<Object>();

        event.setProperty(key, value);

        assertSame(value, event.getProperty(key, ArrayList.class));
        assertSame(value, event.getProperty(key, List.class));
        assertSame(value, event.getProperty(key, Object.class));
    }

    @Test
    public void testGetPropertyNames() {
        assertNotNull(event.getPropertyNames());
        assertTrue(event.getPropertyNames().isEmpty());

        for (int i = 0; i < 100; i++) {
            String key = "string.property." + i;
            event.setProperty(key, Integer.valueOf(i));

            ReservedKey reservedKey = new ReservedKey("reserved.key.property." + i);
            event.setProperty(reservedKey, Integer.valueOf(i));
        }

        List<String> propertyNames = event.getPropertyNames();
        for (int i = 0; i < 100; i++) {
            String key = "string.property." + i;
            assertTrue(propertyNames.contains(key));
            assertEquals(Integer.valueOf(i), event.getProperty(key));
            assertEquals(Integer.valueOf(i), event.getProperty(key, Integer.class));

            ReservedKey reservedKey = new ReservedKey("reserved.key.property." + i);
            assertTrue(propertyNames.contains(reservedKey.getName()));
            assertEquals(Integer.valueOf(i), event.getProperty(reservedKey));
            assertEquals(Integer.valueOf(i), event.getProperty(reservedKey, Integer.class));
        }
    }

    @Test
    public void testSetReadOnly() {
        // Set a few properties and make sure they're good
        for (int i = 0; i < 10; i++) {
            String key = "string.property." + i;
            event.setProperty(key, Integer.valueOf(i));

            ReservedKey reservedKey = new ReservedKey("reserved.key.property." + i);
            event.setProperty(reservedKey, Integer.valueOf(i));
        }
        for (int i = 0; i < 10; i++) {
            String key = "string.property." + i;
            assertEquals(Integer.valueOf(i), event.getProperty(key));
            assertEquals(Integer.valueOf(i), event.getProperty(key, Integer.class));

            ReservedKey reservedKey = new ReservedKey("reserved.key.property." + i);
            assertEquals(Integer.valueOf(i), event.getProperty(reservedKey));
            assertEquals(Integer.valueOf(i), event.getProperty(reservedKey, Integer.class));
        }
        // Set the event's readOnly flag
        event.setReadOnly(true);

        try {
            event.setProperty("test.string", this);
            fail("setProperty(string) should have failed");
        } catch (UnsupportedOperationException uoe) {
            // expected error
        }
        assertNull("setProperty string bad update", event.getProperty("test.string"));

        ReservedKey setKey = new ReservedKey("test.key1");
        try {
            event.setProperty(setKey, "test.value1");
            fail("setProperty(key) should have failed");
        } catch (UnsupportedOperationException uoe) {
            // expected error
        }
        assertNull("setProperty key bad update", event.getProperty(setKey));

        try {
            Map<String, Object> newmap = new HashMap<String, Object>();
            newmap.put("test.value", "value");
            event.setProperties(newmap);
            fail("setProperties() should have failed");
        } catch (UnsupportedOperationException uoe) {
            // expected error
        }
        assertNull("setProperties bad update", event.getProperty("test.value"));

        try {
            event.getProperties().clear();
            fail("clear should have failed");
        } catch (UnsupportedOperationException uoe) {
            // expected error
        }
        assertTrue("should not be empty", (false == event.getProperties().isEmpty()));

        ReservedKey remKey = new ReservedKey("reserved.key.property.0");
        try {
            event.getProperties().remove(remKey);
            fail("remove() should have failed");
        } catch (UnsupportedOperationException uoe) {
            // expected error
        }
        assertNotNull("remove should have failed", event.getProperty(remKey));

        //        // Try to change the values all to zero and verify it didn't take
        //        for (int i = 0; i < 10; i++) {
        //            String key = "string.property." + i;
        //            event.setProperty(key, Integer.valueOf(0));
        //
        //            ReservedKey reservedKey = new ReservedKey("reserved.key.property." + i);
        //            event.setProperty(reservedKey, Integer.valueOf(0));
        //        }
        for (int i = 0; i < 10; i++) {
            String key = "string.property." + i;
            assertEquals(Integer.valueOf(i), event.getProperty(key));
            assertEquals(Integer.valueOf(i), event.getProperty(key, Integer.class));

            ReservedKey reservedKey = new ReservedKey("reserved.key.property." + i);
            assertEquals(Integer.valueOf(i), event.getProperty(reservedKey));
            assertEquals(Integer.valueOf(i), event.getProperty(reservedKey, Integer.class));
        }
    }

    @Test
    public void testSetProperties() {
        final List<String> keys = new ArrayList<String>();
        final List<ReservedKey> reservedKeys = new ArrayList<ReservedKey>();
        final Map<String, String> props = new HashMap<String, String>();

        // Make sure we're starting out empty
        assertTrue(event.getProperties().isEmpty());

        // Build up some keys and set data into the event that we'll overwrite
        for (int i = 0; i < 100; i++) {
            String key = "key." + i;
            keys.add(key);
            props.put(key, key);
            event.setProperty(key, Integer.valueOf(0));

            ReservedKey reservedKey = new ReservedKey("reserved.key." + i);
            reservedKeys.add(reservedKey);
            props.put(reservedKey.getName(), reservedKey.getName());
            event.setProperty(reservedKey, Integer.valueOf(0));
        }

        event.setProperties(props);

        assertNotSame(props, event.getProperties());
        for (String key : props.keySet()) {
            assertEquals(key, event.getProperties().get(key));
        }
    }

    @Test
    public void testGetTopicObject() {
        Topic topic1 = new Topic("com/ibm/liberty/test/Topic");
        EventImpl event1 = new EventImpl(topic1);
        assertSame(topic1, event1.getTopicObject());
        assertEquals(topic1.getName(), event1.getTopic());

        assertEquals(topic, event.getTopicObject());
        assertEquals(topic.getName(), event.getTopic());
    }

    @Test
    public void testGetTopic() {
        for (int i = 0; i < 100; i++) {
            Topic topic = new Topic("com/liberty/test/Topic" + i);
            EventImpl event = new EventImpl(topic);
            assertEquals(topic.getName(), event.getTopic());
        }
    }

    @Test
    public void testGetProperties() {
        assertNotNull(event.getProperties());
        assertTrue(event.getProperties().isEmpty());

        for (int i = 0; i < 100; i++) {
            String key = "key." + i;
            event.setProperty(key, Integer.valueOf(i));

            ReservedKey reservedKey = new ReservedKey("reserved.key." + i);
            event.setProperty(reservedKey, Integer.valueOf(i));
        }

        assertEquals(200, event.getProperties().size());

        for (int i = 0; i < 100; i++) {
            String key = "key." + i;
            assertTrue(event.getProperties().containsKey(key));
            assertEquals(Integer.valueOf(i), event.getProperties().get(key));

            key = "reserved.key." + i;
            assertTrue(event.getProperties().containsKey(key));
            assertEquals(Integer.valueOf(i), event.getProperties().get(key));
        }
    }

    @Test
    public void testMatches() {
        final Filter filter = context.mock(Filter.class);
        event.setReadOnly(true);
        final Dictionary<String, Object> properties = event.getProperties();
        context.checking(new Expectations() {
            {
                oneOf(filter).match(properties);
                will(returnValue(true));
            }
        });
        assertTrue(event.matches(filter));
    }

    @Test
    public void testToString() {
        String string = event.toString();
        assertNotNull(string);
        assertTrue(string.contains(topic.getName()));
    }

    @Test
    public void testGetContextData() {
        EventImpl event = new EventImpl(new Topic("Test"));
        CurrentEvent.push(event);
        EventLocal<String> local = EventLocal.createLocal("localname");
        event.set(local, "localvalue");
        assertEquals("localvalue", event.getContextData("localname"));
        CurrentEvent.pop();
    }

    @Ignore
    @Test
    public void testRun() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testFireEvent() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testFireSynchronousEvent() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testCancel() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testIsCancelled() {
        fail("Not yet implemented");
    }

    @Ignore
    @Test
    public void testIsDone() {
        fail("Not yet implemented");
    }

}
