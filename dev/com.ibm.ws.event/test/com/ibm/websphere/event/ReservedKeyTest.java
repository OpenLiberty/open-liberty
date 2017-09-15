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
package com.ibm.websphere.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReservedKeyTest {

    @Test(expected = NullPointerException.class)
    public void testReservedKeyNullName() {
        new ReservedKey(null);
    }

    @Test
    public void testHashCode() {
        ReservedKey test = new ReservedKey("test");
        assertEquals(test.hashCode(), test.getSlot());

        ReservedKey test1 = new ReservedKey("test");
        assertEquals(test.hashCode(), test1.hashCode());

        ReservedKey test2 = new ReservedKey("test2");
        assertFalse(test1.hashCode() == test2.hashCode());
        assertEquals(test2.hashCode(), test2.getSlot());
    }

    @Test
    public void testGetName() {
        ReservedKey test = new ReservedKey("test");
        ReservedKey test1 = new ReservedKey("test");
        ReservedKey test2 = new ReservedKey("test2");

        assertEquals("test", test.getName());
        assertEquals("test", test1.getName());
        assertEquals("test2", test2.getName());
    }

    @Test
    public void testEquals() {
        ReservedKey test = new ReservedKey("test");
        ReservedKey test1 = new ReservedKey("test");
        ReservedKey test2 = new ReservedKey("test2");
        ReservedKey test3 = new ReservedKey("test2");

        assertNotSame(test, test1);
        assertEquals(test, test1);

        assertNotSame(test, test1);
        assertFalse(test1.equals(test2));
        assertEquals(test2, test3);
    }

    @Test
    public void testToString() {
        ReservedKey test = new ReservedKey("test");
        assertNotNull(test.toString());
        assertTrue(test.toString().contains("test"));
        assertTrue(test.toString().contains("@" + Integer.toHexString(test.getSlot())));
    }
}
