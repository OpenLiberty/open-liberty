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
public class DDValidModTests_J11 extends CommonTests_Core {
    public static final Class<?> TEST_CLASS = DDValidModTests_J11.class;

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(TEST_CLASS, "server_mod_j11.xml", setUpTestModules_J11);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        commonTearDown(TEST_CLASS, tearDownTestModules, NO_ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Module_J11() throws Exception {
        test(TEST_CLASS, "testAutoInstall");    
    }

    @Test
    public void testBindingsConfig_Module_J11() throws Exception {
        test(TEST_CLASS, "testBindingsConfig");
    }   
    
    @Test
    public void testWebExtension_Module_J11() throws Exception {
        test(TEST_CLASS, "testWebExtension");
    }
    
    @Test
    public void testWebExtensionNoBindings_Module_J11() throws Exception {
        test(TEST_CLASS, "testWebExtensionNoBindings");
    }
    
    @Test
    public void testWebBindings_Module_J11() throws Exception {
        test(TEST_CLASS, "testWebBindings");
    }
    
    @Test
    public void testWebBindingsNoBindings_Module_J11() throws Exception {
        test(TEST_CLASS, "testWebBindingsNoBindings");
    }
    
    @Test
    public void testEJBBindings_Module_J11() throws Exception {
        test(TEST_CLASS, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtension_Module_J11() throws Exception {
        test(TEST_CLASS, "testEJBExtension");
    }
    
    @Test
    public void testEJBBindingsNoBindings_Module_J11() throws Exception {
        test(TEST_CLASS, "testEJBBindingsNoBindings");
    }
    
    @Test
    public void testEJBExtensionNoBindings_Module_J11() throws Exception {
        test(TEST_CLASS, "testEJBExtensionNoBindings");
    }
    
    @Test
    public void testApplicationExtensionFromWebApp_Module_J11() throws Exception {
        test(TEST_CLASS, "testApplicationExtensionFromWebApp");
    }

    @Test
    public void testSecurityRoleOverridesFromWebApp_Module_J11() throws Exception {
        test(TEST_CLASS, "testSecurityRoleOverridesFromWebApp");
    }

    @Test
    public void testManagedBeanBindings_Module_J11() throws Exception {
        test(TEST_CLASS, "testManagedBeanBindings");
    }
}
