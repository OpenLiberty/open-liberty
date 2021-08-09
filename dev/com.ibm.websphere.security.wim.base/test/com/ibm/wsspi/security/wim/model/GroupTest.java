/*******************************************************************************
 * Copyright (c) 2014,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GroupTest {

    @Test
    public void testCaseSentitive() {
        Group group = new Group();
        assertEquals("String", group.getDataType("cn"));
    }

    @Test
    public void testCaseInSentitive() {
        Group group = new Group();
        assertEquals(null, group.getDataType("CN"));
    }

    @Test
    public void isMultiValuedProperty() {
        Group entity = new Group();
        Group.addExtendedProperty("extendedProperty1", "String", false, null);
        Group.addExtendedProperty("extendedProperty2", "String", true, null);

        /*
         * Test standard properties.
         */
        assertFalse(entity.isMultiValuedProperty("cn"));
        assertTrue(entity.isMultiValuedProperty("members"));
        assertTrue(entity.isMultiValuedProperty("displayName"));
        assertTrue(entity.isMultiValuedProperty("description"));
        assertTrue(entity.isMultiValuedProperty("businessCategory"));
        assertTrue(entity.isMultiValuedProperty("seeAlso"));

        /*
         * Check extended properties.
         */
        assertFalse(entity.isMultiValuedProperty("extendedProperty1"));
        assertTrue(entity.isMultiValuedProperty("extendedProperty2"));

        /*
         * Check super class properties.
         */
        PartyTest.isMultiValuedProperty(entity);
    }
}
