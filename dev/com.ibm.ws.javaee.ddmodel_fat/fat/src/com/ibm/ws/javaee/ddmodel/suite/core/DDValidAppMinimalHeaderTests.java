/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.suite.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class DDValidAppMinimalHeaderTests extends CommonTests_Core {
    public static final Class<?> TEST_CLASS = DDValidAppMinimalHeaderTests.class;

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(TEST_CLASS, "server_app.xml", setUpTestAppMinimalHeaders);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        commonTearDown(TEST_CLASS, tearDownTestApp, NO_ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Application_MH() throws Exception {
        test(TEST_CLASS, "testAutoInstall");    
    }

    @Test
    public void testConfig_Application_MH() throws Exception {
        test(TEST_CLASS, "testConfig");
    }

    @Test
    public void testBindingsConfig_Application_MH() throws Exception {
        test(TEST_CLASS, "testBindingsConfig");
    }

    @Test
    public void testSecurityRoleOverrides_Application_MH() throws Exception {
        test(TEST_CLASS, "testSecurityRoleOverrides");
    }

    @Test
    public void testWebExtension_Application_MH() throws Exception {
        test(TEST_CLASS, "testWebExtension");
    }

    @Test
    public void testWebBindings_Application_MH() throws Exception {
        test(TEST_CLASS, "testWebBindings");
    }
    
    @Test
    public void testEJBBindings_Application_MH() throws Exception {
        test(TEST_CLASS, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtension_Application_MH() throws Exception {
        test(TEST_CLASS, "testEJBExtension");
    }
    
    @Test
    public void testApplicationExtension_Application_MH() throws Exception {
        test(TEST_CLASS, "testApplicationExtension");
    }
    
    @Test
    public void testManagedBeanBindings_Application_MH() throws Exception {
        test(TEST_CLASS, "testManagedBeanBindings");
    }
    
    @Test
    public void testWebserviceBindings_Application_MH() throws Exception {
        test(TEST_CLASS, "testWebserviceBindings");
    }
}
