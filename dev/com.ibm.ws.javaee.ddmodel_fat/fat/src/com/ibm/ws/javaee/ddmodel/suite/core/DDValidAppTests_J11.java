/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
@Mode(TestMode.LITE)
public class DDValidAppTests_J11 extends CommonTests_Core {
    public static final Class<?> TEST_CLASS = DDValidAppTests_J11.class;

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(TEST_CLASS, "server_app_j11.xml", setUpTestApp_J11);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        commonTearDown(TEST_CLASS, tearDownTestApp, NO_ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Application_J11() throws Exception {
        test(TEST_CLASS, "testAutoInstall");    
    }

    @Test
    public void testConfig_Application_J11() throws Exception {
        test(TEST_CLASS, "testConfig");
    }

    @Test
    public void testBindingsConfig_Application_J11() throws Exception {
        test(TEST_CLASS, "testBindingsConfig");
    }
    
    @Test
    public void testSecurityRoleOverrides_Application_J11() throws Exception {
        test(TEST_CLASS, "testSecurityRoleOverrides");
    }
    
    @Test
    public void testWebExtension_Application_J11() throws Exception {
        test(TEST_CLASS, "testWebExtension");
    }
    
    @Test
    public void testWebExtensionNoBindings_Application_J11() throws Exception {
        test(TEST_CLASS, "testWebExtensionNoBindings");
    }
    
    @Test
    public void testWebBindings_Application_J11() throws Exception {
        test(TEST_CLASS, "testWebBindings");
    }
    
    @Test
    public void testWebBindingsNoBindings_Application_J11() throws Exception {
        test(TEST_CLASS, "testWebBindingsNoBindings");
    }
    
    @Test
    public void testEJBBindings_Application_J11() throws Exception {
        test(TEST_CLASS, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtension_Application_J11() throws Exception {
        test(TEST_CLASS, "testEJBExtension");
    }
    
    @Test
    public void testEJBBindingsNoBindings_Application_J11() throws Exception {
        test(TEST_CLASS, "testEJBBindingsNoBindings");
    }
    
    @Test
    public void testEJBExtensionNoBindings_Application_J11() throws Exception {
        test(TEST_CLASS, "testEJBExtensionNoBindings");
    }
    
    @Test
    public void testApplicationExtension_Application_J11() throws Exception {
        test(TEST_CLASS, "testApplicationExtension");
    }
    
    @Test
    public void testManagedBeanBindings_Application_J11() throws Exception {
        test(TEST_CLASS, "testManagedBeanBindings");
    }
    
    @Test
    public void testWebserviceBindings_Application_J11() throws Exception {
        test(TEST_CLASS, "testWebserviceBindings");
    }

    @Test
    public void testWebserviceBindingsNoBindings_Application_J11() throws Exception {
        test(TEST_CLASS, "testWebserviceBindingsNoBindings");
    }
}
