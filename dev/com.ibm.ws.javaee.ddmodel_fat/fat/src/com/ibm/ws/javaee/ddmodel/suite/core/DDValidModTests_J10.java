/*******************************************************************************
 * Copyright (c) 2020,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
@MinimumJavaLevel(javaLevel = 11)
@Mode(TestMode.LITE)
public class DDValidModTests_J10 extends CommonTests_Core {
    public static final Class<?> TEST_CLASS = DDValidModTests_J10.class;

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(TEST_CLASS, "server_mod_j10.xml", setUpTestModules_J10);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        commonTearDown(TEST_CLASS, tearDownTestModules, NO_ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Module_J10() throws Exception {
        test(TEST_CLASS, "testAutoInstall");    
    }

    @Test
    public void testBindingsConfig_Module_J10() throws Exception {
        test(TEST_CLASS, "testBindingsConfig");
    }   
    
    @Test
    public void testWebExtension_Module_J10() throws Exception {
        test(TEST_CLASS, "testWebExtension");
    }
    
    @Test
    public void testWebExtensionNoBindings_Module_J10() throws Exception {
        test(TEST_CLASS, "testWebExtensionNoBindings");
    }
    
    @Test
    public void testWebBindings_Module_J10() throws Exception {
        test(TEST_CLASS, "testWebBindings");
    }
    
    @Test
    public void testWebBindingsNoBindings_Module_J10() throws Exception {
        test(TEST_CLASS, "testWebBindingsNoBindings");
    }
    
    @Test
    public void testEJBBindings_Module_J10() throws Exception {
        test(TEST_CLASS, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtension_Module_J10() throws Exception {
        test(TEST_CLASS, "testEJBExtension");
    }
    
    @Test
    public void testEJBBindingsNoBindings_Module_J10() throws Exception {
        test(TEST_CLASS, "testEJBBindingsNoBindings");
    }
    
    @Test
    public void testEJBExtensionNoBindings_Module_J10() throws Exception {
        test(TEST_CLASS, "testEJBExtensionNoBindings");
    }
    
    @Test
    public void testApplicationExtensionFromWebApp_Module_J10() throws Exception {
        test(TEST_CLASS, "testApplicationExtensionFromWebApp");
    }

    @Test
    public void testSecurityRoleOverridesFromWebApp_Module_J10() throws Exception {
        test(TEST_CLASS, "testSecurityRoleOverridesFromWebApp");
    }

    @Test
    public void testManagedBeanBindings_Module_J10() throws Exception {
        test(TEST_CLASS, "testManagedBeanBindings");
    }
}
