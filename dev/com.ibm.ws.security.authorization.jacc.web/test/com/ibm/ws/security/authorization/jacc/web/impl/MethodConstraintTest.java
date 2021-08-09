/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.web.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class MethodConstraintTest {

    /**
     * Tests constructor
     * Expected result: make sure that the object is constructed properly
     * and all fields are set as default value.
     */
    @Test
    public void ctorNormal() {
        MethodConstraint mc = new MethodConstraint();
        assertNotNull(mc);
        assertTrue(mc.isRoleSetEmpty());
        assertFalse(mc.isExcluded());
        assertFalse(mc.isUnchecked());
        assertFalse(mc.isUserDataNone());
        assertNull(mc.getUserData());
    }

    /**
     * Tests setExcludedMethod
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setExcludedNormal() {
        MethodConstraint mc = new MethodConstraint();
        mc.setExcluded();
        assertTrue(mc.isExcluded());
    }

    /**
     * Tests setUnchecked
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUncheckedNormal() {
        MethodConstraint mc = new MethodConstraint();
        mc.setUnchecked();
        assertTrue(mc.isUnchecked());
    }

    /**
     * Tests setRole
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setRoleNormal() {
        MethodConstraint mc = new MethodConstraint();
        mc.setRole("ROLE1");
        assertFalse(mc.isRoleSetEmpty());
        List<String> list = mc.getRoleList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("ROLE1", list.get(0));
        mc.setRole("ROLE2");
        assertFalse(mc.isRoleSetEmpty());
        list = mc.getRoleList();
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("ROLE1", list.get(0));
        assertEquals("ROLE2", list.get(1));
    }

    /**
     * Tests setUserData
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUserDataNone() {
        MethodConstraint mc = new MethodConstraint();
        mc.setUserData("NONE");
        assertTrue(mc.isUserDataNone());
        // check constraintType isn't changed
        assertNull(mc.getUserData());
        mc.setUserData("NONE");
        assertTrue(mc.isUserDataNone());
        // check constraintType isn't changed
        assertNull(mc.getUserData());
    }

    /**
     * Tests setUserData
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUserDataConfidential() {
        MethodConstraint mc = new MethodConstraint();
        mc.setUserData("CONFIDENTIAL");
        assertFalse(mc.isUserDataNone());
        assertEquals(mc.getUserData(), "CONFIDENTIAL");
        mc.setUserData("CONFIDENTIAL");
        assertFalse(mc.isUserDataNone());
        assertEquals(mc.getUserData(), "CONFIDENTIAL");
    }

    /**
     * Tests setUserData
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUserDataMixed() {
        MethodConstraint mc = new MethodConstraint();
        mc.setUserData("CONFIDENTIAL");
        assertFalse(mc.isUserDataNone());
        assertEquals(mc.getUserData(), "CONFIDENTIAL");
        mc.setUserData("NONE");
        assertTrue(mc.isUserDataNone());
        assertEquals(mc.getUserData(), "CONFIDENTIAL");
    }

    /**
     * Tests setUserData
     * Expected result: make sure that the value is set properly.
     */
    @Test
    public void setUserDataMixedAlternative() {
        // this testcase is a reverse order of testSetUserDataMixed1 to invoke setUserData 
        MethodConstraint mc = new MethodConstraint();
        mc.setUserData("NONE");
        assertTrue(mc.isUserDataNone());
        assertNull(mc.getUserData());
        mc.setUserData("CONFIDENTIAL");
        assertTrue(mc.isUserDataNone());
        assertEquals(mc.getUserData(), "CONFIDENTIAL");
    }
}
