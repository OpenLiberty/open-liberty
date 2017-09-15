/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ibm.websphere.event.EventLocal;

/**
 * Test the event local map structure.
 */
public class EventLocalMapTest {

    /**
     * Test a single map with no parent.
     */
    @Test
    public void testSingleMap() {
        EventLocal<String> key1 = EventLocal.createLocal();
        EventLocalMap<EventLocal<String>, String> map = new EventLocalMap<EventLocal<String>, String>();
        assertNull(map.get(key1));
        map.put(key1, "teststring");
        assertEquals("teststring", map.get(key1));
        assertEquals("teststring", map.remove(key1));
        assertNull(map.get(key1));

        EventLocal<String> key2 = EventLocal.createLocal();
        map.put(key1, "teststring1");
        map.put(key2, "teststring2");
        assertEquals("teststring1", map.get(key1));
        assertEquals("teststring2", map.get(key2));
        // modify one value
        map.put(key2, "teststring3");
        assertEquals("teststring1", map.get(key1));
        assertEquals("teststring3", map.get(key2));
        // remove one value
        assertEquals("teststring1", map.remove(key1));
        assertNull(map.get(key1));
        assertEquals("teststring3", map.get(key2));
    }

    /**
     * Test linked maps.
     */
    @Test
    public void testParentMap() {
        EventLocal<String> key1 = EventLocal.createLocal();
        EventLocal<String> key2 = EventLocal.createLocal();
        EventLocalMap<EventLocal<String>, String> parent = new EventLocalMap<EventLocal<String>, String>();
        parent.put(key1, "parent1");
        parent.put(key2, "parent2");

        EventLocalMap<EventLocal<String>, String> child = new EventLocalMap<EventLocal<String>, String>(parent);
        assertEquals("parent1", child.get(key1));
        assertEquals("parent2", child.get(key2));
        EventLocal<String> key3 = EventLocal.createLocal();
        // add something to the child map, no impact on parent
        child.put(key3, "child3");
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent2", parent.get(key2));
        assertNull(parent.get(key3));
        assertEquals("parent1", child.get(key1));
        assertEquals("parent2", child.get(key2));
        assertEquals("child3", child.get(key3));
        // remove something from the child map, no impact on parent
        assertEquals("parent1", child.remove(key1));
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent2", parent.get(key2));
        assertNull(child.get(key1));
        assertEquals("parent2", child.get(key2));
        assertEquals("child3", child.get(key3));
        // add something to parent, child sees update
        EventLocal<String> key4 = EventLocal.createLocal();
        parent.put(key4, "parent4");
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent2", parent.get(key2));
        assertEquals("parent4", parent.get(key4));
        assertEquals("parent2", child.get(key2));
        assertEquals("child3", child.get(key3));
        assertEquals("parent4", child.get(key4));
        // remove something from parent, child sees update
        assertEquals("parent4", parent.remove(key4));
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent2", parent.get(key2));
        assertEquals("parent2", child.get(key2));
        assertEquals("child3", child.get(key3));
        assertNull(child.get(key4));
        // change a child value
        child.put(key2, "child2");
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent2", parent.get(key2));
        assertEquals("child2", child.get(key2));
        assertEquals("child3", child.get(key3));
        // change a parent value that child has already changed
        parent.put(key2, "parent2Delta");
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent2Delta", parent.get(key2));
        assertEquals("child2", child.get(key2));
        assertEquals("child3", child.get(key3));
    }

    /**
     * Test map handling of named event locals.
     */
    @Test
    public void testNamedLocals() {
        EventLocal<String> key1 = EventLocal.createLocal("named1");
        assertEquals("named1", key1.toString());
        EventLocal<String> key2 = EventLocal.createLocal();
        EventLocalMap<EventLocal<String>, String> parent = new EventLocalMap<EventLocal<String>, String>();
        parent.put(key1, "parent1");
        parent.put(key2, "parent2");
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent1", parent.get("named1"));
        assertEquals("parent2", parent.get(key2));

        // modify a value
        parent.put(key1, "parent1new");
        assertEquals("parent1new", parent.get(key1));
        assertEquals("parent1new", parent.get("named1"));
        assertEquals("parent2", parent.get(key2));

        //remove a value
        assertEquals("parent1new", parent.remove(key1));
        assertNull(parent.get(key1));
        assertNull(parent.get("named1"));
        assertEquals("parent2", parent.get(key2));
        parent.put(key1, "parent1");
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent1", parent.get("named1"));
        assertEquals("parent2", parent.get(key2));

        EventLocalMap<EventLocal<String>, String> child = new EventLocalMap<EventLocal<String>, String>(parent);
        assertEquals("parent1", parent.get("named1"));
        assertEquals("parent1", child.get("named1"));
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent1", child.get(key1));
        assertEquals("parent2", parent.get(key2));
        assertEquals("parent2", child.get(key2));
        assertNull(parent.get("named2"));
        assertNull(child.get("named2"));

        // test putting into the child
        EventLocal<String> key3 = EventLocal.createLocal("named3");
        child.put(key3, "child3");
        assertEquals("parent1", parent.get("named1"));
        assertEquals("parent1", child.get("named1"));
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent1", child.get(key1));
        assertEquals("parent2", parent.get(key2));
        assertEquals("parent2", child.get(key2));
        assertNull(parent.get("named2"));
        assertEquals("child3", child.get("named3"));
        assertEquals("child3", child.get(key3));
        assertNull(parent.get("named3"));
        assertNull(parent.get(key3));

        // test removing parent value in child, no impact on parent
        child.remove(key1);
        assertEquals("parent1", parent.get("named1"));
        assertEquals("parent1", parent.get(key1));
        assertNull(child.get("named1"));
        assertNull(child.get(key1));
        assertEquals("parent2", parent.get(key2));
        assertEquals("parent2", child.get(key2));
        assertNull(parent.get("named2"));
        assertEquals("child3", child.get("named3"));
        assertEquals("child3", child.get(key3));
        assertNull(parent.get("named3"));
        assertNull(parent.get(key3));

        // add something to parent, child sees update
        EventLocal<String> key4 = EventLocal.createLocal("named4");
        parent.put(key4, "parent4");
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent1", parent.get("named1"));
        assertEquals("parent2", parent.get(key2));
        assertNull(parent.get("named3"));
        assertNull(parent.get(key3));
        assertEquals("parent4", parent.get(key4));
        assertEquals("parent4", parent.get("named4"));
        assertNull(child.get("named1"));
        assertNull(child.get(key1));
        assertEquals("parent2", child.get(key2));
        assertEquals("child3", child.get(key3));
        assertEquals("child3", child.get("named3"));
        assertEquals("parent4", child.get(key4));
        assertEquals("parent4", child.get("named4"));

        // remove something from parent, child sees update
        assertEquals("parent4", parent.remove(key4));
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent1", parent.get("named1"));
        assertEquals("parent2", parent.get(key2));
        assertNull(parent.get("named3"));
        assertNull(parent.get(key3));
        assertNull(parent.get("named4"));
        assertNull(parent.get(key4));
        assertNull(child.get("named1"));
        assertNull(child.get(key1));
        assertEquals("parent2", child.get(key2));
        assertEquals("child3", child.get(key3));
        assertEquals("child3", child.get("named3"));
        assertNull(child.get(key4));
        assertNull(child.get("named4"));

        // change a child value
        child.put(key1, "child1");
        assertEquals("parent1", parent.get(key1));
        assertEquals("parent1", parent.get("named1"));
        assertEquals("parent2", parent.get(key2));
        assertNull(parent.get("named3"));
        assertNull(parent.get(key3));
        assertNull(parent.get("named4"));
        assertNull(parent.get(key4));
        assertEquals("child1", child.get(key1));
        assertEquals("child1", child.get("named1"));
        assertEquals("parent2", child.get(key2));
        assertEquals("child3", child.get(key3));
        assertEquals("child3", child.get("named3"));
        assertNull(child.get(key4));
        assertNull(child.get("named4"));

        // change a parent value that child has already changed
        parent.put(key1, "parent1Delta");
        assertEquals("parent1Delta", parent.get(key1));
        assertEquals("parent1Delta", parent.get("named1"));
        assertEquals("parent2", parent.get(key2));
        assertNull(parent.get("named3"));
        assertNull(parent.get(key3));
        assertNull(parent.get("named4"));
        assertNull(parent.get(key4));
        assertEquals("child1", child.get(key1));
        assertEquals("child1", child.get("named1"));
        assertEquals("parent2", child.get(key2));
        assertEquals("child3", child.get(key3));
        assertEquals("child3", child.get("named3"));
        assertNull(child.get(key4));
        assertNull(child.get("named4"));
    }

    /**
     * Run tests that trigger map storage growth.
     */
    @Test
    public void testMapExpansion() {
        EventLocalMap<Object, String> map = new EventLocalMap<Object, String>();
        // dump a bunch of items that will result in expanding internal storage
        final int iterations = 200;
        List<Object> keys = new ArrayList<Object>(iterations);
        for (int i = 0; i < iterations; i++) {
            EventLocal<String> key = EventLocal.createLocal("name" + i);
            keys.add(key);
            map.put(key, "value" + i);
        }
        // now make sure they're all there correctly
        for (int i = 0; i < iterations; i++) {
            assertEquals("value" + i, map.get(keys.get(i)));
            assertEquals("value" + i, map.get("name" + i));
        }
    }
}
