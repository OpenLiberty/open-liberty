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
package com.ibm.ws.jmx.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.ibm.ws.jmx.internal.DelayedMBeanRegistrationState;

/**
 *
 */
public class DelayedMBeanRegistrationStateTest {

    @Test
    public void testValueOf() {
        assertSame("Expecting same instance as enum constant.", DelayedMBeanRegistrationState.DELAYED, DelayedMBeanRegistrationState.valueOf("DELAYED"));
        assertSame("Expecting same instance as enum constant.", DelayedMBeanRegistrationState.PROCESSING, DelayedMBeanRegistrationState.valueOf("PROCESSING"));
        assertSame("Expecting same instance as enum constant.", DelayedMBeanRegistrationState.REGISTERED, DelayedMBeanRegistrationState.valueOf("REGISTERED"));
        assertSame("Expecting same instance as enum constant.", DelayedMBeanRegistrationState.UNREGISTERED, DelayedMBeanRegistrationState.valueOf("UNREGISTERED"));
    }

    @Test
    public void testValues() {
        DelayedMBeanRegistrationState[] values = DelayedMBeanRegistrationState.values();
        assertEquals(4, values.length);
        Set<DelayedMBeanRegistrationState> set = new HashSet<DelayedMBeanRegistrationState>(Arrays.asList(values));
        assertEquals(4, set.size());
        assertTrue("Expecting DELAYED", set.contains(DelayedMBeanRegistrationState.DELAYED));
        assertTrue("Expecting PROCESSING", set.contains(DelayedMBeanRegistrationState.PROCESSING));
        assertTrue("Expecting REGISTERED", set.contains(DelayedMBeanRegistrationState.REGISTERED));
        assertTrue("Expecting UNREGISTERED", set.contains(DelayedMBeanRegistrationState.UNREGISTERED));
    }
}
