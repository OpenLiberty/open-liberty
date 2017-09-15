/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.basic.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 *
 */
public class BasicGroupTest {

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicGroup#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals_null() {
        BasicGroup group = new BasicGroup("group", new HashSet<String>());
        assertFalse(group.equals(null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicGroup#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals_BasicUser() {
        BasicGroup group = new BasicGroup("group", new HashSet<String>());
        assertFalse(group.equals(new BasicUser("user", "password")));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicGroup#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals_differentName() {
        BasicGroup group = new BasicGroup("group", new HashSet<String>());
        BasicGroup groupX = new BasicGroup("groupX", new HashSet<String>());
        assertFalse(group.equals(groupX));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicGroup#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals_sameNameDifferentGroups() {
        Set<String> set1 = new HashSet<String>();
        set1.add("group1");
        BasicGroup group = new BasicGroup("group", set1);
        Set<String> set2 = new HashSet<String>();
        set2.add("group2");
        BasicGroup groupX = new BasicGroup("group", set2);
        assertTrue(group.equals(groupX));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicGroup#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals_self() {
        BasicGroup group = new BasicGroup("group", new HashSet<String>());
        assertTrue(group.equals(group));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicGroup#hashCode()}.
     */
    @Test
    public void testHashCode() {
        BasicGroup group = new BasicGroup("group", new HashSet<String>());
        assertEquals("Hash should be the hash of the name",
                     "group".hashCode(), group.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicGroup#toString()}.
     */
    @Test
    public void testToString() {
        BasicGroup group = new BasicGroup("group", new HashSet<String>());
        assertEquals("toString should be the name and group set",
                     "group" + ", " + new HashSet<String>(), group.toString());
    }
}
