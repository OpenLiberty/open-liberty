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
package com.ibm.ws.webcontainer.security.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;

/**
 *
 */
public class SecurityConstraintTest {

    private static SharedOutputManager outputMgr;
    private static List<String> testRoles;
    private static SecurityConstraint securityConstraint;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
        testRoles = createTestRoles();
        securityConstraint = createTestSecurityConstraint();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    @Test
    public void getRoles() {
        final String methodName = "getRoles";
        try {
            List<String> roles = securityConstraint.getRoles();
            assertEquals("The roles must be the same as the ones used in the constructor.", testRoles, roles);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void isSSLRequired() {
        final String methodName = "isSSLRequired";
        try {
            assertFalse("The SSL required must be the same as the one used in the constructor.", securityConstraint.isSSLRequired());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void isAccessPrecluded() {
        final String methodName = "isAccessPrecluded";
        try {
            assertFalse("The access precluded must be the same as the one used in the constructor.", securityConstraint.isAccessPrecluded());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private static SecurityConstraint createTestSecurityConstraint() {
        boolean accessPrecluded = false;
        boolean sslRequired = false;
        boolean fromHttpConstraint = false;
        boolean accessUncovered = false;
        SecurityConstraint securityConstraint = new SecurityConstraint(null, testRoles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        return securityConstraint;
    }

    private static List<String> createTestRoles() {
        List<String> roles = new ArrayList<String>();
        roles.add("tester");
        return roles;
    }
}
