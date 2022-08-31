/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.extensions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HttpAuthenticationMechanismsTrackerTest {

    private static final String APP_NAME = "App1";

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

}
