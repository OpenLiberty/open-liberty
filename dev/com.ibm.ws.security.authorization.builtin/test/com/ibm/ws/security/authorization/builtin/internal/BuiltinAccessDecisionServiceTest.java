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
package com.ibm.ws.security.authorization.builtin.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Vector;

import javax.security.auth.Subject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class BuiltinAccessDecisionServiceTest {
    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final String resourceName = "myApp";
    private final Subject subject = new Subject();
    private final static ArrayList<String> requiredRoles = new ArrayList<String>();
    private final static ArrayList<String> emptyList = new ArrayList<String>();
    private final static String role1 = "Manager";
    private final static String role2 = "Developer";
    private final static String role3 = "Employee";
    private static Vector<String> assignedRoles = new Vector<String>();
    private final static BuiltinAccessDecisionService accDecision = new BuiltinAccessDecisionService();

    /**
     * Test method for {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAccessDecisionService#BuiltinAccessDecisionService()} .
     */
    @Test
    public void testBuiltinAccessDecision() {
        assertNotNull("We should be able to create a new BuiltinAccessDecisionService object",
                      new BuiltinAccessDecisionService());
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAccessDecisionService#isGranted(java.lang.String, java.util.List, java.util.List, javax.security.auth.Subject)}
     * .
     */
    @Test
    public void testIsGranted_true() {
        requiredRoles.add(role3);
        assignedRoles.add(role1);
        assignedRoles.add(role3);
        assertTrue("isGranted should return true",
                   accDecision.isGranted(resourceName, requiredRoles, assignedRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAccessDecisionService#isGranted(java.lang.String, java.util.List, java.util.List, javax.security.auth.Subject)}
     * .
     */
    @Test
    public void testIsGranted_false() {
        requiredRoles.clear();
        assignedRoles.clear();
        requiredRoles.add(role2);
        assignedRoles.add(role1);
        assignedRoles.add(role3);
        assertFalse("isGranted should return false",
                    accDecision.isGranted(resourceName, requiredRoles, assignedRoles, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAccessDecisionService#isGranted(java.lang.String, java.util.List, java.util.List, javax.security.auth.Subject)}
     * .
     */
    @Test
    public void testIsGranted_falseNullAssignedRoles() {
        assertFalse("isGranted should return false",
                    accDecision.isGranted(resourceName, requiredRoles, null, subject));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authorization.builtin.internal.BuiltinAccessDecisionService#isGranted(java.lang.String, java.util.List, java.util.List, javax.security.auth.Subject)}
     * .
     */
    @Test
    public void testIsGranted_falseEmptyAssignedRoles() {
        assignedRoles.clear();
        assertFalse("isGranted should return false",
                    accDecision.isGranted(resourceName, requiredRoles, emptyList, subject));
    }
}
