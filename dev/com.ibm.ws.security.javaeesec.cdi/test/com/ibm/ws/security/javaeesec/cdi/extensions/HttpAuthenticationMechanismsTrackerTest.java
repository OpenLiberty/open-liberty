/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.security.javaeesec.cdi.extensions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HttpAuthenticationMechanismsTrackerTest {

    private static final String APP_NAME = "App1";
    private static final String MODULE_NAME = "App1.war";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testInitialize_nullAppName_nullMap() {
        HttpAuthenticationMechanismsTracker httpAuthenticationMechanismsTracker = new HttpAuthenticationMechanismsTracker();
        httpAuthenticationMechanismsTracker.initialize(null);

        assertNull("There must not be a module map for a null application name.", httpAuthenticationMechanismsTracker.getModuleMap(null));
    }

    @Test
    public void testInitialize_nonNullAppName_nonNullMap() {
        HttpAuthenticationMechanismsTracker httpAuthenticationMechanismsTracker = new HttpAuthenticationMechanismsTracker();
        httpAuthenticationMechanismsTracker.initialize(APP_NAME);

        assertNotNull("There must be a module map for a non null application name.", httpAuthenticationMechanismsTracker.getModuleMap(APP_NAME));
    }

    @Test
    public void testGetAuthMechs_nullAppName_null() {
        HttpAuthenticationMechanismsTracker httpAuthenticationMechanismsTracker = new HttpAuthenticationMechanismsTracker();
        httpAuthenticationMechanismsTracker.initialize(null);

        assertNull("There must not be AuthMechs for a null application name.", httpAuthenticationMechanismsTracker.getAuthMechs(null, null));
    }

    @Test
    public void testGetAuthMechs_nonNullAppName_uninitialized_null() {
        HttpAuthenticationMechanismsTracker httpAuthenticationMechanismsTracker = new HttpAuthenticationMechanismsTracker();

        assertNull("There must not be AuthMechs for an uninitialized HttpAuthenticationMechanismsTracker.",
                   httpAuthenticationMechanismsTracker.getAuthMechs(APP_NAME, MODULE_NAME));
    }

    @Test
    public void testIsEmptyModuleMap_nullAppName_true() {
        HttpAuthenticationMechanismsTracker httpAuthenticationMechanismsTracker = new HttpAuthenticationMechanismsTracker();
        httpAuthenticationMechanismsTracker.initialize(null);

        assertTrue("The module map is empty for a null application name.", httpAuthenticationMechanismsTracker.isEmptyModuleMap(null));
    }

}
