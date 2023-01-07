/*******************************************************************************
 * Copyright (c) 2020,2022 IBM Corporation and others.
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
public class DDValidModMinimalHeaderTests extends CommonTests_Core {
    public static final Class<?> TEST_CLASS = DDValidModMinimalHeaderTests.class;

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(TEST_CLASS, "server_mod.xml", setUpTestModulesMinimalHeaders);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        commonTearDown(TEST_CLASS, tearDownTestModules, NO_ALLOWED_ERRORS);
    }

    //

    @Test
    public void testAutoInstall_Module_MH() throws Exception {
        test(TEST_CLASS, "testAutoInstall");    
    }

    @Test
    public void testBindingsConfig_Module_MH() throws Exception {
        test(TEST_CLASS, "testBindingsConfig");
    }   
    
    @Test
    public void testWebExtension_Module_MH() throws Exception {
        test(TEST_CLASS, "testWebExtension");
    }
    
    @Test
    public void testWebBindings_Module_MH() throws Exception {
        test(TEST_CLASS, "testWebBindings");
    }
    
    @Test
    public void testEJBBindings_Module_MH() throws Exception {
        test(TEST_CLASS, "testEJBBindings");
    }
    
    @Test
    public void testEJBExtension_Module_MH() throws Exception {
        test(TEST_CLASS, "testEJBExtension");
    }
    
    @Test
    public void testApplicationExtensionFromWebApp_Module_MH() throws Exception {
        test(TEST_CLASS, "testApplicationExtensionFromWebApp");
    }
    
    @Test
    public void testSecurityRoleOverridesFromWebApp_Module_MH() throws Exception {
        test(TEST_CLASS, "testSecurityRoleOverridesFromWebApp");
    }
    
    @Test
    public void testManagedBeanBindings_Module_MH() throws Exception {
        test(TEST_CLASS, "testManagedBeanBindings");
    }
}
