/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class RoleInfoTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule outputRule = outputMgr;

    /**
     * Tests constructor
     * Expected result: get the expected parameters.
     */
    @Test
    public void RoleInfoCtorRoleOnly() {
        String rn = "roleName";
        RoleInfo ri = new RoleInfo(rn);
        assertEquals(rn, ri.getRoleName());
        assertFalse(ri.isDenyAll());
        assertFalse(ri.isPermitAll());
    }

    /**
     * Tests constructor
     * Expected result: get the expected parameters.
     */
    @Test
    public void RoleInfoCtorNoParam() {
        RoleInfo ri = new RoleInfo();
        assertNull(ri.getRoleName());
        assertFalse(ri.isDenyAll());
        assertFalse(ri.isPermitAll());
    }

    /**
     * Tests setPermitAll
     * Expected result: get the expected parameters.
     */
    @Test
    public void setPermitAllTest() {
        String rn = "roleName";
        RoleInfo ri = new RoleInfo(rn);
        assertEquals(rn, ri.getRoleName());
        ri.setPermitAll();
        assertNull(ri.getRoleName());
        assertFalse(ri.isDenyAll());
        assertTrue(ri.isPermitAll());
    }

    /**
     * Tests setDenyAll
     * Expected result: get the expected parameters.
     */
    @Test
    public void setDenyAllTest() {
        String rn = "roleName";
        RoleInfo ri = new RoleInfo(rn);
        assertEquals(rn, ri.getRoleName());
        ri.setDenyAll();
        assertNull(ri.getRoleName());
        assertTrue(ri.isDenyAll());
        assertFalse(ri.isPermitAll());
    }

    /**
     * Tests toString
     * Expected result: get the expected result
     */
    @Test
    public void toStringTest() {
        String rn = "roleName";
        String output = "role : " + rn + " DenyAll : false PermitAll : false";

        RoleInfo ri = new RoleInfo(rn);
        assertEquals(output, ri.toString());
    }
}
